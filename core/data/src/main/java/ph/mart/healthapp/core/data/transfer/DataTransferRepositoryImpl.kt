package ph.mart.healthapp.core.data.transfer

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The same sequence the Profile screen used to run itself, moved behind one transaction.
 *
 * It composes the domain repositories rather than reaching for their DAOs: every clamp and default
 * an ordinary write goes through — `addReading`'s pressure ranges, `upsertSession`'s goal hours,
 * `upsertDay`'s supplement counts — has to apply to an imported row too, and duplicating that here
 * is exactly how the two would drift. Room's writer connection is confined to this coroutine, so a
 * DAO call made from inside the block joins the open transaction instead of opening its own.
 */
internal class DataTransferRepositoryImpl(
    private val database: AppDatabase,
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val moodRepository: MoodRepository,
    private val fastingRepository: FastingRepository,
    private val supplementRepository: SupplementRepository,
    private val bloodPressureRepository: BloodPressureRepository,
) : DataTransferRepository {

    override suspend fun replaceAll(data: ImportData) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                data.profile?.let { profileRepository.saveProfile(it) }

                // Weight and measurements are upserted by date rather than cleared first, so an
                // import merges history instead of discarding entries the file doesn't mention.
                foodRepository.deleteAllEntries()
                foodRepository.addEntries(data.foodEntries)
                data.weightEntries.forEach { progressRepository.upsertWeightEntry(it) }
                data.measurements.forEach { progressRepository.upsertMeasurementEntry(it) }

                waterRepository.clearAllDays()
                data.waterDays.forEach { waterRepository.upsertDay(it) }

                exerciseRepository.deleteAllEntries()
                data.exercises.forEach { exerciseRepository.addEntry(it) }

                moodRepository.clearAllDays()
                data.moodDays.forEach { moodRepository.upsertDay(it) }

                // Clears a running fast too, which is the honest reading of replace-in-full: the
                // timer belongs to the history being replaced, not to the device.
                fastingRepository.clearAllSessions()
                data.fastSessions.forEach { fastingRepository.upsertSession(it) }

                // Supplements before their days: a day row points at a supplement id, and the ids
                // are restored verbatim rather than regenerated so the ticks keep their subject.
                supplementRepository.clearAll()
                data.supplements.forEach { supplementRepository.upsertSupplement(it) }
                data.supplementDays.forEach { supplementRepository.upsertDay(it) }

                bloodPressureRepository.clearAllReadings()
                data.bloodPressure.forEach { bloodPressureRepository.addReading(it) }
            }
        }
    }
}
