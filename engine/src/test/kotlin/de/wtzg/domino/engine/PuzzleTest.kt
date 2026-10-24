package de.wtzg.domino.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleTest {
    @Test
    fun puzzlesHaveOneClearAnswer() {
        val rnd = Random(4)
        for (mode in GameMode.entries) repeat(15) {
            val p = Puzzles.generate(mode, rnd)
            assertEquals(0, p.round.turn)
            assertTrue(p.isGood(p.best.move))
            assertTrue(p.best.pros.isNotEmpty())
            val wrong = p.ranked.first { it.move.tile != p.best.move.tile }
            assertFalse(p.isGood(wrong.move))
        }
    }
}
