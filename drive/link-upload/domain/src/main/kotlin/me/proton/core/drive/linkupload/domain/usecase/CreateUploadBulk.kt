/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.linkupload.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderContext
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class CreateUploadBulk @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
) {
    suspend operator fun invoke(
        parentFolderContext: FolderContext,
        shareId: ShareId,
        uploadFileDescriptions: List<UploadFileDescription>,
        shouldDeleteSource: Boolean = false,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
    ): Result<UploadBulk> = coRunCatching {
        linkUploadRepository.insertUploadBulk(
            UploadBulk(
                parentFolderContext = parentFolderContext,
                shareId = shareId,
                uploadFileDescriptions = uploadFileDescriptions,
                shouldDeleteSourceUri = shouldDeleteSource,
                networkTypeProviderType = networkTypeProviderType,
                shouldAnnounceEvent = shouldAnnounceEvent,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
            )
        )
    }
}
