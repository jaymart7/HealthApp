package ph.mart.healthapp.core.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ph.mart.healthapp.core.data.profile.local.ProfileDao
import ph.mart.healthapp.core.data.profile.local.ProfileEntity

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = true)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
