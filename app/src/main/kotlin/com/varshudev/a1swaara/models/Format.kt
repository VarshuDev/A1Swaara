package com.varshudev.a1swaara.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Immutable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Format(
    @PrimaryKey val songId: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    val bitrate: Long? = null,
    val contentLength: Long? = null,
    val lastModified: Long? = null,
    val loudnessDb: Float? = null,
    val codec: String? = null,
    val sampleRate: Int? = null,
    val channelCount: Int? = null
) {
    /**
     * Returns a human-readable quality tier label based on codec and bitrate.
     * LOSSLESS: Local FLAC, WAV, ALAC files
     * HI-RES LOSSLESS: Local lossless files with sample rate above 48kHz
     * MAXIMUM: Streaming at 200kbps or above (OPUS 251 / AAC 256)
     * HIGH: Streaming between 128-199kbps
     * NORMAL: Streaming below 128kbps
     */
    @get:Ignore
    val qualityTier: String
        get() {
            val codecLower = codec?.lowercase() ?: mimeType?.lowercase() ?: ""
            val isLosslessCodec = codecLower.contains("flac") ||
                codecLower.contains("wav") ||
                codecLower.contains("pcm") ||
                codecLower.contains("alac") ||
                codecLower.contains("aiff")

            if (isLosslessCodec) {
                val rate = sampleRate ?: 44100
                return if (rate > 48000) "HI-RES LOSSLESS" else "LOSSLESS"
            }

            val kbps = bitrate ?: 0L
            return when {
                kbps >= 200 -> "MAXIMUM"
                kbps >= 128 -> "HIGH"
                kbps > 0 -> "NORMAL"
                else -> "UNKNOWN"
            }
        }

    /** Short codec display name for the audio quality badge. */
    @get:Ignore
    val codecDisplayName: String
        get() {
            val raw = codec ?: mimeType ?: return ""
            val lower = raw.lowercase()
            return when {
                lower.contains("opus") -> "OPUS"
                lower.contains("aac") || lower.contains("mp4a") -> "AAC"
                lower.contains("flac") -> "FLAC"
                lower.contains("wav") || lower.contains("pcm") -> "WAV"
                lower.contains("alac") -> "ALAC"
                lower.contains("vorbis") -> "VORBIS"
                lower.contains("mp3") || lower.contains("mpeg") -> "MP3"
                lower.contains("webm") || lower.contains("webma") -> "WEBM"
                else -> raw.substringAfterLast("/").uppercase().take(8)
            }
        }

    /** Formatted bitrate string, e.g. "256 kbps". */
    @get:Ignore
    val bitrateDisplayText: String?
        get() {
            val kbps = bitrate ?: return null
            return if (kbps > 0) "$kbps kbps" else null
        }
}
