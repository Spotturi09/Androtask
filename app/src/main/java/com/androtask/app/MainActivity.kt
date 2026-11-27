package com.androtask.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.androtask.app.ui.ResumeApp
import com.androtask.app.ui.ResumeViewModel
import com.androtask.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: ResumeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleShare(intent)
        setContent { AppTheme { ResumeApp(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** A job posting shared from another app (LinkedIn, Indeed, Chrome…). */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) return
        val text = listOfNotNull(
            intent.getStringExtra(Intent.EXTRA_SUBJECT),
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
        ).joinToString("\n").trim()
        vm.onJobShared(text)
    }

    override fun onStop() {
        // Persist anything still waiting for the autosave debounce.
        vm.saveDirtyNow()
        super.onStop()
    }
}
