package de.wtzg.domino.feature.online

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.wtzg.domino.DominoApplication
import de.wtzg.domino.MainActivity
import de.wtzg.domino.R
import de.wtzg.domino.data.online.OnlineException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Checks the server now and then and posts a notification when it's your turn in an
 * online game. Uses no push service, so a turn can take up to ~15 minutes to show.
 */
class TurnCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as DominoApplication).container.online
        if (repo.account.first().registration == null) return Result.success()
        val games = try {
            repo.api().games()
        } catch (_: OnlineException) {
            return Result.retry()
        }
        val foreground = withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        val seen = repo.notified()
        val due = games.filter { it.isYourTurn && (seen[it.id] ?: -1) < it.version }
        if (!foreground) {
            due.forEach { g ->
                val others = g.seats.filterIndexed { i, _ -> i != g.yourSeat }.joinToString(", ")
                notify(g.id, "Your turn in ${g.mode.title}", "Playing with $others")
            }
        }
        // Forget finished games; remember what we've told you about.
        repo.setNotified(games.filter { it.isYourTurn }.associate { it.id to it.version })
        return Result.success()
    }

    private fun notify(gameId: String, title: String, text: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val open = Intent(Intent.ACTION_VIEW, Uri.parse("dominoes://game/$gameId"), ctx, MainActivity::class.java)
        val pending = PendingIntent.getActivity(ctx, gameId.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(ctx).notify(gameId.hashCode(), n)
    }

    companion object {
        private const val CHANNEL = "turns"
        private const val WORK = "turn-check"

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Your turn", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "When it's your move in an online game"
                },
            )
        }

        /** Runs the check every 15 minutes (Android's minimum) while online. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TurnCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
