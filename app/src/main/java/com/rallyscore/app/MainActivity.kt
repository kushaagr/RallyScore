package com.rallyscore.app

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: ScoreViewModel by viewModels()
    private lateinit var announcer: Announcer
    private lateinit var haptics: Haptics

    private val interaction = MutableStateFlow(SystemClock.uptimeMillis())
    private val interactionFlow = interaction.asStateFlow()
    private var dimJob: Job? = null
    private var dimmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The scoreboard must never let the screen turn off.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = AudioManager.STREAM_MUSIC
        hideSystemBars()

        announcer = Announcer(this)
        haptics = Haptics(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.announcements.collect { announcer.speak(it) } }
                launch { vm.haptics.collect { if (vm.settings.value.vibrate) haptics.play(it) } }
                launch {
                    vm.settings.map { it.orientation }.distinctUntilChanged().collect { applyOrientation(it) }
                }
                launch {
                    vm.settings.map { it.dimWhenIdle to it.dimAfterMin }.distinctUntilChanged().collect { markActive() }
                }
            }
        }

        setContent {
            RallyScoreApp(vm = vm, interaction = interactionFlow, onVolume = ::showVolumePanel)
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
    }

    override fun onPause() {
        super.onPause()
        vm.input.cancelAll()
        dimJob?.cancel()
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

    private fun applyOrientation(mode: OrientationMode) {
        requestedOrientation = when (mode) {
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.LOCKED -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
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
