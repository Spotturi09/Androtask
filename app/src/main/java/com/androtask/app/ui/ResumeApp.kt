package com.androtask.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ResumeApp(vm: ResumeViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(vm, onOpen = { nav.navigate("edit/$it") }, onPreview = { nav.navigate("preview/$it") })
        }
        composable("edit/{id}") { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            EditorScreen(
                vm, id,
                onBack = { nav.popBackStack() },
                onPreview = { nav.navigate("preview/$id") },
                // Open the AI-improved copy in place of the original's editor.
                onOpenResume = { newId -> nav.navigate("edit/$newId") { popUpTo("home") } },
            )
        }
        composable("preview/{id}") { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            PreviewScreen(vm, id, onBack = { nav.popBackStack() })
        }
    }
}
