package androidmakers.service.context

import com.apollographql.apollo3.api.ExecutionContext
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions

class CacheControlContext: ExecutionContext.Element {
    internal var maxAge = 1800L

    @Synchronized
    fun updateMaxAge(maxAge: Long) {
        if (maxAge < this.maxAge) {
            this.maxAge = maxAge
        }
    }

    companion object Key: ExecutionContext.Key<CacheControlContext>

    override val key: ExecutionContext.Key<CacheControlContext>
        get() = Key
}

internal fun ExecutionContext.updateMaxAge(maxAge: Long) = get(CacheControlContext)!!.updateMaxAge(maxAge)
internal fun ExecutionContext.maxAge() = get(CacheControlContext)!!.maxAge