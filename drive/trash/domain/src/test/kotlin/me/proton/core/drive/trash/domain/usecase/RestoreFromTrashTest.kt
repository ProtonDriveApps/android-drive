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
import me.proton.core.drive.linktrash.data.test.repository.state
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RestoreFromTrashTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: LinkTrashRepository

    @Inject
    lateinit var manager: StubbedWorkManager

    @Inject
    lateinit var restoreFromTrash: RestoreFromTrash

    @Before
    fun setUp() {
        hiltRule.inject()
    }


    @Test
    fun success() = runTest {
        restoreFromTrash(userId, fileId)

        assertEquals(TrashState.RESTORING, repository.state[listOf(fileId)])
    }

    @Test
    fun failing() = runTest {
        manager.behavior = StubbedWorkManager.BEHAVIOR_ERROR

        restoreFromTrash(userId, fileId)

        assertEquals(TrashState.TRASHED, repository.state[listOf(fileId)])
    }
}