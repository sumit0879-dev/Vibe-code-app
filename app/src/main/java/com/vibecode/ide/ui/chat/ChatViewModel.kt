package com.vibecode.ide.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecode.ide.data.remote.AiClient
import com.vibecode.ide.data.remote.StreamEvent
import com.vibecode.ide.data.repository.ChatRepository
import com.vibecode.ide.data.repository.FileRepository
import com.vibecode.ide.data.repository.ModelRepository
import com.vibecode.ide.data.repository.ProjectRepository
import com.vibecode.ide.data.repository.ProviderRepository
import com.vibecode.ide.domain.ContextBudget
import com.vibecode.ide.domain.model.AiModel
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.ChatMessage
import com.vibecode.ide.domain.model.ChatSession
import com.vibecode.ide.domain.model.MessageRole
import com.vibecode.ide.domain.model.ToolCallRecord
import com.vibecode.ide.domain.diff.DiffLine
import com.vibecode.ide.domain.diff.DiffUtil
import com.vibecode.ide.domain.tools.ParsedToolCall
import com.vibecode.ide.domain.tools.SystemPromptBuilder
import com.vibecode.ide.domain.tools.ToolCallParser
import com.vibecode.ide.domain.tools.ToolName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A file change awaiting explicit user approval before being written to disk. */
data class PendingChange(
    val messageId: String,
    val tool: ToolName,
    val path: String,
    val newContent: String?, // null for delete
    val oldContent: String?, // null if the file didn't exist yet
    val diff: List<DiffLine>,
)

