/*
 * Copyright (c) 2021-2023 Proton AG.
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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsToggleItem
import me.proton.core.drive.settings.presentation.event.DebugSettingsViewEvent
import me.proton.core.drive.settings.presentation.state.DebugSettingsViewState
import me.proton.core.drive.i18n.R as I18N

@Immutable
data class DebugSettingsStateAndEvent(
    val viewState: DebugSettingsViewState,
    val viewEvent: DebugSettingsViewEvent,
)

@Composable
fun DebugSettings(
    viewState: DebugSettingsViewState,
    viewEvent: DebugSettingsViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        ProtonSettingsHeader(title = I18N.string.debug_settings_section)

        EditableSettingsEntry(
            label = I18N.string.debug_settings_host,
            value = viewState.host,
            onUpdate = viewEvent.onUpdateHost
        )
        EditableSettingsEntry(
            label = I18N.string.debug_settings_base_url,
            value = viewState.baseUrl,
            onUpdate = viewEvent.onUpdateBaseUrl
        )
        EditableSettingsEntry(
            label = I18N.string.debug_settings_app_version_header,
            value = viewState.appVersionHeader,
            onUpdate = viewEvent.onUpdateAppVersionHeader
        )
        ProtonSettingsToggleItem(
            name = stringResource(id = I18N.string.debug_settings_use_exception_message),
            hint = stringResource(id = I18N.string.debug_settings_use_exception_message_description),
            value = viewState.useExceptionMessage,
        ) { useExceptionMessage ->
            viewEvent.onToggleUseExceptionMessage(useExceptionMessage)
        }

        ProtonSettingsToggleItem(
            name = stringResource(id = I18N.string.debug_settings_log_to_file),
            hint = stringResource(id = I18N.string.debug_settings_log_to_file_description),
            value = viewState.logToFileEnabled,
        ) { logToFileEnabled ->
            viewEvent.onToggleLogToFileEnabled(logToFileEnabled)
        }

        val localContext = LocalContext.current
        ProtonSettingsItem(
            name = stringResource(id = I18N.string.debug_settings_send_log),
        ) {
            viewEvent.sendDebugLog(localContext)
        }

        ProtonSettingsItem(
            name = stringResource(id = I18N.string.debug_settings_reset),
            hint = stringResource(id = I18N.string.debug_settings_reset_description),
        ) {
            viewEvent.onReset
        }

    }
}
