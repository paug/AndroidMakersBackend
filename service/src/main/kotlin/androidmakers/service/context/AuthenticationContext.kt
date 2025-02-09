package androidmakers.service.context

import com.apollographql.apollo.api.ExecutionContext

class AuthenticationContext(val uid: String?) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<*>
        get() = Key

    companion object Key : ExecutionContext.Key<AuthenticationContext>
}

internal fun ExecutionContext.uid() = this.get(AuthenticationContext)?.uid