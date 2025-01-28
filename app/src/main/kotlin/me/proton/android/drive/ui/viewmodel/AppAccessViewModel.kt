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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.lock.data.extension.onNotAvailable
import me.proton.android.drive.lock.data.extension.onReady
import me.proton.android.drive.lock.data.extension.onSetupRequired
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.android.drive.lock.domain.usecase.DisableAppLock
import me.proton.android.drive.lock.domain.usecase.EnableAppLock
import me.proton.android.drive.lock.domain.usecase.GetLockState
import me.proton.android.drive.ui.viewevent.AppAccessViewEvent
import me.proton.android.drive.ui.viewstate.AccessOption
import me.proton.android.drive.ui.viewstate.AppAccessViewState
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@Suppress("StaticFieldLeak")
@HiltViewModel
class AppAccessViewModel @Inject constructor(
     @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val disableAppLock: DisableAppLock,
    private val enableAppLock: EnableAppLock,
    private val getLockState: GetLockState,
    private val broadcastMessages: BroadcastMessages,
    private val appLockManager: AppLockManager,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val initialViewState: AppAccessViewState = AppAccessViewState(
        title = appContext.getString(I18N.string.app_lock_access_title),
        enabledOption = AccessOption.NONE,
    )
    val viewState: Flow<AppAccessViewState> = appLockManager.enabled.map { enabled ->
        initialViewState.copy(
            enabledOption = if (!enabled) AccessOption.NONE else AccessOption.SYSTEM
        )
    }

    fun viewEvent(
        navigateToSystemAccess: () -> Unit,
        navigateBack: () -> Unit,
    ): AppAccessViewEvent = object : AppAccessViewEvent {
        override val onDisable: () -> Unit = { doDisableAppLock(navigateBack) }
        override val onSystem: () -> Unit = {
            getLockState()
                .onNotAvailable {
                    broadcastMessages(
                        userId = userId,
                        message = appContext.getString(I18N.string.app_lock_system_not_available),
                        type = BroadcastMessage.Type.WARNING,
                    )
                }
                .onSetupRequired {
                    navigateToSystemAccess()
                }
                .onReady {
                    doEnableSystemLock(navigateBack)
                }
        }
    }

    private fun doDisableAppLock(navigateBack: () -> Unit) = viewModelScope.launch {
        if (appLockManager.isEnabled().not()) return@launch
        disableAppLock()
            .onFailure { error ->
                error.log(VIEW_MODEL, "Cannot disable app lock")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(appContext, true),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
            .onSuccess {
                broadcastMessages(
                    userId = userId,
                    message = appContext.getString(I18N.string.app_lock_disabled),
                    type = BroadcastMessage.Type.INFO,
                )
                navigateBack()
            }
    }

    private fun doEnableSystemLock(navigateBack: () -> Unit) = viewModelScope.launch {
        if (appLockManager.isEnabled()) return@launch
        enableAppLock(lockType = AppLockType.SYSTEM)
            .onFailure { error ->
                error.log(VIEW_MODEL, "Cannot enable app lock")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(appContext, true),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
            .onSuccess {
                broadcastMessages(
                    userId = userId,
                    message = appContext.getString(I18N.string.app_lock_enabled),
                    type = BroadcastMessage.Type.INFO,
                )
                navigateBack()
            }
    }
}
