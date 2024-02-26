/*
 * Copyright (c) 2021-2024 Proton AG.
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
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.DecryptNestedPrivateKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.NodeKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.keyId
import me.proton.core.drive.key.domain.extension.nestedPrivateKey
import me.proton.core.drive.key.domain.repository.KeyRepository
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linknode.domain.extension.withAncestorsFromRoot
import me.proton.core.drive.linknode.domain.usecase.GetLinkNode
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import javax.inject.Inject

class BuildNodeKey @Inject constructor(
    private val getLinkNode: GetLinkNode,
    private val getShareKey: GetShareKey,
    private val keyRepository: KeyRepository,
    private val decryptNestedPrivateKey: DecryptNestedPrivateKey,
) {
    suspend operator fun invoke(link: Link): Result<Key.Node> = coRunCatching {
        var key: Key.Node? = null
        var error: Throwable? = null
        getLinkNode(link.id).toResult().getOrThrow().withAncestorsFromRoot { link ->
            keyRepository.getKey(link.id.userId, link.keyId)?.let { nodeKey ->
                key = nodeKey as? Key.Node
            } ?: buildKey(key, link)
                .onSuccess { nodeKey -> key = nodeKey }
                .onFailure {
                    key = null
                    error = it
                    return@withAncestorsFromRoot
                }
        }
        key ?: throw IllegalStateException("Key is null after building process", error)
    }

    suspend operator fun invoke(
        userId: UserId,
        parentKey: Key,
        uploadFileLink: UploadFileLink,
        signatureAddress: String,
    ): Result<Key.Node> =
        coRunCatching {
            NodeKey(
                key = decryptNestedPrivateKey(
                    userId = userId,
                    decryptKey = parentKey.keyHolder,
                    key = uploadFileLink.nestedPrivateKey,
                    signatureAddress = signatureAddress,
                    allowCompromisedVerificationKeys = true,
                ).getOrThrow(),
                parent = parentKey,
            )
        }

    private suspend fun buildKey(parentKey: Key?, link: Link): Result<Key.Node> =
        if (parentKey != null) buildNodeKey(parentKey, link)
        else buildRootNodeKey(link)

    private suspend fun buildNodeKey(parentKey: Key, link: Link): Result<Key.Node> = coRunCatching {
        NodeKey(
            key = decryptNestedPrivateKey(
                userId = link.id.userId,
                decryptKey = parentKey.keyHolder,
                key = link.nestedPrivateKey,
                signatureAddress = link.signatureAddress,
                allowCompromisedVerificationKeys = true,
            ).getOrThrow(),
            parent = parentKey,
        )
    }

    private suspend fun buildRootNodeKey(link: Link): Result<Key.Node> = coRunCatching {
        require(link.parentId == null) { "This method can be used only on root link" }
        buildNodeKey(
            getShareKey(link.id.shareId).getOrThrow(),
            link,
        ).getOrThrow()
    }
}
