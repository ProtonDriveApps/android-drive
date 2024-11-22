/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.notification.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.notification.domain.usecase.HasUserRejectedNotificationPermissionRationale
import me.proton.core.drive.notification.presentation.viewevent.NotificationPermissionViewEvent
import javax.inject.Inject

@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val hasUserRejectedNotificationPermissionRationale: HasUserRejectedNotificationPermissionRationale,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    fun viewEvent(
        navigateToNotificationPermissionRationale: () -> Unit,
    ): NotificationPermissionViewEvent = object : NotificationPermissionViewEvent {
        override val onPermissionDenied = { permissionState: PermissionState, shouldShowRationale: Boolean ->
            onPermissionDenied(permissionState, shouldShowRationale, navigateToNotificationPermissionRationale)
        }
    }

    private fun onPermissionDenied(
        permissionState: PermissionState,
        shouldShowRationale: Boolean,
        navigateToNotificationPermissionRationale: () -> Unit
    ) {
        if (shouldShowRationale.not()) {
            if (permissionState.status.shouldShowRationale.not()) {
                permissionState.launchPermissionRequest()
            }
        } else {
            viewModelScope.launch {
                val hasRejected = hasUserRejectedNotificationPermissionRationale(userId)
                    .getOrNull(
                        tag = LogTag.NOTIFICATION,
                        message = "Has user rejected notification permission rationale failed",
                    ) ?: false
                if (hasRejected.not()) {
                    navigateToNotificationPermissionRationale()
                }
            }
        }
    }
}
