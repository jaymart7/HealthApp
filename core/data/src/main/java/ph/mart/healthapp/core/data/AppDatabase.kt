package ph.mart.healthapp.core.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryDao
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryEntity
import ph.mart.healthapp.core.data.food.local.FavoriteFoodDao
import ph.mart.healthapp.core.data.food.local.FavoriteFoodEntity
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity
import ph.mart.healthapp.core.data.food.local.SavedMealDao
import ph.mart.healthapp.core.data.food.local.SavedMealEntity
import ph.mart.healthapp.core.data.food.local.SavedMealItemEntity
import ph.mart.healthapp.core.data.health.local.HealthLinkDao
import ph.mart.healthapp.core.data.health.local.HealthLinkEntity
import ph.mart.healthapp.core.data.health.local.SleepDayDao
import ph.mart.healthapp.core.data.health.local.SleepDayEntity
import ph.mart.healthapp.core.data.mood.local.MoodDayDao
import ph.mart.healthapp.core.data.mood.local.MoodDayEntity
import ph.mart.healthapp.core.data.profile.local.ProfileDao
import ph.mart.healthapp.core.data.profile.local.ProfileEntity
import ph.mart.healthapp.core.data.progress.local.MeasurementEntryDao
import ph.mart.healthapp.core.data.progress.local.MeasurementEntryEntity
import ph.mart.healthapp.core.data.progress.local.ProgressPhotoDao
import ph.mart.healthapp.core.data.progress.local.ProgressPhotoEntity
import ph.mart.healthapp.core.data.progress.local.WeightEntryDao
import ph.mart.healthapp.core.data.progress.local.WeightEntryEntity
import ph.mart.healthapp.core.data.water.local.WaterDayDao
import ph.mart.healthapp.core.data.water.local.WaterDayEntity

@Database(
    entities = [
        ProfileEntity::class,
        FoodEntryEntity::class,
        FavoriteFoodEntity::class,
        SavedMealEntity::class,
        SavedMealItemEntity::class,
        WeightEntryEntity::class,
        MeasurementEntryEntity::class,
        ProgressPhotoEntity::class,
        WaterDayEntity::class,
        ExerciseEntryEntity::class,
        MoodDayEntity::class,
        HealthLinkEntity::class,
        SleepDayEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun favoriteFoodDao(): FavoriteFoodDao
    abstract fun savedMealDao(): SavedMealDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun measurementEntryDao(): MeasurementEntryDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun waterDayDao(): WaterDayDao
    abstract fun exerciseEntryDao(): ExerciseEntryDao
    abstract fun moodDayDao(): MoodDayDao
    abstract fun healthLinkDao(): HealthLinkDao
    abstract fun sleepDayDao(): SleepDayDao
}
