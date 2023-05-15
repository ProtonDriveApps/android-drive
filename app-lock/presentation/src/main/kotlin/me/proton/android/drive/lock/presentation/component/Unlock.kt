/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.lock.presentation.component

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import me.proton.android.drive.lock.presentation.R
import me.proton.android.drive.lock.presentation.viewevent.UnlockViewEvent
import me.proton.android.drive.lock.presentation.viewmodel.UnlockViewModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun Unlock(
    userId: UserId,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<UnlockViewModel>()
    Unlock(
        userId = userId,
        viewEvent = viewModel.viewEvent,
        modifier = modifier,
    )
}

@Composable
fun Unlock(
    userId: UserId,
    viewEvent: UnlockViewEvent,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewEvent.onShowBiometric(userId)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .conditional(isPortrait) {
                navigationBarsPadding()
            }
    ) {
        LogoHeader(
            modifier = Modifier
                .weight(LogoHeaderWeight)
        )
        Actions(
            modifier = Modifier
                .weight(1f - LogoHeaderWeight),
            onUnlock = { viewEvent.onShowBiometric(userId) },
            onSignOut = { viewEvent.onSignOut(userId) },
        )
    }

}

@Composable
private fun LogoHeader(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Image(
            painter = rememberDrawablePainter(
                drawable = getDrawable(
                    light = R.drawable.welcome_header_light,
                    dark = R.drawable.welcome_header_dark,
                    dayNight = R.drawable.welcome_header,
                )
            ),
            contentDescription = null,
            contentScale = ContentScale.None,
        )
    }
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Image(
            painter = rememberDrawablePainter(
                drawable = getDrawable(
                    light = CorePresentation.drawable.logo_drive_with_text_light,
                    dark = CorePresentation.drawable.logo_drive_with_text_dark,
                    dayNight = BasePresentation.drawable.logo_drive_with_text_daylight,
                )
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(top = DriveLogoTopPadding)
                .align(Alignment.TopCenter)
        )
        val buttonModifier = Modifier
            .conditional(isPortrait) {
                fillMaxWidth()
            }
            .conditional(isLandscape) {
                widthIn(min = ButtonMinWidth)
            }
            .heightIn(min = ListItemHeight)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = MediumSpacing)
                .align(Alignment.BottomCenter)
        ) {
            ProtonSolidButton(
                onClick = onUnlock,
                modifier = buttonModifier,
            ) {
                Text(text = stringResource(id = I18N.string.app_lock_unlock_the_app))
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SmallSpacing)
            )
            ProtonTextButton(
                onClick = onSignOut,
                modifier = buttonModifier,
            ) {
                Text(text = stringResource(id = I18N.string.common_sign_out))
            }
        }
    }
}

@Composable
private fun getDrawable(@DrawableRes light: Int, @DrawableRes dark: Int, @DrawableRes dayNight: Int): Drawable? =
    AppCompatResources.getDrawable(
        LocalContext.current,
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> dark
            AppCompatDelegate.MODE_NIGHT_NO -> light
            else -> dayNight
        }
    )

@Preview
@Composable
private fun UnlockPreview() {
    ProtonTheme {
        Unlock(
            userId = UserId("123"),
            viewEvent = object : UnlockViewEvent {
                override val onShowBiometric: (UserId) -> Unit = {}
                override val onSignOut: (UserId) -> Unit = {}
            }
        )
    }
}

private val LogoHeaderWeight: Float @Composable get() = if (isPortrait) 0.44f else 0.3f
private val ButtonMinWidth = 300.dp
private val DriveLogoTopPadding = 24.dp
