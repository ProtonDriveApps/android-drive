/*
 * Copyright (c) 2023-2024 Proton AG.
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
import kotlinx.coroutines.flow.shareIn
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.ui.navigation.HomeTab
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.HomeViewEvent
import me.proton.android.drive.ui.viewstate.HomeViewState
import me.proton.android.drive.usecase.CanGetMoreFreeStorage
import me.proton.core.domain.entity.SessionUserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.component.NavigationTab
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
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
    private val canGetMoreFreeStorage: CanGetMoreFreeStorage,
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val tabs: StateFlow<Map<out HomeTab, NavigationTab>> = MutableStateFlow(
        listOfNotNull(
            Screen.Files to NavigationTab(
                iconResId = CorePresentation.drawable.ic_proton_folder,
                titleResId = I18N.string.title_files
            ),
            takeIf { configurationProvider.photosFeatureFlag }?.let {
                Screen.Photos to NavigationTab(
                    iconResId = CorePresentation.drawable.ic_proton_image,
                    titleResId = I18N.string.photos_title
                )
            },
            Screen.Computers to NavigationTab(
                iconResId = CorePresentation.drawable.ic_proton_tv,
                titleResId = I18N.string.computers_title
            ),
            Screen.Shared to NavigationTab(
                iconResId = CorePresentation.drawable.ic_proton_link,
                titleResId = I18N.string.title_shared,
            ),
            Screen.SharedTabs to NavigationTab(
                iconResId = CorePresentation.drawable.ic_proton_users,
                titleResId = I18N.string.title_shared,
            ),
        ).associateBy({ tab -> tab.first }, { tab -> tab.second })
    )

    private val currentDestination = MutableStateFlow<String?>(null)
    fun setCurrentDestination(route: String) {
        currentDestination.value = route
    }

    val viewState: Flow<HomeViewState> =
        combine(
            userManager.observeUser(SessionUserId(userId.id)),
            currentDestination.filterNotNull(),
            getFeatureFlagFlow(FeatureFlagId.driveSharingInvitations(userId)),
            tabs.filter { tabs -> tabs.isNotEmpty() },
        ) { user, selectedScreen, sharingFeatureFlag, tabs ->
            val sharingEnabled = sharingFeatureFlag.state == FeatureFlag.State.ENABLED
            getViewState(
                user = user,
                startDestination = selectedScreen,
                tabs = tabs.filter { tab ->
                    when(tab.key){
                        Screen.Shared -> !sharingEnabled
                        Screen.SharedTabs -> sharingEnabled
                        else -> true
                    }
                }
            )
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun viewEvent(
        navigateToSigningOut: () -> Unit,
        navigateToTrash: () -> Unit,
        navigateToTab: (route: String) -> Unit,
        navigateToOffline: () -> Unit,
        navigateToSettings: () -> Unit,
        navigateToBugReport: () -> Unit,
        navigateToSubscription: () -> Unit,
        navigateToGetMoreFreeStorage: () -> Unit,
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
                override val onGetFreeStorage = navigateToGetMoreFreeStorage
            }
    }

    private val NavigationTab.screen: HomeTab
        get() = tabs.value.firstNotNullOf { (screen, value) -> screen.takeIf { value == this } }

    private suspend fun getViewState(
        user: User?,
        startDestination: String,
        tabs: Map<out HomeTab, NavigationTab>,
    ) =
        HomeViewState(
            tabs = tabs.values.toList(),
            selectedTab = tabs.firstNotNullOfOrNull { (screen, tab) ->
                tab.takeIf { screen.route == startDestination }
            },
            navigationDrawerViewState = NavigationDrawerViewState(
                I18N.string.app_name,
                BuildConfig.VERSION_NAME,
                currentUser = user,
                showGetFreeStorage = user?.let { canGetMoreFreeStorage(user) } ?: false,
            )
        )
}
