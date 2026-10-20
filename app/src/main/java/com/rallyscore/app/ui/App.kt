package com.rallyscore.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rallyscore.app.Screen
import com.rallyscore.app.ScoreViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RallyScoreApp(
    vm: ScoreViewModel,
    interaction: StateFlow<Long>,
    contentOrientation: StateFlow<Int>,
    onVolume: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val lastInteraction by interaction.collectAsStateWithLifecycle()
    val orientation by contentOrientation.collectAsStateWithLifecycle()

    RallyTheme(settings.theme) {
        when (screen) {
            Screen.SCOREBOARD -> ScoreboardScreen(vm, settings, lastInteraction, orientation, onVolume)
            Screen.SETTINGS -> SettingsScreen(vm, settings, onBack = { vm.closeSettings() })
        }
    }
}
