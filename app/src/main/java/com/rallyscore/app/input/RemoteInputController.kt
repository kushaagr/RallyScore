package com.rallyscore.app.input

import com.rallyscore.app.model.Side
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class InputConfig(
    /** How long a button must be held before the score starts climbing. */
    val holdDelayMs: Long,
    /** Time between points while climbing. */
    val holdStepMs: Long,
    /** Events closer together than this belong to the same held press (remote auto-repeat). */
    val chainGapMs: Long,
    /** Max time between the two presses of a double press. */
    val doublePressMs: Long,
    /** Both buttons pressed within this window count as "both at once". */
    val comboWindowMs: Long,
)

/**
 * Turns raw key down/up events from the phone or a Bluetooth remote into
 * press / hold / double-press / both-buttons actions.
 *
 * A "session" is one physical press. Phones send repeated DOWN events while held;
 * many remotes instead send a stream of DOWN/UP pairs. Both are merged into one
 * session as long as the gaps stay under [InputConfig.chainGapMs].
 */
class RemoteInputController(
    private val scope: CoroutineScope,
    private val config: () -> InputConfig,
    private val actions: Actions,
) {
    interface Actions {
        /** Size of the undo history; used to roll back a press that turned out to be part of a combo. */
        fun historyMarker(): Int
        fun press(side: Side)
        /** One step of hold-to-climb. Returns false when the score can't go higher. */
        fun holdStep(side: Side, marker: Int): Boolean
        fun isGameOver(): Boolean
        fun isWinner(side: Side): Boolean
        fun doublePress(side: Side)
        /** Both buttons: discard changes back to [discardTo] (if not null), then undo. */
        fun combo(discardTo: Int?)
    }

    private class Session(
        val side: Side,
        val start: Long,
        val marker: Int,
        val idleBefore: Long,
        val startedGameOver: Boolean,
    ) {
        var lastEvent: Long = start
        var down: Boolean = true
        var downs: Int = 1
        var consumed: Boolean = false
        var autofill: Boolean = false
        var holdJob: Job? = null
        var endJob: Job? = null
    }

    private data class Tap(val end: Long, val idleBefore: Long)
    private data class Recent(val start: Long, val marker: Int)

    private val sessions = HashMap<Side, Session>()
    private val lastActivity = HashMap<Side, Long>()
    private val lastTap = HashMap<Side, Tap>()
    private val recent = HashMap<Side, Recent>()

    fun onKeyDown(side: Side, time: Long) {
        val cfg = config()
        val current = sessions[side]

        // Continuation of a held press (phone auto-repeat or remote stream).
        if (current != null && (current.down || time - current.lastEvent <= cfg.chainGapMs)) {
            if (!current.down) current.downs++
            current.down = true
            current.lastEvent = time
            current.endJob?.cancel()
            lastActivity[side] = time
            return
        }
        current?.let { end(it) }

        val idleBefore = lastActivity[side]?.let { time - it } ?: Long.MAX_VALUE
        lastActivity[side] = time
        val session = Session(side, time, actions.historyMarker(), idleBefore, actions.isGameOver())
        sessions[side] = session

        // Both buttons at once -> undo.
        val other = sessions[side.other]
        if (other != null && other.consumed) {
            session.consumed = true
            return
        }
        val otherRecent = recent[side.other]
        val otherJustPressed = otherRecent != null && time - otherRecent.start <= cfg.comboWindowMs
        val otherHeld = other != null && (other.down || other.autofill)
        if (otherJustPressed || otherHeld) {
            other?.let {
                it.consumed = true
                it.holdJob?.cancel()
            }
            session.consumed = true
            recent.clear()
            lastTap.clear()
            actions.combo(if (otherJustPressed) otherRecent?.marker else null)
            return
        }
        recent[side] = Recent(time, session.marker)

        // Double press of the winner's button after the game is over -> new game.
        val tap = lastTap[side]
        if (session.startedGameOver && tap != null &&
            time - tap.end <= cfg.doublePressMs &&
            tap.idleBefore >= cfg.doublePressMs &&
            actions.isWinner(side)
        ) {
            session.consumed = true
            lastTap.remove(side)
            actions.doublePress(side)
            return
        }

        actions.press(side)
        session.holdJob = scope.launch {
            delay(cfg.holdDelayMs)
            session.autofill = true
            while (isActive && sessions[side] === session && !session.consumed) {
                if (!actions.holdStep(side, session.marker)) break
                delay(cfg.holdStepMs)
            }
        }
    }

    fun onKeyUp(side: Side, time: Long) {
        val session = sessions[side] ?: return
        session.down = false
        session.lastEvent = time
        lastActivity[side] = time
        session.endJob?.cancel()
        session.endJob = scope.launch {
            delay(config().chainGapMs)
            end(session)
        }
    }

    /** Forget everything, e.g. when the app loses focus and key-up events may never arrive. */
    fun cancelAll() {
        sessions.values.forEach {
            it.holdJob?.cancel()
            it.endJob?.cancel()
        }
        sessions.clear()
        lastTap.clear()
        recent.clear()
    }

    private fun end(s: Session) {
        s.holdJob?.cancel()
        if (sessions[s.side] === s) sessions.remove(s.side)
        val cfg = config()
        val duration = s.lastEvent - s.start
        val shortTap = !s.consumed && !s.autofill && duration < cfg.holdDelayMs
        if (!shortTap) {
            lastTap.remove(s.side)
            return
        }
        // Two very quick taps can merge into one session; still treat them as a double press.
        if (s.downs == 2 && s.startedGameOver && s.idleBefore >= cfg.doublePressMs && actions.isWinner(s.side)) {
            lastTap.remove(s.side)
            actions.doublePress(s.side)
            return
        }
        if (s.downs == 1) lastTap[s.side] = Tap(s.lastEvent, s.idleBefore) else lastTap.remove(s.side)
    }
}
