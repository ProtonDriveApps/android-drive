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
package me.proton.core.drive.files.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKMoveOnly
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.LinksResult
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.drive.sdk.entity.NodeResultPair
import me.proton.drive.sdk.entity.NodeUid
import javax.inject.Inject

class ChangeParent @Inject constructor(
    private val changeParentLegacy: ChangeParentLegacy,
    private val changeParentSdk: ChangeParentSdk,
    private val getShare: GetShare,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(
        linkId: LinkId,
        folderId: ParentId,
    ): Result<Unit> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKMoveOnly(linkId.userId)).on) {
            val volumeId = getShare(linkId.shareId).toResult().getOrThrow().volumeId
            val nodeUid = linkId.nodeUid(volumeId)
            val resultPair = changeParentSdk(
                userId = linkId.userId,
                nodeUids = listOf(nodeUid),
                newParentFolderUid = folderId.nodeUid(volumeId),
            ).getOrThrow().first { it.nodeUid == nodeUid }
            if (resultPair is NodeResultPair.Failure) {
                throw resultPair.error
            }
        } else {
            changeParentLegacy(linkId, folderId).getOrThrow()
        }
    }

    suspend operator fun invoke(
        parentId: ParentId,
        linkIds: Set<LinkId>,
    ): Result<LinksResult> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKMoveOnly(parentId.userId)).on) {
            val volumeId = getShare(parentId.shareId).toResult().getOrThrow().volumeId
            val nodeUids = linkIds.associate { linkId -> linkId.nodeUid(volumeId) to linkId }
            changeParentSdk(
                userId = parentId.userId,
                nodeUids = nodeUids.keys.toList(),
                newParentFolderUid = parentId.nodeUid(volumeId),
            ).getOrThrow().toLinksResult(nodeUids)
        } else {
            changeParentLegacy(parentId, linkIds).getOrThrow()
        }
    }

    private fun List<NodeResultPair>.toLinksResult(nodeUids: Map<NodeUid, LinkId>): LinksResult =
        LinksResult(
            results = map { pair ->
                val linkId = nodeUids[pair.nodeUid]?.id
                when (pair) {
                    is NodeResultPair.Success -> LinksResult.LinkResult.Success(linkId)
                    is NodeResultPair.Failure -> LinksResult.LinkResult.Error(
                        linkId = linkId,
                        code = pair.error.error?.primaryCode ?: 0L,
                        error = pair.error.message,
                    )
                }
            },
        )
}
