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

package me.proton.core.drive.drivelink.trash.domain.usecase

import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class GetDecryptedTrashedDriveLinks @Inject constructor(
    private val getTrashedDriveLinks: GetTrashedDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
) {

    operator fun invoke(shareId: ShareId) =
        getTrashedDriveLinks(shareId)
            .mapCatching { driveLinks ->
                decryptDriveLinks(driveLinks)
            }
}
