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
package me.proton.core.drive.upload.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import javax.inject.Inject

class CancelAllUpload @Inject constructor(
    private val uploadWorkManager: UploadWorkManager,
) {
    suspend operator fun invoke(userId: UserId) =
        uploadWorkManager.cancelAll(userId)

    suspend operator fun invoke(userId: UserId, shareId: ShareId) =
        uploadWorkManager.cancelAllByShare(userId, shareId)

    suspend operator fun invoke(userId: UserId, uriStrings: List<String>) =
        uploadWorkManager.cancelAllByUris(userId, uriStrings)
}
