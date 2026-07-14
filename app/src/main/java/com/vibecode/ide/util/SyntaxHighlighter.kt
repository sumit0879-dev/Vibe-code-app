package com.vibecode.ide.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.vibecode.ide.ui.theme.SyntaxPalette

enum class Language(val extensions: Set<String>) {
    KOTLIN(setOf("kt", "kts")),
    JAVA(setOf("java")),
    JAVASCRIPT(setOf("js", "jsx", "mjs")),
    TYPESCRIPT(setOf("ts", "tsx")),
    PYTHON(setOf("py")),
    XML(setOf("xml")),
    JSON(setOf("json")),
    HTML(setOf("html", "htm")),
    CSS(setOf("css")),
    MARKDOWN(setOf("md", "markdown")),
    GRADLE(setOf("gradle")),
    YAML(setOf("yaml", "yml")),
    SHELL(setOf("sh", "bash")),
    PLAIN(emptySet());

    companion object {
        fun fromFileName(name: String): Language {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { ext in it.extensions } ?: PLAIN
        }
    }
}

private val KEYWORDS: Map<Language, Set<String>> = mapOf(
    Language.KOTLIN to setOf(
        "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while",
        "do", "return", "package", "import", "private", "public", "protected", "internal", "override",
        "companion", "init", "constructor", "this", "super", "null", "true", "false", "is", "as",
        "in", "out", "try", "catch", "finally", "throw", "suspend", "inline", "reified", "sealed",
        "data", "enum", "abstract", "open", "lateinit", "by", "get", "set", "typealias", "vararg",
    ),
    Language.JAVA to setOf(
        "public", "private", "protected", "class", "interface", "extends", "implements", "static",
        "final", "void", "int", "long", "double", "float", "boolean", "char", "byte", "short",
        "new", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
        "try", "catch", "finally", "throw", "throws", "import", "package", "this", "super", "null",
        "true", "false", "abstract", "synchronized", "volatile", "enum", "instanceof",
    ),
    Language.JAVASCRIPT to setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "do", "return", "class",
        "extends", "new", "this", "super", "import", "export", "default", "from", "async", "await",
        "try", "catch", "finally", "throw", "typeof", "instanceof", "null", "undefined", "true", "false",
        "switch", "case", "break", "continue", "yield", "static", "get", "set",
    ),
    Language.TYPESCRIPT to setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "do", "return", "class",
        "extends", "implements", "interface", "type", "new", "this", "super", "import", "export",
        "default", "from", "async", "await", "try", "catch", "finally", "throw", "typeof", "as",
        "null", "undefined", "true", "false", "switch", "case", "break", "continue", "enum", "public",
        "private", "protected", "readonly", "static",
    ),
    Language.PYTHON to setOf(
        "def", "class", "if", "elif", "else", "for", "while", "return", "import", "from", "as",
        "try", "except", "finally", "raise", "with", "lambda", "None", "True", "False", "and", "or",
        "not", "in", "is", "pass", "break", "continue", "yield", "async", "await", "global", "nonlocal",
        "self",
    ),
    Language.SHELL to setOf(
        "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac", "function",
        "echo", "export", "local", "return",
    ),
)

private val COMMENT_REGEX: Map<Language, Regex> = mapOf(
    Language.KOTLIN to Regex("//.*|/\\*[\\s\\S]*?\\*/"),
    Language.JAVA to Regex("//.*|/\\*[\\s\\S]*?\\*/"),
    Language.JAVASCRIPT to Regex("//.*|/\\*[\\s\\S]*?\\*/"),
    Language.TYPESCRIPT to Regex("//.*|/\\*[\\s\\S]*?\\*/"),
    Language.PYTHON to Regex("#.*"),
    Language.SHELL to Regex("#.*"),
    Language.GRADLE to Regex("//.*|/\\*[\\s\\S]*?\\*/"),
    Language.XML to Regex("<!--[\\s\\S]*?-->"),
    Language.HTML to Regex("<!--[\\s\\S]*?-->"),
    Language.YAML to Regex("#.*"),
)

private val STRING_REGEX = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
private val NUMBER_REGEX = Regex("\\b\\d+(\\.\\d+)?[fFlLdD]?\\b")
private val ANNOTATION_REGEX = Regex("@\\w+")
private val FUNCTION_CALL_REGEX = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()")
private val TYPE_REGEX = Regex("\\b[A-Z][a-zA-Z0-9_]*\\b")

/**
 * Tokenizes [code] for [language] and renders it as an AnnotatedString using
 * the given [palette]. Deliberately regex-based (not a full parser) — fast
 * enough to run on every keystroke on a phone CPU while still giving useful
 * highlighting for 10+ common languages.
 */
fun highlightCode(code: String, language: Language, palette: SyntaxPalette): AnnotatedString = buildAnnotatedString {
    append(code)
    if (language == Language.PLAIN || code.isEmpty()) return@buildAnnotatedString

    val claimed = BooleanArray(code.length)

    fun applyStyle(range: IntRange, style: SpanStyle) {
        if (range.first < 0 || range.last >= code.length) return
        for (idx in range) if (claimed[idx]) return
        addStyle(style, range.first, range.last + 1)
        for (idx in range) claimed[idx] = true
    }

    // Comments first (highest priority — nothing inside should be re-colored)
    COMMENT_REGEX[language]?.findAll(code)?.forEach { m ->
        applyStyle(m.range, SpanStyle(color = palette.comment))
    }
    // Strings
    STRING_REGEX.findAll(code).forEach { m ->
        applyStyle(m.range, SpanStyle(color = palette.string))
    }
    // Annotations / decorators
    ANNOTATION_REGEX.findAll(code).forEach { m ->
        applyStyle(m.range, SpanStyle(color = palette.annotation))
    }
    // Numbers
    NUMBER_REGEX.findAll(code).forEach { m ->
        applyStyle(m.range, SpanStyle(color = palette.number))
    }
    // Keywords
    KEYWORDS[language]?.let { keywords ->
        Regex("\\b(${keywords.joinToString("|")})\\b").findAll(code).forEach { m ->
            applyStyle(m.range, SpanStyle(color = palette.keyword, fontWeight = FontWeight.Medium))
        }
    }
    // Types (PascalCase identifiers)
    if (language in setOf(Language.KOTLIN, Language.JAVA, Language.TYPESCRIPT)) {
        TYPE_REGEX.findAll(code).forEach { m ->
            applyStyle(m.range, SpanStyle(color = palette.type))
        }
    }
    // Function calls
    FUNCTION_CALL_REGEX.findAll(code).forEach { m ->
        applyStyle(m.groups[1]!!.range, SpanStyle(color = palette.function))
    }
}
