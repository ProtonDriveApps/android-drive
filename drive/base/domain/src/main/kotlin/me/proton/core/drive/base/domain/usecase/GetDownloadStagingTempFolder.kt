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
package me.proton.core.drive.base.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.VolumeWithRevision
import me.proton.core.drive.base.domain.extension.volumeWithRevision
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.drive.sdk.entity.RevisionUid
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetDownloadStagingTempFolder @Inject constructor(
    private val storageLocationProvider: StorageLocationProvider,
) {
    suspend operator fun invoke(
        userId: UserId,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) = withContext(coroutineContext) {
        storageLocationProvider.getPermanentTempFolder(userId, path = "download")
    }

    suspend operator fun invoke(
        userId: UserId,
        volumeId: String,
        revisionId: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) = withContext(coroutineContext) {
        storageLocationProvider.getPermanentTempFolder(
            userId = userId,
            path = "download/" + VolumeWithRevision(volumeId, revisionId).path
        )
    }

    suspend operator fun invoke(
        userId: UserId,
        revisionUid: RevisionUid,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) = withContext(coroutineContext) {
        storageLocationProvider.getPermanentTempFolder(
            userId = userId,
            path = "download/" + revisionUid.volumeWithRevision.path
        )
    }
}
