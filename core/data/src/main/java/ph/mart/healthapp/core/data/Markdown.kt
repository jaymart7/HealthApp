package ph.mart.healthapp.core.data

/**
 * Markdown out of model output, because asking for none of it does not get none of it.
 *
 * Every Gemini prompt in this app that produces prose already says "no markdown, no headings, no
 * bold" — and the model writes `**62 g**` and `* item` anyway. A prompt is a request; this is the
 * enforcement, and it sits with the other things the sanitizers reject: stray quotes, runs of
 * whitespace, and an answer longer than the bubble it has to fit.
 *
 * Stripping rather than rendering, which is the decision the whole shape follows from: what comes
 * out of here is what Room persists, what the bubble draws, and what Copy and Share hand out, so
 * there is one representation of an answer rather than a marked-up one and a rendered one that
 * have to agree. `DECISIONS.md` → **AI — the coach & the daily insight** has the argument.
 *
 * Its own file, not `Ai.kt`: that one's `AI_THINKING` is a top-level `val` built from a Firebase
 * type, so touching anything beside it runs that construction in `<clinit>`. This file is pure —
 * no Android, no Firebase — which is what lets `MarkdownTest` reach it, the argument
 * [sanitizeInsight][ph.mart.healthapp.core.data.insight.sanitizeInsight] and
 * [loggable][ph.mart.healthapp.core.data.food.loggable] are already written under.
 */

/** A fence line, not the code inside it: the content is still the answer. */
private val FENCE = Regex("^\\s*```.*$")

/** `---`, `***`, `___` on their own. Three or more, so a `- ` bullet is never one. */
private val RULE = Regex("^\\s*(?:-{3,}|\\*{3,}|_{3,})\\s*$")

/** The words of a heading are a sentence — only the hashes go. */
private val HEADING = Regex("^\\s*#{1,6}\\s+")

private val QUOTE = Regex("^\\s*>\\s?")

/** Normalised to `- `, which is the one list format the coach's prompt asks for. */
private val BULLET = Regex("^\\s*[*+•]\\s+")

private val LINK = Regex("\\[([^\\]\\n]+)]\\([^)\\s]*\\)")

/**
 * Longest marker first, and every one of them needs a **closing** partner hugging non-space —
 * which is what leaves `2 * 3` alone and leaves a half-streamed `**Prot` reading as itself until
 * its closer lands, rather than flickering mid-answer.
 *
 * The lone `_` is the one that needs a word-boundary guard as well: `snake_case` is not italics,
 * and persisted names in this app are full of it.
 */
private val INLINE = listOf(
    Regex("\\*\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*\\*"),
    Regex("\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*"),
    Regex("\\*(?=\\S)([^*\\n]+?)(?<=\\S)\\*"),
    Regex("__(?=\\S)(.+?)(?<=\\S)__"),
    Regex("(?<![A-Za-z0-9_])_(?=\\S)([^_\\n]+?)(?<=\\S)_(?![A-Za-z0-9_])"),
    Regex("~~(?=\\S)(.+?)(?<=\\S)~~"),
    Regex("`([^`\\n]+)`"),
)

/**
 * The marked-up answer in, the plain one out. Line-level rules run before inline ones, or `* item`
 * loses its word to the emphasis pass.
 *
 * Numbered lists are left exactly as they are: `1.` is how a person writes a list too.
 */
internal fun stripMarkdown(text: String): String = text
    .lines()
    .filterNot { FENCE.matches(it) || RULE.matches(it) }
    .joinToString("\n") { line ->
        var stripped = line
            .replace(HEADING, "")
            .replace(QUOTE, "")
            .replace(BULLET, "- ")
        INLINE.forEach { stripped = it.replace(stripped, "$1") }
        LINK.replace(stripped, "$1")
    }
