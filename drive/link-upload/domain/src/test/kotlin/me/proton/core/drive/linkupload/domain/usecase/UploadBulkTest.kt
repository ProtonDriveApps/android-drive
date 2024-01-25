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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.linkupload.data.factory.UploadBlockFactoryImpl
import me.proton.core.drive.linkupload.data.repository.LinkUploadRepositoryImpl
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UploadBulkTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var createUploadBulk: CreateUploadBulk
    private lateinit var deleteUploadBulk: DeleteUploadBulk

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        val linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
        createUploadBulk = CreateUploadBulk(linkUploadRepository)
        deleteUploadBulk = DeleteUploadBulk(linkUploadRepository)
    }

    @Test
    fun `Upload bulk should conserve the order of uris`() = runTest {
        val uploadFileDescriptions = listOf(
            "content://media/external/images/media/001",
            "content://media/external/videos/media/001",
            "content://media/external/images/media/002",
            "content://media/external/images/media/003",
            "content://media/external/images/media/004",
            "content://media/external/videos/media/002",
            "content://com.android.providers.downloads.documents/document/001"
        ).map { uri -> UploadFileDescription(uri) }
        val id = createUploadBulk(
            volumeId = VolumeId(volumeId),
            parent = folderId.folder(),
            uploadFileDescriptions = uploadFileDescriptions,
            networkTypeProviderType = NetworkTypeProviderType.BACKUP,
            shouldAnnounceEvent = false,
            priority = UploadFileLink.BACKUP_PRIORITY,
            shouldBroadcastErrorMessage = false,
        ).getOrThrow().id

        val uploadBulk =  deleteUploadBulk(id).getOrThrow()

        assertEquals(
            uploadFileDescriptions,
            uploadBulk.uploadFileDescriptions,
        )
    }

    private suspend fun FolderId.folder() =
        database.db.linkDao.getLinkWithPropertiesFlow(shareId.userId, shareId.id, id)
            .first()?.toLink() as Link.Folder

}
