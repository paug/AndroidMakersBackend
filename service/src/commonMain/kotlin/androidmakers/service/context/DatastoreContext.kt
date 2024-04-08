package androidmakers.service.context

import com.apollographql.apollo3.api.ExecutionContext
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions

class DatastoreContext: ExecutionContext.Element {
    private var datastore: Datastore? = null

    @Synchronized
    fun datastore(): Datastore {
        if (datastore == null) {
            datastore = DatastoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.getApplicationDefault()).build().service
        }
        return datastore!!
    }

    companion object Key: ExecutionContext.Key<DatastoreContext>

    override val key: ExecutionContext.Key<DatastoreContext>
        get() = Key
}

internal fun ExecutionContext.datastore(): Datastore = get(DatastoreContext)!!.datastore()