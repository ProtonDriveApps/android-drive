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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.photos.presentation.viewevent.BackupPermissionsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.BackupPermissionsEffect
import me.proton.android.drive.photos.presentation.viewstate.BackupPermissionsViewState
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.notification.presentation.component.NotificationPermission

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun BackupPermissions(
    viewState: BackupPermissionsViewState,
    viewEvent: BackupPermissionsViewEvent,
    navigateToNotificationPermissionRationale: () -> Unit,
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = viewState.permissions,
        onPermissionsResult = viewEvent.onPermissionResult,
    )
    val backupPermissions = if (permissionsState.allPermissionsGranted) {
        BackupPermissions.Granted()
    } else if (permissionsState.permissions.any { it.status.isGranted }) {
        BackupPermissions.Granted(partial = true)
    } else {
        BackupPermissions.Denied(permissionsState.shouldShowRationale)
    }
    // Keyed on the state and not on allPermissionsGranted, which stays false when access to
    // selected photos only is granted.
    LaunchedEffect(backupPermissions) {
        viewEvent.onPermissionsChanged(backupPermissions)
    }
    LaunchedEffect(viewState, LocalContext.current) {
        viewState.effect
            .onEach { effect ->
                when (effect) {
                    BackupPermissionsEffect.RequestPermission -> permissionsState.launchMultiplePermissionRequest()
                }
            }
            .launchIn(this)
    }
    if (viewState.shouldRequestNotificationPermission && backupPermissions is BackupPermissions.Granted) {
        NotificationPermission(
            shouldShowRationale = true,
            navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale
        )
    }
}
