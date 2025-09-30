/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.data.test

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.get
import me.proton.core.drive.test.api.retryableErrorResponse
import me.proton.core.drive.test.api.routing
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.TimeoutOverride
import me.proton.core.network.domain.isRetryable
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MockWebServerTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var apiProvider: ApiProvider

    @Test
    fun response() = runTest(timeout = 10.seconds) {
        driveRule.server.routing {
            get("/tests/ping") {
                MockResponse()
            }
        }

        val result = apiProvider.get<TestApi>().invoke { ping(TimeoutOverride()) }

        assertTrue(result.isSuccess)
    }

    @Test
    fun errorResponse() = runTest(timeout = 10.seconds) {
        driveRule.server.routing {
            get("/tests/ping") {
                errorResponse()
            }
        }

        val result = apiProvider.get<TestApi>().invoke { ping(TimeoutOverride()) }

        assertFalse(result.isSuccess)
        assertFalse(result.isRetryable())
    }

    @Test
    fun retryableErrorResponse() = runTest(timeout = 10.seconds) {
        driveRule.server.routing {
            get("/tests/ping") {
                retryableErrorResponse()
            }
        }

        val result = apiProvider.get<TestApi>().invoke { ping(TimeoutOverride()) }

        assertFalse(result.isSuccess)
        assertTrue(result.isRetryable())
    }
}
