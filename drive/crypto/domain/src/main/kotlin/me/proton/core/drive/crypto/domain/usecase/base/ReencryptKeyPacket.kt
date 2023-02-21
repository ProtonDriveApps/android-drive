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
package me.proton.core.drive.crypto.domain.usecase.base

import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId

interface ReencryptKeyPacket {
    suspend operator fun invoke(
        message: EncryptedMessage,
        link: Link,
        shareKey: Key.Node,
    ): Result<String>

    suspend operator fun invoke(
        message: EncryptedMessage,
        linkId: LinkId,
        shareKey: Key.Node,
    ): Result<String>
}
