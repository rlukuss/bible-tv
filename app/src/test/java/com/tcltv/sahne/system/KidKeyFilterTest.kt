package com.tcltv.sahne.system

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidKeyFilterTest {

    private val everyday = listOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
    )

    @Test
    fun adultsKeepEveryKey() {
        listOf(KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_ASSIST, 288)
            .forEach { assertFalse("key $it", KidKeyFilter.blocks(it, isChild = false, matineeInFront = false)) }
    }

    @Test
    fun aChildKeepsTheKeysNeededToWatch() {
        everyday.forEach { assertFalse("key $it", KidKeyFilter.blocks(it, isChild = true, matineeInFront = false)) }
    }

    @Test
    fun aChildLosesChannelsAssistantAndTheBrandButtons() {
        // 288 stands for a TV maker's own shortcut key, such as the Free remote's app buttons.
        listOf(KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_TV, KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_SETTINGS, 288)
            .forEach { assertTrue("key $it", KidKeyFilter.blocks(it, isChild = true, matineeInFront = false)) }
    }

    @Test
    fun digitsOnlyWorkInMatineeForAChild() {
        assertTrue(KidKeyFilter.blocks(KeyEvent.KEYCODE_7, isChild = true, matineeInFront = false))
        assertFalse(KidKeyFilter.blocks(KeyEvent.KEYCODE_7, isChild = true, matineeInFront = true))
        assertFalse(KidKeyFilter.blocks(KeyEvent.KEYCODE_DEL, isChild = true, matineeInFront = true))
    }

    @Test
    fun matineeInFrontDoesNotOpenTheOtherKeys() {
        assertTrue(KidKeyFilter.blocks(KeyEvent.KEYCODE_CHANNEL_UP, isChild = true, matineeInFront = true))
    }
}
