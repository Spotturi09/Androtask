package com.androtask.app.data

import com.androtask.app.model.Resume
import com.androtask.app.model.newId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Backup(val version: Int = 1, val resumes: List<Resume>)

/**
 * Stores each resume as a JSON file in [dir]. Writes go to a temp file first and are then
 * renamed, so a crash mid-write never corrupts an existing resume. All methods are
 * synchronised because autosave, "save on background" and create can overlap.
 */
class ResumeRepository(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    init {
        dir.mkdirs()
    }

    private fun fileFor(id: String) = File(dir, "$id.json")

    @Synchronized
    fun all(): List<Resume> = dir.listFiles { f -> f.extension == "json" }.orEmpty()
        .mapNotNull { f -> runCatching { json.decodeFromString<Resume>(f.readText()) }.getOrNull() }
        .sortedByDescending { it.updatedAt }

    @Synchronized
    fun get(id: String): Resume? = fileFor(id).takeIf { it.exists() }
        ?.let { runCatching { json.decodeFromString<Resume>(it.readText()) }.getOrNull() }

    @Synchronized
    fun save(resume: Resume) {
        val tmp = File.createTempFile(resume.id, ".tmp", dir)
        tmp.writeText(json.encodeToString(resume))
        if (!tmp.renameTo(fileFor(resume.id))) {
            fileFor(resume.id).writeText(tmp.readText())
            tmp.delete()
        }
    }

    @Synchronized
    fun delete(id: String) {
        fileFor(id).delete()
    }

    @Synchronized
    fun duplicate(id: String): Resume? {
        val original = get(id) ?: return null
        val copy = original.copy(
            id = newId(),
            title = "${original.title} (copy)",
            updatedAt = System.currentTimeMillis(),
        )
        save(copy)
        return copy
    }

    @Synchronized
    fun exportBackup(): String = json.encodeToString(Backup(resumes = all()))

    /** Imports resumes from a backup. Existing resumes with the same id are replaced. Returns the count. */
    @Synchronized
    fun importBackup(text: String): Int {
        val backup = json.decodeFromString<Backup>(text)
        backup.resumes.forEach { save(it) }
        return backup.resumes.size
    }
}
