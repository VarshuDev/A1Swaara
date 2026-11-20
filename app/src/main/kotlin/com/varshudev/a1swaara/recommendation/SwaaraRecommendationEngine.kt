package com.varshudev.a1swaara.recommendation

import androidx.media3.common.MediaItem
import java.util.Calendar

enum class TimeOfDaySlot(val label: String, val querySuffix: String) {
    MORNING("Morning Freshness & Soulful Melodies", "morning acoustic peaceful melody"),
    AFTERNOON("Afternoon Energy & Rhythmic Beats", "upbeat rhythm pop hits"),
    EVENING("Golden Hour & Party Anthems", "party dance energetic hits"),
    LATE_NIGHT("Late Night Chill & Lo-Fi Reverberations", "slowed reverb chill lofi acoustic")
}

data class AcousticProfile(
    val language: String?,
    val mood: String?,
    val keywords: Set<String>
)

data class PersonalizedFeedQueries(
    val trendingQuery: String,
    val viralQuery: String,
    val topChartQuery: String,
    val artistQuery: String,
    val timeSlot: TimeOfDaySlot,
    val timeSlotHeading: String
)

/**
 * Sovereign Acoustic Continuum & Recommendation Engine for A1 Swaara.
 * Implements harmonic queue reranking, anti-fatigue artist pacing, circadian time-of-day slots,
 * and multi-vector user affinity profiles.
 */
object SwaaraRecommendationEngine {

