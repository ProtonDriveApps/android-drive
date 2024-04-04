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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.ComputerOptionsViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.drivelink.device.presentation.component.DeviceOptions
import me.proton.core.drive.drivelink.device.presentation.options.DeviceOptionEntry
import me.proton.core.drive.link.domain.entity.FolderId

@Composable
fun ComputerOptions(
    runAction: RunAction,
    navigateToRenameComputer: (deviceId: DeviceId, folderId: FolderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ComputerOptionsViewModel>()
    val viewModelDevice by rememberFlowWithLifecycle(viewModel.device).collectAsState(initial = null)
    viewModelDevice?.let { device ->
        val entries = viewModel.entries(
            runAction,
            navigateToRenameComputer,
        )
        ComputerOptions(
            device = device,
            entries = entries,
            modifier = modifier
                .navigationBarsPadding()
                .testTag(ComputerOptionsTestTag.computerOptions),
        )
    }
}

@Composable
fun ComputerOptions(
    device: Device,
    entries: List<DeviceOptionEntry>,
    modifier: Modifier = Modifier,
) {
    DeviceOptions(
        device = device,
        entries = entries,
        modifier = modifier,
    )
}

object ComputerOptionsTestTag {
    const val computerOptions = "computer options context menu"
}
