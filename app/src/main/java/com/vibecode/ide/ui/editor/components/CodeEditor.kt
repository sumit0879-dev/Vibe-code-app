package com.vibecode.ide.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibecode.ide.ui.theme.SyntaxPalette
import com.vibecode.ide.ui.theme.codeTextStyle
import com.vibecode.ide.util.Language
import com.vibecode.ide.util.highlightCode

/**
 * A syntax-highlighted, line-numbered plain text code editor. Highlighting is
 * applied via BasicTextField's `visualTransformation`, so the underlying
 * editable text stays a plain String — cursor/selection behave normally.
 */
@Composable
fun CodeEditor(
    key: String,
    content: String,
    language: Language,
    palette: SyntaxPalette,
    fontSizeSp: Int,
    showLineNumbers: Boolean,
    wordWrap: Boolean,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the file identity (not on content) so that typing doesn't reset the
    // cursor position on every recomposition — only switching files does.
    var fieldValue by remember(key) {
        mutableStateOf(TextFieldValue(content, TextRange(content.length)))
    }
    val lineCount = remember(fieldValue.text) { fieldValue.text.count { it == '\n' } + 1 }
    val textStyle = codeTextStyle(fontSizeSp).copy(color = palette.default)
    val horizontalScroll = rememberScrollState()

    Row(
        modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        if (showLineNumbers) {
            LineNumberGutter(lineCount = lineCount, fontSizeSp = fontSizeSp, palette = palette)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .let { if (!wordWrap) it.horizontalScroll(horizontalScroll) else it }
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = {
                    fieldValue = it
                    onContentChange(it.text)
                },
                textStyle = textStyle,
                cursorBrush = SolidColor(palette.default),
                modifier = if (wordWrap) Modifier.fillMaxSize() else Modifier.widthIn(min = 2000.dp),
                visualTransformation = { text ->
                    TransformedText(highlightCode(text.text, language, palette), OffsetMapping.Identity)
                },
            )
        }
    }
}

@Composable
private fun LineNumberGutter(lineCount: Int, fontSizeSp: Int, palette: SyntaxPalette) {
    Box(
        Modifier
            .widthIn(min = 40.dp)
            .fillMaxHeight()
            .background(palette.background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(
            text = (1..lineCount).joinToString("\n") { it.toString() },
            style = codeTextStyle(fontSizeSp).copy(color = palette.lineNumber, textAlign = TextAlign.End),
        )
    }
}
