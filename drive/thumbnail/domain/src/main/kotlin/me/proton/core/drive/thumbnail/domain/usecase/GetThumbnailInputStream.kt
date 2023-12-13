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

package me.proton.core.drive.thumbnail.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.file.base.domain.extension.url
import me.proton.core.drive.file.base.domain.usecase.FetchThumbnailUrl
import me.proton.core.drive.file.base.domain.usecase.GetUrlInputStream
import java.io.InputStream
import javax.inject.Inject

class GetThumbnailInputStream @Inject constructor(
    private val getUrlInputStream: GetUrlInputStream,
    private val fetchThumbnailUrl: FetchThumbnailUrl,
) {

    suspend operator fun invoke(
        thumbnailId: ThumbnailId,
    ): Result<InputStream> = coRunCatching {
        getUrlInputStream(
            userId = thumbnailId.userId,
            url = fetchThumbnailUrl(thumbnailId).getOrThrow().url,
        ).getOrThrow()
    }
}
