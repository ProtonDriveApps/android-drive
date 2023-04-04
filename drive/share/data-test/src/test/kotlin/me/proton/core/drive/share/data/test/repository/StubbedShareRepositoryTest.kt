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
package me.proton.core.drive.share.data.test.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.onSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.data.test.nullable.NullableShare
import me.proton.core.drive.share.data.test.repository.StubbedShareRepository.Companion.mainShareId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.entity.ShareInfo
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StubbedShareRepositoryTest {

    private val repository = StubbedShareRepository()

    private val userId = UserId("user-id")
    private val volumeId = VolumeId("volume-id")

    @Test
    fun getSharesFlow() = runTest {
        assertNotNull(repository.getSharesFlow(userId).first())
    }


    @Test
    fun `getSharesFlow volumeId`() = runTest {
        assertNotNull(repository.getSharesFlow(userId, volumeId).first())
    }

    @Test
    fun `hasShares userId`() = runTest {
        assertTrue(repository.hasShares(userId))
    }

    @Test
    fun `hasShares userId volumeId`() = runTest {
        assertTrue(repository.hasShares(userId, volumeId))
    }

    @Test
    fun `hasShare shareId`() = runTest {
        assertTrue(repository.hasShare(mainShareId))
    }

    @Test
    fun `hasShareWithKey shareId`() = runTest {
        assertTrue(repository.hasShareWithKey(mainShareId))
    }

    @Test
    fun getShareFlow() = runTest {
        assertTrue(repository.getShareFlow(mainShareId).first() is DataResult.Success)
    }

    @Test
    fun fetchShare() = runTest {
        val sharesFlow = repository.getSharesFlow(userId)
        val shareId2 = ShareId(userId, "share-2")
        repository.fetchShare(shareId2)
        sharesFlow.first().onSuccess { shares ->
            assertEquals(listOf(mainShareId, shareId2), shares.map { it.id })
        }
    }

    @Test
    fun createShare() = runTest {
        val sharesFlow = repository.getSharesFlow(userId)
        val shareIdResult = repository.createShare(
            userId, volumeId, ShareInfo(
                addressId = AddressId("address-id"),
                name = "create",
                rootLinkId = "rootLinkId",
                shareKey = "shareKey",
                sharePassphrase = "sharePassphrase",
                sharePassphraseSignature = "sharePassphraseSignature",
                passphraseKeyPacket = "",
                nameKeyPacket = ""
            )
        )
        sharesFlow.first().onSuccess { shares ->
            assertEquals(NullableShare(
                ShareId(userId, "share-create"),
                volumeId,
                addressId = AddressId("address-id"),
                rootLinkId = "rootLinkId",
                key = "shareKey",
                passphrase = "sharePassphrase",
                passphraseSignature = "sharePassphraseSignature",
            ), shares.first { it.id == shareIdResult.getOrThrow() })
        }
    }

    @Test
    fun deleteShare() = runTest {
        val sharesFlow = repository.getSharesFlow(userId)
        val shareId2 = ShareId(userId, "share-2")
        repository.fetchShare(shareId2)
        repository.deleteShare(shareId2, false)
        sharesFlow.first().onSuccess { shares ->
            assertEquals(listOf(mainShareId), shares.map { it.id })
        }
    }
}