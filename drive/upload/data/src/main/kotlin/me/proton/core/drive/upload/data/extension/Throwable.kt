/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.upload.data.extension

import me.proton.android.drive.verifier.data.extension.log
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable

internal val Throwable.isRetryable: Boolean
    get() = when (this) {
        is ApiException -> {
            when (this.error) {
                is ApiResult.Error.Timeout,
                is ApiResult.Error.NoInternet -> true
                else -> this.error.isRetryable()
            }
        }
        is VerifierException -> this.cause.isRetryable
        else -> false
    }

internal fun Throwable.log(tag: String, message: String? = null): Throwable = this.also {
    when (this) {
        is VerifierException -> this.log(tag, message.orEmpty())
        else -> this.log(tag, message)
    }
}
