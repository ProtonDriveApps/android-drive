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
package me.proton.core.drive.file.base.domain.entity

interface Block {
    val index: Long
    val url: String
    val hashSha256: String?
    val encSignature: String?

    fun copy(
        index: Long = this.index,
        url: String = this.url,
        hashSha256: String? = this.hashSha256,
        encSignature: String? = this.encSignature,
    ) = Block(
        index = index,
        url = url,
        hashSha256 = hashSha256,
        encSignature = encSignature,
    )

    companion object {
        operator fun invoke(index: Long, url: String, hashSha256: String?, encSignature: String?) =
            object : Block {
                override val index: Long = index
                override val url: String = url
                override val hashSha256: String? = hashSha256
                override val encSignature: String? = encSignature
            }
    }
}
