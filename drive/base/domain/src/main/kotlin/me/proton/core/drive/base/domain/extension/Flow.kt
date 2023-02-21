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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.util.coRunCatching

fun <T> flowOf(block: suspend () -> T): Flow<T> = flow { emit(block()) }

fun <R, T> Flow<Result<T>>.mapCatching(transform: suspend (T) -> R): Flow<Result<R>> =
    map { value -> coRunCatching { transform(value.getOrThrow()) } }

@JvmName("mapCatchingDataResult")
fun <R, T> Flow<DataResult<T>>.mapCatching(transform: suspend (T) -> R): Flow<Result<R>> =
    filterSuccessOrError().map { value -> coRunCatching { transform(value.toResult().getOrThrow()) } }

inline fun <R, T> Flow<T>.mapWithPrevious(crossinline transform: suspend (T?, T) -> R): Flow<R> {
    var previous: T? = null
    return map { current ->
        transform(previous, current).also {
            previous = current
        }
    }
}
