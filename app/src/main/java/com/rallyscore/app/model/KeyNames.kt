package com.rallyscore.app.model

import android.view.KeyEvent

object KeyNames {
    fun of(keyCode: Int): String =
        KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }
}
