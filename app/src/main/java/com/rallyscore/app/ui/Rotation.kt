package com.rallyscore.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/**
 * Lays the content out as if the screen were turned (width/height swapped when [swapAxes])
 * and draws it rotated by [angle] degrees, so text stays upright while the window doesn't rotate.
 */
fun Modifier.rotatedContent(angle: Float, swapAxes: Boolean): Modifier =
    this
        .layout { measurable, constraints ->
            val inner = if (swapAxes) {
                Constraints(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight,
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth,
                )
            } else {
                constraints
            }
            val placeable = measurable.measure(inner)
            val w = if (swapAxes) placeable.height else placeable.width
            val h = if (swapAxes) placeable.width else placeable.height
            layout(w, h) { placeable.place((w - placeable.width) / 2, (h - placeable.height) / 2) }
        }
        .graphicsLayer { rotationZ = angle }

/**
 * Animated content rotation for a physical [orientation] (0/90/180/270, as reported by
 * OrientationEventListener). Always turns the short way round.
 */
@Composable
fun rememberContentAngle(orientation: Int): Float {
    val target = -orientation.toFloat()
    val cumulative = remember { mutableFloatStateOf(target) }
    LaunchedEffect(target) {
        val current = cumulative.floatValue
        val delta = (((target - current) % 360f) + 540f) % 360f - 180f
        cumulative.floatValue = current + delta
    }
    val animated by animateFloatAsState(cumulative.floatValue, tween(350), label = "contentRotation")
    return animated
}
