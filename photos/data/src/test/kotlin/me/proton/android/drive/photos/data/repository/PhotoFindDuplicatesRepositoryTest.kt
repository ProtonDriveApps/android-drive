/*
 * Copyright (c) 2023-2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.api.request.FindDuplicatesRequest
import me.proton.core.drive.photo.data.api.response.DuplicateDto
import me.proton.core.drive.photo.data.api.response.FindDuplicatesResponse
import me.proton.core.drive.photo.data.repository.PhotoRepositoryImpl
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class PhotoFindDuplicatesRepositoryTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private val photoApiDataSource = mockk<PhotoApiDataSource>()
    private val getPhotoShare = mockk<GetPhotoShare>()
    private val photoShare = mockk<Share>()

    private lateinit var photoFindDuplicatesRepository: PhotoFindDuplicatesRepository

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }

        coEvery { getPhotoShare(any()) } returns flowOf(photoShare.asSuccess)
        coEvery { photoShare.volumeId } returns volumeId

        photoFindDuplicatesRepository = PhotoFindDuplicatesRepository(
            PhotoRepositoryImpl(photoApiDataSource, database.db),
            getPhotoShare,
        )
    }

    @Test
    fun test() = runTest {
        coEvery {
            photoApiDataSource.findDuplicate(
                userId, volumeId, FindDuplicatesRequest(
                    nameHashes = listOf("hash"),
                    clientUids = emptyList(),
                )
            )
        } answers {
            FindDuplicatesResponse(
                code = ProtonApiCode.SUCCESS,
                duplicates = listOf(
                    duplicateDto("hash0", ""),
                    duplicateDto("hash1", "client-id"),
                    duplicateDto("hash1", "other-client-id"),
                )
            )
        }

        val backupDuplicates = photoFindDuplicatesRepository.findDuplicates(
            folderId = folderId,
            nameHashes = listOf("hash"),
            clientUids = listOf("client-id"),
        )

        assertEquals(
            listOf(
                backupDuplicate("hash0", ""),
                backupDuplicate("hash1", "client-id"),
            ),
            backupDuplicates,
        )
    }

    private fun backupDuplicate(hash: String, clientId: String) = BackupDuplicate(
        parentId = folderId,
        hash = hash,
        contentHash = null,
        linkId = null,
        linkState = Link.State.ACTIVE,
        revisionId = null,
        clientUid = clientId,
    )

    private fun duplicateDto(hash: String, clientId: String) = DuplicateDto(
        hash = hash,
        contentHash = null,
        state = 1,
        clientUid = clientId,
        linkId = null,
        revisionId = null
    )

}
