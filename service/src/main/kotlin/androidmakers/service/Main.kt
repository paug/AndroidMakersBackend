@file:Suppress("OPT_IN_USAGE")

package androidmakers.service

import androidmakers.openfeedback.openfeedbackModule
import androidmakers.service.context.AuthenticationContext
import androidmakers.service.context.CacheControlContext
import androidmakers.service.context.DatastoreContext
import androidmakers.service.context.maxAge
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.execution.ktor.respondGraphQL
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.datastore.DatastoreOptions
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.time.Duration.Companion.minutes

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    /**
     * Block to get the data just after boot
     */
    runBlocking {
        Sessionize.importAndroidMakers2025()
    }
    /**
     * And then update every 5min.
     * A better version of this would use webhooks but Sessionize doesn't have that yet.
     */
    GlobalScope.launch {
        while (true) {
            delay(5.minutes)
            Sessionize.importAndroidMakers2025()
        }
    }

    embeddedServer(Netty, port = System.getenv("POST")?.toIntOrNull() ?: 8080) {
        install(CORS) {
            anyHost()
            allowNonSimpleContentTypes = true
            allowHeader("Authorization")
            allowHeader("conference")
        }

        routing {
            val executableSchema = androidmakers.graphql.AndroidMakersExecutableSchemaBuilder().build()
            post("/graphql") {
                call.respondGraphQL2(executableSchema)
            }
            get("/graphql") {
                call.respondGraphQL2(executableSchema)
            }
            get(Regex("/sandbox/?")) {
                call.respondRedirect(call.url { path("/sandbox/index.html") })
            }
            get("/sandbox/index.html") {
                call.respondText(sandboxIndex, ContentType.parse("text/html"))
            }
        }
        openfeedbackModule("/openfeedback.json")
    }.start(wait = true)
}

val datastore = DatastoreOptions.newBuilder()
    .setCredentials(GoogleCredentials.getApplicationDefault()).build().service

private fun Any?.toJsonString(): String {
    return this.toJsonElement().toString()
}

private fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is List<*> -> JsonArray(map { it.toJsonElement() })
        is Map<*, *> -> JsonObject(map { it.key as String to it.value.toJsonElement() }.toMap())
        else -> error("Cannot serialize '$this' to JSON")
    }
}

private suspend fun RoutingCall.respondGraphQL2(executableSchema: ExecutableSchema) {
    val authResult = firebaseUid()
    var uid: String? =null
    when (authResult) {
        is FirebaseUidResult.Error -> {
            respondText(ContentType.parse("application/json"), HttpStatusCode.Unauthorized) {
                mapOf("type" to "signout", "firebaseError" to authResult.code).toJsonString()
            }
            return
        }
        FirebaseUidResult.Expired -> {
            respondText(ContentType.parse("application/json"), HttpStatusCode.Unauthorized) {
                mapOf("type" to "refresh").toJsonString()
            }
            return
        }
        is FirebaseUidResult.SignedIn -> {
            uid = authResult.uid
        }
        FirebaseUidResult.SignedOut -> {
            uid = null
        }
    }
    val context = context(uid)
    respondGraphQL(executableSchema, context) {
        var maxAge = context.maxAge()

        if (it?.errors != null) {
            // Do not cache errors
            maxAge = 0
        }
        headers {
            if (maxAge == 0L) {
                append("Cache-Control", "no-store")
            } else {
                append("Cache-Control", "public, max-age=$maxAge")
            }
        }
    }
}

private fun ApplicationCall.firebaseUid(): FirebaseUidResult {
    val idToken = request.headers.get("authorization")
        ?.substring("Bearer ".length)

    if (idToken == null) {
        return FirebaseUidResult.SignedOut
    }

    return idToken.firebaseUid()
}

private fun ApplicationCall.context(uid: String?): ExecutionContext {
    return AuthenticationContext(uid) + DatastoreContext(datastore) + CacheControlContext()
}