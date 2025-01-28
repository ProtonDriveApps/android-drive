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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.ui.navigation.HomeTab
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.HomeViewEvent
import me.proton.android.drive.ui.viewstate.HomeViewState
import me.proton.android.drive.usecase.CanGetMoreFreeStorage
import me.proton.android.drive.usecase.GetDynamicHomeTabsFlow
import me.proton.android.drive.usecase.ShouldShowOverlay
import me.proton.core.domain.entity.SessionUserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.component.NavigationTab
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerViewEvent
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerViewState
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.DynamicHomeTab
import me.proton.drive.android.settings.domain.entity.UserOverlay
import me.proton.drive.android.settings.domain.entity.WhatsNewKey
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@ExperimentalCoroutinesApi
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    userManager: UserManager,
    savedStateHandle: SavedStateHandle,
    private val canGetMoreFreeStorage: CanGetMoreFreeStorage,
    getDynamicHomeTabsFlow: GetDynamicHomeTabsFlow,
    private val broadcastMessages: BroadcastMessages,
    private val shouldShowOverlay: ShouldShowOverlay,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var navigateToTab: ((route: String) -> Unit)? = null

    private val tabs: StateFlow<Map<out HomeTab, NavigationTab>> = getDynamicHomeTabsFlow(userId)
        .map { dynamicHomeTabs ->
            dynamicHomeTabs
                .filter { dynamicHomeTab -> dynamicHomeTab.isEnabled }
                .sortedBy { dynamicHomeTab -> dynamicHomeTab.order }
                .map { dynamicHomeTab ->
                    dynamicHomeTab.screen to NavigationTab(
                        iconResId = dynamicHomeTab.iconResId,
                        titleResId = dynamicHomeTab.titleResId,
                    )
                }
                .associateBy({ tab -> tab.first }, { tab -> tab.second })
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val currentDestination = MutableStateFlow<String?>(null)
    fun setCurrentDestination(route: String) {
        currentDestination.value = route
    }

    val viewState: Flow<HomeViewState> =
        combine(
            userManager.observeUser(SessionUserId(userId.id)),
            currentDestination.filterNotNull(),
            tabs.filter { tabs -> tabs.isNotEmpty() },
        ) { user, selectedScreen, tabs ->
            handleInvalidDestination(selectedScreen, tabs)
            getViewState(
                user = user,
                startDestination = selectedScreen,
                tabs = tabs,
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
        navigateToOnboarding: () -> Unit,
        navigateToWhatsNew: (WhatsNewKey) -> Unit,
        navigateToRatingBooster: () -> Unit,
    ): HomeViewEvent = object : HomeViewEvent {
        override val onTab = { tab: NavigationTab -> navigateToTab(tab.screen(userId)) }
        override val onFirstLaunch: () -> Unit = {
            viewModelScope.launch {
                when (val overlay = shouldShowOverlay()) {
                    UserOverlay.Onboarding -> navigateToOnboarding()
                    is UserOverlay.WhatsNew -> navigateToWhatsNew(overlay.key)
                    UserOverlay.RatingBooster -> navigateToRatingBooster()
                    null -> {}
                }
            }
        }
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
    }.also {
        this.navigateToTab = navigateToTab
    }

    suspend fun shouldShowOverlay(): UserOverlay? =
        shouldShowOverlay(userId).getOrNull(VIEW_MODEL, "Should show overlay failed")
            ?.also { overlay -> CoreLogger.i(VIEW_MODEL, "Showing overlay: $overlay") }

    private val NavigationTab.screen: HomeTab
        get() = tabs.value.firstNotNullOf { (screen, value) -> screen.takeIf { value == this } }

    private val DynamicHomeTab.screen: HomeTab get() = when (route) {
        Screen.Files.route -> Screen.Files
        Screen.Photos.route -> Screen.Photos
        Screen.Computers.route -> Screen.Computers
        Screen.SharedTabs.route -> Screen.SharedTabs
        else -> error("Unhandled tab item route: $route")
    }

    private fun getViewState(
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

    private fun handleInvalidDestination(destinationRoute: String, tabs: Map<out HomeTab, NavigationTab>) {
        val routes = tabs.values.map { navigationTab -> navigationTab.screen.route }
        if (destinationRoute !in routes) {
            CoreLogger.w(VIEW_MODEL, "Invalid destination route: $destinationRoute")
            navigateToTab?.invoke(tabs.values.first().screen(userId))
            broadcastMessages(
                userId = userId,
                message = appContext.getString(I18N.string.in_app_notification_destination_screen_not_found),
                type = BroadcastMessage.Type.ERROR,
            )
        }
    }
}
