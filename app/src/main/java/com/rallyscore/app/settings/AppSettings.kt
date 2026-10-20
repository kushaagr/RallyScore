package com.rallyscore.app.settings

import android.content.Context
import android.view.KeyEvent
import com.rallyscore.app.input.InputConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class OrientationMode { AUTO, PORTRAIT, LANDSCAPE, LOCKED }

data class AppSettings(
    val target: Int = 21,
    val max: Int = 30,
    val player1: String = "Player 1",
    val player2: String = "Player 2",
    val showNames: Boolean = true,
    val showServe: Boolean = true,
    val theme: ThemeMode = ThemeMode.DARK,
    val orientation: OrientationMode = OrientationMode.AUTO,
    val announce: Boolean = true,
    val vibrate: Boolean = true,
    val volumeKeys: Boolean = true,
    val keyA: Int = KeyEvent.KEYCODE_VOLUME_UP,
    val keyB: Int = KeyEvent.KEYCODE_VOLUME_DOWN,
    val holdDelayMs: Int = 700,
    val holdStepMs: Int = 150,
    val chainGapMs: Int = 120,
    val doublePressMs: Int = 450,
    val comboWindowMs: Int = 250,
    val pixelShift: Boolean = true,
    val fadeControls: Boolean = true,
    val dimWhenIdle: Boolean = true,
    val dimAfterMin: Int = 5,
    val swapped: Boolean = false,
) {
    fun toInputConfig() = InputConfig(
        holdDelayMs = holdDelayMs.toLong(),
        holdStepMs = holdStepMs.toLong(),
        chainGapMs = chainGapMs.toLong(),
        doublePressMs = doublePressMs.toLong(),
        comboWindowMs = comboWindowMs.toLong(),
    )

    fun name(player: Int): String = if (player == 0) player1 else player2
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val raw = transform(_settings.value)
        val target = raw.target.coerceIn(1, 99)
        val fixed = raw.copy(target = target, max = raw.max.coerceIn(target, 99))
        _settings.value = fixed
        save(fixed)
    }

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            target = prefs.getInt("target", d.target),
            max = prefs.getInt("max", d.max),
            player1 = prefs.getString("player1", null) ?: d.player1,
            player2 = prefs.getString("player2", null) ?: d.player2,
            showNames = prefs.getBoolean("showNames", d.showNames),
            showServe = prefs.getBoolean("showServe", d.showServe),
            theme = enumOr(prefs.getString("theme", null), d.theme),
            orientation = enumOr(prefs.getString("orientation", null), d.orientation),
            announce = prefs.getBoolean("announce", d.announce),
            vibrate = prefs.getBoolean("vibrate", d.vibrate),
            volumeKeys = prefs.getBoolean("volumeKeys", d.volumeKeys),
            keyA = prefs.getInt("keyA", d.keyA),
            keyB = prefs.getInt("keyB", d.keyB),
            holdDelayMs = prefs.getInt("holdDelayMs", d.holdDelayMs),
            holdStepMs = prefs.getInt("holdStepMs", d.holdStepMs),
            chainGapMs = prefs.getInt("chainGapMs", d.chainGapMs),
            doublePressMs = prefs.getInt("doublePressMs", d.doublePressMs),
            comboWindowMs = prefs.getInt("comboWindowMs", d.comboWindowMs),
            pixelShift = prefs.getBoolean("pixelShift", d.pixelShift),
            fadeControls = prefs.getBoolean("fadeControls", d.fadeControls),
            dimWhenIdle = prefs.getBoolean("dimWhenIdle", d.dimWhenIdle),
            dimAfterMin = prefs.getInt("dimAfterMin", d.dimAfterMin),
            swapped = prefs.getBoolean("swapped", d.swapped),
        )
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putInt("target", s.target)
            .putInt("max", s.max)
            .putString("player1", s.player1)
            .putString("player2", s.player2)
            .putBoolean("showNames", s.showNames)
            .putBoolean("showServe", s.showServe)
            .putString("theme", s.theme.name)
            .putString("orientation", s.orientation.name)
            .putBoolean("announce", s.announce)
            .putBoolean("vibrate", s.vibrate)
            .putBoolean("volumeKeys", s.volumeKeys)
            .putInt("keyA", s.keyA)
            .putInt("keyB", s.keyB)
            .putInt("holdDelayMs", s.holdDelayMs)
            .putInt("holdStepMs", s.holdStepMs)
            .putInt("chainGapMs", s.chainGapMs)
            .putInt("doublePressMs", s.doublePressMs)
            .putInt("comboWindowMs", s.comboWindowMs)
            .putBoolean("pixelShift", s.pixelShift)
            .putBoolean("fadeControls", s.fadeControls)
            .putBoolean("dimWhenIdle", s.dimWhenIdle)
            .putInt("dimAfterMin", s.dimAfterMin)
            .putBoolean("swapped", s.swapped)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
