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

package me.proton.core.drive.folder.create.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKCreateFolder
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.linkId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class CreateFolder @Inject constructor(
    private val createFolderLegacy: CreateFolderLegacy,
    private val createFolderSdk: CreateFolderSdk,
    private val getFeatureFlag: GetFeatureFlag,
    private val getLink: GetLink,
    private val getShare: GetShare,
) {
    suspend operator fun invoke(
        parentFolder: Link.Folder,
        folderName: String,
        shouldUpdateEvent: Boolean = true,
    ): Result<Pair<String, FolderId>> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKCreateFolder(parentFolder.userId)).on) {
            val share = getShare(parentFolder.id.shareId).toResult().getOrThrow()
            createFolderSdk(
                userId = parentFolder.userId,
                parentFolderUid = parentFolder.nodeUid(share.volumeId),
                folderName = folderName,
                shouldUpdateEvent = shouldUpdateEvent,
            ).getOrThrow().let { (name, folderNode) ->
                name to folderNode.linkId(parentFolder.id.shareId)
            }
        } else {
            createFolderLegacy(
                parentFolder = parentFolder,
                folderName = folderName,
                shouldUpdateEvent = shouldUpdateEvent,
            ).getOrThrow()
        }
    }

    suspend operator fun invoke(
        parentFolderId: FolderId,
        folderName: String,
        shouldUpdateEvent: Boolean = true,
    ): Result<Pair<String, FolderId>> = coRunCatching {
        invoke(
            parentFolder = getLink(parentFolderId).toResult().getOrThrow(),
            folderName = folderName,
            shouldUpdateEvent = shouldUpdateEvent,
        ).getOrThrow()
    }
}
