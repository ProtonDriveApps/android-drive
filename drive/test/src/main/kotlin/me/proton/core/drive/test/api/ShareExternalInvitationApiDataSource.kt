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

import me.proton.core.drive.base.data.api.response.Response
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.share.user.data.api.entities.ShareExternalInvitationDto
import me.proton.core.drive.share.user.data.api.request.CreateShareExternalInvitationRequest
import me.proton.core.drive.share.user.data.api.response.GetSharesExternalInvitationsResponse
import me.proton.core.drive.share.user.data.api.response.PostShareExternalInvitationResponse
import me.proton.core.drive.share.user.domain.entity.ShareUser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.getExternalInvitations(block: RequestContext.() -> MockResponse) {
    routing {
        get("/drive/v2/shares/{shareId}/external-invitations") {
            block()
        }
    }
}

fun MockWebServer.getExternalInvitations(vararg emails: String) {
    getExternalInvitations {
        jsonResponse {
            GetSharesExternalInvitationsResponse(
                emails.map { email ->
                    ShareExternalInvitationDto(
                        id = "invitation-id-$email",
                        inviterEmail = "inviter@proton.me",
                        inviteeEmail = email,
                        permissions = 0,
                        externalInvitationSignature = "invitation-signature",
                        state = 1,
                        createTime = 0,
                    )
                }
            )
        }
    }
}

fun MockWebServer.createExternalInvitation(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/v2/shares/{shareId}/external-invitations", block)
}

fun MockWebServer.createExternalInvitation(inviterEmail: String) = createExternalInvitation {
    jsonResponse {
        request<CreateShareExternalInvitationRequest>().toResponse(inviterEmail)
    }
}

fun CreateShareExternalInvitationRequest.toResponse(inviterEmail: String) =
    PostShareExternalInvitationResponse(
        with(invitation) {
            ShareExternalInvitationDto(
                id = "invitation-id-${inviteeEmail}",
                inviteeEmail = inviteeEmail,
                inviterEmail = inviterEmail,
                permissions = permissions,
                externalInvitationSignature = externalInvitationSignature,
                state = 1,
                createTime = 0,
            )
        }
    )

fun MockWebServer.updateExternalInvitation(block: RequestContext.() -> MockResponse) = routing {
    put("/drive/v2/shares/{shareId}/external-invitations/{invitationId}", block)
}

fun MockWebServer.updateExternalInvitation() = updateExternalInvitation {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

fun MockWebServer.deleteExternalInvitation(block: RequestContext.() -> MockResponse) = routing {
    delete("/drive/v2/shares/{shareId}/external-invitations/{invitationId}", block)
}

fun MockWebServer.deleteExternalInvitation() = deleteExternalInvitation {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

fun MockWebServer.sendExternalEmail(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/v2/shares/{shareId}/external-invitations/{invitationId}/sendemail", block)
}

fun MockWebServer.sendExternalEmail() = sendExternalEmail {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}
