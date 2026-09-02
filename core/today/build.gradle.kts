plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "ph.mart.healthapp.core.today"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Deliberately the whole dependency list. This module is on the watch's classpath, so a
    // dependency on `:core:data` here would ship Room, Firebase AI and play-services-auth to a
    // wrist — see TodaySnapshot's KDoc.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
