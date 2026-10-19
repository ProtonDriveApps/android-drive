/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.settings.presentation.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.viewmodel.hiltViewModelOrNull
import me.proton.core.drive.base.presentation.component.ProtonSettingsToggleItem
import me.proton.core.usersettings.presentation.compose.R
import me.proton.core.usersettings.presentation.compose.viewmodel.UserSettingsViewModel
import me.proton.core.usersettings.presentation.compose.viewmodel.UserSettingsViewModel.Action
import me.proton.core.usersettings.presentation.compose.viewmodel.UserSettingsViewModel.State

@Composable
fun CrashReportSettingToggleItem(
    modifier: Modifier = Modifier,
    viewModel: UserSettingsViewModel? = hiltViewModelOrNull(),
    divider: @Composable () -> Unit = { HorizontalDivider() },
) {
    val state = when (viewModel) {
        null -> State()
        else -> rememberAsState(viewModel.state, viewModel.initialState).value
    }
    CrashReportSettingToggleItem(
        modifier = modifier,
        isEnabled = state.crashReports,
        onToggle = { viewModel?.perform(Action.ToggleCrashReport) },
        divider = { divider() }
    )
}

@Composable
fun CrashReportSettingToggleItem(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
    divider: @Composable () -> Unit = { HorizontalDivider() },
) {
    ProtonSettingsToggleItem(
        modifier = modifier,
        name = stringResource(id = R.string.device_settings_crashreport_title),
        hint = stringResource(id = R.string.device_settings_crashreport_hint),
        value = isEnabled,
        onToggle = { onToggle(!it) }
    )
    divider()
}
