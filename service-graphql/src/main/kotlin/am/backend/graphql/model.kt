package am.backend.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.FullEntity
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.ListValue
import com.google.cloud.datastore.StringValue
import am.backend.DefaultApplication.Companion.KEY_UID
import graphql.introspection.Introspection
import graphql.schema.DataFetchingEnvironment
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.springframework.stereotype.Component

internal fun googleCredentials(name: String): GoogleCredentials {
    return GoogleCredentials::class.java.classLoader.getResourceAsStream(name)?.use {
        GoogleCredentials.fromStream(it)
    } ?: error("no credentials found for $name")
}

internal fun initDatastore(): Datastore {
    return DatastoreOptions.newBuilder()
        .setCredentials(googleCredentials("firebase_service_account_key.json")).build().service
}

private val datastore = initDatastore()

@GraphQLDirective(
    name = "requiresOptIn",
    description = "This field can be changed without warning",
    locations = [Introspection.DirectiveLocation.FIELD_DEFINITION]
)
annotation class RequiresOptIn(val feature: String)

val userKeyFactory = datastore.newKeyFactory().setKind("KIND_USER")

private fun keyForUser(uid: String): Key {
    return userKeyFactory.newKey(uid)
}

fun List<String>.toListValue() = ListValue(map { StringValue(it)})
fun ListValue.toList() = this.get().map { it.get() as String }

private fun FullEntity<*>.bookmarks(): Bookmarks {
    return try {
        Bookmarks(getList<StringValue>("bookmarks").map { it.get() })
    } catch (e: Exception) {
        Bookmarks(emptyList())
    }
}

@Component
class RootMutation : Mutation {
    fun addBookmark(dfe: DataFetchingEnvironment, sessionId: String): Bookmarks {
        val uid = dfe.uid()
        check(uid != null) {
            "This call requires authentication"
        }
        val entity = try {
            val existingEntity = datastore.get(keyForUser(uid))
            Entity.newBuilder(existingEntity)
                .set("bookmarks", existingEntity.getList<StringValue>("bookmarks") + StringValue(sessionId))
                .build()
        } catch (e: Exception) {
            Entity.newBuilder(keyForUser(uid))
                .set("bookmarks", listOf(StringValue(sessionId)))
                .build()
        }

        datastore.put(entity)

        return entity.bookmarks()
    }

    fun removeBookmark(dfe: DataFetchingEnvironment, sessionId: String): Bookmarks {
        val uid = dfe.uid()
        check(uid != null) {
            "This call requires authentication"
        }
        val existingEntity = datastore.get(keyForUser(uid))

        val entity = Entity.newBuilder(existingEntity)
            .set("bookmarks", existingEntity.getList<StringValue>("bookmarks").filter { it.get() != sessionId })
            .build()

        datastore.put(entity)
        return entity.bookmarks()
    }
}

@Component
class RootQuery : Query {
    fun rooms(dfe: DataFetchingEnvironment): List<Room> {
        return seData.rooms.mapNotNull { it.toRoom() }
    }

    fun sessions(
        dfe: DataFetchingEnvironment,
        first: Int? = 100,
        after: String? = null,
        filter: SessionFilter? = null,
        orderBy: SessionOrderBy? = SessionOrderBy(
            field = SessionField.STARTS_AT,
            direction = OrderByDirection.ASCENDING
        )
    ): SessionConnection {
        val nodes = seData.sessions.page(first ?: 100, after) { it.id }.map { it.toSession() }
        return SessionConnection(
            nodes = nodes,
            pageInfo = PageInfo(
                endCursor = nodes.lastOrNull()?.id
            )
        )
    }

    fun <T> List<T>.page(first: Int, after: String?, block: (T) -> String): List<T> {
        val startIndex = if (after != null) {
            val i = after.let { map { block(it) }.indexOf(it) }

            if (i < 0) {
                error("Invalid cursor: $after")
            }
            i + 1
        } else {
            0
        }
        val endIndex = minOf(size, startIndex + (first ?: 100))

        return subList(startIndex, endIndex)
    }

    @Deprecated("Use speakersPage instead")
    fun speakers(dfe: DataFetchingEnvironment): List<Speaker> {
        return seData.speakers.map { it.toSpeaker() }
    }

    fun speakersPage(
        dfe: DataFetchingEnvironment,
        first: Int? = 10,
        after: String? = null,
    ): SpeakerConnection {
        val nodes = seData.speakers.page(first ?: 100, after) { it.id }.map { it.toSpeaker() }

        return SpeakerConnection(
            nodes = nodes,
            pageInfo = PageInfo(
                endCursor = nodes.lastOrNull()?.id
            )
        )
    }

    fun speaker(dfe: DataFetchingEnvironment, id: String): Speaker {
        return seData.speakers.first { it.id == id }.toSpeaker()
    }

    fun venue(dfe: DataFetchingEnvironment, id: String): Venue {
        return seData.venues.first { it.id == id }.toVenue()
    }

    fun venues(dfe: DataFetchingEnvironment): List<Venue> {
        return seData.venues.map { it.toVenue() }
    }

    fun partnerGroups(dfe: DataFetchingEnvironment): List<PartnerGroup> {
        return seData.partners.map { it.toPartnerGroup() }
    }

