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

package me.proton.core.drive.base.domain.usecase

import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.repository.ClientUidRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class GetOrCreateClientUidTest {

    private lateinit var getOrCreateClientUid: GetOrCreateClientUid
    private val uuid = UUID(0, 0)

    @Before
    fun setUp() {
        val repository = object : ClientUidRepository {
            private var clientUid: ClientUid? = null

            override suspend fun get(): ClientUid? = clientUid

            override suspend fun insert(clientUid: ClientUid) {
                this.clientUid = clientUid
            }

        }
        getOrCreateClientUid = GetOrCreateClientUid(
            GetClientUid(repository),
            CreateClientUid(repository, object : CreateUuid {
                override suspend fun invoke(coroutineContext: CoroutineContext): UUID = uuid
            })
        )
    }

    @Test
    fun `clientUid should only be created if it does not already exist`() = runTest {

        val clientUid1 = getOrCreateClientUid()
        val clientUid2 = getOrCreateClientUid()

        assertEquals(uuid.toString(), clientUid1.getOrThrow())
        assertEquals(clientUid1, clientUid2)
    }
}
