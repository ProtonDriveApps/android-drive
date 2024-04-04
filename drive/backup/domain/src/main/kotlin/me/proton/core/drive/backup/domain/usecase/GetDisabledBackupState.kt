/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject

class GetDisabledBackupState @Inject constructor(
    private val getAllBuckets: GetAllBuckets,
    private val configurationProvider: ConfigurationProvider,
) {
    operator fun invoke(): Flow<BackupState> = getAllBuckets().map { bucketEntries ->
        BackupState(
            isBackupEnabled = false,
            hasDefaultFolder = bucketEntries?.any { entry ->
                entry.bucketName == configurationProvider.backupDefaultBucketName
            },
            backupStatus = null,
        )
    }
}
