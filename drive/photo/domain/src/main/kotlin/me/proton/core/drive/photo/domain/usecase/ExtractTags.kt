/*
 * Copyright (c) 2024-2025 Proton AG.
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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.provider.TagsProvider
import javax.inject.Inject

class ExtractTags @Inject constructor(
    private val tagsProviders: @JvmSuppressWildcards Set<TagsProvider>,
) {
    suspend operator fun invoke(uriString: String?): Result<Set<PhotoTag>> = coRunCatching {
        tagsProviders.map { tagsProvider ->
            tagsProvider(requireNotNull(uriString))
        }.flatten().toSet()
    }

    suspend operator fun invoke(
        fileId: FileId,
        uriString: String?,
    ): Result<Set<PhotoTag>> = coRunCatching {
        if (uriString != null) {
            invoke(uriString).getOrNull(
                LogTag.PHOTO, "Cannot get tags by uri fallback to file id"
            ) ?: invoke(fileId).getOrThrow()
        } else {
            invoke(fileId).getOrThrow()
        }
    }

    private suspend operator fun invoke(fileId: FileId): Result<Set<PhotoTag>> = coRunCatching {
        tagsProviders.map { tagsProvider ->
            tagsProvider(fileId)
        }.flatten().toSet()
    }
}
