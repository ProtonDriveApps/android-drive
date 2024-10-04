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

package me.proton.core.drive.crypto.domain.entity

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.bytes
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.spec.GCMParameterSpec

data class CipherSpec(
    val transformation: Transformation,
    val algorithmParameterSpec: (iv: ByteArray) -> AlgorithmParameterSpec,
    val ivSize: Bytes = 12.bytes,
) {
    enum class Transformation(val value: String) {
        AES_GCM_NO_PADDING("AES/GCM/NoPadding"),
    }

    enum class Algorithm(val value: String) {
        AES("AES"),
    }

    companion object {
        val AES_GCM_NO_PADDING_IV_16_BYTES get() = CipherSpec(
            transformation = Transformation.AES_GCM_NO_PADDING,
            algorithmParameterSpec = { iv ->
                GCMParameterSpec(128, iv)
            },
            ivSize = 16.bytes,
        )
    }
}
