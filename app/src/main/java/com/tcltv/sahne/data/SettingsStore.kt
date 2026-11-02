package com.tcltv.sahne.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import java.io.IOException
import java.util.UUID
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

enum class CardTheme { DARK, LIGHT }

/** ROW: large title with one app row and last played. GRID: a favorites shelf above a grid of every app. */
enum class HomeLayout { ROW, GRID }

data class WeatherLocation(val name: String, val latitude: Double, val longitude: Double)

data class InputOverride(val name: String?, val logo: DeviceLogo?)

data class LauncherSettings(
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val showGreeting: Boolean = true,
    val reduceMotion: Boolean = false,
    /** Every effect off and a plain background, for slow TVs (see [ExtremePerformance]). */
    val extremePerformance: Boolean = false,
    val adaptiveBackground: Boolean = true,
    val glassEffects: Boolean = true,
    val airplayReceiver: Boolean = false,
    val cardTheme: CardTheme = CardTheme.LIGHT,
    val homeLayout: HomeLayout = HomeLayout.GRID,
    val showClock: Boolean = true,
    val showWeather: Boolean = true,
    val showVerse: Boolean = true,
    val showAirPlay: Boolean = true,
    val showMemory: Boolean = true,
    val showInputs: Boolean = true,
    val showNotifications: Boolean = true,
    val showChromecast: Boolean = true,
    val showLastPlayed: Boolean = true,
    val focusOutline: Boolean = true,
    val profileName: String = "",
    val profileColor: Int = 0,
    val showPlayUpdates: Boolean = true,
    val weatherLocation: WeatherLocation = DEFAULT_LOCATION,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val weatherRefreshMinutes: Int = 30,
    val autoDetectInputs: Boolean = true,
    val hideDisconnectedInputs: Boolean = true,
    val inputOverrides: Map<String, InputOverride> = emptyMap(),
    val appOrder: List<String> = emptyList(),
    val hiddenApps: Set<String> = emptySet(),
    val shownApps: Set<String> = emptySet(),
    val showSystemApps: Boolean = false,
    /** The grid's "Add app" tile; apps can also be added from Settings > Apps. */
    val showAddTile: Boolean = true,
    val autoCleanMemory: Boolean = false,
    val memoryThresholdPercent: Int = 80,
    val memoryCleanExclusions: Set<String> = emptySet(),
    val appOverrides: Map<String, AppOverride> = emptyMap(),
    val folders: List<Folder> = emptyList(),
    val parental: Parental = Parental(),
    /** "" for the colour that follows the focused app, else a [WallpaperKey]. */
    val wallpaper: String = "",
    val wallpaperAnimated: Boolean = false,
    /** File names of pictures the user added as wallpapers. */
    val userWallpapers: List<String> = emptyList(),
    /** Everyone who watches; the active one's home is the one in the fields above (see [ProfileSnapshot]). */
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String? = null,
) {
    val activeProfile: Profile? get() = profiles.firstOrNull { it.id == activeProfileId }

    companion object {
        // A family.properties `city=` line sets the home's own town at build time; Paris otherwise.
        val DEFAULT_LOCATION = FamilySeed.city(com.tcltv.sahne.BuildConfig.FAMILY) ?: WeatherLocation("Paris", 48.8566, 2.3522)
    }
}

