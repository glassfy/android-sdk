package io.glassfy.androidsdk.internal.utils

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern



internal class DurationFormatter private constructor(
    private val days: Int,
    private val weeks: Int,
    private val months: Int,
    private val years: Int)
{
    enum class Unit {
        YEAR,
        MONTH,
        WEEK,
        DAY
    }

    companion object {
        fun parseISO8601Period(text: CharSequence) = runCatching {
            internalParseISO8601Period(text)
        }.getOrNull()


        fun unitName(unit: Unit, locale: Locale = Locale.getDefault()) = runCatching {
            evaluateUnitName(unit, locale)
        }.getOrNull()

        private fun internalParseISO8601Period(text: CharSequence): DurationFormatter {
            val iso8601Pattern = Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?", Pattern.CASE_INSENSITIVE)
            val matcher: Matcher = iso8601Pattern.matcher(text)
            if (matcher.matches()) {
                val negate = if ("-" == matcher.group(1)) -1 else 1
                val yearMatch = matcher.group(2)
                val monthMatch = matcher.group(3)
                val weekMatch = matcher.group(4)
                val dayMatch = matcher.group(5)
                if (yearMatch != null || monthMatch != null || dayMatch != null || weekMatch != null) {
                    fun parseNumber(str: String?, negate: Int): Int {
                        return if (str.isNullOrEmpty()) 0 else Math.multiplyExact(str.toInt(), negate)
                    }

                    val years = parseNumber(yearMatch, negate)
                    val months = parseNumber(monthMatch, negate)
                    val weeks = parseNumber(weekMatch, negate)
                    val days = Math.addExact(parseNumber(dayMatch, negate), Math.multiplyExact(weeks, 7))

                    return DurationFormatter(days, weeks, months, years)
                }
            }
            throw Exception("Text cannot be parsed to a Period")
        }

        private val numbersAndSpaceRegex = Regex("\\p{N}| ")

        private fun evaluateUnitName(unit: Unit, locale: Locale): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val measure = when (unit) {
                Unit.DAY -> Measure(1, MeasureUnit.DAY)
                Unit.WEEK -> Measure(1, MeasureUnit.WEEK)
                Unit.MONTH -> Measure(1, MeasureUnit.MONTH)
                Unit.YEAR -> Measure(1, MeasureUnit.YEAR)
            }

            return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
                .format(measure)
                .replace(numbersAndSpaceRegex,"") // remove any kind of numeric character in any script language (+ spaces)
            } else {
                throw Exception("SDK VERSION lower than N")
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun bestMeasure(days: Int, weeks: Int, months: Int, years: Int): Measure {
            return when (bestUnit(weeks, months, years)) {
                Unit.YEAR -> Measure(years, MeasureUnit.YEAR)
                Unit.MONTH -> Measure(months, MeasureUnit.MONTH)
                Unit.WEEK -> Measure(weeks, MeasureUnit.WEEK)
                Unit.DAY -> Measure(days, MeasureUnit.DAY)
            }
        }

        private fun bestUnit(weeks: Int, months: Int, years: Int): Unit {
            if (years > 0) return Unit.YEAR
            if (months > 0) return Unit.MONTH
            if (weeks > 0) return Unit.WEEK
            return Unit.DAY
        }
    }

    fun format(locale: Locale = Locale.getDefault()) = runCatching {
        internalFormat(days, weeks, months, years, locale)
    }.getOrNull()

    val unit = bestUnit(weeks, months, years)

    val totalDays = days + weeks*7 + months*30 + years*365

    private fun internalFormat(days: Int, weeks: Int, months: Int, years: Int, locale: Locale) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).format(bestMeasure(days, weeks, months, years))
        } else {
            throw Exception("Text cannot be parsed to a Period")
        }
}
