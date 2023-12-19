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

package me.proton.core.drive.volume.crypto.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.repository.VolumeRepository
import me.proton.core.drive.volume.domain.usecase.GetVolumes
import javax.inject.Inject

class DeleteLocalVolumes @Inject constructor(
    private val getVolumes: GetVolumes,
    private val getShare: GetShare,
    private val repository: VolumeRepository,
) {

    suspend operator fun invoke(linkIds: List<LinkId>) = coRunCatching {
        linkIds.groupBy { linkId -> linkId.userId }.forEach { (userId, linkIds) ->
            val linkIdStrings =  linkIds.map { linkId -> linkId.id }
            getVolumes(userId, flowOf(false))
                .toResult()
                .getOrThrow()
                .filter { volume ->
                    getShare(ShareId(userId, volume.shareId), flowOf(false))
                        .toResult()
                        .getOrNull()
                        ?.rootLinkId in linkIdStrings
                }
                .forEach { volume ->
                    repository.removeVolume(userId, volume.id)
                }
        }
    }
}
