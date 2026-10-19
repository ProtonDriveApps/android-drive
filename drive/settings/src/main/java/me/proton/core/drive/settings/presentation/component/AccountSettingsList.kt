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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.core.accountmanager.presentation.compose.R
import me.proton.core.accountmanager.presentation.compose.viewmodel.AccountSettingsViewModel
import me.proton.core.accountmanager.presentation.compose.viewmodel.AccountSettingsViewState
import me.proton.core.auth.fido.domain.entity.Fido2RegisteredKey
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.viewmodel.hiltViewModelOrNull
import me.proton.core.drive.base.presentation.component.ProtonSettingsHeader
import me.proton.core.drive.base.presentation.component.ProtonSettingsItem
import me.proton.core.user.domain.entity.UserRecovery

@Composable
fun AccountSettingsListHeader() {
    ProtonSettingsHeader(title = stringResource(R.string.account_settings_list_header))
}

@Composable
fun AccountSettingsList(
    modifier: Modifier = Modifier,
    viewModel: AccountSettingsViewModel? = hiltViewModelOrNull(),
    onPasswordManagementClick: () -> Unit,
    onRecoveryEmailClick: () -> Unit,
    onSecurityKeysClick: () -> Unit,
    header: LazyListScope.() -> Unit = { item { AccountSettingsListHeader() } },
    footer: LazyListScope.() -> Unit = {},
    divider: @Composable () -> Unit = { HorizontalDivider() },
) {
    val state = when (viewModel) {
        null -> AccountSettingsViewState.Null
        else -> viewModel.state.collectAsStateWithLifecycle().value
    }
    AccountSettingsList(
        modifier = modifier,
        state = state,
        onPasswordManagementClick = onPasswordManagementClick,
        onRecoveryEmailClick = onRecoveryEmailClick,
        onSecurityKeysClick = onSecurityKeysClick,
        header = header,
        footer = footer,
        divider = divider
    )
}

@Composable
fun AccountSettingsList(
    modifier: Modifier = Modifier,
    state: AccountSettingsViewState,
    onPasswordManagementClick: () -> Unit,
    onRecoveryEmailClick: () -> Unit,
    onSecurityKeysClick: () -> Unit,
    header: LazyListScope.() -> Unit = { item { AccountSettingsListHeader() } },
    footer: LazyListScope.() -> Unit = {},
    divider: @Composable () -> Unit = { HorizontalDivider() },
) {
    when (state) {
        is AccountSettingsViewState.Hidden -> Unit
        is AccountSettingsViewState.CredentialLess -> Unit
        is AccountSettingsViewState.LoggedIn -> AccountSettingsList(
            modifier = modifier,
            state = state,
            onPasswordManagementClick = onPasswordManagementClick,
            onRecoveryEmailClick = onRecoveryEmailClick,
            onSecurityKeysClick = onSecurityKeysClick,
            header = header,
            footer = footer,
            divider = divider
        )
    }
}

@Composable
fun AccountSettingsList(
    modifier: Modifier = Modifier,
    state: AccountSettingsViewState.LoggedIn,
    onPasswordManagementClick: () -> Unit,
    onRecoveryEmailClick: () -> Unit,
    onSecurityKeysClick: () -> Unit,
    header: LazyListScope.() -> Unit = { item { AccountSettingsListHeader() } },
    footer: LazyListScope.() -> Unit = {},
    divider: @Composable () -> Unit = { HorizontalDivider() },
) {
    val recoveryHint = state.recoveryEmail
        ?: stringResource(R.string.account_settings_list_item_recovery_hint_not_set)

    val passwordHint = when (state.recoveryState) {
        null -> null
        UserRecovery.State.None -> null
        UserRecovery.State.Cancelled -> null
        UserRecovery.State.Expired -> null
        UserRecovery.State.Grace -> stringResource(R.string.account_settings_list_item_password_hint_grace)
        UserRecovery.State.Insecure -> stringResource(R.string.account_settings_list_item_password_hint_insecure)
    }

    Surface(
        color = ProtonTheme.colors.backgroundNorm,
        contentColor = ProtonTheme.colors.textNorm,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(state = rememberLazyListState()) {
            header()
            item {
                ProtonSettingsItem(
                    name = stringResource(R.string.account_settings_list_item_password_header),
                    hint = passwordHint,
                    onClick = onPasswordManagementClick
                )
                divider()
            }
            item {
                ProtonSettingsItem(
                    name = stringResource(R.string.account_settings_list_item_recovery_header),
                    hint = recoveryHint,
                    onClick = onRecoveryEmailClick
                )
                divider()
            }
            if (state.securityKeysVisible) {
                item {
                    SecurityKeysSettingsItem(
                        onSecurityKeysClick = onSecurityKeysClick,
                        registeredSecurityKeys = state.registeredSecurityKeys
                    )
                }
            }
            footer()
        }
    }
}

@Composable
fun SecurityKeysSettingsItem(
    onSecurityKeysClick: () -> Unit,
    registeredSecurityKeys: List<Fido2RegisteredKey>?
) {
    ProtonSettingsItem(
        name = stringResource(R.string.account_settings_list_item_security_keys_header),
        hint = securityKeysSettingsItemHint(registeredSecurityKeys),
        onClick = onSecurityKeysClick
    )
}

@Composable
private fun securityKeysSettingsItemHint(registeredSecurityKeys: List<Fido2RegisteredKey>?): String = when {
    registeredSecurityKeys.isNullOrEmpty() ->
        stringResource(R.string.account_settings_list_item_security_keys_hint_not_set)

    registeredSecurityKeys.size == 1 -> stringResource(
        R.string.account_settings_list_item_security_keys_hint_single,
        registeredSecurityKeys.first().name
    )

    else -> pluralStringResource(
        R.plurals.account_settings_list_item_security_keys_hint_many,
        registeredSecurityKeys.size,
        registeredSecurityKeys.size
    )
}
