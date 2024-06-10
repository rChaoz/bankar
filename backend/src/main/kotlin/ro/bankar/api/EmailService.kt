package ro.bankar.api

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import java.util.*

// TODO
object EmailService {
    private val freemakerConfig = Configuration(Version(2, 3, 20)).apply {
        setClassForTemplateLoading(object {}.javaClass, "/payment")
        defaultEncoding = "UTF-8"
        locale = Locale.ENGLISH
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
    }

    // TODO
}