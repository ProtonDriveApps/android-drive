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
package me.proton.core.drive.link.data.api

import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.request.MoveLinkRequest
import me.proton.core.drive.link.data.api.request.RenameLinkRequest
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.entity.RenameInfo
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

class LinkApiDataSource(private val apiProvider: ApiProvider) {
    @Throws(ApiException::class)
    suspend fun getLink(linkId: LinkId): LinkDto =
        apiProvider.get<LinkApi>(linkId.userId).invoke { getLink(linkId.shareId.id, linkId.id) }.valueOrThrow.linkDto

    @Throws(ApiException::class)
    suspend fun moveLink(
        linkId: LinkId,
        moveInfo: MoveInfo,
    ) =
        apiProvider.get<LinkApi>(linkId.userId).invoke {
            moveLink(
                linkId.shareId.id,
                linkId.id,
                MoveLinkRequest(
                    name = moveInfo.name,
                    hash = moveInfo.hash,
                    originalHash = moveInfo.previousHash,
                    parentLinkId = moveInfo.parentLinkId,
                    nodePassphrase = moveInfo.nodePassphrase,
                    nameSignatureEmail = moveInfo.nameSignatureEmail,
                )
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun renameLink(linkId: LinkId, renameInfo: RenameInfo) =
        apiProvider.get<LinkApi>(linkId.userId).invoke {
            renameLink(
                shareId = linkId.shareId.id,
                linkId = linkId.id,
                request = RenameLinkRequest(
                    name = renameInfo.name,
                    hash = renameInfo.hash,
                    originalHash = renameInfo.previousHash,
                    mimeType = renameInfo.mimeType,
                    signatureAddress = renameInfo.signatureAddress
                )
            )
        }.valueOrThrow
}
