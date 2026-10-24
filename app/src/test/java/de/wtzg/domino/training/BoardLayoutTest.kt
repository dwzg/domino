package de.wtzg.domino.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class BoardLayoutTest {
    private fun overlap(a: Laid, b: Laid): Float {
        val w = minOf(a.x + a.w, b.x + b.w) - maxOf(a.x, b.x)
        val h = minOf(a.y + a.h, b.y + b.h) - maxOf(a.y, b.y)
        return if (w > 0 && h > 0) w * h else 0f
    }

    /** True if the two rectangles share an edge or corner. */
    private fun touchingHalves(a: Laid, b: Laid): Boolean {
        val gapX = maxOf(a.x, b.x) - minOf(a.x + a.w, b.x + b.w)
        val gapY = maxOf(a.y, b.y) - minOf(a.y + a.h, b.y + b.h)
        return gapX <= 0.001f && gapY <= 0.001f
    }

    @Test
    fun originTracksLeftPlays() {
        var b = Board().play(Move(Tile(6, 6), End.RIGHT))
        b = b.play(Move(Tile(6, 1), End.LEFT))
        b = b.play(Move(Tile(1, 2), End.LEFT))
        assertEquals(2, b.origin)
        assertEquals(Tile(6, 6), b.line[b.origin].tile)
    }

    @Test
    fun tilesNeverOverlapAndNeighboursTouch() {
        val rnd = Random(11)
        for (width in listOf(4f, 5f, 7f)) repeat(300) {
            var r = Round.deal(rnd)
            while (!r.isOver) {
                r = when {
                    r.canDraw -> r.draw().first
                    r.mustPass -> r.pass()
                    else -> r.play(r.legalMoves().random(rnd))
                }
                val laid = BoardLayout.layout(r.board, width)
                assertEquals(r.board.line.size, laid.size)
                for (i in laid.indices) for (j in i + 1 until laid.size) {
                    if (overlap(laid[i], laid[j]) > 0.001f) fail("Overlap ${laid[i]} / ${laid[j]} on ${r.board}")
                }
                // Tiles that follow each other on the line must touch on the table.
                val byTile = laid.associateBy { it.tile }
                r.board.line.zipWithNext().forEach { (a, b) ->
                    assertTrue("$a and $b don't touch", touchingHalves(byTile.getValue(a.tile), byTile.getValue(b.tile)))
                }
            }
        }
    }

    @Test
    fun layoutIsStableAsTilesAreAdded() {
        val rnd = Random(3)
        repeat(100) {
            var r = Round.deal(rnd)
            var before = emptyList<Laid>()
            while (!r.isOver) {
                r = when {
                    r.canDraw -> r.draw().first
                    r.mustPass -> r.pass()
                    else -> r.play(r.legalMoves().random(rnd))
                }
                val now = BoardLayout.layout(r.board).associateBy { it.tile }
                before.forEach { assertEquals(it, now[it.tile]) }
                before = now.values.toList()
            }
        }
    }

    @Test
    fun slotMatchesWhereTheTileLands() {
        val b = Board().play(Move(Tile(5, 5), End.RIGHT))
        val move = Move(Tile(5, 2), End.LEFT)
        val slot = BoardLayout.slot(b, move)
        assertEquals(-2.5f, slot.x)
        assertEquals(2, slot.first)
        assertEquals(5, slot.second)
    }
}
