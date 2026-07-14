package com.vibecode.ide.ui.editor.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibecode.ide.domain.model.FileNode
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.Motion
import com.vibecode.ide.ui.theme.TextMuted

@Composable
fun FileTreeView(
    root: FileNode?,
    expandedChildren: Map<String, List<FileNode>>,
    onToggleDirectory: (FileNode) -> Unit,
    onOpenFile: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (root == null) return
    LazyColumn(modifier) {
        val topLevel = root.children.orEmpty()
        items(topLevel, key = { it.path }) { node ->
            TreeNodeRow(
                node = node, depth = 0,
                expandedChildren = expandedChildren,
                onToggleDirectory = onToggleDirectory,
                onOpenFile = onOpenFile,
            )
        }
    }
}

@Composable
private fun TreeNodeRow(
    node: FileNode,
    depth: Int,
    expandedChildren: Map<String, List<FileNode>>,
    onToggleDirectory: (FileNode) -> Unit,
    onOpenFile: (FileNode) -> Unit,
) {
    val isExpanded = expandedChildren.containsKey(node.path)
    val rotation by animateFloatAsState(if (isExpanded) 90f else 0f, animationSpec = Motion.responsive(), label = "chevron")

    Column(Modifier.animateContentSize(Motion.panel())) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { if (node.isDirectory) onToggleDirectory(node) else onOpenFile(node) }
                .padding(start = (10 + depth * 14).dp, top = 8.dp, bottom = 8.dp, end = 12.dp),
        ) {
            if (node.isDirectory) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(rotation),
                    tint = TextMuted,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (isExpanded) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                    contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = AuroraViolet,
                )
            } else {
                Spacer(Modifier.width(22.dp))
                Icon(
                    iconForFile(node.name), contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = iconTintForFile(node.name),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                node.name, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (node.isDirectory && isExpanded) {
            val children = expandedChildren[node.path].orEmpty()
            children.forEach { child ->
                TreeNodeRow(child, depth + 1, expandedChildren, onToggleDirectory, onOpenFile)
            }
        }
    }
}

private fun iconForFile(name: String): ImageVector {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts", "java" -> Icons.Filled.Code
        "xml" -> Icons.Filled.Html
        "json" -> Icons.Filled.DataObject
        "md" -> Icons.AutoMirrored.Filled.Article
        "png", "jpg", "jpeg", "webp", "svg" -> Icons.Filled.Image
        "gradle" -> Icons.Filled.Build
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun iconTintForFile(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
    "kt", "kts", "java" -> AuroraCyan
    "png", "jpg", "jpeg", "webp", "svg" -> androidx.compose.ui.graphics.Color(0xFFFBBF54)
    else -> TextMuted
}
