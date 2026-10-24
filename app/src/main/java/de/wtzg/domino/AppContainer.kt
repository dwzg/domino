package de.wtzg.domino

import android.content.Context
import de.wtzg.domino.data.SavedGameRepository
import de.wtzg.domino.data.ScoreKeeperRepository
import de.wtzg.domino.data.SettingsRepository
import de.wtzg.domino.data.StatsRepository
import de.wtzg.domino.data.appStore
import de.wtzg.domino.data.scoreKeeperStore

/** Manual dependency injection: one place that builds the app's repositories. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val settings = SettingsRepository(app.appStore)
    val stats = StatsRepository(app.appStore)
    val savedGame = SavedGameRepository(app.appStore)
    val scoreKeeper = ScoreKeeperRepository(app.scoreKeeperStore)
}
