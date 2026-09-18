package ph.mart.healthapp.core.data.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.MIGRATIONS

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "fitpulse.db")
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            // No destructive fallback: a version Room cannot reach from throws on open rather
            // than quietly emptying somebody's diary. MIGRATIONS covers every version back to 1.
            .addMigrations(*MIGRATIONS)
            .build()
    }
}
