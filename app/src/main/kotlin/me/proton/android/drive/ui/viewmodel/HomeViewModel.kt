/*
 * Copyright (c) 2023 Proton AG.
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.R
import me.proton.android.drive.ui.navigation.HomeTab
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.HomeViewEvent
import me.proton.android.drive.ui.viewstate.HomeViewState
import me.proton.core.domain.entity.SessionUserId
import me.proton.core.drive.base.presentation.component.NavigationTab
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerViewEvent
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerViewState
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
class HomeViewModel @Inject constructor(
    userManager: UserManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val tabs: Map<HomeTab, NavigationTab> = mapOf(
        Screen.Files to NavigationTab(CorePresentation.drawable.ic_proton_folder_filled, BasePresentation.string.title_files),
        Screen.Shared to NavigationTab(CorePresentation.drawable.ic_proton_link, BasePresentation.string.title_shared)
    )
    private val currentDestination = MutableStateFlow(Screen.Files.route)
    fun setCurrentDestination(route: String) {
        currentDestination.value = route
    }

    val initialViewState = getViewState(null, currentDestination.value)
    val viewState: Flow<HomeViewState> =
        combine(
            userManager.observeUser(SessionUserId(userId.id)),
            currentDestination
        ) { user, selectedScreen ->
            getViewState(user, selectedScreen)
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun viewEvent(
        navigateToSigningOut: () -> Unit,
        navigateToTrash: () -> Unit,
        navigateToTab: (route: String) -> Unit,
        navigateToOffline: () -> Unit,
        navigateToSettings: () -> Unit,
        sendBugReport: () -> Unit,
    ): HomeViewEvent = object : HomeViewEvent {
        override val onTab = { tab: NavigationTab -> navigateToTab(tab.screen(userId)) }
        override val navigationDrawerViewEvent: NavigationDrawerViewEvent =
            object : NavigationDrawerViewEvent {
                override val onMyFiles = { navigateToTab(Screen.Files(userId)) }
                override val onTrash = navigateToTrash
                override val onOffline = navigateToOffline
                override val onSettings = navigateToSettings
                override val onSignOut = navigateToSigningOut
                override val onBugReport = sendBugReport
            }
    }

    private val NavigationTab.screen: HomeTab
        get() = tabs.firstNotNullOf { (screen, value) -> screen.takeIf { value == this } }

    private fun getViewState(
        user: User?,
        startDestination: String,
    ) =
        HomeViewState(
            tabs = tabs.values.toList(),
            selectedTab = tabs.firstNotNullOf { (screen, tab) ->
                tab.takeIf { screen.route == startDestination }
            },
            navigationDrawerViewState = NavigationDrawerViewState(
                R.string.app_name,
                BuildConfig.VERSION_NAME,
                currentUser = user
            )
        )
}
