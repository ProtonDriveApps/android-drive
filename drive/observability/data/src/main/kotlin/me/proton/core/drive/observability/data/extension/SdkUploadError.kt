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

import me.proton.core.drive.observability.domain.metrics.sdk.UploadErrorsTotal
import me.proton.drive.sdk.telemetry.UploadError as SdkUploadError

fun SdkUploadError.toType(): UploadErrorsTotal.Type = when (this) {
    SdkUploadError.UNRECOGNIZED -> UploadErrorsTotal.Type.unknown
    SdkUploadError.SERVER_ERROR -> UploadErrorsTotal.Type.server_error
    SdkUploadError.NETWORK_ERROR -> UploadErrorsTotal.Type.network_error
    SdkUploadError.INTEGRITY_ERROR -> UploadErrorsTotal.Type.integrity_error
    SdkUploadError.RATE_LIMITED -> UploadErrorsTotal.Type.rate_limited
    SdkUploadError.VALIDATION_ERROR -> UploadErrorsTotal.Type.validation_error
    SdkUploadError.HTTP_CLIENT_SIDE_ERROR -> UploadErrorsTotal.Type.`4xx`
    SdkUploadError.UNKNOWN -> UploadErrorsTotal.Type.unknown
}
