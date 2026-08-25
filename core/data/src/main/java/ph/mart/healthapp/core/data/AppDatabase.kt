package ph.mart.healthapp.core.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity
import ph.mart.healthapp.core.data.profile.local.ProfileDao
import ph.mart.healthapp.core.data.profile.local.ProfileEntity
import ph.mart.healthapp.core.data.progress.local.MeasurementEntryDao
import ph.mart.healthapp.core.data.progress.local.MeasurementEntryEntity
import ph.mart.healthapp.core.data.progress.local.ProgressPhotoDao
import ph.mart.healthapp.core.data.progress.local.ProgressPhotoEntity
import ph.mart.healthapp.core.data.progress.local.WeightEntryDao
import ph.mart.healthapp.core.data.progress.local.WeightEntryEntity

@Database(
    entities = [
        ProfileEntity::class,
        FoodEntryEntity::class,
        WeightEntryEntity::class,
        MeasurementEntryEntity::class,
        ProgressPhotoEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun measurementEntryDao(): MeasurementEntryDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
}
