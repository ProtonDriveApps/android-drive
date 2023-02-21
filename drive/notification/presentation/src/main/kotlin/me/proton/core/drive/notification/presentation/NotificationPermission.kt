/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.notification.presentation

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission(
) {
    if (Build.VERSION.SDK_INT >= TIRAMISU) {
        NotificationPermission(
            permissionState = rememberPermissionState(permission = POST_NOTIFICATIONS),
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(TIRAMISU)
@Composable
fun NotificationPermission(
    permissionState: PermissionState,
) {
    LaunchedEffect(permissionState) {
        when (val status = permissionState.status) {
            PermissionStatus.Granted -> Unit
            is PermissionStatus.Denied -> if (status.shouldShowRationale.not()) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}
