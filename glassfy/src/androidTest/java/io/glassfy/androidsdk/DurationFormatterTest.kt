package io.glassfy.androidsdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.glassfy.androidsdk.internal.utils.DurationFormatter
import java.util.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DurationFormatterTest {
    @Test
    fun parseAndFormat() {
        // english
        val enLocale = Locale.ENGLISH
        assertEquals("1 day", DurationFormatter.parseISO8601Period("P1D")!!.format(enLocale)!!)
        assertEquals("3 days", DurationFormatter.parseISO8601Period("P3D")!!.format(enLocale)!!)
        assertEquals("1 week", DurationFormatter.parseISO8601Period("P1W")!!.format(enLocale)!!)
        assertEquals("4 weeks", DurationFormatter.parseISO8601Period("P4W")!!.format(enLocale)!!)
        assertEquals("1 month", DurationFormatter.parseISO8601Period("P1M")!!.format(enLocale)!!)
        assertEquals("3 months", DurationFormatter.parseISO8601Period("P3M")!!.format(enLocale)!!)
        assertEquals("6 months", DurationFormatter.parseISO8601Period("P6M")!!.format(enLocale)!!)
        assertEquals("1 year", DurationFormatter.parseISO8601Period("P1Y")!!.format(enLocale)!!)

        // arabic
        val arLocale = Locale.forLanguageTag("ar")
        assertEquals("1 يوم", DurationFormatter.parseISO8601Period("P1D")!!.format(arLocale)!!)
        assertEquals("3 يوم", DurationFormatter.parseISO8601Period("P3D")!!.format(arLocale)!!)
        assertEquals("1 أسبوع", DurationFormatter.parseISO8601Period("P1W")!!.format(arLocale)!!)
        assertEquals("4 أسبوع", DurationFormatter.parseISO8601Period("P4W")!!.format(arLocale)!!)
        assertEquals("1 شهر", DurationFormatter.parseISO8601Period("P1M")!!.format(arLocale)!!)
        assertEquals("3 شهر", DurationFormatter.parseISO8601Period("P3M")!!.format(arLocale)!!)
        assertEquals("6 شهر", DurationFormatter.parseISO8601Period("P6M")!!.format(arLocale)!!)
        assertEquals("1 عام", DurationFormatter.parseISO8601Period("P1Y")!!.format(arLocale)!!)

    }
}