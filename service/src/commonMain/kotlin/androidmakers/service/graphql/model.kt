@file:OptIn(ApolloExperimental::class)

package androidmakers.service.graphql

import androidmakers.service.Sessionize
import androidmakers.service.context.bookmarksKeyFactory
import androidmakers.service.context.datastore
import androidmakers.service.context.uid
import androidmakers.service.context.updateMaxAge
import com.apollographql.apollo3.annotations.*
import com.apollographql.apollo3.api.ExecutionContext
import com.google.cloud.datastore.BooleanValue
import com.google.cloud.datastore.Entity
import kotlinx.datetime.LocalDateTime

const val KIND_BOOKMARKS = "Bookmarks"

@GraphQLMutationRoot
class RootMutation {
    fun addBookmark(executionContext: ExecutionContext, sessionId: String): BookmarkConnection {
        val uid = executionContext.uid()
        check(uid != null) {
            "bookmarks require authentication"
        }

        if (Sessionize.data().sessions.none { it.id == sessionId }) {
            throw Error("Cannot add bookmark for inexisting session '$sessionId'")
        }

        val datastore = executionContext.datastore()

        val key = executionContext.bookmarksKeyFactory().newKey(uid)
        val entity = datastore.get(key)
        val entityBuilder = if (entity == null) {
            Entity.newBuilder(key)!!
        } else {
            Entity.newBuilder(entity)
        }

        entityBuilder.set(sessionId, BooleanValue.of(true))

        val newEntity = entityBuilder.build()
        datastore.runInTransaction {
            it.put(newEntity)
        }

        executionContext.updateMaxAge(0)
        return newEntity.names.toBookmarkConnection()
    }

    private fun Set<String>.toBookmarkConnection(): BookmarkConnection {
        return BookmarkConnection(Sessionize.data().sessions.filter { this.contains(it.id) })
    }

    fun removeBookmark(executionContext: ExecutionContext, sessionId: String): BookmarkConnection {
        val uid = executionContext.uid()
        check(uid != null) {
            "bookmarks require authentication"
        }

        val datastore = executionContext.datastore()

        val key = executionContext.bookmarksKeyFactory().newKey(uid)
        val entity = datastore.get(key)
        if (entity == null) {
            return BookmarkConnection(emptyList())
        }

        val newEntity = Entity.newBuilder(entity)
            .remove(sessionId)
            .build()

        datastore.runInTransaction {
            it.put(newEntity)
        }

        executionContext.updateMaxAge(0)
        return newEntity.names.toBookmarkConnection()
    }

    /**
     * Deletes the current user account, requires authentication
     */
    fun deleteAccount(executionContext: ExecutionContext): Boolean {
        val uid = executionContext.uid()
        check(uid != null) {
            "bookmarks require authentication"
        }

        val datastore = executionContext.datastore()

        val key = executionContext.bookmarksKeyFactory().newKey(uid)
        datastore.delete(key)

        return true
    }
}


@GraphQLQueryRoot
class RootQuery {
    fun rooms(): List<Room> {
        return Sessionize.data().rooms
    }

    fun sessions(
        @GraphQLDefault("10") first: Int,
        @GraphQLDefault("null") after: String?,
        @GraphQLDefault("{field: STARTS_AT, direction: ASCENDING}") orderBy: SessionOrderBy
    ): SessionConnection {
        var sessions =  Sessionize.data().sessions

        when (orderBy.direction) {
                OrderByDirection.ASCENDING -> {
                    when (orderBy.field) {
                        SessionField.STARTS_AT -> {
                            sessions = sessions.sortedBy {
                                it.startsAt
                            }
                        }
                    }
                }

            OrderByDirection.DESCENDING -> {
                when (orderBy.field) {
                    SessionField.STARTS_AT -> {
                        sessions = sessions.sortedByDescending {
                            it.startsAt
                        }
                    }
                }
            }
        }

        return sessions.splice(first, after) { nodes, pageInfo ->
            SessionConnection(nodes, pageInfo)
        }
    }

    private fun <T: Node, C> List<T>.splice(first: Int, after: String?, block: (List<T>, PageInfo) -> C): C {
        var i = 0
        if (after != null) {
            i = indexOfFirst { it.id == after }
            if (i < 0) {
                error("No item and cursor '$after'")
            } else {
                i += 1
            }
        }
        var count = minOf(size - i, first)
        return block(
            this.subList(i, i + count),
            PageInfo(get(i + count - 1).id)
        )
    }

    @Deprecated("Use speakersPage instead")
    fun speakers(): List<Speaker> {
        return Sessionize.data().speakers
    }

    fun speakersPage(
        @GraphQLDefault("10") first: Int,
        @GraphQLDefault("null") after: String?,
    ): SpeakerConnection {
        return Sessionize.data().speakers.splice(first, after) { nodes, pageInfo ->
            SpeakerConnection(nodes, pageInfo)
        }
    }

