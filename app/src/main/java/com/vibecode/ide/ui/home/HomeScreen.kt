package com.vibecode.ide.ui.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vibecode.ide.domain.model.Project
import com.vibecode.ide.ui.components.pressScale
import com.vibecode.ide.ui.theme.*
import com.vibecode.ide.util.StoragePermission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home is deliberately not "app bar + two buttons + card list" anymore. It's
 * a single scrolling command deck: a big gradient hero that starts a session,
 * a row of circular launcher-style quick actions, and history rendered as a
 * flat accent-striped timeline instead of boxed cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (String) -> Unit,
    onOpenProviders: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentProjects by viewModel.recentProjects.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenProjectDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasStorageAccess by remember { mutableStateOf(StoragePermission.hasAccess(context)) }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { hasStorageAccess = StoragePermission.hasAccess(context) }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { hasStorageAccess = StoragePermission.hasAccess(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasStorageAccess = StoragePermission.hasAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            allFilesLauncher.launch(StoragePermission.manageAllFilesIntent(context))
        } else {
            legacyPermissionLauncher.launch(StoragePermission.legacyPermissions)
        }
    }

    Scaffold(containerColor = VoidBlack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                HomeHeader(onOpenSettings = onOpenSettings)
            }
            item {
                AnimatedVisibility(visible = !hasStorageAccess) {
                    StoragePermissionBanner(onGrant = ::requestStorageAccess)
                }
            }
            item {
                HeroLaunchCard(
                    onStart = { if (hasStorageAccess) showNewProjectDialog = true else requestStorageAccess() },
                )
            }
            item {
                LauncherRow(
                    onOpenFolder = { if (hasStorageAccess) showOpenProjectDialog = true else requestStorageAccess() },
                    onOpenProviders = onOpenProviders,
                    onOpenSettings = onOpenSettings,
                )
            }
            if (recentProjects.isEmpty()) {
                item { EmptyState() }
            } else {
                item {
                    Text(
                        "HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(recentProjects, key = { _, it -> it.id }) { index, project ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(project.id) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(Motion.DURATION_SLOW, delayMillis = index * 40)) +
                            slideInHorizontally(
                                tween(Motion.DURATION_SLOW, delayMillis = index * 40, easing = Motion.EmphasizedEasing),
                                initialOffsetX = { it / 6 },
                            ),
                    ) {
                        TimelineRow(
                            project = project,
                            accent = timelineAccent(index),
                            onClick = { onOpenProject(project.id) },
                        )
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            isCreating = uiState.isCreatingProject,
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name ->
                viewModel.createProject(name) { projectId ->
                    showNewProjectDialog = false
                    onOpenProject(projectId)
                }
            },
        )
    }

    if (showOpenProjectDialog) {
        OpenProjectDialog(
            defaultRoot = uiState.defaultProjectsRoot,
            onDismiss = { showOpenProjectDialog = false },
            onOpen = { path ->
                viewModel.openProjectAtPath(path) { projectId ->
                    showOpenProjectDialog = false
                    onOpenProject(projectId)
                }
            },
        )
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(message) },
        )
    }
}

private fun timelineAccent(index: Int): Color = when (index % 4) {
    0 -> AuroraViolet
    1 -> AuroraCyan
    2 -> AuroraTeal
    else -> AuroraPink
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(AuroraViolet, AuroraCyan)),
            ),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "vibecode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextMuted)
        }
    }
}

@Composable
private fun HeroLaunchCard(onStart: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(SurfaceElevated, SurfaceDeep, AuroraViolet.copy(alpha = 0.22f)),
                ),
            )
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onStart)
            .padding(24.dp),
    ) {
        Column {
            Text(
                "What are we\nbuilding today?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                lineHeight = MaterialTheme.typography.headlineSmall.fontSize * 1.15f,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Describe it in plain language — VibeCode writes, edits, and runs the code.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(AuroraViolet, AuroraCyan)))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start new session", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LauncherRow(onOpenFolder: () -> Unit, onOpenProviders: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        LauncherIcon(icon = Icons.Filled.FolderOpen, label = "Open folder", tint = AuroraCyan, onClick = onOpenFolder)
        LauncherIcon(icon = Icons.Filled.SettingsInputAntenna, label = "Providers", tint = AuroraViolet, onClick = onOpenProviders)
        LauncherIcon(icon = Icons.Filled.Tune, label = "Preferences", tint = AuroraTeal, onClick = onOpenSettings)
    }
}

@Composable
private fun LauncherIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tint.copy(alpha = 0.14f))
                .pressScale(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
    }
}

@Composable
private fun StoragePermissionBanner(onGrant: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(20.dp, 4.dp, 20.dp, 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Storage access needed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                "Without this, projects and files can't actually be created on disk.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onGrant) { Text("Grant") }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.History, contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your sessions will show up here once you start or open a project.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun TimelineRow(project: Project, accent: Color, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(38.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(project.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                project.rootPath,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
            )
        }
        Text(
            dateFormat.format(Date(project.lastOpenedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = SurfaceElevated, modifier = Modifier.padding(start = 37.dp))
}

@Composable
private fun NewProjectDialog(isCreating: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column {
                Text("Give your project a name. It will be created under VibeCodeProjects/ on device storage.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Project name") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && !isCreating, onClick = { onCreate(name) }) {
                if (isCreating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun OpenProjectDialog(defaultRoot: String, onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    var path by remember { mutableStateOf(defaultRoot) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Folder") },
        text = {
            Column {
                Text("Enter the full path to an existing folder on device storage.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = path, onValueChange = { path = it },
                    label = { Text("Folder path") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = path.isNotBlank(), onClick = { onOpen(path) }) { Text("Open") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
