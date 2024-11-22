/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.share.crypto.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.extension.transformSuccess
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.photo.CreatePhotoInfo
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.onDisabledOrNotFound
import me.proton.core.drive.feature.flag.domain.extension.onEnabled
import me.proton.core.drive.feature.flag.domain.usecase.WithFeatureFlag
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.exception.ShareException
import me.proton.core.drive.share.domain.usecase.GetAddressId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.user.domain.entity.AddressId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePhotoShare @Inject constructor(
    private val createPhotoInfo: CreatePhotoInfo,
    private val photoRepository: PhotoRepository,
    private val getOrCreateMainShare: GetOrCreateMainShare,
    private val getShare: GetShare,
    private val getShares: GetShares,
    private val getPhotoShare: GetPhotoShare,
    private val withFeatureFlag: WithFeatureFlag,
    private val getAddressId: GetAddressId,
) {
    operator fun invoke(userId: UserId): Flow<DataResult<Share>> =
        getOrCreateMainShare(userId)
            .distinctUntilChanged()
            .transformSuccess { result ->
                emitAll(
                    invoke(
                        userId = userId,
                        volumeId = result.value.volumeId,
                    )
                )
            }

    operator fun invoke(userId: UserId, volumeId: VolumeId): Flow<DataResult<Share>> = flow {
        try {
            withFeatureFlag(FeatureFlagId.drivePhotosUploadDisabled(userId)) { featureFlag ->
                featureFlag
                    .onDisabledOrNotFound {
                        val addressId = getAddressId(userId, volumeId).getOrThrow()
                        val photoShareId = createPhotoShare(userId, volumeId, addressId)
                        emitAll(getShare(photoShareId))
                    }
                    .onEnabled {
                        emit(
                            DataResult.Error.Local(
                                message = "Disabled by DisableDrivePhotosUpload feature flag",
                                cause = ShareException.CreatingShareNotAllowed(userId, Share.Type.PHOTO),
                            )
                        )
                    }
            }
        } catch (e: ApiException) {
            emit(e.toDataResult())
        } catch (e: Exception) {
            emit(DataResult.Error.Local("Cannot read share", e))
        }
    }

    private suspend fun createPhotoShare(
        userId: UserId,
        volumeId: VolumeId,
        addressId: AddressId,
    ): ShareId = coRunCatching {
        photoRepository.createPhotoShareWithRootLink(
            userId = userId,
            photoInfo = createPhotoInfo(userId, volumeId, addressId).getOrThrow(),
        )
    }.fold(
        onSuccess = { (shareId, _) -> ShareId(userId, shareId) },
        onFailure = { error ->
            if (error.hasProtonErrorCode(ProtonApiCode.ALREADY_EXISTS)) {
                getShares(userId, Share.Type.PHOTO, refresh = flowOf(true)).toResult().getOrThrow()
                getPhotoShare(userId, volumeId).toResult().getOrThrow().id
            } else {
                throw error
            }
        }
    )
}