    private val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "of", "with", "by",
        "song", "songs", "audio", "video", "official", "full", "track", "remix", "version",
        "feat", "ft", "featuring", "from", "movie", "album", "soundtrack", "original",
        "lyric", "lyrics", "hd", "4k", "music"
    )

    fun getTimeOfDaySlot(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): TimeOfDaySlot {
        return when (hour) {
            in 5..10 -> TimeOfDaySlot.MORNING
            in 11..16 -> TimeOfDaySlot.AFTERNOON
            in 17..21 -> TimeOfDaySlot.EVENING
            else -> TimeOfDaySlot.LATE_NIGHT
        }
    }

    fun extractKeywords(text: String): Set<String> {
        if (text.isBlank()) return emptySet()
        return text.lowercase()
            .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()
    }

    fun analyzeProfile(title: String, artist: String): AcousticProfile {
        val combined = "$title $artist".lowercase()
        val keywords = extractKeywords(combined)

        var language: String? = null
        var mood: String? = null

        // Regional Linguistic Matrix Detection
        if (Regex("[\\u0C80-\\u0CFF]|kannada|sandalwood|sanjith|hegde|vijay\\s*prakash|puneeth|appu|yash|kgf|charlie|kantara|sapta|tagaru|arjun\\s*janya|charan\\s*raj|raghu\\s*dixit").containsMatchIn(combined)) {
            language = "kannada"
        } else if (Regex("[\\u0B80-\\u0BFF]|tamil|kollywood|anirudh|rahman|yuvan|harris\\s*jayaraj|ilayaraja|santhosh\\s*narayanan|jailer|leo|vikram|beast|dhanush|siva\\s*karthikeyan|rajinikanth").containsMatchIn(combined)) {
            language = "tamil"
        } else if (Regex("[\\u0C00-\\u0C7F]|telugu|tollywood|thaman|devi\\s*sri|dsp|sid\\s*sriram|pushpa|rrr|devara|salaar|kalki|allu\\s*arjun|mahesh\\s*babu|prabhas|keeravani").containsMatchIn(combined)) {
            language = "telugu"
        } else if (Regex("[\\u0A00-\\u0A7F]|punjabi|sidhu|moose|aujla|diljit|shubh|dhillon|amrit|manak|praak|jaani|hardy|ammy|honey\\s*singh").containsMatchIn(combined)) {
            language = "punjabi"
        } else if (Regex("[\\u0D00-\\u0D7F]|malayalam|mollywood|sushin\\s*shyam|shaan|vidyasagar|manjummel|avesham|lucifer|mohanlal").containsMatchIn(combined)) {
            language = "malayalam"
        } else if (Regex("lofi|lo-fi|chill|midnight|slowed|reverb|ambient|sleep|peace").containsMatchIn(combined)) {
            language = "lofi"
            mood = "chill"
        } else if (Regex("hindi|bollywood|arijit|pritam|shreya|kakkar|badshah|jubin|atif|kk|sonu\\s*nigam|shaan|udit|sanu|amit\\s*trivedi|vishal|darshan").containsMatchIn(combined)) {
            language = "hindi"
        } else if (Regex("weeknd|taylor|swift|drake|billie|eilish|sheeran|dua\\s*lipa|bieber|eminem|rihanna|post\\s*malone|bruno\\s*mars|coldplay|kendrick").containsMatchIn(combined)) {
            language = "english"
        }

        // Mood Classification
        if (mood == null) {
            mood = when {
                Regex("dance|party|club|dj|remix|blast|bass|drop|beat|house|edm").containsMatchIn(combined) -> "party"
                Regex("sad|broken|pain|tears|alone|melancholy|heartbreak").containsMatchIn(combined) -> "melancholic"
                Regex("love|romantic|tum|ishq|pyar|kadhal|prema|beloved").containsMatchIn(combined) -> "romantic"
                Regex("fire|pump|energy|fast|run|power|workout|anthem").containsMatchIn(combined) -> "energetic"
                else -> "chill"
            }
        }

        return AcousticProfile(language = language, mood = mood, keywords = keywords)
    }

    fun calculateAffinityScore(
        candidateTitle: String,
        candidateArtist: String,
        candidateId: String,
        currentTitle: String,
        currentArtist: String,
        currentId: String,
        recentHistoryIds: List<String> = emptyList(),
        likedSongIds: Set<String> = emptySet(),
        recentArtists: List<String> = emptyList()
    ): Double {
        if (candidateId == currentId) return -999.0

        var score = 50.0
        val currentProfile = analyzeProfile(currentTitle, currentArtist)
        val candidateProfile = analyzeProfile(candidateTitle, candidateArtist)

        val candArtistClean = candidateArtist.lowercase().trim()
        val currArtistClean = currentArtist.lowercase().trim()

        // 1. Artist Synergy
        if (candArtistClean.isNotBlank() && currArtistClean.isNotBlank()) {
            if (candArtistClean == currArtistClean) {
                score += 35.0
            } else if (candArtistClean.contains(currArtistClean) || currArtistClean.contains(candArtistClean)) {
                score += 20.0
            }
        }

        // 2. Language Matrix Harmony
        if (currentProfile.language != null && candidateProfile.language != null) {
            if (currentProfile.language == candidateProfile.language) {
                score += 30.0
            } else {
                score -= 20.0 // Soft penalty for unintended language leaps
            }
        }

        // 3. Mood Alignment
        if (currentProfile.mood != null && candidateProfile.mood != null) {
            if (currentProfile.mood == candidateProfile.mood) {
                score += 20.0
            }
        }

        // 4. Keyword Semantic Overlap
        val overlap = candidateProfile.keywords.count { it in currentProfile.keywords }
        score += minOf(30.0, overlap * 8.0)

        // 5. Liked Song Boost
        if (candidateId in likedSongIds) {
            score += 25.0
        }

        // 6. Anti-Fatigue & Artist Pacing
        val recentIndex = recentHistoryIds.take(15).indexOf(candidateId)
        if (recentIndex != -1) {
            score -= (15 - recentIndex) * 10.0 // Heavy penalty for recently played
        }

        // If previous 2 tracks were by this same artist, introduce fatigue penalty
        val lastTwoArtists = recentArtists.take(2).map { it.lowercase().trim() }
        if (lastTwoArtists.size >= 2 && lastTwoArtists[0] == candArtistClean && lastTwoArtists[1] == candArtistClean) {
            score -= 40.0
        }

        return score
    }

    /**
     * Reranks candidate radio/auto-DJ items so that queue flow is acoustically continuous,
     * avoids duplicate songs, prevents artist monopolies, and maximizes listening enjoyment.
     */
    fun rerankRadioQueue(
        currentTrack: MediaItem?,
        candidates: List<MediaItem>,
        recentHistoryIds: List<String> = emptyList(),
        likedSongIds: Set<String> = emptySet()
    ): List<MediaItem> {
        if (candidates.isEmpty()) return emptyList()

        val currentTitle = currentTrack?.mediaMetadata?.title?.toString().orEmpty()
        val currentArtist = currentTrack?.mediaMetadata?.artist?.toString().orEmpty()
        val currentId = currentTrack?.mediaId.orEmpty()

        // Filter out identical current song or blank mediaIds
        val uniqueCandidates = candidates
            .filter { it.mediaId.isNotBlank() && it.mediaId != currentId }
            .distinctBy { it.mediaId }

        val scored = uniqueCandidates.map { item ->
            val title = item.mediaMetadata.title?.toString().orEmpty()
            val artist = item.mediaMetadata.artist?.toString().orEmpty()
            val score = calculateAffinityScore(
                candidateTitle = title,
                candidateArtist = artist,
                candidateId = item.mediaId,
                currentTitle = currentTitle,
                currentArtist = currentArtist,
                currentId = currentId,
                recentHistoryIds = recentHistoryIds,
                likedSongIds = likedSongIds
            )
            item to score
        }.sortedByDescending { it.second }

        // Pacing pass: prevent 3 consecutive tracks by the exact same artist
        val paced = mutableListOf<MediaItem>()
        val deferred = mutableListOf<MediaItem>()
        var lastArtist = ""
        var artistStreak = 0

        for ((item, _) in scored) {
            val artist = item.mediaMetadata.artist?.toString()?.lowercase()?.trim().orEmpty()
            if (artist.isNotBlank() && artist == lastArtist) {
                if (artistStreak >= 2) {
                    deferred.add(item)
                    continue
                }
                artistStreak++
            } else {
                lastArtist = artist
                artistStreak = 1
            }
            paced.add(item)
        }

        paced.addAll(deferred)
        return paced
    }

    /**
     * Generates tailored search queries for the Home feed based on time of day,
     * selected language matrix, and top affinity artists.
     */
    fun buildFeedQueries(
        lang: String,
        topArtists: List<String> = emptyList(),
        hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): PersonalizedFeedQueries {
        val slot = getTimeOfDaySlot(hour)
        val isForYou = lang.equals("for_you", ignoreCase = true)
        val cleanTopArtist = topArtists.firstOrNull { it.isNotBlank() }

        val trendingQuery = when {
            isForYou && !cleanTopArtist.isNullOrBlank() -> "$cleanTopArtist ${slot.querySuffix} hits"
            isForYou -> "trending ${slot.querySuffix} hits"
            lang.equals("english", ignoreCase = true) -> "top hits ${slot.querySuffix}"
            else -> "$lang ${slot.querySuffix} hits"
        }

        val viralQuery = when {
            isForYou && topArtists.size > 1 -> "${topArtists[1]} viral hits chart"
            isForYou -> "viral hits trending chart"
            lang.equals("english", ignoreCase = true) -> "viral billboard hits"
            else -> "$lang viral hits chart"
        }

        val topChartQuery = when {
            isForYou -> "top 50 global music hits"
            lang.equals("english", ignoreCase = true) -> "billboard top 50"
            else -> "$lang top 50 songs chart"
        }

        val artistQuery = when {
            isForYou -> "top global music artists"
            lang.equals("english", ignoreCase = true) -> "popular music artists"
            else -> "$lang top playback singers"
        }

        return PersonalizedFeedQueries(
            trendingQuery = trendingQuery,
            viralQuery = viralQuery,
            topChartQuery = topChartQuery,
            artistQuery = artistQuery,
            timeSlot = slot,
            timeSlotHeading = slot.label
        )
    }
}
