package com.vibecode.ide.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibecode.ide.ui.chat.ChatViewModel
import com.vibecode.ide.ui.chat.components.ChatPanel
import com.vibecode.ide.ui.chat.components.DiffApprovalDialog
import com.vibecode.ide.ui.editor.components.CodeEditor
import com.vibecode.ide.ui.editor.components.EditorTabBar
import com.vibecode.ide.ui.editor.components.FileTreeView
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.EditorColorTheme
import com.vibecode.ide.ui.theme.SurfaceDeep
import com.vibecode.ide.ui.theme.SurfaceElevated
import com.vibecode.ide.ui.theme.TextMuted
import com.vibecode.ide.ui.theme.VoidBlack
import com.vibecode.ide.ui.theme.paletteFor
import com.vibecode.ide.util.Language
import kotlinx.coroutines.launch

private const val PAGE_CODE = 0
private const val PAGE_CHAT = 1

/**
 * Deliberately not a drawer-plus-bottom-nav shell. The file tree lives in a
 * bottom sheet you summon on demand, Code/Chat are two pages you swipe
 * between (or jump via the floating pill), and a floating assistant bubble
 * is always reachable from the code page — closer to a command-driven IDE
 * than a stock multi-tab app.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenModels: (String) -> Unit,
    editorViewModel: EditorViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
) {
    val editorState by editorViewModel.uiState.collectAsState()
    val chatState by chatViewModel.uiState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    val pagerState = rememberPagerState(initialPage = PAGE_CODE) { 2 }
    val scope = rememberCoroutineScope()
    var showFileTree by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun goTo(page: Int) = scope.launch { pagerState.animateScrollToPage(page) }

    Scaffold(
        containerColor = VoidBlack,
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    Text(
                        editorState.project?.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    val activeTab = editorState.openTabs.getOrNull(editorState.activeTabIndex)
                    if (pagerState.currentPage == PAGE_CODE && activeTab?.isDirty == true) {
                        IconButton(onClick = editorViewModel::saveActiveTab) {
                            Icon(Icons.Filled.Save, contentDescription = "Save", tint = AuroraCyan)
                        }
                    }
                    IconButton(onClick = { showFileTree = true }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Files", tint = TextMuted)
                    }
                }
                DestinationPills(
                    page = pagerState.currentPage,
                    isAiBusy = chatState.isSending,
                    onSelect = ::goTo,
                )
            }
        },
        floatingActionButton = {
            if (pagerState.currentPage == PAGE_CODE) {
                FloatingActionButton(
                    onClick = { goTo(PAGE_CHAT) },
                    containerColor = AuroraViolet,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Ask AI")
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { page ->
            when (page) {
                PAGE_CHAT -> ChatPanel(
                    messages = messages,
                    providers = chatState.providers,
                    models = chatState.models,
                    selectedProvider = chatState.selectedProvider,
                    selectedModel = chatState.selectedModel,
                    isSending = chatState.isSending,
                    onSelectProvider = chatViewModel::selectProvider,
                    onSelectModel = chatViewModel::selectModel,
                    onSend = chatViewModel::sendMessage,
                    onStop = chatViewModel::stopStreaming,
                    onRevertToolCall = chatViewModel::revertToolCall,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> EditorArea(editorState, editorViewModel)
            }
        }
    }

    if (showFileTree) {
        FileTreeSheet(
            sheetState = sheetState,
            projectName = editorState.project?.name ?: "Project",
            editorState = editorState,
            editorViewModel = editorViewModel,
            onDismiss = { showFileTree = false },
            onFileOpened = {
                showFileTree = false
                scope.launch { pagerState.scrollToPage(PAGE_CODE) }
            },
        )
    }

    chatState.pendingChange?.let { pending ->
        DiffApprovalDialog(
            change = pending,
            onApprove = {
                chatViewModel.approvePendingChange()
                val fileName = pending.path.substringAfterLast('/')
                if (pending.newContent != null) {
                    editorViewModel.openOrRefreshFileWithContent(pending.path, fileName, pending.newContent)
                } else {
                    editorViewModel.refreshTree()
                }
            },
            onReject = chatViewModel::rejectPendingChange,
            onDismiss = chatViewModel::dismissPendingChangePreviewOnly,
        )
    }

    editorState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = editorViewModel::clearError,
            confirmButton = { TextButton(onClick = editorViewModel::clearError) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(message) },
        )
    }

    chatState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = chatViewModel::clearError,
            confirmButton = { TextButton(onClick = chatViewModel::clearError) { Text("OK") } },
            title = { Text("AI request failed") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun DestinationPills(page: Int, isAiBusy: Boolean, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        Pill("Code", Icons.Filled.Code, page == PAGE_CODE, AuroraCyan, false, Modifier.weight(1f)) { onSelect(PAGE_CODE) }
        Pill("Chat", Icons.Filled.AutoAwesome, page == PAGE_CHAT, AuroraViolet, isAiBusy, Modifier.weight(1f)) { onSelect(PAGE_CHAT) }
    }
}

@Composable
private fun Pill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    busy: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.16f) else SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) accent else TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) accent else TextMuted)
        if (busy) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(AuroraViolet))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTreeSheet(
    sheetState: SheetState,
    projectName: String,
    editorState: EditorUiState,
    editorViewModel: EditorViewModel,
    onDismiss: () -> Unit,
    onFileOpened: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDeep,
    ) {
        Text(
            projectName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 8.dp),
        )
        HorizontalDivider()
        FileTreeView(
            root = editorState.rootNode,
            expandedChildren = editorState.expandedChildren,
            onToggleDirectory = editorViewModel::toggleDirectory,
            onOpenFile = {
                editorViewModel.openFile(it)
                onFileOpened()
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun EditorArea(state: EditorUiState, viewModel: EditorViewModel) {
    Column(Modifier.fillMaxSize()) {
        EditorTabBar(
            tabs = state.openTabs,
            activeIndex = state.activeTabIndex,
            onSelect = viewModel::selectTab,
            onClose = viewModel::closeTab,
        )
        val activeTab = state.openTabs.getOrNull(state.activeTabIndex)
        if (activeTab == null) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "Open a file from the folder icon above to start editing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            val palette = paletteFor(EditorColorTheme.DARK_PLUS)
            CodeEditor(
                key = activeTab.path + ":" + activeTab.version,
                content = activeTab.content,
                language = Language.fromFileName(activeTab.name),
                palette = palette,
                fontSizeSp = 14,
                showLineNumbers = true,
                wordWrap = false,
                onContentChange = viewModel::updateActiveTabContent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
