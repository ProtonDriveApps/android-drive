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

package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.drive.sdk.entity.AvailableNameTarget
import me.proton.drive.sdk.entity.NodeUid
import javax.inject.Inject

class AvoidDuplicateFileNameSdk @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
) {
    suspend operator fun invoke(
        userId: UserId,
        parentFolderUid: NodeUid,
        name: String,
        target: AvailableNameTarget = AvailableNameTarget.FILE,
    ): Result<String> = coRunCatching {

        protonDriveClientProvider
            .getOrCreate(userId)
            .getOrThrow()
            .getAvailableName(
                parentFolderUid = parentFolderUid,
                name = name,
                target = target,
            )
    }
}
