package sync

import data.JsonData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import sessionize.SeData
import sessionize.sessionizeData
import java.io.File

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val sessionizeData = sessionizeData()

    val file = File("../service-graphql/src/main/resources/data.json")
    val data = file.inputStream().use {
        json.decodeFromStream<JsonData>(it)
    }

    val newData = data.merge(sessionizeData)

    file.outputStream().use {
        json.encodeToStream(newData, it)
    }
}

private fun JsonData.merge(seData: SeData): JsonData {
    return copy(
        sessions = seData.sessions,
        speakers = seData.speakers,
        rooms = seData.rooms,
        categories = seData.categories
    )
}
