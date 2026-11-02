package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtremePerformanceTest {

    private val defaults = LauncherSettings(glassEffects = true, wallpaperAnimated = false)
    private val current = LauncherSettings(
        glassEffects = true,
        wallpaperAnimated = true,
        adaptiveBackground = true,
        reduceMotion = false,
        profileName = "Paul",
    )

    @Test
    fun turningItOnSwitchesEveryEffectOff() {
        val s = ExtremePerformance.apply(current, on = true, defaults)
        assertTrue(s.extremePerformance)
        assertFalse(s.glassEffects)
        assertFalse(s.wallpaperAnimated)
        assertFalse(s.adaptiveBackground)
        assertTrue(s.reduceMotion)
    }

    @Test
    fun turningItOffGivesBackWhatThisTvStartsWith() {
        val extreme = ExtremePerformance.apply(current, on = true, defaults)
        val s = ExtremePerformance.apply(extreme, on = false, defaults)
        assertFalse(s.extremePerformance)
        assertEquals(defaults.glassEffects, s.glassEffects)
        assertEquals(defaults.wallpaperAnimated, s.wallpaperAnimated)
        assertEquals(defaults.adaptiveBackground, s.adaptiveBackground)
        assertEquals(defaults.reduceMotion, s.reduceMotion)
    }

    @Test
    fun nothingElseChanges() {
        assertEquals("Paul", ExtremePerformance.apply(current, on = true, defaults).profileName)
    }
}
