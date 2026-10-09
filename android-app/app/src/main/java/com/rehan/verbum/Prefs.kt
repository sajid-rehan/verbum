package com.rehan.verbum

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Prefs {
    private const val FILE = "verbum_prefs"
    private const val KEY_OWNER = "github_owner"
    private const val KEY_REPO = "github_repo"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_LAST_SYNC = "last_sync_date"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        FILE,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(ctx: Context, owner: String, repo: String, token: String) {
        prefs(ctx).edit()
            .putString(KEY_OWNER, owner)
            .putString(KEY_REPO, repo)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun isConfigured(ctx: Context): Boolean {
        val p = prefs(ctx)
        return !p.getString(KEY_TOKEN, null).isNullOrBlank()
    }

    fun owner(ctx: Context): String = prefs(ctx).getString(KEY_OWNER, "") ?: ""
    fun repo(ctx: Context): String = prefs(ctx).getString(KEY_REPO, "") ?: ""
    fun token(ctx: Context): String = prefs(ctx).getString(KEY_TOKEN, "") ?: ""

    fun lastSyncDate(ctx: Context): String = prefs(ctx).getString(KEY_LAST_SYNC, "") ?: ""
    fun setLastSyncDate(ctx: Context, date: String) {
        prefs(ctx).edit().putString(KEY_LAST_SYNC, date).apply()
    }
}
