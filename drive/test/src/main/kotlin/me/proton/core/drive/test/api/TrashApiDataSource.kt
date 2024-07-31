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
import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.base.data.api.response.Response
import me.proton.core.drive.link.data.api.response.LinkResponse
import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.drive.trash.data.api.request.LinkIDsRequest
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.trash() = routing {
    delete("/drive/volumes/{volumeId}/trash") {
        jsonResponse {
            CodeResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
            )
        }
    }
}

fun MockWebServer.trashMultiple(
    responsePerLink: (String) -> LinkResponse = { linkId ->
        LinkResponse(linkId, response = Response(ProtonApiCode.SUCCESS))
    },
) = routing {
    post("/drive/shares/{enc_shareID}/folders/{enc_linkID}/trash_multiple") {
        jsonResponse {
            LinkResponses(
                code = ProtonApiCode.SUCCESS,
                responses = with(request<LinkIDsRequest>()) { linkIDs.map(responsePerLink) }
            )
        }
    }
}

fun MockWebServer.restoreMultiple(
    responsePerLink: (String) -> LinkResponse = { linkId ->
        LinkResponse(linkId, response = Response(ProtonApiCode.SUCCESS))
    },
) = routing {
    put("/drive/shares/{enc_shareID}/trash/restore_multiple") {
        jsonResponse {
            LinkResponses(
                code = ProtonApiCode.SUCCESS,
                responses = with(request<LinkIDsRequest>()) { linkIDs.map(responsePerLink) }
            )
        }
    }
}

fun MockWebServer.deleteMultiple(
    responsePerLink: (String) -> LinkResponse = { linkId ->
        LinkResponse(linkId, response = Response(ProtonApiCode.SUCCESS))
    },
) = routing {
    post("/drive/shares/{enc_shareID}/trash/delete_multiple") {
        jsonResponse {
            LinkResponses(
                code = ProtonApiCode.SUCCESS,
                responses = with(request<LinkIDsRequest>()) { linkIDs.map(responsePerLink) }
            )
        }
    }
}
