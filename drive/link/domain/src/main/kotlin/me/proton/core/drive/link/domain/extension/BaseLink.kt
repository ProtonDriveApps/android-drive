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

package me.proton.core.drive.link.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Timestamp
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId

val List<BaseLink>.ids: List<LinkId> get() = map { link -> link.id }

val BaseLink.shareId: ShareId get() = id.shareId

val BaseLink.userId: UserId get() = shareId.userId

fun BaseLink.requireParentId(): FolderId = requireNotNull(parentId)

val BaseLink.isSharedUrlExpired get() = shareUrlExpirationTime?.let { expirationTime ->
    expirationTime < Timestamp.now
} == true
