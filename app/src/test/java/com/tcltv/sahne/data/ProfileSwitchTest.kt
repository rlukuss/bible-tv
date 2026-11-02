package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSwitchTest {

    private val current = LauncherSettings(
        profileName = "Paul",
        profileColor = 2,
        appOrder = listOf("com.netflix.ninja"),
        hiddenApps = setOf("org.videolan.vlc"),
        showAddTile = false,
    )

    @Test
    fun withoutASeedTheCurrentHomeBecomesTheOnlyProfile() {
        val (profiles, activeId) = ProfileSwitch.initial(current, seed = emptyList(), newId = { "new" })
        val profile = profiles.single()
        assertEquals("new", activeId)
        assertEquals("Paul", profile.name)
        assertEquals(ProfileKind.ADULT, profile.kind)
        assertEquals(2, profile.color)
        assertEquals(ProfileSnapshot.of(current), profile.snapshot)
    }

    @Test
    fun anUnnamedHomeGetsTheDefaultName() {
        val (profiles, _) = ProfileSwitch.initial(LauncherSettings(), seed = emptyList(), newId = { "new" })
        assertEquals(ProfileSwitch.DEFAULT_NAME, profiles.single().name)
    }

    @Test
    fun aSeedGivesAdultsTheCurrentHomeAndChildrenTheirOwn() {
        val seed = FamilySeed.parse(
            """
            profile.1=Paul|adult
            profile.2=Enfants|child|fr.tfou.max
            """.trimIndent(),
        )
        val (profiles, activeId) = ProfileSwitch.initial(current, seed, newId = { error("seed ids are kept") })
        assertEquals("seed-1", activeId)
        assertEquals(ProfileSnapshot.of(current), profiles[0].snapshot)
        assertEquals(ProfileSnapshot.CHILD, profiles[1].snapshot)
        assertEquals(setOf("fr.tfou.max"), profiles[1].allowedApps)
    }

    @Test
    fun switchingKeepsTheActiveHomeAndLoadsTheTargetOne() {
        val paul = Profile("a", "Paul", ProfileKind.ADULT)
        val kids = Profile("b", "Enfants", ProfileKind.CHILD, snapshot = ProfileSnapshot.CHILD)
        val switched = ProfileSwitch.switch(current, listOf(paul, kids), activeId = "a", targetId = "b")!!
        assertEquals(ProfileSnapshot.of(current), switched.profiles.first { it.id == "a" }.snapshot)
        assertEquals(kids, switched.target)
    }

    @Test
    fun switchingKeepsANewNameAndColourGivenToTheActiveProfile() {
        val paul = Profile("a", "Paul", ProfileKind.ADULT, color = 0)
        val marie = Profile("b", "Marie", ProfileKind.ADULT)
        val renamed = current.copy(profileName = "Papy Paul", profileColor = 4)
        val kept = ProfileSwitch.switch(renamed, listOf(paul, marie), activeId = "a", targetId = "b")!!.profiles.first()
        assertEquals("Papy Paul", kept.name)
        assertEquals(4, kept.color)
    }

    @Test
    fun aBlankNameKeepsTheProfilesName() {
        val paul = Profile("a", "Paul", ProfileKind.ADULT)
        assertEquals("Paul", ProfileSwitch.kept(paul, current.copy(profileName = "  ")).name)
    }

    @Test
    fun switchingToAnUnknownOrTheSameProfileDoesNothing() {
        val paul = Profile("a", "Paul", ProfileKind.ADULT)
        assertNull(ProfileSwitch.switch(current, listOf(paul), activeId = "a", targetId = "zzz"))
        assertNull(ProfileSwitch.switch(current, listOf(paul), activeId = "a", targetId = "a"))
    }
}
