package com.androtask.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.androtask.app.ai.AiSettings
import com.androtask.app.ai.GeminiClient

/** File types the importer accepts. */
val ImportMimeTypes = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain",
    "image/*",
)

@Composable
fun AiSettingsDialog(settings: AiSettings, onDismiss: () -> Unit, onSaved: () -> Unit = {}) {
    var key by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, null) },
        title = { Text("Gemini AI settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI features use your own Gemini API key. Get a free key at Google AI Studio.",
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
                }) { Text("Open Google AI Studio") }
                OutlinedTextField(
                    key, { key = it }, label = { Text("Gemini API key") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    model, { model = it }, label = { Text("Model") }, singleLine = true,
                    supportingText = { Text("Default: ${GeminiClient.DEFAULT_MODEL}") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        enabled = !testing && key.isNotBlank(),
                        onClick = {
                            testing = true; testResult = null
                            scope.launch {
                                val r = withContext(Dispatchers.IO) {
                                    runCatching { GeminiClient(key, model.ifBlank { GeminiClient.DEFAULT_MODEL }).checkConnection() }
                                }
                                testing = false
                                testResult = r.fold({ true to "Connected. Gemini is ready." }, { false to (it.message ?: it.toString()) })
                            }
                        },
                    ) { Text("Test connection") }
                    if (testing) {
                        Spacer(Modifier.width(12.dp)); CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                testResult?.let { (ok, msg) ->
                    Text(msg, style = MaterialTheme.typography.bodySmall,
                        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                Text("When you use AI, the resume text (or file) is sent to Google's Gemini API. Your photo is never sent.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.apiKey = key
                settings.model = model.ifBlank { GeminiClient.DEFAULT_MODEL }
                onSaved(); onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AiBusyDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    Text("Usually 5–30 seconds", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    )
}

@Composable
fun AiErrorDialog(message: String, onDismiss: () -> Unit, onOpenSettings: (() -> Unit)?) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI couldn't finish") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = onOpenSettings?.let { open -> { TextButton(onClick = { onDismiss(); open() }) { Text("AI settings") } } },
    )
}

@Composable
fun TailorDialog(onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var job by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, null) },
        title = { Text("Improve with AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gemini rewrites your summary and bullet points for impact and fixes grammar. " +
                    "Paste a job ad to tailor it to that job. It's saved as a new copy, so your original stays as it is.",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    job, { job = it }, label = { Text("Job description (optional)") }, minLines = 4,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).padding(top = 4.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onStart(job) }) { Text(if (job.isBlank()) "Improve" else "Tailor") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
