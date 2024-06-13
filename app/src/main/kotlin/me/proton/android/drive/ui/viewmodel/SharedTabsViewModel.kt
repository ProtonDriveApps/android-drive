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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.ui.viewevent.SharedTabsViewEvent
import me.proton.android.drive.ui.viewstate.SharedTab
import me.proton.android.drive.ui.viewstate.SharedTabsViewState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import javax.inject.Inject
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class SharedTabsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    shouldUpgradeStorage: ShouldUpgradeStorage,
) : ViewModel(),
    UserViewModel by UserViewModel(savedStateHandle),
    HomeTabViewModel,
    NotificationDotViewModel by NotificationDotViewModel(shouldUpgradeStorage) {
    private val sharedWithMeTab = SharedTab(SharedTab.Type.SHARED_WITH_ME, I18N.string.shared_with_me_title)
    private val sharedWithByTab = SharedTab(SharedTab.Type.SHARED_BY_ME, I18N.string.shared_by_me_title)
    private val selectedTab: MutableStateFlow<SharedTab> = MutableStateFlow(sharedWithMeTab)
    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()

    val initialViewState = SharedTabsViewState(
        titleResId = I18N.string.title_shared,
        navigationIconResId = CorePresentation.drawable.ic_proton_hamburger,
        notificationDotVisible = false,
        tabs = listOf(sharedWithMeTab, sharedWithByTab),
        selectedTab = selectedTab.value,
    )
    val viewState: Flow<SharedTabsViewState> = combine(
        selectedTab,
        notificationDotRequested,
    ) { selected, notificationDotRequested ->
        initialViewState.copy(
            selectedTab = selected,
            notificationDotVisible = notificationDotRequested,
        )
    }

    fun viewEvent(): SharedTabsViewEvent = object : SharedTabsViewEvent {

        override val onTopAppBarNavigation = {
            viewModelScope.launch { _homeEffect.emit(HomeEffect.OpenDrawer) }
            Unit
        }

        override val onTab = { sharedTab: SharedTab ->
            selectedTab.value = sharedTab
        }
    }
}
