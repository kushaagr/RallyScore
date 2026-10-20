package com.rallyscore.app.feedback

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class Haptic { TICK, DOWN, UNDO, RESET, BLOCKED, WIN }

class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }

    fun play(h: Haptic) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (h) {
            Haptic.TICK -> VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            Haptic.DOWN -> VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 20), -1)
            Haptic.UNDO -> VibrationEffect.createWaveform(longArrayOf(0, 15, 50, 15, 50, 15), -1)
            Haptic.RESET -> VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
            Haptic.BLOCKED -> VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 60), -1)
            Haptic.WIN -> VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80, 80, 200), -1)
        }
        v.vibrate(effect)
    }
}

/** Speaks the score on the media stream, so the in-app volume button controls its loudness. */
class Announcer(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        if (tts.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE) tts.language = Locale.US
        tts.setSpeechRate(1.1f)
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        val params = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC) }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "score")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
