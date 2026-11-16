package ru.echosystems.omni.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormDateTest {
    @Test
    fun acceptsBothFormatsAndRejectsImpossibleDates() {
        assertEquals("2024-02-29", FormDate.parse("29.02.2024")?.toIso())
        assertEquals("16.11.2026", FormDate.parse("2026-11-16")?.toDisplay())
        listOf("29.02.2025", "29.02.1900", "31.04.2026", "2026-13-01", "0000-01-01", "1.1.2026").forEach {
            assertNull(FormDate.parse(it), it)
        }
    }

    @Test
    fun calendarRoundTripsIncludingLeapCenturiesAndEpochBoundary() {
        listOf("0001-01-01", "1900-03-01", "1969-12-31", "1970-01-01", "2000-02-29", "2026-11-16", "9999-12-31").forEach {
            val date = requireNotNull(FormDate.parse(it))
            assertEquals(date, FormDate.fromUtcMillis(date.toUtcMillis()), it)
        }
        assertEquals(0L, FormDate.parse("1970-01-01")?.toUtcMillis())
        assertEquals("1969-12-31", FormDate.fromUtcMillis(-1L).toIso())
    }
}
