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
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.effectiveMaxDriveSpace
import me.proton.core.drive.base.domain.extension.effectiveUsedDriveSpace
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.user.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.math.abs

class HasEnoughAvailableSpace @Inject constructor(
    private val getUploadFileSize: GetUploadFileSize,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        userId: UserId,
        uriStrings: List<String>,
        onNotEnough: (suspend (Bytes) -> Unit)? = null,
    ): Boolean {
        val uploadSize = uriStrings.sumOf { uriString -> getUploadFileSize(uriString).value }
        return hasEnoughDriveSpace(userId, uploadSize, onNotEnough)
    }

    @JvmName("hasEnoughAvailableSpaceWithDescription")
    suspend operator fun invoke(
        userId: UserId,
        uploadFileDescriptions: List<UploadFileDescription>,
        onNotEnough: (suspend (Bytes) -> Unit)? = null,
    ): Boolean {
        val uploadSize = uploadFileDescriptions.sumOf { uploadFileDescription ->
            getUploadFileSize(uploadFileDescription).value
        }
        return hasEnoughDriveSpace(userId, uploadSize, onNotEnough)
    }

    private suspend fun hasEnoughDriveSpace(
        userId: UserId,
        uploadSize: Long,
        onNotEnough: (suspend (Bytes) -> Unit)?
    ) = userRepository.getUser(userId).let { user ->
        val availableSpace =
            user.effectiveMaxDriveSpace.value - user.effectiveUsedDriveSpace.value - uploadSize
        (availableSpace >= 0).also { hasEnoughSpace ->
            if (!hasEnoughSpace) onNotEnough?.invoke(Bytes(abs(availableSpace)))
        }
    }
}
