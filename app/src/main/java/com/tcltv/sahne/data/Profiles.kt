package com.tcltv.sahne.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Who a profile is for: a child's home only shows the apps a parent allowed, and leaving it needs the PIN. */
enum class ProfileKind { ADULT, CHILD }

/**
 * The part of the settings that makes up one person's home screen. The active profile's copy lives in the usual
 * settings keys, so the home screen reads them as before; switching profiles swaps these values in one write.
 */
data class ProfileSnapshot(
    val appOrder: List<String> = emptyList(),
    val hiddenApps: Set<String> = emptySet(),
    val shownApps: Set<String> = emptySet(),
    val folders: List<Folder> = emptyList(),
    val showInputs: Boolean = true,
    val showAddTile: Boolean = true,
    val showLastPlayed: Boolean = true,
) {
    companion object {
        fun of(settings: LauncherSettings) = ProfileSnapshot(
            appOrder = settings.appOrder,
            hiddenApps = settings.hiddenApps,
            shownApps = settings.shownApps,
            folders = settings.folders,
            showInputs = settings.showInputs,
            showAddTile = settings.showAddTile,
            showLastPlayed = settings.showLastPlayed,
        )

        /** A child's home: no live TV or inputs, no "Add app" tile and no last played from the grown-ups' apps. */
        val CHILD = ProfileSnapshot(showInputs = false, showAddTile = false, showLastPlayed = false)
    }
}

data class Profile(
    val id: String,
    val name: String,
    val kind: ProfileKind,
    /** An index into the avatar colours, as the single-profile setting used. */
    val color: Int = 0,
    /** For a child: the only apps shown and opened. Unused for adults. */
    val allowedApps: Set<String> = emptySet(),
    val snapshot: ProfileSnapshot = ProfileSnapshot(),
)

internal object ProfilesCodec {

    fun encode(profiles: List<Profile>): String = JSONArray().apply {
        profiles.forEach { p ->
            put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("kind", p.kind.name)
                    .put("color", p.color)
                    .put("allowed", JSONArray(p.allowedApps.sorted()))
                    .put("home", encodeSnapshot(p.snapshot)),
            )
        }
    }.toString()

    /** Anything unreadable gives no profiles rather than a crash: the store then starts over from the settings. */
    fun decode(text: String?): List<Profile> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(::decodeProfile) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun decodeProfile(o: JSONObject): Profile? {
        val id = o.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = o.optString("name").trim().takeIf { it.isNotEmpty() } ?: return null
        val kind = ProfileKind.entries.firstOrNull { it.name == o.optString("kind") } ?: return null
        return Profile(
            id = id,
            name = name,
            kind = kind,
            color = o.optInt("color", 0),
            allowedApps = o.optJSONArray("allowed").strings().toSet(),
            snapshot = o.optJSONObject("home")?.let(::decodeSnapshot) ?: ProfileSnapshot(),
        )
    }

    private fun encodeSnapshot(s: ProfileSnapshot) = JSONObject()
        .put("order", JSONArray(s.appOrder))
        .put("hidden", JSONArray(s.hiddenApps.sorted()))
        .put("shown", JSONArray(s.shownApps.sorted()))
        .put("folders", JSONArray(CustomizationJson.encodeFolders(s.folders)))
        .put("inputs", s.showInputs)
        .put("addTile", s.showAddTile)
        .put("lastPlayed", s.showLastPlayed)

    private fun decodeSnapshot(o: JSONObject): ProfileSnapshot {
        val defaults = ProfileSnapshot()
        return ProfileSnapshot(
            appOrder = o.optJSONArray("order").strings(),
            hiddenApps = o.optJSONArray("hidden").strings().toSet(),
            shownApps = o.optJSONArray("shown").strings().toSet(),
            folders = CustomizationJson.decodeFolders(o.optJSONArray("folders")?.toString()),
            showInputs = o.optBoolean("inputs", defaults.showInputs),
            showAddTile = o.optBoolean("addTile", defaults.showAddTile),
            showLastPlayed = o.optBoolean("lastPlayed", defaults.showLastPlayed),
        )
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
}
