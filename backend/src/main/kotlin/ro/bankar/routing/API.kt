package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import ro.bankar.api.SmsService

fun Route.configureAPIs() {
    // Configure SMS service
    get("sms_report") {
        // Notify waiting request (that sent the SMS)
        val messageID = call.request.queryParameters["message_id"]
        val status = call.request.queryParameters["status"]?.toIntOrNull()
        if (messageID == null || status == null || !SmsService.onSmsReport(messageID, status)) {
            call.respond(HttpStatusCode.BadRequest); return@get
        }
        call.respond(HttpStatusCode.OK)
    }
}