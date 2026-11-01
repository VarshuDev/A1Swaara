# PRD: Swaara Lossless Audio Engine (v1.1.4)

## Introduction

Implement an Apple Music-style lossless audio experience in A1 Swaara. The system will always select the highest available audio quality from YouTube streams (OPUS 251kbps / AAC 256kbps), provide a dedicated audio quality preference with multiple tiers, configure ExoPlayer for maximum fidelity output (32-bit float, hi-res hardware sink), display real-time audio quality indicator badges in the player UI (similar to Apple Music's "Lossless" / "Hi-Res Lossless" labels), and transparently pass through local FLAC/WAV/ALAC files without any re-encoding. Data consumption is not a concern -- audio quality is the top priority.

## Goals

- Always select and play the highest available bitrate audio stream from YouTube
- Add user-facing audio quality preference tiers: Auto, Normal, High, Maximum
- Configure ExoPlayer audio renderer for audiophile-grade output (32-bit float, no dithering)
- Display real-time audio quality badges in the player UI showing codec, bitrate, and sample rate
- Pass through local lossless files (FLAC, WAV, ALAC) with zero transcoding
- Automatically disable volume normalization and skip-silence when Maximum quality is active
- Increase audio buffer sizes for uninterrupted high-bitrate streaming

## User Stories

### US-001: Audio Quality Preference Enum and Storage
**Description:** As a user, I want to choose my preferred audio quality tier so that the app always streams at my chosen fidelity level.

**Acceptance Criteria:**
- [ ] Add `AudioQuality` enum to `PlayerPreferences` with values: Auto, Normal (128kbps), High (192kbps), Maximum (256kbps+)
- [ ] Add `audioQuality` property to `PlayerPreferences` defaulting to `Maximum`
- [ ] Add `losslessMode` boolean property to `PlayerPreferences` defaulting to `true`
- [ ] When `losslessMode` is true, automatically disable `volumeNormalization` and `skipSilence`
- [ ] Code compiles cleanly offline

### US-002: Maximum Bitrate Stream Selection in Extractor
**Description:** As a user, I want the app to always pick the highest bitrate audio stream from YouTube so that I get the best possible sound quality.

**Acceptance Criteria:**
- [ ] Modify `audioStreamComparator` in `NewPipeExtractorClient` to rank OPUS above M4A above WEBMA above MP3
- [ ] When `AudioQuality.Maximum` is selected, always pick the stream with the highest `averageBitrate`
- [ ] When `AudioQuality.Normal` is selected, pick streams with bitrate closest to 128kbps
- [ ] When `AudioQuality.High` is selected, pick streams with bitrate closest to 192kbps
- [ ] When `AudioQuality.Auto`, use current default comparator (highest available)
- [ ] Log the selected stream's codec, bitrate, and itag in Logcat
- [ ] Code compiles cleanly offline

### US-003: Hi-Fi ExoPlayer Audio Renderer Configuration
**Description:** As a user, I want the audio renderer to output at the highest possible hardware fidelity so that I hear every detail.

**Acceptance Criteria:**
- [ ] In `createRendersFactory`, always pass `enableFloatOutput = true` to `DefaultAudioSink`
- [ ] Increase buffer sizes: minBuffer to 60s, maxBuffer to 240s for uninterrupted streaming
- [ ] Set audio content type to `AUDIO_CONTENT_TYPE_MUSIC` and usage to `USAGE_MEDIA` (already done, verify)
- [ ] Disable audio offload mode on all Android versions in lossless mode
- [ ] Request exclusive audio focus when lossless mode is active
- [ ] Code compiles cleanly offline

### US-004: Real-Time Audio Quality Badge in Player UI
**Description:** As a user, I want to see what audio quality is currently playing (codec, bitrate, quality tier label) in the player UI, similar to Apple Music's Lossless badge.

**Acceptance Criteria:**
- [ ] Create `SwaaraAudioQualityBadge` composable showing quality tier label (LOSSLESS, HI-RES, HIGH, NORMAL)
- [ ] Display current codec name (OPUS, AAC, FLAC, WAV, ALAC) and bitrate (e.g. "256 kbps")
- [ ] For local FLAC/WAV/ALAC files, show "LOSSLESS" or "HI-RES LOSSLESS" badge
- [ ] For streaming OPUS 251kbps+, show "MAXIMUM" badge
- [ ] Badge appears below the song title in both Classic and New player layouts
- [ ] Animate badge appearance with a subtle fade-in
- [ ] Code compiles cleanly offline

### US-005: Audio Quality Settings Section in Player Settings
**Description:** As a user, I want a dedicated audio quality section in Player Settings to configure my streaming and playback quality preferences.

**Acceptance Criteria:**
- [ ] Add "Audio Quality" settings group in `PlayerSettings` screen above the existing "Audio" group
- [ ] Add enum selector for `AudioQuality` (Auto / Normal / High / Maximum) with descriptions
- [ ] Add toggle for "Lossless Mode" with description explaining it disables normalization and skip-silence
- [ ] Show informational text: "Local files (FLAC, WAV, ALAC) always play at original lossless quality"
- [ ] Add string resources for all new labels and descriptions
- [ ] Code compiles cleanly offline

### US-006: Format Model Enhancement and Quality Metadata Persistence
**Description:** As a developer, I need to store richer audio format metadata so the quality badge can display accurate information.

**Acceptance Criteria:**
- [ ] Add `codec` (String?), `sampleRate` (Int?), and `channelCount` (Int?) fields to `Format` entity
- [ ] Room auto-migration handles the new nullable columns without data loss
- [ ] `writeStreamMetadata` in `NewPipeAudioMediaSourceFactory` populates new fields from the selected stream
- [ ] For local files, populate format metadata from `MediaStore` columns or ExoPlayer's `TrackSelectionParameters`
- [ ] Code compiles cleanly offline

### US-007: Version Bump and Build Verification
**Description:** As a developer, I need to verify the lossless audio engine builds cleanly and produces a verified v1.1.4 APK.

**Acceptance Criteria:**
- [ ] Version bumped to `1.1.4` (versionCode 15) in `build.gradle.kts`
- [ ] `./gradlew.bat --offline assembleHttp3Release` completes with zero errors
- [ ] Output APK generated at `app/build/outputs/apk/http3/release/`
- [ ] Zero regressions in online streaming, podcast player, or local file playback
- [ ] APK copied to `A1Swaara_apk/A1_Swaara_v1.1.4.apk`

## Functional Requirements

- FR-1: Add `AudioQuality` enum with tiers: Auto, Normal (128kbps cap), High (192kbps cap), Maximum (no cap, highest available)
- FR-2: Modify stream selection comparator to respect the user's chosen quality tier
- FR-3: Configure ExoPlayer `DefaultAudioSink` with 32-bit float output enabled permanently
- FR-4: Increase streaming buffer durations to 60s min / 240s max for high-bitrate uninterrupted playback
- FR-5: Display real-time audio quality badge (codec + bitrate + tier label) in the player UI
- FR-6: Add "Audio Quality" settings section with quality tier selector and lossless mode toggle
- FR-7: Enhance `Format` Room entity with codec, sampleRate, and channelCount columns
- FR-8: Local FLAC/WAV/ALAC files always play at original quality with "LOSSLESS" badge
- FR-9: When lossless mode is active, automatically disable volume normalization and silence skipping
- FR-10: Version bump to 1.1.4 (versionCode 15)

## Non-Goals

- No actual FLAC/lossless streaming from YouTube (YouTube does not serve lossless)
- No audio format conversion or transcoding
- No Dolby Atmos or spatial audio implementation
- No per-song quality override
- No Wi-Fi vs cellular quality differentiation (user said ignore data consumption)

## Technical Considerations

- YouTube provides audio streams up to OPUS 251kbps and AAC 256kbps -- these are the maximum available
- ExoPlayer natively supports FLAC, ALAC, WAV, OPUS, Vorbis, AAC decoding
- `DefaultAudioSink` with `enableFloatOutput=true` outputs 32-bit float PCM to the hardware
- Room auto-migration with `@AutoMigration` handles adding nullable columns
- The `audioStreamComparator` in `NewPipeExtractorClient` is the single control point for stream selection
- Local files bypass network resolution entirely via `isLocal` check in `NewPipeAudioMediaSourceFactory`

## Success Metrics

- Maximum quality stream (OPUS 251kbps or AAC 256kbps) selected for every YouTube playback
- Audio quality badge visible and accurate for both streaming and local files
- Zero audible artifacts, gaps, or degradation in playback quality
- Build compiles cleanly offline with no regressions

## Open Questions

- None. Implementation scope is clear.
