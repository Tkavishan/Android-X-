package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.Screen
import com.example.ui.VaultXViewModel
import com.example.ui.screens.DecryptScreen
import com.example.ui.screens.EncryptScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VaultBlack
import com.example.ui.theme.VaultXTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VaultXViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaultXTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = VaultBlack
                ) {
                    VaultXApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun VaultXApp(viewModel: VaultXViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        viewModel.navigateBack()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.DASHBOARD -> MainDashboardScreen(
                viewModel = viewModel,
                onNavigate = { viewModel.navigateTo(it) }
            )
            Screen.ENCRYPT -> EncryptScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBack() }
            )
            Screen.DECRYPT -> DecryptScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBack() }
            )
            Screen.HISTORY -> HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBack() }
            )
            Screen.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBack() }
            )
        }
    }
}
