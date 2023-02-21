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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.share.CreateShareUrlInfo.Companion.RANDOM_URL_PASSWORD_SIZE
import me.proton.core.drive.crypto.domain.usecase.share.DecryptUrlPassword
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import javax.inject.Inject

class GetPublicUrl @Inject constructor(
    private val decryptUrlPassword: DecryptUrlPassword,
) {
    suspend operator fun invoke(
        userId: UserId,
        shareUrl: ShareUrl,
    ): Result<String> = coRunCatching {
        when {
            shareUrl.flags.isLegacy -> shareUrl.publicUrl
            else -> {
                val urlPassword = decryptUrlPassword(
                    userId = userId,
                    encryptedUrlPassword = shareUrl.encryptedUrlPassword,
                    creatorEmail = shareUrl.creatorEmail,
                ).getOrThrow()
                "${shareUrl.publicUrl}#${urlPassword.take(RANDOM_URL_PASSWORD_SIZE)}"
            }
        }
    }
}
