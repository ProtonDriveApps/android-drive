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
import me.proton.core.drive.share.user.data.api.entities.UserInvitationIdDto
import me.proton.core.drive.share.user.data.api.response.GetUserInvitationsResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.getUserInvitations(block: RequestContext.() -> MockResponse) {
    routing {
        get("/drive/v2/shares/invitations") {
            block()
        }
    }
}

fun MockWebServer.getUserInvitations(vararg emails: String) {
    getUserInvitations {
        jsonResponse {
            GetUserInvitationsResponse(
                emails.map { email ->
                    UserInvitationIdDto(
                        id = "user-invitation-id-$email",
                        volumeId = "user-invitation-volume-id-$email",
                        shareId = "user-invitation-share-id-$email",
                    )
                }
            )
        }
    }
}

fun MockWebServer.acceptUserInvitation(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/v2/shares/invitations/{invitationId}/accept", block)
}

fun MockWebServer.acceptUserInvitation() = acceptUserInvitation {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

fun MockWebServer.rejectUserInvitation(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/v2/shares/invitations/{invitationId}/reject", block)
}

fun MockWebServer.rejectUserInvitation() = rejectUserInvitation {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}
