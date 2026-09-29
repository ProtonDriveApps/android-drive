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

package me.proton.core.drive.base.domain.extension

import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.drive.sdk.ProtonDriveSdkException


fun ApiException.hasHttpCode(code: Int): Boolean =
    (error as? ApiResult.Error.Http)?.httpCode == code

fun Throwable.hasHttpCode(code: Int): Boolean = when (this) {
    is ProtonDriveSdkException -> when (val error = toApiException()) {
        is ApiException -> error.hasHttpCode(code)
        else -> false
    }
    is ApiException -> hasHttpCode(code)
    else -> false
}

inline fun <T> Throwable.onProtonHttpException(
    block: (protonData: ApiResult.Error.ProtonData) -> T
): T? = when {
    this is ProtonDriveSdkException -> onProtonHttpException(block)
    else -> ((this as? ApiException)?.error as? ApiResult.Error.Http)?.proton?.let { protonData ->
        block(protonData)
    }
}

fun Throwable.hasThrowableOrCauseProtonErrorCode(code: Int): Boolean = when (this) {
    is ProtonDriveSdkException -> hasProtonErrorCode(code) || cause?.hasProtonErrorCode(code) == true
    else -> hasProtonErrorCode(code) || cause?.hasProtonErrorCode(code) == true
}

inline fun <reified T : Throwable> Throwable?.findThrowable(): T? {
    var current = this
    while (true) {
        when (current) {
            null -> return null
            is T -> return current
            else -> current = current.cause
        }
    }
}

/** Errors that leave the VM unusable, so continuing only produces cascading, unactionable failures. */
val Throwable.isFatal: Boolean get() =
    this is VirtualMachineError || this is LinkageError || this is ThreadDeath
