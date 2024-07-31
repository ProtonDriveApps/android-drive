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

package me.proton.core.drive.drivelink.data.usecase

import android.net.Uri
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetUserEmail
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetProtonDocumentUriString
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class GetProtonDocumentUriStringImpl @Inject constructor(
    private val getShare: GetShare,
    private val getUserEmail: GetUserEmail,
    private val configurationProvider: ConfigurationProvider,
) : GetProtonDocumentUriString {

    override suspend operator fun invoke(driveLink: DriveLink): Result<String> = coRunCatching {
        Uri.Builder()
            .scheme("https")
            .authority("docs.${configurationProvider.host}")
            .appendPath("doc")
            .appendQueryParameter("volumeId", driveLink.volumeId.id)
            .appendQueryParameter("linkId", driveLink.id.id)
            .appendQueryParameter("email", driveLink.getEmail())
            .build()
            .toString()
    }

    private suspend fun DriveLink.getEmail() = getShare(shareId).toResult().getOrThrow()
        .addressId
        ?.let { addressId ->
            getUserEmail(userId, addressId)
        } ?: getUserEmail(userId)
}
