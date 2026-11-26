package com.androtask.app.layout

import com.androtask.app.model.Resume
import com.androtask.app.model.SectionType
import com.androtask.app.model.Skill
import com.androtask.app.model.TemplateId
import com.androtask.app.model.bulletLines
import com.androtask.app.model.dateRange

private const val INK = 0xFF1E2329
private const val MUTED = 0xFF5F6670
private const val WHITE = 0xFFFFFFFF
private const val HAIRLINE = 0xFFD5D9DE

/**
 * Lays out [resume] with its selected template. If the content spills onto an extra page,
 * the layout is retried slightly tighter and the tighter version is used when it saves a page
 * (recruiters strongly prefer a full page over a page and a half).
 */
fun layoutResume(resume: Resume, measurer: TextMeasurer): Document {
    val normal = layoutAt(resume, measurer, 1f)
    if (normal.pages.size == 1) return normal
    for (density in floatArrayOf(0.94f, 0.88f)) {
        val tight = layoutAt(resume, measurer, density)
        if (tight.pages.size < normal.pages.size) return tight
    }
    return normal
}

private fun layoutAt(resume: Resume, measurer: TextMeasurer, density: Float): Document = when (resume.style.template) {
    TemplateId.CLASSIC -> ClassicTemplate(resume, measurer, density).build()
    TemplateId.MODERN -> ModernTemplate(resume, measurer, density).build()
    TemplateId.MINIMAL -> MinimalTemplate(resume, measurer, density).build()
    TemplateId.SIDEBAR -> SidebarTemplate(resume, measurer, density).build()
}

fun skillLevelLabel(level: Int): String = when (level) {
    1 -> "Beginner"; 2 -> "Basic"; 3 -> "Intermediate"; 4 -> "Advanced"; 5 -> "Expert"; else -> ""
}

fun languageLevelLabel(level: Int): String = when (level) {
    1 -> "Beginner"; 2 -> "Elementary"; 3 -> "Intermediate"; 4 -> "Fluent"; 5 -> "Native"; else -> ""
}

/** Shared section rendering; templates override the header and heading look. */
abstract class BaseTemplate(protected val resume: Resume, measurer: TextMeasurer, protected val density: Float = 1f) {
    protected val accent = resume.style.accent
    protected val font = resume.style.font
    protected open val margin = 44f
    protected val doc = DocumentBuilder(measurer) { page, index -> decoratePage(page, index) }

    /** Text style; body sizes shrink a little when [density] < 1, headline sizes stay put. */
    protected fun style(size: Float, bold: Boolean = false, italic: Boolean = false, color: Long = INK) =
        TextStyle(if (size <= 12.5f) size * (0.5f + density / 2f) else size, bold, italic, color, font)

    /** Vertical spacing scaled by [density]. */
    protected fun Column.gap(v: Float) = space(v * density)

    protected open val body get() = style(9.6f)
    protected open val bodyMuted get() = style(9.2f, color = MUTED)
    protected open val itemTitle get() = style(10.4f, bold = true)
    protected open val itemSub get() = style(9.6f, italic = true, color = MUTED)
    protected open val date get() = style(9.2f, color = MUTED)

    protected open fun decoratePage(page: Page, index: Int) {}

    protected abstract fun heading(col: Column, title: String)

    protected fun sectionTitle(section: SectionType): String =
        if (section == SectionType.CUSTOM) resume.customTitle.ifBlank { "Additional" } else section.label

    protected fun contactItems(): List<String> = with(resume.personal) {
        listOf(email, phone, location, linkedin, website).map { it.trim() }.filter { it.isNotEmpty() }
    }

