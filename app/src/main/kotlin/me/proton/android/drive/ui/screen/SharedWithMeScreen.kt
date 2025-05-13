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

package me.proton.android.drive.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewmodel.SharedWithMeViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.drivelink.shared.presentation.component.Shared
import me.proton.core.drive.drivelink.shared.presentation.component.UserInvitationBanner
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId

@Composable
fun SharedWithMeScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToFiles: (FolderId, String?) -> Unit,
    navigateToPreview: (FileId) -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    navigateToUserInvitation: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SharedWithMeViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = viewModel.initialViewState)
    val userInvitationViewState by viewModel.userInvitationBannerViewState.collectAsStateWithLifecycle(
        initialValue = null
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateToFiles = navigateToFiles,
            navigateToPreview = navigateToPreview,
            navigateToAlbum = navigateToAlbum,
            navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
            navigateToUserInvitation = navigateToUserInvitation,
            lifecycle = lifecycle,
        )
    }
    viewModel.HandleHomeEffect(homeScaffoldState)
    val sharedItems = rememberFlowWithLifecycle(flow = viewModel.driveLinks)
    val listEffect = rememberFlowWithLifecycle(flow = viewModel.listEffect)
    Shared(
        viewState = viewState,
        viewEvent = viewEvent,
        sharedItems = sharedItems,
        listEffect = listEffect,
        driveLinksFlow = viewModel.driveLinksMap,
        modifier = modifier
            .testTag(SharedWithMeTestTag.content),
        headerContent = {
            Box(Modifier.defaultMinSize(minHeight = 1.dp)) {
                // minHeight: always draw to have the header visible in the lazy list
                AnimatedVisibility(
                    visible = userInvitationViewState != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    userInvitationViewState?.let { viewState ->
                        UserInvitationBanner(
                            description = viewState.description,
                            onClick = viewEvent.onUserInvitation,
                        )
                    }
                }
            }
        }
    )
}

object SharedWithMeTestTag {
    const val content = "files content"
}
