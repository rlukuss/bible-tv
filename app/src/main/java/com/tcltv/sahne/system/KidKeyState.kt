package com.tcltv.sahne.system

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * What the remote's filter needs from the app: whether a child is watching and whether Matinee is on screen. The
 * Home button service runs in its own process, so the app sends each change to it and keeps a copy in a small file
 * the service reads when it starts again.
 */
object KidKeyState {

    const val ACTION_CHANGED = "com.tcltv.sahne.action.KID_KEYS_CHANGED"
    const val EXTRA_CHILD = "child"
    const val EXTRA_IN_FRONT = "inFront"

    data class State(val isChild: Boolean = false, val matineeInFront: Boolean = false)

    @Volatile
    private var current: State? = null

    fun setChild(context: Context, isChild: Boolean) = publish(context) { it.copy(isChild = isChild) }

    fun setMatineeInFront(context: Context, inFront: Boolean) = publish(context) { it.copy(matineeInFront = inFront) }

    @Synchronized
    private fun publish(context: Context, change: (State) -> State) {
        val before = current ?: read(context)
        val after = change(before)
        current = after
        if (after == before) return
        runCatching { file(context).writeText("${after.isChild.flag()}${after.matineeInFront.flag()}") }
        context.sendBroadcast(
            Intent(ACTION_CHANGED)
                .setPackage(context.packageName)
                .putExtra(EXTRA_CHILD, after.isChild)
                .putExtra(EXTRA_IN_FRONT, after.matineeInFront),
        )
    }

    /** The last state the app wrote; a missing or unreadable file means an adult (nothing blocked). */
    fun read(context: Context): State {
        val text = runCatching { file(context).readText() }.getOrNull() ?: return State()
        return State(isChild = text.getOrNull(0) == '1', matineeInFront = text.getOrNull(1) == '1')
    }

    private fun Boolean.flag() = if (this) '1' else '0'

    private fun file(context: Context) = File(context.filesDir, "kid_keys")
}
