package de.wtzg.domino

import android.app.Application
import androidx.work.Configuration
import de.wtzg.domino.feature.online.TurnCheckWorker

/**
 * Initialises WorkManager on demand (see the manifest), so scheduling the turn checker
 * works no matter how the app was started.
 */
class DominoApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Cheap when you never play online: the worker returns at once without an account.
        TurnCheckWorker.schedule(this)
    }
}
