package com.vibecode.ide.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecode.ide.domain.diff.DiffLine
import com.vibecode.ide.domain.diff.DiffLineType
import com.vibecode.ide.domain.tools.ToolName
import com.vibecode.ide.ui.chat.PendingChange
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.DiffAddBg
import com.vibecode.ide.ui.theme.DiffAddText
import com.vibecode.ide.ui.theme.DiffRemoveBg
import com.vibecode.ide.ui.theme.DiffRemoveText
import com.vibecode.ide.ui.theme.ErrorRed
import com.vibecode.ide.ui.theme.SurfaceDeep

@Composable
fun DiffApprovalDialog(
    change: PendingChange,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint) = when (change.tool) {
                    ToolName.CREATE_FILE -> Icons.Filled.AddCircle to AuroraCyan
                    ToolName.DELETE_FILE -> Icons.Filled.DeleteForever to ErrorRed
                    else -> Icons.Filled.Edit to AuroraViolet
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    when (change.tool) {
                        ToolName.CREATE_FILE -> "Create file"
                        ToolName.DELETE_FILE -> "Delete file"
                        else -> "Update file"
                    },
                )
            }
        },
        text = {
            Column {
                Text(
                    change.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDeep)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Spacer(Modifier.width(0.dp))
                if (change.tool == ToolName.DELETE_FILE) {
                    Text(
                        "This will permanently delete the file. This cannot be undone from within the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                } else {
                    DiffLinesList(change.diff, Modifier.padding(top = 10.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onApprove) { Text("Approve", color = AuroraCyan) } },
        dismissButton = { TextButton(onClick = onReject) { Text("Reject", color = ErrorRed) } },
    )
}

/** Shared diff rendering used by both the pending-approval dialog and the after-the-fact review dialog. */
@Composable
private fun DiffLinesList(diff: List<DiffLine>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier
            .heightIn(max = 360.dp)
            .clip(RoundedCornerShape(10.dp)),
    ) {
        items(diff) { line ->
            val (bg, fg) = when (line.type) {
                DiffLineType.ADDED -> DiffAddBg to DiffAddText
                DiffLineType.REMOVED -> DiffRemoveBg to DiffRemoveText
                DiffLineType.EQUAL -> SurfaceDeep to MaterialTheme.colorScheme.onSurfaceVariant
            }
            val prefix = when (line.type) {
                DiffLineType.ADDED -> "+ "
                DiffLineType.REMOVED -> "− "
                DiffLineType.EQUAL -> "  "
            }
            Text(
                prefix + line.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = fg,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
    }
}

/**
 * Read-only look at a change that's already been decided (approved, rejected, or reverted) —
 * opened by tapping a [com.vibecode.ide.ui.chat.components.ToolCallChip] in the transcript.
 * Unlike [DiffApprovalDialog] this never touches disk except through the optional Revert action.
 */
@Composable
fun ChangeReviewDialog(
    tool: ToolName,
    path: String,
    diff: List<DiffLine>,
    resultSummary: String?,
    canRevert: Boolean,
    onRevert: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint) = when (tool) {
                    ToolName.CREATE_FILE -> Icons.Filled.AddCircle to AuroraCyan
                    ToolName.DELETE_FILE -> Icons.Filled.DeleteForever to ErrorRed
                    else -> Icons.Filled.Edit to AuroraViolet
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    when (tool) {
                        ToolName.CREATE_FILE -> "Created file"
                        ToolName.DELETE_FILE -> "Deleted file"
                        else -> "Updated file"
                    },
                )
            }
        },
        text = {
            Column {
                Text(
                    path,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDeep)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                resultSummary?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (tool == ToolName.DELETE_FILE) {
                    Text(
                        "The file's last known content, shown as fully removed:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                DiffLinesList(diff, Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            if (canRevert) {
                TextButton(onClick = onRevert) { Text("Revert", color = ErrorRed) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
