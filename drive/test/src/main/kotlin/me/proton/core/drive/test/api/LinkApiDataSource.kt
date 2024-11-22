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

import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.request.GetLinksRequest
import me.proton.core.drive.link.data.api.response.GetLinkResponse
import me.proton.core.drive.link.data.api.response.GetLinksResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.checkAvailableHashes(block: RequestContext.() -> MockResponse) {
    routing {
        post("/drive/shares/{enc_shareID}/links/{enc_linkID}/checkAvailableHashes") {
            block()
        }
    }
}

fun MockWebServer.getLink(block: RequestContext.() -> MockResponse) = routing {
    get("/drive/shares/{enc_shareID}/links/{enc_linkID}", block)
}

fun MockWebServer.getLink(link: LinkDto) {
    getLink {
        jsonResponse {
            GetLinkResponse(
                code = 1000,
                linkDto = link,
            )
        }
    }
}


fun MockWebServer.getLinks(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/shares/{enc_shareID}/links/fetch_metadata", block)
}

fun MockWebServer.getLinksWithParents(block: (String) -> LinkDto) {
    getLinks {
        jsonResponse {
            GetLinksResponse(
                code = 1000,
                links = request<GetLinksRequest>().linkIds.map(block),
                parents = emptyList(),
            )
        }
    }
}

