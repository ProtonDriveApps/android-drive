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

package me.proton.core.drive.linkupload.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.factory.UploadBlockFactoryImpl
import me.proton.core.drive.linkupload.data.repository.LinkUploadRepositoryImpl
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.uploadFileLink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GetUploadFileLinksPagedTest {


    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var getUploadFileLinksPaged: GetUploadFileLinksPaged

    private lateinit var mainRoot: FolderId
    private lateinit var folderA: FolderId
    private lateinit var folderB: FolderId
    private lateinit var photoRoot: FolderId

    private lateinit var folderAUploads: List<UploadFileLink>
    private lateinit var folderBUploads: List<UploadFileLink>
    private lateinit var folderPhotoUploads: List<UploadFileLink>

    @Before
    fun setUp() = runTest {
        val linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
        getUploadFileLinksPaged = GetUploadFileLinksPaged(
            linkUploadRepository,
            object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader = ""
                override val dbPageSize = 150
            }
        )

        mainRoot = database.myFiles {
            folder("folderA")
            folder("folderB")
        }
        photoRoot = database.photo {}

        folderA = FolderId(mainRoot.shareId, "folderA")
        folderB = FolderId(mainRoot.shareId, "folderB")

        folderAUploads = with(folderA) {
            listOf(
                uploadFileLink(1),
                uploadFileLink(2),
                uploadFileLink(3),
            )
        }

        folderBUploads = with(folderB) {
            listOf(
                uploadFileLink(4),
                uploadFileLink(5),
            )
        }

        folderPhotoUploads = with(photoRoot) {
            listOf(
                uploadFileLink(6),
            )
        }
        linkUploadRepository.insertUploadFileLinks(
            folderAUploads + folderBUploads + folderPhotoUploads
        )
    }

    @Test
    fun all() = runTest {
        assertEquals(
            folderAUploads + folderBUploads + folderPhotoUploads,
            getUploadFileLinksPaged(userId),
        )
    }

    @Test
    fun byShareMain() = runTest {
        assertEquals(
            folderAUploads + folderBUploads,
            getUploadFileLinksPaged(userId, mainRoot.shareId),
        )
    }

    @Test
    fun bySharePhoto() = runTest {
        assertEquals(
            folderPhotoUploads,
            getUploadFileLinksPaged(userId, photoShareId),
        )
    }

    @Test
    fun byFolderA() = runTest {
        assertEquals(
            folderAUploads,
            getUploadFileLinksPaged(userId, folderA),
        )
    }

    @Test
    fun byFolderB() = runTest {
        assertEquals(
            folderAUploads,
            getUploadFileLinksPaged(userId, folderA),
        )
    }

    @Test
    fun byFolderPhotoRoot() = runTest {
        assertEquals(
            folderPhotoUploads,
            getUploadFileLinksPaged(userId, photoRoot),
        )
    }

}
