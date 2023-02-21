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
package me.proton.core.drive.base.data.usecase

import android.util.Base64
import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.base.domain.usecase.Sha256
import me.proton.core.drive.base.domain.util.coRunCatching
import java.security.MessageDigest
import javax.inject.Inject

class Sha256Impl @Inject constructor() : Sha256 {

    override fun invoke(input: String, format: Sha256.OutputFormat): Result<String> = coRunCatching {
        with (invoke(input).getOrThrow()) {
            when (format) {
                Sha256.OutputFormat.BASE_64 -> Base64.encodeToString(this, Base64.NO_WRAP)
                Sha256.OutputFormat.HEX -> this.toHex()
            }
        }
    }

    override fun invoke(input: String): Result<ByteArray> = coRunCatching {
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
    }
}
