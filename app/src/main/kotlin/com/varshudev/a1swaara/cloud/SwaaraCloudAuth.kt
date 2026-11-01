package com.varshudev.a1swaara.cloud

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.UUID

/**
 * Manages Google Sign-In and anonymous device identity for A1 Swaara Cloud.
 * Bridges Supabase Auth sessions with local preferences.
 */
class SwaaraCloudAuth private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var userId: String
        get() {
            var id = prefs.getString(KEY_USER_ID, null)
            if (id.isNullOrBlank()) {
                id = generateNewUserId()
                prefs.edit().putString(KEY_USER_ID, id).apply()
            }
            return id
        }
        set(value) {
            prefs.edit().putString(KEY_USER_ID, value.trim()).apply()
        }

    var displayName: String
        get() = prefs.getString(KEY_DISPLAY_NAME, defaultDeviceName()) ?: defaultDeviceName()
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value.trim()).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value?.trim()).apply()

    var avatarUrl: String?
        get() = prefs.getString(KEY_AVATAR_URL, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_URL, value?.trim()).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var sessionExpiresAt: Long
        get() = prefs.getLong(KEY_SESSION_EXPIRES_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_SESSION_EXPIRES_AT, value).apply()

    val isSignedIn: Boolean
        get() = !accessToken.isNullOrBlank() && !userEmail.isNullOrBlank()

    val isSignedInFlow: kotlinx.coroutines.flow.MutableStateFlow<Boolean> by lazy {
        kotlinx.coroutines.flow.MutableStateFlow(isSignedIn)
    }

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TS, value).apply()

    fun saveSession(session: SupabaseSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .putString(KEY_USER_EMAIL, session.user.email)
            .putString(KEY_AVATAR_URL, session.user.avatarUrl)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_SESSION_EXPIRES_AT, System.currentTimeMillis() + (session.expiresIn * 1000L))
            .apply()
        isSignedInFlow.value = true
    }

    fun signOut(): String {
        val newGuestId = generateNewUserId()
        prefs.edit()
            .putString(KEY_USER_ID, newGuestId)
            .putString(KEY_DISPLAY_NAME, defaultDeviceName())
            .remove(KEY_USER_EMAIL)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_SESSION_EXPIRES_AT)
            .putLong(KEY_LAST_SYNC_TS, 0L)
            .apply()
        isSignedInFlow.value = false
        return newGuestId
    }

    fun linkAccount(targetUserId: String) {
        if (targetUserId.isNotBlank()) {
            userId = targetUserId.trim()
        }
    }

    fun resetToNewAccount(): String {
        return signOut()
    }

    private fun generateNewUserId(): String {
        return UUID.randomUUID().toString()
    }

    private fun isValidUuid(str: String): Boolean {
        return runCatching { UUID.fromString(str) }.isSuccess
    }

    private fun defaultDeviceName(): String {
        val model = Build.MODEL ?: "Android"
        return "Swaara ($model)"
    }

    companion object {
        private const val PREFS_NAME = "a1swaara_cloud_auth_prefs"
        private const val KEY_USER_ID = "cloud_user_id"
        private const val KEY_DISPLAY_NAME = "cloud_display_name"
        private const val KEY_USER_EMAIL = "cloud_user_email"
        private const val KEY_AVATAR_URL = "cloud_avatar_url"
        private const val KEY_ACCESS_TOKEN = "cloud_access_token"
        private const val KEY_REFRESH_TOKEN = "cloud_refresh_token"
        private const val KEY_SESSION_EXPIRES_AT = "cloud_session_expires_at"
        private const val KEY_AUTO_SYNC = "cloud_auto_sync"
        private const val KEY_LAST_SYNC_TS = "cloud_last_sync_ts"

        @Volatile
        private var INSTANCE: SwaaraCloudAuth? = null

        fun getInstance(context: Context): SwaaraCloudAuth {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SwaaraCloudAuth(context).also { INSTANCE = it }
            }
        }
    }
}
