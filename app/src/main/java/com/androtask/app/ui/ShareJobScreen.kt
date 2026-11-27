package com.androtask.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.androtask.app.ai.AiException
import kotlinx.coroutines.launch

/** Shown when a job posting is shared into the app: pick a resume and tailor a copy to the job. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareJobScreen(vm: ResumeViewModel, onDone: (String) -> Unit, onCancel: () -> Unit) {
    val shared by vm.sharedJob.collectAsState()
    val resumes by vm.resumes.collectAsState()
    var job by remember(shared) { mutableStateOf(shared.orEmpty()) }
    var selected by remember(resumes) { mutableStateOf(resumes.firstOrNull()?.id) }
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<AiException?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun start() {
        val id = selected ?: return
        if (!vm.aiSettings.configured) { showSettings = true; return }
        scope.launch {
            busy = "Reading the job and tailoring your resume…"
            val r = vm.resolveJobText(job).fold({ vm.aiTailor(id, it) }, { Result.failure(it) })
            busy = null
            r.onSuccess(onDone).onFailure { error = it as? AiException }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tailor to this job") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Cancel") } },
            )
        },
        bottomBar = {
            Button(
                onClick = ::start,
                enabled = selected != null && job.isNotBlank(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text("Tailor selected resume") }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Job posting", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    job, { job = it }, minLines = 4,
                    supportingText = { Text("A link is fine: the app reads the posting. If the page needs a login, paste the text.") },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                )
            }
            item {
                Text(
                    if (resumes.isEmpty()) "You don't have a resume yet. Create or import one first." else "Choose the resume to tailor",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(resumes, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth().clickable { selected = r.id }) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == r.id, onClick = { selected = r.id })
                        Column {
                            Text(r.title, style = MaterialTheme.typography.bodyLarge)
                            val sub = listOf(r.personal.fullName, r.personal.jobTitle).filter { it.isNotBlank() }.joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Text("Light changes only: summary, bullet and skill order, and matching keywords. Your facts stay the same, " +
                    "and the result is saved as a new copy.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showSettings) AiSettingsDialog(vm.aiSettings, onDismiss = { showSettings = false })
    busy?.let { AiBusyDialog(it) }
    error?.let { e ->
        AiErrorDialog(e.message ?: "Unknown error", onDismiss = { error = null },
            onOpenSettings = if (e.kind in setOf(AiException.Kind.NO_KEY, AiException.Kind.BAD_KEY, AiException.Kind.BAD_MODEL)) ({ showSettings = true }) else null)
    }
}
