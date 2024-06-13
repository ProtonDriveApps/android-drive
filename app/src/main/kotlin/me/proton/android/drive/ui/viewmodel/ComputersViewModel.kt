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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.ui.viewevent.ComputersViewEvent
import me.proton.android.drive.ui.viewstate.ComputersViewState
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.extension.logDefaultMessage
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.extension.name
import me.proton.core.drive.device.domain.usecase.RefreshDevices
import me.proton.core.drive.drivelink.device.domain.usecase.GetDecryptedDevicesSortedByName
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.i18n.R
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import javax.inject.Inject
import me.proton.core.drive.drivelink.device.presentation.R as DriveLinkDevicePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class ComputersViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    getDevices: GetDecryptedDevicesSortedByName,
    savedStateHandle: SavedStateHandle,
    private val refreshDevices: RefreshDevices,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
    shouldUpgradeStorage: ShouldUpgradeStorage,
) : ViewModel(),
    UserViewModel by UserViewModel(savedStateHandle),
    HomeTabViewModel,
    NotificationDotViewModel by NotificationDotViewModel(shouldUpgradeStorage) {

    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()

    private val emptyState = ListContentState.Empty(
        imageResId = getThemeDrawableId(
            light = DriveLinkDevicePresentation.drawable.empty_devices_light,
            dark = DriveLinkDevicePresentation.drawable.empty_devices_dark,
            dayNight = DriveLinkDevicePresentation.drawable.empty_devices_daynight,
        ),
        titleId = I18N.string.computers_empty_title,
        descriptionResId = I18N.string.computers_empty_description,
    )
    private val isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val listContentState: MutableStateFlow<ListContentState> = MutableStateFlow(ListContentState.Loading)

    val initialViewState = ComputersViewState(
        title = appContext.getString(R.string.computers_title),
        navigationIconResId = me.proton.core.presentation.R.drawable.ic_proton_hamburger,
        notificationDotVisible = false,
        listContentState = listContentState.value,
        isRefreshEnabled = listContentState.value != ListContentState.Loading
    )

    val viewState: Flow<ComputersViewState> = combine(
        listContentState,
        isRefreshing,
        notificationDotRequested
    ) { state, refreshing, notificationDotRequested ->
        initialViewState.copy(
            notificationDotVisible = notificationDotRequested,
            listContentState = when (state) {
                is ListContentState.Content -> state.copy(isRefreshing = refreshing)
                is ListContentState.Empty -> state.copy(isRefreshing = refreshing)
                is ListContentState.Error -> state.copy(isRefreshing = refreshing)
                else -> state
            },
            isRefreshEnabled = state != ListContentState.Loading,
        )
    }

    val devices: Flow<List<Device>> = getDevices(userId)
        .transform { result ->
            when (result) {
                is DataResult.Processing -> listContentState.value = ListContentState.Loading
                is DataResult.Error -> {
                    result.log(VIEW_MODEL)
                    broadcastMessages(
                        userId = userId,
                        message = result.logDefaultMessage(
                            context = appContext,
                            useExceptionMessage = configurationProvider.useExceptionMessage,
                            tag = VIEW_MODEL,
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
                is DataResult.Success -> {
                    val devices = result.value
                    if (devices.isEmpty()) {
                        listContentState.value = emptyState
                    } else {
                        listContentState.value = ListContentState.Content()
                    }
                    emit(result.value)
                }
            }
        }

    fun viewEvent(
        navigateToSyncedFolders: (FolderId, String?) -> Unit,
        navigateToComputerOptions: (deviceId: DeviceId) -> Unit,
    ): ComputersViewEvent = object : ComputersViewEvent {

        override val onTopAppBarNavigation = {
            viewModelScope.launch { _homeEffect.emit(HomeEffect.OpenDrawer) }
            Unit
        }

        override val onDevice = { device: Device ->
            val name = takeIf { device.cryptoName is CryptoProperty.Decrypted }?.let { device.name }
            navigateToSyncedFolders(device.rootLinkId, name)
        }

        override val onMoreOptions = { device: Device ->
            navigateToComputerOptions(device.id)
        }

        override val onRefresh = {
            viewModelScope.launch {
                isRefreshing.value = true
                refreshDevices(userId)
                    .onFailure { error ->
                        isRefreshing.value = false
                        error.log(VIEW_MODEL)
                        broadcastMessages(
                            userId = userId,
                            message = error.logDefaultMessage(
                                context = appContext,
                                tag = VIEW_MODEL,
                                useExceptionMessage = configurationProvider.useExceptionMessage,
                            ),
                            type = BroadcastMessage.Type.ERROR,
                        )
                    }
                    .onSuccess {
                        isRefreshing.value = false
                    }
            }
            Unit
        }
    }
}
