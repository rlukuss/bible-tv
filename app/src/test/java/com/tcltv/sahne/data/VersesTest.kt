package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VersesTest {
    @Test
    fun `never shows the same verse twice in a row`() {
        var previous = -1
        val random = Random(7)
        repeat(500) {
            val next = Verses.next(previous, random)
            assertNotEquals(previous, next)
            assertTrue(next in Verses.ALL.indices)
            previous = next
        }
    }

    @Test
    fun `reaches every verse`() {
        val random = Random(3)
        val seen = HashSet<Int>()
        var previous = -1
        repeat(5000) { previous = Verses.next(previous, random).also { seen += it } }
        assertEquals(Verses.ALL.size, seen.size)
    }

    @Test
    fun `every verse fits the top bar and names its reference`() {
        Verses.ALL.forEach {
            assertTrue(it.text, it.text.length <= 110)
            assertTrue(it.text, it.reference.isNotBlank())
        }
    }
}
