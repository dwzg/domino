package de.wtzg.domino.training

/**
 * A tile's spot on the table, in units of half a tile (a tile is 2×1).
 * [first] is shown on the top (vertical) or left (horizontal) half.
 */
data class Laid(
    val tile: Tile,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val first: Int,
    val second: Int,
) {
    val vertical: Boolean get() = h > w
}

/**
 * Lays the line out like on a real table: the opening tile sits in the middle,
 * the right arm runs right and snakes downwards at the edge, the left arm runs
 * left and snakes upwards. Doubles lie crosswise. Tiles never move once placed,
 * so new ones can be animated in.
 */
object BoardLayout {
    /** Rows stay within ±this many units of the centre (a turn tile may stick out by one). */
    const val HALF_WIDTH = 5f

    fun layout(board: Board, halfWidth: Float = HALF_WIDTH): List<Laid> {
        if (board.isEmpty) return emptyList()
        val start = board.line[board.origin]
        val t0 = start.tile
        val out = mutableListOf(
            if (t0.isDouble) Laid(t0, -0.5f, -1f, 1f, 2f, t0.a, t0.a)
            else Laid(t0, -1f, -0.5f, 2f, 1f, start.left, start.right)
        )
        val edge = if (t0.isDouble) 0.5f else 1f
        // Right arm: the side facing the opening tile is `left`.
        arm(board.line.drop(board.origin + 1).map { Triple(it.tile, it.left, it.right) }, edge, 1, 1, halfWidth, out)
        // Left arm, walked outwards: the side facing the opening tile is `right`.
        arm(board.line.take(board.origin).reversed().map { Triple(it.tile, it.right, it.left) }, -edge, -1, -1, halfWidth, out)
        return out
    }

    /** Where [move] would land if played now. */
    fun slot(board: Board, move: Move, halfWidth: Float = HALF_WIDTH): Laid =
        layout(board.play(move), halfWidth).first { it.tile == move.tile }

    private fun arm(
        tiles: List<Triple<Tile, Int, Int>>, // tile, number touching the line, outer number
        startX: Float,
        dir: Int,
        vdir: Int,
        halfWidth: Float,
        out: MutableList<Laid>,
    ) {
        var cx = startX // x of the open end
        var cy = 0f // centre line of the current row
        var heading = dir
        var afterTurn = false
        for ((tile, match, outer) in tiles) {
            val crosswise = tile.isDouble && !afterTurn
            val needed = if (crosswise) 1f else 2f
            val fits = if (heading > 0) cx + needed <= halfWidth else cx - needed >= -halfWidth
            if (!fits) {
                // Corner: stand the tile up, matching half level with the row,
                // then continue the other way one row further out.
                val x = if (heading > 0) cx else cx - 1
                val y = if (vdir > 0) cy - 0.5f else cy - 1.5f
                val (top, bottom) = if (vdir > 0) match to outer else outer to match
                out += Laid(tile, x, y, 1f, 2f, top, bottom)
                cx += heading
                cy += 2f * vdir
                heading = -heading
                afterTurn = true
                continue
            }
            if (crosswise) {
                val x = if (heading > 0) cx else cx - 1
                out += Laid(tile, x, cy - 1f, 1f, 2f, match, match)
                cx += heading
            } else {
                val x = if (heading > 0) cx else cx - 2
                val (l, r) = if (heading > 0) match to outer else outer to match
                out += Laid(tile, x, cy - 0.5f, 2f, 1f, l, r)
                cx += 2 * heading
            }
            afterTurn = false
        }
    }
}
