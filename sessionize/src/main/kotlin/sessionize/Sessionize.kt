package sessionize

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request


@Serializable
class SeData(
    val sessions: List<SeSession>,
    val speakers: List<SeSpeaker>,
    val categories: List<SeCategory>,
    val rooms: List<SeRoom>
)

@Serializable
class SeSpeaker(
    val id: String,
    val bio: String? = null,
    val fullName: String,
    val profilePicture: String? = null,
    val links: List<SeLink>,
    val tagLine: String? = null
)

@Serializable
class SeLink(
    val title: String,
    val url: String
)

@Serializable
class SeSession(
    val id: String,
    val title: String,
    val description: String?,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val speakers: List<String>,
    val roomId: Int? = null,
    val categoryItems: List<Int>,
    val isServiceSession: Boolean,
)

@Serializable
class SeRoom(
    val id: Int,
    val name: String,
    val sort: Int
)

@Serializable
class SeCategory(
    val id: Int,
    val title: String,
    val items: List<SeItem>
)

@Serializable
class SeItem(
    val id: Int,
    val name: String,
    val sort: Int
)

private val json = Json {
    ignoreUnknownKeys = true
}

fun sessionizeData(): SeData {
    val url = "https://sessionize.com/api/v2/ok1n6jgj/view/All"

    return Request.Builder()
        .get()
        .url(url)
        .build()
        .let {
            OkHttpClient()
                .newCall(it)
                .execute()
        }.let {
            json.decodeFromString(SeData.serializer(), it.body.string())
        }
}