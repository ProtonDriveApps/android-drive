/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.linkupload.domain.usecase.sdk

import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.parentFolderUid
import me.proton.core.drive.linkupload.domain.extension.userId
import me.proton.core.drive.linkupload.domain.usecase.UpdateName
import me.proton.drive.sdk.ProtonSdkError
import javax.inject.Inject

class ResolveNameConflict @Inject constructor(
    private val protonSdkClientProvider: ProtonDriveClientProvider,
    private val updateName: UpdateName,
) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        data: ProtonSdkError.Data.NodeNameConflict,
    ) = coRunCatching {
        with(uploadFileLink) {
            val client = protonSdkClientProvider.getOrCreate(userId).getOrThrow()
            val availableName = client.getAvailableName(parentFolderUid, name)
            updateName(id, availableName).getOrThrow()
        }
    }
}
