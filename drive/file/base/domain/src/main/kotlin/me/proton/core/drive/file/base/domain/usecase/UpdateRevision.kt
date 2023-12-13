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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.file.base.domain.entity.PhotoAttributes
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.link.domain.entity.FileId
import javax.inject.Inject

class UpdateRevision @Inject constructor(
    private val fileRepository: FileRepository,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(
        fileId: FileId,
        revisionId: String,
        manifestSignature: String,
        signatureAddress: String,
        blockNumber: Long,
        xAttr: String,
        photoAttributes: PhotoAttributes?,
    ) = coRunCatching {
        updateEventAction(fileId.shareId) {
            fileRepository.updateRevision(
                fileId = fileId,
                revisionId = revisionId,
                manifestSignature = manifestSignature,
                signatureAddress = signatureAddress,
                blockNumber = blockNumber,
                xAttr = xAttr,
                photoAttributes = photoAttributes,
            ).getOrThrow()
        }
    }
}
