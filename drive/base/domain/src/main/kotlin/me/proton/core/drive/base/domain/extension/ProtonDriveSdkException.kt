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

package me.proton.core.drive.base.domain.extension

import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError

inline fun <T> ProtonDriveSdkException.onProtonHttpException(
    block: (protonData: ApiResult.Error.ProtonData) -> T
): T? =
    error
        ?.firstErrorWithProtonApiCodeOrNull
        ?.toProtonData()
        ?.let { protonData ->
            block(protonData)
        }

val ProtonSdkError?.firstErrorWithProtonApiCodeOrNull: ProtonSdkError? get() =
    firstErrorOrNull { error ->
        error.primaryCode != null && error.domain == ProtonSdkError.ErrorDomain.Api
    } ?: firstErrorOrNull { error ->
        error.primaryCode != null && error.domain == ProtonSdkError.ErrorDomain.BusinessLogic
    }

fun ProtonSdkError?.firstErrorDomainOrNull(errorDomain: ProtonSdkError.ErrorDomain): ProtonSdkError? =
    firstErrorOrNull { error -> error.domain == errorDomain }

fun ProtonSdkError?.firstErrorOrNull(predicate: (ProtonSdkError) -> Boolean): ProtonSdkError? {
    var protonSdkError: ProtonSdkError? = this
    while (protonSdkError != null) {
        if (predicate(protonSdkError)) return protonSdkError
        protonSdkError = protonSdkError.innerError
    }
    return null
}

fun ProtonSdkError.toProtonData(): ApiResult.Error.ProtonData? =
    primaryCode?.let { primaryCode ->
        ApiResult.Error.ProtonData(
            code = primaryCode.toInt(),
            error = message,
        )
    }

fun ProtonDriveSdkException.toApiException(): ApiException? = error?.toApiException(this)

fun ProtonSdkError.toApiException(
    cause: Throwable? = null,
): ApiException? = when (domain) {
    ProtonSdkError.ErrorDomain.Api -> {
        val secondaryCode = secondaryCode?.toInt()
        if (secondaryCode != null) {
            ApiException(
                ApiResult.Error.Http(
                    httpCode = secondaryCode,
                    message = message,
                    proton = toProtonData(),
                    cause = cause,
                )
            )
        } else {
            null
        }
    }


    ProtonSdkError.ErrorDomain.Network -> ApiException(
        ApiResult.Error.NoInternet(cause = cause)
    )

    ProtonSdkError.ErrorDomain.Transport -> ApiException(
        ApiResult.Error.Timeout(isConnectedToNetwork = false, cause = cause)
    )

    ProtonSdkError.ErrorDomain.Serialization -> ApiException(
        ApiResult.Error.Parse(cause = cause)
    )

    else -> innerError?.toApiException(cause)
}
