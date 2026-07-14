package com.vibecode.ide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VibeDarkColorScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = TextOnAccent,
    primaryContainer = SurfaceHighest,
    onPrimaryContainer = AuroraViolet,
    secondary = AuroraCyan,
    onSecondary = TextOnAccent,
    tertiary = AuroraTeal,
    onTertiary = TextOnAccent,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHighest,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceDeep,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceOverlay,
    error = ErrorRed,
    onError = TextOnAccent,
    errorContainer = DiffRemoveBg,
    onErrorContainer = ErrorRed,
    outline = StrokeMedium,
    outlineVariant = StrokeSubtle,
)

private val VibeLightColorScheme = lightColorScheme(
    primary = AuroraIndigo,
    onPrimary = LightSurface,
    secondary = AuroraCyan,
    onSecondary = LightSurface,
    tertiary = AuroraTeal,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceHighest,
    surfaceContainerHighest = LightSurfaceHighest,
    error = ErrorRed,
    outline = LightStroke,
)

/** Rounder, calmer corners than Material defaults — matches the "soft glass" surfaces. */
val VibeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class AppThemeMode { DARK, LIGHT, SYSTEM }

@Composable
fun VibeCodeAITheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) VibeDarkColorScheme else VibeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = VibeShapes,
        content = content,
    )
}

/** Named editor color themes selectable independently of the app chrome theme. */
enum class EditorColorTheme(val displayName: String) {
    DARK_PLUS("Aurora Dark"),
    LIGHT("Light"),
    MONOKAI("Monokai"),
    DRACULA("Dracula"),
    SOLARIZED("Solarized"),
}

data class SyntaxPalette(
    val background: androidx.compose.ui.graphics.Color,
    val keyword: androidx.compose.ui.graphics.Color,
    val string: androidx.compose.ui.graphics.Color,
    val number: androidx.compose.ui.graphics.Color,
    val comment: androidx.compose.ui.graphics.Color,
    val function: androidx.compose.ui.graphics.Color,
    val type: androidx.compose.ui.graphics.Color,
    val annotation: androidx.compose.ui.graphics.Color,
    val operator: androidx.compose.ui.graphics.Color,
    val default: androidx.compose.ui.graphics.Color,
    val lineNumber: androidx.compose.ui.graphics.Color,
    val selection: androidx.compose.ui.graphics.Color,
)

fun paletteFor(theme: EditorColorTheme): SyntaxPalette = when (theme) {
    EditorColorTheme.DARK_PLUS -> SyntaxPalette(
        background = SurfaceDeep, keyword = SyntaxKeyword, string = SyntaxString,
        number = SyntaxNumber, comment = SyntaxComment, function = SyntaxFunction,
        type = SyntaxType, annotation = SyntaxAnnotation, operator = SyntaxOperator,
        default = SyntaxDefault, lineNumber = TextMuted, selection = AuroraViolet.copy(alpha = 0.25f),
    )
    EditorColorTheme.LIGHT -> SyntaxPalette(
        background = LightBackground, keyword = androidx.compose.ui.graphics.Color(0xFF7C3AED),
        string = androidx.compose.ui.graphics.Color(0xFF15803D), number = androidx.compose.ui.graphics.Color(0xFFB45309),
        comment = androidx.compose.ui.graphics.Color(0xFF94A3B8), function = androidx.compose.ui.graphics.Color(0xFF2563EB),
        type = androidx.compose.ui.graphics.Color(0xFFB45309), annotation = androidx.compose.ui.graphics.Color(0xFFDB2777),
        operator = androidx.compose.ui.graphics.Color(0xFF0891B2), default = LightTextPrimary,
        lineNumber = androidx.compose.ui.graphics.Color(0xFF94A3B8), selection = AuroraIndigo.copy(alpha = 0.18f),
    )
    EditorColorTheme.MONOKAI -> SyntaxPalette(
        background = androidx.compose.ui.graphics.Color(0xFF272822), keyword = androidx.compose.ui.graphics.Color(0xFFF92672),
        string = androidx.compose.ui.graphics.Color(0xFFE6DB74), number = androidx.compose.ui.graphics.Color(0xFFAE81FF),
        comment = androidx.compose.ui.graphics.Color(0xFF75715E), function = androidx.compose.ui.graphics.Color(0xFFA6E22E),
        type = androidx.compose.ui.graphics.Color(0xFF66D9EF), annotation = androidx.compose.ui.graphics.Color(0xFFFD971F),
        operator = androidx.compose.ui.graphics.Color(0xFFF92672), default = androidx.compose.ui.graphics.Color(0xFFF8F8F2),
        lineNumber = androidx.compose.ui.graphics.Color(0xFF75715E), selection = androidx.compose.ui.graphics.Color(0xFF49483E),
    )
    EditorColorTheme.DRACULA -> SyntaxPalette(
        background = androidx.compose.ui.graphics.Color(0xFF282A36), keyword = androidx.compose.ui.graphics.Color(0xFFFF79C6),
        string = androidx.compose.ui.graphics.Color(0xFFF1FA8C), number = androidx.compose.ui.graphics.Color(0xFFBD93F9),
        comment = androidx.compose.ui.graphics.Color(0xFF6272A4), function = androidx.compose.ui.graphics.Color(0xFF50FA7B),
        type = androidx.compose.ui.graphics.Color(0xFF8BE9FD), annotation = androidx.compose.ui.graphics.Color(0xFFFFB86C),
        operator = androidx.compose.ui.graphics.Color(0xFFFF79C6), default = androidx.compose.ui.graphics.Color(0xFFF8F8F2),
        lineNumber = androidx.compose.ui.graphics.Color(0xFF6272A4), selection = androidx.compose.ui.graphics.Color(0xFF44475A),
    )
    EditorColorTheme.SOLARIZED -> SyntaxPalette(
        background = androidx.compose.ui.graphics.Color(0xFF002B36), keyword = androidx.compose.ui.graphics.Color(0xFF859900),
        string = androidx.compose.ui.graphics.Color(0xFF2AA198), number = androidx.compose.ui.graphics.Color(0xFFD33682),
        comment = androidx.compose.ui.graphics.Color(0xFF586E75), function = androidx.compose.ui.graphics.Color(0xFF268BD2),
        type = androidx.compose.ui.graphics.Color(0xFFB58900), annotation = androidx.compose.ui.graphics.Color(0xFFCB4B16),
        operator = androidx.compose.ui.graphics.Color(0xFF6C71C4), default = androidx.compose.ui.graphics.Color(0xFF839496),
        lineNumber = androidx.compose.ui.graphics.Color(0xFF586E75), selection = androidx.compose.ui.graphics.Color(0xFF073642),
    )
}
