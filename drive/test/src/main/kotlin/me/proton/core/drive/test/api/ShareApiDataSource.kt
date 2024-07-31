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

import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.db.test.findRootId
import me.proton.core.drive.db.test.findShareType
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.request.GetLinksRequest
import me.proton.core.drive.link.data.api.response.GetLinksResponse
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.api.request.ShareAccessWithNodeRequest
import me.proton.core.drive.share.data.api.response.GetShareBootstrapResponse
import me.proton.core.drive.share.data.api.response.GetSharesResponse
import me.proton.core.drive.share.data.api.response.GetUnmigratedSharesResponse
import me.proton.core.drive.share.data.api.response.UpdateUnmigratedSharesResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.getShare(shares: List<ShareDto>) = routing {
    get("/drive/shares") {
        jsonResponse {
            GetSharesResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
                shareDtos = shares.map { share ->
                    share.copy(
                        type = requireNotNull(
                            recordedRequest.requestUrl?.queryParameter("ShareType")
                        ).toLong(),
                    )
                }
            )
        }
    }
}

fun MockWebServer.getShareBootstrap() = routing {
    get("/drive/shares/@{enc_shareID}") {
        jsonResponse {
            val shareId = requireNotNull(parameters["enc_shareID"])
            val shareType = findShareType(shareId)
            GetShareBootstrapResponse(
                code = ProtonApiCode.SUCCESS,
                shareId = shareId,
                type = shareType,
                state = 1,
                linkId = findRootId(shareId).id,
                volumeId = volumeId.id,
                creator = "",
                flags = 0,
                locked = false,
                key = "",
                passphrase = "s".repeat(32),
                passphraseSignature = "",
                addressId = "address-id",
                creationTime = null,
                memberships = emptyList(),
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

fun MockWebServer.getUnmigratedShares(block: RequestContext.() -> MockResponse) = routing {
    get("/drive/migrations/shareaccesswithnode/unmigrated", block)
}

fun MockWebServer.getUnmigratedShares(shareIds: List<String>) = routing {
    getUnmigratedShares {
        if (times == 1) {
            jsonResponse {
                GetUnmigratedSharesResponse(
                    code = 1000,
                    shareIds = shareIds,
                )
            }
        } else {
            response(404)
        }
    }
}

fun MockWebServer.updateUnmigratedShares() = routing {
    routing {
        post("/drive/migrations/shareaccesswithnode") {
            jsonResponse {
                UpdateUnmigratedSharesResponse(
                    code = 1000,
                    shareIds = request<ShareAccessWithNodeRequest>()
                        .passphraseNodeKeyPackets.map { passphraseNodeKeyPacket ->
                            passphraseNodeKeyPacket.shareId
                        },
                    errors = emptyList(),
                )
            }
        }
    }
}

fun MockWebServer.deleteShare(block: RequestContext.() -> MockResponse) = routing {
    routing {
        delete("/drive/shares/@{enc_shareID}") {
            block()
        }
    }
}

fun MockWebServer.deleteShare() = deleteShare { jsonResponse {} }
