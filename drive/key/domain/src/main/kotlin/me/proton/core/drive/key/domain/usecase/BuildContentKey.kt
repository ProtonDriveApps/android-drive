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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.factory.ContentKeyFactory
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import javax.inject.Inject

class BuildContentKey @Inject constructor(
    private val getNodeKey: GetNodeKey,
    private val getAddressKeys: GetAddressKeys,
    private val contentKeyFactory: ContentKeyFactory,
    private val getSignatureAddress: GetSignatureAddress,
) {
    suspend operator fun invoke(file: Link.File, fileKey: Key.Node): Result<ContentKey> = coRunCatching {
        contentKeyFactory.createContentKey(
            decryptKey = fileKey,
            verifyKey = listOf(fileKey, getAddressKeys(file.id.userId, file.uploadedBy)),
            contentKeyPacket = file.contentKeyPacket,
            contentKeyPacketSignature = file.contentKeyPacketSignature ?: "" // TODO: see what to do in this case
        )
    }

    suspend operator fun invoke(file: Link.File) = coRunCatching {
        invoke(
            file = file,
            fileKey = getNodeKey(file).getOrThrow()
        ).getOrThrow()
    }

    suspend operator fun invoke(
        userId: UserId,
        uploadFile: UploadFileLink,
        fileKey: Key.Node,
    ): Result<ContentKey> = invoke(
        userId = userId,
        contentKeyPacket = uploadFile.contentKeyPacket,
        contentKeyPacketSignature = uploadFile.contentKeyPacketSignature,
        fileKey = fileKey,
    )

    suspend operator fun invoke(
        userId: UserId,
        contentKeyPacket: String,
        contentKeyPacketSignature: String,
        fileKey: Key.Node,
    ): Result<ContentKey> = coRunCatching {
        contentKeyFactory.createContentKey(
            decryptKey = fileKey,
            verifyKey = listOf(fileKey, getAddressKeys(userId, getSignatureAddress(userId))),
            contentKeyPacket = contentKeyPacket,
            contentKeyPacketSignature = contentKeyPacketSignature
        )
    }
}
