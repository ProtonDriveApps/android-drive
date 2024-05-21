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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import me.proton.android.drive.ui.viewmodel.ShareMemberOptionsViewModel
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.drivelink.shared.presentation.component.ShareUserOptions

@Composable
fun ShareMemberOptions(
    runAction: RunAction,
    modifier: Modifier = Modifier,
) = ShareMemberOptions(
    viewModel = hiltViewModel(),
    runAction = runAction,
    modifier = modifier
        .testTag(MemberOptionsDialogTestTag.contextMenu),
)

@Composable
fun ShareMemberOptions(
    viewModel: ShareMemberOptionsViewModel,
    runAction: RunAction,
    modifier: Modifier = Modifier,
) {
    val nullableMember by rememberFlowWithLifecycle(viewModel.viewState).collectAsState(initial = null)
    val member = nullableMember ?: return

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val entries by remember(viewModel, lifecycle) {
        viewModel.entries(
            runAction = runAction,
        ).flowWithLifecycle(
            lifecycle = lifecycle,
            minActiveState = Lifecycle.State.STARTED
        )
    }.collectAsState(initial = null)
    val memberEntries = entries ?: return
    ShareUserOptions(
        viewState = member,
        entries = memberEntries,
        modifier = modifier.navigationBarsPadding(),
    )
}

object MemberOptionsDialogTestTag {
    const val contextMenu = "member options context menu"
}

