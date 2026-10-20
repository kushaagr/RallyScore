package com.rallyscore.app

import android.app.Application
import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rallyscore.app.feedback.Haptic
import com.rallyscore.app.input.RemoteInputController
import com.rallyscore.app.model.KeyNames
import com.rallyscore.app.model.Rules
import com.rallyscore.app.model.Score
import com.rallyscore.app.model.Side
import com.rallyscore.app.settings.AppSettings
import com.rallyscore.app.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen { SCOREBOARD, SETTINGS }

class ScoreViewModel(app: Application) : AndroidViewModel(app), RemoteInputController.Actions {

    private val settingsRepo = SettingsRepository(app)
    val settings: StateFlow<AppSettings> = settingsRepo.settings

    private val store = app.getSharedPreferences("score", Context.MODE_PRIVATE)
    private val undoStack = ArrayDeque<Score>()
    private val redoStack = ArrayDeque<Score>()

    private val _score = MutableStateFlow(Score())
    val score: StateFlow<Score> = _score.asStateFlow()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    val screen = MutableStateFlow(Screen.SCOREBOARD)

    /** Non-null while Settings waits for a button press to assign to that side. */
    val learning = MutableStateFlow<Side?>(null)

    /** True while the button tester in Settings is open. */
    val testing = MutableStateFlow(false)
    private val _keyLog = MutableStateFlow<List<String>>(emptyList())
    val keyLog: StateFlow<List<String>> = _keyLog.asStateFlow()
    private var lastLoggedTime = 0L

    private val _announcements = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()
    private val _haptics = MutableSharedFlow<Haptic>(extraBufferCapacity = 16)
    val haptics: SharedFlow<Haptic> = _haptics.asSharedFlow()

    val input = RemoteInputController(viewModelScope, { settings.value.toInputConfig() }, this)

    private var announceJob: Job? = null

    private val rules: Rules
        get() = settings.value.let { Rules(it.target, it.max) }

    init {
        restore()
    }

    fun playerAt(side: Side): Int = side.player(settings.value.swapped)

    // ---- Scoring -------------------------------------------------------------

    fun increment(side: Side): Boolean {
        val s = _score.value
        if (rules.isOver(s)) {
            emitHaptic(Haptic.BLOCKED)
            return false
        }
        val p = playerAt(side)
        val next = s.with(p, s[p] + 1).copy(server = p)
        commit(next)
        emitHaptic(if (rules.isOver(next)) Haptic.WIN else Haptic.TICK)
        return true
    }

    fun decrement(side: Side) {
        val s = _score.value
        val p = playerAt(side)
        if (s[p] == 0) {
            emitHaptic(Haptic.BLOCKED)
            return
        }
        commit(s.with(p, s[p] - 1))
        emitHaptic(Haptic.DOWN)
    }

    fun reset() {
        if (_score.value == Score()) {
            emitHaptic(Haptic.BLOCKED)
            return
        }
        commit(Score())
        emitHaptic(Haptic.RESET)
    }

    fun undo(): Boolean {
        val prev = undoStack.removeLastOrNull()
        if (prev == null) {
            emitHaptic(Haptic.BLOCKED)
            return false
        }
        redoStack.addLast(_score.value)
        setScore(prev)
        emitHaptic(Haptic.UNDO)
        return true
    }

    fun redo(): Boolean {
        val next = redoStack.removeLastOrNull()
        if (next == null) {
            emitHaptic(Haptic.BLOCKED)
            return false
        }
        undoStack.addLast(_score.value)
        setScore(next)
        emitHaptic(Haptic.TICK)
        return true
    }

    fun swapSides() {
        input.cancelAll()
        settingsRepo.update { it.copy(swapped = !it.swapped) }
        emitHaptic(Haptic.TICK)
    }

    private fun commit(next: Score) {
        undoStack.addLast(_score.value)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        setScore(next)
    }

    private fun setScore(next: Score) {
        _score.value = next
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        persist()
        scheduleAnnouncement()
    }

    // ---- Remote / volume-key actions ------------------------------------------

    override fun historyMarker(): Int = undoStack.size

    override fun press(side: Side) {
        increment(side)
    }

    override fun holdStep(side: Side, marker: Int): Boolean {
        val s = _score.value
        if (rules.isOver(s)) return false
        val p = playerAt(side)
        val next = s.with(p, s[p] + 1).copy(server = p)
        // The whole press-and-hold is a single undo step.
        if (undoStack.size == marker + 1) setScore(next) else commit(next)
        val over = rules.isOver(next)
        emitHaptic(if (over) Haptic.WIN else Haptic.TICK)
        return !over
    }

