package com.androtask.app

import com.androtask.app.layout.DocumentBuilder
import com.androtask.app.layout.DrawOp
import com.androtask.app.layout.TextStyle
import com.androtask.app.layout.layoutResume
import com.androtask.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ResumeLayoutTest {
    private val m = AwtRenderer.measurer

    @Test fun wrapNeverExceedsWidthAndKeepsAllWords() {
        val doc = DocumentBuilder(m)
        val style = TextStyle(10f)
        val text = "The quick brown fox jumps over the lazy dog ".repeat(20) + "Supercalifragilisticexpialidocious".repeat(4)
        val lines = doc.wrap(text, style, 120f)
        lines.forEach { assertTrue("too wide: $it", m.width(it, style) <= 120f) }
        assertEquals(text.replace(" ", ""), lines.joinToString("").replace(" ", ""))
    }

    @Test fun everyTemplateKeepsTextInsidePage() {
        for (t in TemplateId.entries) {
            val doc = layoutResume(SampleData.resume(t, photo = true), m)
            for (page in doc.pages) for (op in page.ops.filterIsInstance<DrawOp.Text>()) {
                val right = op.x + m.width(op.text, op.style)
                assertTrue("$t: '${op.text}' overflows right ($right)", right <= page.width - 20f)
                assertTrue("$t: '${op.text}' starts off page", op.x >= 0f)
                assertTrue("$t: '${op.text}' below page", op.baseline <= page.height - 15f)
            }
        }
    }

    @Test fun longResumeBreaksOntoMorePagesWithPageNumbers() {
        val base = SampleData.resume(TemplateId.CLASSIC)
        val long = base.copy(experience = List(8) { base.experience[it % base.experience.size] })
        val doc = layoutResume(long, m)
        assertTrue(doc.pages.size >= 2)
        val last = doc.pages.last().ops.filterIsInstance<DrawOp.Text>().map { it.text }
        assertTrue(last.contains("${doc.pages.size} / ${doc.pages.size}"))
    }

    @Test fun slightlyLongResumeIsTightenedOntoOnePage() {
        for (t in TemplateId.entries) {
            assertEquals("$t should fit one page", 1, layoutResume(SampleData.resume(t), m).pages.size)
        }
    }

    @Test fun sidebarBackgroundRepeatsOnEveryPage() {
        val base = SampleData.resume(TemplateId.SIDEBAR)
        val doc = layoutResume(base.copy(experience = List(8) { base.experience[it % 3] }), m)
        assertTrue(doc.pages.size >= 2)
        doc.pages.forEach { p -> assertTrue(p.ops.first() is DrawOp.Rect) }
    }

    @Test fun hiddenAndEmptySectionsDoNotRender() {
        val r = SampleData.resume(TemplateId.CLASSIC).toggleHidden(SectionType.REFERENCES).copy(projects = emptyList())
        val texts = layoutResume(r, m).pages.flatMap { it.ops }.filterIsInstance<DrawOp.Text>().map { it.text }
        assertFalse(texts.contains("REFERENCES"))
        assertFalse(texts.contains("PROJECTS"))
        assertTrue(texts.contains("EXPERIENCE"))
    }

    @Test fun emptyResumeStillProducesOnePage() {
        TemplateId.entries.forEach { t ->
            val doc = layoutResume(Resume(style = ResumeStyle(template = t)), m)
            assertEquals(1, doc.pages.size)
        }
    }

    /** Writes PNGs of every template to build/template-previews for visual review. */
    @Test fun renderTemplatePreviews() {
        val out = File("build/template-previews")
        for (t in TemplateId.entries) {
            val doc = layoutResume(SampleData.resume(t, photo = t == TemplateId.SIDEBAR || t == TemplateId.MODERN), m)
            AwtRenderer.renderToPng(doc, File(out, "${t.name.lowercase()}.png"))
        }
        assertTrue(File(out, "modern.png").exists())
    }
}
