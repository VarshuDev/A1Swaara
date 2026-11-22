package com.varshudev.a1swaara.cloud

import android.content.Context

data class SyncResult(
    val success: Boolean,
    val favoritesCount: Int = 0,
    val playlistsCount: Int = 0,
    val message: String
)

/**
 * The legacy Firestore library backup sent user data without a user credential.
 * Do not send or restore private data until an authenticated, owner-scoped
 * backend and its security rules are deployed. Existing remote data is untouched.
 */
object SwaaraCloudSync {
    private const val DISABLED_MESSAGE =
        "Legacy cloud backup and restore are unavailable until secure authentication is configured."

    suspend fun backup(context: Context): SyncResult = SyncResult(false, message = DISABLED_MESSAGE)

    suspend fun restore(context: Context): SyncResult = SyncResult(false, message = DISABLED_MESSAGE)

    fun scheduleAutoBackup(context: Context, delayMs: Long = 30000L) = Unit
}
