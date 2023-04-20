package data

import kotlinx.serialization.Serializable
import sessionize.SeCategory
import sessionize.SeRoom
import sessionize.SeSession
import sessionize.SeSpeaker

@Serializable
data class JsonData(
    val sessions: List<SeSession>,
    val speakers: List<SeSpeaker>,
    val rooms: List<SeRoom>,
    val categories: List<SeCategory>,
    val partners: List<JsonPartnerGroup>,
    val venues: List<JsonVenue>,
)

@Serializable
data class JsonPartnerGroup(
    val kind: String,
    val items: List<JsonPartner>
)

@Serializable
data class JsonPartner(
    val name: String,
    val url: String,
    val photoUrl: String
)

@Serializable
data class JsonVenue(
    val id: String,
    val name: String,
    val address: String,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?
)