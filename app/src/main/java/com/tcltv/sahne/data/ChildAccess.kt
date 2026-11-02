package com.tcltv.sahne.data

/**
 * What a child's profile may open on its own: the apps a parent allowed. Everything else on the TV (live TV, inputs,
 * the Play Store's notices, system settings, app info, editing the home) waits for the parental PIN.
 */
object ChildAccess {

    fun allows(profile: Profile?, packageName: String): Boolean =
        profile?.kind != ProfileKind.CHILD || packageName in profile.allowedApps

    /** [items] in their order, without what [profile] may not open (folder cards included, as they hold other apps). */
    fun <T> visible(items: List<T>, profile: Profile?, packageOf: (T) -> String): List<T> =
        if (profile?.kind != ProfileKind.CHILD) items else items.filter { allows(profile, packageOf(it)) }

    fun needsGrownUp(profile: Profile?): Boolean = profile?.kind == ProfileKind.CHILD
}
