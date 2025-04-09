/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.photos.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.android.drive.photos.domain.usecase.EnablePhotosBackup
import me.proton.android.drive.photos.domain.usecase.SetupPhotosBackup
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.usecase.StartBackup
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class EnablePhotosBackupImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val setupPhotosBackup: SetupPhotosBackup,
    private val backupManager: BackupManager,
    private val startBackup: StartBackup,
    private val permissionsManager: BackupPermissionsManager,
    private val configurationProvider: ConfigurationProvider,
    private val announceEvent: AnnounceEvent,
) : EnablePhotosBackup {

    override suspend operator fun invoke(folderId: FolderId): Result<PhotoBackupState> = coRunCatching {
        when (val permissions = permissionsManager.getBackupPermissions(refresh = true)) {
            is BackupPermissions.Granted -> enableBackup(
                folderId = folderId,
                allFolders = permissions.partial,
            )
            else -> throw SecurityException(appContext.getString(I18N.string.photos_error_missing_permissions))
        }
    }

    private suspend fun enableBackup(
        folderId: FolderId,
        allFolders: Boolean = false
    ): PhotoBackupState = if (backupManager.isEnabled(folderId).first().not()) {
        val folderName = configurationProvider.backupDefaultBucketName
        val folderFilter: (String) -> Boolean = if (allFolders) {
            { _ -> true }
        } else {
            val folderNames = configurationProvider.backupAdditionalBucketNames
            val names = listOf(folderName) + folderNames
            { name -> name in names }
        }
        setupPhotosBackup(folderId, folderFilter).getOrThrow().let { results ->
            if (results.isEmpty()) {
                PhotoBackupState.NoFolder(folderName)
            } else {
                PhotoBackupState.Enabled(
                    folderNames = results.map { result -> result.folderName },
                    backupFolders = results.map { result -> result.backupFolder },
                )
            }
        }.also { state ->
            if (state is PhotoBackupState.Enabled) {
                announceEvent(folderId.userId, Event.BackupEnabled(folderId))
            }
            startBackup(folderId)
        }
    } else {
        PhotoBackupState.Disabled
    }
}
