/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.notification.presentation.component

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import me.proton.core.drive.notification.presentation.viewmodel.NotificationPermissionViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission(
    shouldShowRationale: Boolean,
    navigateToNotificationPermissionRationale: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= TIRAMISU) {
        NotificationPermission(
            permissionState = rememberPermissionState(permission = POST_NOTIFICATIONS),
            shouldShowRationale = shouldShowRationale,
            navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(TIRAMISU)
@Composable
fun NotificationPermission(
    permissionState: PermissionState,
    shouldShowRationale: Boolean,
    navigateToNotificationPermissionRationale: () -> Unit,
) {
    val viewModel = hiltViewModel<NotificationPermissionViewModel>()
    val viewEvent = remember {
        viewModel.viewEvent(navigateToNotificationPermissionRationale)
    }
    NotificationPermission(
        permissionState = permissionState,
        shouldShowRationale = shouldShowRationale,
        onPermissionDenied = viewEvent.onPermissionDenied,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(TIRAMISU)
@Composable
fun NotificationPermission(
    permissionState: PermissionState,
    shouldShowRationale: Boolean,
    onPermissionDenied: (PermissionState, Boolean) -> Unit,
) {
    LaunchedEffect(permissionState) {
        when (permissionState.status) {
            PermissionStatus.Granted -> Unit
            is PermissionStatus.Denied -> onPermissionDenied(permissionState, shouldShowRationale)
        }
    }
}
