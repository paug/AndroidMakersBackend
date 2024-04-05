package androidmakers.openfeedback

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.atomicfu.locks.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import sessionize.sessionizeData
import kotlinx.serialization.*
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
}

fun Application.openfeedbackModule(path: String) {
    routing {
        get(path) {
            call.respondText(
                contentType = ContentType.parse("application/json"),
                text = openfeedbackData()
            )
        }
    }
}


private val lock = ReentrantLock()

private var lastMillis: Long = 0
private var lastValue: String? = null
private fun openfeedbackData(): String = lock.withLock {
    if (lastValue != null && (Clock.systemUTC().millis()  - lastMillis) < 60_000) {
        // Make sure we're not calling sessionize too often
        return@withLock lastValue!!
    }
    val seData = sessionizeData()
    val ofData = OfData(
        sessions = seData.sessions.filter { !it.isServiceSession }.map {
            OfSession(
                id = it.id,
                title = it.title,
                startTime = it.startsAt.toString(),
                endTime = it.endsAt.toString(),
                speakers = it.speakers,
                trackTitle = "",
            )
        }.associateBy { it.id },
        speakers = seData.speakers.map {
            OfSpeaker(
                id = it.id,
                name = it.fullName,
                socials = it.links.map {
                    OfSocial(name = it.title, link = it.url)
                },
                photoUrl = it.profilePicture
            )
        }.associateBy { it.id }
    )

    val value = json.encodeToString(ofData)
    lastValue = value
    lastMillis = Clock.systemUTC().millis()
    return value
}

@Serializable
class OfData(
    val sessions: Map<String, OfSession>,
    val speakers: Map<String, OfSpeaker>
)

@Serializable
class OfSession(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val speakers: List<String>,
    val trackTitle: String
)

@Serializable
class OfSpeaker(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val socials: List<OfSocial>
)


@Serializable
class OfSocial(
    val name: String,
    val link: String
)
