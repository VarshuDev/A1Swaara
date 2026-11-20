package com.varshudev.a1swaara.recommendation

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwaaraRecommendationEngineTest {

    @Test
    fun `test time of day slots mapping`() {
        assertEquals(TimeOfDaySlot.MORNING, SwaaraRecommendationEngine.getTimeOfDaySlot(6))
        assertEquals(TimeOfDaySlot.MORNING, SwaaraRecommendationEngine.getTimeOfDaySlot(9))
        assertEquals(TimeOfDaySlot.AFTERNOON, SwaaraRecommendationEngine.getTimeOfDaySlot(13))
        assertEquals(TimeOfDaySlot.EVENING, SwaaraRecommendationEngine.getTimeOfDaySlot(19))
        assertEquals(TimeOfDaySlot.LATE_NIGHT, SwaaraRecommendationEngine.getTimeOfDaySlot(23))
        assertEquals(TimeOfDaySlot.LATE_NIGHT, SwaaraRecommendationEngine.getTimeOfDaySlot(2))
    }

    @Test
    fun `test keyword extraction filters stopwords`() {
        val keywords = SwaaraRecommendationEngine.extractKeywords("Belakina Kavithe Official HD Video Song from Banaras")
        assertTrue("belakina" in keywords)
        assertTrue("kavithe" in keywords)
        assertTrue("banaras" in keywords)
        assertFalse("the" in keywords)
        assertFalse("song" in keywords)
        assertFalse("video" in keywords)
        assertFalse("from" in keywords)
    }

    @Test
    fun `test language matrix recognition`() {
        val kannadaProfile = SwaaraRecommendationEngine.analyzeProfile("Singara Siriye", "Vijay Prakash, Charan Raj")
        assertEquals("kannada", kannadaProfile.language)

        val tamilProfile = SwaaraRecommendationEngine.analyzeProfile("Naa Ready", "Anirudh Ravichander, Thalapathy Vijay")
        assertEquals("tamil", tamilProfile.language)

        val teluguProfile = SwaaraRecommendationEngine.analyzeProfile("Oo Antava", "Devi Sri Prasad, Indravathi")
        assertEquals("telugu", teluguProfile.language)

        val punjabiProfile = SwaaraRecommendationEngine.analyzeProfile("295", "Sidhu Moose Wala")
        assertEquals("punjabi", punjabiProfile.language)

        val lofiProfile = SwaaraRecommendationEngine.analyzeProfile("Midnight Rain Lo-Fi Reverb", "Chill Beats")
        assertEquals("lofi", lofiProfile.language)
        assertEquals("chill", lofiProfile.mood)
    }

    @Test
    fun `test calculateAffinityScore gives high affinity to same artist and language`() {
        val scoreSame = SwaaraRecommendationEngine.calculateAffinityScore(
            candidateTitle = "Soul of Dia",
            candidateArtist = "Sanjith Hegde",
            candidateId = "cand1",
            currentTitle = "Belakina Kavithe",
            currentArtist = "Sanjith Hegde",
            currentId = "curr1"
        )

        val scoreDiff = SwaaraRecommendationEngine.calculateAffinityScore(
            candidateTitle = "Starboy",
            candidateArtist = "The Weeknd",
            candidateId = "cand2",
            currentTitle = "Belakina Kavithe",
            currentArtist = "Sanjith Hegde",
            currentId = "curr1"
        )

        assertTrue("Same artist and language must score substantially higher than disparate genre", scoreSame > scoreDiff)
    }

    @Test
    fun `test calculateAffinityScore penalizes recently played songs in history`() {
        val scoreFresh = SwaaraRecommendationEngine.calculateAffinityScore(
            candidateTitle = "Kesariya",
            candidateArtist = "Arijit Singh",
            candidateId = "fresh1",
            currentTitle = "Apna Bana Le",
            currentArtist = "Arijit Singh",
            currentId = "curr1",
            recentHistoryIds = listOf("other1", "other2")
        )

        val scoreRepeated = SwaaraRecommendationEngine.calculateAffinityScore(
            candidateTitle = "Kesariya",
            candidateArtist = "Arijit Singh",
            candidateId = "fresh1",
            currentTitle = "Apna Bana Le",
            currentArtist = "Arijit Singh",
            currentId = "curr1",
            recentHistoryIds = listOf("fresh1", "other2")
        )

        assertTrue("Recently played song in history must be heavily penalized", scoreFresh > scoreRepeated)
    }

    @Test
    fun `test rerankRadioQueue deduplicates and breaks consecutive artist monopolies`() {
        fun makeItem(id: String, title: String, artist: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .build()
                )
                .build()
        }

        val current = makeItem("c1", "Current Kannada Song", "Vijay Prakash")
        val candidates = listOf(
            makeItem("s1", "Song 1", "Vijay Prakash"),
            makeItem("s2", "Song 2", "Vijay Prakash"),
            makeItem("s3", "Song 3", "Vijay Prakash"),
            makeItem("s4", "Song 4", "Charan Raj"),
            makeItem("s1", "Duplicate of Song 1", "Vijay Prakash"), // duplicate
            makeItem("c1", "Duplicate of Current", "Vijay Prakash") // duplicate of current
        )

        val reranked = SwaaraRecommendationEngine.rerankRadioQueue(
            currentTrack = current,
            candidates = candidates
        )

        // c1 should be excluded
        assertFalse(reranked.any { it.mediaId == "c1" })
        // s1 should appear only once
        assertEquals(1, reranked.count { it.mediaId == "s1" })

        // Check pacing: the first 2 might be Vijay Prakash, but the 3rd should be Charan Raj (artist alternation)
        assertEquals("Vijay Prakash", reranked[0].mediaMetadata.artist)
        assertEquals("Vijay Prakash", reranked[1].mediaMetadata.artist)
        assertEquals("Charan Raj", reranked[2].mediaMetadata.artist)
    }

    @Test
    fun `test buildFeedQueries creates personalized contextual queries`() {
        val queries = SwaaraRecommendationEngine.buildFeedQueries(
            lang = "for_you",
            topArtists = listOf("Sanjith Hegde", "Arijit Singh"),
            hour = 8 // Morning
        )

        assertEquals(TimeOfDaySlot.MORNING, queries.timeSlot)
        assertTrue(queries.trendingQuery.contains("Sanjith Hegde"))
        assertTrue(queries.trendingQuery.contains("morning"))
        assertTrue(queries.viralQuery.contains("Arijit Singh"))
    }
}
