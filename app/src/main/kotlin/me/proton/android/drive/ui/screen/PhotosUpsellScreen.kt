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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.component.PromoContainer
import me.proton.android.drive.ui.viewmodel.PhotosUpsellViewModel
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.drive.base.presentation.R as BasePresentation

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
        modifier = modifier,
        onMoreStorage = { viewEvent.onMoreStorage() },
        onCancel = { viewEvent.onCancel() },
    )
}

@Composable
private fun PhotosUpsell(
    modifier: Modifier,
    onMoreStorage: () -> Unit,
    onCancel: () -> Unit
) {
    PromoContainer(
        modifier = modifier.systemBarsPadding(),
        titleResId = I18N.string.photos_upsell_title,
        descriptionResId = I18N.string.photos_upsell_description,
        imageResId = getThemeDrawableId(
            light = BasePresentation.drawable.img_upsell_drive_light,
            dark = BasePresentation.drawable.img_upsell_drive_dark,
            dayNight = BasePresentation.drawable.img_upsell_drive_daynight,
        ),
        actionText = stringResource(id = I18N.string.photos_upsell_get_storage_action),
        dismissActionText = stringResource(id = I18N.string.photos_upsell_dismiss_action),
        onAction = { onMoreStorage() },
        onCancel = { onCancel() },
    )
}

@Preview
@Preview(widthDp = 600, heightDp = 360)
@Composable
private fun PhotosUpsellPreview() {
    ProtonTheme {
        PhotosUpsell(
            modifier = Modifier.fillMaxSize(),
            onMoreStorage = {},
            onCancel = {},
        )
    }
}
