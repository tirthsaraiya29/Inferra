package com.inferra

import com.inferra.data.repository.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun testUserSettingsDefaultValues() {
        val defaultSettings = UserSettings()
        assertEquals(0.8f, defaultSettings.glassIntensity)
        assertTrue(defaultSettings.isDarkMode)
        assertEquals("", defaultSettings.modelStoragePath)
    }

    @Test
    fun testUserSettingsCustomValues() {
        val customSettings = UserSettings(
            glassIntensity = 0.5f,
            isDarkMode = false,
            isTelemetryEnabled = true,
            autoRefreshData = false,
            modelStoragePath = "/storage/emulated/0/Download/models"
        )
        assertEquals(0.5f, customSettings.glassIntensity)
        assertEquals(false, customSettings.isDarkMode)
        assertTrue(customSettings.isTelemetryEnabled)
        assertEquals("/storage/emulated/0/Download/models", customSettings.modelStoragePath)
    }
}
