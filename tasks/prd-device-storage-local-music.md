# PRD: Device Storage Local Music Engine for A1 Swaara (v1.1.3)

## 1. Introduction/Overview
A1 Swaara currently supports online YouTube streaming, cached audio, and podcasts. Users who have high-fidelity offline audio files (MP3, FLAC, M4A, WAV, OGG) stored directly on their Android devices currently cannot browse or play those files inside the app.

This feature introduces a native **Device Storage Local Music Engine** that scans Android's `MediaStore.Audio.Media`, indexes local audio tracks with full metadata (ID3 tags, album art URI, duration, bitrate, artist), and integrates seamless offline playback into A1 Swaara's existing ExoPlayer pipeline and Library UI.

---

## 2. Goals
- Provide fast, asynchronous scanning of on-device music folders without UI stutter.
- Support modern Android 13+ granular media permissions (`READ_MEDIA_AUDIO`) and backward compatibility for Android 8-12 (`READ_EXTERNAL_STORAGE`).
- Seamlessly route local `content://` and `file://` URIs through `PlayerService` without triggering network/Piped/YouTube lookups.
- Add a dedicated **"On-Device Files"** tab in the Library Screen with sorting, searching, and instant shuffle playback.
- Ensure 100% offline compilation and zero regressions in online streaming.

---

## 3. User Stories

### US-001: Android Storage Permissions Architecture
**Description:** As a user, I want the app to gracefully request permission to access my device's audio files so that my local library can be indexed safely.

**Acceptance Criteria:**
- [ ] Add `READ_MEDIA_AUDIO` permission to `AndroidManifest.xml` for SDK >= 33.
- [ ] Add `READ_EXTERNAL_STORAGE` permission with `maxSdkVersion="32"` for backward compatibility.
- [ ] Implement reactive runtime permission launcher composable that informs the user why audio access is requested.
- [ ] Gracefully handle permission rejection with an informative empty-state screen and button to open System App Settings.
- [ ] Code compiles cleanly offline (`assembleHttp3Release --offline`).

---

### US-002: MediaStore Audio Scanner Engine
**Description:** As a user, I want the app to scan my device's music files and extract titles, artists, albums, durations, and album artwork.

**Acceptance Criteria:**
- [ ] Create `SwaaraDeviceMusicScanner.kt` utilizing Android `ContentResolver` and `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
- [ ] Filter out ringtones, alarms, notifications, and audio files shorter than 10 seconds.
- [ ] Parse title, artist, album, duration (ms), file size, date added, content URI, and album art URI (`content://media/external/audio/albumart/<id>`).
- [ ] Expose scanning as a Kotlin Coroutines `Flow<List<DeviceAudioTrack>>` with dispatching on `Dispatchers.IO`.
- [ ] Support manual pull-to-refresh / rescan trigger.
- [ ] Typecheck and lint pass.

---

### US-003: PlayerService Local Playback Routing
**Description:** As a user, I want tapping a local song to immediately start playback in the player with album art and seek bar support.

**Acceptance Criteria:**
- [ ] In `PlayerService.kt`, detect whether a `mediaId` represents a local track (`isLocal = true` or `uri.scheme == "content"` / `"file"`).
- [ ] Bypass YouTube / Piped stream extraction for local tracks and set `MediaItem.Builder().setUri(contentUri)` directly.
- [ ] Ensure player notification, lock screen controls, and progress scrub bar reflect local song playback without errors.
- [ ] Support next, previous, shuffle, and repeat operations within local queues.
- [ ] Typecheck passes and no network calls are dispatched for local files.

---

### US-004: Dedicated "On-Device" Tab in Library & Search
**Description:** As a user, I want a dedicated section in my Library to browse, sort, and search my on-device songs.

**Acceptance Criteria:**
- [ ] Add an "On-Device" / "Local Storage" tab to `SwaaraLibraryScreen` or Library navigation.
- [ ] Display track list with track number, title, artist, duration, and embedded album art thumbnail via Coil.
- [ ] Provide sorting options: By Title (A-Z), By Artist, By Date Added (Recent first), By Duration.
- [ ] Include an in-page quick filter/search bar to find tracks instantly.
- [ ] Include "Play All" and "Shuffle All" floating or header action buttons.
- [ ] Typecheck and Compose lint pass.

---

### US-005: Offline Build & Playback Verification
**Description:** As a developer, I want to verify that the entire local music pipeline builds cleanly in offline mode and outputs a verified v1.1.3 APK.

**Acceptance Criteria:**
- [ ] `./gradlew.bat --offline assembleHttp3Release` completes with zero errors.
- [ ] Output APK generated in `app/build/outputs/apk/http3/release/app-http3-release.apk`.
- [ ] Manifest and APK version bumped to `1.1.3` (versionCode 14).
- [ ] No regression in podcast mode or streaming mode.

---

## 4. Functional Requirements
- **FR-1:** The scanner must query `MediaStore.Audio.Media.IS_MUSIC != 0`.
- **FR-2:** The playback engine must never attempt YouTube stream URL resolution if `track.isLocalDeviceAudio == true`.
- **FR-3:** When permission is revoked, the tab must not crash and should show a permission prompt with one-click settings launcher.
- **FR-4:** Local songs can be added into app playlists alongside online songs.

---

## 5. Non-Goals (Out of Scope)
- ID3 tag editing / writing back to the user's audio files (read-only for safety).
- Built-in audio format converting / transcoding.
- Uploading local audio files to cloud storage (respects user privacy and copyright).

---

## 6. Technical Architecture

```
[ Android MediaStore / Storage ]
              │ (ContentResolver query)
              ▼
   [ SwaaraDeviceMusicScanner ]
              │
              ▼ (StateFlow<List<DeviceAudioTrack>>)
   [ SwaaraDeviceMusicViewModel ]
              │
              ▼ (Jetpack Compose UI)
   [ SwaaraDeviceMusicScreen / Library Tab ]
              │
              ▼ (MediaItem with content:// URI)
   [ PlayerService (ExoPlayer) ] ──► [ Hardware AudioSink ]
```
