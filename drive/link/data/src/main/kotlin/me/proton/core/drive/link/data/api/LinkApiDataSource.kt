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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.request.CheckAvailableHashesRequest
import me.proton.core.drive.link.data.api.request.GetLinksRequest
import me.proton.core.drive.link.data.api.request.MoveLinkRequest
import me.proton.core.drive.link.data.api.request.RenameLinkRequest
import me.proton.core.drive.link.data.extension.toCopyLinkRequest
import me.proton.core.drive.link.data.extension.toMoveMultipleLinksRequest
import me.proton.core.drive.link.domain.entity.CheckAvailableHashesInfo
import me.proton.core.drive.link.domain.entity.CopyInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.entity.MoveMultipleInfo
import me.proton.core.drive.link.domain.entity.RenameInfo
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

class LinkApiDataSource(private val apiProvider: ApiProvider) {
    @Throws(ApiException::class)
    suspend fun getLink(linkId: LinkId): LinkDto =
        apiProvider.get<LinkApi>(linkId.userId)
            .invoke { getLink(linkId.shareId.id, linkId.id) }.valueOrThrow.linkDto

    @Throws(ApiException::class)
    suspend fun checkAvailableHashes(
        linkId: LinkId,
        checkAvailableHashesInfo: CheckAvailableHashesInfo,
    ) =
        apiProvider.get<LinkApi>(linkId.userId).invoke {
            checkAvailableHashes(
                linkId.shareId.id,
                linkId.id,
                CheckAvailableHashesRequest(
                    hashes = checkAvailableHashesInfo.hashes,
                    clientUid = listOfNotNull(checkAvailableHashesInfo.clientUid),
                )
            )
        }.valueOrThrow

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
                    nodePassphraseSignature = moveInfo.nodePassphraseSignature,
                    nameSignatureEmail = moveInfo.nameSignatureEmail,
                    signatureEmail = moveInfo.signatureEmail,
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

    @Throws(ApiException::class)
    suspend fun getLinks(shareId: ShareId, linkIds: Set<String>) =
        apiProvider.get<LinkApi>(shareId.userId).invoke {
            getLinks(
                shareId = shareId.id,
                request = GetLinksRequest(
                    linkIds = linkIds.toList(),
                )
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun moveMultipleLinks(
        userId: UserId,
        volumeId: VolumeId,
        moveMultipleInfo: MoveMultipleInfo,
    ) =
        apiProvider.get<LinkApi>(userId).invoke {
            moveMultipleLinks(
                volumeId = volumeId.id,
                request = moveMultipleInfo.toMoveMultipleLinksRequest(),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun transferMultipleLinks(
        userId: UserId,
        volumeId: VolumeId,
        moveMultipleInfo: MoveMultipleInfo,
    ) =
        apiProvider.get<LinkApi>(userId).invoke {
            transferMultipleLinks(
                volumeId = volumeId.id,
                request = moveMultipleInfo.toMoveMultipleLinksRequest(),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun copyFile(
        userId: UserId,
        volumeId: VolumeId,
        fileId: FileId,
        copyInfo: CopyInfo,
    ) =
        apiProvider.get<LinkApi>(userId).invoke {
            copyLink(
                volumeId = volumeId.id,
                linkId = fileId.id,
                request = copyInfo.toCopyLinkRequest(),
            )
        }.valueOrThrow.linkId
}
