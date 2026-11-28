package com.inferra

import com.inferra.data.repository.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun testUserSettingsDefaultValues() {
        val defaultSettings = UserSettings()
        assertTrue(defaultSettings.isDarkMode)
    }

    @Test
    fun testUserSettingsCustomValues() {
        val customSettings = UserSettings(
            isDarkMode = false
        )
        assertEquals(false, customSettings.isDarkMode)
    }
}
