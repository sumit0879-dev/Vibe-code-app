package com.vibecode.ide.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibecode.ide.domain.model.AiModel
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.ChatMessage
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.SurfaceDeep
import com.vibecode.ide.ui.theme.SurfaceElevated
import com.vibecode.ide.ui.theme.TextMuted
import com.vibecode.ide.ui.theme.VoidBlack

private val suggestedPrompts = listOf(
    "Explain what this project does",
    "Find and fix any obvious bugs",
    "Add a dark mode toggle",
    "Write unit tests for this file",
)

/**
 * Chat is a full-screen destination now, styled as a terminal-style transcript
 * (left-aligned, no bubble chrome for the assistant) rather than a generic
 * messaging-app thread — closer to how Claude Code / Gemini CLI render output.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    providers: List<AiProvider>,
    models: List<AiModel>,
    selectedProvider: AiProvider?,
    selectedModel: AiModel?,
    isSending: Boolean,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (AiModel) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRevertToolCall: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize().background(VoidBlack)) {
        ModelBar(
            providers = providers,
            models = models,
            selectedProvider = selectedProvider,
            selectedModel = selectedModel,
            onSelectProvider = onSelectProvider,
            onSelectModel = onSelectModel,
        )

        if (messages.isEmpty()) {
            ChatEmptyState(Modifier.weight(1f), onSend = onSend)
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    var visible by remember(message.id) { mutableStateOf(false) }
                    LaunchedEffect(message.id) { visible = true }
                    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200))) {
                        MessageBubble(message, onRevertToolCall = onRevertToolCall)
                    }
                }
            }
        }

        ComposerBar(input = input, onInputChange = { input = it }, isSending = isSending, onSend = onSend, onStop = onStop)
    }
}

@Composable
private fun ModelBar(
    providers: List<AiProvider>,
    models: List<AiModel>,
    selectedProvider: AiProvider?,
    selectedModel: AiModel?,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (AiModel) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceDeep).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(AuroraCyan))
        Spacer(Modifier.width(8.dp))
        SelectorText(
            label = selectedProvider?.name ?: "Select provider",
            items = providers.map { it.name },
            onSelect = { index -> onSelectProvider(providers[index]) },
            emptyLabel = "No providers — add one first",
        )
        Text(" / ", color = TextMuted, style = MaterialTheme.typography.labelLarge)
        SelectorText(
            label = selectedModel?.displayName ?: "Select model",
            items = models.map { it.displayName },
            onSelect = { index -> onSelectModel(models[index]) },
            emptyLabel = "No models — add one first",
        )
    }
}

@Composable
private fun SelectorText(label: String, items: List<String>, onSelect: (Int) -> Unit, emptyLabel: String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (items.isEmpty()) {
                DropdownMenuItem(text = { Text(emptyLabel) }, onClick = { expanded = false })
            }
            items.forEachIndexed { index, text ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}

@Composable
private fun ChatEmptyState(modifier: Modifier, onSend: (String) -> Unit) {
    Column(
        modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(AuroraViolet, AuroraCyan))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.padding(top = 16.dp))
        Text("Ask VibeCode AI anything", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            "It can read, explain, refactor, and propose file changes. You approve every edit before it touches disk.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.padding(top = 20.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedPrompts) { prompt ->
                SuggestionChip(prompt) { onSend(prompt) }
            }
        }
    }
}

@Composable
private fun ComposerBar(
    input: String,
    onInputChange: (String) -> Unit,
    isSending: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceDeep).padding(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message VibeCode AI…") },
            maxLines = 5,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = AuroraViolet,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.padding(start = 8.dp))
        if (isSending) {
            FilledIconButton(
                onClick = onStop,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Icon(Icons.Filled.Stop, contentDescription = "Stop") }
        } else {
            FilledIconButton(
                onClick = { if (input.isNotBlank()) { onSend(input); onInputChange("") } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AuroraViolet),
            ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Send") }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
