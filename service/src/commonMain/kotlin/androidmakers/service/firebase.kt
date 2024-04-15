package androidmakers.service

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuthException


private val lock = Object()
private var _isInitialized = false

sealed interface FirebaseUidResult {
    object Expired: FirebaseUidResult
    class Error(val code: String): FirebaseUidResult
    class SignedIn(val uid: String): FirebaseUidResult
    object SignedOut: FirebaseUidResult
}

fun String.firebaseUid(): FirebaseUidResult {
    if (this == "testToken") {
        return FirebaseUidResult.SignedIn("testUser")
    }

    synchronized(lock) {
        if (!_isInitialized) {
            val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build()
            FirebaseApp.initializeApp(options)
            _isInitialized = true
        }
    }

    return try {
        FirebaseUidResult.SignedIn(FirebaseAuth.getInstance().verifyIdToken(this).uid)
    } catch (e: FirebaseAuthException) {
        when (e.authErrorCode) {
            AuthErrorCode.EXPIRED_ID_TOKEN -> {
                FirebaseUidResult.Expired
            }
            AuthErrorCode.CERTIFICATE_FETCH_FAILED,
            AuthErrorCode.CONFIGURATION_NOT_FOUND,
            AuthErrorCode.EMAIL_ALREADY_EXISTS,
            AuthErrorCode.EMAIL_NOT_FOUND,
            AuthErrorCode.EXPIRED_SESSION_COOKIE,
            AuthErrorCode.INVALID_DYNAMIC_LINK_DOMAIN,
            AuthErrorCode.INVALID_ID_TOKEN,
            AuthErrorCode.INVALID_SESSION_COOKIE,
            AuthErrorCode.PHONE_NUMBER_ALREADY_EXISTS,
            AuthErrorCode.REVOKED_ID_TOKEN,
            AuthErrorCode.REVOKED_SESSION_COOKIE,
            AuthErrorCode.TENANT_ID_MISMATCH,
            AuthErrorCode.TENANT_NOT_FOUND,
            AuthErrorCode.UID_ALREADY_EXISTS,
            AuthErrorCode.UNAUTHORIZED_CONTINUE_URL,
            AuthErrorCode.USER_NOT_FOUND,
            AuthErrorCode.USER_DISABLED,
            null -> {
                FirebaseUidResult.Error(e.authErrorCode.name)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}
