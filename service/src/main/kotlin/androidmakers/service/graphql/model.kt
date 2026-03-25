@file:OptIn(ApolloExperimental::class)

package androidmakers.service.graphql

import androidmakers.service.Sessionize
import androidmakers.service.context.*
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.execution.StringCoercing
import com.apollographql.execution.annotation.GraphQLDefault
import com.apollographql.execution.annotation.GraphQLMutation
import com.apollographql.execution.annotation.GraphQLQuery
import com.apollographql.execution.annotation.GraphQLScalar
import com.google.cloud.datastore.BooleanValue
import com.google.cloud.datastore.Cursor
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Clock

const val KIND_BOOKMARKS = "Bookmarks"
const val KIND_FEED_ITEMS = "FeedItems"
const val KIND_FEEDBACK = "Feedback"

/**
 * A Markdown string as described by https://spec.commonmark.org
 */
@GraphQLScalar(StringCoercing::class)
typealias Markdown = String

@GraphQLScalar(StringCoercing::class)
typealias ID = String

class FeedItemInput(
    val title: Optional<String?>,
    val body: Optional<Markdown?>,
)

internal val adminUids = setOf("AY7jNYS4EpNhRHBxW89LjomGCtl1")

sealed interface FeedItemResult

enum class FeedItemType {
    INFO,
    ALERT,
    ANNOUNCEMENT
}

data class FeedItemFailure(val message: String = "Something wrong happened") : FeedItemResult
data class FeedItemSuccess(
    val feedItem: FeedItem
) : FeedItemResult

class FeedItem(
    val id: ID,
    val type: FeedItemType,
    val createdAt: GraphQLInstant,
    val title: String,
    val body: Markdown
)

enum class Rating {
    Disappointed,
    Neutral,
    Happy
}

class FeedbackInput(
    val id: ID,
    val rating: Rating,
    val comment: Markdown
)

sealed interface FeedbackResult

data class FeedbackFailure(val message: String = "Something wrong happened") : FeedbackResult
data class FeedbackSuccess(
    val feedback: Feedback
) : FeedbackResult

class Feedback(
    val id: ID,
    val rating: Rating,
    val comment: Markdown
)

@GraphQLMutation
class RootMutation {
    fun upsertFeedback(executionContext: ExecutionContext, feedback: FeedbackInput): FeedbackResult {
        val datastore = executionContext.datastore()
        val key = datastore.newKeyFactory().setKind(KIND_FEEDBACK).newKey(feedback.id)
        val entity = datastore.get(key)
        val builder = if (entity == null) {
            val key = datastore.newKeyFactory().setKind(KIND_FEEDBACK).newKey(feedback.id)
            Entity.newBuilder(key)
        } else {
            Entity.newBuilder(entity)
        }
        builder.apply {
            set("rating", feedback.rating.name)
            set("comment", feedback.comment)
        }

        val newEntity = datastore.put(builder.build())

        return FeedbackSuccess(
            Feedback(
                id = feedback.id,
                rating = Rating.valueOf(newEntity.getString("rating")),
                comment = newEntity.getString("comment"),
            )
        )
    }

    fun updateFeedItem(executionContext: ExecutionContext, id: ID, feedItem: FeedItemInput): FeedItemResult {
        val authenticationContext = executionContext.get(AuthenticationContext)!!
        if (authenticationContext.uid !in adminUids) {
            throw Error("Only admins can add feed items")
        }

        val datastore = executionContext.datastore()
        val key = datastore.newKeyFactory().setKind(KIND_FEED_ITEMS).newKey(id.toLong())
        val entity = datastore.get(key)
        if (entity == null) {
            return FeedItemFailure("No item found for id $id")
        }
        val updatedEntity = Entity.newBuilder(entity)
            .apply {
                if (feedItem.title.getOrNull() != null) {
                    set("title", feedItem.title.getOrThrow())
                }
                if (feedItem.body.getOrNull() != null) {
                    set("body", feedItem.body.getOrThrow())
                }
            }
            .build()

        val newEntity = datastore.put(updatedEntity)

        return FeedItemSuccess(
            FeedItem(
                id = id,
                title = newEntity.getString("title"),
                body = newEntity.getString("body"),
                createdAt = Instant.parse(newEntity.getString("createdAt")),
                type = newEntity.getString("type").toFeedItemType() ?: FeedItemType.INFO
            )
        )
    }

