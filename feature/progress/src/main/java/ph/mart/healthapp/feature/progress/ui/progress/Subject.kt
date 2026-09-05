package ph.mart.healthapp.feature.progress.ui.progress

/**
 * The four families the twelve metric subjects sort into. The grouping is what replaced a
 * thirteen-pill scrolling tab strip: past the fifth pill nothing was reachable without a swipe
 * nobody knew to make, and no pill said whether there was anything behind it.
 *
 * Names are the handoff's. The subject *sets* are what matter — "Training" and "Wellbeing" are
 * labels over "the two you do on purpose" and "the four a watch or a two-tap reflection reports".
 */
enum class SubjectGroup(val label: String) {
    Body("Body"),
    Nutrition("Nutrition"),
    Training("Training"),
    Wellbeing("Wellbeing"),
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
enum class Subject(
    val label: String,
    val group: SubjectGroup?,
    val accent: SubjectAccent,
    val emptyHint: String,
) {
    Weight("Weight", SubjectGroup.Body, SubjectAccent.Primary, "How weight tracking works"),
    Photos("Photos", SubjectGroup.Body, SubjectAccent.Primary, "How progress photos work"),
    Measurements("Measurements", SubjectGroup.Body, SubjectAccent.Primary, "How measurements work"),
    Nutrition("Food", SubjectGroup.Nutrition, SubjectAccent.Primary, "How the food diary works"),
    Fasting("Fasting", SubjectGroup.Nutrition, SubjectAccent.Primary, "How fasting works"),
    Supplements("Supplements", SubjectGroup.Nutrition, SubjectAccent.Primary, "How supplements work"),
    Activity("Activity", SubjectGroup.Training, SubjectAccent.Secondary, "How activity works"),
    Strength("Strength", SubjectGroup.Training, SubjectAccent.Primary, "How strength works"),
    Sleep("Sleep", SubjectGroup.Wellbeing, SubjectAccent.Secondary, "How sleep works"),
    Mood("Mood", SubjectGroup.Wellbeing, SubjectAccent.Secondary, "How mood works"),
    Cycle("Cycle", SubjectGroup.Wellbeing, SubjectAccent.Secondary, "Log a day"),
    Heart("Heart", SubjectGroup.Wellbeing, SubjectAccent.Secondary, "How heart rate works"),
    BloodPressure("Blood pressure", SubjectGroup.Wellbeing, SubjectAccent.Secondary, "Log a reading"),
    Badges("Badges", group = null, accent = SubjectAccent.Primary, emptyHint = ""),
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
