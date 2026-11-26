package com.androtask.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.androtask.app.model.FontChoice
import com.androtask.app.model.TemplateId
import com.androtask.app.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Accent colours that keep white text readable (used by the header band and sidebar). */
val AccentPalette = listOf(
    0xFF1F5FA8, 0xFF0F766E, 0xFF2E6B4F, 0xFF7C3AED, 0xFFB42318, 0xFFC2410C, 0xFF334155, 0xFF111827,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(vm: ResumeViewModel, id: String, onBack: () -> Unit) {
    val all by vm.resumes.collectAsState()
    val resume = all.firstOrNull { it.id == id } ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdf by remember { mutableStateOf<File?>(null) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val f = pdf
        if (uri != null && f != null) scope.launch {
            val r = vm.copyTo(f, uri)
            Toast.makeText(context, if (r.isSuccess) "PDF saved" else "Couldn't save the PDF", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val f = pdf ?: return@OutlinedButton
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Share resume"))
                        },
                        enabled = pdf != null && !busy,
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
                    Button(
                        onClick = { pdf?.let { saveLauncher.launch(it.name) } },
                        enabled = pdf != null && !busy,
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Download PDF") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Style controls.
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemplateId.entries.forEach { t ->
                        FilterChip(
                            selected = resume.style.template == t,
                            onClick = { vm.update(resume.copy(style = resume.style.copy(template = t))) },
                            label = { Text(if (t.atsFriendly) "${t.label} · ATS" else t.label) },
                        )
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccentPalette.forEach { c ->
                        val selected = resume.style.accent == c
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Color(c))
                                .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { vm.update(resume.copy(style = resume.style.copy(accent = c))) },
                            contentAlignment = Alignment.Center,
                        ) { if (selected) Icon(Icons.Default.Check, "Selected", tint = Color.White) }
                    }
                    Spacer(Modifier.width(4.dp))
                    FontChoice.entries.forEach { f ->
                        FilterChip(
                            selected = resume.style.font == f,
                            onClick = { vm.update(resume.copy(style = resume.style.copy(font = f))) },
                            label = { Text(f.label) },
                        )
                    }
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                val widthPx = with(LocalDensity.current) { (maxWidth - 32.dp).roundToPx() }.coerceAtLeast(300)
                LaunchedEffect(resume.style, resume.updatedAt, widthPx) {
                    busy = true
                    delay(150) // let rapid taps settle
                    vm.flush(id)
                    val file = vm.buildPdf(resume)
                    pages = withContext(Dispatchers.IO) { PdfExporter.renderPages(file, widthPx) }
                    pdf = file
                    busy = false
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(pages) { i, bmp ->
                        Image(
                            bmp.asImageBitmap(),
                            contentDescription = "Page ${i + 1}",
                            modifier = Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height).shadow(4.dp),
                        )
                    }
                }
                if (busy && pages.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}
