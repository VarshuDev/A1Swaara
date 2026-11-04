package com.varshudev.a1swaara.podcast

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class SwaaraPodcastEpisode(
    val id: String,
    val title: String,
    val author: String,
    val durationText: String,
    val thumbnailUrl: String,
    val viewCountText: String? = null
)

data class SwaaraPodcastGenre(
    val id: String,
    val title: String,
    val query: String
)

/**
 * Sovereign Podcast Feed Extractor for A1 Swaara.
 * Connects directly to YouTube's search endpoint to fetch authentic, full-length podcast episodes,
 * interviews, and talk shows with real durations, thumbnails, and channel metadata.
 */
object SwaaraPodcastFeedClient {
    private const val TAG = "SwaaraPodcastFeed"
    private const val SEARCH_API = "https://www.youtube.com/youtubei/v1/search"

    val GENRES = listOf(
        SwaaraPodcastGenre("all", "All Podcasts", "podcast"),
        SwaaraPodcastGenre("raj_shamani", "Raj Shamani", "Raj Shamani podcast Figuring Out"),
        SwaaraPodcastGenre("ranveer", "Ranveer Allahbadia", "The Ranveer Show TRS podcast"),
        SwaaraPodcastGenre("lex_rogan", "Lex & Rogan", "Lex Fridman Joe Rogan podcast episode"),
        SwaaraPodcastGenre("tech_ai", "Tech & AI", "AI technology podcast Sam Altman Nikhil Kamath"),
        SwaaraPodcastGenre("mindset", "Mindset & Health", "Andrew Huberman Dr Joe Dispenza podcast"),
        SwaaraPodcastGenre("business", "Business & Wealth", "business startup finance investing podcast"),
        SwaaraPodcastGenre("crime", "True Crime", "true crime mystery investigation podcast")
    )

    private val cache = ConcurrentHashMap<String, List<SwaaraPodcastEpisode>>()

    private val FALLBACK_EPISODES = listOf(
        SwaaraPodcastEpisode(
            id = "TjPPIf-M6KE",
            title = "Sam Altman On AI Revolution, Superintelligence & Future Of Humanity | FO583 Raj Shamani",
            author = "Raj Shamani",
            durationText = "1:08:01",
            thumbnailUrl = "https://i.ytimg.com/vi/TjPPIf-M6KE/hqdefault.jpg",
            viewCountText = "1.8M views"
        ),
        SwaaraPodcastEpisode(
            id = "4Vz6L8B73i4",
            title = "Neuroscientist's Guide To 10X Your Focus & Memory | Dr Sahar Yousef | FO559 Raj Shamani",
            author = "Raj Shamani",
            durationText = "2:55:06",
            thumbnailUrl = "https://i.ytimg.com/vi/4Vz6L8B73i4/hqdefault.jpg",
            viewCountText = "16M views"
        ),
        SwaaraPodcastEpisode(
            id = "ZAzWPE9CItg",
            title = "AI Crisis in Math: Navier-Stokes, Riemann Hypothesis & Complexity Theory | Lex Fridman Podcast #503",
            author = "Lex Fridman",
            durationText = "3:34:27",
            thumbnailUrl = "https://i.ytimg.com/vi/ZAzWPE9CItg/hqdefault.jpg",
            viewCountText = "950K views"
        ),
        SwaaraPodcastEpisode(
            id = "Rni7Fz7208c",
            title = "Elon Musk: A Different Conversation w/ Nikhil Kamath | Full Episode | People by WTF Ep. 16",
            author = "Nikhil Kamath",
            durationText = "1:54:14",
            thumbnailUrl = "https://i.ytimg.com/vi/Rni7Fz7208c/hqdefault.jpg",
            viewCountText = "3.2M views"
        ),
        SwaaraPodcastEpisode(
            id = "Y566_T-YlNQ",
            title = "Andrew Huberman: Become Mentally Dangerous With These Daily Habits | FO556 Raj Shamani",
            author = "Raj Shamani",
            durationText = "2:46:56",
            thumbnailUrl = "https://i.ytimg.com/vi/Y566_T-YlNQ/hqdefault.jpg",
            viewCountText = "4.5M views"
        ),
        SwaaraPodcastEpisode(
            id = "sRH-XlBE2cQ",
            title = "Ex Navy SEAL Who Killed Osama Bin Laden On The Raid & Fear | Robert J. O’Neill | FO579 Raj Shamani",
            author = "Raj Shamani",
            durationText = "1:49:32",
            thumbnailUrl = "https://i.ytimg.com/vi/sRH-XlBE2cQ/hqdefault.jpg",
            viewCountText = "2.1M views"
        )
    )

    suspend fun fetchPodcasts(query: String = "podcast"): List<SwaaraPodcastEpisode> = withContext(Dispatchers.IO) {
        val cached = cache[query]
        if (!cached.isNullOrEmpty()) {
            return@withContext cached
        }

        try {
            val url = URL(SEARCH_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            val requestJson = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                    })
                })
                put("query", query)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val parsed = JSONObject(responseText)
                val sections = parsed.optJSONObject("contents")
                    ?.optJSONObject("twoColumnSearchResultsRenderer")
                    ?.optJSONObject("primaryContents")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")

                val results = mutableListOf<SwaaraPodcastEpisode>()

                if (sections != null) {
                    for (i in 0 until sections.length()) {
                        val sec = sections.optJSONObject(i)
                        val items = sec?.optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue

                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j) ?: continue
                            val v = item.optJSONObject("videoRenderer") ?: continue

                            val videoId = v.optString("videoId", "")
                            if (videoId.isBlank()) continue

                            val titleRuns = v.optJSONObject("title")?.optJSONArray("runs")
                            val title = titleRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                            if (title.isBlank()) continue

                            val authorRuns = v.optJSONObject("ownerText")?.optJSONArray("runs")
                            val author = authorRuns?.optJSONObject(0)?.optString("text", "Podcast Creator") ?: "Podcast Creator"

                            val duration = v.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
                            val views = v.optJSONObject("viewCountText")?.optString("simpleText", "")?.takeIf { it.isNotBlank() }
                                ?: v.optJSONObject("shortViewCountText")?.optString("simpleText", "")?.takeIf { it.isNotBlank() }

                            val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                            results.add(
                                SwaaraPodcastEpisode(
                                    id = videoId,
                                    title = title,
                                    author = author,
                                    durationText = duration,
                                    thumbnailUrl = thumb,
                                    viewCountText = views
                                )
                            )
                        }
                    }
                }

                if (results.isNotEmpty()) {
                    cache[query] = results
                    return@withContext results
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query YouTube podcasts: ${e.message}")
        }

        return@withContext FALLBACK_EPISODES
    }
}
