package ph.mart.healthapp.core.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity
import ph.mart.healthapp.core.data.profile.local.ProfileDao
import ph.mart.healthapp.core.data.profile.local.ProfileEntity

@Database(entities = [ProfileEntity::class, FoodEntryEntity::class], version = 1, exportSchema = true)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodEntryDao(): FoodEntryDao
}
