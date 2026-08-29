package ph.mart.healthapp.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

fun initAppCheck() {
    val firebaseAppCheck = FirebaseAppCheck.getInstance()
    firebaseAppCheck.installAppCheckProviderFactory(
        PlayIntegrityAppCheckProviderFactory.getInstance()
    )
}
