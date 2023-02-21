/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.key.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyId
import me.proton.core.drive.key.domain.repository.KeyRepository
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class GetNodeKey @Inject constructor(
    private val getLink: GetLink,
    private val keyRepository: KeyRepository,
    private val buildNodeKey: BuildNodeKey,
) {
    suspend operator fun invoke(link: Link): Result<Key.Node> = coRunCatching {
        val userId = link.id.userId
        keyRepository.getKey(userId, link.keyId) as? Key.Node
            ?: buildNodeKey(link)
                .onSuccess { key -> keyRepository.addKey(userId, link.keyId, key) }
                .getOrThrow()
    }

    suspend operator fun invoke(linkId: LinkId): Result<Key.Node> = coRunCatching {
        invoke(
            link = getLink(linkId).toResult().getOrThrow(),
        ).getOrThrow()
    }
}
