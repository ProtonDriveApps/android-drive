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

package me.proton.core.drive.link.domain.entity

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.drive.sdk.entity.NodeUid
import me.proton.drive.sdk.entity.RevisionUid

data class RevisionContext(
    override val userId: UserId,
    val revisionUid: RevisionUid,
    override val volumeType: Volume.Type,
) : NodeContext {
    override val nodeUid: NodeUid get() = revisionUid.nodeUid
}
