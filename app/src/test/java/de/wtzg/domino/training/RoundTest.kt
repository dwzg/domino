package de.wtzg.domino.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RoundTest {
    private fun t(a: Int, b: Int) = Tile(a, b)

    @Test
    fun setHas28TilesAndEachNumberSevenTimes() {
        assertEquals(28, Tile.SET.toSet().size)
        for (n in 0..6) assertEquals(7, Tile.SET.count { it.has(n) })
    }

    @Test
    fun dealGivesSevenEachAndHighestDoubleOpens() {
        repeat(50) { seed ->
            val r = Round.deal(Random(seed))
            assertEquals(7, r.hands[0].size)
            assertEquals(7, r.hands[1].size)
            assertEquals(14, r.boneyard.size)
            val open = r.opening!!
            assertTrue(open in r.hands[r.turn])
            assertEquals(listOf(Move(open, End.RIGHT)), r.legalMoves())
        }
    }

    @Test
    fun boardOrientsTiles() {
        var b = Board().play(Move(t(6, 6), End.RIGHT))
        b = b.play(Move(t(6, 3), End.RIGHT))
        b = b.play(Move(t(1, 6), End.LEFT))
        assertEquals(1, b.leftEnd)
        assertEquals(3, b.rightEnd)
        assertEquals(listOf(1 to 6, 6 to 6, 6 to 3), b.line.map { it.left to it.right })
    }

    @Test
    fun drawingRevealsMissingNumbers() {
        val r = Round(
            hands = listOf(listOf(t(1, 0)), listOf(t(2, 2), t(5, 4))),
            boneyard = listOf(t(6, 6)),
            turn = 0,
            opening = null,
            board = Board().play(Move(t(4, 3), End.RIGHT)),
        )
        assertTrue(r.canDraw)
        val (next, drawn) = r.draw()
        assertEquals(t(6, 6), drawn)
        assertEquals(setOf(4, 3), next.lacks[0])
        assertTrue(Strategy.opponentLacks(next, 1).containsAll(setOf(3, 4)))
    }

    @Test
    fun blockedGoesToLighterHand() {
        var r = Round(
            hands = listOf(listOf(t(1, 0)), listOf(t(6, 5))),
            boneyard = emptyList(),
            turn = 0,
            opening = null,
            board = Board().play(Move(t(3, 2), End.RIGHT)),
        )
        r = r.pass()
        assertNull(r.result)
        r = r.pass()
        val res = r.result!!
        assertTrue(res.blocked)
        assertEquals(0, res.winner)
        assertEquals(11, res.points)
    }

    @Test
    fun goingOutScoresOpponentPips() {
        val r = Round(
            hands = listOf(listOf(t(3, 1)), listOf(t(6, 5), t(2, 0))),
            boneyard = emptyList(),
            turn = 0,
            opening = null,
            board = Board().play(Move(t(3, 3), End.RIGHT)),
        ).play(Move(t(3, 1), End.LEFT))
        assertEquals(HandResult(0, 13, false, listOf(0, 13)), r.result)
    }

    @Test
    fun coachPrefersBlockingWhenOpponentIsStuck() {
        // Ends are 5 and 1; the opponent has shown it has no 5s or 2s.
        // Playing 2|1 on the 1 makes the ends 5 and 2, so the opponent is stuck.
        val r = Round(
            hands = listOf(listOf(t(2, 1), t(6, 1), t(0, 0)), List(4) { t(4, 4) }),
            boneyard = List(5) { t(0, 0) },
            turn = 0,
            opening = null,
            board = Board(listOf(Placed(t(5, 1), 5, 1))),
            lacks = listOf(emptySet(), setOf(5, 2)),
        )
        val best = Strategy.evaluateAll(r).first()
        assertEquals(t(2, 1), best.move.tile)
        assertTrue(best.pros.first().contains("must draw or pass"))
    }

    @Test
    fun coachPlaysLastTile() {
        val r = Round(
            hands = listOf(listOf(t(0, 1)), listOf(t(4, 4))),
            boneyard = emptyList(),
            turn = 0,
            opening = null,
            board = Board().play(Move(t(1, 1), End.RIGHT)),
        )
        assertEquals(t(0, 1), Strategy.chooseMove(r, Difficulty.HARD)!!.tile)
    }

    @Test
    fun aiVsAiGamesAlwaysFinish() {
        val rnd = Random(7)
        repeat(300) { i ->
            var r = Round.deal(rnd)
            var steps = 0
            while (!r.isOver) {
                val diff = Difficulty.entries[(i + r.turn) % 3]
                r = when {
                    r.canDraw -> r.draw().first
                    r.mustPass -> r.pass()
                    else -> r.play(Strategy.chooseMove(r, diff, rnd)!!)
                }
                assertTrue(++steps < 200)
            }
            val res = r.result
            assertNotNull(res)
            // All 28 tiles are accounted for.
            assertEquals(28, r.hands.flatten().size + r.boneyard.size + r.board.line.size)
            assertFalse(res!!.points < 0)
        }
    }

    private fun pointsOver(hands: Int, p0: Difficulty, p1: Difficulty, seed: Int): Pair<Int, Int> {
        val rnd = Random(seed)
        val pts = intArrayOf(0, 0)
        repeat(hands) {
            var r = Round.deal(rnd)
            while (!r.isOver) {
                r = when {
                    r.canDraw -> r.draw().first
                    r.mustPass -> r.pass()
                    else -> r.play(Strategy.chooseMove(r, if (r.turn == 0) p0 else p1, rnd)!!)
                }
            }
            r.result!!.winner?.let { pts[it] += r.result!!.points }
        }
        return pts[0] to pts[1]
    }

    @Test
    fun hardBeatsEasy() {
        val (hard, easy) = pointsOver(400, Difficulty.HARD, Difficulty.EASY, 1)
        println("Hard $hard vs Easy $easy")
        assertTrue(hard > easy)
    }

    @Test
    fun hardBeatsNormal() {
        val (hard, normal) = pointsOver(1000, Difficulty.HARD, Difficulty.NORMAL, 2)
        println("Hard $hard vs Normal $normal")
        assertTrue(hard > normal)
    }
}