    protected fun section(col: Column, section: SectionType) {
        // Keep a heading together with at least the first lines of its content.
        col.ensure(48f)
        heading(col, sectionTitle(section))
        when (section) {
            SectionType.SUMMARY -> col.paragraph(resume.summary.trim(), body)
            SectionType.EXPERIENCE -> resume.experience.filter { it.role.isNotBlank() || it.company.isNotBlank() }
                .forEachIndexed { i, e ->
                    if (i > 0) col.gap(7f)
                    col.ensure(itemTitle.lineHeight + itemSub.lineHeight + body.lineHeight)
                    col.leftRight(e.role.ifBlank { e.company }, itemTitle, dateRange(e.startDate, e.endDate, e.current), date)
                    val sub = listOf(if (e.role.isNotBlank()) e.company else "", e.location).filter { it.isNotBlank() }
                    if (sub.isNotEmpty()) col.paragraph(sub.joinToString("  ·  "), itemSub)
                    col.gap(2f)
                    e.description.bulletLines().forEach { col.bullet(it, body, accent) }
                }
            SectionType.EDUCATION -> resume.education.filter { it.degree.isNotBlank() || it.school.isNotBlank() }
                .forEachIndexed { i, e ->
                    if (i > 0) col.gap(6f)
                    col.ensure(itemTitle.lineHeight + itemSub.lineHeight)
                    col.leftRight(e.degree.ifBlank { e.school }, itemTitle, dateRange(e.startDate, e.endDate), date)
                    val sub = listOf(if (e.degree.isNotBlank()) e.school else "", e.location).filter { it.isNotBlank() }
                    if (sub.isNotEmpty()) col.paragraph(sub.joinToString("  ·  "), itemSub)
                    val details = e.details.bulletLines()
                    if (details.size == 1) col.paragraph(details[0], body) else details.forEach { col.bullet(it, body, accent) }
                }
            SectionType.SKILLS -> col.paragraph(joinSkills(resume.skills, ::skillLevelLabel), body)
            SectionType.LANGUAGES -> col.paragraph(joinSkills(resume.languages, ::languageLevelLabel), body)
            SectionType.PROJECTS -> resume.projects.filter { it.name.isNotBlank() }.forEachIndexed { i, p ->
                if (i > 0) col.gap(6f)
                col.leftRight(p.name, itemTitle, p.link, date)
                val lines = p.description.bulletLines()
                if (lines.size == 1) col.paragraph(lines[0], body) else lines.forEach { col.bullet(it, body, accent) }
            }
            SectionType.CERTIFICATIONS -> resume.certifications.filter { it.name.isNotBlank() }.forEach { c ->
                val name = if (c.issuer.isBlank()) c.name else "${c.name} — ${c.issuer}"
                col.leftRight(name, body.copy(bold = true), c.date, date)
            }
            SectionType.REFERENCES -> resume.references.filter { it.name.isNotBlank() }.forEachIndexed { i, r ->
                if (i > 0) col.gap(5f)
                col.ensure(itemTitle.lineHeight * 2)
                col.paragraph(r.name, itemTitle)
                col.paragraph(r.position, itemSub)
                col.paragraph(r.contact, bodyMuted)
            }
            SectionType.CUSTOM -> {
                val lines = resume.customContent.bulletLines()
                if (lines.size == 1) col.paragraph(lines[0], body) else lines.forEach { col.bullet(it, body, accent) }
            }
        }
        col.gap(12f)
    }

    private fun joinSkills(items: List<Skill>, label: (Int) -> String): String =
        items.filter { it.name.isNotBlank() }.joinToString("   •   ") {
            val l = label(it.level)
            if (l.isEmpty()) it.name.trim() else "${it.name.trim()} ($l)"
        }

    protected fun addPageNumbers() {
        val n = doc.pageCount
        if (n < 2) return
        val s = style(8f, color = MUTED)
        for (i in 0 until n) {
            val text = "${i + 1} / $n"
            val w = doc.measurer.width(text, s)
            doc.page(i).ops += DrawOp.Text(doc.pageWidth - margin - w, doc.pageHeight - 22f, text, s)
        }
    }

    abstract fun build(): Document
}

/** Centred header, ruled headings, single column. Most ATS-friendly. */
private class ClassicTemplate(resume: Resume, m: TextMeasurer, d: Float) : BaseTemplate(resume, m, d) {
    override val margin = 48f

    override fun heading(col: Column, title: String) {
        col.paragraph(title.uppercase(), style(10.5f, bold = true, color = accent))
        col.gap(1f)
        col.rule(accent, 0.9f)
        col.gap(6f)
    }

