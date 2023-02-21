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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GenerateHashKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.link.domain.entity.Link
import javax.inject.Inject

class GenerateNodeHashKey @Inject constructor(
    private val generateHashKey: GenerateHashKey,
    private val getNodeKey: GetNodeKey,
) {
    suspend operator fun invoke(nodeKey: Key.Node): Result<NodeHashKey> = coRunCatching {
        NodeHashKey(
            decryptKey = nodeKey,
            encryptedHashKey = generateHashKey(
                encryptKey = nodeKey.keyHolder,
            ).getOrThrow()
        )
    }

    suspend operator fun invoke(link: Link): Result<NodeHashKey> = coRunCatching {
        invoke(
            nodeKey = getNodeKey(link).getOrThrow(),
        ).getOrThrow()
    }
}
