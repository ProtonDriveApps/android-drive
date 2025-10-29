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
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.file.base.domain.extension.nameEncFile
import me.proton.core.drive.file.base.domain.usecase.MoveToCache
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import javax.inject.Inject

class ApplyCacheOption @Inject constructor(
    private val moveToCache: MoveToCache,
    private val getPermanentFolder: GetPermanentFolder,
    private val getInternalStorageInfo: GetInternalStorageInfo,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<Unit> = coRunCatching {
        with(uploadFileLink) {
            when (internalStorageLimitConstraint()) {
                CacheOption.NONE -> deleteAll(userId, volumeId, draftRevisionId)
                CacheOption.ALL -> moveToCache(userId, volumeId, draftRevisionId)
                CacheOption.THUMBNAIL_DEFAULT -> {
                    moveToCache(
                        userId = userId,
                        volumeId = volumeId,
                        revisionId = draftRevisionId,
                        files = listOfNotNull(
                            getThumbnailFile(
                                userId = userId,
                                volumeId = volumeId,
                                revisionId = draftRevisionId,
                                thumbnailType = ThumbnailType.DEFAULT,
                            )
                        ),
                    )
                    deleteAll(userId, volumeId, draftRevisionId)
                }
            }
        }
    }

    private suspend fun UploadFileLink.internalStorageLimitConstraint() = when (cacheOption) {
        CacheOption.NONE -> cacheOption
        CacheOption.ALL -> internalStorageLimitConstraint(fileSize = size ?: 0.bytes)
        CacheOption.THUMBNAIL_DEFAULT -> internalStorageLimitConstraint(
            fileSize = getThumbnailFile(
                userId = userId,
                volumeId = volumeId,
                revisionId = draftRevisionId,
                thumbnailType = ThumbnailType.DEFAULT,
            )?.size ?: 0.bytes
        )
    }

    private fun UploadFileLink.internalStorageLimitConstraint(fileSize: Bytes): CacheOption {
        val storageInfo = getInternalStorageInfo().getOrThrow()
        val limit = configurationProvider.cacheInternalStorageLimit
        return if (storageInfo.available - fileSize < limit) {
            CoreLogger.w(
                id.logTag(),
                "Overriding cache option to NONE, local storage is low: ${storageInfo.available} for $fileSize"
            )
            CacheOption.NONE
        } else {
            cacheOption
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

    private suspend fun getThumbnailFile(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        thumbnailType: ThumbnailType,
    ): File? = File(
        getPermanentFolder(
            userId = userId,
            volumeId = volumeId.id,
            revisionId = revisionId,
            coroutineContext = FileScope.coroutineContext,
        ),
        thumbnailType.nameEncFile,
    ).takeIf { file -> file.exists() }
}
