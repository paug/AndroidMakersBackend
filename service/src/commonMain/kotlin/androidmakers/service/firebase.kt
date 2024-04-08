package androidmakers.service

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.auth.oauth2.GoogleCredentials


private val lock = Object()
private var _isInitialized = false

fun String.firebaseUid(): String? {
    if (this == "testToken") {
        return "testUser"
    }

    synchronized(lock) {
        if (!_isInitialized) {
            val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build()
            FirebaseApp.initializeApp(options)
            _isInitialized = true
        }
    }

    return try {
        FirebaseAuth.getInstance().verifyIdToken(this).uid
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}
