package com.rallyscore.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rallyscore.app.Screen
import com.rallyscore.app.ScoreViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RallyScoreApp(vm: ScoreViewModel, interaction: StateFlow<Long>, onVolume: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val lastInteraction by interaction.collectAsStateWithLifecycle()

    RallyTheme(settings.theme) {
        when (screen) {
            Screen.SCOREBOARD -> ScoreboardScreen(vm, settings, lastInteraction, onVolume)
            Screen.SETTINGS -> SettingsScreen(vm, settings, onBack = { vm.closeSettings() })
        }
    }
}
