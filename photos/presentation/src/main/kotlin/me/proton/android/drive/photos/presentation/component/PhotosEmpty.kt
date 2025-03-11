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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import me.proton.android.drive.photos.presentation.R
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.drive.base.presentation.component.list.ListEmpty
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.user.presentation.quota.component.StorageBanner
import me.proton.core.drive.i18n.R as I18N

@Composable
fun PhotosEmpty(
    state: ListContentState.Empty,
    viewState: PhotosStatusViewState,
    showPhotosStateBanner: Boolean,
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
    onPermissions: () -> Unit,
    onRetry: () -> Unit,
    onResolve: () -> Unit,
    onGetStorage: () -> Unit,
    onResolveMissingFolder: () -> Unit,
    onChangeNetwork: () -> Unit,
    onIgnoreBackgroundRestrictions: () -> Unit,
    onDismissBackgroundRestrictions: () -> Unit,
) {
    Box(modifier = modifier) {
        if (viewState is PhotosStatusViewState.Preparing
            || viewState is PhotosStatusViewState.InProgress
        ) {
            BackupProgressPhotosEmpty(Modifier.fillMaxSize())
        } else {
            ListEmpty(
                state.imageResId,
                state.titleId,
                state.descriptionResId,
                state.actionResId,
                onAction = {}
            )
        }
        PhotosBanners(modifier = Modifier.align(Alignment.BottomCenter)) {
            PhotosStatesContainer(
                viewState = viewState,
                showPhotosStateBanner = showPhotosStateBanner,
                onEnable = onEnable,
                onPermissions = onPermissions,
                onRetry = onRetry,
                onResolve = onResolve,
                onResolveMissingFolder = onResolveMissingFolder,
                onChangeNetwork = onChangeNetwork,
                onIgnoreBackgroundRestrictions = onIgnoreBackgroundRestrictions,
                onDismissBackgroundRestrictions = onDismissBackgroundRestrictions,
            )
            StorageBanner(onGetStorage = onGetStorage)
        }
    }
}

@Composable
fun BackupProgressPhotosEmpty(
    modifier: Modifier = Modifier,
) {
    BackupInProgress { encrypting ->
        BackupProgressPhotosEmpty(modifier, encrypting)
    }
}


@Composable
fun BackupProgressPhotosEmpty(
    modifier: Modifier = Modifier,
    encrypting: Boolean,
) {
        ConstraintLayout(modifier) {

            val (progress, text) = createRefs()
            CircularProgressIndicator(
                Modifier.constrainAs(progress) {
                    centerTo(parent)
                }
            )

            Text(
                modifier = Modifier.constrainAs(text) {
                    top.linkTo(progress.bottom, ProtonDimens.SmallSpacing)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                text = stringResource(
                    if (encrypting) {
                        I18N.string.photos_empty_loading_label_encrypted
                    } else {
                        I18N.string.photos_empty_loading_label_progress
                    }
                ),
                style = ProtonTheme.typography.defaultWeak,
                textAlign = TextAlign.Center,
            )
        }
}

@Preview
@Composable
private fun PhotosEmptyPreview() {
    PhotosEmpty(
        state = ListContentState.Empty(
            imageResId = R.drawable.empty_photos_daynight,
            titleId = I18N.string.photos_empty_title,
            descriptionResId = I18N.string.photos_empty_description,
            actionResId = null,
        ),
        showPhotosStateBanner = true,
        viewState = PhotosStatusViewState.InProgress(0F, "12 345 items left"),
        onEnable = {},
        onPermissions = {},
        onRetry = { },
        onResolve = { },
        onGetStorage = {},
        onResolveMissingFolder = {},
        onChangeNetwork = {},
        onIgnoreBackgroundRestrictions = {},
        onDismissBackgroundRestrictions = {},
    )
}

@Preview
@Composable
private fun BackupProgressPhotosEmptyEncryptedPreview() {
    ProtonTheme {
        BackupProgressPhotosEmpty(Modifier.fillMaxSize(), true)
    }
}

@Preview
@Composable
private fun BackupProgressPhotosEmptyProgressPreview() {
    ProtonTheme {
        BackupProgressPhotosEmpty(Modifier.fillMaxSize(), false)
    }
}
