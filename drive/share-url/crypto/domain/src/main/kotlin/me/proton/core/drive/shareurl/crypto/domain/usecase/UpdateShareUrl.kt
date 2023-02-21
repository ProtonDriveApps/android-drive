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
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.repository.ShareUrlRepository
import me.proton.core.drive.shareurl.base.domain.usecase.GetShareUrl
import javax.inject.Inject

class UpdateShareUrl @Inject constructor(
    private val getShare: GetShare,
    private val getShareUrl: GetShareUrl,
    private val createShareUrlCustomPasswordInfo: CreateShareUrlCustomPasswordInfo,
    private val shareUrlRepository: ShareUrlRepository,
    private val updateEventAction: UpdateEventAction,
    private val getMainShare: GetMainShare,
    private val dateTimeFormatter: DateTimeFormatter,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        shareUrlId: ShareUrlId,
        customPassword: String?,
        expirationDateIso8601: String?,
        updateExpirationDate: Boolean,
    ): Result<ShareUrl> = coRunCatching {
        val shareUrlCustomPasswordInfo = customPassword?.let { shareUrlCustomPasswordInfo(shareUrlId, customPassword) }
        val shareUrlExpirationDurationInfo = if (updateExpirationDate) {
            expirationDateIso8601?.let {
                shareUrlExpirationDurationInfo(expirationDateIso8601)
            } ?: ShareUrlExpirationDurationInfo(expirationDuration = null)
        } else {
            null
        }
        return invoke(
            shareUrlId = shareUrlId,
            shareUrlCustomPasswordInfo = shareUrlCustomPasswordInfo,
            shareUrlExpirationDurationInfo = shareUrlExpirationDurationInfo,
        )
    }

    private suspend operator fun invoke(
        shareUrlId: ShareUrlId,
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo?,
        shareUrlExpirationDurationInfo : ShareUrlExpirationDurationInfo?,
    ): Result<ShareUrl> = coRunCatching {
        updateEventAction(getMainShare(shareUrlId.shareId.userId).toResult().getOrThrow().id) {
            shareUrlRepository.updateShareUrl(
                shareUrlId = shareUrlId,
                shareUrlCustomPasswordInfo = shareUrlCustomPasswordInfo,
                shareUrlExpirationDurationInfo = shareUrlExpirationDurationInfo,
            ).getOrThrow()
        }
    }

    private suspend fun shareUrlCustomPasswordInfo(
        shareUrlId: ShareUrlId,
        customPassword: String,
    ): ShareUrlCustomPasswordInfo =
        createShareUrlCustomPasswordInfo(
            share = getShare(shareUrlId.shareId).toResult().getOrThrow(),
            shareUrl = getShareUrl(shareUrlId).toResult().getOrThrow(),
            customPassword = customPassword,
        ).getOrThrow()

    private fun shareUrlExpirationDurationInfo(
        expirationDateIso8601: String
    ): ShareUrlExpirationDurationInfo {
        val futureInSeconds = dateTimeFormatter.parseFromIso8601String(expirationDateIso8601).getOrThrow().value
        val nowInSeconds = System.currentTimeMillis() / 1000
        require(futureInSeconds >= nowInSeconds) { "Share Url expiration date cannot be in the past" }
        val expirationDuration = futureInSeconds - nowInSeconds
        require(expirationDuration <= configurationProvider.maxSharedLinkExpirationDuration.inWholeSeconds) {
            "Share Url expiration duration is too big ($expirationDuration/${configurationProvider.maxSharedLinkExpirationDuration.inWholeSeconds})"
        }
        return ShareUrlExpirationDurationInfo(
            expirationDuration = expirationDuration
        )
    }
}
