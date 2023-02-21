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
package me.proton.core.drive.cryptobase.data.usecase

import android.util.Base64
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.cryptobase.domain.usecase.GeneratePassphrase
import javax.inject.Inject

class GeneratePassphraseImpl @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val configurationProvider: ConfigurationProvider,
) : GeneratePassphrase {

    override operator fun invoke(): ByteArray =
        Base64.encode(
            cryptoContext.pgpCrypto.generateRandomBytes(configurationProvider.passphraseSize.value),
            Base64.NO_WRAP,
        )

    override operator fun invoke(
        size: Bytes,
        encodeToBase64Url: Boolean,
    ): String {
        val additionalFlags = if (encodeToBase64Url) Base64.URL_SAFE else 0
        return Base64.encodeToString(
            cryptoContext.pgpCrypto.generateRandomBytes(size.value),
            Base64.NO_WRAP or additionalFlags,
        )
    }
}
