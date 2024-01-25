/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.upload.domain.usecase

import me.proton.core.drive.base.domain.extension.trimForbiddenChars
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.util.UUID
import javax.inject.Inject

class GetUploadFileName @Inject constructor(
    private val uriResolver: UriResolver,
    private val validateLinkName: ValidateLinkName,
) {
    suspend operator fun invoke(uriString: String): String =
        normalized(uriResolver.getName(uriString))

    operator fun invoke(uriInfo: UriResolver.UriInfo?): String =
        normalized(uriInfo?.name)

    suspend operator fun invoke(
        uploadFileDescription: UploadFileDescription,
        uriInfo: UriResolver.UriInfo? = null,
    ): String =
        uploadFileDescription.properties?.let { properties -> normalized(properties.name) }
            ?: uriInfo?.let { invoke(uriInfo) }
            ?: invoke(uploadFileDescription.uri)

    private fun normalized(name: String?) = validateLinkName(
        name = name?.trimForbiddenChars() ?: ""
    ).getOrNull() ?: UUID.randomUUID().toString()
}
