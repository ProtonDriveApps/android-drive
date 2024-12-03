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
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class GetVerificationKeys @Inject constructor(
    private val getLink: GetLink,
    private val getNodeKey: GetNodeKey,
    private val getLinkParentKey: GetLinkParentKey,
    private val getPublicAddressKeys: GetPublicAddressKeys,
) {
    enum class FallbackTo {
        NODE_KEY,
        PARENT_NODE_KEY,
    }

    suspend operator fun invoke(
        link: Link,
        email: String,
        fallbackTo: FallbackTo = FallbackTo.NODE_KEY,
    ): Result<Key> = coRunCatching {
        if (email.isEmpty()) {
            fallbackTo.getKey(link)
        } else {
            getPublicAddressKeys(link.userId, email).getOrThrow()
        }
    }

    suspend operator fun invoke(
        linkId: LinkId,
        email: String,
        fallbackTo: FallbackTo = FallbackTo.NODE_KEY,
    ): Result<Key> = coRunCatching {
        invoke(
            link = getLink(linkId).toResult().getOrThrow(),
            email = email,
            fallbackTo = fallbackTo,
        ).getOrThrow()
    }

    private suspend fun FallbackTo.getKey(
        link: Link
    ) = when (this) {
        FallbackTo.NODE_KEY -> getNodeKey(link).getOrThrow()
        FallbackTo.PARENT_NODE_KEY -> getLinkParentKey(link).getOrThrow()
    }
}
