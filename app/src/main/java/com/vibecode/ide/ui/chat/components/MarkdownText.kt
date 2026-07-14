package com.vibecode.ide.ui.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class CodeBlock(val language: String, val code: String) : MdBlock()
    data class ListBlock(val items: List<String>, val ordered: Boolean) : MdBlock()
}

private fun parseMarkdown(source: String): List<MdBlock> {
    val lines = source.split("\n")
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    val paragraphBuffer = StringBuilder()
    val listBuffer = mutableListOf<String>()
    var listOrdered = false

    fun flushParagraph() {
        if (paragraphBuffer.isNotBlank()) blocks.add(MdBlock.Paragraph(paragraphBuffer.toString().trim()))
        paragraphBuffer.clear()
    }
    fun flushList() {
        if (listBuffer.isNotEmpty()) blocks.add(MdBlock.ListBlock(listBuffer.toList(), listOrdered))
        listBuffer.clear()
    }

    while (i < lines.size) {
        val line = lines[i]
        when {
            line.trim().startsWith("```") -> {
                flushParagraph(); flushList()
                val lang = line.trim().removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i]); i++
                }
                blocks.add(MdBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            }
            line.trim().startsWith("#") -> {
                flushParagraph(); flushList()
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                blocks.add(MdBlock.Heading(level, line.trimStart('#').trim()))
            }
            line.trim().matches(Regex("^[-*]\\s+.*")) -> {
                flushParagraph()
                listOrdered = false
                listBuffer.add(line.trim().removePrefix("-").removePrefix("*").trim())
            }
            line.trim().matches(Regex("^\\d+\\.\\s+.*")) -> {
                flushParagraph()
                listOrdered = true
                listBuffer.add(line.trim().replaceFirst(Regex("^\\d+\\.\\s+"), ""))
            }
            line.isBlank() -> { flushParagraph(); flushList() }
            else -> { flushList(); paragraphBuffer.append(line).append(" ") }
        }
        i++
    }
    flushParagraph(); flushList()
    return blocks
}

/** Renders a (subset of) Markdown: headings, paragraphs with bold/italic/inline-code,
 *  fenced code blocks with a copy button, and bullet/numbered lists. */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.labelLarge
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                is MdBlock.Paragraph -> Text(inlineStyledText(block.text), style = MaterialTheme.typography.bodyMedium)
                is MdBlock.ListBlock -> Column {
                    block.items.forEachIndexed { idx, item ->
                        Row {
                            Text(if (block.ordered) "${idx + 1}. " else "• ", style = MaterialTheme.typography.bodyMedium)
                            Text(inlineStyledText(item), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is MdBlock.CodeBlock -> CodeBlockView(block.language, block.code)
            }
        }
    }
}

@Composable
private fun inlineStyledText(text: String) = buildAnnotatedString {
    var remaining = text
    val boldItalicRegex = Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|`(.+?)`")
    var lastIndex = 0
    boldItalicRegex.findAll(text).forEach { match ->
        append(text.substring(lastIndex, match.range.first))
        when {
            match.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groups[1]!!.value) }
            match.groups[2] != null -> withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(match.groups[2]!!.value) }
            match.groups[3] != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.25f))) { append(match.groups[3]!!.value) }
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) append(text.substring(lastIndex))
}

@Composable
private fun CodeBlockView(language: String, code: String) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                language.ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { copyCode(context, code) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", modifier = Modifier.padding(2.dp))
            }
        }
        Text(
            code,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun copyCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
}
