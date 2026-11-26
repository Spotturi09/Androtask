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
import androidx.compose.ui.test.hasContentDescription
import com.androtask.app.model.SectionType
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

/** Drives the real Compose UI on the JVM. One test per class: each class runs in its own JVM. */
abstract class FlowTestBase {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    protected fun repo() = ResumeRepository(File(ApplicationProvider.getApplicationContext<android.app.Application>().filesDir, "resumes"))

    /** Sends the app to the background, which must write any pending edits to disk. */
    protected fun backgroundApp() {
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class CreateEditFlowTest : FlowTestBase() {
    @Test fun createEditAndAutosaveResume() {
        rule.onNodeWithText("Build a professional resume in minutes").assertIsDisplayed()
        rule.onNodeWithText("New resume", useUnmergedTree = true).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Nursing 2026")
        rule.onNodeWithText("Create", useUnmergedTree = true).performClick()

        // Editor opens with personal details expanded.
        rule.onNodeWithText("Resume strength: 0%").assertIsDisplayed()
        rule.onNode(hasSetTextAction() and hasText("Full name")).performTextInput("Alex Rivera")
        rule.onNode(hasSetTextAction() and hasText("Email")).performTextInput("alex@example.com")

        // Leaving the app right after typing must not lose the edits.
        backgroundApp()
        val saved = repo().all().single()
        assertEquals("Nursing 2026", saved.title)
        assertEquals("alex@example.com", saved.personal.email)
        assertEquals(20, saved.completeness())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class ReorderHideFlowTest : FlowTestBase() {
    @Test fun reorderAndHideSections() {
        rule.onNodeWithText("New resume", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Create", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Personal details").performClick() // collapse
        val list = rule.onNode(hasScrollAction())
        list.performScrollToNode(hasContentDescription("Move Skills up"))
        rule.onNodeWithContentDescription("Move Skills up").performClick()
        list.performScrollToNode(hasContentDescription("Hide References from resume"))
        rule.onNodeWithContentDescription("Hide References from resume").performClick()
        backgroundApp()
        val r = repo().all().single()
        assertEquals(
            listOf(SectionType.SUMMARY, SectionType.EXPERIENCE, SectionType.SKILLS, SectionType.EDUCATION),
            r.normalizedOrder.take(4),
        )
        assertEquals(setOf(SectionType.REFERENCES), r.hidden)
    }
}

/** Same as the real app but without ad SDK initialisation. */
class TestApp : android.app.Application()
