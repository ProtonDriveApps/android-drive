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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.domain.usecase.CancelAllUpload
import javax.inject.Inject

class CancelFiles @Inject constructor(
    private val backupFileRepository: BackupFileRepository,
    private val configurationProvider: ConfigurationProvider,
    private val cancelAllUpload: CancelAllUpload,
) {
    suspend operator fun invoke(userId: UserId, bucketId: Int) = coRunCatching {
        val count = configurationProvider.dbPageSize
        var loaded: Int
        var fromIndex = 0
        do {
            val files = backupFileRepository.getFiles(userId, bucketId, fromIndex, count)
            fromIndex += files.size
            loaded = files.size
            cancelAllUpload(userId, files.map { file -> file.uriString })
        } while (loaded == count)
        fromIndex
    }
}
