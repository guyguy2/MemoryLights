package com.happypuppy.memorylights.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionInfoTest {

    @Test
    fun `getFullVersionInfo returns versionName and longVersionCode`() {
        val packageManager = mockk<PackageManager>()
        val context = mockk<Context>()
        val packageInfo = mockk<PackageInfo>(relaxed = true).apply {
            versionName = "1.2.0"
        }
        every { packageInfo.longVersionCode } returns 12L
        every { context.packageName } returns "com.happypuppy.memorylights"
        every { context.packageManager } returns packageManager
        @Suppress("DEPRECATION")
        every { packageManager.getPackageInfo("com.happypuppy.memorylights", 0) } returns packageInfo
        every {
            packageManager.getPackageInfo(
                "com.happypuppy.memorylights",
                any<PackageManager.PackageInfoFlags>()
            )
        } returns packageInfo

        assertEquals("1.2.0", AppVersionInfo.getVersionName(context))
        assertEquals("12", AppVersionInfo.getVersionCode(context))
        assertEquals("1.2.0 (12)", AppVersionInfo.getFullVersionInfo(context))
    }

    @Test
    fun `getFullVersionInfo falls back cleanly when PackageManager throws`() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.happypuppy.memorylights"
        every { context.packageManager } throws RuntimeException("unavailable")

        assertEquals("1.0.0", AppVersionInfo.getVersionName(context))
        assertEquals("1", AppVersionInfo.getVersionCode(context))
        assertEquals("1.0.0 (1)", AppVersionInfo.getFullVersionInfo(context))
    }
}
