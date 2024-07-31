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
import me.proton.core.drive.base.data.api.response.Response
import me.proton.core.drive.share.user.data.api.entities.ShareInvitationDto
import me.proton.core.drive.share.user.data.api.request.CreateShareInvitationRequest
import me.proton.core.drive.share.user.data.api.response.GetSharesInvitationsResponse
import me.proton.core.drive.share.user.data.api.response.PostShareInvitationResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.getInvitations(block: RequestContext.() -> MockResponse) {
    routing {
        get("/drive/v2/shares/{shareId}/invitations") {
            block()
        }
    }
}

fun MockWebServer.getInvitations(vararg emails: String) {
    getInvitations {
        jsonResponse {
            GetSharesInvitationsResponse(
                emails.map { email ->
                    ShareInvitationDto(
                        id = "invitation-id-$email",
                        inviterEmail = "inviter@proton.me",
                        inviteeEmail = email,
                        permissions = 0,
                        keyPacket = "invitation-key-packet",
                        keyPacketSignature = "invitation-key-packet-signature",
                        createTime = 0
                    )
                }
            )
        }
    }
}

fun MockWebServer.createInvitation(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/v2/shares/{shareId}/invitations", block)
}

fun MockWebServer.createInvitation() = createInvitation {
    jsonResponse {
        request<CreateShareInvitationRequest>().toResponse()
    }
}

fun CreateShareInvitationRequest.toResponse() = PostShareInvitationResponse(
    with(invitation) {
        ShareInvitationDto(
            id = "invitation-id-${inviteeEmail}",
            inviteeEmail = inviteeEmail,
            inviterEmail = inviterEmail,
            permissions = permissions,
            keyPacket = keyPacket,
            keyPacketSignature = keyPacketSignature,
            createTime = 0,
        )
    }
)

fun MockWebServer.updateInvitation(block: RequestContext.() -> MockResponse) = routing {
    put("/drive/v2/shares/{shareId}/invitations/{invitationId}", block)
}

fun MockWebServer.updateInvitation() = updateInvitation {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

fun MockWebServer.deleteInvitation(block: RequestContext.() -> MockResponse) = routing {
    delete("/drive/v2/shares/{shareId}/invitations/{invitationId}", block)
}

fun MockWebServer.deleteInvitation() = deleteInvitation {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

fun MockWebServer.sendEmail(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/v2/shares/{shareId}/invitations/{invitationId}/sendemail", block)
}

fun MockWebServer.sendEmail() = sendEmail {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}
