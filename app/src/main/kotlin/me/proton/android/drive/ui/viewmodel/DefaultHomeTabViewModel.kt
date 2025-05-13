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
import me.proton.android.drive.usecase.GetDynamicHomeTabsFlow
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.settings.presentation.entity.TabItem
import me.proton.drive.android.settings.domain.entity.HomeTab
import me.proton.drive.android.settings.domain.usecase.UpdateHomeTab
import javax.inject.Inject

@HiltViewModel
class DefaultHomeTabViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDynamicHomeTabsFlow: GetDynamicHomeTabsFlow,
    private val updateHomeTab: UpdateHomeTab,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val homeTabItems: Flow<Set<TabItem>> = getDynamicHomeTabsFlow(userId)
        .map { dynamicHomeTabs ->
            dynamicHomeTabs
                .sortedBy { dynamicHomeTab -> dynamicHomeTab.order }
                .map { dynamicHomeTab ->
                    TabItem(
                        iconResId = dynamicHomeTab.iconResId,
                        titleResId = dynamicHomeTab.titleResId,
                        route = dynamicHomeTab.route,
                        isSelected = dynamicHomeTab.isUserDefault,
                        isEnabled = dynamicHomeTab.isEnabled,
                    )
                }.toSet()
        }

    fun onTabItem(tabItem: TabItem) = viewModelScope.launch {
        updateHomeTab(userId, tabItem.toHomeTab())
    }

    private fun TabItem.toHomeTab(): HomeTab = when (route) {
        Screen.Files.route -> HomeTab.FILES
        Screen.Photos.route -> HomeTab.PHOTOS
        Screen.PhotosAndAlbums.route -> HomeTab.PHOTOS
        Screen.Computers.route -> HomeTab.COMPUTERS
        Screen.SharedTabs.route -> HomeTab.SHARED
        else -> error("Unhandled tab item route: $route")
    }
}
