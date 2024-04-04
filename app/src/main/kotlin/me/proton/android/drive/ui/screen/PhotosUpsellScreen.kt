/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.photos.presentation.viewevent.PhotosUpsellViewEvent
import me.proton.android.drive.ui.viewmodel.PhotosUpsellViewModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun PhotosUpsellScreen(
    modifier: Modifier = Modifier,
    runAction: RunAction,
    navigateToSubscription: () -> Unit,
) {
    val viewModel = hiltViewModel<PhotosUpsellViewModel>()
    val viewEvent = remember {
        viewModel.viewEvent(
            runAction = runAction,
            navigateToSubscription = navigateToSubscription,
        )
    }
    DisposableEffect(Unit){
        onDispose {
            viewEvent.onDismiss()
        }
    }
    PhotosUpsell(
        viewEvent = viewEvent,
        modifier = modifier
            .systemBarsPadding(),
    )
}

@Composable
fun PhotosUpsell(
    viewEvent: PhotosUpsellViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IllustratedMessage(
            imageResId = getThemeDrawableId(
                light = BasePresentation.drawable.img_upsell_drive_light,
                dark = BasePresentation.drawable.img_upsell_drive_dark,
                dayNight = BasePresentation.drawable.img_upsell_drive_daynight,
            ),
            titleResId = I18N.string.photos_upsell_title,
            descriptionResId = I18N.string.photos_upsell_description,
        )
        val buttonModifier = Modifier
            .conditional(isPortrait) {
                fillMaxWidth()
            }
            .conditional(isLandscape) {
                widthIn(min = ButtonMinWidth)
            }
            .heightIn(min = ProtonDimens.ListItemHeight)
        ProtonSolidButton(
            onClick = { viewEvent.onMoreStorage() },
            modifier = buttonModifier,
        ) {
            Text(
                text = stringResource(id = I18N.string.photos_upsell_get_storage_action),
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }
        ProtonTextButton(
            onClick = { viewEvent.onCancel() },
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(id = I18N.string.photos_upsell_dismiss_action),
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }
    }
}

private val ButtonMinWidth = 300.dp

@Preview
@Preview(widthDp = 600, heightDp = 360)
@Composable
private fun PhotosUpsellPreview() {
    ProtonTheme {
        PhotosUpsell(
            viewEvent = object : PhotosUpsellViewEvent {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
