package ph.mart.healthapp.feature.training.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseParseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.exercise.lastPerformances
import ph.mart.healthapp.core.data.exercise.recentLiftNames
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * Shared by the log-exercise sheet and the strength workout screen — one form, two presentations,
 * so there is no second ViewModel and therefore no second flow package (see CLAUDE.md's rule).
 *
 * The always-on read side is three fields off one combine: the weight the MET estimate multiplies
 * by, the unit the strength screen prints loads in, and whether a save actually raises today's
 * budget. The weight is the latest weigh-in rather than `Profile.weightKg`, which is the
 * onboarding weight and is never updated — same fallback rule `trendVsSevenDaysAgo(fallbackKg)`
 * uses on Home.
 *
 * Everything the strength screen needs is loaded on demand instead, by
 * [LogExerciseEvent.OnOpenStrength]: the sheet shares this container, and it shows none of it.
 *
 * The two AI dependencies are the sheet's alone, and they are the reason this stayed one
 * ViewModel: a describe field is a second *presentation* of the same form, not a second form, so
 * `:feature:training` keeps its flat package exactly as `StrengthWorkoutScreen` does.
 */
class LogExerciseViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseParseRepository: ExerciseParseRepository,
    private val networkMonitor: NetworkMonitor,
    profileRepository: ProfileRepository,
    progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<LogExerciseUiState, LogExerciseUiState, LogExerciseSideEffect> {

    /** The strength screen's `LaunchedEffect` re-fires on an Activity recreation while this
     * ViewModel survives it, so the routine collection has to be started at most once. */
    private var routinesObserved = false

    /** Lets [LogExerciseEvent.OnCancelParse] cancel just the in-flight call, the way the photo
     * flow's `analysisJob` and talk-to-log's `parseJob` do — cancellation reaches the Firebase AI
     * SDK cooperatively, and `ExerciseParseRepositoryImpl` rethrows it rather than logging a
     * request the user withdrew. */
    private var parseJob: Job? = null

    override val container = orbitContainer<LogExerciseUiState, LogExerciseSideEffect>(LogExerciseUiState()) {
        observeWeight(profileRepository, progressRepository)
    }

    /** Asked by the sheet at the moment of the tap, not observed: a sheet lives seconds and the
     * only answer that matters is the one true when a request is about to be spent. The offline
     * message is the sheet's, so an offline tap never reaches an intent. */
    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: LogExerciseEvent) {
        when (event) {
            is LogExerciseEvent.OnSave -> onSave(event.form, event.dateEpochDay, event.editingId)
            is LogExerciseEvent.OnLoadEditing -> onLoadEditing(event.id)
            is LogExerciseEvent.OnOpenStrength -> onOpenStrength(event.editingId, event.routineId)
            is LogExerciseEvent.OnSaveRoutine -> onSaveRoutine(event.name, event.lifts)
            is LogExerciseEvent.OnParse -> onParse(event.text)
            is LogExerciseEvent.OnParseSets -> onParseSets(event.text)
            LogExerciseEvent.OnCancelParse -> onCancelParse()
        }
    }

    private fun observeWeight(
        profileRepository: ProfileRepository,
        progressRepository: ProgressRepository,
    ) = intent {
        combine(
            profileRepository.observeProfile(),
            progressRepository.observeWeightEntries(),
        ) { profile, entries ->
            entries.maxByOrNull { it.dateEpochDay }?.weightKg to profile
        }.collect { (latestKg, profile) ->
            // Copied onto the state rather than replacing it: the strength load below can land
            // first, and a weigh-in landing after it must not erase the workout being edited.
            reduce {
                state.copy(
                    weightKg = latestKg ?: profile?.weightKg ?: LogExerciseUiState().weightKg,
                    preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric,
                    addExerciseToBudget = profile?.addExerciseToBudget != false,
                )
            }
        }
    }

    /** The sheet's share of [onOpenStrength]'s first read, and nothing else: it shows no chips,
     * no last workout and no routines. */
    private fun onLoadEditing(id: Long) = intent {
        val entry = exerciseRepository.entry(id)
        reduce { state.copy(editing = entry) }
    }

    /** One read for all four: the row being corrected, the session to repeat, the chips, and what
     * each lift was last trained at. [strengthLoaded] is what the screen waits on before it
     * composes a form. */
    private fun onOpenStrength(editingId: Long, routineId: Long) = intent {
        val recent = exerciseRepository.recentStrengthEntries()
        val editing = editingId.takeIf { it > 0 }?.let { exerciseRepository.entry(it) }
        // Read before [strengthLoaded] flips, not off the collection below: the screen seeds its
        // saveable form the moment it composes, and a routine arriving one emission later would
        // re-key that saver — the same trap the hold-back exists for.
        val seedRoutine = routineId.takeIf { it > 0 && editingId <= 0 }?.let { id ->
            routineRepository.observeRoutines().first().firstOrNull { it.id == id }
        }
        reduce {
            state.copy(
                editing = editing,
                // Never the row being corrected: "repeat" would then offer the workout already
                // on screen, which is the one session it can't usefully seed.
                lastWorkout = recent.firstOrNull { it.id != editingId },
                recentLifts = recent.recentLiftNames(),
                // A third fold over the same read — no extra query, and it cannot disagree with
                // the chips it sits beside.
                lastLifts = recent.lastPerformances(),
                seedRoutine = seedRoutine,
                strengthLoaded = true,
            )
        }
        observeRoutines()
    }

    /** Observed rather than read once, unlike everything above: saving a routine from this screen
     * has to show up in its own chip row without a reload. */
    private fun observeRoutines() {
        if (routinesObserved) return
        routinesObserved = true
        intent {
            routineRepository.observeRoutines().collect { routines ->
                reduce { state.copy(routines = routines) }
            }
        }
    }

    private fun onSaveRoutine(name: String, lifts: List<RoutineLift>) = intent {
        routineRepository.addRoutine(name, lifts)
    }

    /** The repository swallows everything but a cancellation, so the only way out of this
     * without reaching the last line is [onCancelParse] — which lowers the flag itself. */
    private fun onParse(text: String) {
        parseJob = intent {
            reduce { state.copy(parsing = true) }
            val result = exerciseParseRepository.parse(text)
            reduce { state.copy(parsing = false) }
            postSideEffect(LogExerciseSideEffect.Parsed(result))
        }
    }

    /** [onParse] on the same job and flag. The unit is the one the screen draws loads in, applied
     * on-device to a load said without one — it is never sent. */
    private fun onParseSets(text: String) {
        parseJob = intent {
            reduce { state.copy(parsing = true) }
            val result = exerciseParseRepository.parseSets(text, state.preferredUnit)
            reduce { state.copy(parsing = false) }
            postSideEffect(LogExerciseSideEffect.SetsParsed(result))
        }
    }

    /**
     * Two steps and a second intent, because the first one kills the coroutine the reduce would
     * otherwise have run in. Called by back, by the cancel button **and by dismissing the sheet**:
     * this ViewModel outlives the sheet, so a spinner abandoned mid-parse would still be spinning
     * the next time the FAB opened a blank one.
     */
    private fun onCancelParse() {
        parseJob?.cancel()
        intent { reduce { state.copy(parsing = false) } }
    }

    private fun onSave(form: LogExerciseForm, dateEpochDay: Long, editingId: Long?) = intent {
        val entry = form.toExerciseEntry(dateEpochDay)
        if (editingId == null) {
            exerciseRepository.addEntry(entry)
        } else {
            exerciseRepository.updateEntry(entry.copy(id = editingId))
        }
        // Zero on a correction as firmly as on a switched-off credit: reopening Tuesday's run to
        // fix its duration is not a workout anybody just did, and congratulating it would make
        // the confirmation fire on an edit loop.
        val credited = when {
            editingId != null -> 0
            state.addExerciseToBudget -> form.burnedKcal
            else -> 0
        }
        postSideEffect(LogExerciseSideEffect.Saved(credited))
    }
}
