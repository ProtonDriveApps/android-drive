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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.accountmanager.presentation.compose.AccountSettingsList
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.accountmanager.presentation.R as AccountPresentation
import me.proton.core.presentation.R as CorePresentation


@Composable
fun AccountSettingsScreen(
    modifier: Modifier = Modifier,
    navigateToPasswordManagement: () -> Unit,
    navigateToRecoveryEmail: () -> Unit,
    navigateToSecurityKeys: () -> Unit,
    navigateBack: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
            title = stringResource(AccountPresentation.string.account_settings_header),
            modifier = Modifier.statusBarsPadding()
        )
        AccountSettingsList(
            onPasswordManagementClick = navigateToPasswordManagement,
            onRecoveryEmailClick = navigateToRecoveryEmail,
            onSecurityKeysClick = navigateToSecurityKeys,
            divider = {}
        )
    }
}

@Preview
@Composable
private fun AccountSettingsScreenPreview() {
    ProtonTheme {
        AccountSettingsScreen(
            navigateToPasswordManagement = {},
            navigateToRecoveryEmail = {},
            navigateToSecurityKeys = {},
            navigateBack = {}
        )
    }
}

