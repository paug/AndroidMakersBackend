package sync

import com.opencsv.CSVWriter
import sessionize.sessionizeData
import java.io.File

fun main() {
    val sessionizeData = sessionizeData()

    val writer = CSVWriter(File("../videos.csv").writer())

    sessionizeData.sessions.forEach {
        writer.writeLine(it.id)
    }
}

fun CSVWriter.writeLine(vararg values: String) {
    writeNext(values)
}