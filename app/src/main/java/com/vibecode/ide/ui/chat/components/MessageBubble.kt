package com.vibecode.ide.ui.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibecode.ide.domain.diff.DiffUtil
import com.vibecode.ide.domain.model.ChatMessage
import com.vibecode.ide.domain.model.MessageRole
import com.vibecode.ide.domain.model.ToolCallRecord
import com.vibecode.ide.domain.tools.ToolName
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.ErrorRed
import com.vibecode.ide.ui.theme.SurfaceDeep
import com.vibecode.ide.ui.theme.SurfaceElevated
import com.vibecode.ide.ui.theme.TextMuted

/**
 * No chat-bubble chrome for the assistant — it's rendered as a labelled,
 * full-width transcript block with a thin left rail (terminal/CLI feel).
 * Only the user's own turn gets a compact right-aligned pill, so the two
 * roles read very differently at a glance instead of looking like a
 * generic two-tone messaging app.
 */
@Composable
fun MessageBubble(message: ChatMessage, onRevertToolCall: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    when (message.role) {
        MessageRole.USER -> UserLine(message, modifier)
        MessageRole.ASSISTANT -> AssistantBlock(message, onRevertToolCall, modifier)
        MessageRole.TOOL -> ToolResultBlock(message, modifier)
        MessageRole.SYSTEM -> Unit // system prompts aren't shown in the chat feed
    }
}

@Composable
private fun UserLine(message: ChatMessage, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(50))
                .background(AuroraViolet.copy(alpha = 0.18f))
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Text(message.content, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssistantBlock(message: ChatMessage, onRevertToolCall: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .padding(start = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(AuroraViolet, AuroraCyan)))
                .then(Modifier),
        )
        Column(Modifier.padding(start = 14.dp, end = 20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AuroraViolet, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "VibeCode AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AuroraViolet,
                )
            }
            Spacer(Modifier.padding(top = 6.dp))
            if (message.isError) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 6.dp))
                    Text(message.content, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (message.content.isBlank() && message.isStreaming) {
                TypingIndicator()
            } else {
                MarkdownText(markdown = message.content)
                if (message.isStreaming) {
                    Spacer(Modifier.padding(top = 4.dp))
                    StreamingCursor()
                }
            }
            if (!message.isStreaming && message.content.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { copyToClipboard(context, message.content) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = TextMuted)
                    }
                }
            }
            message.toolCalls.firstOrNull()?.let { record ->
                val tool = ToolName.fromId(record.toolName)
                if (tool != null && tool.mutatesFiles) {
                    Spacer(Modifier.padding(top = 2.dp))
                    ToolCallChip(record = record, tool = tool, onRevert = { onRevertToolCall(message.id) })
                }
            }
        }
    }
}

private enum class ChipStatus(val label: String, val color: Color) {
    PENDING("Awaiting approval", TextMuted),
    APPLIED("Applied", AuroraCyan),
    FAILED("Failed", ErrorRed),
    REVERTED("Reverted", TextMuted),
    REJECTED("Rejected", ErrorRed),
}

private fun statusOf(record: ToolCallRecord): ChipStatus = when {
    record.approved == null -> ChipStatus.PENDING
    record.approved == false -> ChipStatus.REJECTED
    record.revertedAt != null -> ChipStatus.REVERTED
    record.resultSummary?.startsWith("Failed") == true -> ChipStatus.FAILED
    else -> ChipStatus.APPLIED
}

/**
 * Persistent record of a proposed file change, shown inline in the transcript after the pending
 * dialog has been resolved — this is what lets you scroll back and see (or revert) what an
 * earlier message actually did, instead of that information disappearing once the dialog closes.
 */
@Composable
private fun ToolCallChip(record: ToolCallRecord, tool: ToolName, onRevert: () -> Unit) {
    var showReview by remember(record) { mutableStateOf(false) }
    val status = statusOf(record)
    val (icon, iconTint) = when (tool) {
        ToolName.CREATE_FILE -> Icons.Filled.AddCircle to AuroraCyan
        ToolName.DELETE_FILE -> Icons.Filled.DeleteForever to ErrorRed
        else -> Icons.Filled.Edit to AuroraViolet
    }
    val fileName = record.path?.substringAfterLast('/') ?: "file"
    val clickable = status != ChipStatus.PENDING

    Row(
        Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .then(if (clickable) Modifier.clickable { showReview = true } else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint.copy(alpha = if (status == ChipStatus.PENDING) 0.5f else 1f), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            fileName,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(status.label, style = MaterialTheme.typography.labelSmall, color = status.color)
    }

    if (showReview) {
        val diff = remember(record) { DiffUtil.diff(record.oldContent.orEmpty(), record.newContent.orEmpty()) }
        ChangeReviewDialog(
            tool = tool,
            path = record.path.orEmpty(),
            diff = diff,
            resultSummary = record.resultSummary,
            canRevert = status == ChipStatus.APPLIED,
            onRevert = { onRevert(); showReview = false },
            onDismiss = { showReview = false },
        )
    }
}

@Composable
private fun ToolResultBlock(message: ChatMessage, modifier: Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 33.dp, end = 20.dp, top = 2.dp, bottom = 6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDeep)
                .padding(10.dp),
        ) {
            Text(
                message.content.take(600),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val delay = index * 150
            val alpha by transition.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(AuroraViolet.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun StreamingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Box(Modifier.size(width = 6.dp, height = 14.dp).background(AuroraCyan.copy(alpha = alpha), RoundedCornerShape(2.dp)))
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("VibeCode AI response", text))
}
