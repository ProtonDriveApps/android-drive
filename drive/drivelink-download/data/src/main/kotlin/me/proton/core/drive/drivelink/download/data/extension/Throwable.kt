/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.extension

import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.data.extension.isHttpError
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.observability.domain.metrics.DownloadErrorsTotal
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.isHttpError
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

fun Throwable.toDownloadErrorType(): DownloadErrorsTotal.Type = when(this) {
    is ApiException -> when {
        isHttpError(429) -> DownloadErrorsTotal.Type.rate_limited
        isHttpError(400..499) -> DownloadErrorsTotal.Type.`4xx`
        isHttpError(500..599) -> DownloadErrorsTotal.Type.`5xx`
        else -> when (cause) {
            is UnknownHostException,
            is SocketTimeoutException,
            is ConnectException,
            is SSLHandshakeException -> DownloadErrorsTotal.Type.network_error
            else -> DownloadErrorsTotal.Type.unknown
        }
    }
    is UnknownHostException,
    is SocketTimeoutException,
    is ConnectException,
    is SSLHandshakeException -> DownloadErrorsTotal.Type.network_error
    is CryptoException -> DownloadErrorsTotal.Type.decryption_error
    else -> DownloadErrorsTotal.Type.unknown
}
