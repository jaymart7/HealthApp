plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "ph.mart.healthapp.core.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        // FoodData Central key. Lives in ~/.gradle/gradle.properties (untracked, outside the
        // repo) as `fdcApiKey` — never in this file, which is checked in, the same rule the
        // release signing config follows. Absent it the build still compiles and food search
        // reports Failed, so a fresh clone and CI don't break.
        buildConfigField(
            "String",
            "FDC_API_KEY",
            "\"${providers.gradleProperty("fdcApiKey").orNull.orEmpty()}\"",
        )

        // The Google Health cloud leg, off for release until its OAuth consent screen is verified:
        // an unverified app in Testing answers everyone off the test-user list with "Access
        // blocked". Debug keeps it, since the emulator's account is a test user. Flip the default
        // once verification lands — see GOOGLE_HEALTH_ENABLED in health/HealthSyncRepository.kt.
        buildConfigField("boolean", "GOOGLE_HEALTH_ENABLED", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "GOOGLE_HEALTH_ENABLED", "true")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite.bundled)
    api(libs.kotlinx.coroutines.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)

    // Google Health API auth. Only `health/GoogleHealthAuth.kt` touches this — the access token
    // it hands back is a plain String everywhere else, and no `:feature:*` module sees the type.
    implementation(libs.play.services.auth)

    // Health Connect, the local provider. Scoped here for the same reason Room and
    // play-services-auth are: no `:feature:*` build file names it, and nothing above this module
    // sees an `androidx.health.connect` type — the permission contract crosses the boundary as a
    // framework `ActivityResultContract`, which is the only thing `androidx.activity` is here for.
    implementation(libs.androidx.health.connect.client)
    implementation(libs.androidx.activity)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
