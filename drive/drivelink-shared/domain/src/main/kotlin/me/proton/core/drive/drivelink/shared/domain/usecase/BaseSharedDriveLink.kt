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

package me.proton.core.drive.drivelink.shared.domain.usecase

import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.toTimestampMs
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.shared.domain.entity.SharedDriveLink
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.extension.userId
import me.proton.core.drive.shareurl.crypto.domain.usecase.GetCustomUrlPassword
import me.proton.core.drive.shareurl.crypto.domain.usecase.GetPublicUrl
import me.proton.core.util.kotlin.CoreLogger
import java.util.Date

abstract class BaseSharedDriveLink(
    private val getPublicUrl: GetPublicUrl,
    private val getCustomUrlPassword: GetCustomUrlPassword,
) {

    suspend fun ShareUrl.toSharedDriveLink() = SharedDriveLink(
        volumeId = volumeId,
        shareUrlId = id,
        publicUrl = getPublicUrlProperty(
            userId = id.userId,
            shareUrl = this
        ),
        customPassword = getCustomUrlPasswordProperty(
            userId = id.userId,
            shareUrl = this
        ),
        expirationTime = getExpirationTime(this),
        isLegacy = this.flags.isLegacy,
        permissions = permissions,
    )

    private suspend fun getPublicUrlProperty(
        userId: UserId,
        shareUrl: ShareUrl,
    ): CryptoProperty<String> =
        getPublicUrl(userId, shareUrl).fold(
            onSuccess = { publicUrl ->
                CryptoProperty.Decrypted(
                    value = publicUrl,
                    status = VerificationStatus.Unknown,
                )
            },
            onFailure = { error ->
                CoreLogger.e(LogTag.SHARE, error, "Cannot get public url")
                CryptoProperty.Encrypted(shareUrl.publicUrl)
            }
        )

    private suspend fun getCustomUrlPasswordProperty(
        userId: UserId,
        shareUrl: ShareUrl,
    ): CryptoProperty<String>? =
        getCustomUrlPassword(userId, shareUrl).fold(
            onSuccess = { customUrlPassword ->
                customUrlPassword?.let {
                    CryptoProperty.Decrypted(
                        value = customUrlPassword,
                        status = VerificationStatus.Unknown,
                    )
                }
            },
            onFailure = { error ->
                CoreLogger.w(LogTag.SHARE, error, "Cannot get custom url password")
                null
            }
        )

    protected fun getExpirationTime(shareUrl: ShareUrl): Date? =
        shareUrl.expirationTime?.let { time ->
            Date(time.toTimestampMs().value)
        }
}
