package com.androtask.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasScrollAction
import androidx.test.core.app.ApplicationProvider
import com.androtask.app.data.ResumeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Drives the real Compose UI on the JVM: create → edit → autosave. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApp::class)
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun repo() = ResumeRepository(File(ApplicationProvider.getApplicationContext<android.app.Application>().filesDir, "resumes"))

    @Test fun createEditAndAutosaveResume() {
        rule.onNodeWithText("Build a professional resume in minutes").assertIsDisplayed()
        rule.onNodeWithText("New resume").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Nursing 2026")
        rule.onNodeWithText("Create").performClick()

        // Editor opens with personal details expanded.
        rule.onNodeWithText("Resume strength: 0%").assertIsDisplayed()
        rule.onNode(hasSetTextAction() and hasText("Full name")).performTextInput("Alex Rivera")
        rule.onNode(hasSetTextAction() and hasText("Email")).performTextInput("alex@example.com")
        rule.waitForIdle()
        rule.onNodeWithText("Resume strength: 20%").assertIsDisplayed()

        // Debounced autosave reaches disk.
        rule.mainClock.advanceTimeBy(1000)
        rule.waitUntil(5000) { repo().all().any { it.personal.fullName == "Alex Rivera" } }
        val saved = repo().all().single()
        assertEquals("Nursing 2026", saved.title)
        assertEquals("alex@example.com", saved.personal.email)
    }

    @Test fun reorderAndHideSections() {
        rule.onNodeWithText("New resume").performClick()
        rule.onNodeWithText("Create").performClick()
        // Collapse personal details, then move Skills up one.
        rule.onNodeWithText("Personal details").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Skills"))
        rule.onAllNodesWithText("Skills").onFirst().assertIsDisplayed()
        rule.onAllNodesWithContentDescription("Move up")[3].performClick() // Summary, Experience, Education, [Skills]
        rule.onAllNodesWithContentDescription("Hide from resume")[0].performClick()
        rule.mainClock.advanceTimeBy(1000)
        rule.waitUntil(5000) { repo().all().firstOrNull()?.hidden?.isNotEmpty() == true }
        val r = repo().all().single()
        assertEquals(listOf("SUMMARY", "EXPERIENCE", "SKILLS", "EDUCATION"), r.normalizedOrder.take(4).map { it.name })
        assertTrue(r.hidden.size == 1)
    }
}

/** Same as the real app but without ad SDK initialisation. */
class TestApp : android.app.Application()
