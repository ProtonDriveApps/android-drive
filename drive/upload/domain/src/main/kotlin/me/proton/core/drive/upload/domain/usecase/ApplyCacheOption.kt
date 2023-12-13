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

package me.proton.core.drive.upload.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.usecase.MoveToCache
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class ApplyCacheOption @Inject constructor(
    private val moveToCache: MoveToCache,
    private val getPermanentFolder: GetPermanentFolder,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<Unit> = coRunCatching {
        with(uploadFileLink) {
            when (cacheOption) {
                CacheOption.NONE -> deleteAll(userId, volumeId, draftRevisionId)
                CacheOption.ALL -> moveToCache(userId, volumeId, draftRevisionId)
            }
        }
    }

    private suspend fun deleteAll(userId: UserId, volumeId: VolumeId, revisionId: String) {
        getPermanentFolder(
            userId = userId,
            volumeId = volumeId.id,
            revisionId = revisionId,
            coroutineContext = FileScope.coroutineContext,
        ).deleteRecursively()
    }
}
