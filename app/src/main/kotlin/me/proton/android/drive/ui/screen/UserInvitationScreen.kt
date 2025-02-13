/*
 * Copyright (c) 2024-2025 Proton AG.
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.viewmodel.UserInvitationViewModel
import me.proton.core.drive.drivelink.shared.presentation.component.UserInvitation


@Composable
fun UserInvitationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val viewModel = hiltViewModel<UserInvitationViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(viewModel.initialViewState)
    val viewEvent = remember { viewModel.viewEvent(onBack) }
    val invitations by viewModel.userInvitations.collectAsStateWithLifecycle()
    UserInvitation(
        viewState = viewState,
        viewEvent = viewEvent,
        invitations = invitations.orEmpty(),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .testTag(UserInvitationTestFlag.content),
    )
}

object UserInvitationTestFlag {
    const val content = "user invitation content"
}
