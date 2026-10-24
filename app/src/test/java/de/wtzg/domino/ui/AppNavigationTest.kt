package de.wtzg.domino.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import de.wtzg.domino.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AppNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsAGameFromThePlayTab() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("All Fives").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("All Fives").performClick()
        rule.onNodeWithText("Start game").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Hint").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("All Fives · to 100").assertIsDisplayed()
    }

    @Test
    fun everyTabOpens() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Learn").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Learn").onFirst().performClick()
        rule.onNodeWithText("Puzzles: find the best move").assertIsDisplayed()
        rule.onNodeWithText("Scores").performClick()
        rule.onNodeWithText("Score keeper").assertIsDisplayed()
        rule.onNodeWithText("More").performClick()
        rule.onNodeWithText("Your stats").assertIsDisplayed()
    }
}
