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

package me.proton.core.drive.drivelink.crypto.data.usecase

import android.net.Uri
import android.util.Base64
import me.proton.core.auth.domain.usecase.ForkSession
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetUserEmail
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.GetSessionForkPayload
import me.proton.core.drive.cryptobase.domain.usecase.GenerateNewSessionKey
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetSessionForkProtonDocumentUriString
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class GetSessionForkProtonDocumentUriStringImpl @Inject constructor(
    private val getShare: GetShare,
    private val getUserEmail: GetUserEmail,
    private val configurationProvider: ConfigurationProvider,
    private val generateNewSessionKey: GenerateNewSessionKey,
    private val getSessionForkPayload: GetSessionForkPayload,
    private val forkSession: ForkSession,
) : GetSessionForkProtonDocumentUriString {

    override suspend fun invoke(driveLink: DriveLink): Result<String> = coRunCatching {
        generateNewSessionKey().getOrThrow().use { sessionKey ->
            Uri.Builder().buildSessionForkProtonDocumentUri(
                host = configurationProvider.host,
                selector = forkSession(
                    userId = driveLink.userId,
                    payload = getSessionForkPayload(userId = driveLink.userId, sessionKey = sessionKey).getOrThrow(),
                    childClientId = PROTON_DOCS_CHILD_CLIENT_ID,
                    independent = false,
                ),
                sessionKey = sessionKey,
                driveLink = driveLink,
                email = driveLink.getEmail(),
            ).toString()
        }
    }

    private suspend fun DriveLink.getEmail() = getShare(shareId).toResult().getOrThrow()
        .addressId
        ?.let { addressId ->
            getUserEmail(userId, addressId)
        } ?: getUserEmail(userId)

    companion object {
        private const val PROTON_DOCS_CHILD_CLIENT_ID = "web-docs"
    }
}

internal fun Uri.Builder.buildSessionForkProtonDocumentUri(
    host: String,
    selector: String,
    sessionKey: SessionKey,
    driveLink: DriveLink,
    email: String
): Uri = this
    .scheme("https")
    .authority("docs.$host")
    .appendPath("login")
    .appendFragment(selector, sessionKey, driveLink, email)
    .build()

internal fun Uri.Builder.appendFragment(
    selector: String,
    sessionKey: SessionKey,
    driveLink: DriveLink,
    email: String
): Uri.Builder = this
    .encodedFragment(
        buildString {
            append("selector=$selector")
            append("&sk=${Base64.encodeToString(sessionKey.key, Base64.URL_SAFE or Base64.NO_PADDING)}")
            append("&returnUrl=${Uri.encode(driveLink.returnUrl(email))}")
        }
    )

internal fun DriveLink.returnUrl(email: String): String =
    Uri.Builder()
        .appendPath("doc")
        .appendQueryParameter("volumeId", volumeId.id)
        .appendQueryParameter("linkId", id.id)
        .appendQueryParameter("email", email)
        .build()
        .toString()
