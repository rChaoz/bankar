package ro.bankar.database

import io.ktor.server.application.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ro.bankar.banking.SCreditData
import ro.bankar.model.SCountries

lateinit var COUNTRY_DATA: SCountries
lateinit var CREDIT_DATA: List<SCreditData>

@OptIn(ExperimentalSerializationApi::class)
fun loadStaticData() {
    val c = object {}.javaClass
    COUNTRY_DATA = Json.decodeFromStream(c.getResourceAsStream("/data/countries.json")!!)
    CREDIT_DATA = Json.decodeFromStream(c.getResourceAsStream("/data/credit.json")!!)
}