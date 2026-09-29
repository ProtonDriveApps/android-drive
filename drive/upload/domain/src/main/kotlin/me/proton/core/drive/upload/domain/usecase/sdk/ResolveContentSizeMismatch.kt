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

package me.proton.core.drive.upload.domain.usecase.sdk

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.domain.exception.ContentSizeMismatchUnresolvedException
import me.proton.core.drive.upload.domain.usecase.GetInputStreamSize
import me.proton.drive.sdk.ProtonSdkError
import javax.inject.Inject

class ResolveContentSizeMismatch @Inject constructor(
    private val getInputStreamSize: GetInputStreamSize,

    ) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        data: ProtonSdkError.Data.ContentSizeMismatch,
    ) = coRunCatching {
        with(uploadFileLink) {
            val uploadedSize = checkNotNull(data.uploadedSize) { "Uploaded size must not be null" }
            val uri = checkNotNull(uriString) { "Uri must not be null" }
            val uriSize = getInputStreamSize(uri).getOrThrow()
            if (uriSize.value != uploadedSize) {
                throw ContentSizeMismatchUnresolvedException(
                    uploadedSize = uploadedSize,
                    expectedSize = data.expectedSize,
                    currentSize = uriSize.value,
                )
            }
        }
    }
}
