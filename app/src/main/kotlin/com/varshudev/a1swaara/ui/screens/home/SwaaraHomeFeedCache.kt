package com.varshudev.a1swaara.ui.screens.home

import android.content.Context
import com.varshudev.a1swaara.cloud.TrendingSong
import com.varshudev.a1swaara.ui.components.SwaaraDisplayItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * In-memory and disk-backed (Stale-While-Revalidate) feed cache for SwaaraHomeScreen.
 * Ensures instant cold-start rendering from the last saved feed for the active language while
 * silently refreshing in the background when stale, and never wiping cached items on network errors.
 */
object SwaaraHomeFeedCache {
    private const val STALE_THRESHOLD_MS = 15 * 60 * 1000L

    var cachedLanguage: String? = null
    var trendingSongs: List<SwaaraDisplayItem> = emptyList()
    var quickGridItems: List<SwaaraDisplayItem> = emptyList()
    var liveArtists: List<SwaaraArtistDisplayItem> = emptyList()
    var viralSongs: List<SwaaraDisplayItem> = emptyList()
    var topTwentyList: List<SwaaraDisplayItem> = emptyList()
    var communitySongs: List<TrendingSong> = emptyList()
    var isInitialized: Boolean = false
    var lastUpdatedMs: Long = 0L

    fun hasDataFor(lang: String): Boolean {
        return isInitialized && cachedLanguage == lang && (trendingSongs.isNotEmpty() || quickGridItems.isNotEmpty())
    }

    fun isStale(maxAgeMs: Long = STALE_THRESHOLD_MS): Boolean {
        return lastUpdatedMs == 0L || (System.currentTimeMillis() - lastUpdatedMs) > maxAgeMs
    }

    fun clear() {
        cachedLanguage = null
        trendingSongs = emptyList()
        quickGridItems = emptyList()
        liveArtists = emptyList()
        viralSongs = emptyList()
        topTwentyList = emptyList()
        communitySongs = emptyList()
        isInitialized = false
        lastUpdatedMs = 0L
    }

    fun loadFromDiskIfNeeded(context: Context, lang: String): Boolean {
        if (hasDataFor(lang)) return true
        return runCatching {
            val file = cacheFileFor(context, lang)
            if (!file.exists() || file.length() == 0L) return@runCatching false

            val root = JSONObject(file.readText())
            val savedLang = root.optString("language", "")
            if (!savedLang.equals(lang, ignoreCase = false)) return@runCatching false

            val loadedTrending = parseDisplayList(root.optJSONArray("trendingSongs"))
            val loadedQuickGrid = parseDisplayList(root.optJSONArray("quickGridItems"))
            val loadedArtists = parseArtistList(root.optJSONArray("liveArtists"))
            val loadedViral = parseDisplayList(root.optJSONArray("viralSongs"))
            val loadedTopTwenty = parseDisplayList(root.optJSONArray("topTwentyList"))
            val loadedCommunity = parseTrendingList(root.optJSONArray("communitySongs"))

            if (loadedTrending.isEmpty() && loadedQuickGrid.isEmpty() && loadedViral.isEmpty()) {
                return@runCatching false
            }

            cachedLanguage = lang
            trendingSongs = loadedTrending
            quickGridItems = loadedQuickGrid
            liveArtists = loadedArtists
            viralSongs = loadedViral
            topTwentyList = loadedTopTwenty
            if (loadedCommunity.isNotEmpty()) {
                communitySongs = loadedCommunity
            }
            lastUpdatedMs = root.optLong("timestampMs", 0L)
            isInitialized = true
            true
        }.getOrDefault(false)
    }

    fun saveToDisk(context: Context, lang: String) {
        runCatching {
            val root = JSONObject().apply {
                put("language", lang)
                put("timestampMs", lastUpdatedMs)
                put("trendingSongs", serializeDisplayList(trendingSongs))
                put("quickGridItems", serializeDisplayList(quickGridItems))
                put("liveArtists", serializeArtistList(liveArtists))
                put("viralSongs", serializeDisplayList(viralSongs))
                put("topTwentyList", serializeDisplayList(topTwentyList))
                put("communitySongs", serializeTrendingList(communitySongs))
            }
            val target = cacheFileFor(context, lang)
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(target)) {
                target.writeText(root.toString())
                tmp.delete()
            }
        }
    }

    private fun cacheFileFor(context: Context, lang: String): File {
        val safeLang = lang.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
        return File(context.filesDir, "home_feed_cache_$safeLang.json")
    }

    private fun serializeDisplayList(items: List<SwaaraDisplayItem>): JSONArray {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("subtitle", item.subtitle)
                    put("thumbnailUrl", item.thumbnailUrl ?: JSONObject.NULL)
                    put("badge", item.badge ?: JSONObject.NULL)
                }
            )
        }
        return arr
    }

    private fun parseDisplayList(arr: JSONArray?): List<SwaaraDisplayItem> {
        if (arr == null) return emptyList()
        val list = ArrayList<SwaaraDisplayItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            if (id.isBlank()) continue
            list.add(
                SwaaraDisplayItem(
                    id = id,
                    title = obj.optString("title", "Song"),
                    subtitle = obj.optString("subtitle", "Artist"),
                    thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() && it != "null" },
                    badge = obj.optString("badge").takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return list
    }

    private fun serializeArtistList(items: List<SwaaraArtistDisplayItem>): JSONArray {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("thumbnailUrl", item.thumbnailUrl ?: JSONObject.NULL)
                }
            )
        }
        return arr
    }

    private fun parseArtistList(arr: JSONArray?): List<SwaaraArtistDisplayItem> {
        if (arr == null) return emptyList()
        val list = ArrayList<SwaaraArtistDisplayItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            if (id.isBlank()) continue
            list.add(
                SwaaraArtistDisplayItem(
                    id = id,
                    name = obj.optString("name", "Artist"),
                    thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return list
    }

    private fun serializeTrendingList(items: List<TrendingSong>): JSONArray {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("songId", item.songId)
                    put("title", item.title)
                    put("artist", item.artist)
                    put("artworkUrl", item.artworkUrl ?: JSONObject.NULL)
                    put("playCount", item.playCount)
                    put("rank", item.rank)
                }
            )
        }
        return arr
    }

    private fun parseTrendingList(arr: JSONArray?): List<TrendingSong> {
        if (arr == null) return emptyList()
        val list = ArrayList<TrendingSong>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val songId = obj.optString("songId", "")
            if (songId.isBlank()) continue
            list.add(
                TrendingSong(
                    songId = songId,
                    title = obj.optString("title", "Song"),
                    artist = obj.optString("artist", "Artist"),
                    artworkUrl = obj.optString("artworkUrl").takeIf { it.isNotBlank() && it != "null" },
                    playCount = obj.optLong("playCount", 0L),
                    rank = obj.optInt("rank", 0)
                )
            )
        }
        return list
    }
}
