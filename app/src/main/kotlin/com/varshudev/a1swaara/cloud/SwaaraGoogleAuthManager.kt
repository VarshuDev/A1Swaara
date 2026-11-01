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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.varshudev.a1swaara.BuildConfig
import com.varshudev.a1swaara.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLDecoder

/**
 * Handles Direct Google Sign-In via Android Credential Manager One-Tap
 * and falls back seamlessly to the In-App Popup Dialog.
 * Zero external browser redirection.
 */
object SwaaraGoogleAuthManager {
    private const val TAG = "SwaaraGoogleAuth"

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
        isSignInPopupVisible.value = true
    }

    fun closeSignInPopup() {
        isSignInPopupVisible.value = false
    }

    /**
     * Attempts native Google One-Tap bottom sheet credential manager first.
     * If user cancels or if Play Services is unavailable, smoothly falls back to
     * the in-app popup dialog (zero external browser redirection).
     */
    suspend fun launchGoogleSignIn(context: Context): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        val activity = context.findActivity()

        if (activity != null) {
            try {
                val credentialManager = CredentialManager.create(activity)
                val serverClientId = BuildConfig.SWAARA_GOOGLE_WEB_CLIENT_ID

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                var idToken: String? = null
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    idToken = googleIdTokenCredential.idToken
                }

                if (!idToken.isNullOrBlank()) {
                    val session = SwaaraSupabaseClient.signInWithGoogleIdToken(idToken)
                    if (session != null) {
                        val auth = SwaaraCloudAuth.getInstance(activity)
                        auth.saveSession(session)
                        SwaaraAccountSyncEngine.triggerSync(activity)
                        withContext(Dispatchers.Main) {
                            activity.toast("Welcome, ${session.user.displayName}!")
                        }
                        return@withContext Result.success(session.user)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native One-Tap sign-in was not completed (${e.message}), opening in-app sign in popup fallback...")
            }
        }

        // Fallback: In-app popup dialog (zero external browser redirect)
        withContext(Dispatchers.Main) {
            openSignInPopup()
        }
        Result.failure(IllegalStateException("Redirected to Google Sign-In"))
    }

    /**
     * Fallback that opens the in-app dialog instead of an external browser.
     */
    fun launchBrowserGoogleSignIn(context: Context) {
        openSignInPopup()
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
            Log.e(TAG, "Failed to parse tokens from URL: $url", e)
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
                ?: email?.substringBefore('@')
                ?: "Swaara Listener"
            val avatarUrl = metaObj?.optString("avatar_url") ?: metaObj?.optString("picture")

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
            var user = parseUserFromJwt(accessToken)

            val userJson = SwaaraSupabaseClient.getUser(accessToken)
            if (userJson != null) {
                val userId = userJson.optString("id", user?.id ?: "")
                val email = if (userJson.has("email") && !userJson.isNull("email")) userJson.getString("email") else user?.email
                val metaObj = userJson.optJSONObject("user_metadata")
                val displayName = metaObj?.optString("full_name")
                    ?: metaObj?.optString("name")
                    ?: user?.displayName
                    ?: email?.substringBefore('@')
                    ?: "Swaara Listener"
                val avatarUrl = metaObj?.optString("avatar_url")
                    ?: metaObj?.optString("picture")
                    ?: user?.avatarUrl

                user = SupabaseUser(
                    id = userId,
                    email = email,
                    displayName = displayName,
                    avatarUrl = avatarUrl
                )
            }

            if (user == null || user.id.isBlank()) {
                Log.e(TAG, "Failed to resolve authenticated user")
                return@withContext false
            }

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
