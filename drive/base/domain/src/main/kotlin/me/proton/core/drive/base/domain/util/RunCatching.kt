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

inline fun <R, reified T : Throwable> Result<R>.except(): Result<R> =
    onFailure { if (it is T) throw it }

inline fun <T> coRunCatching(block: () -> T) = runCatching(block).except<T, CancellationException>()

suspend inline fun <T> coRunCatching(coroutineContext: CoroutineContext, crossinline block: suspend () -> T) =
    withContext(coroutineContext) { coRunCatching { block() } }
