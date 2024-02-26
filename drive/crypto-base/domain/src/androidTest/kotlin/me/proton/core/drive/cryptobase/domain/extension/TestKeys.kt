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

package me.proton.core.drive.cryptobase.domain.extension

import me.proton.core.crypto.common.pgp.Armored

object TestKeys {
    object Key1 {
        val passphrase = "bb4eacacb1773f4dd258d4c9d708a6dfed1a24b6e78862454738a405c51f41ab".toByteArray()
        val privateKey: Armored =
            """
            -----BEGIN PGP PRIVATE KEY BLOCK-----
            Version: GopenPGP 2.4.5
            Comment: https://gopenpgp.org

            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
            =YQAi
            -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent()
    }

    object Key2 {
        val passphrase = "ikaAA3dimv9p7D.bqZ6mq.R45LRS6oi".toByteArray()
        val privateKey =
            """
            -----BEGIN PGP PRIVATE KEY BLOCK-----
            Version: ProtonMail

            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
            b5BlAx31rUZ+NdCZyPnU+83opOdYrRjy
            =kArG
            -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent()
    }
}
