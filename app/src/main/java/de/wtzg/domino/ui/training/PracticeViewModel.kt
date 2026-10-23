package de.wtzg.domino.ui.training

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wtzg.domino.data.TrainingRepository
import de.wtzg.domino.training.Difficulty
import de.wtzg.domino.training.Evaluation
import de.wtzg.domino.training.Move
import de.wtzg.domino.training.Round
import de.wtzg.domino.training.Strategy
import de.wtzg.domino.training.Tile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PracticeSettings(val difficulty: Difficulty, val target: Int, val coach: Boolean)

enum class NoteKind { GOOD, TIP, INFO }

data class CoachNote(val kind: NoteKind, val title: String, val text: String)

const val YOU = 0
const val AI = 1

class PracticeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TrainingRepository(app)
    private var aiJob: Job? = null

    var stats by mutableStateOf(repo.load())
        private set
    var settings by mutableStateOf<PracticeSettings?>(null)
        private set
    var round by mutableStateOf<Round?>(null)
        private set
    var scores by mutableStateOf(listOf(0, 0))
        private set
    var matchWinner by mutableStateOf<Int?>(null)
        private set

    /** Feedback on your last move. */
    var coachNote by mutableStateOf<CoachNote?>(null)
        private set

    /** What the AI did on its last turn and what that reveals. */
    var aiNote by mutableStateOf<String?>(null)
        private set
    var hint by mutableStateOf<Evaluation?>(null)
        private set

    /** A tile that fits both ends; waiting for you to pick a side. */
    var selected by mutableStateOf<Tile?>(null)
        private set
    var lastPlaced by mutableStateOf<Tile?>(null)
        private set

    /** Who placed [lastPlaced], so the table can slide it in from the right side. */
    var lastPlacedBy by mutableStateOf(YOU)
        private set
    var aiThinking by mutableStateOf(false)
        private set
    var handRated by mutableStateOf(0)
        private set
    var handMatched by mutableStateOf(0)
        private set

    val matchInProgress: Boolean get() = round != null && matchWinner == null

    fun start(s: PracticeSettings) {
        settings = s
        scores = listOf(0, 0)
        matchWinner = null
        newHand(leader = null)
    }

    fun nextHand() {
        val r = round ?: return
        if (matchWinner != null) {
            settings?.let { start(it) }
        } else {
            newHand(leader = r.result?.winner)
        }
    }

    private fun newHand(leader: Int?) {
        aiJob?.cancel()
        aiThinking = false
        val r = Round.deal(leader = leader)
        round = r
        hint = null
        selected = null
        lastPlaced = null
        aiNote = null
        handRated = 0
        handMatched = 0
        coachNote = when {
            r.opening != null && r.turn == YOU ->
                CoachNote(NoteKind.INFO, "You open", "You hold the highest double, ${r.opening}, so you play it first.")
            r.opening != null ->
                CoachNote(NoteKind.INFO, "AI opens", "The AI holds the highest double, ${r.opening}, so it plays first.")
            r.turn == YOU ->
                CoachNote(NoteKind.INFO, "You lead", "You won the last hand, so lead with any tile. Doubles and heavy tiles are good leads.")
            else -> CoachNote(NoteKind.INFO, "AI leads", "The AI won the last hand, so it leads.")
        }
        afterTurn()
    }

    fun tapTile(tile: Tile) {
        val r = round ?: return
        if (r.isOver || r.turn != YOU || aiThinking) return
        val moves = r.legalMoves().filter { it.tile == tile }
        when (moves.size) {
            0 -> {
                selected = null
                coachNote = CoachNote(
                    NoteKind.INFO, "$tile doesn't fit",
                    "The open ends are ${r.board.leftEnd} and ${r.board.rightEnd}.",
                )
            }
            1 -> play(moves.single())
            else -> selected = if (selected == tile) null else tile
        }
    }

    /** Plays [move] if it's your turn and legal (e.g. from tapping a spot on the table). */
    fun playMove(move: Move) {
        val r = round ?: return
        if (r.isOver || r.turn != YOU || aiThinking || move !in r.legalMoves()) return
        play(move)
    }

    private fun play(move: Move) {
        val r = round ?: return
        val evals = Strategy.evaluateAll(r, YOU)
        val coachOn = settings?.coach == true
        val mine = evals.first { it.move == move }
        val best = evals.first()

        coachNote = when {
            r.hands[YOU].size == 1 -> CoachNote(NoteKind.GOOD, "Domino!", "You went out.")
            evals.size < 2 || (r.board.isEmpty && r.opening != null) ->
                if (coachOn) CoachNote(NoteKind.INFO, "Forced move", "That was your only possible play.") else null
            else -> {
                val good = mine.score >= best.score - 1.0
                handRated++
                if (good) handMatched++
                stats = stats.copy(movesRated = stats.movesRated + 1, movesMatched = stats.movesMatched + if (good) 1 else 0)
                repo.save(stats)
                when {
                    !coachOn -> null
                    good -> CoachNote(
                        NoteKind.GOOD, "Good move",
                        mine.pros.firstOrNull() ?: "That's what the coach would have played.",
                    )
                    else -> CoachNote(
                        NoteKind.TIP, "Better: ${Strategy.describe(best.move, r.board)}",
                        buildString {
                            append(best.pros.take(2).joinToString(" "))
                            mine.cons.firstOrNull()?.let { append("\nYour move: ").append(it) }
                        }.trim(),
                    )
                }
            }
        }
        round = r.play(move)
        lastPlaced = move.tile
        lastPlacedBy = YOU
        selected = null
        hint = null
        afterTurn()
    }

    fun draw() {
        val r = round ?: return
        if (r.turn != YOU || !r.canDraw) return
        val (next, tile) = r.draw()
        round = next
        selected = null
        hint = null
        val l = r.board.leftEnd
        val rt = r.board.rightEnd
        val reveal = if (l == rt) "${l}s" else "${l}s or ${rt}s"
        coachNote = CoachNote(
            NoteKind.INFO, "You drew $tile",
            (if (next.canDraw) "Still nothing fits. Draw again. " else "") +
                "The AI now knows you have no $reveal. Good players use that against you.",
        )
    }

    fun pass() {
        val r = round ?: return
        if (r.turn != YOU || !r.mustPass) return
        round = r.pass()
        coachNote = CoachNote(NoteKind.INFO, "You passed", "The boneyard is empty and nothing fits.")
        afterTurn()
    }

    fun showHint() {
        val r = round ?: return
        if (r.turn != YOU || r.isOver) return
        hint = Strategy.evaluateAll(r, YOU).firstOrNull()
    }

    private fun afterTurn() {
        val r = round ?: return
        when {
            r.isOver -> finishHand(r)
            r.turn == AI -> runAi()
        }
    }

    private fun runAi() {
        val difficulty = settings?.difficulty ?: return
        aiJob = viewModelScope.launch {
            aiThinking = true
            delay(900)
            var r = round ?: return@launch
            val l = r.board.leftEnd
            val rt = r.board.rightEnd
            val reveal = if (l == rt) "${l}s" else "${l}s or ${rt}s"
            var drawn = 0
            while (r.canDraw) {
                r = r.draw().first
                drawn++
                round = r
                delay(350)
            }
            val move = Strategy.chooseMove(r, difficulty)
            val note = StringBuilder()
            if (drawn > 0) {
                note.append("The AI drew $drawn tile${if (drawn > 1) "s" else ""}, so it had no $reveal. ")
            }
            if (move != null) {
                note.append("It played ${Strategy.describe(move, r.board)}.")
                round = r.play(move)
                lastPlaced = move.tile
                lastPlacedBy = AI
            } else {
                note.append("The AI passed. It has no $reveal.")
                round = r.pass()
            }
            aiNote = note.toString().trim()
            aiThinking = false
            afterTurn()
        }
    }

    private fun finishHand(r: Round) {
        val res = r.result ?: return
        res.winner?.let { w ->
            scores = scores.toMutableList().also { it[w] += res.points }
            stats = if (w == YOU) stats.copy(handsWon = stats.handsWon + 1) else stats.copy(handsLost = stats.handsLost + 1)
        }
        val target = settings?.target ?: return
        if (scores.any { it >= target } && scores[0] != scores[1]) {
            val w = if (scores[YOU] > scores[AI]) YOU else AI
            matchWinner = w
            stats = if (w == YOU) stats.copy(matchesWon = stats.matchesWon + 1) else stats.copy(matchesLost = stats.matchesLost + 1)
        }
        repo.save(stats)
    }

    fun quit() {
        aiJob?.cancel()
        aiThinking = false
        round = null
        matchWinner = null
    }

    fun resetStats() {
        stats = de.wtzg.domino.data.TrainingStats()
        repo.save(stats)
    }
}
