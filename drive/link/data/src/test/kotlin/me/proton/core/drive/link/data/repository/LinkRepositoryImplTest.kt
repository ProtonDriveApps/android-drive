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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.usecase.SortLinksByParents
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LinkRepositoryImplTest {

    @get:Rule
    val driveRule = DriveRule(this)

    private lateinit var repository: LinkRepositoryImpl
    private val sortLinksByParents = SortLinksByParents()

    @Inject lateinit var configurationProvider: ConfigurationProvider

    @Before
    fun setUp() {
        repository = LinkRepositoryImpl(mockk(), driveRule.db, sortLinksByParents, configurationProvider)
    }

    @Test
    fun empty() = runTest {
        driveRule.db.myFiles {}

        val linkIds = repository.findLinkIds(userId, volumeId, "link-id")

        assertEquals(emptyList<LinkId>(), linkIds)
    }

    @Test
    fun one() = runTest {
        driveRule.db.user {
            volume {
                mainShare {
                    file("link-id-1")
                }
                photoShare {
                    file("link-id-2")
                }
            }
        }

        val linkIds = repository.findLinkIds(userId, volumeId, "link-id-1")

        assertEquals(listOf(FileId(mainShareId, "link-id-1")), linkIds)
    }

    @Test
    fun many() = runTest {
        driveRule.db.user {
            volume {
                mainShare {
                    folder("link-id")
                }
                photoShare {
                    file("link-id")
                }
            }
        }

        val linkIds = repository.findLinkIds(userId, volumeId, "link-id")

        assertEquals(
            listOf(
                FolderId(mainShareId, "link-id"),
                FileId(photoShareId, "link-id"),
            ), linkIds
        )
    }

    @Test
    fun `no tags`() = runTest {
        driveRule.db.user {
            volume {
                photoShare {
                    file("link-id", tags = emptyList())
                }
            }
        }

        val link = repository.getLinkFlow(FileId(photoShareId, "link-id")).toResult().getOrThrow() as Link.File

        assertEquals(emptyList<PhotoTag>(), link.tags)
    }

    @Test
    fun `many tags`() = runTest {
        val videoTags: List<PhotoTag> = listOf(PhotoTag.Favorites, PhotoTag.Videos)
        val livePhotoTags: List<PhotoTag> = listOf(PhotoTag.LivePhotos)
        driveRule.db.user {
            volume {
                photoShare {
                    file("link-id-video", tags = videoTags.map { tag -> tag.value })
                    file("link-id-live-photo", tags = livePhotoTags.map { tag -> tag.value })
                }
            }
        }

        val videoLink = repository.getLinkFlow(FileId(photoShareId, "link-id-video")).toResult().getOrThrow() as Link.File
        val livePhotoLink = repository.getLinkFlow(FileId(photoShareId, "link-id-live-photo")).toResult().getOrThrow() as Link.File

        assertEquals(livePhotoTags, livePhotoLink.tags)
        assertEquals(videoTags, videoLink.tags)
    }

}
