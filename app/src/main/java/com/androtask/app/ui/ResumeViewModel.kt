package com.androtask.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androtask.app.data.ResumeRepository
import com.androtask.app.model.Resume
import com.androtask.app.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@OptIn(FlowPreview::class)
class ResumeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ResumeRepository(File(app.filesDir, "resumes"))

    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    val resumes: StateFlow<List<Resume>> = _resumes.asStateFlow()

    private val pendingSaves = MutableSharedFlow<Resume>(extraBufferCapacity = 64)

    /** Ids edited since their last write to disk. */
    private val dirty: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    init {
        refresh()
        // Autosave: persist the latest edit shortly after typing stops.
        viewModelScope.launch {
            pendingSaves.debounce(400).collect { r ->
                // Skip if the resume was deleted while the save was pending.
                if (_resumes.value.any { it.id == r.id }) withContext(Dispatchers.IO) { saveIfDirty(r.id) }
            }
        }
    }

    @Synchronized
    private fun saveIfDirty(id: String) {
        if (dirty.remove(id)) resume(id)?.let { repo.save(it) }
    }

    /**
     * Writes every unsaved edit right now. Called when the app goes to the background so
     * nothing typed in the last moments is lost if Android kills the process.
     */
    fun saveDirtyNow() {
        dirty.toList().forEach { saveIfDirty(it) }
    }

    private fun refresh() {
        viewModelScope.launch { _resumes.value = withContext(Dispatchers.IO) { repo.all() } }
    }

    fun resume(id: String): Resume? = _resumes.value.firstOrNull { it.id == id }

    fun create(title: String): String {
        val r = Resume(title = title.ifBlank { "My Resume" })
        _resumes.value = listOf(r) + _resumes.value
        viewModelScope.launch(Dispatchers.IO) { repo.save(r) }
        return r.id
    }

    fun update(resume: Resume) {
        val updated = resume.copy(updatedAt = System.currentTimeMillis())
        _resumes.value = _resumes.value.map { if (it.id == updated.id) updated else it }
        dirty += updated.id
        pendingSaves.tryEmit(updated)
    }

    /** Writes any pending edit immediately (e.g. before exporting). */
    suspend fun flush(id: String) {
        withContext(Dispatchers.IO) { saveIfDirty(id) }
    }

    fun delete(id: String) {
        val photo = resume(id)?.personal?.photoPath
        dirty -= id
        _resumes.value = _resumes.value.filterNot { it.id == id }
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(id)
            deletePhotoIfUnused(photo)
        }
    }

    /** Duplicates share photo files, so only delete one nobody references. */
    private fun deletePhotoIfUnused(path: String?) {
        if (path != null && _resumes.value.none { it.personal.photoPath == path }) File(path).delete()
    }

    fun duplicate(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { flush(id); repo.duplicate(id) }
            refresh()
        }
    }

    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            _resumes.value.forEach { repo.save(it) }
            val text = repo.exportBackup()
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            _resumes.value.size
        }
    }

    suspend fun importBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            repo.importBackup(text)
        }.also { refresh() }
    }

    /** Copies a picked image into app storage (downscaled) and attaches it to the resume. */
    suspend fun setPhoto(id: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = getApplication<Application>()
            val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Unsupported image")
            val scale = 600f / max(src.width, src.height)
            val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true) else src
            // New file name each time so the UI and PDF never show a stale cached image.
            val out = File(ctx.filesDir, "photos/${id}_${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            val r = resume(id) ?: return@runCatching
            val old = r.personal.photoPath
            withContext(Dispatchers.Main) { update(r.copy(personal = r.personal.copy(photoPath = out.absolutePath))) }
            deletePhotoIfUnused(old)
        }
    }

    fun removePhoto(id: String) {
        val r = resume(id) ?: return
        update(r.copy(personal = r.personal.copy(photoPath = null)))
        viewModelScope.launch(Dispatchers.IO) { deletePhotoIfUnused(r.personal.photoPath) }
    }

    /** Builds the PDF in the cache directory. */
    suspend fun buildPdf(resume: Resume): File = withContext(Dispatchers.Default) {
        val safe = resume.personal.fullName.ifBlank { resume.title }.replace(Regex("[^A-Za-z0-9 _-]"), "").trim()
            .replace(' ', '_').ifBlank { "Resume" }
        val file = File(getApplication<Application>().cacheDir, "exports/${safe}_Resume.pdf")
        PdfExporter.export(resume, file)
        file
    }

    suspend fun copyTo(file: File, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            Unit
        }
    }
}
