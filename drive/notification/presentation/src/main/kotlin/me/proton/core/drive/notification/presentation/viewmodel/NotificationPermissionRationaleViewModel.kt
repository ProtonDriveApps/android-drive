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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.notification.domain.usecase.RejectNotificationPermissionRationale
import me.proton.core.drive.notification.presentation.viewevent.NotificationPermissionRationaleViewEvent
import me.proton.core.drive.notification.presentation.viewstate.NotificationPermissionRationaleViewState
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel
class NotificationPermissionRationaleViewModel @Inject constructor(
    private val rejectNotificationPermissionRationale: RejectNotificationPermissionRationale,
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val rationaleContext = RationaleContext.valueOf(
        savedStateHandle.get<String>(RATIONALE_CONTEXT) ?: RationaleContext.DEFAULT.name
    )

    val initialViewState = when (rationaleContext) {
        RationaleContext.BACKUP -> NotificationPermissionRationaleViewState(
            title = appContext.getString(I18N.string.notification_permission_rationale_title_backup),
            description = appContext.getString(I18N.string.notification_permission_rationale_description_backup),
            acceptActionTitle = appContext.getString(I18N.string.notification_permission_rationale_action_accept),
            rejectActionTitle = appContext.getString(I18N.string.notification_permission_rationale_action_reject),
        )
        else -> error("Implementation missing for rationale context: $rationaleContext")
    }

    fun viewEvent(
        permissionState: PermissionState,
        runAction: RunAction,
        dismiss: () -> Unit,
    ) : NotificationPermissionRationaleViewEvent = object : NotificationPermissionRationaleViewEvent {
        override val onAccept = {
            accept(permissionState)
            dismiss()
            //TODO: see why runAction is not working in tests
            //runAction { accept(permissionState) }
        }
        override val onReject = { runAction { reject() } }
    }

    private fun accept(permissionState: PermissionState) {
        if (permissionState.status.shouldShowRationale.not()) {
            permissionState.launchPermissionRequest()
        }
    }

    private fun reject() {
        viewModelScope.launch {
            rejectNotificationPermissionRationale(userId)
                .getOrNull(LogTag.NOTIFICATION, "Reject notification permission rationale failed")
        }
    }

    companion object {
        const val RATIONALE_CONTEXT = "rationaleContext"
    }

    enum class RationaleContext {
        BACKUP,
        DEFAULT,
    }
}
