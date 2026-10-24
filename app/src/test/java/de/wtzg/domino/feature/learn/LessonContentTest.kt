package de.wtzg.domino.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonContentTest {
    @Test
    fun idsAreUniqueAndEveryCategoryHasLessons() {
        assertEquals(Lessons.all.size, Lessons.all.map { it.id }.toSet().size)
        LessonCategory.entries.forEach { c -> assertTrue("$c is empty", Lessons.all.any { it.category == c }) }
        Lessons.all.forEach { assertTrue("${it.id} has no content", it.blocks.isNotEmpty()) }
    }

    @Test
    fun everyTableIsALegalPosition() {
        for (lesson in Lessons.all) {
            for (block in lesson.blocks.filterIsInstance<LessonBlock.Table>()) {
                // Board.play throws on an illegal move.
                val board = runCatching { block.board }.getOrElse { throw AssertionError("${lesson.id}: ${it.message}") }
                val tiles = block.plays.map { it.tile }
                assertEquals("${lesson.id}: a tile is used twice", tiles.size, tiles.toSet().size)
                assertEquals(tiles.size, board.size)
            }
        }
    }

    @Test
    fun handsNeverRepeatATile() {
        for (lesson in Lessons.all) for (block in lesson.blocks.filterIsInstance<LessonBlock.Hand>()) {
            assertEquals(lesson.id, block.tiles.size, block.tiles.toSet().size)
        }
    }

    @Test
    fun tipsExist() {
        assertTrue(Lessons.tips.size >= 10)
    }
}
