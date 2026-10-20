package com.rallyscore.app.model

/** Which on-screen counter: A = left (landscape) / top (portrait), B = right / bottom. */
enum class Side {
    A, B;

    val other: Side get() = if (this == A) B else A

    /** Player index (0 or 1) shown on this side, taking "swap sides" into account. */
    fun player(swapped: Boolean): Int = if (swapped) 1 - ordinal else ordinal
}

data class Score(
    val p1: Int = 0,
    val p2: Int = 0,
    /** Player (0 or 1) who won the last rally and therefore serves; -1 if unknown. */
    val server: Int = -1,
) {
    operator fun get(player: Int): Int = if (player == 0) p1 else p2

    fun with(player: Int, value: Int): Score =
        if (player == 0) copy(p1 = value) else copy(p2 = value)

    fun encode(): String = "$p1,$p2,$server"

    companion object {
        fun decode(text: String): Score? {
            val parts = text.split(',')
            if (parts.size != 3) return null
            val a = parts[0].toIntOrNull() ?: return null
            val b = parts[1].toIntOrNull() ?: return null
            val s = parts[2].toIntOrNull() ?: return null
            return Score(a, b, s)
        }
    }
}

/**
 * Badminton-style rules: first to [target] with a lead of [margin] wins;
 * reaching [max] wins outright (e.g. 30-29).
 */
data class Rules(val target: Int = 21, val max: Int = 30, val margin: Int = 2) {

    fun winner(s: Score): Int? {
        for (p in 0..1) {
            val mine = s[p]
            val theirs = s[1 - p]
            if (mine >= max && mine > theirs) return p
            if (mine >= target && mine - theirs >= margin) return p
        }
        return null
    }

    fun isOver(s: Score): Boolean = winner(s) != null

    fun isGamePoint(s: Score, player: Int): Boolean =
        !isOver(s) && winner(s.with(player, s[player] + 1)) == player
}
