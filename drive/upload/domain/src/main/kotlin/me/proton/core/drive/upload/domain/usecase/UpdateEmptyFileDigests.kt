/*
 * Copyright (c) 2023 Proton AG.
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadDigests
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.UpdateDigests
import me.proton.core.drive.upload.domain.extension.injectMessageDigests
import java.io.ByteArrayInputStream
import javax.inject.Inject

class UpdateEmptyFileDigests @Inject constructor(
    private val updateDigests: UpdateDigests,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<Unit> = coRunCatching {
        val digests = getEmptyDigests()
        updateDigests(uploadFileLink.id, digests)
    }

    private suspend fun getEmptyDigests(): UploadDigests = withContext(Dispatchers.IO) {
        val (digestsInputStream, messageDigests) = ByteArrayInputStream(byteArrayOf()).injectMessageDigests(
            algorithms = configurationProvider.digestAlgorithms,
        )
        digestsInputStream.use { inputStream ->
            inputStream.read()
        }
        messageDigests.associate { messageDigest ->
            messageDigest.algorithm to messageDigest.digest().toHex()
        }.let(::UploadDigests)
    }
}

