package com.tcltv.sahne.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcltv.sahne.LauncherViewModel
import com.tcltv.sahne.R
import com.tcltv.sahne.data.Profile
import com.tcltv.sahne.data.ProfileKind
import com.tcltv.sahne.ui.TextInput
import com.tcltv.sahne.ui.home.profileColorCount

/** Everyone who watches. Opening a profile, adding one and deleting one each ask for the PIN. */
@Composable
internal fun ProfilesPage(viewModel: LauncherViewModel, firstRow: FocusRequester, openProfile: (String) -> Unit) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val profiles = viewModel.profilesForWho(s)
    val newName = stringResource(R.string.profile_new_name)
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    PanelList {
        if (!s.parental.hasPin && profiles.any { it.kind == ProfileKind.CHILD }) Note(R.string.note_profiles_need_pin)
        profiles.forEachIndexed { index, profile ->
            ValueRow(
                label = profile.name,
                subtitle = if (profile.id == s.activeProfileId) stringResource(R.string.profile_watching) else null,
                value = stringResource(kindLabel(profile.kind)),
                modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
            ) { viewModel.withProfilePin { openProfile(profile.id) } }
        }
        ValueRow(stringResource(R.string.profile_add), null, null) { viewModel.addProfile(newName, openProfile) }
    }
}

/** One profile's first name, type, colour and, for a child, the apps it may open. */
@Composable
internal fun ProfileEditorPage(
    viewModel: LauncherViewModel,
    profileId: String,
    firstRow: FocusRequester,
    rename: () -> Unit,
    chooseApps: () -> Unit,
    onDeleted: () -> Unit,
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val profile = viewModel.profilesForWho(s).firstOrNull { it.id == profileId } ?: return
    var confirmDelete by remember { mutableStateOf(false) }
    val deletable = profile.id != s.activeProfileId && s.profiles.size > 1
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    PanelList {
        ValueRow(stringResource(R.string.profile_first_name), null, profile.name, Modifier.focusRequester(firstRow)) { rename() }
        ValueRow(stringResource(R.string.profile_kind), null, stringResource(kindLabel(profile.kind))) {
            viewModel.setProfileKind(profile.id, if (profile.kind == ProfileKind.ADULT) ProfileKind.CHILD else ProfileKind.ADULT)
        }
        ValueRow(stringResource(R.string.setting_profile_color), null, stringResource(R.string.color_number, profile.color.mod(profileColorCount) + 1)) {
            viewModel.setProfileColor(profile.id, (profile.color + 1).mod(profileColorCount))
        }
        if (profile.kind == ProfileKind.CHILD) {
            if (!viewModel.isHomeKeyServiceRunning()) Note(R.string.note_child_keys_off)
            ValueRow(
                stringResource(R.string.profile_allowed_apps),
                null,
                stringResource(R.string.profile_allowed_count, profile.allowedApps.size),
            ) { chooseApps() }
        }
        if (deletable) {
            ValueRow(
                stringResource(R.string.profile_delete),
                null,
                if (confirmDelete) stringResource(R.string.profile_delete_confirm) else null,
            ) {
                if (confirmDelete) viewModel.deleteProfile(profile.id, onDeleted) else confirmDelete = true
            }
        } else {
            Note(R.string.note_profile_kept)
        }
    }
}

@Composable
internal fun ProfileRenamePage(viewModel: LauncherViewModel, profileId: String, firstRow: FocusRequester) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val profile: Profile = viewModel.profilesForWho(s).firstOrNull { it.id == profileId } ?: return
    var name by remember { mutableStateOf(profile.name) }
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    PanelList {
        TextInput(value = name, placeholder = stringResource(R.string.profile_name_placeholder), modifier = Modifier.focusRequester(firstRow)) {
            name = it
            viewModel.renameProfile(profileId, it)
        }
    }
}

/** Every app on the TV, ticked when this child may see and open it. */
@Composable
internal fun ProfileAppsPage(viewModel: LauncherViewModel, profileId: String, firstRow: FocusRequester) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.appsForSettings.collectAsStateWithLifecycle()
    val profile = s.profiles.firstOrNull { it.id == profileId } ?: return
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    PanelList {
        Note(R.string.note_child_apps)
        apps.forEachIndexed { index, app ->
            ToggleRow(
                label = app.label,
                subtitle = null,
                checked = app.packageName in profile.allowedApps,
                modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
            ) { viewModel.toggleProfileApp(profileId, app.packageName) }
        }
    }
}

private fun kindLabel(kind: ProfileKind) = if (kind == ProfileKind.CHILD) R.string.profile_kind_child else R.string.profile_kind_adult

/** The name a profile's page is titled with, as it reads now. */
@Composable
internal fun profileName(viewModel: LauncherViewModel, profileId: String): String {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    return viewModel.profilesForWho(s).firstOrNull { it.id == profileId }?.name ?: stringResource(R.string.setting_profiles)
}