data class ChatUiState(
    val session: ChatSession? = null,
    val providers: List<AiProvider> = emptyList(),
    val models: List<AiModel> = emptyList(),
    val selectedProvider: AiProvider? = null,
    val selectedModel: AiModel? = null,
    val isSending: Boolean = false,
    val pendingChange: PendingChange? = null,
    val errorMessage: String? = null,
    val autoContinueDepth: Int = 0,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository,
    private val aiClient: AiClient,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    private var streamingJob: Job? = null
    private val maxAutoContinues = 4
    private val maxTruncationContinuations = 3

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    val messages: StateFlow<List<ChatMessage>> = _uiState
        .map { it.session?.id }
        .distinctUntilChanged()
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else chatRepository.observeMessages(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val providers = providerRepository.observeProviders()
            providers.collect { list ->
                _uiState.value = _uiState.value.copy(providers = list)
            }
        }
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId)
            val session = chatRepository.getOrCreateLatestSession(
                projectId, project?.selectedProviderId, project?.selectedModelId,
            )
            _uiState.value = _uiState.value.copy(session = session)
            restoreSelection(session)
        }
    }

    private suspend fun restoreSelection(session: ChatSession) {
        val provider = session.providerId?.let { providerRepository.getProvider(it) }
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
        if (provider != null) {
            modelRepository.observeForProvider(provider.id).collect { models ->
                _uiState.value = _uiState.value.copy(
                    models = models,
                    selectedModel = models.firstOrNull { it.id == session.modelId } ?: _uiState.value.selectedModel,
                )
            }
        }
    }

    fun selectProvider(provider: AiProvider) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedProvider = provider, selectedModel = null, models = emptyList())
            projectRepository.updateSelectedModel(projectId, provider.id, null)
            _uiState.value.session?.let { chatRepository.setSessionModel(it.id, provider.id, null) }
            modelRepository.observeForProvider(provider.id).collect { models ->
                _uiState.value = _uiState.value.copy(models = models)
            }
        }
    }

    fun selectModel(model: AiModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedModel = model)
            projectRepository.updateSelectedModel(projectId, model.providerId, model.id)
            _uiState.value.session?.let { chatRepository.setSessionModel(it.id, model.providerId, model.id) }
        }
    }

    fun sendMessage(text: String) {
        val session = _uiState.value.session ?: return
        val provider = _uiState.value.selectedProvider
        val model = _uiState.value.selectedModel
        if (provider == null || model == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Select an AI provider and model first.")
            return
        }
        if (text.isBlank()) return

        viewModelScope.launch {
            chatRepository.appendMessage(chatRepository.newMessage(session.id, MessageRole.USER, text))
            runAssistantTurn(session, provider, model, autoContinueDepth = 0)
        }
    }

    private fun runAssistantTurn(session: ChatSession, provider: AiProvider, model: AiModel, autoContinueDepth: Int) {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)

            val project = projectRepository.getProject(projectId)
            val tree = project?.let { fileRepository.buildTree(it.rootPath) }
            val systemPrompt = SystemPromptBuilder.build(project?.name ?: "project", tree)
            val history = chatRepository.getMessages(session.id)

            val assistantMessage = chatRepository.appendMessage(
                chatRepository.newMessage(session.id, MessageRole.ASSISTANT, "", isStreaming = true),
            )

            streamWithContinuation(
                session = session, provider = provider, model = model, systemPrompt = systemPrompt,
                apiHistory = history, assistantMessage = assistantMessage, accumulated = "",
                continuationsLeft = maxTruncationContinuations, autoContinueDepth = autoContinueDepth,
            )
        }
    }

    /**
     * Streams one model turn, trimming/budgeting `apiHistory` via [ContextBudget] so it fits
     * the model's context window. If the reply gets cut off by the output-token limit
     * (finish_reason "length"/"max_tokens"), automatically resumes into the *same* visible
     * message instead of treating the partial text as final — a truncated reply (e.g. mid
     * tool-call JSON) used to get silently accepted as "done", which is what caused both the
     * unfinished-JSON display bug and the model re-attempting the same task on the next turn.
     */
    private suspend fun streamWithContinuation(
        session: ChatSession, provider: AiProvider, model: AiModel, systemPrompt: String,
        apiHistory: List<ChatMessage>, assistantMessage: ChatMessage, accumulated: String,
        continuationsLeft: Int, autoContinueDepth: Int,
    ) {
        val plan = ContextBudget.plan(model.contextLength, systemPrompt, apiHistory)
        val turnText = StringBuilder(accumulated)
        var finishReason: String? = null
        var streamError: StreamEvent.Error? = null

        aiClient.streamChat(provider, model.modelId, plan.history, systemPrompt, plan.maxOutputTokens).collect { event ->
            when (event) {
                is StreamEvent.Delta -> {
                    turnText.append(event.text)
                    chatRepository.updateMessage(assistantMessage.copy(content = turnText.toString(), isStreaming = true))
                }
                is StreamEvent.Done -> finishReason = event.finishReason
                is StreamEvent.Error -> streamError = event
            }
        }

        val error = streamError
        if (error != null) {
            chatRepository.updateMessage(
                assistantMessage.copy(
                    content = turnText.toString().ifBlank { "⚠️ ${error.message}" },
                    isStreaming = false, isError = true,
                )
            )
            _uiState.value = _uiState.value.copy(isSending = false, errorMessage = error.message)
            return
        }

        val wasTruncated = finishReason == "length" || finishReason == "max_tokens"
        if (wasTruncated && continuationsLeft > 0) {
            val continuedApiHistory = apiHistory +
                chatRepository.newMessage(session.id, MessageRole.ASSISTANT, turnText.toString()) +
                chatRepository.newMessage(session.id, MessageRole.USER, "Continue exactly where you left off. Do not repeat anything already written.")
            streamWithContinuation(
                session, provider, model, systemPrompt, continuedApiHistory, assistantMessage,
                turnText.toString(), continuationsLeft - 1, autoContinueDepth,
            )
            return
        }

        finishAssistantTurn(session, provider, model, assistantMessage.id, turnText.toString(), autoContinueDepth, stillTruncated = wasTruncated)
    }

    private suspend fun finishAssistantTurn(
        session: ChatSession, provider: AiProvider, model: AiModel,
        messageId: String, fullText: String, autoContinueDepth: Int,
        stillTruncated: Boolean = false,
    ) {
        val parsedCall = ToolCallParser.extract(fullText)

        if (parsedCall == null) {
            // Normally unreachable while truncated — streamWithContinuation resolves truncation
            // before we get here. This only fires if the continuation budget ran out, so give a
            // clear note instead of dumping half-written tool JSON into the chat.
            val displayText = if (stillTruncated) {
                ToolCallParser.stripDanglingToolBlock(fullText).ifBlank { fullText } +
                    "\n\n_(Response was cut short and couldn't finish — try asking to continue.)_"
            } else {
                ToolCallParser.stripToolBlock(fullText).ifBlank { fullText }
            }
            chatRepository.updateMessage(chatRepository.newMessage(session.id, MessageRole.ASSISTANT, displayText).copy(id = messageId, isStreaming = false))
            _uiState.value = _uiState.value.copy(isSending = false)
            return
        }

        val displayText = ToolCallParser.stripToolBlock(fullText).ifBlank { fullText }
        val project = projectRepository.getProject(projectId)
        if (project == null) {
            _uiState.value = _uiState.value.copy(isSending = false)
            return
        }

        if (!parsedCall.tool.mutatesFiles) {
            // Non-mutating tools run automatically, then we feed the result back for one more turn.
            val toolRecord = ToolCallRecord(parsedCall.tool.id, parsedCall.rawArgsJson, resultSummary = null, approved = true, path = parsedCall.path)
            chatRepository.updateMessage(
                ChatMessage(messageId, session.id, MessageRole.ASSISTANT, displayText, toolCalls = listOf(toolRecord), isStreaming = false),
            )
            val resultText = executeReadOnlyTool(project.rootPath, parsedCall)
            chatRepository.appendMessage(chatRepository.newMessage(session.id, MessageRole.TOOL, resultText))

            if (autoContinueDepth < maxAutoContinues) {
                runAssistantTurn(session, provider, model, autoContinueDepth + 1)
            } else {
                _uiState.value = _uiState.value.copy(isSending = false)
            }
            return
        }

        // Mutating tool: prepare a diff and require explicit user approval before touching disk.
        val oldContent = if (parsedCall.tool == ToolName.CREATE_FILE) null
        else fileRepository.readFile(project.rootPath, parsedCall.path.orEmpty()).getOrNull()
        val newContent = parsedCall.content ?: ""
        val diff = if (parsedCall.tool == ToolName.DELETE_FILE) emptyList()
        else DiffUtil.diff(oldContent.orEmpty(), newContent)

        val toolRecord = ToolCallRecord(
            toolName = parsedCall.tool.id, argsJson = parsedCall.rawArgsJson, resultSummary = null, approved = null,
            path = parsedCall.path, oldContent = oldContent,
            newContent = if (parsedCall.tool == ToolName.DELETE_FILE) null else newContent,
        )
        chatRepository.updateMessage(
            ChatMessage(messageId, session.id, MessageRole.ASSISTANT, displayText, toolCalls = listOf(toolRecord), isStreaming = false),
        )
        _uiState.value = _uiState.value.copy(
            isSending = false,
            pendingChange = PendingChange(
                messageId = messageId, tool = parsedCall.tool, path = parsedCall.path.orEmpty(),
                newContent = if (parsedCall.tool == ToolName.DELETE_FILE) null else newContent,
                oldContent = oldContent, diff = diff,
            ),
        )
    }

    private suspend fun executeReadOnlyTool(projectRoot: String, call: ParsedToolCall): String = when (call.tool) {
        ToolName.READ_FILE -> {
            fileRepository.readFile(projectRoot, call.path.orEmpty()).fold(
                onSuccess = { "Contents of ${call.path}:\n```\n$it\n```" },
                onFailure = { "Could not read ${call.path}: ${it.message}" },
            )
        }
        ToolName.LIST_DIRECTORY -> {
            try {
                val children = fileRepository.listChildren(projectRoot, call.path ?: ".")
                "Contents of ${call.path ?: "."}:\n" + children.joinToString("\n") { (if (it.isDirectory) "📁 " else "📄 ") + it.name }
            } catch (e: Exception) {
                "Could not list ${call.path ?: "."}: ${e.message}"
            }
        }
        ToolName.SEARCH_PROJECT -> {
            val hits = fileRepository.searchProject(projectRoot, call.query.orEmpty())
            if (hits.isEmpty()) "No matches found for \"${call.query}\"."
            else "Search results for \"${call.query}\":\n" + hits.joinToString("\n") { "${it.filePath}${it.lineNumber?.let { n -> ":$n" } ?: ""} — ${it.lineText}" }
        }
        else -> "(unsupported read-only tool)"
    }

    /** User tapped Approve on a pending file change diff. */
    fun approvePendingChange() {
        val pending = _uiState.value.pendingChange ?: return
        val session = _uiState.value.session ?: return
        val provider = _uiState.value.selectedProvider
        val model = _uiState.value.selectedModel

        viewModelScope.launch {
            val project = projectRepository.getProject(projectId) ?: return@launch
            val resultText = when (pending.tool) {
                ToolName.CREATE_FILE -> fileRepository.createFile(project.rootPath, pending.path, pending.newContent.orEmpty())
                    .fold({ "Created ${pending.path}." }, { "Failed to create ${pending.path}: ${it.message}" })
                ToolName.UPDATE_FILE -> fileRepository.writeFile(project.rootPath, pending.path, pending.newContent.orEmpty())
                    .fold({ "Updated ${pending.path}." }, { "Failed to update ${pending.path}: ${it.message}" })
                ToolName.DELETE_FILE -> fileRepository.deleteFile(project.rootPath, pending.path)
                    .fold({ "Deleted ${pending.path}." }, { "Failed to delete ${pending.path}: ${it.message}" })
                else -> "(nothing to do)"
            }
            updateToolRecordApproval(pending.messageId, approved = true, resultSummary = resultText)
            chatRepository.appendMessage(chatRepository.newMessage(session.id, MessageRole.TOOL, resultText))
            _uiState.value = _uiState.value.copy(pendingChange = null)

            if (provider != null && model != null && _uiState.value.autoContinueDepth < maxAutoContinues) {
                runAssistantTurn(session, provider, model, autoContinueDepth = 1)
            }
        }
    }

    fun rejectPendingChange() {
        val pending = _uiState.value.pendingChange ?: return
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            updateToolRecordApproval(pending.messageId, approved = false, resultSummary = "Rejected by user")
            chatRepository.appendMessage(
                chatRepository.newMessage(session.id, MessageRole.TOOL, "User rejected the proposed change to ${pending.path}."),
            )
            _uiState.value = _uiState.value.copy(pendingChange = null)
        }
    }

    fun dismissPendingChangePreviewOnly() {
        _uiState.value = _uiState.value.copy(pendingChange = null)
    }

    private suspend fun updateToolRecordApproval(messageId: String, approved: Boolean, resultSummary: String) {
        val messages = chatRepository.getMessages(_uiState.value.session!!.id)
        val message = messages.firstOrNull { it.id == messageId } ?: return
        val updatedCalls = message.toolCalls.map { it.copy(approved = approved, resultSummary = resultSummary) }
        chatRepository.updateMessage(message.copy(toolCalls = updatedCalls))
    }

    /**
     * Reverts a previously-approved file-mutating change back to how the file looked before it
     * was applied. Safe to call only on records with approved == true and revertedAt == null —
     * the UI only shows the Revert action in that state, but we re-check here too since this
     * writes to disk.
     */
    fun revertToolCall(messageId: String) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val project = projectRepository.getProject(projectId) ?: return@launch
            val message = chatRepository.getMessages(session.id).firstOrNull { it.id == messageId } ?: return@launch
            val record = message.toolCalls.firstOrNull() ?: return@launch
            if (record.approved != true || record.revertedAt != null) return@launch
            val tool = ToolName.fromId(record.toolName) ?: return@launch
            val path = record.path ?: return@launch

            val resultText = when (tool) {
                // Undo a create by deleting what was created.
                ToolName.CREATE_FILE -> fileRepository.deleteFile(project.rootPath, path)
                    .fold({ "Reverted: deleted $path." }, { "Revert failed: ${it.message}" })
                // Undo an update by writing the pre-change content back.
                ToolName.UPDATE_FILE -> fileRepository.writeFile(project.rootPath, path, record.oldContent.orEmpty())
                    .fold({ "Reverted $path to its previous version." }, { "Revert failed: ${it.message}" })
                // Undo a delete by recreating the file with what it contained before.
                ToolName.DELETE_FILE -> fileRepository.createFile(project.rootPath, path, record.oldContent.orEmpty())
                    .fold({ "Reverted: restored $path." }, { "Revert failed: ${it.message}" })
                else -> return@launch
            }

            val updatedCalls = message.toolCalls.map {
                if (it === record) it.copy(revertedAt = System.currentTimeMillis()) else it
            }
            chatRepository.updateMessage(message.copy(toolCalls = updatedCalls))
            chatRepository.appendMessage(chatRepository.newMessage(session.id, MessageRole.TOOL, resultText))
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.value = _uiState.value.copy(isSending = false)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    fun newChatSession() {
        viewModelScope.launch {
            val provider = _uiState.value.selectedProvider
            val model = _uiState.value.selectedModel
            val session = chatRepository.createSession(projectId, "New chat", provider?.id, model?.id)
            _uiState.value = _uiState.value.copy(session = session)
        }
    }
}
