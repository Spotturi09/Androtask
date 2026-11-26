package com.androtask.app

import com.androtask.app.model.*
import org.junit.Assert.*
import org.junit.Test

class ResumeModelTest {
    @Test fun emptyResumeScoresZeroAndHasNoVisibleSections() {
        val r = Resume()
        assertEquals(0, r.completeness())
        assertTrue(r.visibleSections.isEmpty())
        assertTrue(r.suggestions().contains("Add your full name"))
    }

    @Test fun fullSampleScoresHighAndHasNoSuggestions() {
        val r = SampleData.resume()
        assertTrue(r.completeness() >= 95)
        assertTrue(r.suggestions().isEmpty())
    }

    @Test fun hiddenSectionsAreNotPrinted() {
        val r = SampleData.resume().toggleHidden(SectionType.REFERENCES)
        assertFalse(SectionType.REFERENCES in r.visibleSections)
        assertTrue(SectionType.REFERENCES in r.toggleHidden(SectionType.REFERENCES).visibleSections)
    }

    @Test fun moveSectionReordersAndClampsAtEdges() {
        val r = Resume().moveSection(SectionType.SKILLS, -10)
        assertEquals(SectionType.SKILLS, r.normalizedOrder.first())
        assertEquals(SectionType.entries.size, r.normalizedOrder.size)
        val last = r.moveSection(SectionType.SUMMARY, 100)
        assertEquals(SectionType.SUMMARY, last.normalizedOrder.last())
    }

    @Test fun normalizedOrderAddsMissingSections() {
        val r = Resume(sectionOrder = listOf(SectionType.EXPERIENCE, SectionType.EXPERIENCE))
        assertEquals(SectionType.EXPERIENCE, r.normalizedOrder.first())
        assertEquals(SectionType.entries.toSet(), r.normalizedOrder.toSet())
        assertEquals(SectionType.entries.size, r.normalizedOrder.size)
    }

    @Test fun bulletLinesStripMarkersAndBlanks() {
        assertEquals(listOf("One", "Two", "Three"), "• One\n\n - Two\n*Three  ".bulletLines())
    }

    @Test fun dateRangeFormats() {
        assertEquals("Jan 2020 – Present", dateRange("Jan 2020", "Mar 2021", current = true))
        assertEquals("2019 – 2021", dateRange("2019", "2021"))
        assertEquals("2021", dateRange("", "2021"))
        assertEquals("", dateRange("", ""))
    }
}
