package com.varshudev.a1swaara.cloud

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages Google Sign-In and anonymous device identity for A1 Swaara Cloud.
 * Bridges Supabase Auth sessions with local preferences.
 */
class SwaaraCloudAuth private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val tokenPrefs: SharedPreferences by lazy {
        val key = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext, "a1swaara_cloud_tokens", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { secure ->
            // Migrate tokens written by previous versions before deleting plaintext copies.
            val oldAccess = prefs.getString(KEY_ACCESS_TOKEN, null)
            val oldRefresh = prefs.getString(KEY_REFRESH_TOKEN, null)
            if (oldAccess != null || oldRefresh != null) {
                val edit = secure.edit()
                if (!secure.contains(KEY_ACCESS_TOKEN) && oldAccess != null) edit.putString(KEY_ACCESS_TOKEN, oldAccess)
                if (!secure.contains(KEY_REFRESH_TOKEN) && oldRefresh != null) edit.putString(KEY_REFRESH_TOKEN, oldRefresh)
                check(edit.commit()) { "Could not migrate cloud credentials" }
                check(prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).commit()) {
                    "Could not remove old cloud credentials"
                }
            }
        }
    }

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
        get() = tokenPrefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = tokenPrefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = tokenPrefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = tokenPrefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var sessionExpiresAt: Long
        get() = prefs.getLong(KEY_SESSION_EXPIRES_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_SESSION_EXPIRES_AT, value).apply()

    val isSignedIn: Boolean
        get() = !accessToken.isNullOrBlank() &&
            (sessionExpiresAt > System.currentTimeMillis() || !refreshToken.isNullOrBlank())

    private val refreshMutex = Mutex()

    /** Call from network operations; serializes refresh-token rotation across callers. */
    suspend fun validAccessToken(): String? = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val current = accessToken
            if (!current.isNullOrBlank() && !SessionExpiry.needsRefresh(sessionExpiresAt, System.currentTimeMillis())) {
                if (prefs.getBoolean(KEY_SESSION_VALIDATED, false)) return@withLock current
                // Older versions could save a Google ID token as if it were a
                // Supabase session. Validate migrated sessions before using them.
                val remoteUser = SwaaraSupabaseClient.getUser(current) ?: return@withLock null
                return@withLock synchronized(this@SwaaraCloudAuth) {
                    if (accessToken != current || remoteUser.optString("id") != userId) null
                    else {
                        prefs.edit().putBoolean(KEY_SESSION_VALIDATED, true).apply()
                        current
                    }
                }
            }
            val refresh = refreshToken?.takeIf { it.isNotBlank() } ?: return@withLock null
            val session = SwaaraSupabaseClient.refreshSession(refresh) ?: return@withLock null
            // Do not restore a session if the user signed out during the network request.
            synchronized(this@SwaaraCloudAuth) {
                if (refreshToken != refresh) return@synchronized null
                saveSession(session)
                session.accessToken
            }
        }
    }

    val isSignedInFlow: kotlinx.coroutines.flow.MutableStateFlow<Boolean> by lazy {
        kotlinx.coroutines.flow.MutableStateFlow(isSignedIn)
    }

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TS, value).apply()

    @Synchronized
    fun saveSession(session: SupabaseSession) {
        check(tokenPrefs.edit().putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken).commit()) { "Could not save cloud credentials" }
        prefs.edit()
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .putString(KEY_USER_EMAIL, session.user.email)
            .putString(KEY_AVATAR_URL, session.user.avatarUrl)
            .putLong(KEY_SESSION_EXPIRES_AT, System.currentTimeMillis() + (session.expiresIn * 1000L))
            .putBoolean(KEY_SESSION_VALIDATED, true)
            .apply()
        isSignedInFlow.value = true
    }

    @Synchronized
    fun signOut(): String {
        val newGuestId = generateNewUserId()
        tokenPrefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).commit()
        prefs.edit()
            .putString(KEY_USER_ID, newGuestId)
            .putString(KEY_DISPLAY_NAME, defaultDeviceName())
            .remove(KEY_USER_EMAIL)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_SESSION_EXPIRES_AT)
            .remove(KEY_SESSION_VALIDATED)
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
        private const val KEY_SESSION_VALIDATED = "cloud_session_validated"
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
