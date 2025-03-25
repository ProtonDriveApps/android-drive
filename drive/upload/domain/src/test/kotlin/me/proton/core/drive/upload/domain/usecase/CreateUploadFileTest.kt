/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.upload.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadFileProperties
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.upload.domain.resolver.UriResolver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class CreateUploadFileTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var createUploadFile: CreateUploadFile

    private val uriResolver = object : UriResolver {
        override suspend fun <T> useInputStream(
            uriString: String,
            block: suspend (InputStream) -> T,
        ): T = error("Should not call uri resolver")

        override suspend fun getName(uriString: String) = error("Should not call uri resolver")
        override suspend fun getSize(uriString: String) = error("Should not call uri resolver")
        override suspend fun getMimeType(uriString: String) = error("Should not call uri resolver")
        override suspend fun getLastModified(uriString: String) = error("Should not call uri resolver")
        override suspend fun getParentName(uriString: String) = error("Should not call uri resolver")
        override suspend fun getUriInfo(uriString: String) = error("Should not call uri resolver")
    }

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }
        val configurationProvider = object : ConfigurationProvider {
            override val host: String = ""
            override val baseUrl: String = ""
            override val appVersionHeader: String = ""
        }
        val linkUploadRepository = mockk<LinkUploadRepository>()
        coEvery { linkUploadRepository.insertUploadFileLinks(any()) } answers { firstArg() }
        createUploadFile = CreateUploadFile(
            linkUploadRepository,
            getUploadFileName = GetUploadFileName(
                uriResolver = uriResolver,
                validateLinkName = ValidateLinkName(configurationProvider)
            ),
            getUploadFileMimeType = GetUploadFileMimeType(uriResolver),
            getUploadFileSize = GetUploadFileSize(uriResolver),
            getUploadFileLastModified = GetUploadFileLastModified(uriResolver),
            getUploadFileUriInfo = GetUploadFileUriInfo(uriResolver),
        )
    }

    @Test
    fun `Given upload file properties when create upload file should create it without calling the content resolver`() =
        runTest {
            val uploadFiles = createUploadFile(
                userId = userId,
                volumeId = volumeId,
                parentId = folderId,
                uploadFileDescriptions = listOf(
                    UploadFileDescription(
                        uri = "uri",
                        properties = UploadFileProperties(
                            name = "name",
                            mimeType = "mimeType",
                            size = 0.bytes,
                            lastModified = TimestampMs(0),
                        ),
                    )
                ),
                shouldDeleteSourceUri = false,
                networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
                shouldAnnounceEvent = false,
                cacheOption = CacheOption.ALL,
                priority = 0,
                shouldBroadcastErrorMessage = false
            ).getOrThrow()

            assertEquals(
                listOf(
                    UploadFileLink(
                        id = 0,
                        userId = userId,
                        volumeId = volumeId,
                        shareId = folderId.shareId,
                        parentLinkId = folderId,
                        uriString = "uri",
                        name = "name",
                        mimeType = "mimeType",
                        size = 0.bytes,
                        lastModified = TimestampMs(0),
                        shouldDeleteSourceUri = false,
                        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
                        shouldAnnounceEvent = false,
                        priority = 0,
                        shouldBroadcastErrorMessage = false,
                    )
                ), uploadFiles
            )
        }
}
