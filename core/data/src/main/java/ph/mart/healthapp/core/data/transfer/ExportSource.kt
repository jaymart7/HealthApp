package ph.mart.healthapp.core.data.transfer

import kotlinx.coroutines.flow.first
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.note.NoteRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * Reads every exported domain into one [ImportData]. The one place that list of reads lives:
 * Profile's two export buttons and `BackupWorker` all come through here, so a field added at the
 * next schema version is a change to one file rather than three call sites that have to be kept
 * in step.
 *
 * Still not the `exportAll` [DataTransferRepository] rules out — this adds no repository method
 * and no transaction. Reading is a set of independent calls that cannot leave anything
 * inconsistent, and each still lives on its own domain's repository.
 */
suspend fun collectExport(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    cycleRepository: CycleRepository,
    fastingRepository: FastingRepository,
    supplementRepository: SupplementRepository,
    bloodPressureRepository: BloodPressureRepository,
    noteRepository: NoteRepository,
): ImportData = ImportData(
    profile = profileRepository.observeProfile().first(),
    foodEntries = foodRepository.allEntries(),
    weightEntries = progressRepository.observeWeightEntries().first(),
    measurements = progressRepository.observeMeasurements().first().values.flatten(),
    waterDays = waterRepository.allDays(),
    exercises = exerciseRepository.allEntries(),
    moodDays = moodRepository.allDays(),
    fastSessions = fastingRepository.allSessions(),
    supplements = supplementRepository.allSupplements(),
    supplementDays = supplementRepository.allDays(),
    bloodPressure = bloodPressureRepository.allReadings(),
    cycleDays = cycleRepository.allDays(),
    dayNotes = noteRepository.allNotes(),
)

/** The backup file, and what an import reads back. */
suspend fun exportJson(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    cycleRepository: CycleRepository,
    fastingRepository: FastingRepository,
    supplementRepository: SupplementRepository,
    bloodPressureRepository: BloodPressureRepository,
    noteRepository: NoteRepository,
): String = buildExportJson(
    collectExport(
        profileRepository, foodRepository, progressRepository, waterRepository, exerciseRepository,
        moodRepository, cycleRepository, fastingRepository, supplementRepository,
        bloodPressureRepository, noteRepository,
    ),
)

/** The same data as a zip of CSVs — a spreadsheet's copy, and one nothing reads back. */
suspend fun exportCsvZip(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    cycleRepository: CycleRepository,
    fastingRepository: FastingRepository,
    supplementRepository: SupplementRepository,
    bloodPressureRepository: BloodPressureRepository,
    noteRepository: NoteRepository,
): ByteArray = buildExportCsvZip(
    collectExport(
        profileRepository, foodRepository, progressRepository, waterRepository, exerciseRepository,
        moodRepository, cycleRepository, fastingRepository, supplementRepository,
        bloodPressureRepository, noteRepository,
    ),
)
