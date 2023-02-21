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
package me.proton.core.drive.base.domain.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.network.domain.ApiException

suspend inline fun <T> FlowCollector<DataResult<T>>.fetcher(fetchAction: () -> Unit) {
    emit(DataResult.Processing(ResponseSource.Remote))
    try {
        fetchAction()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        emit(e.error.toDataResult())
    } catch (e: RuntimeException) {
        emit(DataResult.Error.Local(e.message, e))
    }
}

suspend inline fun <T> FlowCollector<DataResult<List<T>>>.listFetcherEmitOnEmpty(fetchAction: () -> List<T>) {
    emit(DataResult.Processing(ResponseSource.Remote))
    try {
        fetchAction().onEmpty { emit(DataResult.Success(ResponseSource.Remote, emptyList())) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        emit(e.error.toDataResult())
    } catch (e: RuntimeException) {
        emit(DataResult.Error.Local(e.message, e))
    }
}

inline fun <T> List<T>.onEmpty(block: () -> Unit) = takeIf { isEmpty() }?.let { block() }
