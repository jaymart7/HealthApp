package ph.mart.healthapp.core.data.transfer

import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.water.WaterDay

/**
 * A parsed backup, ready to be written. The mirror of `buildExportJson`'s parameter list — the
 * feature that reads the file owns the on-disk DTOs and the version gate, and hands the validated
 * domain types over here.
 *
 * It lives in `:core:data` rather than in `:feature:profile` because the write is a transaction
 * across ten domains, and a `:feature:*` module never touches `AppDatabase`.
 */
data class ImportData(
    val profile: Profile?,
    val foodEntries: List<FoodEntry>,
    val weightEntries: List<WeightEntry>,
    val measurements: List<MeasurementEntry>,
    val waterDays: List<WaterDay>,
    val exercises: List<ExerciseEntry>,
    val moodDays: List<MoodDay>,
    val fastSessions: List<FastSession>,
    val supplements: List<Supplement>,
    val supplementDays: List<SupplementDay>,
    val bloodPressure: List<BloodPressureReading>,
    val cycleDays: List<CycleDay>,
)

/**
 * The one write path an import takes, and the only reason this domain exists.
 *
 * Import is replace-in-full, and it has to be **all-or-nothing**. The previous shape cleared each
 * domain and then replayed the file a row at a time from the ViewModel — several thousand separate
 * transactions on a year of logging, with the diary sitting empty for the tens of seconds in
 * between. A crash, a process kill, or the user leaving mid-import left it wiped and half-restored,
 * with the file already consumed and no way back. One transaction is also what turns those
 * thousands of writes into a single commit.
 *
 * There is no `exportAll` twin: reading is a set of independent `all*()` calls that can't leave
 * anything inconsistent, and each already lives on its own domain's repository.
 */
interface DataTransferRepository {
    suspend fun replaceAll(data: ImportData)
}
