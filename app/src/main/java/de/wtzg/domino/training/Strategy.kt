package de.wtzg.domino.training

import kotlin.random.Random

enum class Difficulty(val label: String, val description: String) {
    EASY("Easy", "Plays any tile that fits"),
    NORMAL("Normal", "Dumps its heaviest tiles first"),
    HARD("Hard", "Plays like the coach: counts tiles, reads you and blocks"),
}

/** One reason that makes a move better or worse. */
data class Factor(val points: Double, val text: String?)

data class Evaluation(val move: Move, val factors: List<Factor>) {
    val score: Double get() = factors.sumOf { it.points }

    /** The strongest arguments for this move, best first. */
    val pros: List<String> get() = factors.filter { it.points > 0 && it.text != null }
        .sortedByDescending { it.points }.mapNotNull { it.text }

    val cons: List<String> get() = factors.filter { it.points < 0 && it.text != null }
        .sortedBy { it.points }.mapNotNull { it.text }
}

/**
 * Rates moves using only what the player can know: their own hand, the board,
 * and what the opponent revealed by drawing or passing.
 */
object Strategy {

    /** Numbers [player]'s opponent can't hold, as far as [player] can tell. */
    fun opponentLacks(round: Round, player: Int): Set<Int> {
        val visible = round.hands[player] + round.board.line.map { it.tile }
        val allSeen = (0..6).filter { n -> visible.count { it.has(n) } == 7 }
        return round.lacks[1 - player] + allSeen
    }

    /** Tiles carrying [n] that [player] hasn't seen (with the opponent or in the boneyard). */
    fun unseen(round: Round, player: Int, n: Int): Int {
        val visible = round.hands[player] + round.board.line.map { it.tile }
        return 7 - visible.count { it.has(n) }
    }

    fun evaluateAll(round: Round, player: Int = round.turn): List<Evaluation> =
        round.legalMoves(player).map { evaluate(round, player, it) }.sortedByDescending { it.score }

    fun evaluate(round: Round, player: Int, move: Move): Evaluation {
        val tile = move.tile
        val rest = round.hands[player] - tile
        if (rest.isEmpty()) return Evaluation(move, listOf(Factor(1000.0, "It's your last tile: play it and win the hand.")))
        if (round.board.isEmpty && round.opening != null) {
            return Evaluation(move, listOf(Factor(0.0, "The highest double always opens the hand.")))
        }

        val (l, r) = round.board.endsAfter(move)
        val oppLacks = opponentLacks(round, player)
        val oppTiles = round.hands[1 - player].size
        val f = mutableListOf<Factor>()

        f += Factor(
            tile.pips * 0.6,
            if (tile.pips >= 8) "It sheds ${tile.pips} pips. Heavy tiles cost you points if you lose the hand." else null,
        )
        if (tile.isDouble) {
            f += Factor(3.0, "Doubles fit only one number, so they get stuck easily. Play them while you can.")
        }

        val followUps = rest.count { it.has(l) || it.has(r) }
        f += if (followUps == 0) {
            Factor(-6.0, "It leaves you no tile for either end, so you'll likely have to draw next turn.")
        } else {
            Factor(minOf(followUps, 4) * 1.2, if (followUps >= 2) "You keep $followUps tiles that fit the new ends." else null)
        }

        // Blocking matters more the closer the opponent is to going out.
        val urgency = if (oppTiles <= 2) 2.0 else 1.0
        val lacking = listOf(l, r).distinct().filter { it in oppLacks }
        when {
            l in oppLacks && r in oppLacks -> f += Factor(
                8.0 * urgency,
                if (l == r) "Both ends show $l. Your opponent has no ${l}s, so it must draw or pass."
                else "The ends become $l and $r. Your opponent has neither, so it must draw or pass.",
            )
            lacking.isNotEmpty() -> f += Factor(
                2.5 * urgency,
                "Afterwards one end shows ${lacking.first()}, and your opponent has shown it has no ${lacking.first()}s.",
            )
        }

        // Owning a suit: if you hold most of the unseen tiles of a number, an open end of that number is yours.
        for (e in setOf(l, r)) {
            val mine = rest.count { it.has(e) }
            if (mine >= 2) {
                val othersCould = unseen(round, player, e)
                f += Factor(
                    mine * 1.0 + if (othersCould <= 1) 1.5 else 0.0,
                    "You hold $mine more ${e}s, so the $e end is likely to stay yours.",
                )
            }
        }

        // Opening a number the opponent probably has helps them.
        if (oppTiles <= 2) {
            val risky = listOf(l, r).distinct().filter { it !in oppLacks && unseen(round, player, it) >= 3 }
            if (risky.isNotEmpty()) {
                f += Factor(-2.0, "Your opponent is close to going out, and a ${risky.first()} end may give it a play.")
            }
        }
        return Evaluation(move, f)
    }

    fun chooseMove(round: Round, difficulty: Difficulty, random: Random = Random): Move? {
        val moves = round.legalMoves()
        if (moves.isEmpty()) return null
        return when (difficulty) {
            Difficulty.EASY -> moves.random(random)
            Difficulty.NORMAL -> moves.maxBy { it.tile.pips * 10 + (if (it.tile.isDouble) 5 else 0) + random.nextInt(3) }
            Difficulty.HARD -> moves.maxBy { evaluate(round, round.turn, it).score + random.nextDouble() * 0.01 }
        }
    }

    /** Names a move by the number it is played on, since the line snakes and "left" can sit anywhere. */
    fun describe(move: Move, board: Board): String {
        if (board.isEmpty) return "${move.tile}"
        val l = board.leftEnd!!
        val r = board.rightEnd!!
        val n = if (move.end == End.LEFT) l else r
        return if (l == r) "${move.tile} on a $n end" else "${move.tile} on the $n end"
    }
}
