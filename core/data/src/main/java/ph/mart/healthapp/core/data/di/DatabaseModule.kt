package ph.mart.healthapp.core.data.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "fitpulse.db")
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            // ponytail: no shipped users yet, no Migration objects exist to extend — destructive
            // fallback is fine pre-release. Replace with real Migrations before shipping v2+.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
