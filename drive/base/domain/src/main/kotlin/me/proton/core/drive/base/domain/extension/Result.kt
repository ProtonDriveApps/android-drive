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
package me.proton.core.drive.base.domain.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.CoreLogger

fun <T> Result<T>.toDataResult(): DataResult<T> =
    fold(
        onSuccess = { value -> DataResult.Success(ResponseSource.Local, value) },
        onFailure = { throwable ->
            when (throwable) {
                is ApiException -> DataResult.Error.Remote(throwable.message, throwable)
                else -> DataResult.Error.Local(throwable.message, throwable)
            }
        }
    )

inline fun <T, R> Flow<Result<T>>.transformSuccess(
    crossinline onSuccess: suspend FlowCollector<Result<R>>.(value: T) -> Unit,
): Flow<Result<R>> =
    transform { result ->
        result.fold(
            onSuccess = { value -> onSuccess(value) },
            onFailure = { throwable -> emit(Result.failure(throwable)) }
        )
    }

fun <T> List<Result<T>>.throwOnFailure(lazyMessage: (Int) -> String): List<Result<T>> {
    val failureCount = count { result -> result.isFailure }
    if (failureCount > 0) {
        throw IllegalStateException(
            lazyMessage(failureCount),
            first { result -> result.isFailure }.exceptionOrNull(),
        )
    }
    return this
}

fun <T> Result<T>.getOrNull(tag: String, message: String? = null): T? = this
    .onFailure { error ->
        CoreLogger.d(tag, error, message.orEmpty())
    }
    .getOrNull()
