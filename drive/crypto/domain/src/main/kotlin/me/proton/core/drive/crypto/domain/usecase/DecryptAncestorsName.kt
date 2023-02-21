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
package me.proton.core.drive.crypto.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linknode.domain.usecase.GetLinkAncestors
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Link name is encrypted with parent node key so in order to decrypt it we first get node key from link parent and then
 * decrypt link name with it.
 */
class DecryptAncestorsName @Inject constructor(
    private val decryptLinkName: DecryptLinkName,
    private val getLinkAncestors: GetLinkAncestors,
) {
    operator fun invoke(
        linkId: LinkId,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Flow<DataResult<List<Link>>> =
        getLinkAncestors(linkId).mapSuccess { (_, links) ->
            DataResult.Success(ResponseSource.Local, links.map { link ->
                val decryptedName = decryptLinkName(link, coroutineContext).getOrNull()
                if (decryptedName != null) {
                    when (link) {
                        is Link.Folder -> link.copy(name = decryptedName.text)
                        is Link.File -> link.copy(name = decryptedName.text)
                    }
                } else {
                    link
                }
            })
        }
}
