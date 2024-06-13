/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.drivelink.domain.usecase

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingEditingDisabled
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linknode.domain.usecase.GetLinkAncestors
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetHighestSharePermissions
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class UpdateSharePermissions @Inject constructor(
    private val getShare: GetShare,
    private val getLinkAncestors: GetLinkAncestors,
    private val getHighestSharePermissions: GetHighestSharePermissions,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(driveLink: DriveLink): DriveLink = driveLink.takeIf { link ->
        val share = getShare(link.shareId).toResult().getOrNull(SHARING, "Cannot find share")
        share?.type == Share.Type.STANDARD
    }?.let { link ->
        val sharePermissions = getPermissions(link.id)
        when (link) {
            is DriveLink.Folder -> link.copy(sharePermissions = sharePermissions)
            is DriveLink.File -> link.copy(sharePermissions = sharePermissions)
        }
    } ?: driveLink

    private suspend fun getPermissions(
        linkId: LinkId,
    ): Permissions = getLinkAncestors(linkId).toResult().getOrNull()?.mapNotNull { link ->
        link.sharingDetails?.shareId
    }.orEmpty().let { shareIds ->
        getHighestSharePermissions(shareIds).getOrNull(SHARING, "Cannot get share permissions")
    }?.let { permissions ->
        if (!permissions.isAdmin) {
            permissions.takeUnless {
                getFeatureFlag(driveSharingEditingDisabled(linkId.userId)).state == ENABLED
            }
        } else {
            permissions
        }
    } ?: Permissions.viewer
}
