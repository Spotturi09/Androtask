package com.androtask.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
enum class SectionType(val label: String) {
    SUMMARY("Summary"),
    EXPERIENCE("Experience"),
    EDUCATION("Education"),
    SKILLS("Skills"),
    PROJECTS("Projects"),
    CERTIFICATIONS("Certifications"),
    LANGUAGES("Languages"),
    REFERENCES("References"),
    CUSTOM("Custom section");

    companion object {
        val defaultOrder: List<SectionType> = entries.toList()
    }
}

@Serializable
enum class TemplateId(val label: String, val atsFriendly: Boolean) {
    CLASSIC("Classic", true),
    MODERN("Modern", true),
    MINIMAL("Minimal", true),
    SIDEBAR("Sidebar", false),
}

@Serializable
enum class FontChoice(val label: String) { SANS("Sans"), SERIF("Serif") }

@Serializable
data class ResumeStyle(
    val template: TemplateId = TemplateId.MODERN,
    /** ARGB colour. */
    val accent: Long = 0xFF1F5FA8,
    val font: FontChoice = FontChoice.SANS,
)

@Serializable
data class PersonalInfo(
    val fullName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedin: String = "",
    /** Absolute path to a photo copied into app storage, or null. */
    val photoPath: String? = null,
)

@Serializable
data class Experience(
    val id: String = newId(),
    val role: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val current: Boolean = false,
    /** One achievement per line; each line becomes a bullet point. */
    val description: String = "",
)

@Serializable
data class Education(
    val id: String = newId(),
    val degree: String = "",
    val school: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val details: String = "",
)

@Serializable
data class Skill(
    val id: String = newId(),
    val name: String = "",
    /** 0 = not shown, 1..5 proficiency. */
    val level: Int = 0,
)

@Serializable
data class Project(
    val id: String = newId(),
    val name: String = "",
    val link: String = "",
    val description: String = "",
)

@Serializable
data class Certification(
    val id: String = newId(),
    val name: String = "",
    val issuer: String = "",
    val date: String = "",
)

@Serializable
data class Reference(
    val id: String = newId(),
    val name: String = "",
    val position: String = "",
    val contact: String = "",
)

@Serializable
data class Resume(
    val id: String = newId(),
    val title: String = "My Resume",
    val updatedAt: Long = System.currentTimeMillis(),
    val personal: PersonalInfo = PersonalInfo(),
    val summary: String = "",
    val experience: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val languages: List<Skill> = emptyList(),
    val projects: List<Project> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val references: List<Reference> = emptyList(),
    val customTitle: String = "Interests",
    val customContent: String = "",
    val sectionOrder: List<SectionType> = SectionType.defaultOrder,
    val hidden: Set<SectionType> = emptySet(),
    val style: ResumeStyle = ResumeStyle(),
) {
    /** Section order with any section types added in newer app versions appended. */
    val normalizedOrder: List<SectionType>
        get() = sectionOrder.distinct() + SectionType.entries.filter { it !in sectionOrder }

    fun hasContent(section: SectionType): Boolean = when (section) {
        SectionType.SUMMARY -> summary.isNotBlank()
        SectionType.EXPERIENCE -> experience.any { it.role.isNotBlank() || it.company.isNotBlank() }
        SectionType.EDUCATION -> education.any { it.degree.isNotBlank() || it.school.isNotBlank() }
        SectionType.SKILLS -> skills.any { it.name.isNotBlank() }
        SectionType.PROJECTS -> projects.any { it.name.isNotBlank() }
        SectionType.CERTIFICATIONS -> certifications.any { it.name.isNotBlank() }
        SectionType.LANGUAGES -> languages.any { it.name.isNotBlank() }
        SectionType.REFERENCES -> references.any { it.name.isNotBlank() }
        SectionType.CUSTOM -> customContent.isNotBlank()
    }

    /** Sections that will be printed, in order. */
    val visibleSections: List<SectionType>
        get() = normalizedOrder.filter { it !in hidden && hasContent(it) }

    fun moveSection(section: SectionType, delta: Int): Resume {
        val order = normalizedOrder.toMutableList()
        val from = order.indexOf(section)
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (from == to) return this
        order.add(to, order.removeAt(from))
        return copy(sectionOrder = order)
    }

    fun toggleHidden(section: SectionType): Resume =
        copy(hidden = if (section in hidden) hidden - section else hidden + section)

    /**
     * 0..100 score of how complete the resume is. The weights favour what recruiters
     * look at first: contact details, experience, and skills.
     */
    fun completeness(): Int {
        var score = 0
        if (personal.fullName.isNotBlank()) score += 10
        if (personal.jobTitle.isNotBlank()) score += 5
        if (personal.email.isNotBlank()) score += 10
        if (personal.phone.isNotBlank()) score += 5
        if (personal.location.isNotBlank()) score += 5
        if (summary.length >= 80) score += 15 else if (summary.isNotBlank()) score += 7
        if (hasContent(SectionType.EXPERIENCE)) score += 20
        if (experience.any { it.description.lines().count { l -> l.isNotBlank() } >= 2 }) score += 5
        if (hasContent(SectionType.EDUCATION)) score += 10
        if (skills.count { it.name.isNotBlank() } >= 3) score += 10 else if (hasContent(SectionType.SKILLS)) score += 5
        if (hasContent(SectionType.PROJECTS) || hasContent(SectionType.CERTIFICATIONS) || hasContent(SectionType.LANGUAGES)) score += 5
        return score.coerceAtMost(100)
    }

    /** Short checklist of the most useful things still missing. */
    fun suggestions(): List<String> = buildList {
        if (personal.fullName.isBlank()) add("Add your full name")
        if (personal.email.isBlank()) add("Add an email address")
        if (personal.phone.isBlank()) add("Add a phone number")
        if (summary.isBlank()) add("Write a 2–3 sentence summary")
        if (!hasContent(SectionType.EXPERIENCE)) add("Add at least one job, internship or volunteer role")
        else if (experience.none { it.description.lines().count { l -> l.isNotBlank() } >= 2 })
            add("Describe each job with 2+ achievement bullets")
        if (skills.count { it.name.isNotBlank() } < 3) add("List at least 3 skills")
        if (!hasContent(SectionType.EDUCATION)) add("Add your education")
    }
}

/** Lines of a multi-line field, trimmed, with leading bullet characters removed. */
fun String.bulletLines(): List<String> =
    lines().map { it.trim().trimStart('•', '-', '*', '·').trim() }.filter { it.isNotEmpty() }

fun dateRange(start: String, end: String, current: Boolean = false): String {
    val e = if (current) "Present" else end.trim()
    val s = start.trim()
    return when {
        s.isNotEmpty() && e.isNotEmpty() -> "$s – $e"
        s.isNotEmpty() -> s
        else -> e
    }
}
