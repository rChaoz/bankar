package ro.bankar.api

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import java.util.*


object Email {
    private val freemakerConfig = Configuration(Version(2, 3, 20)).apply {
        setClassForTemplateLoading(object {}.javaClass, "/templates")
        defaultEncoding = "UTF-8"
        locale = Locale.ENGLISH
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
    }

    // TODO Add emails to send here
}