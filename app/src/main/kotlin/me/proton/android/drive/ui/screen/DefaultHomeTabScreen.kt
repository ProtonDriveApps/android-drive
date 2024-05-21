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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.DefaultHomeTabViewModel
import me.proton.core.drive.settings.presentation.component.DefaultHomeTab
import me.proton.core.drive.i18n.R as I18N

@Composable
fun DefaultHomeTabScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<DefaultHomeTabViewModel>()
    DefaultHomeTab(
        title = stringResource(id = I18N.string.title_default_home_screen_tab),
        tabItemsFlow = viewModel.homeTabItems,
        modifier = modifier,
        navigateBack = navigateBack,
        onClick = { tabItem ->  viewModel.onTabItem(tabItem) },
    )
}
