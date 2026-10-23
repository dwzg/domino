package de.wtzg.domino.training

import kotlin.random.Random

/** A double-six domino. */
data class Tile(val a: Int, val b: Int) {
    val pips: Int get() = a + b
    val isDouble: Boolean get() = a == b
    fun has(n: Int): Boolean = a == n || b == n
    fun other(n: Int): Int = if (a == n) b else a
    override fun toString(): String = "$a|$b"

    companion object {
        /** All 28 tiles, high number first. */
        val SET: List<Tile> = (0..6).flatMap { lo -> (lo..6).map { hi -> Tile(hi, lo) } }
    }
}

enum class End { LEFT, RIGHT }

data class Move(val tile: Tile, val end: End)

/** A tile on the line of play, turned so [left] faces the left end. */
data class Placed(val tile: Tile, val left: Int, val right: Int)

data class Board(val line: List<Placed> = emptyList()) {
    val isEmpty: Boolean get() = line.isEmpty()
    val leftEnd: Int? get() = line.firstOrNull()?.left
    val rightEnd: Int? get() = line.lastOrNull()?.right

    fun fits(tile: Tile, end: End): Boolean =
        isEmpty || tile.has(if (end == End.LEFT) leftEnd!! else rightEnd!!)

    fun play(move: Move): Board {
        val t = move.tile
        if (isEmpty) return Board(listOf(Placed(t, t.a, t.b)))
        return if (move.end == End.LEFT) {
            val n = leftEnd!!
            Board(listOf(Placed(t, t.other(n), n)) + line)
        } else {
            val n = rightEnd!!
            Board(line + Placed(t, n, t.other(n)))
        }
    }

    /** Open ends after [move] is played. */
    fun endsAfter(move: Move): Pair<Int, Int> = play(move).let { it.leftEnd!! to it.rightEnd!! }
}

/** Outcome of a hand: [winner] scores [points] (null winner = tie, nobody scores). */
data class HandResult(val winner: Int?, val points: Int, val blocked: Boolean, val pipsLeft: List<Int>)

/**
 * One hand of two-player Draw dominoes. Player 0 is the human, 1 the AI.
 * Each player gets 7 tiles; 14 stay in the boneyard. When you can't play you draw
 * until you can; with an empty boneyard you pass. Two passes in a row block the game.
 */
data class Round(
    val hands: List<List<Tile>>,
    val boneyard: List<Tile>,
    val turn: Int,
    /** Tile that must open the hand (highest double), or null if the leader may play anything. */
    val opening: Tile?,
    val board: Board = Board(),
    val passes: Int = 0,
    /** Numbers each player has shown they don't hold (by drawing or passing). */
    val lacks: List<Set<Int>> = listOf(emptySet(), emptySet()),
    val result: HandResult? = null,
) {
    val isOver: Boolean get() = result != null

    fun legalMoves(player: Int = turn): List<Move> {
        if (isOver || player != turn) return emptyList()
        val hand = hands[player]
        if (board.isEmpty) return (opening?.let { listOf(it) } ?: hand).map { Move(it, End.RIGHT) }
        val l = board.leftEnd!!
        val r = board.rightEnd!!
        return hand.flatMap { t ->
            buildList {
                if (t.has(l)) add(Move(t, End.LEFT))
                if (t.has(r) && l != r) add(Move(t, End.RIGHT))
            }
        }
    }

    val canDraw: Boolean get() = !isOver && boneyard.isNotEmpty() && legalMoves().isEmpty()
    val mustPass: Boolean get() = !isOver && boneyard.isEmpty() && legalMoves().isEmpty()

    fun play(move: Move): Round {
        require(move in legalMoves()) { "Illegal move $move" }
        val p = turn
        val hand = hands[p] - move.tile
        val newHands = hands.toMutableList().also { it[p] = hand }
        val newBoard = board.play(move)
        if (hand.isEmpty()) {
            val pips = newHands.map { h -> h.sumOf { it.pips } }
            return copy(
                hands = newHands, board = newBoard, opening = null, passes = 0,
                result = HandResult(p, pips[1 - p], blocked = false, pipsLeft = pips),
            )
        }
        return copy(hands = newHands, board = newBoard, opening = null, passes = 0, turn = 1 - p)
    }

    /** Draws one tile for the player to move. Returns the new round and the drawn tile. */
    fun draw(): Pair<Round, Tile> {
        check(canDraw) { "Can't draw now" }
        val p = turn
        val tile = boneyard.first()
        val newHands = hands.toMutableList().also { it[p] = it[p] + tile }
        // Drawing proves the player had neither end number; older knowledge is void
        // because the new tile is hidden.
        val newLacks = lacks.toMutableList().also { it[p] = setOf(board.leftEnd!!, board.rightEnd!!) }
        return copy(hands = newHands, boneyard = boneyard.drop(1), lacks = newLacks) to tile
    }

    fun pass(): Round {
        check(mustPass) { "Can't pass now" }
        val p = turn
        val newLacks = lacks.toMutableList().also { it[p] = it[p] + setOf(board.leftEnd!!, board.rightEnd!!) }
        if (passes + 1 >= 2) {
            val pips = hands.map { h -> h.sumOf { it.pips } }
            val winner = when {
                pips[0] < pips[1] -> 0
                pips[1] < pips[0] -> 1
                else -> null
            }
            val points = winner?.let { pips[1 - it] } ?: 0
            return copy(passes = passes + 1, lacks = newLacks, result = HandResult(winner, points, true, pips))
        }
        return copy(passes = passes + 1, lacks = newLacks, turn = 1 - p)
    }

    companion object {
        /**
         * Deals a new hand. If [leader] is given (winner of the last hand) that player
         * opens with any tile; otherwise the highest double opens (or the heaviest tile).
         */
        fun deal(random: Random = Random, leader: Int? = null): Round {
            val tiles = Tile.SET.shuffled(random)
            val hands = listOf(tiles.subList(0, 7), tiles.subList(7, 14))
            val boneyard = tiles.subList(14, 28)
            if (leader != null) return Round(hands, boneyard, turn = leader, opening = null)
            val inPlay = hands.flatten()
            val open = inPlay.filter { it.isDouble }.maxByOrNull { it.a }
                ?: inPlay.maxWith(compareBy<Tile> { it.pips }.thenBy { maxOf(it.a, it.b) })
            val starter = if (open in hands[0]) 0 else 1
            return Round(hands, boneyard, turn = starter, opening = open)
        }
    }
}
