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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.accountmanager.presentation.compose.AccountSettingsItem
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.devicemigration.presentation.settings.SignInToAnotherDeviceItem
import me.proton.core.drive.base.presentation.component.NavigationDrawerAppVersion
import me.proton.core.drive.base.presentation.component.TopAppBar
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
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.accountmanager.presentation.R as AccountPresentation

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
            title = stringResource(id = I18N.string.common_settings),
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .testTag(SettingsTestTag.list)
        ) {

            ProtonSettingsHeader(title = AccountPresentation.string.account_settings_header)
            AccountSettingsItem(
                modifier = Modifier.testTag(SettingsTestTag.account)
            ) {
                viewEvent.onAccountSettings()
            }
            SignInToAnotherDeviceItem(
                content = { label: String, onClick: () -> Unit ->
                    ProtonSettingsItem(name = label, onClick = onClick)
                },
                onLogOut = { viewEvent.onSignOut() }
            )

            ProtonSettingsHeader(title = I18N.string.settings_section_security)

            ProtonSettingsItem(
                name = stringResource(id = I18N.string.settings_app_lock),
                hint = stringResource(id = viewState.appAccessSubtitleResId),
            ) {
                viewEvent.onAppAccess()
            }

            if (viewState.isAutoLockDurationsVisible) {
                ProtonSettingsItem(
                    name = stringResource(id = I18N.string.settings_auto_lock),
                    hint = viewState.autoLockDuration.toString(LocalContext.current),
                ) {
                    viewEvent.onAutoLockDurations()
                }
            }

            if (viewState.isPhotosSettingsVisible) {
                ProtonSettingsHeader(title = I18N.string.settings_section_backup)
                ProtonSettingsItem(
                    name = stringResource(id = I18N.string.settings_photos_backup),
                    hint = stringResource(id = viewState.photosBackupSubtitleResId),
                ) {
                    viewEvent.onPhotosBackup()
                }
            }

            ProtonSettingsHeader(title = I18N.string.settings_section_appearance_settings)

            ProtonSettingsItem(
                name = stringResource(id = I18N.string.settings_theme_entry),
                hint = stringResource(viewState.currentStyle),
            ) {
                showThemeDialog = true
            }

            ProtonSettingsItem(
                name = stringResource(id = I18N.string.settings_home_tab),
                hint = stringResource(id = viewState.defaultHomeTabResId),
            ) {
                viewEvent.onDefaultHomeTab()
            }

            if (viewState.legalLinks.isNotEmpty()) {
                ProtonSettingsHeader(title = I18N.string.settings_section_about)

                viewState.legalLinks.forEach { link ->
                    when (link) {
                        is LegalLink.External -> ExternalSettingsEntry(
                            link = link,
                            onLinkClicked = viewEvent.onLinkClicked
                        )
                    }
                }
            }

            ProtonSettingsHeader(title = I18N.string.settings_section_statistics)
            TelemetrySettingToggleItem(divider = {})
            CrashReportSettingToggleItem( divider = {})

            if (viewState.debugSettingsStateAndEvent != null) {
                DebugSettings(
                    viewState = viewState.debugSettingsStateAndEvent.viewState,
                    viewEvent = viewState.debugSettingsStateAndEvent.viewEvent
                )
            }

            ProtonSettingsHeader(title = I18N.string.settings_section_system)

            ProtonSettingsItem(
                name = stringResource(id = I18N.string.settings_clear_local_cache_entry),
            ) {
                viewEvent.onClearLocalCache()
            }

            if (viewState.isLogSettingVisible) {
                ProtonSettingsItem(
                    name = stringResource(id = I18N.string.settings_show_log),
                ) {
                    viewEvent.onShowLog()
                }
            }

            NavigationDrawerAppVersion(
                modifier = Modifier
                    .padding(top = DefaultSpacing)
                    .testTag(SettingsTestTag.appVersion),
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
                I18N.string.common_app,
                "1.0.0",
                listOf(
                    LegalLink.External(
                        text = I18N.string.navigation_more_section_header,
                        url = I18N.string.common_app
                    )
                ),
                availableStyles = emptyList(),
                currentStyle = I18N.string.common_cancel_action,
                appAccessSubtitleResId = I18N.string.common_cancel_action,
                isAutoLockDurationsVisible = true,
                autoLockDuration = 0.seconds,
                isPhotosSettingsVisible = true,
                photosBackupSubtitleResId = I18N.string.common_off,
                defaultHomeTabResId = I18N.string.photos_title,
            ),
            viewEvent = SettingsViewEvent(
                navigateBack = {},
                onLinkClicked = {},
                onThemeStyleChanged = {},
                onAccountSettings = {},
                onAppAccess = {},
                onAutoLockDurations = {},
                onClearLocalCache = {},
                onPhotosBackup = {},
                onDefaultHomeTab = {},
                onShowLog = {},
                onSignOut = {},
            )
        )
    }
}

object SettingsTestTag {
    const val list = "settings list"
    const val account = "account"
    const val appVersion = "app version"
}
