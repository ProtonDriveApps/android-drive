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
package me.proton.core.drive.share.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.repository.ShareRepository
import javax.inject.Inject

class DeleteShare @Inject constructor(
    private val shareRepository: ShareRepository,
) {

    suspend operator fun invoke(
        shareId: ShareId,
        locallyOnly: Boolean = false,
        force: Boolean = false,
    ): Result<Unit> = coRunCatching {
        if (locallyOnly) {
            check(!force) { "force only apply when locallyOnly is false" }
        }
        if (force) {
            val share = shareRepository.getShareFlow(shareId).toResult().getOrThrow()
            check(share.type == Share.Type.STANDARD) { "force must only be used with standard share" }
        }
        shareRepository.deleteShare(
            shareId = shareId,
            locallyOnly = locallyOnly,
            force = force,
        )
    }
}
