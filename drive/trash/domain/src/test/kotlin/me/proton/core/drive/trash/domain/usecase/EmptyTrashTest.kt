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
import me.proton.core.drive.base.data.test.manager.assertHasWork
import me.proton.core.drive.share.data.test.repository.StubbedShareRepository.Companion.mainShareId
import me.proton.core.drive.share.data.test.repository.StubbedShareRepository.Companion.photoShareId
import me.proton.core.drive.share.domain.repository.ShareRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class EmptyTrashTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var shareRepository: ShareRepository

    @Inject
    lateinit var manager: StubbedWorkManager

    @Inject
    lateinit var emptyTrash: EmptyTrash

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `with shareId`() = runTest {
        emptyTrash(userId, setOf(shareId))

        manager.assertHasWork("emptyTrash", userId, shareId)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `without shareId`() = runTest {
        emptyTrash(userId)

        manager.assertHasWork("emptyTrash", userId, mainShareId, photoShareId)
    }
}