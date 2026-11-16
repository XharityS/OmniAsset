package ru.echosystems.omni.app.ui

internal data class FormDate private constructor(val year: Int, val month: Int, val day: Int) {
    fun toIso(): String = "${year.toString().padStart(4, '0')}-${twoDigits(month)}-${twoDigits(day)}"

    fun toDisplay(): String = "${twoDigits(day)}.${twoDigits(month)}.${year.toString().padStart(4, '0')}"

    fun toUtcMillis(): Long {
        val daysInEarlierMonths = (1 until month).sumOf { daysInMonth(year, it) }
        return (daysBeforeYear(year) - epochDays + daysInEarlierMonths + day - 1) * millisPerDay
    }

    companion object {
        private const val millisPerDay = 86_400_000L
        private val epochDays = daysBeforeYear(1970)
        private val iso = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
        private val local = Regex("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}")

        fun parse(value: String): FormDate? {
            val input = value.trim()
            val (year, month, day) = when {
                iso.matches(input) -> input.split('-').map(String::toInt)
                local.matches(input) -> input.split('.').map(String::toInt).reversed()
                else -> return null
            }
            return if (year in 1..9999 && month in 1..12 && day in 1..daysInMonth(year, month)) {
                FormDate(year, month, day)
            } else null
        }

        // The date picker represents a calendar day as midnight in UTC.
        fun fromUtcMillis(millis: Long): FormDate {
            val days = (millis / millisPerDay - if (millis < 0 && millis % millisPerDay != 0L) 1 else 0) + epochDays
            var lower = 1
            var upper = 10000
            while (upper - lower > 1) {
                val middle = (lower + upper) / 2
                if (daysBeforeYear(middle) <= days) lower = middle else upper = middle
            }
            var month = 1
            var day = (days - daysBeforeYear(lower)).toInt() + 1
            while (day > daysInMonth(lower, month)) {
                day -= daysInMonth(lower, month)
                month++
            }
            return FormDate(lower, month, day)
        }

        private fun daysBeforeYear(year: Int): Long {
            val previous = year - 1L
            return 365 * previous + previous / 4 - previous / 100 + previous / 400
        }

        private fun daysInMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        private fun twoDigits(value: Int) = value.toString().padStart(2, '0')
    }
}
