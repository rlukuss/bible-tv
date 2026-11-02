package com.tcltv.sahne.data

/** Changes made to the profiles in the settings. Each returns the whole new list; the store saves it in one write. */
object ProfileEdits {

    fun add(profiles: List<Profile>, id: String, name: String): List<Profile> =
        profiles + Profile(id = id, name = name.trim(), kind = ProfileKind.ADULT, color = profiles.size)

    fun rename(profiles: List<Profile>, id: String, name: String): List<Profile> {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) profiles else profiles.change(id) { it.copy(name = trimmed) }
    }

    /** A child starts from the reduced home; an adult from the full one, and forgets which apps a child had. */
    fun setKind(profiles: List<Profile>, id: String, kind: ProfileKind): List<Profile> = profiles.change(id) {
        when {
            it.kind == kind -> it
            kind == ProfileKind.CHILD -> it.copy(kind = kind, snapshot = ProfileSnapshot.CHILD)
            else -> it.copy(kind = kind, snapshot = ProfileSnapshot(), allowedApps = emptySet())
        }
    }

    fun setColor(profiles: List<Profile>, id: String, color: Int): List<Profile> = profiles.change(id) { it.copy(color = color) }

    fun toggleAllowed(profiles: List<Profile>, id: String, packageName: String): List<Profile> = profiles.change(id) {
        it.copy(allowedApps = if (packageName in it.allowedApps) it.allowedApps - packageName else it.allowedApps + packageName)
    }

    /** Null when [id] is the profile watching now, the last one left, or unknown. */
    fun delete(profiles: List<Profile>, id: String, activeId: String?): List<Profile>? = when {
        id == activeId || profiles.size < 2 || profiles.none { it.id == id } -> null
        else -> profiles.filterNot { it.id == id }
    }

    private fun List<Profile>.change(id: String, block: (Profile) -> Profile) = map { if (it.id == id) block(it) else it }
}
