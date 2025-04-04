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
package me.proton.core.drive.link.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.LinkId

val LinkId.userId: UserId get() = shareId.userId

fun LinkId?.equalsAsLinkId(other: LinkId?): Boolean =
    if (this == null) {
        other == null
    } else {
        shareId == other?.shareId && id == other.id
    }
