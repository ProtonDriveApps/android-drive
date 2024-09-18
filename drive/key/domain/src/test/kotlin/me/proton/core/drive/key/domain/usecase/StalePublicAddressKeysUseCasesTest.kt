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

package me.proton.core.drive.key.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.NullablePublicAddressInfoEntity
import me.proton.core.drive.db.test.NullablePublicAddressKeyDataEntity
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.withKey
import me.proton.core.drive.test.DriveRule
import me.proton.core.key.domain.entity.key.PublicKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class StalePublicAddressKeysUseCasesTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject lateinit var hasStalePublicAddressKeys: HasStalePublicAddressKeys
    @Inject lateinit var markPublicAddressKeyAsStale: MarkPublicAddressKeyAsStale
    @Inject lateinit var removeAllStalePublicAddressKeys: RemoveAllStalePublicAddressKeys

    private val otherUserEmail = "other@proton.test"

    @Before
    fun before() = runTest {
        driveRule.db.user {
            withKey(
                publicAddressInfoEntity = NullablePublicAddressInfoEntity(
                    email = otherUserEmail
                ),
                publicAddressKeyDataEntity = NullablePublicAddressKeyDataEntity(
                    email = otherUserEmail
                )
            )
        }
    }

    @Test
    fun `has stale key returns true, when key is marked as stale`() = runTest {
        // Given
        markPublicAddressKeyAsStale(
            userId,
            PublicKey(
                key = "public-key-$otherUserEmail",
                isPrimary = true,
                isActive = true,
                canEncrypt = true,
                canVerify = true,
            )
        ).getOrThrow()

        // When
        val result = hasStalePublicAddressKeys(userId, otherUserEmail).getOrThrow()

        // Then
        assertTrue(result)
    }

    @Test
    fun `has stale key returns false, when key is not marked as stale`() = runTest {
        // When
        val result = hasStalePublicAddressKeys(userId, otherUserEmail).getOrThrow()

        // Then
        assertFalse(result)
    }

    @Test
    fun `when marked as stale, has stale key returns true`() = runTest {
        // When
        markPublicAddressKeyAsStale(
            userId,
            PublicKey(
                key = "public-key-$otherUserEmail",
                isPrimary = true,
                isActive = true,
                canEncrypt = true,
                canVerify = true,
            ),
        ).getOrThrow()

        // Then
        assertTrue(hasStalePublicAddressKeys(userId, otherUserEmail).getOrThrow())
    }

    @Test
    fun `mark non-existent key as stale does not insert it in db`() = runTest {
        // Given
        val email = "some@proton.test"

        // When
        markPublicAddressKeyAsStale(
            userId,
            PublicKey(
                key = "public-key-$email",
                isPrimary = true,
                isActive = true,
                canEncrypt = true,
                canVerify = true,
            ),
        ).getOrThrow()

        // Then
        assertFalse(hasStalePublicAddressKeys(userId, email).getOrThrow())
    }

    @Test
    fun `when remove all stale keys is successful then has stale key returns false`() = runTest {
        // Given
        driveRule.db.user {
            db.publicAddressKeyDataDao.insertOrUpdate(
                NullablePublicAddressKeyDataEntity(
                    email = otherUserEmail,
                    publicKey = "public-key-2-$otherUserEmail",
                )
            )
        }
        markPublicAddressKeyAsStale(
            userId,
            listOf(
                PublicKey(
                    key = "public-key-$otherUserEmail",
                    isPrimary = false,
                    isActive = true,
                    canEncrypt = true,
                    canVerify = true,
                ),
                PublicKey(
                    key = "public-key-2-$otherUserEmail",
                    isPrimary = true,
                    isActive = true,
                    canEncrypt = true,
                    canVerify = true,
                ),
            ),
        )

        // When
        removeAllStalePublicAddressKeys(userId, otherUserEmail).getOrThrow()

        // Then
        assertFalse(hasStalePublicAddressKeys(userId, otherUserEmail).getOrThrow())
    }
}
