package com.stripe.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.VisibleForTesting
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

internal class FingerprintRequestParamsFactory @VisibleForTesting internal constructor(
    private val displayMetrics: DisplayMetrics,
    private val packageName: String,
    private val packageManager: PackageManager,
    private val timeZone: String,
    private val clientFingerprintDataStore: ClientFingerprintDataStore
) {
    private val versionName: String?
        get() {
            if (packageName.isNotBlank()) {
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    if (packageInfo?.versionName != null) {
                        return packageInfo.versionName
                    }
                } catch (ignored: PackageManager.NameNotFoundException) {
                }
            }

            return null
        }

    private val screen: String =
        "${displayMetrics.widthPixels}w_${displayMetrics.heightPixels}h_${displayMetrics.densityDpi}dpi"

    private val androidVersionString =
        "Android ${Build.VERSION.RELEASE} ${Build.VERSION.CODENAME} ${Build.VERSION.SDK_INT}"

    internal constructor(context: Context) : this(
        displayMetrics = context.resources.displayMetrics,
        packageName = context.packageName.orEmpty(),
        packageManager = context.packageManager,
        timeZone = createTimezone(),
        clientFingerprintDataStore = ClientFingerprintDataStore.Default(context)
    )

    @JvmSynthetic
    internal fun createParams(): Map<String, Any> {
        return mapOf(
            "v2" to 1,
            "tag" to BuildConfig.VERSION_NAME,
            "src" to "android-sdk",
            "a" to createFirstMap(),
            "b" to createSecondMap()
        )
    }

    private fun createFirstMap(): Map<String, Any> {
        return mapOf(
            "c" to createValueMap(Locale.getDefault().toString()),
            "d" to createValueMap(androidVersionString),
            "f" to createValueMap(screen),
            "g" to createValueMap(timeZone)
        )
    }

    private fun createSecondMap(): Map<String, Any> {
        return mapOf(
            "d" to clientFingerprintDataStore.getMuid(),
            "e" to clientFingerprintDataStore.getSid(),
            "k" to packageName,
            "o" to Build.VERSION.RELEASE,
            "p" to Build.VERSION.SDK_INT,
            "q" to Build.MANUFACTURER,
            "r" to Build.BRAND,
            "s" to Build.MODEL,
            "t" to Build.TAGS
        ).plus(
            versionName?.let { mapOf("l" to it) }.orEmpty()
        )
    }

    private fun createValueMap(value: String): Map<String, Any> {
        return mapOf("v" to value)
    }

    private companion object {
        private fun createTimezone(): String {
            val minutes = TimeUnit.MINUTES.convert(TimeZone.getDefault().rawOffset.toLong(),
                TimeUnit.MILLISECONDS).toInt()
            if (minutes % 60 == 0) {
                return (minutes / 60).toString()
            }

            val decimalValue = BigDecimal(minutes)
                .setScale(2, BigDecimal.ROUND_HALF_EVEN)
            val decHours = decimalValue.divide(
                BigDecimal(60),
                MathContext(2)
            )
                .setScale(2, BigDecimal.ROUND_HALF_EVEN)
            return decHours.toString()
        }
    }
}
