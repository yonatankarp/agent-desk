package com.yonatankarp.agentdesk.core.domain.events

internal fun EventTimestamp.epochMinute(): Long {
    val text = value
    val year = text.substring(0, 4).toInt()
    val month = text.substring(5, 7).toInt()
    val day = text.substring(8, 10).toInt()
    val hour = text.substring(11, 13).toInt()
    val minute = text.substring(14, 16).toInt()
    return daysFromCivil(year, month, day) * 24L * 60L + hour * 60L + minute
}

private fun daysFromCivil(
    year: Int,
    month: Int,
    day: Int,
): Long {
    val adjustedYear = if (month <= 2) year - 1 else year
    val era = floorDiv(adjustedYear, 400)
    val yearOfEra = adjustedYear - era * 400
    val monthPrime = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * monthPrime + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return (era * 146097 + dayOfEra - 719468).toLong()
}

private fun floorDiv(
    value: Int,
    divisor: Int,
): Int {
    val quotient = value / divisor
    val remainder = value % divisor
    return if (remainder != 0 && (value xor divisor) < 0) quotient - 1 else quotient
}
