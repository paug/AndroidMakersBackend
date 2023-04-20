package am.backend.graphql

import data.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import sessionize.SeRoom
import sessionize.SeSession
import sessionize.SeSpeaker

private val json = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
val seData = SeSession::class.java.classLoader.getResourceAsStream("data.json").use {
    json.decodeFromStream<JsonData>(it)
}

val frenchItemId = seData.categories.single { it.title == "Language" }.items.single { it.name == "French" }.id
val englishItemId = seData.categories.single { it.title == "Language" }.items.single { it.name == "English" }.id

fun SeSession.toSession() = Session(
    id = id,
    title = title,
    description = description,
    startsAt = LocalDateTime.parse(startsAt!!),
    endsAt = LocalDateTime.parse(endsAt!!),
    speakerIds = speakers.toSet(),
    shortDescription = null,
    language = language(),
    tags = emptyList(),
    startInstant = LocalDateTime.parse(startsAt!!).toInstant(TimeZone.UTC),
    endInstant = LocalDateTime.parse(endsAt!!).toInstant(TimeZone.UTC),
    roomIds = setOf(roomId.toString()),
    complexity = null,
    feedbackId = id,
    type = "talk",
    isServiceSession = isServiceSession,
)

private fun SeSession.language(): String {
    return when {
        categoryItems.contains(frenchItemId) -> "French"
        else -> "English"
    }
}

fun SeSpeaker.toSpeaker() = Speaker(
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


fun JsonVenue.toVenue() = Venue(
    id = id,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
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


fun SeRoom.toRoom() = Room(
    id = id.toString(),
    name = name,
    capacity = null
)