import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import kotlin.random.Random
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.ksp)

}

android {
    val debugNameSuffix = buildString {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
    }

    val appId = "com.varshudev.a1swaara"

    namespace = "com.varshudev.a1swaara"
    compileSdk = 37

    val localProperties = Properties().apply {
        val localPropFile = rootProject.file("local.properties")
        if (localPropFile.exists()) {
            localPropFile.inputStream().use { load(it) }
        }
    }

    val cloudApiKey = localProperties.getProperty("SWAARA_CLOUD_API_KEY")
        ?: providers.gradleProperty("SWAARA_CLOUD_API_KEY").orNull
        ?: System.getenv("SWAARA_CLOUD_API_KEY") ?: "AIzaSyD8F5v--Ls6YXVCQ_5UhBY20scKJQu7Lxo"
        ?: ""

    val cloudProjectId = localProperties.getProperty("SWAARA_CLOUD_PROJECT_ID")
        ?: providers.gradleProperty("SWAARA_CLOUD_PROJECT_ID").orNull
        ?: System.getenv("SWAARA_CLOUD_PROJECT_ID")
        ?: "raaga-music-8e1f2"

    val supabaseUrl = localProperties.getProperty("SWAARA_SUPABASE_URL")
        ?: providers.gradleProperty("SWAARA_SUPABASE_URL").orNull
        ?: System.getenv("SWAARA_SUPABASE_URL")
        ?: "https://vdfwhjsjkvmcoalgjtke.supabase.co"

    val supabaseAnonKey = localProperties.getProperty("SWAARA_SUPABASE_ANON_KEY")
        ?: providers.gradleProperty("SWAARA_SUPABASE_ANON_KEY").orNull
        ?: System.getenv("SWAARA_SUPABASE_ANON_KEY")
        ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZkZndoanNqa3ZtY29hbGdqdGtlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIzMjE2MTAsImV4cCI6MjEwNzg5NzYxMH0.imIDXk9roPuip3V70PyLNt1xuxe060Ucb-Blk2F-5ec"

    val googleWebClientId = localProperties.getProperty("SWAARA_GOOGLE_WEB_CLIENT_ID")
        ?: providers.gradleProperty("SWAARA_GOOGLE_WEB_CLIENT_ID").orNull
        ?: System.getenv("SWAARA_GOOGLE_WEB_CLIENT_ID")
        ?: "899397486076-7opo34vsigm6pk02judsj3qi4j3foaq3.apps.googleusercontent.com"

    val googleAndroidClientId = localProperties.getProperty("SWAARA_GOOGLE_ANDROID_CLIENT_ID")
        ?: providers.gradleProperty("SWAARA_GOOGLE_ANDROID_CLIENT_ID").orNull
        ?: System.getenv("SWAARA_GOOGLE_ANDROID_CLIENT_ID")
        ?: "899397486076-37f77el6fkqa68vtr2ctblsqmnkr03u2.apps.googleusercontent.com"

    defaultConfig {
        applicationId = appId

        minSdk = 23
        targetSdk = 36

        versionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0"

        multiDexEnabled = true

        buildConfigField("String", "SWAARA_CLOUD_API_KEY", "\"$cloudApiKey\"")
        buildConfigField("String", "SWAARA_CLOUD_PROJECT_ID", "\"$cloudProjectId\"")
        buildConfigField("String", "SWAARA_SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SWAARA_SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SWAARA_GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "SWAARA_GOOGLE_ANDROID_CLIENT_ID", "\"$googleAndroidClientId\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    flavorDimensions += "httpTransport"
    productFlavors {
        create("http3") {
            dimension = "httpTransport"
            isDefault = true
        }

        create("http2Only") {
            dimension = "httpTransport"
            versionNameSuffix = "-HTTP2ONLY"
        }
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("ci") {
            storeFile = System.getenv("ANDROID_NIGHTLY_KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("ANDROID_NIGHTLY_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_NIGHTLY_KEYSTORE_ALIAS")
            keyPassword = System.getenv("ANDROID_NIGHTLY_KEYSTORE_PASSWORD")
        }
        create("release") {
            storeFile = rootProject.file("a1swaara-release.jks")
            storePassword = providers.gradleProperty("SWAARA_KEYSTORE_PASSWORD").orNull
            keyAlias = providers.gradleProperty("SWAARA_KEY_ALIAS").orNull ?: "a1swaara"
            keyPassword = providers.gradleProperty("SWAARA_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["appName"] = "A1 Swaara"
        }

        release {
            isMinifyEnabled = false
            isShrinkResources = false
            manifestPlaceholders["appName"] = "A1 Swaara"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

        create("nightly") {
            initWith(getByName("release"))
            matchingFallbacks += "release"

            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-NIGHTLY"
            manifestPlaceholders["appName"] = "A1 Swara Nightly"
            signingConfig = signingConfigs.findByName("ci")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        // Keep native libraries uncompressed so AGP can place every .so on a 16 KiB ZIP boundary.
        jniLibs {
            useLegacyPackaging = false
        }
        resources.excludes.add("META-INF/**/*")
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    lint {
        // Translations are maintained independently from source changes; missing
        // locales must not block verification of Android/API correctness.
        disable += "MissingTranslation"
    }
}

androidComponents {
    onVariants(selector().withFlavor("httpTransport" to "http2Only")) { variant ->
        variant.packaging.jniLibs.excludes.add("**/libkathttp3.so")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

composeCompiler {
    if (project.findProperty("enableComposeCompilerReports") == "true") {
        val dest = layout.buildDirectory.dir("compose_metrics")
        metricsDestination = dest
        reportsDestination = dest
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)

    implementation(projects.compose.persist)
    implementation(projects.compose.preferences)
    implementation(projects.compose.routing)
    implementation(projects.compose.reordering)

    implementation(fileTree(projectDir.resolve("vendor")))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    // Compose pulls graphics-path transitively. Pin the current release so its native binary uses
    // 16 KiB ELF segment alignment instead of an older transitive version.
    implementation(libs.androidx.graphics.path)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.lottie)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle)
    implementation(libs.compose.viewmodel)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.core)
    implementation(libs.coil.network.okhttp)
    implementation(libs.palette)
    implementation(libs.monet)
    runtimeOnly(projects.core.materialCompat)

    implementation(libs.exoplayer)
    implementation(libs.exoplayer.dash)
    implementation(libs.exoplayer.workmanager)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)

    implementation(libs.workmanager)
    implementation(libs.workmanager.ktx)

    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")


    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.datetime)

    implementation(libs.room)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    implementation(libs.log4j)
    implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    // KatHttp3 code and native libraries exist only in the HTTP/3 source set.
    add("http3Implementation", "com.github.haturatu:kathttp3:v0.1.31")
    implementation(libs.newpipe.nanojson)
    implementation(libs.re2j)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.4")

    implementation(projects.providers.github)
    implementation(projects.providers.youtubeMusic)
    implementation(projects.providers.kugou)
    implementation(projects.providers.lrclib)
    implementation(projects.providers.piped)
    implementation(projects.providers.sponsorblock)
    implementation(projects.providers.translate)
    implementation(projects.providers.common)
    implementation(projects.core.data)
    implementation(projects.core.ui)

}

tasks.register("verifyDebug16kZipAlignment") {
    group = "verification"
    description = "Verifies that every native library in the debug APK is 16 KiB ZIP-aligned."
    dependsOn("assembleDebug")

    doLast {
        val zipalign = android.sdkComponents.sdkDirectory.get().asFile
            .resolve("build-tools/${android.buildToolsVersion}/zipalign")
        check(zipalign.isFile) { "zipalign was not found: $zipalign" }

        val apks = layout.buildDirectory.dir("outputs/apk/debug").get().asFileTree
            .matching { include("*.apk") }
            .files
            .sorted()
        check(apks.isNotEmpty()) { "No debug APK was produced." }

        apks.forEach { apk ->
            providers.exec {
                commandLine(zipalign.absolutePath, "-c", "-P", "16", "-v", "4", apk.absolutePath)
            }.result.get().assertNormalExitValue()
        }
    }
}
