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

package me.proton.core.drive.test.api

import me.proton.core.key.data.api.response.ActivePublicKeysResponse
import me.proton.core.key.data.api.response.AddressDataResponse
import me.proton.core.key.data.api.response.PublicAddressKeyResponse
import me.proton.core.key.data.api.response.PublicAddressKeysResponse
import me.proton.core.key.domain.entity.key.KeyFlags
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer


fun MockWebServer.getPublicAddressKeys(block: RequestContext.() -> MockResponse) = routing {
    get("/core/v4/keys", block)
}

fun MockWebServer.getPublicAddressKeys() = getPublicAddressKeys {
    jsonResponse {
        val email = recordedRequest.requestUrl?.queryParameter("Email")
        PublicAddressKeysResponse(
            recipientType = 1,
            keys = listOf(
                PublicAddressKeyResponse(
                    flags = 2,
                    publicKey = "public-key-$email"
                )
            )
        )
    }
}

fun MockWebServer.getPublicAddressKeysAll(block: RequestContext.() -> MockResponse) = routing {
    get("/core/v4/keys/all", block)
}

fun MockWebServer.getPublicAddressKeysAll() = getPublicAddressKeysAll {
    jsonResponse {
        val email = recordedRequest.requestUrl?.queryParameter("Email")
        ActivePublicKeysResponse(
            address = AddressDataResponse(
                keys = listOf(
                    PublicAddressKeyResponse(
                        flags = KeyFlags.NotObsolete,
                        publicKey = "public-key-$email",
                    )
                )
            ),
            warnings = emptyList(),
            protonMx = false,
            isProton = 1,
        )
    }
}
