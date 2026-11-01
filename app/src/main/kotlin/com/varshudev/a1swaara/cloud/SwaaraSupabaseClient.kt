package com.varshudev.a1swaara.cloud

import android.util.Log
import com.varshudev.a1swaara.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class SupabaseUser(
    val id: String,
    val email: String?,
    val displayName: String,
    val avatarUrl: String?
)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: SupabaseUser
)

/**
 * High-performance, zero-dependency Supabase client built natively on OkHttp.
 * Communicates with PostgREST (/rest/v1), GoTrue Auth (/auth/v1), and Realtime (/realtime/v1).
 */
object SwaaraSupabaseClient {
    private const val TAG = "SwaaraSupabaseClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val BASE_URL: String get() = BuildConfig.SWAARA_SUPABASE_URL.trimEnd('/')
    val ANON_KEY: String get() = BuildConfig.SWAARA_SUPABASE_ANON_KEY.trim()

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun buildHeaders(authToken: String?, extraHeaders: Map<String, String> = emptyMap()): Request.Builder {
        val bearer = if (!authToken.isNullOrBlank()) authToken else ANON_KEY
        val builder = Request.Builder()
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .addHeader("Accept", "application/json")
        for ((k, v) in extraHeaders) {
            builder.addHeader(k, v)
        }
        return builder
    }

    /**
     * Executes GET against PostgREST endpoint: /rest/v1/{table}?{queryParams}
     */
    fun get(table: String, queryParams: String? = null, authToken: String? = null): JSONArray? {
        val queryStr = if (queryParams.isNullOrBlank()) "" else "?$queryParams"
        val url = "$BASE_URL/rest/v1/$table$queryStr"

        val req = buildHeaders(authToken)
            .url(url)
            .get()
            .build()

        return try {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GET $table failed: HTTP ${response.code} ${response.message}")
                    return null
                }
                val body = response.body?.string() ?: return null
                if (body.startsWith("[")) JSONArray(body) else JSONArray().put(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing GET on $table", e)
            null
        }
    }

    /**
     * Executes POST against PostgREST endpoint to insert rows.
     */
    fun insert(table: String, jsonBody: String, authToken: String? = null): Boolean {
        val url = "$BASE_URL/rest/v1/$table"
        val reqBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)

        val req = buildHeaders(authToken, mapOf("Prefer" to "return=minimal"))
            .url(url)
            .post(reqBody)
            .build()

        return try {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()
                    Log.w(TAG, "Insert into $table failed [${response.code}]: $err")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting into $table", e)
            false
        }
    }

    /**
     * Executes Upsert (Insert on conflict update) against PostgREST endpoint.
     */
    fun upsert(table: String, jsonBody: String, onConflict: String? = null, authToken: String? = null): Boolean {
        val url = if (onConflict.isNullOrBlank()) {
            "$BASE_URL/rest/v1/$table"
        } else {
            "$BASE_URL/rest/v1/$table?on_conflict=$onConflict"
        }

        val reqBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val prefer = if (onConflict != null) "resolution=merge-duplicates,return=minimal" else "resolution=merge-duplicates"

        val req = buildHeaders(authToken, mapOf("Prefer" to prefer))
            .url(url)
            .post(reqBody)
            .build()

        return try {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()
                    Log.w(TAG, "Upsert to $table failed [${response.code}]: $err")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting to $table", e)
            false
        }
    }

    /**
     * Executes PATCH against PostgREST endpoint: /rest/v1/{table}?{queryParams}
     */
    fun patch(table: String, jsonBody: String, queryParams: String, authToken: String? = null): Boolean {
        val url = "$BASE_URL/rest/v1/$table?$queryParams"
        val reqBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)

        val req = buildHeaders(authToken, mapOf("Prefer" to "return=minimal"))
            .url(url)
            .patch(reqBody)
            .build()

        return try {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()
                    Log.w(TAG, "Patch $table failed [${response.code}]: $err")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error patching $table", e)
            false
        }
    }

    /**
     * Executes DELETE against PostgREST endpoint: /rest/v1/{table}?{queryParams}
     */
    fun delete(table: String, queryParams: String, authToken: String? = null): Boolean {
        val url = "$BASE_URL/rest/v1/$table?$queryParams"

        val req = buildHeaders(authToken)
            .url(url)
            .delete()
            .build()

        return try {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()
                    Log.w(TAG, "Delete from $table failed [${response.code}]: $err")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting from $table", e)
            false
        }
    }

    /**
     * Exchanges Google ID Token with Supabase Auth for an authenticated session.
     */
    fun signInWithGoogleIdToken(idToken: String): SupabaseSession? {
        val url = "$BASE_URL/auth/v1/token?grant_type=id_token"
        val payload = JSONObject().apply {
            put("provider", "google")
            put("id_token", idToken)
        }
        val reqBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val req = Request.Builder()
            .url(url)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(reqBody)
            .build()

        return try {
            httpClient.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: return null
                if (!response.isSuccessful) {
                    Log.e(TAG, "Supabase Auth Google exchange failed [${response.code}]: $bodyStr")
                    return null
                }

                val json = JSONObject(bodyStr)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 3600L)

                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val email = if (userObj.has("email") && !userObj.isNull("email")) userObj.getString("email") else null

                val metaObj = userObj.optJSONObject("user_metadata")
                val displayName = metaObj?.optString("full_name")
                    ?: metaObj?.optString("name")
                    ?: email?.substringBefore('@')
                    ?: "Swaara Listener"
                val avatarUrl = metaObj?.optString("avatar_url")
                    ?: metaObj?.optString("picture")

                SupabaseSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn,
                    user = SupabaseUser(
                        id = userId,
                        email = email,
                        displayName = displayName,
                        avatarUrl = avatarUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Supabase Google ID token sign-in", e)
            null
        }
    }

    /**
     * Fetches current user profile from /auth/v1/user using auth token.
     */
    fun getUser(authToken: String): JSONObject? {
        val url = "$BASE_URL/auth/v1/user"
        val req = buildHeaders(authToken).url(url).get().build()
        return try {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user info", e)
            null
        }
    }

    /**
     * Connects to Supabase Realtime WebSocket for low-latency live events.
     */
    fun subscribeToRealtimeChannel(
        table: String,
        filter: String? = null,
        onMessage: (JSONObject) -> Unit
    ): WebSocket? {
        val wsUrl = BASE_URL.replace("https://", "wss://").replace("http://", "ws://") +
                "/realtime/v1/websocket?apikey=$ANON_KEY&vsn=1.0.0"

        val request = Request.Builder().url(wsUrl).build()
        val refCounter = AtomicInteger(1)

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Realtime WebSocket connected")
                // Join channel
                val topic = "realtime:public:$table"
                val joinMsg = JSONObject().apply {
                    put("topic", topic)
                    put("event", "phx_join")
                    put("payload", JSONObject())
                    put("ref", refCounter.getAndIncrement().toString())
                }
                webSocket.send(joinMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = json.optString("event")
                    if (event == "postgres_changes" || event == "broadcast") {
                        val payload = json.optJSONObject("payload")
                        if (payload != null) {
                            onMessage(payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing realtime message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Realtime WebSocket error: ${t.message}")
            }
        }

        return try {
            httpClient.newWebSocket(request, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Realtime WebSocket", e)
            null
        }
    }
}
