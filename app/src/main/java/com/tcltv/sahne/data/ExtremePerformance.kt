package com.tcltv.sahne.data

/**
 * One switch for slow TVs: no glass, no moving light, no colour following the focused app, no animation and a plain
 * background (see CinematicBackground). Turning it off goes back to what this TV starts with, not to earlier choices.
 */
object ExtremePerformance {

    fun apply(settings: LauncherSettings, on: Boolean, defaults: LauncherSettings): LauncherSettings =
        if (on) {
            settings.copy(
                extremePerformance = true,
                glassEffects = false,
                wallpaperAnimated = false,
                adaptiveBackground = false,
                reduceMotion = true,
            )
        } else {
            settings.copy(
                extremePerformance = false,
                glassEffects = defaults.glassEffects,
                wallpaperAnimated = defaults.wallpaperAnimated,
                adaptiveBackground = defaults.adaptiveBackground,
                reduceMotion = defaults.reduceMotion,
            )
        }
}
