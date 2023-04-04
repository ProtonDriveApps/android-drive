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
package me.proton.core.drive.trash.domain.usecase

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.data.test.manager.StubbedWorkManager
import me.proton.core.drive.link.data.test.NullableFile
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.data.test.repository.state
import me.proton.core.drive.linktrash.data.test.repository.stateForLinks
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SendToTrashTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: LinkTrashRepository

    @Inject
    lateinit var manager: StubbedWorkManager

    @Inject
    lateinit var sendToTrash: SendToTrash

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `same folder`() = runTest {
        val link1 = NullableFile(folderId, "file-1")
        val link2 = NullableFile(folderId, "file-2")

        sendToTrash(userId, listOf(link1, link2))

        assertEquals(TrashState.TRASHING, repository.stateForLinks(link1, link2))
    }

    @Test
    fun `two folders`() = runTest {

        val share1 = ShareId(userId, "share-1")
        val share2 = ShareId(userId, "share-2")
        val folder1 = FolderId(share1, "folder-1")
        val folder2 = FolderId(share2, "folder-2")
        val link1 = NullableFile(folder1, "file-1")
        val link2 = NullableFile(folder2, "file-2")

        sendToTrash(userId, listOf(link1, link2))

        assertEquals(TrashState.TRASHING, repository.stateForLinks(link1))
        assertEquals(TrashState.TRASHING, repository.stateForLinks(link2))
    }

    @Test
    fun failing() = runTest {
        manager.behavior = StubbedWorkManager.BEHAVIOR_ERROR

        sendToTrash(userId, NullableFile(folderId))

        assertEquals(emptyMap<LinkId, TrashState>(), repository.state)
    }
}