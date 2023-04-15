package sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sessionize.sessionizeData
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

fun main() {
    val seData = sessionizeData()
    var start = LocalDateTime.of(2023, 4, 27, 9, 0).atOffset(ZoneOffset.of("+01:00"))
    val ofData = OfData(
        sessions = seData.sessions.map {
            val end = start + Duration.ofMinutes(30)
            OfSession(
                id = it.id,
                title = it.title,
//                    startTime = it.startsAt.withOffset(),
//                    endTime = it.endsAt.withOffset(),
                startTime = start.toString(),
                endTime = end.toString(),
                speakers = it.speakers
            ).also {
                start = end
            }
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

    val result = json.encodeToString(ofData)

    File("..").resolve("openfeedback_data.json").writeText(result)
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
    val speakers: List<String>
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