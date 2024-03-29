/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.file.base.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.changeParent
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.extension.moveTo
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class MoveToCache @Inject constructor(
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ) = mutex.withLock {
        val permanentFolder = getPermanentFolder(userId, volumeId.id, revisionId, coroutineContext)
        val cacheFolder = getCacheFolder(userId, volumeId.id, revisionId, coroutineContext)
        if (permanentFolder.exists()) {
            permanentFolder.moveTo(cacheFolder)
        }
    }

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        files: List<File>,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ) = mutex.withLock {
        val cacheFolder = getCacheFolder(userId, volumeId.id, revisionId, coroutineContext)
        files.forEach { file ->
            file.changeParent(cacheFolder)
        }
    }
}
