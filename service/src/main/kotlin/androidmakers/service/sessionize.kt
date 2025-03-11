package androidmakers.service

import androidmakers.service.graphql.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNCHECKED_CAST")
private fun <T> Any?.cast() = this as T
private val Any?.asBoolean: Boolean get() = cast()
private val Any?.asList: List<Any?> get() = cast()
private val Any?.asMap: Map<String, Any?> get() = cast()
private val Any?.asString: String get() = cast()
private fun JsonElement.toAny(): Any? {
    return when (this) {
        JsonNull -> null
        is JsonArray -> this.map { it.toAny() }
        is JsonObject -> this.mapValues { it.value.toAny() }
        is JsonPrimitive -> {
            if (isString) {
                content
            } else if (content.contains(".")) {
                content.toDouble()
            } else if (content == "true") {
                true
            } else if (content == "false") {
                false
            } else {
                content.toInt()
            }
        }
    }
}

data class SessionizeData(
    val rooms: List<Room>,
    val sessions: List<Session>,
    val speakers: List<Speaker>,
    val conference: Conference,
    val venues: List<Venue>,
    val partnerGroups: List<PartnerGroup>
)


private val okHttpClient = OkHttpClient.Builder()
    .build()

suspend fun getUrl(url: String): String {
    val request = Request(url.toHttpUrl())

    val response = okHttpClient.newCall(request).executeAsync()

    return response.use {
        check(it.isSuccessful) {
            "Cannot get $url: ${it.body.string()}"
        }

        withContext(Dispatchers.IO) {
            response.body.string()
        }
    }
}

suspend fun getJsonUrl(url: String) = Json.parseToJsonElement(getUrl(url)).toAny()

object Sessionize {
    private val data = AtomicReference<SessionizeData>()

    internal fun data(): SessionizeData {
        val d = data.get()
        if (d == null) {
            runBlocking {
                importAndroidMakers2025()
            }
        }
        return data.get()
    }

