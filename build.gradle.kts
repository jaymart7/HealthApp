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
 * The modules that have been through the localization pass. A module joins this
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
    "feature/training",
    "wear",
)

/** Argument names that carry copy a reader sees. */
val uiArguments = listOf(
    "text", "label", "contentDescription", "title", "body", "placeholder",
    "confirmLabel", "dismissLabel", "reason", "heading", "hint",
)

/**
 * Files whose English is a decision recorded at its own definition — today, only proper names.
 * Each carries the comment saying so; this list is only what keeps [checkUiLiterals] from arguing
 * with it. A pure function that chooses words is not an exception any more: it returns a `Phrase`.
 */
val literalExceptions = listOf(
    "MascotAvatar.kt",
)

/**
 * Fails on a user-facing string literal left in Kotlin in an already-localized module.
 *
 * Stock lint is no help here: `HardcodedText` scans XML layout resources, and this app has none —
 * it would pass clean on a module with three hundred Kotlin literals. Preview fixtures are
 * skipped; they are debug-only sample data and no translator reads them.
 *
 * Two rules. The named one runs over every localized module and catches `text = "Add reading"`.
 * The positional five run only where copy lives — a `ui/` tree, or a shared component — because
 * `StatRow("Systolic", …)` reads the same as a Room query, a prompt or a `@SerialName` everywhere
 * else. That split is not cosmetic: the whole of `:feature:progress` passed this task while
 * thirteen empty-state pages were still English, because every one of those literals was
 * positional.
 *
 * ponytail: a line-based grep, not a parser — a literal split across lines still slips through,
 * and so does copy in a module's non-`ui/` code (`EarnedCalories.kt` sat in `:core:data` for a
 * year). A Compose lint rule is the upgrade path if that starts happening.
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
        // A template opening a sentence: "$days days ago". Unit symbols after a figure are not
        // copy, so "$n kcal" and "$n bpm" pass; shorter symbols (g, kg, ml, cm) never reach three
        // letters.
        Regex(""""\$(\{[^}]*\}|[\w.]+) (?!kcal\b|bpm\b)[a-z]{3,}"""),
        // A lone capital passed as an argument: the macro initials ("P", "C", "F") were copy the
        // rules above could not see, and Filipino's fat is "T".
        Regex("""[(,]\s*"[A-Z]"\s*[,)]"""),
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
                        trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*") ||
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

/** The language this build ships besides English, as its resource folder names it. */
val translationQualifier = "values-fil"

/**
 * Fails when a translation and its English drift apart: a key on one side only, a string whose
 * placeholders differ, or a plural missing a category. Placeholders are the sharp one — a `%1$s`
 * the English has and the translation lost is a crash at the call site, not a typo.
 *
 * Only the forms this app uses count as placeholders (`%s`, `%d`, `%1$s`, `%.1f`, `%%`), so a
 * literal `"% taken"` is text. Every plural item is held to the placeholders of the English
 * `other`: a `one` that drops its `%d` is legal Android, but Filipino's `one` covers more numbers
 * than 1, so it would print the wrong count.
 */
tasks.register("checkTranslations") {
    group = "verification"
    description = "Fails if a translation's keys, placeholders or plurals differ from the English."
    // Copied into locals: `doLast` cannot capture a script property and stay configuration-cacheable.
    val resDirs = localizedModules.associateWith { file("$it/src/main/res") }
    val qualifier = translationQualifier
    doLast {
        val placeholder = Regex("""%(\d+\$)?,?(\.\d+)?[sdf]|%%""")
        // name -> (kind, quantity -> text); a <string> is stored under the quantity "".
        fun read(file: File): Map<String, Pair<String, Map<String, String>>> {
            if (!file.exists()) return emptyMap()
            val root = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(file).documentElement
            val out = linkedMapOf<String, Pair<String, Map<String, String>>>()
            val nodes = root.childNodes
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as? org.w3c.dom.Element ?: continue
                if (el.getAttribute("translatable") == "false") continue
                val name = el.getAttribute("name")
                when (el.tagName) {
                    "string" -> out[name] = "string" to mapOf("" to el.textContent)
                    "plurals" -> {
                        val items = el.getElementsByTagName("item")
                        out[name] = "plurals" to (0 until items.length).associate {
                            val item = items.item(it) as org.w3c.dom.Element
                            item.getAttribute("quantity") to item.textContent
                        }
                    }
                }
            }
            return out
        }
        fun marks(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()
        val problems = resDirs.flatMap { (where, res) ->
            val english = read(File(res, "values/strings.xml"))
            if (english.isEmpty()) return@flatMap emptyList<String>()
            val translated = read(File(res, "$qualifier/strings.xml"))
            buildList {
                (english.keys - translated.keys).forEach { add("$where: missing $it") }
                (translated.keys - english.keys).forEach { add("$where: extra $it") }
                for (key in english.keys.intersect(translated.keys)) {
                    val (kind, en) = english.getValue(key)
                    val (trKind, tr) = translated.getValue(key)
                    if (kind != trKind) {
                        add("$where: $key is a $kind in English and a $trKind here")
                    } else if (kind == "plurals") {
                        listOf("one", "other").filter { it !in tr }
                            .forEach { add("$where: $key has no quantity=\"$it\"") }
                        val want = marks(en.getValue("other"))
                        tr.forEach { (quantity, text) ->
                            if (marks(text) != want) add("$where: $key[$quantity] placeholders ${marks(text)}, English $want")
                        }
                    } else if (marks(en.getValue("")) != marks(tr.getValue(""))) {
                        add("$where: $key placeholders ${marks(tr.getValue(""))}, English ${marks(en.getValue(""))}")
                    }
                }
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Translation drift (${problems.size}):\n" + problems.joinToString("\n"))
        }
    }
}
