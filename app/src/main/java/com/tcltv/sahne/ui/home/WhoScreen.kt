package com.tcltv.sahne.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcltv.sahne.LauncherViewModel
import com.tcltv.sahne.R
import com.tcltv.sahne.data.Profile
import com.tcltv.sahne.data.ProfileKind
import com.tcltv.sahne.ui.LocalReduceMotion
import com.tcltv.sahne.ui.Palette
import com.tcltv.sahne.ui.TintedIcon
import com.tcltv.sahne.ui.interStyle

/** "Who's watching?": one round avatar per profile; OK picks it (with the PIN when leaving a child's profile). */
@Composable
fun WhoScreen(viewModel: LauncherViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profiles = viewModel.profilesForWho(settings)
    val firstFocus = remember { FocusRequester() }
    val focusId = settings.activeProfileId ?: profiles.firstOrNull()?.id
    LaunchedEffect(focusId, profiles.size) { runCatching { firstFocus.requestFocus() } }
    BackHandler { viewModel.dismissWho() }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(stringResource(R.string.who_title), style = interStyle(34.sp, 800))
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            profiles.forEach { profile ->
                WhoAvatar(
                    profile = profile,
                    modifier = if (profile.id == focusId) Modifier.focusRequester(firstFocus) else Modifier,
                    onClick = { viewModel.pickProfile(profile.id) },
                )
            }
        }
    }
}

@Composable
private fun WhoAvatar(profile: Profile, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(if (focused && !reduceMotion) 1.1f else 1f, label = "who-scale")
    val fill = profileColor(profile.color)
    val content = if (fill.luminance() > 0.45f) Color(0xFF2A1A02) else Color.White
    Column(
        modifier
            .width(132.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .scale(scale)
                .size(112.dp)
                .border(4.dp, if (focused) Color.White else Color.Transparent, CircleShape)
                .background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(initialsOf(profile.name), style = interStyle(40.sp, 700, content))
            if (profile.kind == ProfileKind.CHILD) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(30.dp).background(Palette.TextPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { TintedIcon(R.drawable.ic_lock, 16.dp, Color(0xFF2A1A02)) }
            }
        }
        Spacer(Modifier.height(14.dp))
        BasicText(
            profile.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = interStyle(17.sp, if (focused) 700 else 500, if (focused) Palette.TextPrimary else Palette.TextSecondary),
        )
    }
}
