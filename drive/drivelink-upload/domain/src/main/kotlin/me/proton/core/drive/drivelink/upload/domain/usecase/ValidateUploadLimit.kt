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
package me.proton.core.drive.drivelink.upload.domain.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import javax.inject.Inject

class ValidateUploadLimit @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
    private val configurationProvider: ConfigurationProvider,
    private val uploadWorkManager: UploadWorkManager,
) {
    suspend operator fun invoke(userId: UserId, newUploads: Int): Result<Unit> = coRunCatching {
        if (configurationProvider.validateUploadLimit) {
            val uploading = linkUploadRepository.getUploadFileLinks(userId).first().size
            if (uploading + newUploads > configurationProvider.uploadLimitThreshold) {
                uploadWorkManager.broadcastUploadLimitReached(userId)
                throw RuntimeException("Upload limit reached")
            }
        }
    }
}
