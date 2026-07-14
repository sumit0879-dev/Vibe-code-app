package com.vibecode.ide.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibecode.ide.ui.theme.AppThemeMode
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.EditorColorTheme
import com.vibecode.ide.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val backupState by viewModel.backupState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(title = "AI") {
                ListItem(
                    headlineContent = { Text("Providers & Models") },
                    supportingContent = { Text("Configure AI backends, endpoints, and API keys") },
                    modifier = Modifier.clickableRow(onOpenProviders),
                )
            }

            SettingsSection(title = "Appearance") {
                Text("App theme", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.name) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Editor color theme", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorColorTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.editorColorTheme == theme,
                            onClick = { viewModel.setEditorTheme(theme) },
                            label = { Text(theme.displayName) },
                        )
                    }
                }
            }

            SettingsSection(title = "Editor") {
                Text("Font size: ${settings.editorFontSizeSp}sp", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = settings.editorFontSizeSp.toFloat(),
                    onValueChange = { viewModel.setFontSize(it.toInt()) },
                    valueRange = 10f..24f, steps = 13,
                )
                SettingsToggleRow("Show line numbers", settings.showLineNumbers, viewModel::setShowLineNumbers)
                SettingsToggleRow("Word wrap", settings.wordWrap, viewModel::setWordWrap)
                SettingsToggleRow("Auto-save", settings.autoSave, viewModel::setAutoSave)
                Text("Tab size: ${settings.tabSize}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = settings.tabSize.toFloat(),
                    onValueChange = { viewModel.setTabSize(it.toInt()) },
                    valueRange = 2f..8f, steps = 5,
                )
            }

            SettingsSection(title = "Backup & Restore") {
                Text(
                    "Export providers (without API keys), models, and preferences to a JSON " +
                        "file under Android/data/com.vibecode.ide/files/backups/. Import restores them " +
                        "— API keys must be re-entered afterward for security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::exportBackup) { Text("Export") }
                    OutlinedButton(onClick = viewModel::importBackup) { Text("Import") }
                }
            }

            Text(
                "VibeCode AI IDE · v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    backupState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            confirmButton = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } },
            title = { Text("Backup") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = AuroraViolet)
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
