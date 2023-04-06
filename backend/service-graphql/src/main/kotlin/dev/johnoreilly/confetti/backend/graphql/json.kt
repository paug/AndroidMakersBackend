package dev.johnoreilly.confetti.backend.graphql

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Serializable
class SeData(
    val sessions: List<JsonSession>,
    val speakers: List<JsonSpeaker>,
    val partners: List<JsonPartnerGroup>,
    val venues: List<JsonVenue>
)

@Serializable
class JsonSpeaker(
    val id: String,
    val fullName: String,
    val profilePicture: String? = null,
    val links: List<SeLink>,
    val tagLine: String? = null,
    val bio: String? = null,
)

@Serializable
class SeLink(
    val title: String,
    val url: String
)

@Serializable
class JsonSession(
    val id: String,
    val title: String,
    val description: String,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val speakers: List<String>,
    val roomId: Int? = 0,
)


private val json = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
val seData = JsonSession::class.java.classLoader.getResourceAsStream("data.json").use {
    json.decodeFromStream<SeData>(it)
}

fun JsonSession.toSession() = Session(
    id = id,
    title = title,
    description = description,
    startsAt = LocalDateTime.parse(startsAt!!),
    endsAt = LocalDateTime.parse(endsAt!!),
    speakerIds = speakers.toSet(),
    shortDescription = null,
    language = null,
    tags = emptyList(),
    startInstant = LocalDateTime.parse(startsAt).toInstant(TimeZone.UTC),
    endInstant = LocalDateTime.parse(endsAt).toInstant(TimeZone.UTC),
    roomIds = setOf(roomId.toString()),
    complexity = null,
    feedbackId = id,
    type = "talk",
)

fun JsonSpeaker.toSpeaker() = Speaker(
    id = id,
    name = fullName,
    tagline = tagLine,
    photoUrl = profilePicture,
    socials = links.map { Social(link = it.url, name = it.title, icon = null) },
    bio = bio,
    company = null,
    companyLogoUrl = null,
    city = null,
    sessionIds = emptyList()
)


@Serializable
class JsonPartnerGroup(
    val kind: String,
    val items: List<JsonPartner>
)

@Serializable
class JsonPartner(
    val name: String,
    val url: String,
    val photoUrl: String
)

@Serializable
class JsonVenue(
    val id: String,
    val name: String,
    val address: String
)

fun JsonVenue.toVenue() = Venue(
    id = id,
    name = name,
    address = address,
    latitude = null,
    longitude = null,
    imageUrl = null,
    floorPlanUrl = null,
    descriptions = emptyMap()
)

fun JsonPartnerGroup.toPartnerGroup() = PartnerGroup(
    title = this.kind,
    partners = this.items.map {
        it.toPartner()
    }
)

private fun JsonPartner.toPartner(): Partner {
    return Partner(
        name = this.name,
        url = this.url,
        logoUrl = this.photoUrl,
    )
}