    override fun build(): Document {
        val col = Column(doc, margin, doc.pageWidth - margin * 2, { margin }, doc.pageHeight - margin)
        with(resume.personal) {
            col.paragraph(fullName.ifBlank { "Your Name" }, style(23f, bold = true), align = Align.CENTER)
            if (jobTitle.isNotBlank()) col.paragraph(jobTitle, style(12f, color = accent), align = Align.CENTER)
        }
        col.space(3f)
        col.paragraph(contactItems().joinToString("   |   "), bodyMuted, align = Align.CENTER)
        col.space(14f)
        resume.visibleSections.forEach { section(col, it) }
        addPageNumbers()
        return doc.build()
    }
}

/** Accent header band with optional photo, accent-bar headings. */
private class ModernTemplate(resume: Resume, m: TextMeasurer, d: Float) : BaseTemplate(resume, m, d) {
    private var headerHeight = 0f

    override fun heading(col: Column, title: String) {
        val s = style(11.5f, bold = true, color = accent)
        col.add(DrawOp.Rect(col.x, col.y + 1.5f, 3f, s.size, accent))
        col.paragraph(title, s, indent = 9f)
        col.gap(5f)
    }

    override fun build(): Document {
        val p = resume.personal
        val photo = p.photoPath
        val photoSize = 74f
        val textWidth = doc.pageWidth - margin * 2 - (if (photo != null) photoSize + 16f else 0f)
        val nameStyle = style(25f, bold = true, color = WHITE)
        val titleStyle = style(12.5f, color = WHITE)
        val contactStyle = style(9.2f, color = WHITE)
        val contact = contactItems().joinToString("   •   ")

        // Measure the header first so the band fits its text.
        val nameLines = doc.wrap(p.fullName.ifBlank { "Your Name" }, nameStyle, textWidth)
        val titleLines = if (p.jobTitle.isBlank()) emptyList() else doc.wrap(p.jobTitle, titleStyle, textWidth)
        val contactLines = if (contact.isBlank()) emptyList() else doc.wrap(contact, contactStyle, textWidth)
        val textH = nameLines.size * nameStyle.lineHeight + titleLines.size * titleStyle.lineHeight +
            (if (contactLines.isEmpty()) 0f else 6f + contactLines.size * contactStyle.lineHeight)
        headerHeight = maxOf(textH, if (photo != null) photoSize else 0f) + 52f

        val first = doc.page(0)
        first.ops += DrawOp.Rect(0f, 0f, doc.pageWidth, headerHeight, accent)
        val header = Column(doc, margin, textWidth, { 26f }, headerHeight)
        nameLines.forEach { header.paragraph(it, nameStyle) }
        titleLines.forEach { header.paragraph(it, titleStyle) }
        if (contactLines.isNotEmpty()) {
            header.space(6f)
            contactLines.forEach { header.paragraph(it, contactStyle) }
        }
        if (photo != null) {
            val py = (headerHeight - photoSize) / 2f
            first.ops += DrawOp.Image(doc.pageWidth - margin - photoSize, py, photoSize, photo)
        }

        val col = Column(doc, margin, doc.pageWidth - margin * 2,
            { if (it == 0) headerHeight + 22f else margin }, doc.pageHeight - margin)
        resume.visibleSections.forEach { section(col, it) }
        addPageNumbers()
        return doc.build()
    }
}

/** Lots of white space, letter-spaced small headings, thin rules. */
private class MinimalTemplate(resume: Resume, m: TextMeasurer, d: Float) : BaseTemplate(resume, m, d) {
    override val margin = 54f

    override fun heading(col: Column, title: String) {
        col.paragraph(title.uppercase().toCharArray().joinToString(" "), style(9f, bold = true, color = accent))
        col.gap(5f)
    }

