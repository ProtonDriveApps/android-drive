/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.shareurl.crypto.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.share.CreateShareUrlCustomPasswordInfo
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.extension.userId
import me.proton.core.drive.shareurl.base.domain.repository.ShareUrlRepository
import me.proton.core.drive.shareurl.base.domain.usecase.GetShareUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class UpdateShareUrl @Inject constructor(
    private val getShare: GetShare,
    private val getShareUrl: GetShareUrl,
    private val createShareUrlCustomPasswordInfo: CreateShareUrlCustomPasswordInfo,
    private val shareUrlRepository: ShareUrlRepository,
    private val updateEventAction: UpdateEventAction,
    private val dateTimeFormatter: DateTimeFormatter,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        customPassword: String?,
        expirationDateIso8601: String?,
        updateExpirationDate: Boolean,
    ): Result<ShareUrl> = coRunCatching {
        val shareUrlCustomPasswordInfo = customPassword?.let {
            shareUrlCustomPasswordInfo(volumeId, shareUrlId, customPassword)
        }
        val shareUrlExpirationDurationInfo = if (updateExpirationDate) {
            expirationDateIso8601?.let {
                shareUrlExpirationDurationInfo(expirationDateIso8601)
            } ?: ShareUrlExpirationDurationInfo(expirationDuration = null)
        } else {
            null
        }
        return invoke(
            volumeId = volumeId,
            shareUrlId = shareUrlId,
            shareUrlCustomPasswordInfo = shareUrlCustomPasswordInfo,
            shareUrlExpirationDurationInfo = shareUrlExpirationDurationInfo,
        )
    }

    private suspend operator fun invoke(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo?,
        shareUrlExpirationDurationInfo : ShareUrlExpirationDurationInfo?,
    ): Result<ShareUrl> = coRunCatching {
        updateEventAction(shareUrlId.userId, volumeId) {
            shareUrlRepository.updateShareUrl(
                volumeId = volumeId,
                shareUrlId = shareUrlId,
                shareUrlCustomPasswordInfo = shareUrlCustomPasswordInfo,
                shareUrlExpirationDurationInfo = shareUrlExpirationDurationInfo,
            ).getOrThrow()
        }
    }

    private suspend fun shareUrlCustomPasswordInfo(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        customPassword: String,
    ): ShareUrlCustomPasswordInfo =
        createShareUrlCustomPasswordInfo(
            share = getShare(shareUrlId.shareId).toResult().getOrThrow(),
            shareUrl = getShareUrl(volumeId, shareUrlId).toResult().getOrThrow(),
            customPassword = customPassword,
        ).getOrThrow()

    private fun shareUrlExpirationDurationInfo(
        expirationDateIso8601: String
    ): ShareUrlExpirationDurationInfo {
        val futureInSeconds = dateTimeFormatter.parseFromIso8601String(expirationDateIso8601).getOrThrow().value
        val nowInSeconds = System.currentTimeMillis() / 1000
        require(futureInSeconds >= nowInSeconds) { "Share Url expiration date cannot be in the past" }
        val expirationDuration = futureInSeconds - nowInSeconds + delta
        require(expirationDuration <= configurationProvider.maxSharedLinkExpirationDuration.inWholeSeconds) {
            """
                Share Url expiration duration is too big
                ($expirationDuration/${configurationProvider.maxSharedLinkExpirationDuration.inWholeSeconds})
            """.trimIndent().replace("\n", " ")
        }
        return ShareUrlExpirationDurationInfo(
            expirationDuration = expirationDuration
        )
    }

    private companion object {
        val delta = 1.minutes.inWholeSeconds // to avoid miscalculation of expirationTime by the backend
                                             // (few seconds in the past)
    }
}
