package ph.mart.healthapp.wear

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ph.mart.healthapp.wear.di.wearModule

/**
 * Koin, one module wide. The phone's graph is fourteen data modules and six feature ones; the
 * watch has a single repository and a single ViewModel, because the watch has no database — see
 * [ph.mart.healthapp.wear.data.WearSnapshotRepository].
 */
class FitPulseWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FitPulseWearApplication)
            modules(wearModule)
        }
    }
}
