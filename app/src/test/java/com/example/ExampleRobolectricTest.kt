package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppItem
import com.example.data.model.InactivityUnit
import com.example.ui.components.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Archivist", appName)
    }

    @Test
    fun testAppItemStorageCalculations() {
        val app = AppItem(
            packageName = "com.example.test",
            appName = "Test App",
            appBytes = 100 * 1024 * 1024L,
            dataBytes = 20 * 1024 * 1024L,
            cacheBytes = 30 * 1024 * 1024L,
            isArchived = false
        )

        assertEquals(150 * 1024 * 1024L, app.totalBytes)
        assertEquals(130 * 1024 * 1024L, app.reclaimableBytes)
        assertEquals("150.0 MB", FormatUtils.formatBytes(app.totalBytes))
    }

    @Test
    fun testInactivityConversion() {
        val daysMs = InactivityUnit.DAYS.toMillis(7)
        val weeksMs = InactivityUnit.WEEKS.toMillis(1)
        assertEquals(daysMs, weeksMs)
    }
}
