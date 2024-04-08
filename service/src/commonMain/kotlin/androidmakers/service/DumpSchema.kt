@file:Suppress("OPT_IN_USAGE")

package androidmakers.service

import java.io.File
import com.apollographql.apollo3.ast.toUtf8

fun main(args: Array<String>) {
    check(args.isNotEmpty()) {
        """
            the path to the schema is required
        """.trimIndent()
    }

    File(args[0]).apply {
        parentFile.mkdirs()
        writeText(androidmakers.graphql.androidmakersSchemaDocument.toUtf8("  "))
    }
}