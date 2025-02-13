/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.folder.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DeleteLocalContentTest {

    @get:Rule
    var driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var getLink: GetLink

    @Inject
    lateinit var getCacheFolder: GetCacheFolder

    @Inject
    lateinit var getPermanentFolder: GetPermanentFolder

    @Inject
    lateinit var deleteLocalContent: DeleteLocalContent

    @Test
    fun test() = runTest {
        // Given
        folderId = driveRule.db.user {
            volume {
                mainShare {
                    folder("folder-id-1") {
                        file("file-id-1")
                    }
                    file("file-id-2")
                }
            }
        }

        val file1 = getLink(FileId(mainShareId, "file-id-1")).toResult().getOrThrow()
        val file2 = getLink(FileId(mainShareId, "file-id-2")).toResult().getOrThrow()
        val storageFile1 =
            File(getPermanentFolder(userId, volumeId.id, file1.activeRevisionId), "block").apply {
                mkdirs()
                createNewFile()
            }
        val storageFile2 =
            File(getCacheFolder(userId, volumeId.id, file2.activeRevisionId), "block").apply {
                mkdirs()
                createNewFile()
            }

        // When
        deleteLocalContent(volumeId, folderId).getOrThrow()

        // Then
        assertFalse(storageFile1.exists())
        assertFalse(storageFile2.exists())
    }
}
