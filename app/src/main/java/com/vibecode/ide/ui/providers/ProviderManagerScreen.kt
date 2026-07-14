package com.vibecode.ide.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.AuthMethod
import com.vibecode.ide.domain.model.RequestFormat
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderManagerScreen(
    onBack: () -> Unit,
    onOpenModels: (String) -> Unit,
    viewModel: ProviderViewModel = hiltViewModel(),
) {
    val providers by viewModel.providers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var editingForm by remember { mutableStateOf<ProviderFormState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Providers") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingForm = ProviderFormState() },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Provider") },
                containerColor = AuroraViolet,
                contentColor = androidx.compose.ui.graphics.Color.White,
            )
        },
    ) { padding ->
        if (providers.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CloudOff, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("No providers configured", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Add any OpenAI-compatible or custom API — OpenAI, OpenRouter, Groq, "
                        + "a self-hosted endpoint, or anything else.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(providers, key = { it.id }) { provider ->
                    ProviderCard(
                        provider = provider,
                        onEdit = {
                            editingForm = ProviderFormState(
                                id = provider.id, name = provider.name, baseUrl = provider.baseUrl,
                                chatCompletionsPath = provider.chatCompletionsPath,
                                modelsListPath = provider.modelsListPath,
                                authMethod = provider.authMethod, authHeaderName = provider.authHeaderName,
                                requestFormat = provider.requestFormat, apiKey = "",
                            )
                        },
                        onManageModels = { onOpenModels(provider.id) },
                        onDelete = { viewModel.delete(provider) },
                    )
                }
            }
        }
    }

    editingForm?.let { form ->
        ProviderEditDialog(
            initial = form,
            isSaving = uiState.isSaving,
            onDismiss = { editingForm = null },
            onSave = { updated -> viewModel.save(updated) { editingForm = null } },
        )
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Couldn't save provider") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun ProviderCard(
    provider: AiProvider,
    onEdit: () -> Unit,
    onManageModels: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceElevated)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dns, null, tint = AuroraCyan)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(provider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(provider.baseUrl, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(provider.requestFormat.name) }, enabled = false)
                AssistChip(onClick = {}, label = { Text(provider.authMethod.name) }, enabled = false)
                if (provider.apiKey.isNullOrBlank()) {
                    AssistChip(onClick = {}, label = { Text("No key") }, enabled = false)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onManageModels) {
                Icon(Icons.Filled.List, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Manage models")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderEditDialog(
    initial: ProviderFormState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProviderFormState) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var chatPath by remember { mutableStateOf(initial.chatCompletionsPath) }
    var modelsPath by remember { mutableStateOf(initial.modelsListPath) }
    var authMethod by remember { mutableStateOf(initial.authMethod) }
    var authHeaderName by remember { mutableStateOf(initial.authHeaderName) }
    var requestFormat by remember { mutableStateOf(initial.requestFormat) }
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var authMenuExpanded by remember { mutableStateOf(false) }
    var formatMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "Add Provider" else "Edit Provider") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Provider name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL, e.g. https://api.openai.com/v1") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(chatPath, { chatPath = it }, label = { Text("Chat completions path") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(modelsPath, { modelsPath = it }, label = { Text("Models list path") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = formatMenuExpanded, onExpandedChange = { formatMenuExpanded = it }) {
                    OutlinedTextField(
                        value = requestFormat.name, onValueChange = {}, readOnly = true,
                        label = { Text("Request format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = formatMenuExpanded, onDismissRequest = { formatMenuExpanded = false }) {
                        RequestFormat.entries.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { requestFormat = it; formatMenuExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = authMenuExpanded, onExpandedChange = { authMenuExpanded = it }) {
                    OutlinedTextField(
                        value = authMethod.name, onValueChange = {}, readOnly = true,
                        label = { Text("Auth method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = authMenuExpanded, onDismissRequest = { authMenuExpanded = false }) {
                        AuthMethod.entries.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { authMethod = it; authMenuExpanded = false })
                        }
                    }
                }

                if (authMethod == AuthMethod.API_KEY_HEADER) {
                    OutlinedTextField(authHeaderName, { authHeaderName = it }, label = { Text("Header name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(
                    apiKey, { apiKey = it },
                    label = { Text(if (initial.id == null) "API key (optional)" else "New API key (leave blank to keep current)") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    onSave(
                        ProviderFormState(
                            id = initial.id, name = name, baseUrl = baseUrl,
                            chatCompletionsPath = chatPath, modelsListPath = modelsPath,
                            authMethod = authMethod, authHeaderName = authHeaderName,
                            requestFormat = requestFormat, apiKey = apiKey,
                        )
                    )
                },
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
