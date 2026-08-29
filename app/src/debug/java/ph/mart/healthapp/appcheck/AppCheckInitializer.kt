package ph.mart.healthapp.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

fun initAppCheck() {
    val firebaseAppCheck = FirebaseAppCheck.getInstance()
    firebaseAppCheck.installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
    )
}
