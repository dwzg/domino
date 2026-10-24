package de.wtzg.domino.engine

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
    private fun touching(a: Laid, b: Laid): Boolean {
        val gapX = maxOf(a.x, b.x) - minOf(a.x + a.w, b.x + b.w)
        val gapY = maxOf(a.y, b.y) - minOf(a.y + a.h, b.y + b.h)
        return gapX <= 0.001f && gapY <= 0.001f
    }

    private fun chains(board: Board): List<List<Tile>> {
        val s = board.start ?: return emptyList()
        return Arm.entries.map { arm -> listOf(s) + board.arm(arm).map { it.tile } }
    }

    @Test
    fun tilesNeverOverlapAndNeighboursTouch() {
        val rnd = Random(11)
        for (mode in GameMode.entries) for (players in 2..4) for (width in listOf(4f, 5f, 7f)) repeat(40) {
            playOut(Round.deal(Rules(mode, players), rnd), rnd, { Difficulty.EASY }) { r ->
                val laid = BoardLayout.layout(r.board, width)
                assertEquals(r.board.size, laid.size)
                for (i in laid.indices) for (j in i + 1 until laid.size) {
                    if (overlap(laid[i], laid[j]) > 0.001f) fail("$mode: overlap ${laid[i]} / ${laid[j]}")
                }
                val byTile = laid.associateBy { it.tile }
                chains(r.board).forEach { chain ->
                    chain.zipWithNext().forEach { (a, b) ->
                        assertTrue("$mode: $a and $b don't touch", touching(byTile.getValue(a), byTile.getValue(b)))
                    }
                }
            }
        }
    }

    @Test
    fun shownNumbersMatchTheBoard() {
        val rnd = Random(5)
        repeat(100) {
            val r = playOut(Round.deal(Rules(GameMode.ALL_FIVES, 2), rnd), rnd)
            val laid = BoardLayout.layout(r.board).associateBy { it.tile }
            for (arm in Arm.entries) for (link in r.board.arm(arm)) {
                val l = laid.getValue(link.tile)
                assertEquals(setOf(link.inner, link.outer), setOf(l.first, l.second))
            }
        }
    }

    @Test
    fun layoutIsStableAsTilesAreAdded() {
        val rnd = Random(3)
        for (mode in listOf(GameMode.DRAW, GameMode.ALL_FIVES)) repeat(60) {
            var before = emptyList<Laid>()
            playOut(Round.deal(Rules(mode), rnd), rnd) { r ->
                val now = BoardLayout.layout(r.board).associateBy { it.tile }
                before.forEach { assertEquals(it, now[it.tile]) }
                before = now.values.toList()
            }
        }
    }

    @Test
    fun slotMatchesWhereTheTileLands() {
        val b = Board().play(Move(t(5, 5), Arm.EAST))
        val slot = BoardLayout.slot(b, Move(t(5, 2), Arm.WEST))
        assertEquals(-2.5f, slot.x)
        assertEquals(2, slot.first)
        assertEquals(5, slot.second)
    }
}
