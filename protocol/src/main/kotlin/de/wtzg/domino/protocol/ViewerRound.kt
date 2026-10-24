package de.wtzg.domino.protocol

import de.wtzg.domino.engine.Move
import de.wtzg.domino.engine.Round
import de.wtzg.domino.engine.Tile

/**
 * The current hand as you see it, as an engine [Round]: your real tiles, and placeholder
 * tiles for everyone else (only their counts are real). Good for legal moves, hints and
 * the coach, which only use what you can know. Null when no hand is running.
 */
fun GameView.toViewerRound(): Round? {
    if (status != GameStatus.PLAYING || yourSeat < 0 || turn < 0) return null
    val unknown = Tile(0, 0)
    val hands = seats.mapIndexed { i, s -> if (i == yourSeat) hand else List(s.tiles) { unknown } }
    return Round(
        rules = rules,
        hands = hands,
        boneyard = List(boneyard) { unknown },
        turn = turn,
        opening = opening,
        board = board,
        lacks = lacks,
    )
}

/** Moves you may make now (empty when it's not your turn). */
val GameView.legalMoves: List<Move>
    get() = if (isYourTurn) toViewerRound()?.legalMoves() ?: emptyList() else emptyList()

val GameView.canDraw: Boolean get() = isYourTurn && toViewerRound()?.canDraw == true
val GameView.mustPass: Boolean get() = isYourTurn && toViewerRound()?.mustPass == true
