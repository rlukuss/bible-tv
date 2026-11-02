package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilySeedTest {

    private val family = """
        # La famille
        profile.1=Paul|adult
        profile.2=Marie|adult
        profile.3=Léa|adult
        profile.4=Enfants|child|com.google.android.youtube.tvkids,fr.tfou.max,com.disney.disneyplus
    """.trimIndent()

    @Test
    fun theFamilyFileGivesFourProfilesInOrder() {
        val profiles = FamilySeed.parse(family)
        assertEquals(listOf("Paul", "Marie", "Léa", "Enfants"), profiles.map { it.name })
        assertEquals(listOf("seed-1", "seed-2", "seed-3", "seed-4"), profiles.map { it.id })
        assertEquals(ProfileKind.CHILD, profiles[3].kind)
        assertEquals(
            setOf("com.google.android.youtube.tvkids", "fr.tfou.max", "com.disney.disneyplus"),
            profiles[3].allowedApps,
        )
    }

    @Test
    fun eachProfileGetsItsOwnColour() {
        assertEquals(listOf(0, 1, 2, 3), FamilySeed.parse(family).map { it.color })
    }

    @Test
    fun linesAreOrderedByTheirNumberNotTheirPlace() {
        val profiles = FamilySeed.parse("profile.2=Marie|adult\nprofile.1=Paul|adult")
        assertEquals(listOf("Paul", "Marie"), profiles.map { it.name })
    }

    @Test
    fun malformedLinesAreSkipped() {
        val profiles = FamilySeed.parse(
            """
            profile.1=Paul|adult
            profile.2=|adult
            profile.3=Rex|pet
            profile.x=Marie|adult
            something=else
            """.trimIndent(),
        )
        assertEquals(listOf("Paul"), profiles.map { it.name })
    }

    @Test
    fun aChildWithoutAppsIsKeptWithNoAllowedApp() {
        val child = FamilySeed.parse("profile.1=Léo|child").single()
        assertEquals(emptySet<String>(), child.allowedApps)
    }

    @Test
    fun emptyTextGivesNoProfiles() {
        assertEquals(emptyList<Profile>(), FamilySeed.parse(""))
    }

    @Test
    fun cityLineGivesTheWeatherCity() {
        val text = "profile.1=Paul|adult\ncity=Lyon|45.764|4.8357"
        assertEquals(WeatherLocation("Lyon", 45.764, 4.8357), FamilySeed.city(text))
        assertEquals(1, FamilySeed.parse(text).size)
    }

    @Test
    fun missingOrBrokenCityGivesNone() {
        assertEquals(null, FamilySeed.city("profile.1=Paul|adult"))
        assertEquals(null, FamilySeed.city("city=Lyon|north|4.8"))
    }
}
