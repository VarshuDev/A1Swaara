<p align="center">
  <a href="https://github.com/VarshuDev/A1Swaara">
    <img src="assets/promo/banner.svg" alt="A1 Swaara Android Source Code Header" width="100%" />
  </a>
</p>

<p align="center">
  <strong>High-Performance Sovereign Audio Streaming Engine &amp; Studio Suite (Native Android Kotlin Source).</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge&logo=gnu&logoColor=white" alt="License GPLv3" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" /></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Audio-Media3%20ExoPlayer-FF2DAA?style=for-the-badge&logo=android&logoColor=white" alt="Media3" /></a>
  <a href="https://developer.android.com/training/data-storage/room"><img src="https://img.shields.io/badge/DB-Room%20SQLite-3DDC84?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room" /></a>
  <a href="https://github.com/VarshuDev"><img src="https://img.shields.io/badge/Author-%40VarshuDev-38bdf8?style=for-the-badge&logo=github&logoColor=white" alt="Author" /></a>
</p>

<p align="center">
  <a href="#-architecture--codebase-tour"><img src="assets/icons/architecture-mesh.svg" width="16" height="16" align="center" /> Architecture</a> •
  <a href="#-visual-showcase"><img src="assets/icons/device-screen.svg" width="16" height="16" align="center" /> Showcase</a> •
  <a href="#-core-engines-in-code"><img src="assets/icons/signal-core.svg" width="16" height="16" align="center" /> Core Engines</a> •
  <a href="#-building-from-source"><img src="assets/icons/terminal-build.svg" width="16" height="16" align="center" /> Building</a> •
  <a href="#-tech-stack--libraries"><img src="assets/icons/package-cube.svg" width="16" height="16" align="center" /> Tech Stack</a> •
  <a href="#-downloads--pre-built-apks"><img src="assets/icons/package-cube.svg" width="16" height="16" align="center" /> Releases</a>
</p>

---

## <img src="assets/icons/signal-core.svg" width="22" height="22" align="center" /> What is A1 Swaara?

**A1 Swaara** is a sovereign, high-fidelity music streaming application for Android engineered from the ground up on modern Android architecture: **Kotlin 2.x, Jetpack Compose Material 3, AndroidX Media3 ExoPlayer, Room SQLite, and Supabase**.

Designed for audiophiles and power users who care about source code sovereignty, A1 Swaara provides unthrottled **320 kbps audio streaming**, real-time synchronized karaoke lyrics, 10-band hardware parametric equalization, and zero interruptive ads or invasive tracking telemetry.

```
+──────────────────────────────────────────────────────────────────────────+
│  NATIVE ANDROID SUITE: A1 SWAARA SOURCE REPOSITORY                       │
│  AUTHOR: VarshuDev (https://github.com/VarshuDev)                        │
│  STACK: KOTLIN 2.X * JETPACK COMPOSE M3 * MEDIA3 EXOPLAYER * ROOM        │
+──────────────────────────────────────────────────────────────────────────+
```

---

## <img src="assets/icons/architecture-mesh.svg" width="22" height="22" align="center" /> Architecture &amp; Codebase Tour

A1 Swaara follows a modular, reactive Clean Architecture pattern separating UI presentation, playback engines, and data stores:

<p align="center">
  <img src="assets/promo/architecture.svg" alt="A1 Swaara Android Native Architecture" width="100%" />
</p>

### Directory &amp; Package Map

```
app/src/main/kotlin/com/varshudev/a1swaara/
├── cloud/              # Cloud synchronization & collaborative services
│   ├── SwaaraAccountSyncEngine.kt   # Two-way sync between Room DB and Supabase
│   ├── SwaaraJamEngine.kt           # Real-time group listening sessions (Jam)
│   ├── SwaaraRemoteConfig.kt        # Dynamic instance & bitrate failover
│   └── SwaaraTopListenerManager.kt  # Listener hours calculation & leaderboard
├── downloader/         # Background audio download manager & offline cache
├── extractor/          # Stream extractor negotiation & metadata parsers
├── models/             # Immutable data models (Song, Album, Artist, Playlist, Lyrics)
├── player/             # Media3 playback service & gapless crossfade engine
├── preferences/        # Datastore audio quality & theme settings
├── repositories/       # Room SQLite database entities & offline storage
├── service/            # Android foreground playback service & lockscreen controls
├── ui/                 # Declarative Jetpack Compose Material 3 presentation
│   ├── components/     # Reusable UI widgets (NowPlaying, EqualizerDeck, StorySheet)
│   └── screens/        # Full navigation screens (Home, Player, Jam, Podcasts, Profile)
├── updater/            # In-app OTA update manager (GitHub Releases API)
├── utils/              # Hardware canvas story exporter & synced lyrics parsers
└── work/               # Android WorkManager background sync workers
```

