package de.wtzg.domino.feature.play

import de.wtzg.domino.engine.Board
import de.wtzg.domino.engine.Evaluation
import de.wtzg.domino.engine.Move
import de.wtzg.domino.engine.Tile
import de.wtzg.domino.ui.components.TableBadge

/** Another player at the table, as the game screen shows them. */
data class SeatUi(
    val name: String,
    val score: Int,
    val tiles: Int,
    /** Their tiles, once the hand is over. */
    val revealed: List<Tile>? = null,
    /** It's their turn. */
    val active: Boolean = false,
    /** Working out a move (computer) or the turn is theirs and they're online. */
    val thinking: Boolean = false,
    /** A person who doesn't have the game open right now (online games). */
    val away: Boolean = false,
)

data class ResultLine(val name: String, val pips: Int?, val score: Int, val isYou: Boolean)

/** A card shown on top of the table, e.g. the end of a hand or of the match. */
data class OverlayUi(
    val title: String,
    val lines: List<String>,
    val scores: List<ResultLine>,
    val primary: String?,
    val secondary: String? = null,
)

/**
 * Everything the game screen draws. Built from an offline match or from an online
 * game view, so both look and behave the same.
 */
data class TableUi(
    val title: String,
    val board: Board,
    val hand: List<Tile>,
    val legal: List<Move>,
    val yourTurn: Boolean,
    val canDraw: Boolean,
    val mustPass: Boolean,
    val yourScore: Int,
    /** Everyone else in turn order after you. */
    val opponents: List<SeatUi>,
    /** Tiles left to draw; null in Block, where there's no drawing. */
    val boneyard: Int?,
    val status: String,
    val showHint: Boolean = true,
    val selected: Tile? = null,
    val hint: Evaluation? = null,
    val coach: CoachNote? = null,
    val log: List<String> = emptyList(),
    val lastPlaced: Tile? = null,
    val lastPlacedByOpponent: Boolean = false,
    val badge: TableBadge? = null,
    val overlay: OverlayUi? = null,
)

/** Everything the game screen can ask for. */
interface GameActions {
    fun tapTile(tile: Tile)
    fun playMove(move: Move)
    fun draw()
    fun pass()
    fun showHint()

    /** The main button on the overlay (next hand, rematch, ...). */
    fun overlayPrimary()
}
