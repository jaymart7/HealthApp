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
 * Files whose English is a decision recorded at its own definition — a pure function with a JVM
 * test over its exact wording, an exception message, or a proper name. Each carries the comment
 * saying so; this list is only what keeps [checkUiLiterals] from arguing with it.
 */
val literalExceptions = listOf(
    "SubjectSummary.kt",
    "AchievementsDetail.kt",
    "HomeData.kt",
    "DiaryDateHeader.kt",
    "GoalProjectionLine.kt",
    "MascotAvatar.kt",
    "ProfileExport.kt",
)

/**
 * Fails on a user-facing string literal left in Kotlin in an already-localized module.
 *
 * Stock lint is no help here: `HardcodedText` scans XML layout resources, and this app has none —
 * it would pass clean on a module with three hundred Kotlin literals. Preview fixtures are
 * skipped; they are debug-only sample data and no translator reads them.
 *
 * Two rules. The named one runs over every localized module and catches `text = "Add reading"`.
 * The positional three run only where copy lives — a `ui/` tree, or a shared component — because
 * `StatRow("Systolic", …)` reads the same as a Room query, a prompt or a `@SerialName` everywhere
 * else. That split is not cosmetic: the whole of `:feature:progress` passed this task while
 * thirteen empty-state pages were still English, because every one of those literals was
 * positional.
 *
 * ponytail: a line-based grep, not a parser — a literal split across lines, or one starting with a
 * template (`"$n tracked"`), still slips through. A Compose lint rule is the upgrade path if that
 * starts happening.
 */
tasks.register("checkUiLiterals") {
    group = "verification"
    description = "Fails if a localized module still passes a string literal to a UI argument."
    val roots = localizedModules.map { file("$it/src/main") }
    val named = Regex("""\b(${uiArguments.joinToString("|")})\s*=\s*"[A-Z]""")
    val positional = listOf(
        Regex("""[(,]\s*"[A-Z][a-z]"""),  // a literal opening an argument
        Regex("""^\s*"[A-Z][a-z]"""),     // a literal alone on its own line
        Regex("""->\s*"[A-Z][a-z]"""),    // a `when` branch returning copy
    )
    val previewStart = Regex("""fun \w*Preview\(|^(private )?val (PREVIEW|preview)""")
    // Copied into a local: `doLast` cannot capture a script property and stay configuration-cacheable.
    val exceptions = literalExceptions
    doLast {
        val hits = roots.flatMap { root ->
            root.walkTopDown().filter { it.extension == "kt" }.flatMap { source ->
                val path = source.path.replace(File.separatorChar, '/')
                val drawsCopy = ("/ui/" in path || "/designsystem/component/" in path) &&
                    source.name !in exceptions
                var inPreview = false
                source.readLines().mapIndexedNotNull { index, line ->
                    // A preview fixture runs until the next line that starts in column one, so a
                    // `fun …Preview()`, a `val PREVIEW_ITEMS = listOf(` and a two-line `val` all
                    // end where the next top-level declaration begins.
                    if (inPreview) {
                        if (line.isBlank() || line.first().isWhitespace()) return@mapIndexedNotNull null
                        inPreview = false
                    }
                    val trimmed = line.trim()
                    when {
                        previewStart.containsMatchIn(line) -> { inPreview = true; null }
                        // An `error()` or `require()` message is an exception, not copy, and an
                        // annotation argument is never read by anyone.
                        trimmed.startsWith("//") || trimmed.startsWith("*") ||
                            trimmed.startsWith("@") || "error(" in line || "require(" in line -> null
                        named.containsMatchIn(line) ||
                            (drawsCopy && positional.any { it.containsMatchIn(line) }) ->
                            "${source.path}:${index + 1}: $trimmed"
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
