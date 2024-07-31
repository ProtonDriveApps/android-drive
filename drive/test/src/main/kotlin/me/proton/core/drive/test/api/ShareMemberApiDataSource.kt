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
import me.proton.core.drive.share.member.data.api.response.GetShareMemberResponse
import me.proton.core.drive.share.user.data.api.entities.ShareMemberDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.getMembers(block: RequestContext.() -> MockResponse) {
    routing {
        get("/drive/v2/shares/{shareId}/members") {
            block()
        }
    }
}

fun MockWebServer.getMembers(vararg emails: String) {
    getMembers {
        jsonResponse {
            GetShareMemberResponse(
                emails.map { email ->
                    ShareMemberDto(
                        memberId = "member-id-$email",
                        inviterEmail = "inviter@proton.me",
                        email = email,
                        permissions = 0,
                        keyPacket = "member-key-packet",
                        keyPacketSignature = "member-key-packet-signature",
                        sessionKeySignature = "member-session-key-signature",
                        createTime = 0,
                    )
                }
            )
        }
    }
}


fun MockWebServer.updateMember(block: RequestContext.() -> MockResponse) = routing {
    put("/drive/v2/shares/{shareId}/members/{memberId}", block)
}

fun MockWebServer.updateMember() = updateMember {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

fun MockWebServer.deleteMember(block: RequestContext.() -> MockResponse) = routing {
    delete("/drive/v2/shares/{shareId}/members/{memberId}", block)
}

fun MockWebServer.deleteMember() = deleteMember {
    jsonResponse {
        Response(ProtonApiCode.SUCCESS)
    }
}

