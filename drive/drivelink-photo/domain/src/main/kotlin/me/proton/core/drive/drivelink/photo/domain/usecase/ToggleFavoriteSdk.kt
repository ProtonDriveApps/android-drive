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
package me.proton.core.drive.drivelink.photo.domain.usecase

import kotlinx.coroutines.flow.toList
import me.proton.core.drive.base.domain.provider.ProtonPhotosClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.drive.sdk.entity.NodeResultPair
import me.proton.drive.sdk.entity.PhotoTagsUpdate
import me.proton.drive.sdk.entity.PhotoTag as SdkPhotoTag
import javax.inject.Inject

class ToggleFavoriteSdk @Inject constructor(
    private val protonPhotosClientProvider: ProtonPhotosClientProvider,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(
        driveLink: DriveLink.File,
    ): Result<List<NodeResultPair>> = coRunCatching {
        val nodeUid = driveLink.id.nodeUid(driveLink.volumeId)
        val update = if (driveLink.isFavorite) {
            PhotoTagsUpdate(nodeUid = nodeUid, tagsToRemove = listOf(SdkPhotoTag.Favorite))
        } else {
            PhotoTagsUpdate(nodeUid = nodeUid, tagsToAdd = listOf(SdkPhotoTag.Favorite))
        }
        updateEventAction(driveLink.userId, nodeUid) {
            protonPhotosClientProvider.getOrCreate(driveLink.userId).getOrThrow()
                .updatePhotos(listOf(update))
                .toList()
        }
    }
}
