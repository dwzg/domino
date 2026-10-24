package de.wtzg.domino.training

sealed interface LessonBlock {
    data class Text(val text: String) : LessonBlock
    data class Tiles(val tiles: List<Tile>, val caption: String) : LessonBlock
    data class Tip(val text: String) : LessonBlock

    /** Tiles played in order onto a line (first one opens), shown as on the table. */
    data class Line(val tiles: List<Tile>, val caption: String) : LessonBlock {
        val board: Board
            get() = tiles.fold(Board()) { b, t -> b.play(Move(t, End.RIGHT)) }
    }
}

data class Lesson(val title: String, val summary: String, val blocks: List<LessonBlock>)

private fun t(a: Int, b: Int) = Tile(a, b)

object Lessons {
    val all: List<Lesson> = listOf(
        Lesson(
            "1. How Draw dominoes works",
            "The rules you'll play against the AI.",
            listOf(
                LessonBlock.Text(
                    "A double-six set has 28 tiles. Each number from 0 to 6 appears on exactly 7 tiles. " +
                        "Each player gets 7 tiles and the other 14 go face down into the boneyard."
                ),
                LessonBlock.Text(
                    "The highest double opens the first hand. After that, the winner of the previous hand leads " +
                        "with any tile. On your turn, play a tile that matches one of the two open ends."
                ),
                LessonBlock.Line(listOf(t(6, 6), t(6, 3), t(3, 1)), "6|6 opened, then 6|3, then 3|1. Touching halves always match. The open ends are 6 and 1."),
                LessonBlock.Text(
                    "If nothing fits, draw from the boneyard until something does. If the boneyard is empty, you pass. " +
                        "When both players pass in a row, the game is blocked."
                ),
                LessonBlock.Text(
                    "Go out first and you score the pips left in your opponent's hand. In a blocked game, " +
                        "the player with fewer pips wins and scores the opponent's pips. The first to the target score wins the match."
                ),
                LessonBlock.Tip("Every draw is information for your opponent. Lesson 6 shows how to use it."),
            ),
        ),
        Lesson(
            "2. Count your pips",
            "Why heavy tiles are a liability.",
            listOf(
                LessonBlock.Text(
                    "When the hand ends, the tiles left in the loser's hand score for the winner. " +
                        "A 6|5 left in your hand gives away 11 points. A 1|0 gives away only 1."
                ),
                LessonBlock.Tiles(listOf(t(6, 5), t(5, 5), t(1, 0)), "Holding 6|5 and 5|5 at the end costs you 21 points. 1|0 costs 1."),
                LessonBlock.Text(
                    "When you have a choice between equally good moves, play the heavier tile. " +
                        "This matters most when your opponent is down to two or three tiles."
                ),
                LessonBlock.Tip("Light tiles make good emergency exits late in the hand. Don't rush to play 0|0 or 1|0."),
            ),
        ),
        Lesson(
            "3. Play your doubles early",
            "Doubles get stranded.",
            listOf(
                LessonBlock.Text(
                    "A normal tile like 5|2 fits a 5 or a 2. A double like 5|5 fits only a 5. " +
                        "If the other 5s get played before you place it, the double is stuck in your hand."
                ),
                LessonBlock.Tiles(listOf(t(5, 5), t(5, 2)), "5|5 needs a 5 end. 5|2 can go on a 5 or a 2."),
                LessonBlock.Text(
                    "Heavy doubles like 6|6 and 5|5 are the worst to get stuck with: they're hard to place and worth a lot of points. " +
                        "When a 6 or 5 end is open and you hold that double, playing it is usually right."
                ),
                LessonBlock.Tip("A double also keeps the same number open, so if you hold more of that number you keep control."),
            ),
        ),
        Lesson(
            "4. Keep your options open",
            "Always think one turn ahead.",
            listOf(
                LessonBlock.Text(
                    "Before you play, look at what the open ends will be afterwards. Ask yourself whether you'll still have a tile that fits. " +
                        "If not, you'll probably have to draw next turn, which costs time and tells your opponent what you lack."
                ),
                LessonBlock.Tiles(
                    listOf(t(4, 2), t(2, 6), t(4, 1)),
                    "Ends 4 and 3. You hold 4|2, 2|6 and 4|1. Playing 4|2 leaves ends 2 and 3, and 2|6 still fits. Playing 4|1 leaves 1 and 3, and nothing fits.",
                ),
                LessonBlock.Text(
                    "A hand with many different numbers is flexible. A hand with only two numbers is fragile. " +
                        "Play tiles in a way that keeps variety in your hand."
                ),
                LessonBlock.Tip("The coach penalises moves that leave you with nothing to play next turn."),
            ),
        ),
        Lesson(
            "5. Control a number",
            "Make the ends your numbers.",
            listOf(
                LessonBlock.Text(
                    "Each number appears on 7 tiles. If you hold three or four of them, your opponent has few or none. " +
                        "Opening an end with that number gives you plays your opponent can't match."
                ),
                LessonBlock.Tiles(
                    listOf(t(3, 0), t(3, 2), t(3, 5), t(3, 3)),
                    "Four 3s in your hand. Opening a 3 end is good for you.",
                ),
                LessonBlock.Text(
                    "Count what you see. Your own tiles plus the board tell you how many of each number are still hidden. " +
                        "If only one 3 is unaccounted for, a 3 end is very likely to stay yours."
                ),
                LessonBlock.Tip("Strong players keep a running count of each number as tiles hit the board."),
            ),
        ),
        Lesson(
            "6. Read your opponent",
            "Draws and passes give secrets away.",
            listOf(
                LessonBlock.Text(
                    "If your opponent draws or passes, they hold nothing that matches either open end. " +
                        "If the ends were 4 and 1, they have no 4s and no 1s."
                ),
                LessonBlock.Text(
                    "Use it. Try to keep a 4 or a 1 on the ends. If both ends show numbers they lack, " +
                        "they must draw again or pass while you keep playing."
                ),
                LessonBlock.Line(listOf(t(4, 4), t(4, 1)), "The AI drew when the ends were 4 and 1. Keep 4s and 1s showing."),
                LessonBlock.Text(
                    "This knowledge goes stale when they draw. A drawn tile might bring back a number. " +
                        "Only the numbers that made them draw are sure."
                ),
                LessonBlock.Tip("The practice screen tells you what the AI just revealed. Watch for those messages."),
            ),
        ),
        Lesson(
            "7. Count to block",
            "When all 7 of a number are seen, nobody can play it.",
            listOf(
                LessonBlock.Text(
                    "If you can see all 7 tiles of a number (in your hand and on the board), your opponent can't have any. " +
                        "Make both ends that number, or one of those and a number they lack, and they're locked out."
                ),
                LessonBlock.Text(
                    "Remember the double counts as one tile. The 7 tiles with a 2 are 2|0, 2|1, 2|2, 2|3, 2|4, 2|5 and 2|6."
                ),
                LessonBlock.Tiles(listOf(t(2, 0), t(2, 1), t(2, 2), t(2, 3), t(2, 4), t(2, 5), t(2, 6)), "All seven 2s."),
                LessonBlock.Tip("Before blocking the whole game, compare pips: a blocked game goes to the lighter hand."),
            ),
        ),
        Lesson(
            "8. The endgame",
            "Two tiles left changes everything.",
            listOf(
                LessonBlock.Text(
                    "When your opponent has one or two tiles left, stop thinking about your own hand and start defending. " +
                        "Close ends they might use and open numbers they've shown they don't have."
                ),
                LessonBlock.Text(
                    "When you're the one about to go out, plan your last two tiles. Make sure the last one can still be played. " +
                        "Keep it on a number you control."
                ),
                LessonBlock.Text(
                    "If the boneyard is empty, every tile is in one of the two hands. You can work out exactly what your opponent " +
                        "holds by elimination and play perfectly."
                ),
                LessonBlock.Tip("Use the Hint button in tight endgames and compare the coach's idea with your own."),
            ),
        ),
    )

    val tips: List<String> = all.flatMap { l -> l.blocks.filterIsInstance<LessonBlock.Tip>().map { it.text } } + listOf(
        "Look at the ends after your move, not just before it.",
        "When ahead in a match, play safe. When behind, take chances to block.",
        "Each number appears on exactly 7 tiles. Keep counting.",
    )
}
