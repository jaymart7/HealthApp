pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FitPulse"
include(":app")
include(":core:designsystem")
include(":core:data")
include(":core:camera")
include(":core:navigation")
include(":feature:onboarding")
include(":feature:home")
include(":feature:food")
include(":feature:progress")
include(":feature:coach")
include(":feature:profile")
 