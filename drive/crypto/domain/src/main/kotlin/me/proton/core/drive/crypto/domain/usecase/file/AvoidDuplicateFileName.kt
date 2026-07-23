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

package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKAvailableName
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class AvoidDuplicateFileName @Inject constructor(
    private val avoidDuplicateFileNameLegacy: AvoidDuplicateFileNameLegacy,
    private val avoidDuplicateFileNameSdk: AvoidDuplicateFileNameSdk,
    private val getFeatureFlag: GetFeatureFlag,
    private val getShare: GetShare,
) {
    suspend operator fun invoke(
        fileName: String,
        parentFolderId: FolderId,
        folderHashKey: NodeHashKey
    ): Result<String> = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKAvailableName(parentFolderId.userId)).on) {
            val share = getShare(parentFolderId.shareId).toResult().getOrThrow()
            avoidDuplicateFileNameSdk(
                userId = parentFolderId.userId,
                parentFolderUid = parentFolderId.nodeUid(share.volumeId),
                name = fileName,
            ).getOrThrow()
        } else {
            avoidDuplicateFileNameLegacy(
                fileName = fileName,
                parentFolderId = parentFolderId,
                folderHashKey = folderHashKey,
            ).getOrThrow()
        }
    }
}
