package androidmakers.service.graphql

import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.execution.Coercing
import com.apollographql.apollo.execution.ExternalValue
import com.apollographql.execution.annotation.GraphQLName
import com.apollographql.execution.annotation.GraphQLScalar
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * A type representing a formatted kotlinx.datetime.Instant
 */
@GraphQLScalar(GraphQLInstantCoercing::class)
@GraphQLName("Instant")
typealias GraphQLInstant = Instant

object GraphQLInstantCoercing : Coercing<Instant> {
    override fun deserialize(value: ExternalValue): Instant {
        return Instant.parse(value as String)
    }

    override fun parseLiteral(value: GQLValue): Instant {
        return Instant.parse((value as GQLStringValue).value)
    }

    override fun serialize(internalValue: Instant): ExternalValue {
        return internalValue.toString()
    }
}

@GraphQLScalar(GraphQLLocalDateCoercing::class)
@GraphQLName("LocalDate")
typealias GraphQLLocalDate = LocalDate

object GraphQLLocalDateCoercing : Coercing<LocalDate> {
    override fun deserialize(value: ExternalValue): LocalDate {
        return LocalDate.parse(value as String)
    }

    override fun parseLiteral(value: GQLValue): LocalDate {
        return LocalDate.parse((value as GQLStringValue).value)
    }

    override fun serialize(internalValue: LocalDate): ExternalValue {
        return internalValue.toString()
    }
}

@GraphQLScalar(GraphQLLocalDateTimeCoercing::class)
@GraphQLName("LocalDateTime")
typealias GraphQLLocalDateTime = LocalDateTime

object GraphQLLocalDateTimeCoercing : Coercing<LocalDateTime> {
    override fun deserialize(value: ExternalValue): LocalDateTime {
        return LocalDateTime.parse(value as String)
    }

    override fun parseLiteral(value: GQLValue): LocalDateTime {
        return LocalDateTime.parse((value as GQLStringValue).value)
    }

    override fun serialize(internalValue: LocalDateTime): ExternalValue {
        return internalValue.toString()
    }
}

