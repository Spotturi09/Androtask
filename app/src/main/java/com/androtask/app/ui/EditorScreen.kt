package com.androtask.app.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.androtask.app.ai.AiException
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.androtask.app.model.Certification
import com.androtask.app.model.Education
import com.androtask.app.model.Experience
import com.androtask.app.model.Phrases
import com.androtask.app.model.Project
import com.androtask.app.model.Reference
import com.androtask.app.model.Resume
import com.androtask.app.model.SectionType
import com.androtask.app.model.Skill
import com.androtask.app.layout.languageLevelLabel
import com.androtask.app.layout.skillLevelLabel
import kotlinx.coroutines.launch

private fun <T> List<T>.replaced(i: Int, item: T) = toMutableList().also { it[i] = item }
private fun <T> List<T>.removed(i: Int) = toMutableList().also { it.removeAt(i) }
private fun <T> List<T>.moved(i: Int, delta: Int): List<T> {
    val to = (i + delta).coerceIn(0, lastIndex)
    return toMutableList().also { it.add(to, it.removeAt(i)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: ResumeViewModel, id: String, onBack: () -> Unit, onPreview: () -> Unit, onOpenResume: (String) -> Unit) {
    val all by vm.resumes.collectAsState()
    val resume = all.firstOrNull { it.id == id }
    val update: (Resume) -> Unit = { vm.update(it) }

    // ---- AI state ----
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<AiException?>(null) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showTailor by remember { mutableStateOf(false) }

    /** Runs an AI call with a progress dialog; asks for the API key first if missing. */
    fun <T> runAi(message: String, call: suspend () -> Result<T>, onOk: (T) -> Unit) {
        if (!vm.aiSettings.configured) { showAiSettings = true; return }
        scope.launch {
            busy = message
            val r = call()
            busy = null
            r.onSuccess(onOk).onFailure { aiError = it as? AiException }
        }
    }

    /** Applies an AI edit to one field and offers Undo. */
    fun applyWithUndo(apply: (Resume) -> Resume, revert: (Resume) -> Resume) {
        vm.resume(id)?.let { vm.update(apply(it)) }
        scope.launch {
            val res = snackbar.showSnackbar("Updated by Gemini — check the [brackets]", actionLabel = "Undo", duration = SnackbarDuration.Long)
            if (res == SnackbarResult.ActionPerformed) vm.resume(id)?.let { vm.update(revert(it)) }
        }
    }

    val aiActions = AiActions(
        improveSummary = {
            val old = vm.resume(id)?.summary.orEmpty()
            runAi("Writing your summary…", { vm.aiSummary(id) }) { text ->
                applyWithUndo({ it.copy(summary = text) }, { it.copy(summary = old) })
            }
        },
        improveBullets = { expId ->
            val old = vm.resume(id)?.experience?.firstOrNull { it.id == expId }?.description.orEmpty()
            fun set(r: Resume, d: String) = r.copy(experience = r.experience.map { if (it.id == expId) it.copy(description = d) else it })
            runAi("Improving bullet points…", { vm.aiBullets(id, expId) }) { text ->
                applyWithUndo({ set(it, text) }, { set(it, old) })
            }
        },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(resume?.title ?: "Resume", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { if (vm.aiSettings.configured) showTailor = true else showAiSettings = true }) {
                        Icon(Icons.Default.AutoAwesome, "Improve or tailor with AI")
                    }
                    TextButton(onClick = onPreview) {
                        Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("Preview")
                    }
                },
            )
        },
    ) { padding ->
        if (resume == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }
        var expanded by rememberSaveable { mutableStateOf("PERSONAL") }
        val toggle: (String) -> Unit = { key -> expanded = if (expanded == key) "" else key }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { ProgressCard(resume) }
            item {
                SectionCard(
                    title = "Personal details",
                    expanded = expanded == "PERSONAL",
                    onToggle = { toggle("PERSONAL") },
                ) { PersonalEditor(vm, resume, update) }
            }
            val order = resume.normalizedOrder
            items(order, key = { it.name }) { section ->
                val idx = order.indexOf(section)
                SectionCard(
                    title = if (section == SectionType.CUSTOM) resume.customTitle.ifBlank { "Custom section" } else section.label,
                    expanded = expanded == section.name,
                    onToggle = { toggle(section.name) },
                    hidden = section in resume.hidden,
                    onToggleHidden = { update(resume.toggleHidden(section)) },
                    onUp = if (idx > 0) ({ update(resume.moveSection(section, -1)) }) else null,
                    onDown = if (idx < order.lastIndex) ({ update(resume.moveSection(section, 1)) }) else null,
                ) { SectionEditor(section, resume, update, aiActions) }
            }
            item {
                Button(onClick = onPreview, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("Preview & download PDF")
                }
            }
        }
    }

    if (showAiSettings) AiSettingsDialog(vm.aiSettings, onDismiss = { showAiSettings = false })
    if (showTailor) TailorDialog(onDismiss = { showTailor = false }) { job ->
        showTailor = false
        scope.launch { vm.flush(id) }
        runAi(if (job.isBlank()) "Improving your resume…" else "Tailoring your resume to the job…", { vm.improveWithAi(id, job) }) { newId ->
            onOpenResume(newId)
        }
    }
    busy?.let { AiBusyDialog(it) }
    aiError?.let { e ->
        AiErrorDialog(e.message ?: "Unknown error", onDismiss = { aiError = null },
            onOpenSettings = if (e.kind in setOf(AiException.Kind.NO_KEY, AiException.Kind.BAD_KEY, AiException.Kind.BAD_MODEL)) ({ showAiSettings = true }) else null)
    }
}

