/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKLeaveSharedNode
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.share.user.domain.usecase.DeleteLocalSharedWithMe
import me.proton.core.drive.volume.domain.extension.volumeId
import javax.inject.Inject

class LeaveShare @Inject constructor(
    private val leaveShareLegacy: LeaveShareLegacy,
    private val leaveShareSdk: LeaveShareSdk,
    private val getFeatureFlag: GetFeatureFlag,
    private val deleteShare: DeleteShare,
    private val deleteLocalSharedWithMe: DeleteLocalSharedWithMe,
) {

    suspend operator fun invoke(driveLink: DriveLink): Result<Boolean> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKLeaveSharedNode(driveLink.userId)).on) {
            leaveShareSdk(
                userId = driveLink.userId,
                nodeUid = driveLink.id.nodeUid(driveLink.volumeId),
            ).onSuccess { successful ->
                if (successful) {
                    deleteShare(
                        shareId = driveLink.id.shareId,
                        locallyOnly = true
                    ).getOrNull(SHARING, "Cannot remove local share")
                    deleteLocalSharedWithMe(
                        volumeId = driveLink.volumeId,
                        linkId = driveLink.id,
                    ).getOrNull(SHARING, "Cannot remove local shared with me listing")
                }
            }.getOrThrow()
        } else {
            leaveShareLegacy(driveLink).getOrThrow()
        }
    }
}
