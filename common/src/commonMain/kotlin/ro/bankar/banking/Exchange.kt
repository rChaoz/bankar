package ro.bankar.banking

import java.math.BigDecimal

/**
 * For each currency X, the map provides a list of currencies we can exchange to, as well as their respective rates (pairs of currency Y and rate f), as such:
 *
 * 1 of X -> f of Y
 */
typealias SExchangeData = Map<Currency, List<Pair<Currency, Double>>>

/**
 * Gets the rate of exchange from a currency to another
 */
fun SExchangeData.rate(from: Currency, to: Currency) = this[from]?.find { it.first == to }?.second

/**
 * Exchange `amount` of `from` currency to `return value` amount of `to` currency.
 */
fun SExchangeData.exchange(from: Currency, to: Currency, amount: BigDecimal) =
    this[from]?.find { it.first == to }?.let { it.second.toBigDecimal() * amount }

/**
 * Exchange `amount` of `from` currency to `return value` amount of `to` currency.
 */
fun SExchangeData.exchange(from: Currency, to: Currency, amount: Double) =
    this[from]?.find { it.first == to }?.let { it.second * amount }

/**
 * Exchange `return value` amount of `from` currency to `requiredAmount` of `to` currency.
 */
fun SExchangeData.reverseExchange(from: Currency, to: Currency, requiredAmount: BigDecimal) =
    this[from]?.find { it.first == to }?.let { requiredAmount / it.second.toBigDecimal() }

/**
 * Exchange `return value` amount of `from` currency to `requiredAmount` of `to` currency.
 */
fun SExchangeData.reverseExchange(from: Currency, to: Currency, requiredAmount: Double) =
    this[from]?.find { it.first == to }?.let { requiredAmount / it.second }