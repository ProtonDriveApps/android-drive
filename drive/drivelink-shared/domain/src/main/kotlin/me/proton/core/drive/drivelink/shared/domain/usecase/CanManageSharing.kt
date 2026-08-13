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

package me.proton.core.drive.drivelink.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.orOwner
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetVolumeType
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingAdminPermissions
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.Volume
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CanManageSharing @Inject constructor(
    private val getDriveLink: GetDriveLink,
    private val getVolumeType: GetVolumeType,
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
) {
    operator fun invoke(linkId: LinkId): Flow<Boolean> =
        getDriveLink(linkId)
            .mapSuccessValueOrNull()
            .filterNotNull()
            .flatMapLatest { driveLink ->
                invoke(
                    userId = driveLink.userId,
                    volumeType = getVolumeType(driveLink).getOrThrow(),
                    permissions = driveLink.sharePermissions.orOwner,
                )
            }
            .catch { emit(false) }

    operator fun invoke(
        userId: UserId,
        volumeType: Volume.Type,
        permissions: Permissions,
    ): Flow<Boolean> = flow {
        when (volumeType) {
            Volume.Type.PHOTO -> emit(permissions.isOwner)
            else -> emitAll(
                getFeatureFlagFlow(
                    featureFlagId = driveSharingAdminPermissions(userId),
                    emitNotFoundInitially = false,
                )
                    .map { featureFlag ->
                        permissions.isOwner || (permissions.isAdmin && featureFlag.on)
                    }
            )
        }
    }.catch { emit(false) }
}
