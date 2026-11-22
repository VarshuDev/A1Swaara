package com.varshudev.a1swaara.cloud

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.varshudev.a1swaara.BuildConfig
import com.varshudev.a1swaara.utils.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLDecoder

/**
 * Handles Direct In-App Google Sign-In via Android Credential Manager.
 * Features native system account picker bottom sheet with zero WebViews
 * and zero browser redirects.
 */
object SwaaraGoogleAuthManager {
    private const val TAG = "SwaaraGoogleAuth"

    // Retained for backward binary compatibility
    val isSignInPopupVisible = mutableStateOf(false)

    fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun openSignInPopup() {
        isSignInPopupVisible.value = false
    }

    fun closeSignInPopup() {
        isSignInPopupVisible.value = false
    }

    /**
     * Launches native Android Google Sign-In account picker bottom sheet.
     * Uses Android Credential Manager with Google ID Token credentials.
     * Zero WebViews, zero external browser redirection.
     */
    suspend fun launchGoogleSignIn(context: Context): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        val activity = context.findActivity()
        if (activity == null) {
            Log.e(TAG, "Cannot launch Google Sign-In: Context is not an Activity.")
            return@withContext Result.failure(IllegalStateException("Activity context required"))
        }

        val primaryClientId = BuildConfig.SWAARA_GOOGLE_WEB_CLIENT_ID
        val candidateClientIds = listOf(
            primaryClientId,
            "802934415744-ssl232avsu163ae84g51umbjemuj0o3h.apps.googleusercontent.com",
            "899397486076-7opo34vsigm6pk02judsj3qi4j3foaq3.apps.googleusercontent.com"
        ).distinct().filter { it.isNotBlank() }

        val credentialManager = CredentialManager.create(activity)
        var lastException: Exception? = null

