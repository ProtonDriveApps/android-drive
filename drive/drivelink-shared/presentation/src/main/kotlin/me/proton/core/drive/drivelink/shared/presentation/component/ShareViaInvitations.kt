/*
 * Copyright (c) 2022-2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState
import me.proton.core.compose.component.bottomsheet.rememberModalBottomSheetContentState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.ModalBottomSheet
import me.proton.core.drive.drivelink.shared.presentation.effect.SharedDriveInvitationsEffect
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.SharedDriveInvitationsViewModel
import me.proton.core.drive.link.domain.entity.LinkId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShareViaInvitations(
    navigateToDiscardChanges: (LinkId) -> Unit,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<SharedDriveInvitationsViewModel>()
    val viewEvent = viewModel.viewEvent(navigateToDiscardChanges, navigateBack)
    val saveButtonViewState by rememberFlowWithLifecycle(viewModel.saveButtonViewState)
        .collectAsState(initial = viewModel.initialSaveButtonViewState)
    val viewState by rememberFlowWithLifecycle(viewModel.viewState)
        .collectAsState(initial = null)
    Column(
        modifier = modifier.testTag(ShareViaInvitationsTestTag.content),
    ) {
        viewState?.let { viewState ->
            val modalBottomSheetContentState = rememberModalBottomSheetContentState()

            LaunchedEffect(viewModel, viewState, LocalContext.current) {
                viewModel.effect.onEach { effect ->
                    when (effect) {
                        SharedDriveInvitationsEffect.Permissions -> {
                            modalBottomSheetContentState.sheetContent.value = { runAction ->
                                PermissionsOptions(viewState.permissionsViewState) { permissions ->
                                    runAction {
                                        viewEvent.onPermissionsChanged(permissions)
                                    }
                                }
                            }
                            modalBottomSheetContentState.sheetState.show()
                        }
                    }
                }.launchIn(this)
            }

            ModalBottomSheet(
                sheetState = modalBottomSheetContentState.sheetState,
                sheetContent = modalBottomSheetContentState.sheetContent.value,
                viewState = remember { ModalBottomSheetViewState() },
            ) {
                SharedDriveInvitations(
                    viewState = viewState,
                    saveButtonViewState = saveButtonViewState,
                    viewEvent = viewEvent,
                )
            }
        }
    }
}

object ShareViaInvitationsTestTag {
    const val content = "share-via-invitations-content"
}
