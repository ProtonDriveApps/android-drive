/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.documentsprovider.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FileId
import javax.inject.Inject

class GetFileIdContentDigestMap @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val getContentDigest: GetContentDigest,
) {

    suspend operator fun invoke(
        fileIds: Set<FileId>,
    ): Map<FileId, String?> = withContext(Dispatchers.IO) {
        fileIds.chunked(configurationProvider.contentDigestsInParallel)
            .flatMap { chunk ->
                chunk.map { fileId ->
                    async { fileId to getContentDigest(fileId).getOrNull() }
                }.awaitAll()
            }.toMap()
    }
}
