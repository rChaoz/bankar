package ro.bankar.banking

fun checkIBAN(iban: String): Boolean {
    if (iban.length !in 18..34) return false
    val letter = 'A'..'Z'

    val countryCode = iban.substring(0, 2).also { code -> if (code.any { it !in letter }) return false }
    if (countryCode != "RO") return true

    val checkDigits = iban.substring(2, 4).also { digits -> if (digits.any { !it.isDigit() }) return false }
    val bankCode = iban.substring(4, 8).also { code -> if (code.any { it !in letter }) return false }
    val accountNumber = iban.substring(8).also { digits -> if (digits.any { !it.isDigit() }) return false }
    return checkDigits == calcIBANCheckDigits(countryCode, bankCode, accountNumber)
}

fun calcIBANCheckDigits(countryCode: String, bankCode: String, accountNumber: String): String {
    val checkDigits = 98 - ("${bankCode.toInt(36)}$accountNumber${countryCode.toInt(36)}00".toBigInteger() % 97.toBigInteger()).toInt()
    return "%02d".format(checkDigits)
}