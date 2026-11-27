package com.androtask.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ResumeApp(vm: ResumeViewModel = viewModel()) {
    val nav = rememberNavController()
    val shared by vm.sharedJob.collectAsState()
    LaunchedEffect(shared) { if (shared != null) nav.navigate("share") { launchSingleTop = true } }
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
        composable("share") {
            ShareJobScreen(
                vm,
                onDone = { newId ->
                    vm.clearSharedJob()
                    nav.navigate("edit/$newId") { popUpTo("home") }
                },
                onCancel = { vm.clearSharedJob(); if (!nav.popBackStack()) nav.navigate("home") },
            )
        }
        composable("preview/{id}") { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            PreviewScreen(vm, id, onBack = { nav.popBackStack() })
        }
    }
}
