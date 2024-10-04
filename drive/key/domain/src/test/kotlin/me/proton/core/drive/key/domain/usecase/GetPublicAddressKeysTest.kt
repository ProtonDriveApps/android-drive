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
import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.db.test.NullableAddressEntity
import me.proton.core.drive.db.test.NullableAddressKeyEntity
import me.proton.core.drive.db.test.NullablePublicAddressInfoEntity
import me.proton.core.drive.db.test.NullablePublicAddressKeyDataEntity
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.addAddress
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.withKey
import me.proton.core.drive.key.data.db.entity.StalePublicAddressKeyEntity
import me.proton.core.drive.key.domain.entity.AddressKeys
import me.proton.core.drive.key.domain.entity.PublicAddressKeys
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetPublicAddressKeysTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject lateinit var getPublicAddressKeys: GetPublicAddressKeys

    private val email1 = requireNotNull(NullableUserEntity().email)
    private val email2 = "${userId.id}.2@proton.test"
    private val email3 = "${userId.id}.3@proton.test"
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
            addAddress(email2)
        }
    }

    @Test
    fun `get public address keys will return user address key if matched by email`() = runTest {
        // When
        val key = getPublicAddressKeys(userId, email1).getOrThrow()

        // Then
        assertTrue(key is AddressKeys)
        assertEquals(1, key.keyHolder.keys.size)
        assertEquals(KeyId("key-id-user-id@proton.test"), key.keyHolder.keys.first().keyId)
    }

    @Test
    fun `get public address keys for non local user will return local data if available`() = runTest {
        // When
        val key = getPublicAddressKeys(userId, otherUserEmail).getOrThrow()

        // Then

        assertTrue(key is PublicAddressKeys)
        val publicAddressKeys = requireNotNull(key as? PublicAddressKeys)
        val firstKey = publicAddressKeys.publicAddressInfoKeyHolder.publicAddressInfo.address.keys.first()
        assertEquals("public-key-$otherUserEmail", firstKey.publicKey.key)
    }

    @Test
    fun `when local public address key is marked as stale, fresh key is fetched`() = runTest {
        // Given
        driveRule.db.stalePublicAddressKeyDao.insertOrUpdate(
            StalePublicAddressKeyEntity(
                userId = userId,
                email = otherUserEmail,
                key = "public-key-$otherUserEmail",
            )
        )
        val newPublicKeyPrefix = "new-public-key"
        val newPublicKey = "$newPublicKeyPrefix-$otherUserEmail"
        driveRule.server.getPublicAddressKeysAll(newPublicKeyPrefix)

        // When
        val key = getPublicAddressKeys(userId, otherUserEmail).getOrThrow()

        // Then
        assertTrue(key is PublicAddressKeys)
        val publicAddressKeys = requireNotNull(key as? PublicAddressKeys)
        val firstKey = publicAddressKeys.publicAddressInfoKeyHolder.publicAddressInfo.address.keys.first()
        assertEquals(newPublicKey, firstKey.publicKey.key)
    }

    @Test
    fun `when email is not found in local address keys and fetching of public address keys results with KEY_GET_ADDRESS_MISSING, all local address keys are returned`() = runTest {
        driveRule.server.getPublicAddressKeysAll {
            jsonResponse { CodeResponse(code = ProtonApiCode.KEY_GET_ADDRESS_MISSING) }
        }

        // When
        val key = getPublicAddressKeys(userId, email3).getOrThrow()

        // Then
        assertTrue(key is AddressKeys)
        assertEquals(2, key.keyHolder.keys.size)
    }
}
