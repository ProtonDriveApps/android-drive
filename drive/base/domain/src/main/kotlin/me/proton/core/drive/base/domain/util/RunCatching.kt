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

package me.proton.core.drive.base.domain.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Catches [Exception], not [Throwable], and rethrows [CancellationException]. */
inline fun <T> coRunCatchingException(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

suspend inline fun <T> coRunCatchingException(
    coroutineContext: CoroutineContext,
    crossinline block: suspend () -> T,
) = withContext(coroutineContext) { coRunCatchingException { block() } }

@Deprecated(
    "Catches Exception, not Throwable, so it no longer behaves like runCatching",
    ReplaceWith("coRunCatchingException(block)"),
)
inline fun <T> coRunCatching(block: () -> T): Result<T> = coRunCatchingException(block)

@Deprecated(
    "Catches Exception, not Throwable, so it no longer behaves like runCatching",
    ReplaceWith("coRunCatchingException(coroutineContext, block)"),
)
suspend inline fun <T> coRunCatching(
    coroutineContext: CoroutineContext,
    crossinline block: suspend () -> T,
) = coRunCatchingException(coroutineContext, block)
