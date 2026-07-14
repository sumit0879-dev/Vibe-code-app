package com.vibecode.ide.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibecode.ide.ui.theme.AppThemeMode
import com.vibecode.ide.ui.theme.EditorColorTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "vibecode_settings")

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val editorColorTheme: EditorColorTheme = EditorColorTheme.DARK_PLUS,
    val editorFontSizeSp: Int = 14,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoSave: Boolean = true,
    val tabSize: Int = 4,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EDITOR_THEME = stringPreferencesKey("editor_theme")
        val FONT_SIZE = intPreferencesKey("editor_font_size")
        val LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val TAB_SIZE = intPreferencesKey("tab_size")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.DARK,
            editorColorTheme = prefs[Keys.EDITOR_THEME]?.let { runCatching { EditorColorTheme.valueOf(it) }.getOrNull() } ?: EditorColorTheme.DARK_PLUS,
            editorFontSizeSp = prefs[Keys.FONT_SIZE] ?: 14,
            showLineNumbers = prefs[Keys.LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: false,
            autoSave = prefs[Keys.AUTO_SAVE] ?: true,
            tabSize = prefs[Keys.TAB_SIZE] ?: 4,
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setEditorTheme(theme: EditorColorTheme) = context.dataStore.edit { it[Keys.EDITOR_THEME] = theme.name }
    suspend fun setFontSize(sizeSp: Int) = context.dataStore.edit { it[Keys.FONT_SIZE] = sizeSp }
    suspend fun setShowLineNumbers(show: Boolean) = context.dataStore.edit { it[Keys.LINE_NUMBERS] = show }
    suspend fun setWordWrap(wrap: Boolean) = context.dataStore.edit { it[Keys.WORD_WRAP] = wrap }
    suspend fun setAutoSave(auto: Boolean) = context.dataStore.edit { it[Keys.AUTO_SAVE] = auto }
    suspend fun setTabSize(size: Int) = context.dataStore.edit { it[Keys.TAB_SIZE] = size }
}
