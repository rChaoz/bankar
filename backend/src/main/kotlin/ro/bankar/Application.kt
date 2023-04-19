package ro.bankar

import freemarker.cache.ClassTemplateLoader
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.autohead.*
import org.jetbrains.exposed.sql.Database
import ro.bankar.plugins.*

fun main(args: Array<String>) {
    if (args.size == 1) {
        if (args[0] == "init") Database.init()
        else if (args[0] == "reset") Database.reset()
    }
    io.ktor.server.netty.EngineMain.main(args)
}

var DEV_MODE = false

@Suppress("unused")
fun Application.module() {
    DEV_MODE = environment.developmentMode
    configureSerialization()
    configureSessions()
    configureAuthentication()
    Database.connect()
    configureSockets()
    install(AutoHeadResponse)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    configureRouting()
}
