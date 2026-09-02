plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ph.mart.healthapp.wear"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Same applicationId as `:app` on purpose: that is what pairs the two halves, so the
        // watch app installs alongside the phone one rather than as a separate product. The
        // version must move with `:app`'s for the same reason.
        applicationId = "ph.mart.healthapp"
        minSdk = 30
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"
    }

    // Shares `:app`'s untracked credentials — a companion watch app has to carry the same
    // signature as the phone app it pairs with.
    val keystoreFile = providers.gradleProperty("fitpulseKeystoreFile").orNull

    signingConfigs {
        create("release") {
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = providers.gradleProperty("fitpulseKeystorePassword").get()
                keyAlias = providers.gradleProperty("fitpulseKeyAlias").get()
                keyPassword = providers.gradleProperty("fitpulseKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release").takeIf { keystoreFile != null }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // The wire format, and the frozen palette. Deliberately *not* `:core:data` — the watch keeps
    // no database, so Room, Firebase AI and play-services-auth have no business on a wrist.
    implementation(project(":core:today"))
    implementation(project(":core:designsystem"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    // @WearPreviewDevices / @WearPreviewFontScales — the watch's answer to @PreviewLightDark.
    implementation(libs.androidx.wear.compose.ui.tooling)

    // The tile: a second, glanceable surface drawn with protolayout rather than Compose.
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout.material3)
    debugImplementation(libs.androidx.wear.tiles.tooling)
    implementation(libs.androidx.wear.tiles.tooling.preview)

    implementation(libs.play.services.wearable)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
