package ro.bankar

import freemarker.cache.ClassTemplateLoader
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.hsts.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.jetbrains.exposed.sql.Database
import ro.bankar.api.SmsService
import ro.bankar.model.SCountries
import ro.bankar.plugins.*

fun main(args: Array<String>) {
    if (args.size == 1 && args[0] == "reset") Database.reset()
    io.ktor.server.netty.EngineMain.main(args)
}

var DEV_MODE = false
var SKIP_DELIVERY_CHECK = false
lateinit var COUNTRY_DATA: SCountries

@OptIn(ExperimentalSerializationApi::class)
@Suppress("unused")
fun Application.module() {
    DEV_MODE = environment.developmentMode
    SKIP_DELIVERY_CHECK = environment.config.propertyOrNull("ktor.sms.skipDeliveryCheck")?.getString().toBoolean()
    COUNTRY_DATA = Json.decodeFromStream(object {}.javaClass.getResourceAsStream("/data/countries.json")!!)
    SmsService.configure(
        System.getenv("SENDSMS_USER"),
        System.getenv("SENDSMS_KEY"),
        environment.config.propertyOrNull("ktor.sms.reportURL")?.getString(),
    )
    configureSerialization()
    configureSessions()
    configureAuthentication()
    Database.init()
    configureSockets()
    install(AutoHeadResponse)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    if (!DEV_MODE) install(HSTS)
    configureRouting()
}
