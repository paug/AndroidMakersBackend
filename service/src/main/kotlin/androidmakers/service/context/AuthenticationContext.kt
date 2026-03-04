package androidmakers.service.context

import androidmakers.service.firebaseEmail
import com.apollographql.apollo.api.ExecutionContext

internal class AuthenticationContext(val uid: String?) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<*>
        get() = Key

    companion object Key : ExecutionContext.Key<AuthenticationContext>

    val email: String? by lazy {
        uid?.let {
            firebaseEmail(it)
        }
    }
}

internal fun ExecutionContext.uid() = this.get(AuthenticationContext)?.uid

