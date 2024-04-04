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

package me.proton.core.drive.data.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.data.domain.repository.DataRepository
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.get
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.routing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PingActiveUserTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject lateinit var pingActiveUser: PingActiveUser
    @Inject lateinit var baseRepository: BaseRepository
    @Inject lateinit var dataRepository: DataRepository
    @Inject lateinit var configurationProvider: ConfigurationProvider

    @Before
    fun before() = runTest {
        driveRule.server.routing {
            get("/drive/me/active") {
                jsonResponse {
                    CodeResponse(1000)
                }
            }
        }
        driveRule.db.user {  }
    }

    @Test
    fun firstPingActiveUser() = runTest {
        // When
        pingActiveUser(userId).getOrThrow()

        // Then
        assertNotNull(
            baseRepository.getLastFetch(userId, dataRepository.pingActiveUserUrl)
        )
    }

    @Test
    fun beforePingActiveUserDurationElapsesPingActiveUserShouldDoNothing() = runTest {
        // Given
        val currentTimeInMs = TimestampMs().value
        val activeUserPingDurationInMs = configurationProvider.activeUserPingDuration.inWholeMilliseconds
        val offsetDurationInMs = 10.seconds.inWholeMilliseconds
        val lastFetchTimestamp = currentTimeInMs - activeUserPingDurationInMs + offsetDurationInMs
        baseRepository.setLastFetch(lastFetchTimestamp)

        // When
        pingActiveUser(userId).getOrThrow()

        // Then
        assertEquals(
            lastFetchTimestamp,
            requireNotNull(baseRepository.getLastFetch(userId, dataRepository.pingActiveUserUrl)).value,
        )
    }

    @Test
    fun afterPingActiveUserDurationElapsesPingActiveUserShouldPingAgain() = runTest {
        // Given
        val currentTimeInMs = TimestampMs().value
        val activeUserPingDurationInMs = configurationProvider.activeUserPingDuration.inWholeMilliseconds
        val offsetDurationInMs = 10.seconds.inWholeMilliseconds
        val lastFetchTimestamp = currentTimeInMs - activeUserPingDurationInMs - offsetDurationInMs
        baseRepository.setLastFetch(lastFetchTimestamp)

        // When
        pingActiveUser(userId).getOrThrow()

        // Then
        assertTrue(
            requireNotNull(
                baseRepository.getLastFetch(userId, dataRepository.pingActiveUserUrl)
            ).value > lastFetchTimestamp
        )
    }

    private suspend fun BaseRepository.setLastFetch(lastFetchTimestamp: Long) {
        setLastFetch(userId, dataRepository.pingActiveUserUrl, TimestampMs(lastFetchTimestamp))
    }
}
