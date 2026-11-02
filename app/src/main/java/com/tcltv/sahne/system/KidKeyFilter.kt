package com.tcltv.sahne.system

import android.view.KeyEvent

/**
 * The remote in a child's profile: only the keys needed to move around, watch and stop work. Everything else, such as
 * channel keys, the assistant and a TV maker's own app buttons (the Free remote has several), does nothing. Listing
 * what is kept rather than what is blocked also covers buttons whose codes differ from one remote to another.
 */
object KidKeyFilter {

    private val KEPT = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND,
    )

    /** Typing the parental PIN in Matinee needs the digits (and deleting one). */
    private val PIN_KEYS = (KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9).toSet() + KeyEvent.KEYCODE_DEL

    fun blocks(keyCode: Int, isChild: Boolean, matineeInFront: Boolean): Boolean = when {
        !isChild -> false
        keyCode in KEPT -> false
        matineeInFront && keyCode in PIN_KEYS -> false
        else -> true
    }
}
