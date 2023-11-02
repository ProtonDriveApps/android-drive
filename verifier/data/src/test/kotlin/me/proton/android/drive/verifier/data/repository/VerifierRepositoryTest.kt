/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.verifier.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.verifier.data.api.VerifierApiDataSource
import me.proton.android.drive.verifier.data.api.response.GetVerificationDataResponse
import me.proton.android.drive.verifier.domain.entity.VerificationData
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.kotlin.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VerifierRepositoryTest {
    private val apiDataSource = mockk<VerifierApiDataSource>()
    private val userId = UserId("user-id")
    private val shareId = "share-id"
    private val linkId = "link-id"
    private val revisionId = "revision-id"
    private val contentKeyPacket = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val verificationCode = Base64.getEncoder().encodeToString(ByteArray(32) { i -> i.toByte() })
    private lateinit var repository: VerifierRepositoryImpl

    @Before
    fun before() {
        coEvery {
            apiDataSource.getVerificationData(
                userId = any(),
                shareId = any(),
                linkId = any(),
                revisionId = any(),
            )
        } returns GetVerificationDataResponse(
            code = 1000,
            verificationCode = verificationCode,
            contentKeyPacket = contentKeyPacket,
        )
        repository = VerifierRepositoryImpl(apiDataSource)
    }

    @Test
    fun `successful remove of verification data removes it from repository cache`() = runTest {
        // When
        repository.getVerificationData(userId, shareId, linkId, revisionId)
        repository.removeVerificationData(userId, shareId, linkId, revisionId)

        // Then
        val key = VerifierRepositoryImpl.VerificationDataKey(userId, shareId, linkId, revisionId)
        assertNull(repository.verificationDataCache[key])
    }

    @Test
    fun `successful verification data from verifier api data source and repository cache`() = runTest {
        // When
        val verificationData = repository.getVerificationData(userId, shareId, linkId, revisionId)

        // Then
        val expectedVerificationData = VerificationData(
            contentKeyPacket = contentKeyPacket,
            verificationCode = Base64.getDecoder().decode(verificationCode),
        )
        assertEquals(expectedVerificationData, verificationData) { "Verification data mismatch from data source" }
        val key = VerifierRepositoryImpl.VerificationDataKey(userId, shareId, linkId, revisionId)
        assertEquals(expectedVerificationData, repository.verificationDataCache[key]) {
            "Verification data mismatch from cache"
        }
    }

    @Test
    fun `cached verification data is provided when available`() = runTest {
        // Given
        repository.getVerificationData(userId, shareId, linkId, revisionId)
        coEvery {
            apiDataSource.getVerificationData(
                userId = any(),
                shareId = any(),
                linkId = any(),
                revisionId = any(),
            )
        } throws ApiException(ApiResult.Error.Http(httpCode = 500, message = "Internal server error"))

        // When
        val verificationData = repository.getVerificationData(userId, shareId, linkId, revisionId)

        // Then
        val expectedVerificationData = VerificationData(contentKeyPacket, Base64.getDecoder().decode(verificationCode))
        assertEquals(expectedVerificationData, verificationData) { "Verification data mismatch from data source" }
    }

    @Test(expected = ApiException::class)
    fun `when network error occurs it is propagated`() = runTest {
        // Given
        coEvery {
            apiDataSource.getVerificationData(
                userId = any(),
                shareId = any(),
                linkId = any(),
                revisionId = any(),
            )
        } throws ApiException(ApiResult.Error.Http(httpCode = 500, message = "Internal server error"))

        // Then
        repository.getVerificationData(userId, shareId, linkId, revisionId)
    }

    @Test
    fun `many concurrent calls to get same verification data leaves repository cache in consistent state`() = runTest {
        // When
        (0..99).map {
            async {
                repository.getVerificationData(userId, shareId, linkId, revisionId)
            }
        }.awaitAll()

        // Then
        val key = VerifierRepositoryImpl.VerificationDataKey(userId, shareId, linkId, revisionId)
        assertEquals(1, repository.verificationDataCache.keys.size) { "Invalid cache size" }
        val expectedVerificationData = VerificationData(contentKeyPacket, Base64.getDecoder().decode(verificationCode))
        assertEquals(expectedVerificationData, repository.verificationDataCache[key]) {
            "Verification data mismatch from cache"
        }
    }

    @Test
    fun `many concurrent calls to get different verification data and then to remove it leaves repository cache in consistent state`() = runTest {
        // When
        (0..99).map { i ->
            async {
                repository.getVerificationData(userId, shareId, linkId, "${revisionId}_$i")
                repository.removeVerificationData(userId, shareId, linkId, "${revisionId}_$i")
            }
        }.awaitAll()

        // Then
        assertEquals(0, repository.verificationDataCache.keys.size) { "Invalid cache size" }
    }
}
