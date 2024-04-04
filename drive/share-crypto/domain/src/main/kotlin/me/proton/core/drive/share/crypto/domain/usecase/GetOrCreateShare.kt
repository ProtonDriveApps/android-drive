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
package me.proton.core.drive.share.crypto.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetOrCreateShare @Inject constructor(
    private val getLink: GetLink,
    private val getShare: GetShare,
    private val createShare: CreateShare,
) {
    operator fun invoke(volumeId: VolumeId, linkId: LinkId): Flow<DataResult<Share>> = flow {
        val link = getLink(linkId).toResult().getOrNull()
        link?.sharingDetails?.shareId?.let { shareId ->
            emitAll(getShare(shareId))
        } ?: emitAll(createShare(volumeId, linkId))
    }
}
