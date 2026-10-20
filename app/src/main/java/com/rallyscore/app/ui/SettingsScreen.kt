package com.rallyscore.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rallyscore.app.ScoreViewModel
import com.rallyscore.app.model.KeyNames
import com.rallyscore.app.model.Side
import com.rallyscore.app.settings.AppSettings
import com.rallyscore.app.settings.OrientationMode
import com.rallyscore.app.settings.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ScoreViewModel, settings: AppSettings, onBack: () -> Unit) {
    val learning by vm.learning.collectAsStateWithLifecycle()
    val testing by vm.testing.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)
    BackHandler(enabled = learning != null) { vm.cancelLearning() }
    BackHandler(enabled = testing) { vm.stopTesting() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                item { Header("Scoring") }
                item {
                    StepperRow("Points to win", "Winner must lead by 2 points", settings.target, 1..settings.max) { v ->
                        vm.updateSettings { it.copy(target = v) }
                    }
                }
                item {
                    StepperRow("Maximum points", "Reaching this wins outright, even without a 2-point lead", settings.max, settings.target..99) { v ->
                        vm.updateSettings { it.copy(max = v) }
                    }
                }

                item { Header("Players") }
                item {
                    NameField("Player 1", settings.player1) { v -> vm.updateSettings { it.copy(player1 = v.take(24)) } }
                }
                item {
                    NameField("Player 2", settings.player2) { v -> vm.updateSettings { it.copy(player2 = v.take(24)) } }
                }
                item {
                    SwitchRow("Show names", null, settings.showNames) { v -> vm.updateSettings { it.copy(showNames = v) } }
                }
                item {
                    SwitchRow("Show server", "Dot next to the player who won the last rally", settings.showServe) { v ->
                        vm.updateSettings { it.copy(showServe = v) }
                    }
                }

                item { Header("Display") }
                item {
                    ChoiceGroup(
                        title = "Theme",
                        options = listOf(
                            ThemeMode.DARK to "Dark (AMOLED black)",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.SYSTEM to "Follow system",
                        ),
                        selected = settings.theme,
                    ) { v -> vm.updateSettings { it.copy(theme = v) } }
                }
                item {
                    ChoiceGroup(
                        title = "Orientation",
                        options = listOf(
                            OrientationMode.AUTO to "Rotate with phone",
                            OrientationMode.PORTRAIT to "Portrait",
                            OrientationMode.LANDSCAPE to "Landscape",
                            OrientationMode.LOCKED to "Lock current orientation",
                        ),
                        selected = settings.orientation,
                    ) { v -> vm.updateSettings { it.copy(orientation = v) } }
                }

                item { Header("Feedback") }
                item {
                    SwitchRow("Announce score", "Speaks the score after every change", settings.announce) { v ->
                        vm.updateSettings { it.copy(announce = v) }
                    }
                }
                item {
                    SwitchRow("Vibrate", "Short buzz for each point, undo and reset", settings.vibrate) { v ->
                        vm.updateSettings { it.copy(vibrate = v) }
                    }
                }

                item { Header("Buttons & remote") }
                item {
                    SwitchRow("Use volume / remote buttons", "Buttons score instead of changing the volume", settings.volumeKeys) { v ->
                        vm.updateSettings { it.copy(volumeKeys = v) }
                    }
                }
                item { KeyRow("Left / top counter", settings.keyA) { vm.startLearning(Side.A) } }
                item { KeyRow("Right / bottom counter", settings.keyB) { vm.startLearning(Side.B) } }
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedButton(onClick = { vm.startTesting() }) { Text("Test buttons") }
                    }
                }
                item {
                    SliderRow("Hold delay", "How long to hold before the score starts climbing", settings.holdDelayMs, 300..2000, 100, "ms") { v ->
                        vm.updateSettings { it.copy(holdDelayMs = v) }
                    }
                }
                item {
                    SliderRow("Climb speed", "Time between points while holding", settings.holdStepMs, 50..500, 25, "ms") { v ->
                        vm.updateSettings { it.copy(holdStepMs = v) }
                    }
                }
                item {
                    SliderRow(
                        "Repeat gap",
                        "Button events closer than this count as one held press. Raise it if holding the remote only adds " +
                            "single points; lower it if quick taps get merged. \"Test buttons\" shows your remote's gaps.",
                        settings.chainGapMs, 50..400, 10, "ms",
                    ) { v -> vm.updateSettings { it.copy(chainGapMs = v) } }
                }
                item {
                    SliderRow("Double-press window", "Max time between presses to start a new game", settings.doublePressMs, 200..1000, 50, "ms") { v ->
                        vm.updateSettings { it.copy(doublePressMs = v) }
                    }
                }
                item {
                    SliderRow("Both-buttons window", "Presses this close together count as both buttons (undo)", settings.comboWindowMs, 100..600, 25, "ms") { v ->
                        vm.updateSettings { it.copy(comboWindowMs = v) }
                    }
                }

                item { Header("Screen protection") }
                item {
                    SwitchRow("Pixel shift", "Moves the layout a few pixels every minute", settings.pixelShift) { v ->
                        vm.updateSettings { it.copy(pixelShift = v) }
                    }
                }
                item {
                    SwitchRow("Fade controls", "Dims the bottom buttons after 8 seconds without input", settings.fadeControls) { v ->
                        vm.updateSettings { it.copy(fadeControls = v) }
                    }
                }
                item {
                    SwitchRow("Dim when idle", "Lowers brightness when nothing happens (screen stays on)", settings.dimWhenIdle) { v ->
                        vm.updateSettings { it.copy(dimWhenIdle = v) }
                    }
                }
                item {
                    SliderRow("Dim after", "Minutes without input before dimming", settings.dimAfterMin, 1..30, 1, "min", enabled = settings.dimWhenIdle) { v ->
                        vm.updateSettings { it.copy(dimAfterMin = v) }
                    }
                }

                item {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { vm.resetSettings() }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text("Restore default settings")
                        }
                    }
                }
            }
        }

        learning?.let { side -> LearnOverlay(side, onCancel = { vm.cancelLearning() }) }
        if (testing) TestOverlay(vm, onClose = { vm.stopTesting() })
    }
}