    private suspend fun getData(): SessionizeData {
        return getData(
            url = "https://sessionize.com/api/v2/g4o6gyjr/view/All",
            gridSmartUrl = null,
            config = Conference(
                id = "androidmakers2025",
                name = "AndroidMakers by droidcon 2025",
                timezone = "Europe/Paris",
                themeColor = "0xffFB5C49",
                days = listOf(LocalDate(2025, Month.APRIL, 10), LocalDate(2025, Month.APRIL, 11))
            ),
            venues = listOf(
                Venue(
                    id = "conference",
                    name = "Beffroi de Montrouge",
                    address = "Av. de la République, 92120 Montrouge",
                    descriptions = mapOf(
                        "en" to "Cool venue",
                        "fr" to "Venue fraiche",
                    ),
                    latitude = 48.8188958,
                    longitude = 2.3193016,
                    imageUrl = "https://www.beffroidemontrouge.com/wp-content/uploads/2019/09/moebius-1.jpg",
                    floorPlanUrl = "https://storage.googleapis.com/androidmakers-static/floor_plan.png"
                ),
                Venue(
                    id = "afterparty",
                    name = "Café Oz Denfert",
                    address = "3 Pl. Denfert-Rochereau, 75014 Paris",
                    imageUrl = "https://storage.googleapis.com/androidmakers-static/cafeoz.png",
                    latitude = 48.8327857,
                    longitude = 2.3328001,
                    descriptions = emptyMap(),
                    floorPlanUrl = null
                )
            ),
            partnerGroups = listOf(
                PartnerGroup(
                    title = "gold",
                    partners = listOf(
                        Partner(
                            name = "RevenueCat",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/revenuecat.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/revenuecat_dark.png",
                            url = "https://www.revenuecat.com"
                        )
                    )
                ),
                PartnerGroup(
                    title = "silver",
                    partners = listOf(
                        Partner(
                            name = "happn",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/happn_light.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/happn_dark.png",
                            url = "https://www.happn.com/fr/"
                        ),
                        Partner(
                            name = "zimperium",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/zimperium.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/zimperium_dark.png",
                            url = "https://www.zimperium.com/"
                        )
                    )
                ),
                PartnerGroup(
                    title = "bronze",
                    partners = listOf(
                        Partner(
                            name = "appvestor",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/appvestor.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/appvestor_dark.png",
                            url = "https://appvestor.com/"
                        ),
                        Partner(
                            name = "kotzilla",
                            logoUrl = "https://kotzilla.io/kotzillaLogo.webp",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/koin_dark.png",
                            url = "https://www.kotzilla.io/"
                        ),
                        Partner(
                            name = "runway",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/runway.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/runway_dark.png",
                            url = "https://runway.team/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "startup",
                    partners = listOf(
                        Partner(
                            name = "screenshotbot",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/screenshotbot.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/screenshotbot_dark.png",
                            url = "https://screenshotbot.io/"
                        )
                    )
                ),
                PartnerGroup(
                    title = "community",
                    partners = listOf(
                        Partner(
                            name = "Groundbreaker",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/groundbreaker.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/groundbreaker_dark.png",
                            url = "https://groundbreaker.org/"
                        ),
                        Partner(
                            name = "leboncointech",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/leboncoin.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/leboncoin_dark.png",
                            url = "https://medium.com/leboncoin-tech-blog"
                        ),
                        Partner(
                            name = "stickermule",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/stickermule.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/stickermule_dark.png",
                            url = "https://www.stickermule.com/eu/custom-stickers"
                        ),
                        Partner(
                            name = "WomenTechMakers",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/womentechmakers.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/womentechmakers_dark.png",
                            url = "https://www.linkedin.com/company/womentechmakers/"
                        ),
                        Partner(
                            name = "Duchess France",
                            logoUrl = "https://www.duchess-france.fr/assets/bandeau.jpeg",
                            logoUrlDark = "https://www.duchess-france.fr/assets/bandeau.jpeg",
                            url = "https://www.duchess-france.fr"
                        )
                    )

                )
            )
        )
    }
    suspend fun importAndroidMakers2025() {
        try {
            this.data.set(getData())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * @param gridSmartUrl extra json to get the service sessions from. The service sessions are not always in the View/All url
     */
    private suspend fun getData(
        url: String,
        gridSmartUrl: String? = null,
        linksFor: suspend (String) -> List<Link> = { emptyList() },
        partnerGroups: List<PartnerGroup>,
        venues: List<Venue>,
        config: Conference
    ): SessionizeData {
        val data = getJsonUrl(url)

        val categories = data.asMap["categories"].asList.map { it.asMap }
            .flatMap {
                it["items"].asList
            }.map {
                it.asMap
            }.map {
                it["id"] to it["name"]
            }.toMap()


        var sessions = getSessions(data!!, categories, linksFor)
        if (gridSmartUrl != null) {
            sessions = sessions + getServiceSessions(gridSmartUrl, categories, linksFor)
        }

        var rooms = data.asMap["rooms"].asList.map { it.asMap }.map {
            Room(
                id = it.get("id").toString(),
                name = it.get("name").asString,
                capacity = 100
            )
        }
        if (rooms.isEmpty()) {
            rooms = sessions.flatMap { it.roomIds }.distinct().map { Room(id = it, name = it, capacity = 100) }
        }
        val speakers = data.asMap["speakers"].asList.map { it.asMap }.map {
            val speakerId = it.get("id").asString
            Speaker(
                id = speakerId,
                name = it.get("fullName").asString,
                photoUrl = it.get("profilePicture")?.asString,
                photoUrlThumbnail = it.get("profilePicture")?.asString,
                bio = it.get("bio")?.asString,
                tagline = it.get("tagLine")?.asString,
                city = null,
                company = null,
                companyLogoUrl = null,
                sessionIds = sessions.filter { it.speakerIds.contains(speakerId) }.map { it.id },
                socials = it.get("links").asList.map { it.asMap }.map {
                    Social(
                        name = it.get("linkType").asString,
                        link = it.get("url").asString,
                        icon = null
                    )
                }
            )
        }

        return SessionizeData(
            rooms = rooms,
            sessions = sessions,
            speakers = speakers,
            conference = config,
            venues = venues,
            partnerGroups = partnerGroups,
        )
    }

    private suspend fun getServiceSessions(
        gridSmart: String,
        categories: Map<Any?, Any?>,
        linksFor: suspend (String) -> List<Link>
    ): List<Session> {
        val data = getJsonUrl(gridSmart)

        return data.asList.flatMap { it.asMap["rooms"].asList }
            .flatMap { it.asMap["sessions"].asList }
            .map {
                it.asMap
            }
            .mapNotNull {
                if ((it.get("isServiceSession") as? Boolean) != true) {
                    // Filter service sessions
                    return@mapNotNull null
                }
                if (it.get("startsAt") == null || it.get("endsAt") == null) {
                    /**
                     * Guard against sessions that are not scheduled.
                     */
                    return@mapNotNull null
                }
                val tags = it.get("categoryItems")?.asList.orEmpty().mapNotNull { categoryId ->
                    categories.get(categoryId)?.asString
                }
                val id = it.get("id").asString
                val type = when {
                    /**
                     * Those are the welcome & closing sessions.
                     */
                    setOf("2d800312-ba14-4230-86f2-7307d4d250e3", "b5b772ca-cf1f-4405-8729-e3486b0f2ff3", "525651b1-fa59-4228-bdfe-e9655b342544").contains(id) -> "plenary"
                    it.get("isServiceSession").cast<Boolean>() -> "service"
                    else -> "talk"
                }
                Session(
                    id = id,
                    type = type,
                    title = it.get("title").asString,
                    description = it.get("description")?.asString,
                    language = tags.toLanguage(),
                    startsAt = it.get("startsAt").asString.let { LocalDateTime.parse(it) },
                    endsAt = it.get("endsAt").asString.let { LocalDateTime.parse(it) },
                    complexity = null,
                    feedbackId = null,
                    tags = tags,
                    roomIds = listOf(it.get("roomId").toString()).toSet(),
                    speakerIds = it.get("speakers")?.asList.orEmpty().map { it.asMap["id"].asString }.toSet(),
                    shortDescription = null,
                    links = linksFor(it.get("id").asString),
                )
            }

    }

    private suspend fun getSessions(
        data: Any,
        categories: Map<Any?, Any?>,
        linksFor: suspend (String) -> List<Link>
    ): List<Session> {
        return data.asMap["sessions"].asList.map {
            it.asMap
        }.mapNotNull {
            if (it.get("startsAt") == null || it.get("endsAt") == null) {
                /**
                 * Guard against sessions that are not scheduled.
                 */
                return@mapNotNull null
            }
            val tags = it.get("categoryItems").asList.mapNotNull { categoryId ->
                categories.get(categoryId)?.asString
            }
            Session(
                id = it.get("id").asString,
                type = if (it.get("isServiceSession").cast()) "service" else "talk",
                title = it.get("title").asString,
                description = it.get("description")?.asString,
                language = tags.toLanguage(),
                startsAt = it.get("startsAt").asString.let { LocalDateTime.parse(it) },
                endsAt = it.get("endsAt").asString.let { LocalDateTime.parse(it) },
                complexity = null,
                feedbackId = null,
                tags = tags,
                roomIds = listOf(it.get("roomId").toString()).toSet(),
                speakerIds = it.get("speakers").asList.map { it.asString }.toSet(),
                shortDescription = null,
                links = linksFor(it.get("id").asString),
            )
        }
    }
}

private fun List<String>.toLanguage(): String {
    return if (contains("French")) "French" else "English"
}
