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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.ui.navigation.HomeTab
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.HomeViewEvent
import me.proton.android.drive.ui.viewstate.HomeViewState
import me.proton.core.domain.entity.SessionUserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.component.NavigationTab
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.usecase.IsFeatureFlagEnabled
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerViewEvent
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerViewState
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
class HomeViewModel @Inject constructor(
    userManager: UserManager,
    savedStateHandle: SavedStateHandle,
    configurationProvider: ConfigurationProvider,
    isFeatureFlagEnabled: IsFeatureFlagEnabled,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val isPhotosFeatureEnabled: StateFlow<Boolean?> = flow {
        emit(configurationProvider.photosFeatureFlag && isFeatureFlagEnabled(FeatureFlagId.drivePhotos(userId)))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val tabs: StateFlow<Map<out HomeTab, NavigationTab>> = isPhotosFeatureEnabled
        .filterNotNull()
        .map { isPhotosFeatureEnabled ->
            listOfNotNull(
                Screen.Files to NavigationTab(
                    iconResId = CorePresentation.drawable.ic_proton_folder,
                    titleResId = I18N.string.title_files
                ),
                takeIf { isPhotosFeatureEnabled }?.let {
                    Screen.Photos to NavigationTab(
                        iconResId = CorePresentation.drawable.ic_proton_image,
                        titleResId = I18N.string.photos_title
                    )
                },
                Screen.Shared to NavigationTab(
                    iconResId = CorePresentation.drawable.ic_proton_link,
                    titleResId = I18N.string.title_shared
                ),
            ).associateBy({ tab -> tab.first }, { tab -> tab.second })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val currentDestination = MutableStateFlow(Screen.Files.route)
    fun setCurrentDestination(route: String) {
        currentDestination.value = route
    }

    val viewState: Flow<HomeViewState> =
        combine(
            userManager.observeUser(SessionUserId(userId.id)),
            currentDestination,
            tabs.filter { tabs -> tabs.isNotEmpty() },
        ) { user, selectedScreen, tabs ->
            getViewState(user, selectedScreen, tabs)
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun viewEvent(
        navigateToSigningOut: () -> Unit,
        navigateToTrash: () -> Unit,
        navigateToTab: (route: String) -> Unit,
        navigateToOffline: () -> Unit,
        navigateToSettings: () -> Unit,
        navigateToBugReport: () -> Unit,
        navigateToSubscription: () -> Unit,
    ): HomeViewEvent = object : HomeViewEvent {
        override val onTab = { tab: NavigationTab -> navigateToTab(tab.screen(userId)) }
        override val navigationDrawerViewEvent: NavigationDrawerViewEvent =
            object : NavigationDrawerViewEvent {
                override val onMyFiles = { navigateToTab(Screen.Files(userId)) }
                override val onTrash = navigateToTrash
                override val onOffline = navigateToOffline
                override val onSettings = navigateToSettings
                override val onSignOut = navigateToSigningOut
                override val onBugReport = navigateToBugReport
                override val onSubscription = navigateToSubscription
            }
    }

    private val NavigationTab.screen: HomeTab
        get() = tabs.value.firstNotNullOf { (screen, value) -> screen.takeIf { value == this } }

    private fun getViewState(
        user: User?,
        startDestination: String,
        tabs: Map<out HomeTab, NavigationTab>,
    ) =
        HomeViewState(
            tabs = tabs.values.toList(),
            selectedTab = tabs.firstNotNullOf { (screen, tab) ->
                tab.takeIf { screen.route == startDestination }
            },
            navigationDrawerViewState = NavigationDrawerViewState(
                I18N.string.app_name,
                BuildConfig.VERSION_NAME,
                currentUser = user
            )
        )
}