    fun speaker(id: String): Speaker {
        return Sessionize.data().speakers.first { it.id == id }
    }

    fun venue(id: String): Venue {
        return Sessionize.data().venues.first { it.id == id}
    }

    fun venues(): List<Venue> {
        return Sessionize.data().venues
    }

    fun partnerGroups(): List<PartnerGroup> {
        return Sessionize.data().partnerGroups
    }

    fun session(id: String): Session {
        return Sessionize.data().sessions.first { it.id == id }
    }

    fun config(): Conference {
        return Sessionize.data().conference
    }

    fun bookmarkConnection(executionContext: ExecutionContext): BookmarkConnection {
        val uid = executionContext.uid()
        check(uid != null) {
            "bookmarks require authentication"
        }

        val datastore = executionContext.datastore()

        val key = executionContext.bookmarksKeyFactory().newKey(uid)
        val entity = datastore.get(key)
        if (entity == null) {
            return BookmarkConnection(emptyList())
        } else {
            Entity.newBuilder(entity)
        }

        return BookmarkConnection(Sessionize.data().sessions.filter { entity.names.contains(it.id) })
    }

    fun conferences(@GraphQLDefault("null") orderBy: ConferenceOrderBy?): List<Conference> {
        return listOf(Sessionize.data().conference)
    }
}

class  BookmarkConnection(
    val nodes: List<Session>
)

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

enum class SessionField {
    STARTS_AT,
}

class ConferenceOrderBy(
    val field: ConferenceField,
    val direction: OrderByDirection
)

enum class OrderByDirection {
    ASCENDING,
    DESCENDING
}

enum class ConferenceField {
    DAYS,
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

enum class  LinkType {
    YouTube,
    Audio,
    AudioUncompressed,
    Other
}

data class Link(
    val type: LinkType,
    val url: String,
)

sealed interface Node {
    val id: String
}

/**
 */
data class Session(
    override val id: String,
    val title: String,
    /**
     * The description of the event. [description] may contain emojis and '\n' Chars but no markdown or HTML.
     *
     * May be null if no description is available.
     */
    val description: String?,
    /**
     * A shorter version of description for use when real estate is scarce like watches for an example.
     * This field might have the same value as description if a shortDescription is not available
     */
    val shortDescription: String?,
    /**
     * An [IETF language code](https://en.wikipedia.org/wiki/IETF_language_tag) like en-US
     */
    val language: String?,
    internal val speakerIds: Set<String>,
    val tags: List<String>,
    val startsAt: GraphQLLocalDateTime,
    val endsAt: GraphQLLocalDateTime,
    internal val roomIds: Set<String>,
    val complexity: String?,
    val feedbackId: String?,
    /**
     * One of "break", "lunch", "party", "keynote", "talk" or any other conference-specific format
     */
    val type: String,
    val links: List<Link>
): Node {
    fun speakers(): List<Speaker> {
        return Sessionize.data().speakers.filter { speakerIds.contains(it.id) }
    }

    fun room(): Room? {
        val roomId = roomIds.firstOrNull()
        if (roomId == null) {
            return null
        }
        return Sessionize.data().rooms.firstOrNull {
            it.id == roomId
        }
    }

    fun rooms(): List<Room> {
        return Sessionize.data().rooms.filter {
            roomIds.contains(it.id)
        }
    }
}

data class SpeakerConnection(
    val nodes: List<Speaker>,
    val pageInfo: PageInfo,
)

data class Speaker(
    override val id: String,
    val name: String,
    val bio: String?,
    val tagline: String?,
    val company: String?,
    val companyLogoUrl: String?,
    val city: String?,
    val socials: List<Social>,
    val photoUrl: String?,
    val photoUrlThumbnail: String?,
    private val sessionIds: List<String>,
): Node {
    fun sessions(): List<Session> {
        return Sessionize.data().sessions.filter {
            sessionIds.contains(it.id)
        }
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
    private val logoUrl: String,
    private val logoUrlDark: String?,
    val url: String,
) {
    /**
     * @param dark returns the logo for use on a dark background or fallbacks to the light mode if none exist
     */
    fun logoUrl(@GraphQLDefault("false") dark: Boolean?): String {
        return if (dark == true) {
            logoUrlDark ?: logoUrl
        } else {
            logoUrl
        }
    }
}

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
    @Deprecated("use latitude and longitude instead")
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
            return descriptions.get("\"fr\"") ?: descriptions.get("en") ?: ""
        }

    /**
     * The description of the venue. [description] may contain emojis and '\n' Chars but no markdown or HTML.
     *
     * May be null if no description is available.
     */
    fun description(@GraphQLDefault("\"en\"") language: String?): String {
        return descriptions.get(language) ?: descriptions.get("en") ?: ""
    }
}

data class Conference(
    val id: String,
    val name: String,
    val timezone: String,
    val days: List<GraphQLLocalDate>,
    val themeColor: String? = null
)
