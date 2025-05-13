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

package me.proton.core.drive.link.data.extension

import me.proton.core.drive.link.data.api.request.MoveMultipleLinksRequest
import me.proton.core.drive.link.domain.entity.MoveMultipleInfo

fun MoveMultipleInfo.toMoveMultipleLinksRequest() =
    MoveMultipleLinksRequest(
        parentLinkId = parentLinkId.id,
        links = links.map { moveMultipleInfo ->
            moveMultipleInfo.toMoveMultipleLinksRequestLink()
        },
        nameSignatureEmail = nameSignatureEmail,
        signatureEmail = signatureEmail,
    )

fun MoveMultipleInfo.MoveInfo.toMoveMultipleLinksRequestLink() =
    MoveMultipleLinksRequest.Link(
        linkId = linkId.id,
        name = name,
        nodePassphrase = nodePassphrase,
        hash = hash,
        originalHash = originalHash,
        contentHash = (this as? MoveMultipleInfo.MoveInfo.PhotoFile)?.contentHash,
        nodePassphraseSignature = nodePassphraseSignature,
    )
