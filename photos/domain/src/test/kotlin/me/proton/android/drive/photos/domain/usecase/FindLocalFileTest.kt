/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.domain.usecase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.data.usecase.FindLocalFile
import me.proton.core.drive.db.test.NullableLinkEntity
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.linkFilePropertiesEntity
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.di.PhotoBindContextModule
import me.proton.core.drive.photo.domain.usecase.ScanFileByName
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(PhotoBindContextModule::class)
@RunWith(RobolectricTestRunner::class)
class FindLocalFileTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var findLocalFile: FindLocalFile

    private lateinit var fileId: FileId

    @Test
    fun `no file found`() = runTest {
        driveRule.db.photo {
            fileId = file(
                link = NullableLinkEntity(
                    id = "photo-id",
                    parentId = this.link.id,
                    type = 2L,
                    name = "photo.png",
                )
            )
        }
        assertNull(findLocalFile(photoVolumeId, fileId).getOrThrow())
    }

    @Test
    fun `multiple files found`() = runTest {
        driveRule.db.photo {
            val link = NullableLinkEntity(
                id = "photo-id",
                parentId = this.link.id,
                type = 2L,
                name = "multiple",
            )
            fileId = file(
                link = link,
                properties = linkFilePropertiesEntity(
                    link = link,
                    photoContentHash = "e350034ea761f3f8180f9610380347f5c05619b23ff065b1000725ed5f789c7e" // test://empty
                ),
            )
        }
        assertEquals("test://empty", findLocalFile(photoVolumeId, fileId).getOrThrow())
    }

    @Test(expected = FileNotFoundException::class)
    fun `missing file found`() = runTest {
        driveRule.db.photo {
            val link = NullableLinkEntity(
                id = "photo-id",
                parentId = this.link.id,
                type = 2L,
                name = "missing",
            )
            fileId = file(
                link = link,
                properties = linkFilePropertiesEntity(link = link, photoContentHash = "missing"),
            )
        }
        assertNull(findLocalFile(photoVolumeId, fileId).getOrThrow())
    }

    companion object {
        val scanner = ScanFileByName { name ->
            when (name) {
                "multiple" -> listOf("test://uri1", "test://empty")
                "missing" -> listOf("test://missing")
                else -> emptyList()
            }
        }

    }

    @Module
    @InstallIn(SingletonComponent::class)
    @Suppress("Unused")
    object TestPhotosConfigurationModule {
        @Provides
        @Singleton
        fun provideScanFileByNameRepository(): ScanFileByName = scanner
    }
}
