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

package me.proton.android.drive.provider

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.usecase.GetVolumeType
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.provider.ProtonSdkClientProvider
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolume
import me.proton.drive.sdk.ProtonSdkClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppProtonSdkClientProvider @Inject constructor(
    private val getVolumeType: GetVolumeType,
    private val getVolume: GetVolume,
    private val driveClientProvider: AppProtonDriveClientProvider,
    private val photosClientProvider: AppProtonPhotosClientProvider,
) : ProtonSdkClientProvider {

    override suspend fun getOrCreate(
        linkId: LinkId,
    ): Result<ProtonSdkClient> = coRunCatching {
        getOrCreate(
            userId = linkId.userId,
            volumeType = getVolumeType(linkId).getOrThrow(),
        ).getOrThrow()
    }

    override suspend fun getOrCreate(
        userId: UserId,
        volumeId: VolumeId
    ): Result<ProtonSdkClient> = coRunCatching {
        getOrCreate(
            userId = userId,
            volumeType = getVolume(userId, volumeId).toResult().getOrThrow().type,
        ).getOrThrow()
    }

    override suspend fun getOrCreate(
        userId: UserId,
        volumeType: Volume.Type,
    ): Result<ProtonSdkClient> = coRunCatching {
        when (volumeType) {
            Volume.Type.UNKNOWN -> error("Cannot create sdk client for unknown volume type")
            Volume.Type.REGULAR -> driveClientProvider.getOrCreate(userId).getOrThrow()
            Volume.Type.PHOTO -> photosClientProvider.getOrCreate(userId).getOrThrow()
        }
    }

    suspend fun remove(userId: UserId) {
        driveClientProvider.remove(userId)
        photosClientProvider.remove(userId)
    }


}
