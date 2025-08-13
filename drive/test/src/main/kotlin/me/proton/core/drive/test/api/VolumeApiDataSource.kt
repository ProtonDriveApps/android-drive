/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.test.api

import me.proton.core.drive.volume.data.api.entity.ShareUrlContext
import me.proton.core.drive.volume.data.api.response.GetShareUrlsResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.getShareUrls(block: RequestContext.() -> MockResponse) = routing {
    get("/drive/volumes/{enc_volumeId}/urls", block)
}

fun MockWebServer.getShareUrls(contexts: List<ShareUrlContext>) {
    getShareUrls {
        jsonResponse {
            GetShareUrlsResponse(
                code = 1000,
                shareUrlContexts = contexts,
                more = false
            )
        }
    }
}
