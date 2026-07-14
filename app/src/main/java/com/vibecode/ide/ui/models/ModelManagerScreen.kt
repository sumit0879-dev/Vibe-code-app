package com.vibecode.ide.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibecode.ide.domain.model.AiModel
import com.vibecode.ide.domain.model.ModelSource
import com.vibecode.ide.ui.theme.AuroraAmber
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    providerId: String,
    onBack: () -> Unit,
    viewModel: ModelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val models by viewModel.models.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.provider?.name ?: "Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = viewModel::discoverModels, enabled = !uiState.isDiscovering) {
                        if (uiState.isDiscovering) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Auto-discover")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AuroraViolet,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add model")
            }
        },
    ) { padding ->
        if (models.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Memory, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("No models yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap Auto-discover to fetch models from the provider, or add one manually.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(models, key = { it.id }) { model ->
                    ModelRow(
                        model = model,
                        onFavorite = { viewModel.toggleFavorite(model) },
                        onDelete = { viewModel.deleteModel(model) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddModelDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { id, name, ctx, tools, vision ->
                viewModel.addManualModel(id, name, ctx, tools, vision)
                showAddDialog = false
            },
        )
    }

    uiState.discoveryError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Discovery failed") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun ModelRow(model: AiModel, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onFavorite) {
                Icon(
                    if (model.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (model.isFavorite) AuroraAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(model.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(model.modelId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ctx ${model.contextLength}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (model.source == ModelSource.DISCOVERED) {
                        Text("· discovered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun AddModelDialog(
    onDismiss: () -> Unit,
    onAdd: (modelId: String, displayName: String, contextLength: Int, supportsTools: Boolean, supportsVision: Boolean) -> Unit,
) {
    var modelId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var contextLength by remember { mutableStateOf("8192") }
    var supportsTools by remember { mutableStateOf(false) }
    var supportsVision by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(modelId, { modelId = it }, label = { Text("Model ID (sent to API)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    contextLength, { contextLength = it.filter { c -> c.isDigit() } },
                    label = { Text("Context length") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = supportsTools, onCheckedChange = { supportsTools = it })
                    Text("Supports tool calling")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = supportsVision, onCheckedChange = { supportsVision = it })
                    Text("Supports vision/images")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = modelId.isNotBlank(),
                onClick = { onAdd(modelId, displayName, contextLength.toIntOrNull() ?: 8192, supportsTools, supportsVision) },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
