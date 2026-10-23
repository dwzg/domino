package de.wtzg.domino.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    private fun game(mode: GameMode = GameMode.FIRST_TO_TARGET, limit: Int = 100, players: Int = 2) =
        Game(id = 1, players = List(players) { "P$it" }, mode = mode, limit = limit, startedAt = 0)

    @Test
    fun totalsSumRounds() {
        val g = game().addRound(listOf(10, 0)).addRound(listOf(5, 20))
        assertEquals(listOf(15, 20), g.totals)
        assertEquals(1, g.leaderIndex)
    }

    @Test
    fun highestWinsOnceTargetReached() {
        var g = game(limit = 50).addRound(listOf(45, 0))
        assertFalse(g.isFinished)
        g = g.addRound(listOf(10, 0))
        assertEquals(0, g.winnerIndex)
    }

    @Test
    fun tieAtTargetKeepsGameGoing() {
        val g = game(limit = 50).addRound(listOf(50, 50))
        assertNull(g.winnerIndex)
        assertEquals(1, g.addRound(listOf(0, 5)).winnerIndex)
    }

    @Test
    fun lowestWinsAfterRounds() {
        var g = game(mode = GameMode.LOWEST_AFTER_ROUNDS, limit = 2, players = 3)
        g = g.addRound(listOf(10, 3, 7))
        assertFalse(g.isFinished)
        g = g.addRound(listOf(0, 9, 1))
        assertEquals(2, g.winnerIndex)
    }

    @Test
    fun editAndDeleteRounds() {
        var g = game().addRound(listOf(10, 0)).addRound(listOf(0, 10))
        g = g.updateRound(0, listOf(30, 0))
        assertEquals(listOf(30, 10), g.totals)
        g = g.deleteRound(1)
        assertEquals(listOf(30, 0), g.totals)
    }

    @Test
    fun missingScoresCountAsZero() {
        val g = game(players = 3).addRound(listOf(5))
        assertEquals(listOf(5, 0, 0), g.rounds.single())
    }

    @Test
    fun jsonRoundTrip() {
        val g = game(mode = GameMode.LOWEST_AFTER_ROUNDS, limit = 13, players = 4)
            .addRound(listOf(1, 2, 3, 4))
            .copy(finishedAt = 42)
        assertEquals(g, Game.fromJson(g.toJson()))
        assertTrue(Game.fromJson(game().toJson()).finishedAt == null)
    }
}
