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

data class MoveMultipleInfo(
    val parentLinkId: ParentId,
    val links: List<MoveInfo>,
    val nameSignatureEmail: String? = null,
    val signatureEmail: String? = null,
) {
    sealed class MoveInfo {
        abstract val linkId: LinkId
        abstract val name: String
        abstract val nodePassphrase: String
        abstract val hash: String
        abstract val originalHash: String?
        abstract val nodePassphraseSignature: String?

        data class NonPhotoFileOrFolder(
            override val linkId: LinkId,
            override val name: String,
            override val nodePassphrase: String,
            override val hash: String,
            override val originalHash: String? = null,
            override val nodePassphraseSignature: String? = null,
        ) : MoveInfo()

        data class PhotoFile(
            val contentHash: String,
            override val linkId: FileId,
            override val name: String,
            override val nodePassphrase: String,
            override val hash: String,
            override val originalHash: String? = null,
            override val nodePassphraseSignature: String? = null,
        ) : MoveInfo()
    }
}
