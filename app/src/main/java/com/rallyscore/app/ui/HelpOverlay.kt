package com.rallyscore.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val HelpItems = listOf(
    "Swipe" to "Swipe up on a counter to add a point, swipe down to remove one.",
    "Volume buttons" to "Volume Up adds a point to the left/top counter, Volume Down to the right/bottom. " +
        "Works with Bluetooth remotes too. You can assign other buttons in Settings.",
    "Hold a button" to "After a short moment the score climbs quickly until the game is won. " +
        "The whole hold counts as one step for undo.",
    "New game" to "Once a game is won, double-press the winner's button to reset the scoreboard.",
    "Both buttons" to "Press both buttons together to undo.",
    "Undo / Redo" to "Undo reverses anything, including a reset.",
    "Swap sides" to "Switches which player is on which side, e.g. when changing ends. " +
        "Buttons stay tied to the side, not the player.",
    "Orientation" to "The rotate button cycles between turning with the phone, portrait and landscape. " +
        "In Settings you can choose whether only the scores turn or the whole screen.",
    "Scoring" to "First to 21 with a 2-point lead; 30 wins outright. Both limits are adjustable in Settings.",
    "Announcements" to "The score is spoken after every change. Use the speaker button to set the volume.",
    "Screen" to "The screen stays on. To protect AMOLED screens the layout shifts slightly every minute, " +
        "the buttons fade and the screen dims when idle. Touch or press any button to wake it.",
)

@Composable
fun HelpOverlay(onDismiss: () -> Unit, angle: Float = 0f, swapAxes: Boolean = false) {
    Overlay(onDismiss = onDismiss, angle = angle, swapAxes = swapAxes) {
        Text("How to use", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            HelpItems.forEach { (title, body) ->
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Got it") }
    }
}
