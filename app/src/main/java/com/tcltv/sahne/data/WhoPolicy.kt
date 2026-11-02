package com.tcltv.sahne.data

/** What brought up the question "Who's watching?". */
enum class WhoTrigger { COLD_START, SCREEN_ON, PROFILE_MENU }

object WhoPolicy {

    /**
     * Starting the TV or waking it from standby asks again, unless a child was watching: a child stays in their own
     * home until a parent changes it. Picking "Change profile" always asks; the PIN is checked on the way out.
     */
    fun shouldAsk(trigger: WhoTrigger, active: ProfileKind?, profileCount: Int): Boolean = when {
        profileCount < 2 -> false
        trigger == WhoTrigger.PROFILE_MENU -> true
        else -> active != ProfileKind.CHILD
    }

    /** Leaving a child's profile for anyone else needs the parental PIN. */
    fun needsPin(from: ProfileKind?, to: ProfileKind, sameProfile: Boolean): Boolean =
        from == ProfileKind.CHILD && !sameProfile
}
