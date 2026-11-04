# PRD: Sovereign YouTube Podcast Hub & Audio/Video Player Mode

## Introduction
Enhance A1 Swaara's Podcast mode so that when users tap the Podcasts tab, it loads authentic, high-value full-length podcasts directly from YouTube's comprehensive podcast catalog (`https://www.youtube.com/results?search_query=podcast`) instead of music songs or random tracks. Audio plays by default with 0 latency, with an intuitive "View Video" option for users who want to watch the visual episode.

## Goals
- Fetch authentic YouTube podcast episodes with real durations, titles, thumbnails, and channel names.
- Replace music search recommendations with real podcast video metadata.
- Default to audio-only streaming when an episode is selected.
- Provide a toggle in the player to switch between Audio Mode and Video Mode seamlessly.
- Keep Podcast Mode cleanly self-contained on the homepage without mixing into the music search route.

## User Stories

### US-001: Implement YouTube Podcast Feed Extractor
**Description:** As a user, I want A1 Swaara to fetch authentic podcast video metadata from YouTube so I see real talk shows and interviews instead of random songs.
**Acceptance Criteria:**
- Query YouTube search endpoint using YouTube client to retrieve video results for `podcast`.
- Parse video title, videoId, channel/author name, duration, and high-res thumbnail.
- Provide curated category queries (All Podcasts, Raj Shamani, Tech & AI, Mindset & Growth, Business & Money, True Crime, Comedy).
- Typecheck passes

### US-002: Redesign Podcast Environment on Homepage
**Description:** As a user, I want a clean, dedicated podcast feed on the homepage when selecting the Podcasts tab without global music search clutter.
**Acceptance Criteria:**
- Redesign `SwaaraPodcastEnvironment.kt` to display real YouTube podcast episodes.
- Interactive category chip filter updates feed seamlessly.
- Tapping an episode starts instant audio playback.
- No global music search routing triggered from podcast cards.
- Typecheck passes

### US-003: Audio-Only Default Playback with Interactive Video Mode Toggle
**Description:** As a user, I want podcasts to play audio-only by default, but have a "View Video" button to watch the episode when desired.
**Acceptance Criteria:**
- Default to audio-only playback when a podcast item is tapped.
- Add an Audio Mode / Video Mode toggle switch in Now Playing screen for podcasts.
- In Video Mode, display the video stream or web player surface.
- In Audio Mode, display the standard audio album art / animated soundwave.
- Typecheck passes

### US-004: Build & Distribution Verification
**Description:** As a maintainer, I want the updated application to build cleanly offline and package into the release APK.
**Acceptance Criteria:**
- Run `./gradlew.bat --offline assembleHttp3Release` successfully.
- Copy fresh release APK to `A1_Swaara_v1.0.apk`.
- Commit changes cleanly in both Git repositories with author `VarshuDev <varshudev@veloriolabs.internal>`.
- Typecheck passes
