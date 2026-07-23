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

package me.proton.core.drive.drivelink.rename.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKRenameNode
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class RenameLink @Inject constructor(
    private val renameLinkLegacy: RenameLinkLegacy,
    private val renameLinkSdk: RenameLinkSdk,
    private val getFeatureFlag: GetFeatureFlag,
    private val getLink: GetLink,
    private val getShare: GetShare,
    private val validateLinkName: ValidateLinkName,
) {
    suspend operator fun invoke(
        parentFolder: Link.Folder,
        link: Link,
        linkName: String,
    ): Result<Unit> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKRenameNode(link.userId)).on) {
            val share = getShare(link.id.shareId).toResult().getOrThrow()
            renameLinkSdk(
                userId = link.userId,
                nodeUid = link.nodeUid(share.volumeId),
                linkName = linkName,
            ).getOrThrow()
        } else {
            renameLinkLegacy(
                parentFolder = parentFolder,
                link = link,
                linkName = linkName,
            ).getOrThrow()
        }
    }

    suspend operator fun invoke(
        rootFolder: Link.Folder,
        folderName: String,
        nameValidator: (String) -> String = { validateLinkName(folderName).getOrThrow() },
    ): Result<Unit> = coRunCatching {
        require(rootFolder.parentId == null) { "Use this method only for renaming a root folder" }

        if (getFeatureFlag(driveAndroidSDKRenameNode(rootFolder.userId)).on) {
            val share = getShare(rootFolder.id.shareId).toResult().getOrThrow()
            renameLinkSdk(
                userId = rootFolder.userId,
                nodeUid = rootFolder.nodeUid(share.volumeId),
                linkName = folderName,
            ).getOrThrow()
        } else {
            renameLinkLegacy(
                rootFolder = rootFolder,
                folderName = folderName,
                nameValidator = nameValidator,
            ).getOrThrow()
        }
    }

    suspend operator fun invoke(
        linkId: LinkId,
        linkName: String,
    ): Result<Unit> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKRenameNode(linkId.userId)).on) {
            val share = getShare(linkId.shareId).toResult().getOrThrow()
            renameLinkSdk(
                userId = linkId.userId,
                nodeUid = linkId.nodeUid(share.volumeId),
                linkName = linkName,
            ).getOrThrow()
        } else {
            val link = getLink(linkId).toResult().getOrThrow()
            val parentId = requireNotNull(link.parentId) { "Parent must not be null" }
            when (val parent = getLink(parentId).toResult().getOrThrow()) {
                is Link.Folder -> renameLinkLegacy(
                    parentFolder = parent,
                    link = link,
                    linkName = linkName,
                ).getOrThrow()
                else -> error("Album should not be renamed through this endpoint")
            }
        }
    }

    suspend operator fun invoke(
        rootFolderId: FolderId,
        folderName: String,
        nameValidator: (String) -> String = { validateLinkName(folderName).getOrThrow() },
    ): Result<Unit> = coRunCatching {
        invoke(
            rootFolder = getLink(rootFolderId).toResult().getOrThrow(),
            folderName = folderName,
            nameValidator = nameValidator,
        ).getOrThrow()
    }
}
