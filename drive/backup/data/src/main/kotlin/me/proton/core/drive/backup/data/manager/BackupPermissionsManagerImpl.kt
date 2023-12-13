/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.backup.data.manager

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import javax.inject.Inject

class BackupPermissionsManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : BackupPermissionsManager {
    override val requiredBackupPermissions: List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        )

        else -> listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }
    private val _permissions = MutableStateFlow(checkForBackupPermissions())
    override val backupPermissions: Flow<BackupPermissions> = _permissions

    override fun getBackupPermissions(refresh: Boolean): BackupPermissions =
        if (refresh) {
            checkForBackupPermissions()
        } else {
            _permissions.value
        }

    override fun onPermissionChanged(permissions: BackupPermissions) {
        _permissions.value = permissions
    }

    private fun checkForBackupPermissions(): BackupPermissions {
        val allPermissionsGranted = requiredBackupPermissions
            .map { permission ->
                ContextCompat.checkSelfPermission(appContext, permission)
            }
            .all { permission -> permission == PERMISSION_GRANTED }
        return if (allPermissionsGranted) {
            BackupPermissions.Granted
        } else {
            BackupPermissions.Denied(false)
        }
    }
}
