package ro.bankar.database

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ro.bankar.banking.SCountries
import ro.bankar.banking.SCreditData
import ro.bankar.banking.SExchangeData

lateinit var COUNTRY_DATA: SCountries
lateinit var CREDIT_DATA: List<SCreditData>
lateinit var EXCHANGE_DATA: SExchangeData

@OptIn(ExperimentalSerializationApi::class)
fun loadStaticData() {
    val c = object {}.javaClass
    COUNTRY_DATA = Json.decodeFromStream(c.getResourceAsStream("/data/countries.json")!!)
    CREDIT_DATA = Json.decodeFromStream(c.getResourceAsStream("/data/credit.json")!!)
    EXCHANGE_DATA = Json.decodeFromStream(c.getResourceAsStream("/data/exchange.json")!!)
}