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

package me.proton.core.drive.link.domain.usecase

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class FetchLinks @Inject constructor(
    private val repository: LinkRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(
        shareId: ShareId,
        linkIds: Set<String>,
    ): Result<Pair<List<Link>, List<Link>>> = coRunCatching {
        val parents = mutableListOf<Link>()
        val links = mutableListOf<Link>()
        linkIds.chunked(configurationProvider.apiPageSize).forEach { chunkedLinkIds ->
            val result = repository.fetchLinks(shareId, chunkedLinkIds.toSet()).getOrThrow()
            parents.addAll(result.first)
            links.addAll(result.second)
        }
        parents to links
    }
}