        for (clientId in candidateClientIds) {
            try {
                Log.d(TAG, "Requesting native Google Credential with serverClientId: $clientId")

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                var idToken: String? = null
                var credEmail: String? = null

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    idToken = googleIdTokenCredential.idToken
                    credEmail = googleIdTokenCredential.id
                }

                if (!idToken.isNullOrBlank()) {
                    Log.i(TAG, "Native Google ID token retrieved successfully for $credEmail")

                    val session = SwaaraSupabaseClient.signInWithGoogleIdToken(idToken)
                    if (session == null) {
                        lastException = IllegalStateException("Could not authenticate with Supabase. Please try again.")
                        break
                    }

                    // 3. Save session in local auth manager
                    val auth = SwaaraCloudAuth.getInstance(activity)
                    auth.saveSession(session)

                    // 5. Trigger cloud sync (playlists, favorites, top listener)
                    SwaaraAccountSyncEngine.triggerSync(activity)

                    withContext(Dispatchers.Main) {
                        activity.toast("Welcome, ${session.user.displayName}!")
                    }
                    return@withContext Result.success(session.user)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User dismissed Google Sign-In account picker.")
                return@withContext Result.failure(e)
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credentials available on device: ${e.message}")
                lastException = e
                break
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("cancel", ignoreCase = true) ||
                    msg.contains("16:") ||
                    msg.contains("USER_CANCELED")
                ) {
                    Log.d(TAG, "User dismissed Google Sign-In account picker.")
                    return@withContext Result.failure(e)
                }
                Log.w(TAG, "Credential Manager error with client ID $clientId: ${e.message}")
                lastException = e
            }
        }

        withContext(Dispatchers.Main) {
            val errMsg = lastException?.message ?: "Sign-in could not be completed"
            if (!errMsg.contains("cancel", ignoreCase = true)) {
                activity.toast("Google Sign-In: $errMsg")
            }
        }

        Result.failure(lastException ?: IllegalStateException("Google Sign-In failed"))
    }

    /**
     * Launches native in-app sign in directly without opening any browser.
     */
    fun launchBrowserGoogleSignIn(context: Context) {
        val activity = context.findActivity() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            launchGoogleSignIn(activity)
        }
    }

    /**
     * Extracts accessToken, refreshToken, and expiresIn from URL fragments or query parameters.
     */
    fun extractTokensFromUrl(url: String): Triple<String, String, Long>? {
        try {
            val fragmentIndex = url.indexOf('#')
            val queryIndex = url.indexOf('?')
            val paramString = when {
                fragmentIndex != -1 -> url.substring(fragmentIndex + 1)
                queryIndex != -1 -> url.substring(queryIndex + 1)
                else -> return null
            }

            val params = paramString.split('&').associate { param ->
                val parts = param.split('=', limit = 2)
                if (parts.size == 2) {
                    val key = parts[0]
                    val value = try {
                        URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        parts[1]
                    }
                    key to value
                } else {
                    parts[0] to ""
                }
            }

            val accessToken = params["access_token"] ?: return null
            val refreshToken = params["refresh_token"] ?: ""
            val expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600L

            return Triple(accessToken, refreshToken, expiresIn)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OAuth callback", e)
            return null
        }
    }

    /**
     * Decodes the JWT payload locally for instant zero-latency user identity extraction.
     */
    fun parseUserFromJwt(accessToken: String): SupabaseUser? {
        return try {
            val parts = accessToken.split('.')
            if (parts.size < 2) return null
            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            val jsonStr = String(decodedBytes, Charsets.UTF_8)
            val json = JSONObject(jsonStr)

            val userId = json.optString("sub").ifEmpty { json.optString("id") }
            if (userId.isEmpty()) return null

            val email = json.optString("email").ifEmpty { null }
            val metaObj = json.optJSONObject("user_metadata")
            val displayName = metaObj?.optString("full_name")
                ?: metaObj?.optString("name")
                ?: json.optString("name").ifEmpty { null }
                ?: email?.substringBefore('@')
                ?: "Swaara Listener"
            val avatarUrl = metaObj?.optString("avatar_url")
                ?: metaObj?.optString("picture")
                ?: json.optString("picture").ifEmpty { null }

            SupabaseUser(
                id = userId,
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl
            )
        } catch (e: Exception) {
            Log.w(TAG, "JWT payload decoding failed: ${e.message}")
            null
        }
    }

    /**
     * Processes intercepted OAuth tokens, constructs SupabaseSession, saves it,
     * and triggers cloud sync across the app.
     */
    suspend fun processOAuthTokens(
        context: Context,
        accessToken: String,
        refreshToken: String,
        expiresIn: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // A decoded JWT is not proof of authentication. Validate with Supabase.
            val userJson = SwaaraSupabaseClient.getUser(accessToken) ?: return@withContext false
            val userId = userJson.optString("id")
            if (userId.isBlank()) return@withContext false
            val email = if (userJson.has("email") && !userJson.isNull("email")) userJson.getString("email") else null
            val meta = userJson.optJSONObject("user_metadata")
            val user = SupabaseUser(
                id = userId,
                email = email,
                displayName = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                    ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore('@') ?: "Swaara Listener",
                avatarUrl = meta?.optString("avatar_url")?.takeIf { it.isNotBlank() }
                    ?: meta?.optString("picture")?.takeIf { it.isNotBlank() }
            )

            val session = SupabaseSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                user = user
            )

            val auth = SwaaraCloudAuth.getInstance(context)
            auth.saveSession(session)
            SwaaraAccountSyncEngine.triggerSync(context)
            Log.i(TAG, "Successfully authenticated user ${user.displayName} (${user.id})")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "OAuth token processing failed", e)
            false
        }
    }

    /**
     * Handles redirect deep links: swaara://auth-callback#access_token=...&refresh_token=...
     */
    suspend fun handleOAuthCallback(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val tokens = extractTokensFromUrl(uri.toString()) ?: return@withContext false
        processOAuthTokens(context, tokens.first, tokens.second, tokens.third)
    }

    fun signOut(context: Context) {
        val auth = SwaaraCloudAuth.getInstance(context)
        auth.signOut()
    }
}
