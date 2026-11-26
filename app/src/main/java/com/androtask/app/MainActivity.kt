package com.androtask.app

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
        setContent { AppTheme { ResumeApp(vm) } }
    }

    override fun onStop() {
        // Persist anything still waiting for the autosave debounce.
        vm.saveDirtyNow()
        super.onStop()
    }
}
