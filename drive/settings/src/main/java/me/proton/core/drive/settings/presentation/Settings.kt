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

package me.proton.core.drive.settings.presentation

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.NavigationDrawerAppVersion
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.settings.R
import me.proton.core.drive.settings.presentation.component.DebugSettings
import me.proton.core.drive.settings.presentation.component.ExternalSettingsEntry
import me.proton.core.drive.settings.presentation.component.ThemeChooserDialog
import me.proton.core.drive.settings.presentation.event.SettingsViewEvent
import me.proton.core.drive.settings.presentation.extension.toString
import me.proton.core.drive.settings.presentation.state.LegalLink
import me.proton.core.drive.settings.presentation.state.SettingsViewState
import me.proton.core.usersettings.presentation.compose.view.CrashReportSettingToggleItem
import me.proton.core.usersettings.presentation.compose.view.TelemetrySettingToggleItem
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
fun Settings(
    viewState: SettingsViewState,
    viewEvent: SettingsViewEvent,
    modifier: Modifier = Modifier,
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    if (showThemeDialog) {
        ThemeChooserDialog(
            selectedStyle = viewState.currentStyle,
            availableStyles = viewState.availableStyles,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { newStyle ->
                viewEvent.onThemeStyleChanged(newStyle)
                showThemeDialog = false
            }
        )
    }

    Column(modifier) {
        TopAppBar(
            navigationIcon = painterResource(id = viewState.navigationIcon),
            onNavigationIcon = viewEvent.navigateBack,
            title = stringResource(id = BasePresentation.string.title_settings),
        )
        Column(Modifier.verticalScroll(rememberScrollState())) {

            ProtonSettingsHeader(title = R.string.settings_section_security)

            ProtonSettingsItem(
                name = stringResource(id = R.string.settings_app_lock),
                hint = stringResource(id = viewState.appAccessSubtitleResId),
            ) {
                viewEvent.onAppAccess()
            }

            if (viewState.isAutoLockDurationsVisible) {
                ProtonSettingsItem(
                    name = stringResource(id = R.string.settings_auto_lock),
                    hint = viewState.autoLockDuration.toString(LocalContext.current),
                ) {
                    viewEvent.onAutoLockDurations()
                }
            }

            ProtonSettingsHeader(title = R.string.settings_section_appearance_settings)

            ProtonSettingsItem(
                name = stringResource(id = R.string.settings_theme_entry),
                hint = stringResource(viewState.currentStyle),
            ) {
                showThemeDialog = true
            }



            if (viewState.legalLinks.isNotEmpty()) {
                ProtonSettingsHeader(title = R.string.settings_section_about)

                viewState.legalLinks.forEach { link ->
                    when (link) {
                        is LegalLink.External -> ExternalSettingsEntry(
                            link = link,
                            onLinkClicked = viewEvent.onLinkClicked
                        )
                    }
                }
            }

            if (viewState.debugSettingsStateAndEvent != null) {
                DebugSettings(
                    viewState = viewState.debugSettingsStateAndEvent.viewState,
                    viewEvent = viewState.debugSettingsStateAndEvent.viewEvent
                )
            }

            TelemetrySettingToggleItem(divider = {})
            CrashReportSettingToggleItem(divider = {})

            NavigationDrawerAppVersion(
                modifier = Modifier.padding(top = DefaultSpacing),
                name = stringResource(id = viewState.appNameResId),
                version = viewState.appVersion
            )
        }
    }
}

@Preview(
    name = "Settings in light mode",
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Settings in dark mode",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
@Suppress("unused")
private fun SettingsPreview() {
    ProtonTheme {
        Settings(
            modifier = Modifier.background(MaterialTheme.colors.background),
            viewState = SettingsViewState(
                CorePresentation.drawable.ic_proton_arrow_up,
                BasePresentation.string.title_app,
                "1.0.0",
                listOf(
                    LegalLink.External(
                        text = BasePresentation.string.navigation_more_section_header,
                        url = BasePresentation.string.title_app
                    )
                ),
                availableStyles = emptyList(),
                currentStyle = BasePresentation.string.common_cancel_action,
                appAccessSubtitleResId = BasePresentation.string.common_cancel_action,
                isAutoLockDurationsVisible = true,
                autoLockDuration = 0.seconds,
            ),
            viewEvent = SettingsViewEvent(
                navigateBack = {},
                onLinkClicked = {},
                onThemeStyleChanged = {},
                onAppAccess = {},
                onAutoLockDurations = {}
            )
        )
    }
}
