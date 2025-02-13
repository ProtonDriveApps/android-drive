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

import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.share.user.data.api.response.GetSharedWithMeListingsResponse
import me.proton.core.drive.share.user.data.api.response.LinkSharedWithMeResponseDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.sharedWithMe(block: RequestContext.() -> MockResponse) {
    routing {
        get("/drive/v2/sharedwithme") {
            block()
        }
    }
}

fun MockWebServer.sharedWithMe(vararg links: LinkSharedWithMeResponseDto) {
    sharedWithMe {
        jsonResponse {
            GetSharedWithMeListingsResponse(
                code = ProtonApiCode.SUCCESS,
                links = links.toList(),
                more = false,
            )
        }
    }
}
