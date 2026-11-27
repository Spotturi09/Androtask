package com.androtask.app.ai

import com.androtask.app.model.FontChoice
import com.androtask.app.model.Resume
import com.androtask.app.model.ResumeStyle
import com.androtask.app.model.SectionType
import com.androtask.app.model.SectionType.CERTIFICATIONS
import com.androtask.app.model.SectionType.CUSTOM
import com.androtask.app.model.SectionType.EDUCATION
import com.androtask.app.model.SectionType.EXPERIENCE
import com.androtask.app.model.SectionType.LANGUAGES
import com.androtask.app.model.SectionType.PROJECTS
import com.androtask.app.model.SectionType.REFERENCES
import com.androtask.app.model.SectionType.SKILLS
import com.androtask.app.model.SectionType.SUMMARY
import com.androtask.app.model.TemplateId

/**
 * Career-level "themes": each sets the layout (template, section order, colour) and tells
 * Gemini how to pitch the wording.
 */
enum class CareerLevel(
    val label: String,
    val description: String,
    val template: TemplateId,
    val accent: Long,
    val font: FontChoice,
    val order: List<SectionType>,
    val hidden: Set<SectionType>,
    val guidance: String,
) {
    ENTRY(
        "Entry-level / Graduate",
        "Education and projects first; potential, skills and eagerness to learn.",
        TemplateId.MODERN, 0xFF0F766E, FontChoice.SANS,
        listOf(SUMMARY, EDUCATION, PROJECTS, EXPERIENCE, SKILLS, CERTIFICATIONS, LANGUAGES, CUSTOM, REFERENCES),
        emptySet(),
        "The candidate is early-career. Lead with education, projects, internships, volunteering and transferable " +
            "skills. Show eagerness to learn and concrete coursework or project outcomes. Summary: 2 sentences.",
    ),
    MID(
        "Mid-level",
        "Experience first; solid, measurable results and core skills.",
        TemplateId.MODERN, 0xFF1F5FA8, FontChoice.SANS,
        listOf(SUMMARY, EXPERIENCE, SKILLS, PROJECTS, EDUCATION, CERTIFICATIONS, LANGUAGES, CUSTOM, REFERENCES),
        setOf(REFERENCES),
        "The candidate is a mid-level professional. Emphasise ownership of work, measurable results and growing " +
            "responsibility. Keep bullets concrete and skills-focused.",
    ),
    SENIOR(
        "Senior / Specialist",
        "Deep expertise, complex projects, mentoring and impact.",
        TemplateId.CLASSIC, 0xFF334155, FontChoice.SANS,
        listOf(SUMMARY, EXPERIENCE, SKILLS, CERTIFICATIONS, PROJECTS, EDUCATION, LANGUAGES, CUSTOM, REFERENCES),
        setOf(REFERENCES),
        "The candidate is senior. Emphasise depth of expertise, complex or high-stakes work, technical/strategic " +
            "decisions, mentoring others and business impact. Fewer, stronger bullets for older roles.",
    ),
    MANAGER(
        "Manager / Team lead",
        "Leading teams, delivery, hiring, budgets and stakeholder outcomes.",
        TemplateId.CLASSIC, 0xFF1F5FA8, FontChoice.SERIF,
        listOf(SUMMARY, EXPERIENCE, SKILLS, CERTIFICATIONS, EDUCATION, PROJECTS, LANGUAGES, CUSTOM, REFERENCES),
        setOf(REFERENCES, PROJECTS),
        "The candidate is a people manager. Emphasise team size led, hiring and developing people, delivery " +
            "against goals, budgets, process improvements and cross-team stakeholder outcomes.",
    ),
    EXECUTIVE(
        "Executive / Director",
        "Strategy, P&L, transformation and board-level results. Concise and authoritative.",
        TemplateId.MINIMAL, 0xFF111827, FontChoice.SERIF,
        listOf(SUMMARY, EXPERIENCE, EDUCATION, CERTIFICATIONS, SKILLS, LANGUAGES, CUSTOM, PROJECTS, REFERENCES),
        setOf(REFERENCES, PROJECTS),
        "The candidate is an executive. Write a concise, authoritative executive summary about vision, strategy and " +
            "scale. Emphasise P&L or budget responsibility, organisational size, growth, transformation, revenue or " +
            "cost outcomes and board/stakeholder leadership. Max 4 bullets per role; group skills as leadership " +
            "competencies.",
    );

    /** Applies this level's layout to [r], leaving the content alone. */
    fun applyLayout(r: Resume): Resume = r.copy(
        style = ResumeStyle(template = template, accent = accent, font = font),
        sectionOrder = order,
        hidden = hidden.filter { r.hasContent(it) || it in r.hidden }.toSet(),
    )
}