---

## <img src="assets/icons/device-screen.svg" width="22" height="22" align="center" /> Visual Showcase

<table align="center" width="100%">
  <tr>
    <td width="50%" align="center">
      <strong><img src="assets/icons/music-bars.svg" width="18" height="18" align="center" /> Now Playing &amp; Synced Karaoke Lyrics</strong><br>
      <em>Dynamic ambient backlight glow with real-time timestamped word tracking.</em><br><br>
      <img src="assets/promo/showcase_player.jpg" alt="A1 Swaara Now Playing & Synced Lyrics" width="100%" />
    </td>
    <td width="50%" align="center">
      <strong><img src="assets/icons/equalizer-knob.svg" width="18" height="18" align="center" /> 10-Band Studio Equalizer &amp; Bass Pump</strong><br>
      <em>Hardware DSP shaping, +12dB sub-harmonic bass, and 9:16 Story Creator.</em><br><br>
      <img src="assets/promo/showcase_studio.jpg" alt="A1 Swaara Studio Equalizer" width="100%" />
    </td>
  </tr>
</table>

---

## <img src="assets/icons/signal-core.svg" width="22" height="22" align="center" /> Core Engines in Code

### 1. <img src="assets/icons/headphones.svg" width="20" height="20" align="center" /> 32-Bit Float PCM Audio &amp; Media3 ExoPlayer
- Implemented in `com.varshudev.a1swaara.player`.
- Directly outputs **32-bit floating-point PCM audio** to Android hardware sinks and external USB DACs, preserving full dynamic range.
- **Unthrottled Stream Selection**: Prioritizes 251 kbps OPUS and 256 kbps AAC direct streams.
- **240s Anti-Stutter Lookahead Buffer**: Pre-caches audio to eliminate buffering interruptions on mobile cellular networks.

### 2. <img src="assets/icons/microphone.svg" width="20" height="20" align="center" /> Synchronized Karaoke Lyrics Engine
- Implemented in `com.varshudev.a1swaara.utils.SynchronizedLyrics`.
- Extracts and parses YouTube timestamped transcripts and standard LRC formats.
- Real-time auto-scroll animation synced to ExoPlayer playback timestamps with tactile line jumping.

### 3. <img src="assets/icons/canvas-story.svg" width="20" height="20" align="center" /> 9:16 Social Story Canvas Studio
- Implemented in `com.varshudev.a1swaara.utils.SwaaraStoryExporter`.
- Uses native Android hardware `Canvas` rendering to generate high-resolution 9:16 vertical cards (1080×1920) for Instagram and WhatsApp Stories.
- Renders dynamic audio waveforms, blurred album art backgrounds (`FastBlur.kt`), and customizable 60-character vibe quotes.

### 4. <img src="assets/icons/jam-users.svg" width="20" height="20" align="center" /> Real-Time Jam &amp; Collaborative Listening
- Implemented in `com.varshudev.a1swaara.cloud.SwaaraJamEngine`.
- Enables real-time synchronized playback across friends via peer session tokens and Supabase WebSockets.

### 5. <img src="assets/icons/sync-loop.svg" width="20" height="20" align="center" /> 1-Click Multi-Platform Playlist Importer
- Implemented in `com.varshudev.a1swaara.utils.PlaylistImporter`.
- Migrates existing playlists directly from **Spotify**, **YouTube Music**, or **CSV exports** without requiring authentication tokens.

### 6. <img src="assets/icons/podcast-mic.svg" width="20" height="20" align="center" /> Native Podcasts &amp; Spoken Audio Hub
- Implemented in `com.varshudev.a1swaara.ui.screens.podcast`.
- Segregated spoken audio feed featuring top serial podcasts (Tech &amp; AI, Mindset &amp; Growth, Business, and True Crime) with dedicated episode bookmarking.

