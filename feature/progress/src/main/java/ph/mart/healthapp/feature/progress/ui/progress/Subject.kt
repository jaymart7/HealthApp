package ph.mart.healthapp.feature.progress.ui.progress

import androidx.annotation.StringRes
import ph.mart.healthapp.feature.progress.R

/**
 * The four families the twelve metric subjects sort into. The grouping is what replaced a
 * thirteen-pill scrolling tab strip: past the fifth pill nothing was reachable without a swipe
 * nobody knew to make, and no pill said whether there was anything behind it.
 *
 * Names are the handoff's. The subject *sets* are what matter — "Training" and "Wellbeing" are
 * labels over "the two you do on purpose" and "the four a watch or a two-tap reflection reports".
 */
enum class SubjectGroup(@StringRes val label: Int) {
    Body(R.string.progress_group_body),
    Nutrition(R.string.progress_group_nutrition),
    Training(R.string.progress_group_training),
    Wellbeing(R.string.progress_group_wellbeing),
}

/** Which theme role a subject's card preview and trend line draw in. */
enum class SubjectAccent { Primary, Secondary }

/**
 * One readable subject on the Progress tab — a card on the overview and a detail page behind it.
 * Replaces `ProgressTab`, whose only job was to name a pill.
 *
 * [group] is null for exactly one entry, [Badges], which is drawn as a summary row under the four
 * grids rather than a metric card: it is an achievement list, not a trend, and a card promising a
 * chart would be a card that lies. It still has a detail page, so all thirteen stay reachable.
 *
 * [accent] is an explicit column rather than a fold over [group] because Activity breaks the
 * pattern — it sits in Training with the lifting, but its steps come off a watch like Sleep's and
 * Heart's do, and the handoff draws every imported series in `secondary`.
 *
 * [emptyHint] is the line under "Nothing yet" on an empty card. It is a door to the detail page,
 * which carries the explanation — except Blood pressure's, the one subject with a logging sheet
 * already on this screen, whose hint opens that sheet instead.
 */
/**
 * [coachQuestion] is what this page would ask the coach, and **null means the page carries no
 * coach action at all**.
 *
 * The nine that have one are the nine the coach can answer: its tools reach food and macros,
 * water, training, steps, sleep, mood, fasting, the weight trend, the change in a body
 * measurement and — since the two read tools began carrying them — the supplements they ticked
 * off against what was due. Nothing else. A button on Heart would buy a
 * shrug, and a shrug reads as a broken feature — the rule the follow-up chips already follow.
 * This is the one place that decision lives, so a page cannot disagree with it.
 */
enum class Subject(
    @StringRes val label: Int,
    val group: SubjectGroup?,
    val accent: SubjectAccent,
    @StringRes val emptyHint: Int,
    @StringRes val coachQuestion: Int? = null,
) {
    Weight(R.string.progress_subject_weight, SubjectGroup.Body, SubjectAccent.Primary, R.string.progress_hint_weight, R.string.progress_ask_weight),
    Photos(R.string.progress_subject_photos, SubjectGroup.Body, SubjectAccent.Primary, R.string.progress_hint_photos),
    Measurements(R.string.progress_subject_measurements, SubjectGroup.Body, SubjectAccent.Primary, R.string.progress_hint_measurements, R.string.progress_ask_measurements),
    Nutrition(R.string.progress_subject_nutrition, SubjectGroup.Nutrition, SubjectAccent.Primary, R.string.progress_hint_nutrition, R.string.progress_ask_nutrition),
    Fasting(R.string.progress_subject_fasting, SubjectGroup.Nutrition, SubjectAccent.Primary, R.string.progress_hint_fasting, R.string.progress_ask_fasting),
    Supplements(R.string.progress_subject_supplements, SubjectGroup.Nutrition, SubjectAccent.Primary, R.string.progress_hint_supplements, R.string.progress_ask_supplements),
    Activity(R.string.progress_subject_activity, SubjectGroup.Training, SubjectAccent.Secondary, R.string.progress_hint_activity, R.string.progress_ask_activity),
    Strength(R.string.progress_subject_strength, SubjectGroup.Training, SubjectAccent.Primary, R.string.progress_hint_strength, R.string.progress_ask_strength),
    Sleep(R.string.progress_subject_sleep, SubjectGroup.Wellbeing, SubjectAccent.Secondary, R.string.progress_hint_sleep, R.string.progress_ask_sleep),
    Mood(R.string.progress_subject_mood, SubjectGroup.Wellbeing, SubjectAccent.Secondary, R.string.progress_hint_mood, R.string.progress_ask_mood),
    Cycle(R.string.progress_subject_cycle, SubjectGroup.Wellbeing, SubjectAccent.Secondary, R.string.progress_hint_cycle),
    Heart(R.string.progress_subject_heart, SubjectGroup.Wellbeing, SubjectAccent.Secondary, R.string.progress_hint_heart),
    BloodPressure(R.string.progress_subject_pressure, SubjectGroup.Wellbeing, SubjectAccent.Secondary, R.string.progress_hint_pressure),
    Badges(R.string.progress_subject_badges, group = null, accent = SubjectAccent.Primary, emptyHint = R.string.progress_hint_none),
}

/**
 * The subjects in [group], in declaration order — the order the grid draws them in before
 * tracked-before-empty sorting moves the ones with nothing to say to the end.
 *
 * [cycleTracking] is the one thing that can take a subject out of the grid entirely, and it is
 * `Profile.cycleTrackingOn`. Every other empty subject keeps its slot dashed because it is empty
 * for want of data; Cycle may be permanently irrelevant to whoever is holding the phone, and a
 * card that can never say anything is worse than no card.
 */
fun subjectsIn(group: SubjectGroup, cycleTracking: Boolean = true): List<Subject> =
    Subject.entries.filter { it.group == group && (cycleTracking || it != Subject.Cycle) }
