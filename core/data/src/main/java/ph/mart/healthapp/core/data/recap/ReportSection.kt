package ph.mart.healthapp.core.data.recap

/**
 * The four blocks the coach's report card draws, and the *only* thing that crosses a module
 * boundary when one of them is tapped.
 *
 * The card lives in `:feature:coach` and the pages it opens are `:feature:progress`'s, so the
 * callback out carries this enum's [name] as a plain String and `:app` maps it onto the route —
 * `onStartRoutine`'s shape, for the rule every cross-feature reference in this app follows:
 * neither module learns the other's types.
 *
 * Here rather than in the feature because the sections are a property of the *report*, not of the
 * card: [Recap] is what decides there are four of them and what each one has to say.
 *
 * The `name` is what travels, so it stays in Kotlin — the rule every persisted or compared enum
 * name in this app follows. The words on screen are `:feature:coach`'s string resources.
 */
enum class ReportSection {
    Nutrition,
    Steps,
    Training,
    Weight,
}