/** AI actions offered inside section editors. */
class AiActions(val improveSummary: () -> Unit, val improveBullets: (experienceId: String) -> Unit)

@Composable
private fun ProgressCard(r: Resume) {
    val score = r.completeness()
    val tips = r.suggestions()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Text("Resume strength: $score%", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth())
            tips.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    hidden: Boolean = false,
    onToggleHidden: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                color = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (onToggleHidden != null) IconButton(onClick = onToggleHidden) {
                Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    if (hidden) "Show $title on resume" else "Hide $title from resume")
            }
            if (onUp != null || onDown != null) {
                IconButton(onClick = { onUp?.invoke() }, enabled = onUp != null) { Icon(Icons.Default.KeyboardArrowUp, "Move $title up") }
                IconButton(onClick = { onDown?.invoke() }, enabled = onDown != null) { Icon(Icons.Default.KeyboardArrowDown, "Move $title down") }
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.padding(8.dp))
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hidden) Text("Hidden from the PDF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                content()
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun PersonalEditor(vm: ResumeViewModel, r: Resume, update: (Resume) -> Unit) {
    val p = r.personal
    val set: (com.androtask.app.model.PersonalInfo) -> Unit = { update(r.copy(personal = it)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            vm.setPhoto(r.id, uri).onFailure { Toast.makeText(context, "Couldn't use that image", Toast.LENGTH_SHORT).show() }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val bmp = remember(p.photoPath) { p.photoPath?.let { BitmapFactory.decodeFile(it) } }
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (bmp != null) Image(bmp.asImageBitmap(), "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Default.Person, null)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (bmp == null) "Add photo" else "Change photo") }
            if (bmp != null) TextButton(onClick = { vm.removePhoto(r.id) }) { Text("Remove") }
        }
    }
    Text("Photos appear on the Modern and Sidebar templates. Many US/UK employers prefer no photo.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Field("Full name", p.fullName, { set(p.copy(fullName = it)) })
    Field("Job title", p.jobTitle, { set(p.copy(jobTitle = it)) }, placeholder = "e.g. Registered Nurse")
    Field("Email", p.email, { set(p.copy(email = it)) }, keyboard = KeyboardType.Email)
    Field("Phone", p.phone, { set(p.copy(phone = it)) }, keyboard = KeyboardType.Phone)
    Field("City, Country", p.location, { set(p.copy(location = it)) })
    Field("LinkedIn", p.linkedin, { set(p.copy(linkedin = it)) }, keyboard = KeyboardType.Uri, placeholder = "linkedin.com/in/you")
    Field("Website / portfolio", p.website, { set(p.copy(website = it)) }, keyboard = KeyboardType.Uri)
}

@Composable
private fun ColumnScope.SectionEditor(section: SectionType, r: Resume, update: (Resume) -> Unit, ai: AiActions) {
    when (section) {
        SectionType.SUMMARY -> {
            Field("Professional summary", r.summary, { update(r.copy(summary = it)) }, multiline = true,
                placeholder = "2–3 sentences about who you are and what you offer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiChip("Improve with AI", ai.improveSummary)
                PhraseButton("Examples", PhraseKind.SUMMARY) { update(r.copy(summary = it)) }
            }
        }
        SectionType.EXPERIENCE -> {
            r.experience.forEachIndexed { i, e ->
                val set: (Experience) -> Unit = { update(r.copy(experience = r.experience.replaced(i, it))) }
                ItemCard(i, r.experience.size,
                    onMove = { d -> update(r.copy(experience = r.experience.moved(i, d))) },
                    onDelete = { update(r.copy(experience = r.experience.removed(i))) }) {
                    Field("Job title", e.role, { set(e.copy(role = it)) })
                    Field("Company", e.company, { set(e.copy(company = it)) })
                    Field("Location", e.location, { set(e.copy(location = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field("Start", e.startDate, { set(e.copy(startDate = it)) }, Modifier.weight(1f), placeholder = "Jan 2022")
                        if (!e.current) Field("End", e.endDate, { set(e.copy(endDate = it)) }, Modifier.weight(1f), placeholder = "Mar 2024")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(e.current, { set(e.copy(current = it)) }); Text("I currently work here")
                    }
                    Field("Achievements (one per line)", e.description, { set(e.copy(description = it)) }, multiline = true,
                        placeholder = "Increased sales by 20%…")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiChip("Improve with AI") { ai.improveBullets(e.id) }
                        PhraseButton("Examples", PhraseKind.BULLET) {
                            set(e.copy(description = appendLine(e.description, it)))
                        }
                    }
                }
            }
            AddButton("Add job") { update(r.copy(experience = r.experience + Experience())) }
        }
        SectionType.EDUCATION -> {
            r.education.forEachIndexed { i, e ->
                val set: (Education) -> Unit = { update(r.copy(education = r.education.replaced(i, it))) }
                ItemCard(i, r.education.size,
                    onMove = { d -> update(r.copy(education = r.education.moved(i, d))) },
                    onDelete = { update(r.copy(education = r.education.removed(i))) }) {
                    Field("Degree / qualification", e.degree, { set(e.copy(degree = it)) })
                    Field("School / university", e.school, { set(e.copy(school = it)) })
                    Field("Location", e.location, { set(e.copy(location = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field("Start", e.startDate, { set(e.copy(startDate = it)) }, Modifier.weight(1f))
                        Field("End", e.endDate, { set(e.copy(endDate = it)) }, Modifier.weight(1f))
                    }
                    Field("Details (GPA, honours, courses)", e.details, { set(e.copy(details = it)) }, multiline = true)
                }
            }
            AddButton("Add education") { update(r.copy(education = r.education + Education())) }
        }
        SectionType.SKILLS -> {
            SkillList(r.skills, ::skillLevelLabel) { update(r.copy(skills = it)) }
            PhraseButton("Suggested skills", PhraseKind.SKILL) { name ->
                if (r.skills.none { it.name.equals(name, true) }) update(r.copy(skills = r.skills + Skill(name = name)))
            }
        }
        SectionType.LANGUAGES -> SkillList(r.languages, ::languageLevelLabel) { update(r.copy(languages = it)) }
        SectionType.PROJECTS -> {
            r.projects.forEachIndexed { i, p ->
                val set: (Project) -> Unit = { update(r.copy(projects = r.projects.replaced(i, it))) }
                ItemCard(i, r.projects.size,
                    onMove = { d -> update(r.copy(projects = r.projects.moved(i, d))) },
                    onDelete = { update(r.copy(projects = r.projects.removed(i))) }) {
                    Field("Project name", p.name, { set(p.copy(name = it)) })
                    Field("Link", p.link, { set(p.copy(link = it)) }, keyboard = KeyboardType.Uri)
                    Field("Description (one point per line)", p.description, { set(p.copy(description = it)) }, multiline = true)
                }
            }
            AddButton("Add project") { update(r.copy(projects = r.projects + Project())) }
        }
        SectionType.CERTIFICATIONS -> {
            r.certifications.forEachIndexed { i, c ->
                val set: (Certification) -> Unit = { update(r.copy(certifications = r.certifications.replaced(i, it))) }
                ItemCard(i, r.certifications.size,
                    onMove = { d -> update(r.copy(certifications = r.certifications.moved(i, d))) },
                    onDelete = { update(r.copy(certifications = r.certifications.removed(i))) }) {
                    Field("Certificate", c.name, { set(c.copy(name = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field("Issuer", c.issuer, { set(c.copy(issuer = it)) }, Modifier.weight(1f))
                        Field("Date", c.date, { set(c.copy(date = it)) }, Modifier.weight(1f))
                    }
                }
            }
            AddButton("Add certificate") { update(r.copy(certifications = r.certifications + Certification())) }
        }
        SectionType.REFERENCES -> {
            r.references.forEachIndexed { i, ref ->
                val set: (Reference) -> Unit = { update(r.copy(references = r.references.replaced(i, it))) }
                ItemCard(i, r.references.size,
                    onMove = { d -> update(r.copy(references = r.references.moved(i, d))) },
                    onDelete = { update(r.copy(references = r.references.removed(i))) }) {
                    Field("Name", ref.name, { set(ref.copy(name = it)) })
                    Field("Position & company", ref.position, { set(ref.copy(position = it)) })
                    Field("Email or phone", ref.contact, { set(ref.copy(contact = it)) })
                }
            }
            AddButton("Add reference") { update(r.copy(references = r.references + Reference())) }
        }
        SectionType.CUSTOM -> {
            Field("Section title", r.customTitle, { update(r.copy(customTitle = it)) }, placeholder = "Interests, Volunteering, Awards…")
            Field("Content (one point per line)", r.customContent, { update(r.copy(customContent = it)) }, multiline = true)
        }
    }
}

@Composable
private fun AiChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) })
}

private fun appendLine(text: String, line: String) = if (text.isBlank()) line else text.trimEnd() + "\n" + line

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(label)
    }
}

@Composable
private fun ItemCard(index: Int, count: Int, onMove: (Int) -> Unit, onDelete: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMove(-1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = index < count - 1) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
            }
            content()
        }
    }
}

@Composable
private fun SkillList(items: List<Skill>, levelLabel: (Int) -> String, onChange: (List<Skill>) -> Unit) {
    items.forEachIndexed { i, s ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field("Name", s.name, { onChange(items.replaced(i, s.copy(name = it))) }, Modifier.weight(1f))
            var open by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { open = true }) { Text(levelLabel(s.level).ifEmpty { "Level" }) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    (0..5).forEach { lvl ->
                        DropdownMenuItem(text = { Text(if (lvl == 0) "Don't show level" else levelLabel(lvl)) },
                            onClick = { open = false; onChange(items.replaced(i, s.copy(level = lvl))) })
                    }
                }
            }
            IconButton(onClick = { onChange(items.removed(i)) }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
    AddButton("Add") { onChange(items + Skill()) }
}

private enum class PhraseKind { SUMMARY, BULLET, SKILL }

@Composable
private fun PhraseButton(label: String, kind: PhraseKind, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    AssistChip(onClick = { open = true }, label = { Text(label) }, leadingIcon = { Icon(Icons.Default.Lightbulb, null) })
    if (open) PhraseDialog(kind, onDismiss = { open = false }, onPick = { onPick(it); if (kind == PhraseKind.SUMMARY) open = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhraseDialog(kind: PhraseKind, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var roleIndex by rememberSaveable { mutableStateOf(0) }
    val role = Phrases.roles[roleIndex]
    val options = when (kind) {
        PhraseKind.SUMMARY -> role.summaries
        PhraseKind.BULLET -> role.bullets
        PhraseKind.SKILL -> role.skills
    }
    var added by remember(roleIndex) { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(if (kind == PhraseKind.SKILL) "Suggested skills" else "Pick a phrase, then edit the [brackets]") },
        text = {
            Column {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Phrases.roles.forEachIndexed { i, rp ->
                        FilterChip(selected = i == roleIndex, onClick = { roleIndex = i }, label = { Text(rp.role) })
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (kind == PhraseKind.SKILL) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            options.forEach { s ->
                                FilterChip(selected = s in added, onClick = { onPick(s); added = added + s }, label = { Text(s) })
                            }
                        }
                    } else {
                        options.forEach { s ->
                            Text(
                                (if (s in added) "✓ " else "+ ") + s,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(s); added = added + s }.padding(vertical = 8.dp),
                            )
                            HorizontalDivider()
                        }
                        if (kind == PhraseKind.BULLET) {
                            Spacer(Modifier.height(8.dp))
                            Text("Strong verbs: " + Phrases.actionVerbs.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
    )
}
