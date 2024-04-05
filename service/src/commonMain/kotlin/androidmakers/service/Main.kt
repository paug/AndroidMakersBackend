package androidmakers.service

import androidmakers.openfeedback.openfeedbackModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*

fun main(args: Array<String>) {
    embeddedServer(Netty, port = System.getenv("POST")?.toIntOrNull() ?: 8080) {
        install(CORS) {
            anyHost()
        }
        openfeedbackModule("/api/openfeedback.json")
    }.start(wait = true)
}