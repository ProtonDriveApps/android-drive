/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.BackupPermissions
import me.proton.android.drive.photos.presentation.component.LibraryFolders
import me.proton.android.drive.ui.action.PhotoExtractDataAction
import me.proton.android.drive.ui.screen.PhotosBackup
import me.proton.android.drive.ui.viewmodel.OnboardingViewModel
import me.proton.android.drive.ui.viewmodel.PhotosBackupViewModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.base.presentation.component.Onboarding as BaseOnboarding

@Composable
fun Onboarding(
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<OnboardingViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = null)
    val viewEvent = remember(dismiss, viewModel) { viewModel.viewEvent(dismiss) }
    val onboardingViewState = viewState ?: return

    Box(modifier = modifier
        .fillMaxSize()
        .conditional(isPortrait) {
            navigationBarsPadding()
        }
        .testTag(OnboardingTestTag.screen)
    ) {
        AnimatedVisibility(
            visible = !onboardingViewState.isPhotoBackupVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
        ) {
            BaseOnboarding(
                viewState = onboardingViewState,
                viewEvent = viewEvent,
                modifier = Modifier
                    .testTag(OnboardingTestTag.main)
            )
        }
        AnimatedVisibility(
            visible = onboardingViewState.isPhotoBackupVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            OnboardingPhotoBackup(
                doneButtonTitle = onboardingViewState.doneButtonTitle,
                navigateBack = viewEvent.onBack,
                navigateToConfirmStopSyncFolder = { _, _ -> },
                onDone = viewEvent.onDone,
                modifier = Modifier
                    .testTag(OnboardingTestTag.photoBackup)
            )
        }
    }

    BackupPermissions(
        viewState = viewModel.backupPermissionsViewState,
        viewEvent = viewModel.backupPermissionsViewEvent,
        navigateToNotificationPermissionRationale = {},
    )
}

@Composable
fun OnboardingPhotoBackup(
    doneButtonTitle: String,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    navigateToConfirmStopSyncFolder: (FolderId, Int) -> Unit,
    onDone: () -> Unit,
) {
    val viewModel = hiltViewModel<PhotosBackupViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent()
    }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        PhotosBackup(
            viewState = viewState,
            viewEvent = viewEvent,
            modifier = Modifier
                .weight(1f),
            navigateBack = navigateBack,
        ) {
            PhotoExtractDataAction()
            LibraryFolders(
                modifier = Modifier.fillMaxSize(),
                navigateToConfirmStopSyncFolder = navigateToConfirmStopSyncFolder,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val buttonModifier = Modifier
                .padding(all = MediumSpacing)
                .conditional(isPortrait) {
                    fillMaxWidth()
                }
                .conditional(isLandscape) {
                    widthIn(min = ButtonMinWidth)
                }
                .heightIn(min = ListItemHeight)
            ProtonSolidButton(
                modifier = buttonModifier,
                onClick = onDone,
            ) {
                Text(
                    text = doneButtonTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val ButtonMinWidth = 300.dp

object OnboardingTestTag {
    const val screen = "onboarding screen"
    const val main = "main onboarding screen"
    const val photoBackup = "photo backup onboarding screen"
}
