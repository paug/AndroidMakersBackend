package androidmakers.service.graphql

import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.execution.Coercing
import com.apollographql.apollo.execution.ExternalValue
import com.apollographql.execution.annotation.GraphQLScalar
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * A type representing a formatted kotlinx.datetime.Instant
 */
@GraphQLScalar(GraphQLInstantCoercing::class)
typealias GraphQLInstant = Instant

object GraphQLInstantCoercing : Coercing<Instant> {
    override fun deserialize(value: ExternalValue): Instant {
        return Instant.parse(value as String)
    }

    override fun parseLiteral(gqlValue: GQLValue): Instant {
        return Instant.parse((gqlValue as GQLStringValue).value)
    }

    override fun serialize(internalValue: Instant): ExternalValue {
        return internalValue.toString()
    }
}

@GraphQLScalar(GraphQLLocalDateCoercing::class)
typealias GraphQLLocalDate = LocalDate

object GraphQLLocalDateCoercing : Coercing<LocalDate> {
    override fun deserialize(value: ExternalValue): LocalDate {
        return LocalDate.parse(value as String)
    }

    override fun parseLiteral(gqlValue: GQLValue): LocalDate {
        return LocalDate.parse((gqlValue as GQLStringValue).value)
    }

    override fun serialize(internalValue: LocalDate): ExternalValue {
        return internalValue.toString()
    }
}

@GraphQLScalar(GraphQLLocalDateTimeCoercing::class)
typealias GraphQLLocalDateTime = LocalDateTime

object GraphQLLocalDateTimeCoercing : Coercing<LocalDateTime> {
    override fun deserialize(value: ExternalValue): LocalDateTime {
        return LocalDateTime.parse(value as String)
    }

    override fun parseLiteral(gqlValue: GQLValue): LocalDateTime {
        return LocalDateTime.parse((gqlValue as GQLStringValue).value)
    }

    override fun serialize(internalValue: LocalDateTime): ExternalValue {
        return internalValue.toString()
    }
}
