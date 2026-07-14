package com.vibecode.ide.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// "Aurora Terminal" palette — a near-black obsidian workspace lit by a single
// violet -> cyan aurora accent. Inspired by the calm confidence of modern AI
// coding tools (Claude Code, Cursor, Gemini CLI) rather than a generic IDE.
// ============================================================================

// Base surfaces
val VoidBlack = Color(0xFF07070C)          // app background, deepest layer
val SurfaceBase = Color(0xFF0E0E17)        // scaffold background
val SurfaceDeep = Color(0xFF12121C)        // sunken panels (file tree, gutters)
val SurfaceElevated = Color(0xFF181822)    // cards, bubbles, top bars
val SurfaceHighest = Color(0xFF21212E)     // active tab, pressed states, inputs
val SurfaceOverlay = Color(0xFF262635)     // dialogs, sheets
val StrokeSubtle = Color(0x1AFFFFFF)       // hairline borders on dark surfaces
val StrokeMedium = Color(0x33FFFFFF)

// Aurora brand accents
val AuroraViolet = Color(0xFF8B7CF6)
val AuroraIndigo = Color(0xFF6366F1)
val AuroraCyan = Color(0xFF22D3EE)
val AuroraTeal = Color(0xFF2DD4BF)
val AuroraPink = Color(0xFFF472B6)
val AuroraAmber = Color(0xFFFBBF54)
val AuroraRed = Color(0xFFFB7185)
val AuroraGreen = Color(0xFF4ADE80)

// Text
val TextPrimary = Color(0xFFF3F1FF)
val TextSecondary = Color(0xFFACA8C4)
val TextMuted = Color(0xFF716C8C)
val TextOnAccent = Color(0xFF0B0812)

// Semantic
val SuccessGreen = AuroraGreen
val WarningAmber = AuroraAmber
val ErrorRed = AuroraRed
val DiffAddBg = Color(0xFF102A20)
val DiffAddText = Color(0xFF6EE7B7)
val DiffRemoveBg = Color(0xFF2E1420)
val DiffRemoveText = Color(0xFFFB7185)

// Syntax highlighting tokens (tuned to sit on SurfaceDeep)
val SyntaxKeyword = Color(0xFFC4B5FD)
val SyntaxString = Color(0xFF86EFAC)
val SyntaxNumber = Color(0xFFFDBA74)
val SyntaxComment = Color(0xFF645F80)
val SyntaxFunction = Color(0xFF7DD3FC)
val SyntaxType = Color(0xFFFDE68A)
val SyntaxAnnotation = Color(0xFFF9A8D4)
val SyntaxOperator = Color(0xFF67E8F9)
val SyntaxDefault = TextPrimary

// Light theme (secondary, dark-first product)
val LightBackground = Color(0xFFFAFAFD)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F0F9)
val LightSurfaceHighest = Color(0xFFE7E4F5)
val LightTextPrimary = Color(0xFF15131F)
val LightTextSecondary = Color(0xFF5B5674)
val LightStroke = Color(0x14000000)