    override fun isGameOver(): Boolean = rules.isOver(_score.value)

    override fun isWinner(side: Side): Boolean = rules.winner(_score.value) == playerAt(side)

    override fun doublePress(side: Side) = reset()

    override fun combo(discardTo: Int?) {
        // Drop the point added by whichever button landed first, then undo.
        if (discardTo != null) {
            while (undoStack.size > discardTo) {
                _score.value = undoStack.removeLast()
            }
        }
        if (!undo()) setScore(_score.value)
    }

    // ---- Announcements ---------------------------------------------------------

    private fun scheduleAnnouncement() {
        if (!settings.value.announce) return
        announceJob?.cancel()
        announceJob = viewModelScope.launch {
            delay(ANNOUNCE_DEBOUNCE_MS) // a held button only announces the final score
            _announcements.tryEmit(buildAnnouncement())
        }
    }

    private fun buildAnnouncement(): String {
        val s = _score.value
        val r = rules
        val st = settings.value
        val winner = r.winner(s)
        if (winner != null) {
            return "Game. ${st.name(winner)} wins, ${s[winner]} to ${s[1 - winner]}."
        }
        val a = s[playerAt(Side.A)]
        val b = s[playerAt(Side.B)]
        val base = if (a == b) "${spoken(a)} all" else "${spoken(a)}, ${spoken(b)}"
        val gamePoint = (0..1).any { r.isGamePoint(s, it) }
        return if (gamePoint) "$base. Game point." else base
    }

    private fun spoken(n: Int) = if (n == 0) "love" else n.toString()

    private fun emitHaptic(h: Haptic) {
        _haptics.tryEmit(h)
    }

    // ---- Settings & navigation -------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepo.update(transform)

    fun resetSettings() = settingsRepo.update { AppSettings() }

    fun openSettings() {
        input.cancelAll()
        screen.value = Screen.SETTINGS
    }

    fun closeSettings() {
        learning.value = null
        testing.value = false
        screen.value = Screen.SCOREBOARD
    }

    fun startLearning(side: Side) {
        learning.value = side
    }

    fun cancelLearning() {
        learning.value = null
    }

    fun finishLearning(keyCode: Int) {
        val side = learning.value ?: return
        learning.value = null
        settingsRepo.update { s ->
            when (side) {
                Side.A -> if (s.keyB == keyCode) s.copy(keyA = keyCode, keyB = s.keyA) else s.copy(keyA = keyCode)
                Side.B -> if (s.keyA == keyCode) s.copy(keyB = keyCode, keyA = s.keyB) else s.copy(keyB = keyCode)
            }
        }
        emitHaptic(Haptic.TICK)
    }

    fun startTesting() {
        _keyLog.value = emptyList()
        lastLoggedTime = 0L
        testing.value = true
    }

    fun stopTesting() {
        testing.value = false
    }

    fun logKey(event: KeyEvent) {
        val t = event.eventTime
        val gap = if (lastLoggedTime == 0L) 0L else t - lastLoggedTime
        lastLoggedTime = t
        val dir = if (event.action == KeyEvent.ACTION_DOWN) "down" else "up"
        val rep = if (event.repeatCount > 0) " (repeat)" else ""
        val line = "+${gap} ms   ${KeyNames.of(event.keyCode)} $dir$rep"
        _keyLog.value = (listOf(line) + _keyLog.value).take(16)
    }

    // ---- Persistence ---------------------------------------------------------------

    private fun persist() {
        store.edit()
            .putString("current", _score.value.encode())
            .putString("undo", undoStack.joinToString(";") { it.encode() })
            .putString("redo", redoStack.joinToString(";") { it.encode() })
            .apply()
    }

    private fun restore() {
        store.getString("current", null)?.let { Score.decode(it) }?.let { _score.value = it }
        undoStack.addAll(decodeList(store.getString("undo", null)))
        redoStack.addAll(decodeList(store.getString("redo", null)))
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun decodeList(text: String?): List<Score> =
        text.orEmpty().split(';').mapNotNull { Score.decode(it) }

    private companion object {
        const val MAX_HISTORY = 300
        const val ANNOUNCE_DEBOUNCE_MS = 350L
    }
}
