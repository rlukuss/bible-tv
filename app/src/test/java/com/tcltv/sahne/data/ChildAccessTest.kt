package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildAccessTest {

    private val kids = Profile(
        id = "k",
        name = "Enfants",
        kind = ProfileKind.CHILD,
        allowedApps = setOf("com.google.android.youtube.tvkids", "fr.tfou.max", "com.disney.disneyplus"),
    )
    private val paul = Profile(id = "r", name = "Paul", kind = ProfileKind.ADULT)

    @Test
    fun aChildOnlyGetsTheAllowedApps() {
        assertTrue(ChildAccess.allows(kids, "fr.tfou.max"))
        assertFalse(ChildAccess.allows(kids, "com.netflix.ninja"))
        assertFalse(ChildAccess.allows(kids, "com.android.vending"))
    }

    @Test
    fun adultsAndAHomeWithoutProfilesGetEverything() {
        assertTrue(ChildAccess.allows(paul, "com.netflix.ninja"))
        assertTrue(ChildAccess.allows(null, "com.netflix.ninja"))
    }

    @Test
    fun aChildWithNoAllowedAppGetsNone() {
        assertFalse(ChildAccess.allows(kids.copy(allowedApps = emptySet()), "fr.tfou.max"))
    }

    @Test
    fun theListKeepsItsOrderAndDropsTheRest() {
        val apps = listOf("com.netflix.ninja", "com.disney.disneyplus", "folder:1", "fr.tfou.max")
        assertEquals(listOf("com.disney.disneyplus", "fr.tfou.max"), ChildAccess.visible(apps, kids) { it })
        assertEquals(apps, ChildAccess.visible(apps, paul) { it })
    }

    @Test
    fun onlyAChildNeedsAGrownUpForTheRestOfTheTv() {
        assertTrue(ChildAccess.needsGrownUp(kids))
        assertFalse(ChildAccess.needsGrownUp(paul))
        assertFalse(ChildAccess.needsGrownUp(null))
    }
}
