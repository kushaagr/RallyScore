package com.rallyscore.app

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rallyscore.app.feedback.Announcer
import com.rallyscore.app.feedback.Haptics
import com.rallyscore.app.model.Side
import com.rallyscore.app.settings.OrientationMode
import com.rallyscore.app.ui.RallyScoreApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

class MainActivity : ComponentActivity() {

    private val vm: ScoreViewModel by viewModels()
    private lateinit var announcer: Announcer
    private lateinit var haptics: Haptics

    private val interaction = MutableStateFlow(SystemClock.uptimeMillis())
    private val interactionFlow = interaction.asStateFlow()
    private var dimJob: Job? = null

    // Physical orientation the scoreboard contents face when "rotate content" is on.
    private val contentOrientation = MutableStateFlow(0)
    private val contentOrientationFlow = contentOrientation.asStateFlow()
    private var sensorOrientation = 0
    private lateinit var orientationListener: OrientationEventListener
    private var dimmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The scoreboard must never let the screen turn off.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = AudioManager.STREAM_MUSIC
        hideSystemBars()

        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val nearest = ((degrees + 45) / 90 % 4) * 90
                val distance = abs(degrees - nearest).let { min(it, 360 - it) }
                if (distance > 30) return // hysteresis: ignore angles between two orientations
                if (nearest != sensorOrientation) {
                    sensorOrientation = nearest
                    updateContentOrientation()
                }
            }
        }

        announcer = Announcer(this)
        haptics = Haptics(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.announcements.collect { announcer.speak(it) } }
                launch { vm.haptics.collect { if (vm.settings.value.vibrate) haptics.play(it) } }
                launch {
                    combine(vm.settings, vm.screen) { s, screen ->
                        Triple(s.orientation, s.rotateContent, screen == Screen.SCOREBOARD)
                    }.distinctUntilChanged().collect { (mode, rotateContent, onScoreboard) ->
                        applyOrientation(mode, rotateContent && onScoreboard)
                        updateContentOrientation()
                    }
                }
                launch {
                    vm.settings.map { it.dimWhenIdle to it.dimAfterMin }.distinctUntilChanged().collect { markActive() }
                }
            }
        }

        setContent {
            RallyScoreApp(
                vm = vm,
                interaction = interactionFlow,
                contentOrientation = contentOrientationFlow,
                onVolume = ::showVolumePanel,
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val isBack = code == KeyEvent.KEYCODE_BACK

        if (vm.learning.value != null && !isBack) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) vm.finishLearning(code)
            return true
        }
        if (vm.testing.value && !isBack) {
            vm.logKey(event)
            return true
        }

        val s = vm.settings.value
        if (vm.screen.value == Screen.SCOREBOARD && s.volumeKeys) {
            val side = when (code) {
                s.keyA -> Side.A
                s.keyB -> Side.B
                else -> null
            }
            if (side != null) {
                markActive()
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> vm.input.onKeyDown(side, event.eventTime)
                    KeyEvent.ACTION_UP -> vm.input.onKeyUp(side, event.eventTime)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        markActive()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        markActive()
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
    }

    override fun onPause() {
        super.onPause()
        vm.input.cancelAll()
        dimJob?.cancel()
        orientationListener.disable()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars() else vm.input.cancelAll()
    }

    override fun onDestroy() {
        announcer.shutdown()
        super.onDestroy()
    }

    /** Restores brightness and restarts the idle timer (burn-in protection). */
    private fun markActive() {
        interaction.value = SystemClock.uptimeMillis()
        if (dimmed) setDimmed(false)
        dimJob?.cancel()
        val s = vm.settings.value
        if (s.dimWhenIdle) {
            dimJob = lifecycleScope.launch {
                delay(s.dimAfterMin * 60_000L)
                setDimmed(true)
            }
        }
    }

    private fun setDimmed(dim: Boolean) {
        dimmed = dim
        window.attributes = window.attributes.apply {
            screenBrightness = if (dim) 0.05f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    private fun applyOrientation(mode: OrientationMode, contentRotates: Boolean) {
        requestedOrientation = if (contentRotates) {
            // The window stays put; the scoreboard turns its contents instead.
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            when (mode) {
                OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationMode.LOCKED -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
        }
    }

    /** Applies the orientation setting to the latest sensor reading (content-rotation mode). */
    private fun updateContentOrientation() {
        val current = contentOrientation.value
        val sensor = sensorOrientation
        contentOrientation.value = when (vm.settings.value.orientation) {
            OrientationMode.AUTO -> sensor
            OrientationMode.PORTRAIT -> when {
                sensor % 180 == 0 -> sensor
                current % 180 == 0 -> current
                else -> 0
            }
            OrientationMode.LANDSCAPE -> when {
                sensor % 180 != 0 -> sensor
                current % 180 != 0 -> current
                else -> 270
            }
            OrientationMode.LOCKED -> current
        }
    }

    private fun showVolumePanel() {
        val am = getSystemService(AudioManager::class.java) ?: return
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
