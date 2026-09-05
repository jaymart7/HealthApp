// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.services) apply false
}

/**
 * The modules that have been through the localization pass (ROADMAP item 3). A module joins this
 * list in its own commit, and from then on [checkUiLiterals] keeps literals out of it.
 */
val localizedModules = listOf(
    "app",
    "core/data",
    "core/designsystem",
    "core/navigation",
    "feature/coach",
    "feature/food",
    "feature/home",
    "feature/onboarding",
    "feature/profile",
    "feature/progress",
    "wear",
)

/** Argument names that carry copy a reader sees. */
val uiArguments = listOf(
    "text", "label", "contentDescription", "title", "body", "placeholder",
    "confirmLabel", "dismissLabel", "reason", "heading", "hint",
)

/**
 * Fails on a user-facing string literal left in Kotlin in an already-localized module.
 *
 * Stock lint is no help here: `HardcodedText` scans XML layout resources, and this app has none —
 * it would pass clean on a module with three hundred Kotlin literals. Preview fixtures are
 * skipped; they are debug-only sample data and no translator reads them.
 *
 * ponytail: a line-based grep, not a parser — a literal split across lines, or passed positionally
 * rather than by name, slips through. A Compose lint rule is the upgrade path if that starts
 * happening.
 */
tasks.register("checkUiLiterals") {
    group = "verification"
    description = "Fails if a localized module still passes a string literal to a UI argument."
    val roots = localizedModules.map { file("$it/src/main") }
    val literal = Regex("""\b(${uiArguments.joinToString("|")})\s*=\s*"[A-Z]""")
    val previewStart = Regex("""fun \w*Preview\(""")
    doLast {
        val hits = roots.flatMap { root ->
            root.walkTopDown().filter { it.extension == "kt" }.flatMap { source ->
                var inPreview = false
                source.readLines().mapIndexedNotNull { index, line ->
                    when {
                        previewStart.containsMatchIn(line) -> { inPreview = true; null }
                        inPreview -> { if (line == "}") inPreview = false; null }
                        literal.containsMatchIn(line) -> "${source.path}:${index + 1}: ${line.trim()}"
                        else -> null
                    }
                }
            }
        }
        if (hits.isNotEmpty()) {
            throw GradleException(
                "Hardcoded UI strings in localized modules:\n" + hits.joinToString("\n"),
            )
        }
    }
}
