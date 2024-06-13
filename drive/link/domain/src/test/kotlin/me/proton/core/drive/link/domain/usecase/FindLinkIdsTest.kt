/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.link.domain.usecase

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.data.repository.LinkRepositoryImpl
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FindLinkIdsTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var findLinkIds: FindLinkIds
    private val sortLinksByParents = SortLinksByParents()

    @Before
    fun setUp() {
        findLinkIds = FindLinkIds(LinkRepositoryImpl(mockk(), database.db, sortLinksByParents))
    }

    @Test
    fun empty() = runTest {
        database.myFiles {}

        val linkIds = findLinkIds(userId, volumeId, "link-id").getOrThrow()

        assertEquals(emptyList<LinkId>(), linkIds)
    }

    @Test
    fun many() = runTest {
        database.db.user {
            volume {
                mainShare {
                    file("link-id")
                }
                photoShare {
                    file("link-id")
                }
            }
        }

        val linkIds = findLinkIds(userId, volumeId, "link-id").getOrThrow()

        assertEquals(listOf(
            FileId(mainShareId, "link-id"),
            FileId(photoShareId, "link-id"),
        ), linkIds)
    }
}
