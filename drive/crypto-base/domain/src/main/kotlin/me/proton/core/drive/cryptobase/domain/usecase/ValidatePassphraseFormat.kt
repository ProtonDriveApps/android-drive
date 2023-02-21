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
package me.proton.core.drive.cryptobase.domain.usecase

import javax.inject.Inject

class ValidatePassphraseFormat @Inject constructor() {
    operator fun invoke(decryptedToken: ByteArray): Boolean =
        when (decryptedToken.size) {
            64 -> validate32BytesAsHexString(decryptedToken)
            44 -> validate32BytesAsBase64(decryptedToken)
            32 -> true
            else -> false
        }

    private fun validate32BytesAsHexString(decryptedToken: ByteArray): Boolean =
        HEX_REGEX.matches(decryptedToken.map { byte -> byte.toInt().toChar() }.joinToString(""))

    private fun validate32BytesAsBase64(decryptedToken: ByteArray): Boolean =
        BASE64_REGEX.matches(decryptedToken.decodeToString())

    companion object {
        private val HEX_REGEX = "([0-9A-Fa-f]{2})+$".toRegex()
        private val BASE64_REGEX = "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$".toRegex()
    }
}
