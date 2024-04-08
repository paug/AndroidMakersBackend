@file:Suppress("OPT_IN_USAGE")

package androidmakers.service

import androidmakers.openfeedback.openfeedbackModule
import androidmakers.service.context.AuthenticationContext
import androidmakers.service.context.CacheControlContext
import androidmakers.service.context.DatastoreContext
import androidmakers.service.context.maxAge
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.ktor.respondGraphQL
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    /**
     * Block to get the data just after boot
     */
    runBlocking {
        Sessionize.importAndroidMakers2024()
    }
    /**
     * And then update every 5min.
     * A better version of this would use webhooks but Sessionize doesn't have that yet.
     */
    GlobalScope.launch {
        while (true) {
            delay(5.minutes)
            Sessionize.importAndroidMakers2024()
        }
    }
    embeddedServer(Netty, port = System.getenv("POST")?.toIntOrNull() ?: 8080) {
        install(CORS) {
            anyHost()
            allowNonSimpleContentTypes = true
        }

        routing {
            val executableSchema = androidmakers.graphql.AndroidmakersExecutableSchemaBuilder().build()
            post("/graphql") {
                apolloCall(executableSchema)
            }
            get("/graphql") {
                apolloCall(executableSchema)
            }
            get("/sandbox/index.html") {
                call.respondText(sandboxIndex, ContentType.parse("text/html"))
            }
        }
        openfeedbackModule("/openfeedback.json")
    }.start(wait = true)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.apolloCall(executableSchema: ExecutableSchema) {
    val context = call.context()
    call.respondGraphQL(executableSchema, context) {
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

private fun ApplicationCall.firebaseUid(): String? {
    return try {
        request.headers.get("authorization")
            ?.substring("Bearer ".length)
            ?.firebaseUid()
    } catch (e: FirebaseAuthException) {
        throw e
    }
}

private fun ApplicationCall.context(): ExecutionContext {
    return AuthenticationContext(firebaseUid()) + DatastoreContext() + CacheControlContext()
}