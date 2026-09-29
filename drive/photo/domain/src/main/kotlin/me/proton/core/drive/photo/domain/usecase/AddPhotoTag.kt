/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKUpdatePhotos
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.sdk.entity.NodeResultPair
import javax.inject.Inject

class AddPhotoTag @Inject constructor(
    private val addPhotoTagLegacy: AddPhotoTagLegacy,
    private val addPhotoTagSdk: AddPhotoTagSdk,
    private val getShare: GetShare,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(fileId: FileId, tags: Set<PhotoTag>) = coRunCatching {
        checkTags(tags)
        val share = getShare(fileId.shareId).toResult().getOrThrow()
        invoke(share.volumeId, fileId, tags).getOrThrow()
    }

    suspend operator fun invoke(volumeId: VolumeId, fileId: FileId, tags: Set<PhotoTag>) = coRunCatching {
        checkTags(tags)
        if (getFeatureFlag(driveAndroidSDKUpdatePhotos(fileId.userId)).on) {
            val nodeUid = fileId.nodeUid(volumeId)
            val resultPair = addPhotoTagSdk(fileId.userId, nodeUid, tags)
                .getOrThrow().first { pair -> pair.nodeUid == nodeUid }
            if (resultPair is NodeResultPair.Failure) {
                throw resultPair.error
            }
        } else {
            addPhotoTagLegacy(volumeId, fileId, tags).getOrThrow()
        }
    }

    private fun checkTags(tags: Set<PhotoTag>) {
        check(PhotoTag.Favorites !in tags) {
            "Favorite cannot be added with AddPhotoTag use AddPhotoFavorite"
        }
        check(tags.isNotEmpty()) { "Tags set should not be empty" }
    }
}
