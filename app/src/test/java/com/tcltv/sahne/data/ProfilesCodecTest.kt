package com.tcltv.sahne.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilesCodecTest {

    private val paul = Profile(
        id = "a",
        name = "Paul",
        kind = ProfileKind.ADULT,
        color = 3,
        snapshot = ProfileSnapshot(
            appOrder = listOf("com.netflix.ninja", "com.google.android.youtube.tv"),
            hiddenApps = setOf("org.videolan.vlc"),
            folders = listOf(Folder("f1", "Replay", listOf("fr.tf1.mytf1.free"))),
        ),
    )

    private val kids = Profile(
        id = "b",
        name = "Enfants",
        kind = ProfileKind.CHILD,
        color = 0,
        allowedApps = setOf("com.google.android.youtube.tvkids", "fr.tfou.max", "com.disney.disneyplus"),
        snapshot = ProfileSnapshot(showInputs = false, showAddTile = false, showLastPlayed = false),
    )

    @Test
    fun profilesSurviveARoundTrip() {
        assertEquals(listOf(paul, kids), ProfilesCodec.decode(ProfilesCodec.encode(listOf(paul, kids))))
    }

    @Test
    fun invalidJsonGivesNoProfiles() {
        assertEquals(emptyList<Profile>(), ProfilesCodec.decode("{oops"))
    }

    @Test
    fun blankOrMissingGivesNoProfiles() {
        assertEquals(emptyList<Profile>(), ProfilesCodec.decode(""))
        assertEquals(emptyList<Profile>(), ProfilesCodec.decode(null))
    }

    @Test
    fun missingFieldsTakeTheDefaults() {
        val profile = ProfilesCodec.decode("""[{"id":"c","name":"Léa","kind":"ADULT"}]""").single()
        assertEquals(ProfileSnapshot(), profile.snapshot)
        assertEquals(emptySet<String>(), profile.allowedApps)
        assertEquals(0, profile.color)
    }

    @Test
    fun aProfileOfAnUnknownKindIsSkipped() {
        val text = """[{"id":"d","name":"Rex","kind":"PET"},{"id":"e","name":"Marie","kind":"ADULT"}]"""
        assertEquals(listOf("Marie"), ProfilesCodec.decode(text).map { it.name })
    }

    @Test
    fun aProfileWithoutIdOrNameIsSkipped() {
        val text = """[{"name":"Sans id","kind":"ADULT"},{"id":"x","name":" ","kind":"ADULT"}]"""
        assertEquals(emptyList<Profile>(), ProfilesCodec.decode(text))
    }
}
