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

package me.proton.core.drive.documentsprovider.domain.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId

@Serializable
data class DocumentId(
    @SerialName("userId")
    @Serializable(with = UserIdSerializer::class)
    val userId: UserId,
    @SerialName("linkId")
    @Serializable(with = LinkIdSerializer::class)
    val linkId: LinkId?,
)

internal class UserIdSerializer : KSerializer<UserId> {
    override val descriptor = buildClassSerialDescriptor("UserId") {
        element<String>("id")
    }

    override fun deserialize(decoder: Decoder): UserId = decoder.decodeStructure(descriptor) {
        var id = ""
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> id = decodeStringElement(descriptor, 0)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        UserId(id)
    }

    override fun serialize(encoder: Encoder, value: UserId) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.id)
    }
}

internal class LinkIdSerializer : KSerializer<LinkId> {
    override val descriptor = buildClassSerialDescriptor("LinkId") {
        element<String>("userId")
        element<String>("shareId")
        element<String>("fileId")
        element<String>("folderId")
    }

    override fun deserialize(decoder: Decoder): LinkId = decoder.decodeStructure(descriptor) {
        var userId = ""
        var shareId = ""
        var fileId = ""
        var folderId = ""
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> userId = decodeStringElement(descriptor, 0)
                1 -> shareId = decodeStringElement(descriptor, 1)
                2 -> fileId = decodeStringElement(descriptor, 2)
                3 -> folderId = decodeStringElement(descriptor, 3)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        if (fileId.isNotBlank()) {
            FileId(ShareId(UserId(userId), shareId), fileId)
        } else {
            FolderId(ShareId(UserId(userId), shareId), folderId)
        }
    }

    override fun serialize(encoder: Encoder, value: LinkId) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.userId.id)
        encodeStringElement(descriptor, 1, value.shareId.id)
        (value as? FileId)?.id?.let { id -> encodeStringElement(descriptor, 2, id) }
        (value as? FolderId)?.id?.let { id -> encodeStringElement(descriptor, 3, id) }
    }
}
