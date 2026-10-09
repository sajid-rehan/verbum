package com.rehan.verbum

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "SyncWorker"
private const val WORK_NAME = "verbum_daily_sync"

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ctx = applicationContext
            val force = inputData.getBoolean(KEY_FORCE, false)
            val client = GithubClient(
                owner = Prefs.owner(ctx),
                repo = Prefs.repo(ctx),
                token = Prefs.token(ctx),
            )

            Log.d(TAG, "Fetching manifest…")
            val manifest = client.fetchManifest()
            val today = manifest.date

            if (!force && Prefs.lastSyncDate(ctx) == today) {
                Log.d(TAG, "Already synced for $today, skipping")
                return@withContext Result.success()
            }

            GalleryManager.deleteAllAlbumImages(ctx)

            manifest.cards.forEachIndexed { i, card ->
                Log.d(TAG, "Downloading ${i + 1}/${manifest.cards.size}: ${card.word}")
                val bytes = client.fetchImage(card.file)
                GalleryManager.saveImage(ctx, bytes, card.file, albumIndex = i)
            }

            Prefs.setLastSyncDate(ctx, today)
            Log.d(TAG, "Sync complete for $today")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val KEY_FORCE = "force"

        fun forceSync(ctx: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(Data.Builder().putBoolean(KEY_FORCE, true).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueue(request)
        }

        fun schedule(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