    fun session(dfe: DataFetchingEnvironment, id: String): Session {
        return seData.sessions.first { it.id == id }.toSession()
    }

    private val conference = Conference(
        id = "androidmakers2023",
        name = "Android Makers by droidcon",
        timezone = "Europe/Paris",
        days = listOf(LocalDate(2023, 4, 27), LocalDate(2023, 4, 28))
    )

    fun config(dfe: DataFetchingEnvironment): Conference {
        return conference
    }

    fun bookmarks(dfe: DataFetchingEnvironment): Bookmarks? {
        val uid = dfe.uid()
        check(uid != null) {
            "This call requires authentication"
        }

        return try {
            val existingEntity  = datastore.get(keyForUser(uid))
            existingEntity.bookmarks()
        } catch (e: Exception) {
            Bookmarks(emptyList())
        }
    }

    fun conferences(orderBy: ConferenceOrderBy? = null): List<Conference> {
        return listOf(conference)
    }
}

class Bookmarks(val sessionIds: List<String>) {
    val id = "Bookmarks"
}


class LocalDateTimeFilter(
    val before: LocalDateTime? = null,
    val after: LocalDateTime? = null,
)

class SessionFilter(
    val startsAt: LocalDateTimeFilter? = null,
    val endsAt: LocalDateTimeFilter? = null,
)

class SessionOrderBy(
    val field: SessionField,
    val direction: OrderByDirection
)

enum class SessionField(val value: String) {
    STARTS_AT("start"),
}

class ConferenceOrderBy(
    val field: ConferenceField,
    val direction: OrderByDirection
)

enum class OrderByDirection {
    ASCENDING,
    DESCENDING
}

enum class ConferenceField(val value: String) {
    DAYS("days"),
}

fun DataFetchingEnvironment.uid(): String? {
    return graphQlContext.get(KEY_UID)
}

data class Room(
    val id: String,
    val name: String,
    val capacity: Int?,
)

data class SessionConnection(
    val nodes: List<Session>,
    val pageInfo: PageInfo,
)


data class PageInfo(
    val endCursor: String?,
)

/**
 */
data class Session(
    val id: String,
    val title: String,
    val description: String?,
    @GraphQLDescription(
        """A shorter version of description for use when real estate is scarce like watches for an example.
This field might have the same value as description if a shortDescription is not available"""
    )
    val shortDescription: String?,
    @GraphQLDescription("""An [IETF language code](https://en.wikipedia.org/wiki/IETF_language_tag) like en-US""")
    val language: String?,
    private val speakerIds: Set<String>,
    val tags: List<String>,
    @Deprecated("use startsAt instead")
    val startInstant: Instant,
    @Deprecated("use endsAt instead")
    val endInstant: Instant,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    private val roomIds: Set<String>,
    val complexity: String?,
    val feedbackId: String?,
    @GraphQLDescription("""One of "break", "lunch", "party", "keynote", "talk" or any other conference-specific format""")
    val type: String,
    val isServiceSession: Boolean
) {
    fun speakers(dfe: DataFetchingEnvironment): List<Speaker> {
        return seData.speakers.filter { speakerIds.contains(it.id) }.map { it.toSpeaker() }
    }

    fun room(dfe: DataFetchingEnvironment): Room? {
        return seData.rooms.firstOrNull { roomIds.contains(it.id.toString()) }?.toRoom()
    }

    fun rooms(dfe: DataFetchingEnvironment): List<Room> {
        return listOf(room(dfe)!!)
    }
}

data class SpeakerConnection(
    val nodes: List<Speaker>,
    val pageInfo: PageInfo,
)

data class Speaker(
    val id: String,
    val name: String,
    val bio: String?,
    val tagline: String?,
    val company: String?,
    val companyLogoUrl: String?,
    val city: String?,
    val socials: List<Social>,
    val photoUrl: String?,
    private val sessionIds: List<String>,
) {
    fun sessions(
        dfe: DataFetchingEnvironment,
    ): List<Session> {
        return seData.sessions.filter { sessionIds.contains(it.id) }.map { it.toSession() }
    }
}


data class Social(
    val icon: String?,
    @Deprecated("use url instead", ReplaceWith("url"))
    val link: String,
    val name: String,
) {
    @Suppress("DEPRECATION")
    val url: String
        get() = link
}

data class PartnerGroup(
    val title: String,
    val partners: List<Partner>,
)

data class Partner(
    val name: String,
    val logoUrl: String,
    val url: String,
)

/**
 * @property floorPlanUrl the url to an image containing the floor plan
 */
data class Venue(
    val id: String,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String? = null,
    val imageUrl: String?,
    val floorPlanUrl: String?,
    private val descriptions: Map<String, String>
) {
    @Deprecated(
        "use latitude and " +
            "longitude instead"
    )
    val coordinates: String?
        get() {
            return if (latitude != null && longitude != null) {
                "$latitude,$longitude"
            } else {
                null
            }
        }

    @Deprecated("use description(language: \"fr\") instead")
    val descriptionFr: String
        get() {
            return descriptions.get("fr") ?: descriptions.get("en") ?: ""
        }

    fun description(language: String? = "en"): String {
        return descriptions.get(language) ?: descriptions.get("en") ?: ""
    }
}

data class Conference(
    val id: String,
    val name: String,
    val timezone: String,
    val days: List<LocalDate>
)
