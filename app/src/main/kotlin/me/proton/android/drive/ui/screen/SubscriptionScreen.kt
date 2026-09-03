/*
 * Copyright (c) 2026 Proton AG.
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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.proton.android.core.designsystem.theme.ProtonTheme as ProtonDesignSystemTheme
import me.proton.android.payment.ui.subscriptionmanagement.SubscriptionManagementScreen
import me.proton.android.payment.ui.subscriptionmanagement.SubscriptionManagementViewModel

/**
 * Drive's host for the UFC subscription screen. Loads the dedicated ViewModel here (not in the nav
 * graph) and hosts the SDK screen inside the UFC ProtonTheme, which initializes ProtonPalette.
 */
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SubscriptionManagementViewModel>()
    ProtonDesignSystemTheme {
        SubscriptionManagementScreen(
            viewModel = viewModel,
            onNavigateUpClick = onNavigateBack,
            modifier = modifier,
        )
    }
}
