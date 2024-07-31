/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.base.data.extension

import me.proton.core.drive.base.domain.api.ProtonApiCode.FEATURE_DISABLED
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult.Error.Certificate
import me.proton.core.network.domain.ApiResult.Error.Connection
import me.proton.core.network.domain.ApiResult.Error.Http
import me.proton.core.network.domain.ApiResult.Error.NoInternet
import me.proton.core.network.domain.ApiResult.Error.ProtonData
import me.proton.core.network.domain.ApiResult.Error.Timeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.FileNotFoundException

@RunWith(Parameterized::class)
class ThrowableKtTest(
    private val wrapper: Wrapper,
    private val retryable: Boolean,
) {

    @Test
    fun isRetryable() {
        assertEquals(retryable, wrapper.error.isRetryable)
    }

    class Wrapper(val error: Throwable) {
        override fun toString(): String {
            return if (error is ApiException) {
                "${error.javaClass.name}: ${error.error}"
            } else {
                error.toString()
            }
        }
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0} should be retryable: {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(Wrapper(Throwable()), false),
            arrayOf(Wrapper(IllegalStateException()), false),
            arrayOf(Wrapper(IllegalArgumentException()), false),
            arrayOf(Wrapper(ApiException(NoInternet(FileNotFoundException()))), false),
            arrayOf(Wrapper(ApiException(NoInternet(null))), true),
            arrayOf(Wrapper(ApiException(Timeout(true))), true),
            arrayOf(Wrapper(ApiException(Certificate(IllegalStateException()))), true),
            arrayOf(Wrapper(ApiException(Connection(true))), true),
            arrayOf(Wrapper(ApiException(Http(400, ""))), false),
            arrayOf(Wrapper(ApiException(Http(408, ""))), true),
            arrayOf(Wrapper(ApiException(Http(421, ""))), true),
            arrayOf(Wrapper(ApiException(Http(424, ""))), false),
            arrayOf(
                Wrapper(
                    ApiException(
                        Http(424, "", ProtonData(FEATURE_DISABLED, ""))
                    )
                ), true
            ),
            arrayOf(Wrapper(ApiException(Http(429, ""))), true),
            arrayOf(Wrapper(ApiException(Http(500, ""))), true),
            arrayOf(Wrapper(ApiException(Http(503, ""))), true),
            arrayOf(Wrapper(ApiException(Http(505, ""))), true),
        )
    }
}
