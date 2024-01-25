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

package me.proton.core.drive.file.base.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.extension.moveTo
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MoveRevisionTo @Inject constructor(
    private val getPermanentFolder: GetPermanentFolder
) {
    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        fromRevisionId: String,
        toRevisionId: String,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ) {
        val sourceFolder = getPermanentFolder(userId, volumeId.id, fromRevisionId, coroutineContext)
        require(sourceFolder.exists()) {
            "Source folder does not exist: ${sourceFolder.path}"
        }
        val destinationFolder = getPermanentFolder(userId, volumeId.id, toRevisionId, coroutineContext)
        sourceFolder.moveTo(destinationFolder)
    }
}
