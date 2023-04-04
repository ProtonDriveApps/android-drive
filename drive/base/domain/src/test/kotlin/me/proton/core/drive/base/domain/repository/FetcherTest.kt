/*
 * Copyright (c) 2023 Proton AG.
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class FetcherTest {

    @Test
    fun `Given success When fetch Then emit value`() = runTest {
        val values = flow<DataResult<Unit>> {
            fetcher {
                emit(DataResult.Success(ResponseSource.Local, Unit))
            }
        }.toList()
        assertEquals(DataResult.Processing(ResponseSource.Remote), values[0])
        assertEquals(DataResult.Success(ResponseSource.Local, Unit), values[1])
    }

    @Test
    fun `Given api exception When fetch Then emit error with api exception`() = runTest {
        val errorMessage = "Unable to resolve host"
        val value = flow<DataResult<Nothing>> {
            fetcher {
                throw ApiException(
                    ApiResult.Error.Connection(
                        cause = UnknownHostException(errorMessage)
                    )
                )
            }
        }.last()

        val error = value as DataResult.Error
        assertEquals(errorMessage, error.message)
        assertEquals(ResponseSource.Remote, error.source)
        assertEquals(ApiException::class.java, error.cause?.javaClass)
    }

    @Test
    fun `Given runtime exception When fetch Then emit error with runtime exception`() = runTest {
        val errorMessage = "error"
        val value = flow<DataResult<Nothing>> {
            fetcher {
                throw RuntimeException(errorMessage)
            }
        }.last()

        val error = value as DataResult.Error
        assertEquals(errorMessage, error.message)
        assertEquals(ResponseSource.Local, error.source)
        assertEquals(RuntimeException::class.java, error.cause?.javaClass)
    }

    @Test
    fun `Given cancellation exception When fetch Then emit error with runtime exception`() {
        val flow = flow<DataResult<Nothing>> {
            fetcher {
                throw CancellationException()
            }
        }
        assertThrows(CancellationException::class.java) {
            runTest { flow.last() }
        }
    }
}