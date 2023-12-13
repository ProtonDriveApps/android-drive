/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class GetContentHash @Inject constructor(
    private val hmacSha256: HmacSha256,
    private val getLink: GetLink,
    private val getNodeHashKey: GetNodeHashKey,
) {
    suspend operator fun invoke(folderId: FolderId, input: String): Result<String> = coRunCatching {
        invoke(
            hashKey = getNodeHashKey(
                folder = getLink(folderId).toResult().getOrThrow(),
            ).getOrThrow(),
            input = input,
        ).getOrThrow()
    }

    suspend operator fun invoke(folder: Link.Folder, input: String): Result<String> = coRunCatching {
        invoke(
            hashKey = getNodeHashKey(folder).getOrThrow(),
            input = input,
        ).getOrThrow()
    }

    suspend operator fun invoke(folderId: FolderId, folderKey: Key.Node, input: String): Result<String> =
        coRunCatching {
            invoke(
                hashKey = getNodeHashKey(
                    folder = getLink(folderId).toResult().getOrThrow(),
                    folderKey = folderKey,
                ).getOrThrow(),
                input = input,
            ).getOrThrow()
        }

    suspend operator fun invoke(folder: Link.Folder, folderKey: Key.Node, input: String): Result<String> =
        coRunCatching {
            invoke(
                hashKey = getNodeHashKey(
                    folder = folder,
                    folderKey = folderKey,
                ).getOrThrow(),
                input = input,
            ).getOrThrow()
        }

    suspend operator fun invoke(hashKey: NodeHashKey, input: String): Result<String> = coRunCatching {
        hmacSha256(hashKey, input).getOrThrow()
    }
}