    override fun build(): Document {
        val col = Column(doc, margin, doc.pageWidth - margin * 2, { margin }, doc.pageHeight - margin)
        with(resume.personal) {
            col.paragraph(fullName.ifBlank { "Your Name" }, style(27f))
            if (jobTitle.isNotBlank()) col.paragraph(jobTitle, style(12f, color = MUTED))
        }
        col.space(4f)
        col.paragraph(contactItems().joinToString("   ·   "), bodyMuted)
        col.space(10f)
        col.rule(HAIRLINE, 0.6f)
        col.space(16f)
        resume.visibleSections.forEach { section(col, it) }
        addPageNumbers()
        return doc.build()
    }
}

/** Two columns: coloured sidebar (photo, contact, skills, languages, certificates) + main column. */
private class SidebarTemplate(resume: Resume, m: TextMeasurer, d: Float) : BaseTemplate(resume, m, d) {
    private val sidebarWidth = 190f
    private val sidePad = 20f
    private val sideSections = setOf(SectionType.SKILLS, SectionType.LANGUAGES, SectionType.CERTIFICATIONS)
    private var inSidebar = false

    override fun decoratePage(page: Page, index: Int) {
        page.ops += DrawOp.Rect(0f, 0f, sidebarWidth, page.height, accent)
    }

    override fun heading(col: Column, title: String) {
        if (inSidebar) {
            col.paragraph(title.uppercase(), style(10f, bold = true, color = WHITE))
            col.space(2f)
            col.rule(0x99FFFFFF, 0.6f)
            col.space(6f)
        } else {
            col.paragraph(title.uppercase(), style(11f, bold = true, color = accent))
            col.gap(1f)
            col.rule(HAIRLINE, 0.7f)
            col.gap(6f)
        }
    }

    override fun build(): Document {
        val p = resume.personal
        val side = Column(doc, sidePad, sidebarWidth - sidePad * 2, { 36f }, doc.pageHeight - 36f)
        inSidebar = true
        p.photoPath?.let {
            val size = 96f
            side.add(DrawOp.Image(side.x + (side.width - size) / 2f, side.y, size, it))
            side.space(size + 14f)
        }
        side.paragraph(p.fullName.ifBlank { "Your Name" }, style(18f, bold = true, color = WHITE))
        if (p.jobTitle.isNotBlank()) side.paragraph(p.jobTitle, style(10.5f, color = 0xE6FFFFFF))
        side.space(16f)
        val contacts = contactItems()
        if (contacts.isNotEmpty()) {
            heading(side, "Contact")
            contacts.forEach { side.paragraph(it, style(8.8f, color = WHITE)); side.space(2f) }
            side.space(12f)
        }
        val white = style(9f, color = WHITE)
        resume.visibleSections.filter { it in sideSections }.forEach { s ->
            side.ensure(40f)
            heading(side, s.label)
            when (s) {
                SectionType.SKILLS -> resume.skills.filter { it.name.isNotBlank() }.forEach { levelRow(side, it, white) }
                SectionType.LANGUAGES -> resume.languages.filter { it.name.isNotBlank() }.forEach { levelRow(side, it, white) }
                else -> resume.certifications.filter { it.name.isNotBlank() }.forEach { c ->
                    side.paragraph(c.name, white.copy(bold = true))
                    val meta = listOf(c.issuer, c.date).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) side.paragraph(meta, style(8.4f, color = 0xCCFFFFFF))
                    side.space(4f)
                }
            }
            side.space(12f)
        }

        inSidebar = false
        val mainX = sidebarWidth + 24f
        val main = Column(doc, mainX, doc.pageWidth - mainX - 32f, { 38f }, doc.pageHeight - 38f)
        resume.visibleSections.filter { it !in sideSections }.forEach { section(main, it) }
        addPageNumbers()
        return doc.build()
    }

    private fun levelRow(col: Column, skill: Skill, s: TextStyle) {
        col.paragraph(skill.name.trim(), s)
        if (skill.level in 1..5) {
            col.ensure(8f)
            for (i in 0 until 5) {
                col.add(DrawOp.Circle(col.x + 3f + i * 10f, col.y + 3f, 3f,
                    if (i < skill.level) WHITE else 0x55FFFFFF))
            }
            col.space(9f)
        }
        col.space(3f)
    }
}
