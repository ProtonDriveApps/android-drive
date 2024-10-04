/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.crypto.domain.extension

import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.drive.crypto.domain.entity.CipherSpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

fun SessionKey.secretKey(transformation: CipherSpec.Transformation): SecretKey =
    transformation.algorithm.let { algorithm ->
        algorithm.checkKeySize(key)
        SecretKeySpec(key, algorithm.value)
    }

private val CipherSpec.Transformation.algorithm: CipherSpec.Algorithm get() = when (this) {
    CipherSpec.Transformation.AES_GCM_NO_PADDING -> CipherSpec.Algorithm.AES
}

private fun CipherSpec.Algorithm.checkKeySize(key: ByteArray) = when (this) {
    CipherSpec.Algorithm.AES -> require(key.size in setOf(16, 24, 32)) { "Key must be 128, 192 or 256 bits" }
}
