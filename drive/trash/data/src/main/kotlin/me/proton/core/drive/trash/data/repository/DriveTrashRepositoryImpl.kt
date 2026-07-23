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

package me.proton.core.drive.trash.data.repository

import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKTrashNode
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKTrashOperations
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class DriveTrashRepositoryImpl @Inject constructor(
    private val legacy: DriveTrashRepositoryLegacy,
    private val sdk: DriveTrashRepositorySdk,
    private val getFeatureFlag: GetFeatureFlag,
    private val linkTrashRepository: LinkTrashRepository,
) : DriveTrashRepository {

    override suspend fun sendToTrash(
        userId: UserId,
        volumeId: VolumeId,
        links: List<LinkId>,
    ): Map<LinkId, DataResult<Unit>> =
        if (getFeatureFlag(driveAndroidSDKTrashNode(userId)).on) {
            sdk.sendToTrash(userId, volumeId, links)
        } else {
            legacy.sendToTrash(userId, volumeId, links)
        }

    override suspend fun restoreFromTrash(
        userId: UserId,
        volumeId: VolumeId,
        links: List<LinkId>,
    ): Map<LinkId, DataResult<Unit>> =
        if (getFeatureFlag(driveAndroidSDKTrashOperations(userId)).on) {
            sdk.restoreFromTrash(userId, volumeId, links)
        } else {
            legacy.restoreFromTrash(userId, volumeId, links)
        }

    override suspend fun emptyTrash(userId: UserId, volumeId: VolumeId) {
        if (getFeatureFlag(driveAndroidSDKTrashOperations(userId)).on) {
            sdk.emptyTrash(userId, volumeId)
        } else {
            legacy.emptyTrash(userId, volumeId)
        }
        linkTrashRepository.markTrashedLinkAsDeleted(userId, volumeId)
    }

    override suspend fun deleteItemsFromTrash(
        userId: UserId,
        volumeId: VolumeId,
        links: List<LinkId>,
    ): Map<LinkId, DataResult<Unit>> =
        if (getFeatureFlag(driveAndroidSDKTrashOperations(userId)).on) {
            sdk.deleteItemsFromTrash(userId, volumeId, links)
        } else {
            legacy.deleteItemsFromTrash(userId, volumeId, links)
        }

    override suspend fun fetchTrashContent(
        userId: UserId,
        volumeId: VolumeId,
        pageIndex: Int,
        pageSize: Int,
    ): Result<Pair<List<Link>, SaveAction>> = legacy.fetchTrashContent(userId, volumeId, pageIndex, pageSize)
}
