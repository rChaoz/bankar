package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ro.bankar.api.SmsService

fun Routing.configureAPIs() {
    // Configure SMS service
    SmsService.configure(System.getenv("SENDSMS_USER"), System.getenv("SENDSMS_KEY"), System.getenv("REPORT_URL"))
    get("sms_report") {
        val messageID = call.request.queryParameters["message_id"]
        val status = call.request.queryParameters["status"]?.toIntOrNull()
        if (messageID == null || status == null || !SmsService.onSmsReport(messageID, status)) {
            call.respond(HttpStatusCode.BadRequest); return@get
        }
        call.respond(HttpStatusCode.OK)
    }
}