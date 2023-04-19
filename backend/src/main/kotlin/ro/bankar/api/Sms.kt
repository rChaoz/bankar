package ro.bankar.api

object Sms {
    fun send(phone: String, message: String) {
        println("Sending SMS to $phone: $message")
    }
}