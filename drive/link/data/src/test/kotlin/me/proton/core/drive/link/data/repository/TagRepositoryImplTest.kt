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

package me.proton.core.drive.link.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.repository.TagRepository
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.deleteLinkTags
import me.proton.core.drive.test.api.postLinkFavorite
import me.proton.core.drive.test.api.postLinkTags
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagRepositoryImplTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var fileId: FileId

    @Inject
    lateinit var tagRepository: TagRepository

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.user {
            volume {
                mainShare {
                    file("link-id")
                }
            }
        }
        fileId = FileId(folderId.shareId, "link-id")
    }

    @Test
    fun favorite() = runTest {
        driveRule.server.postLinkFavorite()

        tagRepository.addFavorite(volumeId, fileId, null)
    }

    @Test
    fun addTags() = runTest {
        driveRule.server.postLinkTags()

        tagRepository.addTags(volumeId, fileId, listOf(PhotoTag.Screenshots))
    }

    @Test
    fun deleteTags() = runTest {
        driveRule.server.deleteLinkTags()
        tagRepository.deleteTags(volumeId, fileId, listOf(PhotoTag.Favorites))
    }

}
