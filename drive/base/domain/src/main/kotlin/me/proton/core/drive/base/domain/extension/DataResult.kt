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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.util.kotlin.exhaustive

fun <T> T?.asSuccessOrNullAsError(): DataResult<T> = this
    ?.let { value -> DataResult.Success(ResponseSource.Local, value) }
    ?: DataResult.Error.Local("No value", NoSuchElementException())

val <T> T.asSuccess: DataResult.Success<T>
    get() = DataResult.Success(ResponseSource.Local, this)

inline fun <T> DataResult<T>.onFailure(
    action: (error: DataResult.Error) -> Unit,
) = apply {
    if (this is DataResult.Error) {
        action(this)
    }
}

suspend fun <T> Flow<DataResult<T>>.resultValueOrNull() = transform { result ->
    when (result) {
        is DataResult.Success -> emit(result.value)
        is DataResult.Error -> emit(null)
        is DataResult.Processing -> Unit
    }.exhaustive
}.first()

suspend fun <T> Flow<DataResult<T>>.resultValueOrThrow() = transform { result ->
    when (result) {
        is DataResult.Success -> emit(result.value)
        is DataResult.Error -> throw result.cause!!
        is DataResult.Processing -> Unit
    }.exhaustive
}.first()

fun <T> Flow<DataResult<T>>.filterSuccessOrError() = filter { result ->
    when (result) {
        is DataResult.Success -> true
        is DataResult.Error -> true
        is DataResult.Processing -> false
    }
}

suspend fun <T> Flow<DataResult<T>>.firstSuccessOrError(): DataResult<T> =
    filterSuccessOrError().first()

suspend fun <T> Flow<DataResult<T>>.toResult(): Result<T> = firstSuccessOrError().toResult()

fun <T> DataResult<T>.toResult() = when (this) {
    is DataResult.Processing ->
        throw IllegalArgumentException("Received DataResult.Processing, did you call filterSuccessOrError?")
    is DataResult.Success -> Result.success(value)
    is DataResult.Error -> Result.failure(RuntimeException(message, cause))
}

inline val <Key, Value> Map<Key, DataResult<Value>>.successes: Map<Key, Value>
    get() = filter { (_, value) -> value is DataResult.Success }.mapValues { (_, result) ->
        (result as DataResult.Success<Value>).value
    }

@Suppress("UNCHECKED_CAST")
inline fun <Key, Value> Map<Key, DataResult<Value>>.onFailure(
    action: (errors: Map<Key, DataResult.Error>, successes: Map<Key, Value>) -> Unit,
) = apply {
    if (any { (_, result) -> result is DataResult.Error }) {
        action(
            filter { (_, result) -> result is DataResult.Error } as Map<Key, DataResult.Error>,
            successes
        )
    }
}

inline fun <Key, Value> Map<Key, DataResult<Value>>.onSuccess(
    action: (successes: Map<Key, Value>) -> Unit,
) = apply {
    if (all { (_, result) -> result is DataResult.Success<Value> }) {
        action(successes)
    }
}

// Core's `transformSuccess` binds `DataResult`'s generic to a non-nullable type. This is temporary until fixed in Core
@ExperimentalCoroutinesApi
inline fun <T, R> Flow<DataResult<T>>.transformSuccess(
    crossinline transform: suspend FlowCollector<DataResult<R>>.(value: DataResult.Success<T>) -> Unit
): Flow<DataResult<R>> = transformLatest {
    when (it) {
        is DataResult.Processing -> emit(it)
        is DataResult.Error -> emit(it)
        is DataResult.Success -> transform(it)
    }.exhaustive
}
