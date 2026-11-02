package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileEditsTest {

    private val paul = Profile("r", "Paul", ProfileKind.ADULT, color = 0)
    private val kids = Profile(
        "k", "Enfants", ProfileKind.CHILD, color = 3,
        allowedApps = setOf("fr.tfou.max"), snapshot = ProfileSnapshot.CHILD,
    )
    private val family = listOf(paul, kids)

    @Test
    fun aNewProfileIsAnAdultWithTheNextColour() {
        val added = ProfileEdits.add(family, id = "n", name = "Léa")
        assertEquals(3, added.size)
        assertEquals(Profile("n", "Léa", ProfileKind.ADULT, color = 2), added.last())
    }

    @Test
    fun aBlankNameIsNotSaved() {
        assertEquals(family, ProfileEdits.rename(family, "r", "   "))
        assertEquals("Papy", ProfileEdits.rename(family, "r", "  Papy ").first().name)
    }

    @Test
    fun becomingAChildGivesTheChildHome() {
        val child = ProfileEdits.setKind(family, "r", ProfileKind.CHILD).first()
        assertEquals(ProfileKind.CHILD, child.kind)
        assertEquals(ProfileSnapshot.CHILD, child.snapshot)
    }

    @Test
    fun becomingAnAdultGivesAFullHomeAndForgetsTheAllowedApps() {
        val adult = ProfileEdits.setKind(family, "k", ProfileKind.ADULT).last()
        assertEquals(ProfileSnapshot(), adult.snapshot)
        assertEquals(emptySet<String>(), adult.allowedApps)
    }

    @Test
    fun keepingTheSameKindChangesNothing() {
        assertEquals(family, ProfileEdits.setKind(family, "k", ProfileKind.CHILD))
    }

    @Test
    fun allowedAppsToggle() {
        val once = ProfileEdits.toggleAllowed(family, "k", "com.disney.disneyplus")
        assertEquals(setOf("fr.tfou.max", "com.disney.disneyplus"), once.last().allowedApps)
        val twice = ProfileEdits.toggleAllowed(once, "k", "com.disney.disneyplus")
        assertEquals(setOf("fr.tfou.max"), twice.last().allowedApps)
    }

    @Test
    fun theColourIsSet() {
        assertEquals(5, ProfileEdits.setColor(family, "r", 5).first().color)
    }

    @Test
    fun theWatchingProfileAndTheLastOneCannotBeDeleted() {
        assertNull(ProfileEdits.delete(family, id = "r", activeId = "r"))
        assertNull(ProfileEdits.delete(listOf(paul), id = "r", activeId = null))
        assertEquals(listOf(paul), ProfileEdits.delete(family, id = "k", activeId = "r"))
    }

    @Test
    fun anUnknownProfileChangesNothing() {
        assertEquals(family, ProfileEdits.rename(family, "zzz", "X"))
        assertNull(ProfileEdits.delete(family, id = "zzz", activeId = "r"))
    }
}
