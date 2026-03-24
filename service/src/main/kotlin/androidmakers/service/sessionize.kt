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
                importData()
            }
        }
        return data.get()
    }

    private suspend fun getData(): SessionizeData {
        return getData(
            url = "https://sessionize.com/api/v2/kqy4c3ye/view/All",
            gridSmartUrl = null,
            config = Conference(
                id = "androidmakers2026",
                name = "AndroidMakers by droidcon 2026",
                timezone = "Europe/Paris",
                themeColor = "0xffFB5C49",
                days = listOf(LocalDate(2026, Month.APRIL, 9), LocalDate(2026, Month.APRIL, 10))
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
                            url = "https://www.revenuecat.com/for-developers/"
                        ),
                        Partner(
                            name = "Stripe",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/stripe.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/stripe_dark.png",
                            url = "https://stripe.com/"
                        ),
                        Partner(
                            name = "Trade Republic",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/trade_republic.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/trade_republic_dark.png",
                            url = "https://traderepublic.com/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "silver",
                    partners = listOf(
                        Partner(
                            name = "Bitrise",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/bitrise.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/bitrise_dark.png",
                            url = "https://bitrise.io/"
                        ),
                        Partner(
                            name = "FastSpring",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/fast_spring.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/fast_spring_dark.png",
                            url = "https://fastspring.com/"
                        ),
                        Partner(
                            name = "Runway",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/runway.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/runway_dark.png",
                            url = "https://runway.team/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "bronze",
                    partners = listOf(
                        Partner(
                            name = "Kotzilla",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/kotzilla.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/kotzilla_dark.png",
                            url = "https://kotzilla.io/"
                        ),
                        Partner(
                            name = "Maestro",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/maestro.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/maestro_dark.png",
                            url = "https://maestro.dev/"
                        ),
                        Partner(
                            name = "Sentry",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/sentry.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/sentry_dark.png",
                            url = "https://sentry.io/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "startup",
                    partners = listOf(
                        Partner(
                            name = "emulator.wtf",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/emulator_wtf.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/emulator_wtf_dark.png",
                            url = "https://emulator.wtf/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "content sponsor",
                    partners = listOf(
                        Partner(
                            name = "Google",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/google.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/google_dark.png",
                            url = "https://about.google.com/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "lanyard sponsor",
                    partners = listOf(
                        Partner(
                            name = "Amo",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/amo.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/amo_dark.png",
                            url = "https://amo.co/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "t-shirt sponsor",
                    partners = listOf(
                        Partner(
                            name = "Coyote",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/coyote_light.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/coyote_dark.png",
                            url = "https://www.moncoyote.com/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "goodie bag sponsor",
                    partners = listOf(
                        Partner(
                            name = "RevenueCat",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/revenuecat.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/revenuecat_dark.png",
                            url = "https://www.revenuecat.com/for-developers/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "community",
                    partners = listOf(
                        Partner(
                            name = "Duchess France",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/duchess.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/duchess_dark.png",
                            url = "https://www.duchess-france.fr/"
                        ),
                        Partner(
                            name = "GDG Bordeaux",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/gdg_bordeaux.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/gdg_bordeaux_dark.png",
                            url = "https://gdg.community.dev/gdg-bordeaux/"
                        ),
                        Partner(
                            name = "GDG Le Mans",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/gdg_le_mans.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/gdg_le_mans_dark.png",
                            url = "https://gdg.community.dev/gdg-le-mans/"
                        ),
                        Partner(
                            name = "GDG Paris",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/gdg_paris.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/gdg_paris_dark.png",
                            url = "https://gdg.community.dev/gdg-paris/"
                        ),
                        Partner(
                            name = "GDG Sophia Antipolis",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/gdg_sophia.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/gdg_sophia_dark.png",
                            url = "https://gdg.community.dev/gdg-sophia-antipolis/"
                        ),
                        Partner(
                            name = "Ladies of Code Paris",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/ladies.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/ladies_dark.png",
                            url = "https://ladiesofcodeparis.netlify.app/"
                        ),
                        Partner(
                            name = "leboncoin tech",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/leboncoin.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/leboncoin_dark.png",
                            url = "https://leboncoincorporate.com/equipes-tech/"
                        ),
                        Partner(
                            name = "PAUG",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/paug.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/paug_dark.png",
                            url = "https://paug.fr/"
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
                            url = "https://www.technovation.org/women-techmakers/"
                        ),
                    )
                ),
                PartnerGroup(
                    title = "speaker sponsors",
                    partners = listOf(
                        Partner(
                            name = "Almaviva",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/almaviva.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/almaviva_dark.png",
                            url = "https://www.almaviva.it/"
                        ),
                        Partner(
                            name = "GetYourGuide",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/get_your_guide.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/get_your_guide_dark.png",
                            url = "https://www.getyourguide.careers/"
                        ),
                        Partner(
                            name = "Infomaniak",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/infomaniak.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/infomaniak_dark.png",
                            url = "https://www.infomaniak.com/"
                        ),
                        Partner(
                            name = "JetBrains",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/jetbrains_light.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/jetbrains_dark.png",
                            url = "https://www.jetbrains.com/"
                        ),
                        Partner(
                            name = "Novibet",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/novibet.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/novibet_dark.png",
                            url = "https://www.novibet.com/"
                        ),
                        Partner(
                            name = "Pictarine",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/pictarine.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/pictarine_dark.png",
                            url = "https://pictarine.com/"
                        ),
                        Partner(
                            name = "Skroutz",
                            logoUrl = "https://storage.googleapis.com/androidmakers-static/partners/skroutz.png",
                            logoUrlDark = "https://storage.googleapis.com/androidmakers-static/partners/skroutz_dark.png",
                            url = "https://corporate.skroutz.gr/en/"
                        ),
                    )
                )
            )
        )
    }
    suspend fun importData() {
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
