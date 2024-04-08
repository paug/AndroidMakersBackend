package androidmakers.service.context

import androidmakers.service.graphql.KIND_BOOKMARKS
import com.apollographql.apollo3.api.ExecutionContext
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.KeyFactory

class DatastoreContext(val datastore: Datastore): ExecutionContext.Element {
    val bookmarksKeyFactory = datastore.newKeyFactory().setKind(KIND_BOOKMARKS)

    companion object Key: ExecutionContext.Key<DatastoreContext>

    override val key: ExecutionContext.Key<DatastoreContext>
        get() = Key
}

internal fun ExecutionContext.datastore(): Datastore = get(DatastoreContext)!!.datastore
internal fun ExecutionContext.bookmarksKeyFactory(): KeyFactory = get(DatastoreContext)!!.bookmarksKeyFactory