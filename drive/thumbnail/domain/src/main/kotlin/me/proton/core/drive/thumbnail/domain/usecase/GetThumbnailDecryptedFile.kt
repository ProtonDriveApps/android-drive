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

package me.proton.core.drive.thumbnail.domain.usecase

import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.file.base.domain.extension.nameDecFile
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetThumbnailDecryptedFile @Inject constructor(
    private val getPermanentFolder: GetPermanentFolder,
    private val getCacheFolder: GetCacheFolder,
) {

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        type: ThumbnailType,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ) = withContext(coroutineContext) {
        type.getDecFileIn(
            getPermanentFolder(userId, volumeId.id, revisionId),
            getCacheFolder(userId, volumeId.id, revisionId),
        )
    }

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        type: ThumbnailType,
        inCacheFolder: Boolean,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ): File = withContext(coroutineContext) {
        File(
            if (inCacheFolder) {
                getCacheFolder(userId, volumeId.id, revisionId)
            } else {
                getPermanentFolder(userId, volumeId.id, revisionId)
            },
            type.nameDecFile
        )
    }

    private fun ThumbnailType.getDecFileIn(vararg folders: File): File? =
        folders
            .map { folder -> File(folder, nameDecFile) }
            .firstOrNull { file -> file.exists() }
}
