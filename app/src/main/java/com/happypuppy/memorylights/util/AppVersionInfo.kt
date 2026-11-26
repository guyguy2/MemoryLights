package com.happypuppy.memorylights.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Utility object to read app version information dynamically from [PackageManager],
 * matching the pattern used in Simple Day Counter and Simple Speech Timer.
 */
object AppVersionInfo {
    fun getVersionName(context: Context): String {
        return try {
            getPackageInfo(context).versionName?.takeIf { it.isNotBlank() } ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun getVersionCode(context: Context): String {
        return try {
            getPackageInfo(context).longVersionCode.toString()
        } catch (_: Exception) {
            "1"
        }
    }

    fun getFullVersionInfo(context: Context): String {
        return "${getVersionName(context)} (${getVersionCode(context)})"
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}