@Composable
private fun LearnOverlay(side: Side, onCancel: () -> Unit) {
    val which = if (side == Side.A) "left / top" else "right / bottom"
    Overlay(onDismiss = onCancel) {
        Text("Press a button", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Press the button on your remote or phone that should add a point to the $which counter.")
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
    }
}

@Composable
private fun TestOverlay(vm: ScoreViewModel, onClose: () -> Unit) {
    val log by vm.keyLog.collectAsStateWithLifecycle()
    Overlay(onDismiss = onClose) {
        Text("Test buttons", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Press and hold your remote buttons. The gap between events while holding shows what " +
                "\"Repeat gap\" needs to exceed.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .heightIn(min = 120.dp, max = 320.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (log.isEmpty()) Text("Waiting for a button…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            log.forEach { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Done") }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StepperRow(title: String, subtitle: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleBlock(title, subtitle, Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 36.dp),
        )
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, contentDescription = "Increase")
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleBlock(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> ChoiceGroup(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        options.forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun NameField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun KeyRow(title: String, keyCode: Int, onLearn: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleBlock(title, KeyNames.of(keyCode), Modifier.weight(1f))
        TextButton(onClick = onLearn) { Text("Change") }
    }
}

@Composable
private fun SliderRow(
    title: String,
    subtitle: String,
    value: Int,
    range: IntRange,
    step: Int,
    unit: String,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("${local.roundToInt()} $unit", style = MaterialTheme.typography.bodyMedium)
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            enabled = enabled,
        )
    }
}