    fun addFeedItem(executionContext: ExecutionContext, feedItem: FeedItemInput): FeedItemResult {
        val authenticationContext = executionContext.get(AuthenticationContext)!!
        if (authenticationContext.uid !in adminUids) {
            throw Error("Only admins can add feed items")
        }

        val datastore = executionContext.datastore()

        check(feedItem.title.getOrNull() != null) {
            "title is required"
        }
        check(feedItem.body.getOrNull() != null) {
            "markdown is required"
        }
        val key = datastore.newKeyFactory().setKind(KIND_FEED_ITEMS).newKey()
        val now = Clock.System.now()

        val entity = Entity.newBuilder(key)!!
            .set("title", feedItem.title.getOrThrow())
            .set("body", feedItem.body.getOrThrow())
            .set("createdAt", now.toString())
            .build()

        val result = datastore.runInTransaction {
            it.put(entity)
        }

        return FeedItemSuccess(
            FeedItem(
                id = result.key.id.toString(),
                title = result.getString("title"),
                body = result.getString("body"),
                createdAt = result.getString("createdAt").toInstant(),
                type = result.getString("type").toFeedItemType() ?: FeedItemType.INFO
            )
        )
    }

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

class FeatureFlags(
    val feed: Boolean,
    val venue: Boolean,
)

@GraphQLQuery
class RootQuery {
    fun featureFlags(): FeatureFlags = FeatureFlags(true, true)

    /**
     * @return null if the Feedback isn't found.
     */
    fun feedback(executionContext: ExecutionContext, id: ID): Feedback? {
        val datastore = executionContext.datastore()
        val key = datastore.newKeyFactory().setKind(KIND_FEEDBACK).newKey(id)
        val entity = datastore.get(key)
        if (entity == null) {
            return null
        }
        return Feedback(
            id = id,
            rating = Rating.valueOf(entity.getString("rating")),
            comment = entity.getString("comment"),
        )

    }

    fun rooms(): List<Room> {
        return Sessionize.data().rooms
    }

    fun sessions(
        @GraphQLDefault("10") first: Int,
        @GraphQLDefault("null") after: String?,
        @GraphQLDefault("{field: STARTS_AT, direction: ASCENDING}") orderBy: SessionOrderBy
    ): SessionConnection {
        var sessions = Sessionize.data().sessions

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

    private fun <T : Node, C> List<T>.splice(first: Int, after: String?, block: (List<T>, PageInfo) -> C): C {
        var i = 0
        if (after != null) {
            i = indexOfFirst { it.id == after }
            if (i < 0) {
                error("No item and cursor '$after'")
            } else {
                i += 1
            }
        }
        val count = minOf(size - i, first)
        return block(
            this.subList(i, i + count),
            PageInfo(get(i + count - 1).id, i + count < size)
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
        return Sessionize.data().venues.first { it.id == id }
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

    fun feedItemsConnection(
        executionContext: ExecutionContext,
        @GraphQLDefault("10") first: Int,
        @GraphQLDefault("null") after: String?
    ): FeedItemsConnection {
        val datastore = executionContext.datastore()

        val query = Query.newEntityQueryBuilder()
            .setKind(KIND_FEED_ITEMS)
            .addOrderBy(StructuredQuery.OrderBy.desc("createdAt"))
            .setLimit(first)
            .apply {
                if (after != null) {
                    setStartCursor(Cursor.fromUrlSafe(after))
                }
            }
            .build()

        val results = datastore.run(query)

        val allItems = mutableListOf<FeedItem>()
        results.forEach { entity ->
            allItems.add(
                FeedItem(
                    id = entity.key.id.toString(),
                    title = entity.getString("title"),
                    body = entity.getString("body"),
                    createdAt = entity.getString("createdAt").toInstant(),
                    type = entity.getString("type")?.toFeedItemType() ?: FeedItemType.INFO
                )
            )
        }

        return FeedItemsConnection(
            nodes = allItems,
            pageInfo = PageInfo(allItems.lastOrNull()?.id, results.hasNext())
        )
    }

    /**
     * The current logged in user or null if the user is not logged in
     */
    fun user(executionContext: ExecutionContext): User? {
        val authenticationContext = executionContext[AuthenticationContext]!!
        if (authenticationContext.uid == null) {
            return null
        }
        return User(id = authenticationContext.uid, isAdmin = authenticationContext.uid in adminUids)
    }
}

class User(
    val id: String,
    val isAdmin: Boolean
)

class FeedItemsConnection(
    val nodes: List<FeedItem>,
    val pageInfo: PageInfo
)

class BookmarkConnection(
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
    val hasNextPage: Boolean = true
)

enum class LinkType {
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
) : Node {
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
) : Node {
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

private fun String.toFeedItemType(): FeedItemType? {
    return try {
        FeedItemType.valueOf(this)
    } catch (_: Exception) {
        println("Unknown feed item type: $this")
        null
    }
}
