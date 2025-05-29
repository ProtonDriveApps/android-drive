/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.presentation.extension

import android.Manifest
import android.net.Uri
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import me.proton.core.compose.activity.CameraLauncher

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberPermissionCameraLauncher(
    cameraLauncher: CameraLauncher,
    onPermissionResult: (Boolean) -> Unit
): PermissionCameraLauncher {
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA) { result ->
        onPermissionResult(result)
    }
    return PermissionCameraLauncher(cameraLauncher, permissionState)
}

@OptIn(ExperimentalPermissionsApi::class)
data class PermissionCameraLauncher(
    private val cameraLauncher: CameraLauncher,
    private val permissionState: PermissionState,
) {
    fun capture(destinationUri: Uri, onNotFound: () -> Unit) =
        if (permissionState.status == PermissionStatus.Granted) {
            cameraLauncher.captureWithNotFound(destinationUri, onNotFound)
        } else {
            permissionState.launchPermissionRequest()
        }
}
