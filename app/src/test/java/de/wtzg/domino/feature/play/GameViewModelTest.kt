package de.wtzg.domino.feature.play

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import de.wtzg.domino.data.GameOptions
import de.wtzg.domino.data.SavedGameRepository
import de.wtzg.domino.data.SettingsRepository
import de.wtzg.domino.data.StatsRepository
import de.wtzg.domino.engine.Difficulty
import de.wtzg.domino.engine.GameMode
import de.wtzg.domino.engine.Rules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var storeScope: CoroutineScope

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        storeScope = CoroutineScope(dispatcher + SupervisorJob())
    }

    @After fun tearDown() {
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun playsAWholeMatchAgainstTheAiAndKeepsStats() = runTest(dispatcher) {
        val store = PreferenceDataStoreFactory.create(scope = storeScope) { tmp.newFile("app.preferences_pb") }
        val saved = SavedGameRepository(store)
        val stats = StatsRepository(store)
        val vm = GameViewModel(saved, stats, SettingsRepository(store))

        vm.start(Rules(GameMode.ALL_FIVES, players = 3, target = 30), GameOptions(Difficulty.HARD, coach = true))
        advanceUntilIdle()
        assertNotNull("game is saved", saved.load())

        var steps = 0
        while (vm.state.value.match?.isOver == false) {
            val s = vm.state.value
            val r = s.round!!
            when {
                r.isOver -> vm.nextHand()
                s.yourTurn && r.canDraw -> vm.draw()
                s.yourTurn && r.mustPass -> vm.pass()
                s.yourTurn -> vm.playMove(r.legalMoves().first())
            }
            advanceUntilIdle()
            assertTrue("match never ends", ++steps < 2_000)
        }
        val m = vm.state.value.match!!
        assertTrue(m.scores[m.winner!!] >= 30)
        val st = stats.stats.first().mode(GameMode.ALL_FIVES)
        assertEquals(1, st.matches)
        assertTrue(st.handsWon + st.handsLost >= 1 || !m.round.isOver)
        assertNull("finished matches aren't kept for resuming", saved.load())
    }
}
