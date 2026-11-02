package com.tcltv.sahne.data

/** The profile rules that need no Android: the first profiles, and what a switch writes. */
object ProfileSwitch {

    const val DEFAULT_NAME = "Moi"

    /**
     * The profiles to start with and the one to make active. With the family's file, adults keep the home screen as
     * it is now and children start from a reduced one; without it, the current home becomes the only profile.
     */
    fun initial(settings: LauncherSettings, seed: List<Profile>, newId: () -> String): Pair<List<Profile>, String> {
        if (seed.isNotEmpty()) {
            val profiles = seed.map { p ->
                p.copy(snapshot = if (p.kind == ProfileKind.CHILD) ProfileSnapshot.CHILD else ProfileSnapshot.of(settings))
            }
            val active = profiles.firstOrNull { it.kind == ProfileKind.ADULT } ?: profiles.first()
            return profiles to active.id
        }
        val only = Profile(
            id = newId(),
            name = settings.profileName.trim().ifEmpty { DEFAULT_NAME },
            kind = ProfileKind.ADULT,
            color = settings.profileColor,
            snapshot = ProfileSnapshot.of(settings),
        )
        return listOf(only) to only.id
    }

    data class Switched(val profiles: List<Profile>, val target: Profile)

    /**
     * The active profile keeps a copy of the home as it is now, with the name and colour it may have been given in
     * the profile settings since; null when there is nothing to switch to.
     */
    fun switch(settings: LauncherSettings, profiles: List<Profile>, activeId: String?, targetId: String): Switched? {
        if (targetId == activeId) return null
        val target = profiles.firstOrNull { it.id == targetId } ?: return null
        val saved = profiles.map { if (it.id == activeId) kept(it, settings) else it }
        return Switched(saved, target)
    }

    /** [profile] as the current settings show it, for the active profile. */
    fun kept(profile: Profile, settings: LauncherSettings): Profile = profile.copy(
        name = settings.profileName.trim().ifEmpty { profile.name },
        color = settings.profileColor,
        snapshot = ProfileSnapshot.of(settings),
    )
}

/**
 * The family's profiles from `family.properties`, read at build time so first names stay off GitHub. One line per
 * profile: `profile.N=First name|adult or child|allowed apps, comma separated (children only)`.
 */
object FamilySeed {

    private val LINE = Regex("""^profile\.(\d+)\s*=\s*(.*)$""")

    fun parse(text: String): List<Profile> = text.lineSequence()
        .mapNotNull { LINE.find(it.trim()) }
        .mapNotNull { match -> match.groupValues[1].toInt() to (parseProfile(match.groupValues[1], match.groupValues[2]) ?: return@mapNotNull null) }
        .sortedBy { it.first }
        .mapIndexed { index, (_, profile) -> profile.copy(color = index) }
        .toList()

    private val CITY = Regex("""^city\s*=\s*(.*)$""")

    /** The weather city from a `city=Name|latitude|longitude` line, so a home's own town stays out of the source. */
    fun city(text: String): WeatherLocation? {
        val parts = text.lineSequence().firstNotNullOfOrNull { CITY.find(it.trim()) }?.groupValues?.get(1)?.split('|')?.map { it.trim() }
            ?: return null
        val name = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
        val latitude = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
        val longitude = parts.getOrNull(2)?.toDoubleOrNull() ?: return null
        return WeatherLocation(name, latitude, longitude)
    }

    private fun parseProfile(number: String, value: String): Profile? {
        val parts = value.split('|').map { it.trim() }
        val name = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
        val kind = when (parts.getOrNull(1)?.lowercase()) {
            "adult" -> ProfileKind.ADULT
            "child" -> ProfileKind.CHILD
            else -> return null
        }
        return Profile(
            id = "seed-$number",
            name = name,
            kind = kind,
            allowedApps = parts.getOrNull(2).orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        )
    }
}