// A corrupt settings file resets to defaults instead of crash-looping the Home app.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val showGreeting = booleanPreferencesKey("show_greeting")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val extremePerformance = booleanPreferencesKey("extreme_performance")
        val adaptiveBackground = booleanPreferencesKey("adaptive_background")
        val glassEffects = booleanPreferencesKey("glass_effects")
        val airplayReceiver = booleanPreferencesKey("airplay_receiver")
        val cardTheme = stringPreferencesKey("card_theme")
        val homeLayout = stringPreferencesKey("home_layout")
        val showClock = booleanPreferencesKey("show_clock")
        val showWeather = booleanPreferencesKey("show_weather")
        val showVerse = booleanPreferencesKey("show_verse")
        val showAirPlay = booleanPreferencesKey("show_airplay")
        val showMemory = booleanPreferencesKey("show_memory")
        val showInputs = booleanPreferencesKey("show_inputs")
        val showNotifications = booleanPreferencesKey("show_notifications")
        val showChromecast = booleanPreferencesKey("show_google_home")
        val showLastPlayed = booleanPreferencesKey("show_last_played")
        val focusOutline = booleanPreferencesKey("focus_outline")
        val profileName = stringPreferencesKey("profile_name")
        val profileColor = intPreferencesKey("profile_color")
        val showPlayUpdates = booleanPreferencesKey("show_play_updates")
        val locationName = stringPreferencesKey("weather_location_name")
        val latitude = doublePreferencesKey("weather_latitude")
        val longitude = doublePreferencesKey("weather_longitude")
        val temperatureUnit = stringPreferencesKey("temperature_unit")
        val weatherRefreshMinutes = intPreferencesKey("weather_refresh_minutes")
        val autoDetectInputs = booleanPreferencesKey("auto_detect_inputs")
        val hideDisconnectedInputs = booleanPreferencesKey("hide_disconnected_inputs")
        val appOrder = stringPreferencesKey("app_order")
        val hiddenApps = stringSetPreferencesKey("hidden_apps")
        val shownApps = stringSetPreferencesKey("shown_apps")
        val showSystemApps = booleanPreferencesKey("show_system_apps")
        val showAddTile = booleanPreferencesKey("show_add_tile")
        val autoCleanMemory = booleanPreferencesKey("auto_clean_memory")
        val memoryThreshold = intPreferencesKey("memory_threshold_percent")
        val memoryExclusions = stringSetPreferencesKey("memory_clean_exclusions")
        val wallpaper = stringPreferencesKey("wallpaper")
        val wallpaperAnimated = booleanPreferencesKey("wallpaper_animated")
        val userWallpapers = stringPreferencesKey("user_wallpapers")
        val appOverrides = stringPreferencesKey("app_overrides")
        val folders = stringPreferencesKey("folders")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val pinKind = stringPreferencesKey("pin_kind")
        val protectSettings = booleanPreferencesKey("protect_settings")
        val pinFailures = intPreferencesKey("pin_failures")
        val pinLockedUntil = longPreferencesKey("pin_locked_until")
        val profiles = stringPreferencesKey("profiles")
        val activeProfileId = stringPreferencesKey("active_profile_id")
        const val INPUT_NAME_PREFIX = "input_name:"
        const val INPUT_LOGO_PREFIX = "input_logo:"
    }

    val settings: Flow<LauncherSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    private fun Preferences.toSettings(): LauncherSettings {
        // The TV's own capability picks the starting values for the effects; saved choices still win below.
        val defaults = DeviceProfile.of(context).defaults()
        val overrides = mutableMapOf<String, InputOverride>()
        asMap().forEach { (key, value) ->
            val name = key.name
            when {
                name.startsWith(Keys.INPUT_NAME_PREFIX) -> {
                    val id = name.removePrefix(Keys.INPUT_NAME_PREFIX)
                    overrides[id] = (overrides[id] ?: InputOverride(null, null)).copy(name = value as String)
                }
                name.startsWith(Keys.INPUT_LOGO_PREFIX) -> {
                    val id = name.removePrefix(Keys.INPUT_LOGO_PREFIX)
                    val logo = DeviceLogo.entries.firstOrNull { it.name == value }
                    overrides[id] = (overrides[id] ?: InputOverride(null, null)).copy(logo = logo)
                }
            }
        }
        return LauncherSettings(
            use24Hour = this[Keys.use24Hour] ?: defaults.use24Hour,
            showSeconds = this[Keys.showSeconds] ?: defaults.showSeconds,
            showGreeting = this[Keys.showGreeting] ?: defaults.showGreeting,
            reduceMotion = this[Keys.reduceMotion] ?: defaults.reduceMotion,
            extremePerformance = this[Keys.extremePerformance] ?: false,
            adaptiveBackground = this[Keys.adaptiveBackground] ?: defaults.adaptiveBackground,
            glassEffects = this[Keys.glassEffects] ?: defaults.glassEffects,
            airplayReceiver = this[Keys.airplayReceiver] ?: defaults.airplayReceiver,
            cardTheme = CardTheme.entries.firstOrNull { it.name == this[Keys.cardTheme] } ?: defaults.cardTheme,
            homeLayout = HomeLayout.entries.firstOrNull { it.name == this[Keys.homeLayout] } ?: defaults.homeLayout,
            showClock = this[Keys.showClock] ?: defaults.showClock,
            showWeather = this[Keys.showWeather] ?: defaults.showWeather,
            showVerse = this[Keys.showVerse] ?: defaults.showVerse,
            showAirPlay = this[Keys.showAirPlay] ?: defaults.showAirPlay,
            showMemory = this[Keys.showMemory] ?: defaults.showMemory,
            showInputs = this[Keys.showInputs] ?: defaults.showInputs,
            showNotifications = this[Keys.showNotifications] ?: defaults.showNotifications,
            showChromecast = this[Keys.showChromecast] ?: defaults.showChromecast,
            showLastPlayed = this[Keys.showLastPlayed] ?: defaults.showLastPlayed,
            focusOutline = this[Keys.focusOutline] ?: defaults.focusOutline,
            profileName = this[Keys.profileName] ?: defaults.profileName,
            profileColor = this[Keys.profileColor] ?: defaults.profileColor,
            showPlayUpdates = this[Keys.showPlayUpdates] ?: defaults.showPlayUpdates,
            weatherLocation = WeatherLocation(
                name = this[Keys.locationName] ?: defaults.weatherLocation.name,
                latitude = this[Keys.latitude] ?: defaults.weatherLocation.latitude,
                longitude = this[Keys.longitude] ?: defaults.weatherLocation.longitude,
            ),
            temperatureUnit = TemperatureUnit.entries.firstOrNull { it.name == this[Keys.temperatureUnit] }
                ?: defaults.temperatureUnit,
            weatherRefreshMinutes = this[Keys.weatherRefreshMinutes] ?: defaults.weatherRefreshMinutes,
            autoDetectInputs = this[Keys.autoDetectInputs] ?: defaults.autoDetectInputs,
            hideDisconnectedInputs = this[Keys.hideDisconnectedInputs] ?: defaults.hideDisconnectedInputs,
            inputOverrides = overrides,
            appOrder = this[Keys.appOrder]?.split(',')?.filter { it.isNotBlank() } ?: defaults.appOrder,
            hiddenApps = this[Keys.hiddenApps] ?: defaults.hiddenApps,
            shownApps = this[Keys.shownApps] ?: defaults.shownApps,
            showSystemApps = this[Keys.showSystemApps] ?: defaults.showSystemApps,
            showAddTile = this[Keys.showAddTile] ?: defaults.showAddTile,
            autoCleanMemory = this[Keys.autoCleanMemory] ?: defaults.autoCleanMemory,
            memoryThresholdPercent = this[Keys.memoryThreshold] ?: defaults.memoryThresholdPercent,
            memoryCleanExclusions = this[Keys.memoryExclusions] ?: defaults.memoryCleanExclusions,
            wallpaper = this[Keys.wallpaper] ?: defaults.wallpaper,
            wallpaperAnimated = this[Keys.wallpaperAnimated] ?: defaults.wallpaperAnimated,
            userWallpapers = this[Keys.userWallpapers]?.split('|')?.filter { it.isNotBlank() } ?: defaults.userWallpapers,
            appOverrides = CustomizationJson.decodeOverrides(this[Keys.appOverrides]),
            folders = CustomizationJson.decodeFolders(this[Keys.folders]),
            parental = Parental(
                pinHash = this[Keys.pinHash],
                pinSalt = this[Keys.pinSalt],
                pinKind = PinKind.entries.firstOrNull { it.name == this[Keys.pinKind] } ?: PinKind.DIGITS,
                protectSettings = this[Keys.protectSettings] ?: defaults.parental.protectSettings,
                failures = this[Keys.pinFailures] ?: 0,
                lockedUntilMillis = this[Keys.pinLockedUntil] ?: 0L,
            ),
            profiles = ProfilesCodec.decode(this[Keys.profiles]),
            activeProfileId = this[Keys.activeProfileId],
        )
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        try {
            context.dataStore.edit { block(it) }
        } catch (e: IOException) {
            // A failed write (e.g. full storage) keeps the previous value; there is nothing better to fall back to.
            Log.w("SettingsStore", "Couldn't save settings", e)
        }
    }

    suspend fun setUse24Hour(value: Boolean) = edit { it[Keys.use24Hour] = value }
    suspend fun setShowSeconds(value: Boolean) = edit { it[Keys.showSeconds] = value }
    suspend fun setShowGreeting(value: Boolean) = edit { it[Keys.showGreeting] = value }
    suspend fun setReduceMotion(value: Boolean) = edit { it[Keys.reduceMotion] = value }

    suspend fun setExtremePerformance(on: Boolean) = edit { prefs ->
        val s = ExtremePerformance.apply(prefs.toSettings(), on, DeviceProfile.of(context).defaults())
        prefs[Keys.extremePerformance] = s.extremePerformance
        prefs[Keys.glassEffects] = s.glassEffects
        prefs[Keys.wallpaperAnimated] = s.wallpaperAnimated
        prefs[Keys.adaptiveBackground] = s.adaptiveBackground
        prefs[Keys.reduceMotion] = s.reduceMotion
    }
    suspend fun setAdaptiveBackground(value: Boolean) = edit { it[Keys.adaptiveBackground] = value }
    suspend fun setGlassEffects(value: Boolean) = edit { it[Keys.glassEffects] = value }
    suspend fun setAirplayReceiver(value: Boolean) = edit { it[Keys.airplayReceiver] = value }
    suspend fun setCardTheme(value: CardTheme) = edit { it[Keys.cardTheme] = value.name }
    suspend fun setHomeLayout(value: HomeLayout) = edit { it[Keys.homeLayout] = value.name }
    suspend fun setShowClock(value: Boolean) = edit { it[Keys.showClock] = value }
    suspend fun setShowWeather(value: Boolean) = edit { it[Keys.showWeather] = value }
    suspend fun setShowVerse(value: Boolean) = edit { it[Keys.showVerse] = value }
    suspend fun setShowAirPlay(value: Boolean) = edit { it[Keys.showAirPlay] = value }
    suspend fun setShowMemory(value: Boolean) = edit { it[Keys.showMemory] = value }
    suspend fun setShowInputs(value: Boolean) = edit { it[Keys.showInputs] = value }
    suspend fun setShowNotifications(value: Boolean) = edit { it[Keys.showNotifications] = value }
    suspend fun setShowChromecast(value: Boolean) = edit { it[Keys.showChromecast] = value }
    suspend fun setShowLastPlayed(value: Boolean) = edit { it[Keys.showLastPlayed] = value }
    suspend fun setFocusOutline(value: Boolean) = edit { it[Keys.focusOutline] = value }
    suspend fun setProfileName(value: String) = edit { it[Keys.profileName] = value.trim() }
    suspend fun setProfileColor(value: Int) = edit { it[Keys.profileColor] = value }
    suspend fun setShowPlayUpdates(value: Boolean) = edit { it[Keys.showPlayUpdates] = value }

    suspend fun setWeatherLocation(location: WeatherLocation) = edit {
        it[Keys.locationName] = location.name
        it[Keys.latitude] = location.latitude
        it[Keys.longitude] = location.longitude
    }

    suspend fun setTemperatureUnit(value: TemperatureUnit) = edit { it[Keys.temperatureUnit] = value.name }
    suspend fun setWeatherRefreshMinutes(value: Int) = edit { it[Keys.weatherRefreshMinutes] = value }
    suspend fun setAutoDetectInputs(value: Boolean) = edit { it[Keys.autoDetectInputs] = value }
    suspend fun setHideDisconnectedInputs(value: Boolean) = edit { it[Keys.hideDisconnectedInputs] = value }

    suspend fun setInputOverride(inputId: String, name: String?, logo: DeviceLogo?) = edit {
        val nameKey = stringPreferencesKey(Keys.INPUT_NAME_PREFIX + inputId)
        val logoKey = stringPreferencesKey(Keys.INPUT_LOGO_PREFIX + inputId)
        if (name.isNullOrBlank()) it.remove(nameKey) else it[nameKey] = name.trim()
        if (logo == null) it.remove(logoKey) else it[logoKey] = logo.name
    }

    suspend fun setAppOrder(order: List<String>) = edit { it[Keys.appOrder] = order.joinToString(",") }
    suspend fun setAppVisibility(hidden: Set<String>, shown: Set<String>) = edit {
        it[Keys.hiddenApps] = hidden
        it[Keys.shownApps] = shown
    }
    suspend fun setShowSystemApps(value: Boolean) = edit { it[Keys.showSystemApps] = value }
    suspend fun setShowAddTile(value: Boolean) = edit { it[Keys.showAddTile] = value }
    suspend fun setAutoCleanMemory(value: Boolean) = edit { it[Keys.autoCleanMemory] = value }
    suspend fun setMemoryThreshold(value: Int) = edit { it[Keys.memoryThreshold] = value }
    suspend fun setMemoryCleanExclusions(value: Set<String>) = edit { it[Keys.memoryExclusions] = value }

    suspend fun setWallpaper(value: String) = edit { it[Keys.wallpaper] = value }
    suspend fun setWallpaperAnimated(value: Boolean) = edit { it[Keys.wallpaperAnimated] = value }
    suspend fun setUserWallpapers(value: List<String>) = edit { it[Keys.userWallpapers] = value.joinToString("|") }

    suspend fun setAppOverrides(value: Map<String, AppOverride>) = edit { it[Keys.appOverrides] = CustomizationJson.encodeOverrides(value) }
    suspend fun setFolders(value: List<Folder>) = edit { it[Keys.folders] = CustomizationJson.encodeFolders(value) }

    suspend fun setPin(hash: String, salt: String, kind: PinKind) = edit {
        it[Keys.pinHash] = hash
        it[Keys.pinSalt] = salt
        it[Keys.pinKind] = kind.name
        it[Keys.pinFailures] = 0
        it[Keys.pinLockedUntil] = 0L
    }

    suspend fun clearPin() = edit {
        it.remove(Keys.pinHash)
        it.remove(Keys.pinSalt)
        it[Keys.pinFailures] = 0
        it[Keys.pinLockedUntil] = 0L
    }

    suspend fun setProtectSettings(value: Boolean) = edit { it[Keys.protectSettings] = value }

    suspend fun recordPinFailure(now: Long) = edit {
        val failures = (it[Keys.pinFailures] ?: 0) + 1
        it[Keys.pinFailures] = failures
        it[Keys.pinLockedUntil] = now + PinHasher.lockoutMillisAfter(failures)
    }

    suspend fun clearPinFailures() = edit {
        it[Keys.pinFailures] = 0
        it[Keys.pinLockedUntil] = 0L
    }

    /** Creates the first profiles once: the family's from [seed], or the current home as the only one. */
    suspend fun ensureProfiles(seed: List<Profile>) = edit { prefs ->
        if (ProfilesCodec.decode(prefs[Keys.profiles]).isNotEmpty()) return@edit
        val (profiles, activeId) = ProfileSwitch.initial(prefs.toSettings(), seed) { UUID.randomUUID().toString() }
        prefs[Keys.profiles] = ProfilesCodec.encode(profiles)
        prefs[Keys.activeProfileId] = activeId
        prefs.writeHome(profiles.first { it.id == activeId })
    }

    /** Keeps the active profile's home and puts [targetId]'s in its place, in one write. */
    suspend fun switchProfile(targetId: String) = edit { prefs ->
        val settings = prefs.toSettings()
        val switched = ProfileSwitch.switch(settings, settings.profiles, settings.activeProfileId, targetId) ?: return@edit
        prefs[Keys.profiles] = ProfilesCodec.encode(switched.profiles)
        prefs[Keys.activeProfileId] = targetId
        prefs.writeHome(switched.target)
    }

    /**
     * Saves the profiles [change] returns from the current ones (null leaves them). The profile watching now keeps its
     * name and colour in the home's own keys, and takes the matching home if it became a child or an adult.
     */
    suspend fun editProfiles(change: (profiles: List<Profile>, activeId: String?) -> List<Profile>?) = edit { prefs ->
        val settings = prefs.toSettings()
        val activeId = settings.activeProfileId
        val current = settings.profiles.map { if (it.id == activeId) ProfileSwitch.kept(it, settings) else it }
        val changed = change(current, activeId) ?: return@edit
        prefs[Keys.profiles] = ProfilesCodec.encode(changed)
        val before = current.firstOrNull { it.id == activeId } ?: return@edit
        val after = changed.firstOrNull { it.id == activeId } ?: return@edit
        if (after.kind != before.kind) {
            prefs.writeHome(after)
        } else {
            prefs[Keys.profileName] = after.name
            prefs[Keys.profileColor] = after.color
        }
    }

    private fun MutablePreferences.writeHome(profile: Profile) {
        val home = profile.snapshot
        this[Keys.profileName] = profile.name
        this[Keys.profileColor] = profile.color
        this[Keys.appOrder] = home.appOrder.joinToString(",")
        this[Keys.hiddenApps] = home.hiddenApps
        this[Keys.shownApps] = home.shownApps
        this[Keys.folders] = CustomizationJson.encodeFolders(home.folders)
        this[Keys.showInputs] = home.showInputs
        this[Keys.showAddTile] = home.showAddTile
        this[Keys.showLastPlayed] = home.showLastPlayed
    }

    suspend fun reset() = edit { it.clear() }
}
