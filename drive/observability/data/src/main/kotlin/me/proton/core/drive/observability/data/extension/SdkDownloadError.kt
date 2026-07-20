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

package me.proton.core.drive.observability.data.extension

import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErrorsTotal
import me.proton.drive.sdk.telemetry.DownloadError as SdkDownloadError


fun SdkDownloadError.toType(): DownloadErrorsTotal.Type = when (this) {
    SdkDownloadError.UNRECOGNIZED -> DownloadErrorsTotal.Type.unknown
    SdkDownloadError.SERVER_ERROR -> DownloadErrorsTotal.Type.server_error
    SdkDownloadError.NETWORK_ERROR -> DownloadErrorsTotal.Type.network_error
    SdkDownloadError.DECRYPTION_ERROR -> DownloadErrorsTotal.Type.decryption_error
    SdkDownloadError.INTEGRITY_ERROR -> DownloadErrorsTotal.Type.integrity_error
    SdkDownloadError.RATE_LIMITED -> DownloadErrorsTotal.Type.rate_limited
    SdkDownloadError.VALIDATION_ERROR -> DownloadErrorsTotal.Type.validation_error
    SdkDownloadError.HTTP_CLIENT_SIDE_ERROR -> DownloadErrorsTotal.Type.`4xx`
    SdkDownloadError.UNKNOWN -> DownloadErrorsTotal.Type.unknown
}
