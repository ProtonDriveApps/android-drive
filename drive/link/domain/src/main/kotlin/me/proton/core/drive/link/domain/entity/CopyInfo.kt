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

package me.proton.core.drive.link.domain.entity

sealed class CopyInfo {
    abstract val name: String
    abstract val hash: String
    abstract val targetVolumeId: String
    abstract val targetParentLinkId: String
    abstract val nodePassphrase: String
    abstract val nameSignatureEmail: String
    abstract val nodePassphraseSignature: String?
    abstract val signatureEmail: String?

    data class File(
        override val name: String,
        override val hash: String,
        override val targetVolumeId: String,
        override val targetParentLinkId: String,
        override val nodePassphrase: String,
        override val nameSignatureEmail: String,
        override val nodePassphraseSignature: String? = null,
        override val signatureEmail: String? = null,
    ) : CopyInfo()

    data class Photo(
        val photos: Photos,
        override val name: String,
        override val hash: String,
        override val targetVolumeId: String,
        override val targetParentLinkId: String,
        override val nodePassphrase: String,
        override val nameSignatureEmail: String,
        override val nodePassphraseSignature: String? = null,
        override val signatureEmail: String? = null,
    ) : CopyInfo() {

        data class Photos(
            val contentHash: String,
            val relatedPhotos: List<RelatedPhoto> = emptyList(),
        )

        data class RelatedPhoto(
            val linkId: String,
            val name: String,
            val hash: String,
            val nodePassphrase: String,
            val contentHash: String,
        )
    }
}
