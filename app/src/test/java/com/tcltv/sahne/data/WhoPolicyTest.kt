package com.tcltv.sahne.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoPolicyTest {

    @Test
    fun startingOrWakingAsksWhenAnAdultOrNobodyWasWatching() {
        listOf(WhoTrigger.COLD_START, WhoTrigger.SCREEN_ON).forEach { trigger ->
            assertTrue(WhoPolicy.shouldAsk(trigger, ProfileKind.ADULT, profileCount = 4))
            assertTrue(WhoPolicy.shouldAsk(trigger, active = null, profileCount = 4))
        }
    }

    @Test
    fun aChildStaysAChildAfterStartingOrWaking() {
        assertFalse(WhoPolicy.shouldAsk(WhoTrigger.COLD_START, ProfileKind.CHILD, profileCount = 4))
        assertFalse(WhoPolicy.shouldAsk(WhoTrigger.SCREEN_ON, ProfileKind.CHILD, profileCount = 4))
    }

    @Test
    fun theProfileMenuAlwaysAsksEvenForAChild() {
        assertTrue(WhoPolicy.shouldAsk(WhoTrigger.PROFILE_MENU, ProfileKind.CHILD, profileCount = 4))
        assertTrue(WhoPolicy.shouldAsk(WhoTrigger.PROFILE_MENU, ProfileKind.ADULT, profileCount = 2))
    }

    @Test
    fun aSingleProfileIsNeverAsked() {
        WhoTrigger.entries.forEach { assertFalse(WhoPolicy.shouldAsk(it, ProfileKind.ADULT, profileCount = 1)) }
    }

    @Test
    fun leavingAChildProfileNeedsThePin() {
        assertTrue(WhoPolicy.needsPin(from = ProfileKind.CHILD, to = ProfileKind.ADULT, sameProfile = false))
        assertTrue(WhoPolicy.needsPin(from = ProfileKind.CHILD, to = ProfileKind.CHILD, sameProfile = false))
        assertFalse(WhoPolicy.needsPin(from = ProfileKind.CHILD, to = ProfileKind.CHILD, sameProfile = true))
        assertFalse(WhoPolicy.needsPin(from = ProfileKind.ADULT, to = ProfileKind.CHILD, sameProfile = false))
        assertFalse(WhoPolicy.needsPin(from = null, to = ProfileKind.ADULT, sameProfile = false))
    }
}
