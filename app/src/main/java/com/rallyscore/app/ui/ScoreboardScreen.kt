package com.rallyscore.app.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rallyscore.app.ScoreViewModel
import com.rallyscore.app.model.Rules
import com.rallyscore.app.model.Score
import com.rallyscore.app.model.Side
import com.rallyscore.app.settings.AppSettings
import kotlinx.coroutines.delay

/** Small orbit (in dp) the whole layout walks through, one step per minute, to avoid burn-in. */
private val ShiftPattern = listOf(0 to 0, 2 to 1, 3 to -1, 1 to -3, -2 to -2, -3 to 1, -1 to 3, 2 to 3)

@Composable
fun ScoreboardScreen(
    vm: ScoreViewModel,
    settings: AppSettings,
    lastInteraction: Long,
    onVolume: () -> Unit,
) {
    val palette = LocalPalette.current
    val score by vm.score.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val rules = Rules(settings.target, settings.max)

    BackHandler(enabled = showHelp) { showHelp = false }

    // Burn-in protection: pixel shift.
    var shiftIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(settings.pixelShift) {
        if (!settings.pixelShift) {
            shiftIndex = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(60_000)
            shiftIndex = (shiftIndex + 1) % ShiftPattern.size
        }
    }
    val (dx, dy) = ShiftPattern[shiftIndex]

    // Burn-in protection: fade the control bar when idle.
    var controlsActive by remember { mutableStateOf(true) }
    LaunchedEffect(lastInteraction, settings.fadeControls) {
        controlsActive = true
        if (settings.fadeControls) {
            delay(8_000)
            controlsActive = false
        }
    }
    val controlsAlpha by animateFloatAsState(if (controlsActive) 1f else 0.2f, label = "controls")

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(12.dp)
                .offset(dx.dp, dy.dp),
        ) {
            if (landscape) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SidePanel(vm, Side.A, score, rules, settings, Modifier.weight(1f).fillMaxHeight())
                    SidePanel(vm, Side.B, score, rules, settings, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SidePanel(vm, Side.A, score, rules, settings, Modifier.weight(1f).fillMaxWidth())
                    SidePanel(vm, Side.B, score, rules, settings, Modifier.weight(1f).fillMaxWidth())
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .alpha(controlsAlpha),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarButton(Icons.Filled.RestartAlt, "Reset") { vm.reset() }
                BarButton(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo) { vm.undo() }
                BarButton(Icons.AutoMirrored.Filled.Redo, "Redo", enabled = canRedo) { vm.redo() }
                BarButton(if (landscape) Icons.Filled.SwapHoriz else Icons.Filled.SwapVert, "Swap sides") { vm.swapSides() }
                BarButton(Icons.AutoMirrored.Filled.VolumeUp, "Volume") { onVolume() }
                BarButton(Icons.AutoMirrored.Filled.HelpOutline, "Help") { showHelp = true }
                BarButton(Icons.Filled.Settings, "Settings") { vm.openSettings() }
            }
        }

        if (showHelp) HelpOverlay(onDismiss = { showHelp = false })
    }
}

@Composable
private fun SidePanel(
    vm: ScoreViewModel,
    side: Side,
    score: Score,
    rules: Rules,
    settings: AppSettings,
    modifier: Modifier,
) {
    val player = side.player(settings.swapped)
    val winner = rules.winner(score)
    val status = when {
        winner == player -> "GAME"
        rules.isGamePoint(score, player) -> "GAME POINT"
        else -> ""
    }
    CounterPanel(
        value = score[player],
        name = settings.name(player),
        showName = settings.showNames,
        isServer = settings.showServe && winner == null && score.server == player,
        status = status,
        highlight = winner == player,
        onIncrement = { vm.increment(side) },
        onDecrement = { vm.decrement(side) },
        modifier = modifier,
    )
}

@Composable
private fun CounterPanel(
    value: Int,
    name: String,
    showName: Boolean,
    isServer: Boolean,
    status: String,
    highlight: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val threshold = with(density) { 40.dp.toPx() }
    val inc by rememberUpdatedState(onIncrement)
    val dec by rememberUpdatedState(onDecrement)
    val shape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(palette.panel)
            .border(1.dp, palette.panelBorder, shape)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -threshold) inc() else if (total > threshold) dec()
                        total = 0f
                    },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val digitSize = with(density) { minOf(maxHeight * 0.55f, maxWidth * 0.6f).toSp() }
        val color = if (highlight) palette.win else palette.digits

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isServer) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                    )
                    if (showName) Spacer(Modifier.width(8.dp))
                }
                if (showName) {
                    Text(
                        text = name,
                        color = palette.muted,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = value.toString(),
                color = color,
                fontSize = digitSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(lineHeight = digitSize * 1.05f),
            )
            Text(
                text = status,
                color = if (highlight) palette.win else palette.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                modifier = Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) palette.icon else palette.icon.copy(alpha = 0.3f),
            modifier = Modifier.size(28.dp),
        )
    }
}