---

## <img src="assets/icons/terminal-build.svg" width="22" height="22" align="center" /> Building From Source

### Prerequisites
- JDK 17 (or JDK 21), Git, and `make`
- Android SDK platform 37 and the build tools required by the Android Gradle plugin; the app supports API 23 and above.

### Build and test

```bash
git clone https://github.com/VarshuDev/A1Swaara.git
cd A1Swaara
cp local.properties.example local.properties  # set sdk.dir for your machine
make build              # prepares the pinned, patched extractor and builds HTTP/3 debug
make test               # JVM unit tests for both transport flavors
make adbinstall         # installs HTTP/3 debug on a connected device
```

The debug APK is at `app/build/outputs/apk/http3/debug/app-http3-debug.apk`.
After `make setup`, run a direct Gradle command with
`NEWPIPE_EXTRACTOR_DIR=build/newpipeextractor`, for example
`NEWPIPE_EXTRACTOR_DIR=build/newpipeextractor ./gradlew :app:assembleHttp2OnlyDebug`.
The Makefile verifies the extractor commit and fails if its patches cannot be applied.

For releases, `gradle.properties` is the version-name source. CI needs the release
keystore, passwords, alias, and `SWAARA_RELEASE_CERT_SHA256` repository secrets;
the certificate fingerprint must match the key used for already-installed releases.
Never publish an APK signed with an unrelated key.

### Cloud safety and current limitations

The checked-in Supabase policy is not automatically deployed. Test it against a
**disposable** Supabase project before applying it; see `tests/supabase_rls.sql`.
It limits profile columns and restricts Jam mutations to the host. Private Jam
creation is temporarily disabled until authenticated invitations and membership
are implemented; listener counts and stale-room cleanup need server-side services.
Public queue submissions still need abuse controls and rate limiting.

Legacy Firestore library backup/restore is disabled because it did not use user
authentication. Existing cloud data is not deleted. `firestore.rules` is a proposed
fail-closed baseline, **not a deployed configuration**; deploying it as-is will
also disable legacy Firestore features. Audit existing data and migrate those
features to authenticated access first. Supabase library sync currently merges
items by name; it does not fully reconcile playlist edits or deletions and should
not be relied on as a complete backup. Offline listening-stat upload is paused
until it can be made idempotent. Keep an independent local backup.

Room schema export is enabled and missing migration paths fail rather than delete
user data. Commit generated schemas and add migration tests for every version change.

---

## <img src="assets/icons/package-cube.svg" width="22" height="22" align="center" /> Tech Stack &amp; Libraries

| Category | Component / Library | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.x | Modern functional language &amp; coroutines |
| **UI Framework** | Jetpack Compose M3 | Declarative reactive UI with Material You theming |
| **Audio Engine** | AndroidX Media3 ExoPlayer | Audio pipeline, foreground service, floating PCM |
| **Local Database** | Room SQLite + KSP | Offline music library, favorites, and listening history |
| **Networking** | OkHttp 5 / HTTP/3 QUIC | Resilient media streaming and API extraction |
| **Cloud Services** | Supabase REST / Firestore REST | Account merge, leaderboard stats, and community features |
| **Image Pipeline** | Coil 3.x | High-performance asynchronous album art loading |
| **Stream Extraction**| NewPipeExtractor | Open-source streaming protocol resolution |
| **Background Sync** | AndroidX WorkManager | Idempotent daily stats sync and background updates |

---

## <img src="assets/icons/package-cube.svg" width="22" height="22" align="center" /> Downloads &amp; Pre-Built APKs

If you want ready-to-install standalone APK packages without compiling from source:

👉 **[Download Pre-Built A1 Swaara APKs from the Release Hub](https://github.com/VarshuDev/A1Swaara_apk/releases)**

---

## <img src="assets/icons/crown-author.svg" width="22" height="22" align="center" /> Author &amp; Credits

Engineered, architected, and maintained with care by **[VarshuDev](https://github.com/VarshuDev)**.

Released under the **[GNU General Public License v3.0](LICENSE)**.
