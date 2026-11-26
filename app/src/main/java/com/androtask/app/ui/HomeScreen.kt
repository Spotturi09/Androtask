package com.androtask.app.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androtask.app.model.Resume
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ResumeViewModel, onOpen: (String) -> Unit, onPreview: (String) -> Unit) {
    val resumes by vm.resumes.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showNew by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Resume?>(null) }
    var deleting by remember { mutableStateOf<Resume?>(null) }
    var menu by remember { mutableStateOf(false) }
    var showAiSettings by remember { mutableStateOf(false) }
    var pickAfterSettings by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<com.androtask.app.ai.AiException?>(null) }

    val importAiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = "Reading your resume with Gemini…"
            val r = vm.importWithAi(uri)
            busy = null
            r.onSuccess { onOpen(it) }.onFailure { aiError = it as? com.androtask.app.ai.AiException }
        }
    }
    val startImport = {
        if (vm.aiSettings.configured) importAiLauncher.launch(ImportMimeTypes)
        else { pickAfterSettings = true; showAiSettings = true }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val r = vm.exportBackup(uri)
            Toast.makeText(context, r.fold({ "Backed up $it resume(s)" }, { "Backup failed: ${it.message}" }), Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val r = vm.importBackup(uri)
            Toast.makeText(context, r.fold({ "Restored $it resume(s)" }, { "Not a valid backup file" }), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume Builder") },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Import existing resume (AI)") }, onClick = {
                            menu = false; startImport()
                        })
                        DropdownMenuItem(text = { Text("AI settings") }, onClick = {
                            menu = false; showAiSettings = true
                        })
                        DropdownMenuItem(text = { Text("Back up all resumes") }, onClick = {
                            menu = false; exportLauncher.launch("resume-backup.json")
                        })
                        DropdownMenuItem(text = { Text("Restore from backup") }, onClick = {
                            menu = false; importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        })
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New resume") },
            )
        },
        bottomBar = { Box(Modifier.navigationBarsPadding()) { BannerAd() } },
    ) { padding ->
        if (resumes.isEmpty()) {
            EmptyState(Modifier.padding(padding), onImport = startImport)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(resumes, key = { it.id }) { r ->
                    ResumeCard(
                        r,
                        onClick = { onOpen(r.id) },
                        onPreview = { onPreview(r.id) },
                        onRename = { renaming = r },
                        onDuplicate = { vm.duplicate(r.id) },
                        onDelete = { deleting = r },
                    )
                }
            }
        }
    }

    if (showNew) TitleDialog("New resume", "", "Create", onDismiss = { showNew = false }) {
        showNew = false
        onOpen(vm.create(it))
    }
    renaming?.let { r ->
        TitleDialog("Rename", r.title, "Save", onDismiss = { renaming = null }) {
            vm.update(r.copy(title = it.ifBlank { r.title })); renaming = null
        }
    }
    if (showAiSettings) AiSettingsDialog(
        vm.aiSettings,
        onDismiss = { showAiSettings = false; pickAfterSettings = false },
        onSaved = { if (pickAfterSettings && vm.aiSettings.configured) importAiLauncher.launch(ImportMimeTypes) },
    )
    busy?.let { AiBusyDialog(it) }
    aiError?.let { e ->
        AiErrorDialog(e.message ?: "Unknown error", onDismiss = { aiError = null },
            onOpenSettings = if (e.kind in setOf(com.androtask.app.ai.AiException.Kind.NO_KEY,
                    com.androtask.app.ai.AiException.Kind.BAD_KEY, com.androtask.app.ai.AiException.Kind.BAD_MODEL)) ({ showAiSettings = true }) else null)
    }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${r.title}\"?") },
            text = { Text("This can't be undone. Tip: use “Back up all resumes” first.") },
            confirmButton = { TextButton(onClick = { vm.delete(r.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onImport: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Description, null, Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Build a professional resume in minutes", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Free PDF download. No watermark. Works offline.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.OutlinedButton(onClick = onImport) {
            Icon(androidx.compose.material.icons.Icons.Default.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text("Import existing resume")
        }
        Text("PDF, Word, text or photo — filled in by Gemini AI",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ResumeCard(
    r: Resume,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val score = r.completeness()
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                    val sub = listOf(r.personal.fullName, r.personal.jobTitle).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Edited " + DateUtils.getRelativeTimeSpanString(r.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Preview & download") }, onClick = { menu = false; onPreview() })
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth())
            Text("$score% complete", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun TitleDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(text, { text = it }, label = { Text("Name, e.g. \"Marketing – 2026\"") }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
