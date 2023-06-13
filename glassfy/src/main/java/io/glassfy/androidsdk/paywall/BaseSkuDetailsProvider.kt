package io.glassfy.androidsdk.paywall

import android.os.Build
import androidx.annotation.RequiresApi
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.SkuDetails
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.N)
internal open class BaseSkuDetailsProvider {
    fun durations(): JSONObject {
        return JSONObject().apply {
            DurationFormatter.allUnits().forEach {
                put(DurationFormatter.unitKey(it), DurationFormatter.unitName(it))
            }
        }
    }

    fun locale(languageCode: String): Locale {
        return try {
            val lan = Locale.forLanguageTag(languageCode)
            if (lan.language.isNullOrEmpty()) Locale.getDefault() else lan
        } catch (_: Exception) {
            Locale.getDefault()
        }
    }

    fun buildSkusDetails(skus: List<Sku>, locale: Locale, suffix: String): JSONObject {
        return JSONObject().apply {
            val percentFormatter = NumberFormat.getPercentInstance(locale)
            val priceFormatter = NumberFormat.getCurrencyInstance(locale)
            val skuDailyPrices = FloatArray(skus.count())

            skus.forEachIndexed { idx, s ->
                val skusDetail = JSONObject().apply {
                    val p = s.product

                    val localeCurrencyCode = priceFormatter.currency?.currencyCode
                    if (localeCurrencyCode.isNullOrEmpty() || localeCurrencyCode != p.priceCurrencyCode) {
                        priceFormatter.currency = try {
                            Currency.getInstance(p.priceCurrencyCode)
                        } catch (_: Exception) {
                            Currency.getInstance("USD")
                        }
                    }

                    val hasFreeOffers = if (p.freeTrialPeriod.isNotEmpty()) 1 else -1 // phase 1
                    val hasIntroOffers = if (p.introductoryPriceAmountPeriod.isNotEmpty()) 1 else -1 // phase 2

                    val msg = JSONObject().apply {
                        put("${suffix}TITLE", p.title)
                        put("${suffix}DESCRIPTION", p.description)
                        put("${suffix}ORIGINAL_PRICE", p.originalPrice)

                        val formatter = DurationFormatter.parseISO8601Period(p.subscriptionPeriod)
                        put("${suffix}ORIGINAL_PERIOD", formatter?.unitName() ?: "DAY")
                        put("${suffix}ORIGINAL_DURATION", formatter?.format(locale))

                        val totalDays = formatter?.totalDays ?: 1

                        val originalPrice = p.originalPriceAmountMicro / 1000000.0f
                        val originalPriceDaily = originalPrice / totalDays
                        val originalPriceWeekly = originalPriceDaily * 7.0f
                        val originalPriceYearly = originalPriceDaily * 365.0f
                        val originalPriceMonthly = originalPriceYearly / 12.0f

                        skuDailyPrices[idx] = originalPriceDaily

                        put("${suffix}ORIGINAL_DAILY", priceFormatter.format(originalPriceDaily))
                        put("${suffix}ORIGINAL_WEEKLY", priceFormatter.format(originalPriceWeekly))
                        put("${suffix}ORIGINAL_MONTHLY", priceFormatter.format(originalPriceMonthly))
                        put("${suffix}ORIGINAL_YEARLY", priceFormatter.format(originalPriceYearly))

                        if (hasFreeOffers > 0) {
                            val freeFormatter = DurationFormatter.parseISO8601Period(p.freeTrialPeriod)
                            put("${suffix}INTRO_PERIOD", freeFormatter?.unitName() ?: "DAY")
                            put("${suffix}INTRO_DURATION", freeFormatter?.format(locale))
                            put("${suffix}INTRO_PRICE", "\$GL_FREE")
                            put("${suffix}INTRO_DAILY", "\$GL_FREE")
                            put("${suffix}INTRO_WEEKLY", "\$GL_FREE")
                            put("${suffix}INTRO_MONTHLY", "\$GL_FREE")
                            put("${suffix}INTRO_YEARLY", "\$GL_FREE")
                            put("${suffix}INTRO_DISCOUNT", percentFormatter.format(0))
                        } else if (hasIntroOffers > 0) {
                            val introFormatter = DurationFormatter.parseISO8601Period(p.introductoryPriceAmountPeriod)
                            put("${suffix}INTRO_PERIOD", introFormatter?.unitName() ?: "DAY")
                            put("${suffix}INTRO_DURATION", introFormatter?.format(locale))

                            val introPrice = p.introductoryPriceAmountMicro / 1000000.0f
                            val introTotalDays = introFormatter?.totalDays ?: 1
                            val introPriceDaily = introPrice / introTotalDays
                            val introPriceWeekly = introPriceDaily * 7.0f
                            val introPriceYearly = introPriceDaily * 365.0f
                            val introPriceMonthly = introPriceYearly / 12.0f

                            put("${suffix}INTRO_PRICE", priceFormatter.format(introPrice))
                            put("${suffix}INTRO_DAILY", priceFormatter.format(introPriceDaily))
                            put("${suffix}INTRO_WEEKLY", priceFormatter.format(introPriceWeekly))
                            put("${suffix}INTRO_MONTHLY", priceFormatter.format(introPriceMonthly))
                            put("${suffix}INTRO_YEARLY", priceFormatter.format(introPriceYearly))

                            val introDiscount = introPriceDaily / originalPriceDaily
                            // TODO: \$ missing in original NoCode impl?
                            put("INTRO_DISCOUNT", percentFormatter.format(introDiscount))
                        }

                        val price = p.priceAmountMicro / 1000000.0f
                        val priceDaily = price / totalDays
                        val priceWeekly = priceDaily * 7.0f
                        val priceYearly = priceDaily * 365.0f
                        val priceMonthly = priceYearly / 12.0f

                        put("${suffix}PERIOD", optString("ORIGINAL_PERIOD"))
                        put("${suffix}PRICE", p.price)
                        put("${suffix}DURATION", optString("ORIGINAL_DURATION"))
                        put("${suffix}DAILY", priceFormatter.format(priceDaily))
                        put("${suffix}WEEKLY", priceFormatter.format(priceWeekly))
                        put("${suffix}MONTHLY", priceFormatter.format(priceMonthly))
                        put("${suffix}YEARLY", priceFormatter.format(priceYearly))
                    }

                    val productJson = JSONObject().apply {
                        put("price", p.price)
                        put("localeidentifier", locale.language)
                        put("pricelocale", p.priceCurrencyCode)
                        put("countrycode", locale.country)
                        put("productidentifier", s.skuId)
                        put("subscriptionperiod", p.subscriptionPeriod)
                        put("introductoryprice", p.introductoryPrice)
                        put("discounts", emptyList<String>())
                    }

                    put("msg", msg)
                    put("product", productJson)
                    put("identifier", s.skuId)
                    put("offeringid", s.offeringId)
                    put("introductoryeligibility", 0)
                    put("promotionaleligibility", -1)
                    put("extravars", s.extravars)
                }
                put(s.skuId, skusDetail)
            }

            skus.forEachIndexed { i, sku ->
                skus.forEachIndexed { j, _ ->
                    var discount = 0.0
                    if (skuDailyPrices[i] > 0.0 && skuDailyPrices[j] > 0.0) {
                        discount = 1.0 - skuDailyPrices[i] / skuDailyPrices[j]
                    }

                    optJSONObject(sku.skuId)
                        ?.optJSONObject("msg")
                        ?.put(
                            "${suffix}ORIGINAL_DISCOUNT_" + j + 1,
                            percentFormatter.format(discount)
                        )
                }
            }
        }
    }
}