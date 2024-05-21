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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.settings.presentation.entity.TabItem
import me.proton.drive.android.settings.domain.entity.HomeTab
import me.proton.drive.android.settings.domain.usecase.GetHomeTab
import me.proton.drive.android.settings.domain.usecase.UpdateHomeTab
import javax.inject.Inject
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class DefaultHomeTabViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getHomeTab: GetHomeTab,
    configurationProvider: ConfigurationProvider,
    private val updateHomeTab: UpdateHomeTab,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val tabItems = listOfNotNull(
        TabItem(
            iconResId = CorePresentation.drawable.ic_proton_folder,
            titleResId = I18N.string.title_files,
            route = Screen.Files.route,
            isSelected = false,
        ),
        takeIf { configurationProvider.photosFeatureFlag }
            ?.let {
                TabItem(
                    iconResId = CorePresentation.drawable.ic_proton_image,
                    titleResId = I18N.string.photos_title,
                    route = Screen.Photos.route,
                    isSelected = false,
                )
            },
        TabItem(
            iconResId = CorePresentation.drawable.ic_proton_tv,
            titleResId = I18N.string.computers_title,
            route = Screen.Computers.route,
            isSelected = false,
        ),
        TabItem(
            iconResId = CorePresentation.drawable.ic_proton_link,
            titleResId = I18N.string.title_shared,
            route = Screen.Shared.route,
            isSelected = false,
        ),
    )

    val homeTabItems: Flow<Set<TabItem>> = getHomeTab(userId).map { homeTab ->
        tabItems.map { tabItem ->
            if (tabItem.toHomeTab() == homeTab) {
                tabItem.copy(isSelected = true)
            } else {
                tabItem
            }
        }.toSet()
    }

    fun onTabItem(tabItem: TabItem) = viewModelScope.launch {
        updateHomeTab(userId, tabItem.toHomeTab())
    }

    private fun TabItem.toHomeTab(): HomeTab = when {
        route == Screen.Files.route -> HomeTab.FILES
        route == Screen.Photos.route -> HomeTab.PHOTOS
        route == Screen.Computers.route -> HomeTab.COMPUTERS
        route == Screen.Shared.route -> HomeTab.SHARED
        else -> error("Unhandled tab item route: $route")
    }
}
