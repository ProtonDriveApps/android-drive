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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.photos.domain.usecase.RemovePhotosFromAlbum
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.documentsprovider.domain.usecase.ExportToDownload
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbums
import me.proton.core.drive.feature.flag.domain.usecase.AlbumsFeatureFlag
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.CreateAlbumEntry
import me.proton.core.drive.files.presentation.entry.DownloadEntry
import me.proton.core.drive.files.presentation.entry.MoveEntry
import me.proton.core.drive.files.presentation.entry.RemoveFromAlbumEntry
import me.proton.core.drive.files.presentation.entry.TrashEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.SharingDetails
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultipleFileOrFolderOptionsViewModelTest {
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getSelectedDriveLinks = mockk<GetSelectedDriveLinks>()
    private val sendToTrash = mockk<SendToTrash>()
    private val exportToDownload = mockk<ExportToDownload>()
    private val deselectLinks = mockk<DeselectLinks>()
    private val addToAlbumInfo = mockk<AddToAlbumInfo>()
    private val getFeatureFlagFlow = mockk<GetFeatureFlagFlow>()
    private val configurationProvider = mockk<ConfigurationProvider>()
    private val removePhotosFromAlbum = mockk<RemovePhotosFromAlbum>()
    private val broadcastMessages = mockk<BroadcastMessages>()

    @Before
    fun before() {
        coEvery { savedStateHandle.get<String>(any()) } returns "value"
        coEvery { getSelectedDriveLinks.invoke(any()) } returns flowOf()
        coEvery { getFeatureFlagFlow.invoke(any(), any(), any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.NOT_FOUND))
        }
        coEvery { getFeatureFlagFlow.refreshAfterDuration} answers {
            { false }
        }
        coEvery { configurationProvider.albumsFeatureFlag} returns true
    }

    @Test
    fun `files options`() = runTest {
        // Given
        val files = listOf(fileDriveLink)

        // When
        val entries = files.fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                DownloadEntry::class,
                MoveEntry::class,
                TrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `photos options without album feature flag`() = runTest {
        // Given
        val files = listOf(photoDriveLink)

        // When
        val entries = files.fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                DownloadEntry::class,
                TrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `photos options with album feature flag `() = runTest {
        // Given
        val featureFlagId = driveAlbums(UserId("value"))
        coEvery { getFeatureFlagFlow(featureFlagId, any(), any()) } returns
                flowOf( FeatureFlag(featureFlagId, State.ENABLED))
        val files = listOf(photoDriveLink)

        // When
        val entries = files.fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                RemoveFromAlbumEntry::class,
                CreateAlbumEntry::class,
                DownloadEntry::class,
                TrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }


    private suspend fun List<DriveLink>.fileOptionEntries() =
        fileOrFolderOptionsViewModel().entries(
            driveLinks = this,
            runAction = {},
            navigateToMove = { _: SelectionId, _: FolderId? -> },
            navigateToCreateNewAlbum = {  },
            dismiss = {},
        ).filterNotNull().first()

    private fun fileOrFolderOptionsViewModel() = MultipleFileOrFolderOptionsViewModel(
        appContext = ApplicationProvider.getApplicationContext(),
        savedStateHandle = savedStateHandle,
        getSelectedDriveLinks = getSelectedDriveLinks,
        sendToTrash = sendToTrash,
        exportToDownload = exportToDownload,
        deselectLinks = deselectLinks,
        addToAlbumInfo = addToAlbumInfo,
        removePhotosFromAlbum = removePhotosFromAlbum,
        getFeatureFlagFlow = getFeatureFlagFlow,
        albumsFeatureFlag = AlbumsFeatureFlag(getFeatureFlagFlow, configurationProvider),
        broadcastMessages = broadcastMessages,
        configurationProvider = configurationProvider,
    )

    private val fileLink = Link.File(
        id = FileId(ShareId(UserId("USER_ID"), "SHARE_ID"), "ID"),
        parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
        activeRevisionId = "revision",
        size = Bytes(123),
        lastModified = TimestampS(System.currentTimeMillis() / 1000),
        mimeType = "video/mp4",
        numberOfAccesses = 2,
        isShared = true,
        uploadedBy = "m4@proton.black",
        hasThumbnail = false,
        name = "Link name",
        key = "key",
        passphrase = "passphrase",
        passphraseSignature = "signature",
        contentKeyPacket = "contentKeyPacket",
        contentKeyPacketSignature = null,
        attributes = Attributes(0),
        permissions = Permissions(0),
        state = Link.State.ACTIVE,
        nameSignatureEmail = "",
        hash = "",
        expirationTime = null,
        nodeKey = "",
        nodePassphrase = "",
        nodePassphraseSignature = "",
        signatureEmail = "",
        creationTime = TimestampS(0),
        trashedTime = null,
        shareUrlExpirationTime = null,
        xAttr = null,
        sharingDetails = SharingDetails(
            shareId = ShareId(UserId("USER_ID"), "SHARING_ID"),
            shareUrlId = ShareUrlId(ShareId(UserId("USER_ID"), "SHARING_ID"), "")
        ),
        photoCaptureTime = null,
        photoContentHash = null,
        mainPhotoLinkId = null,
    )

    private val fileDriveLink = DriveLink.File(
        link = fileLink,
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = true,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = DownloadState.Downloaded(),
        trashState = null,
        cryptoName = CryptoProperty.Decrypted("Link name", VerificationStatus.Success),
        cryptoXAttr = CryptoProperty.Decrypted(
            """{"Common":{"ModificationTime":"2023-07-27T13:52:23.636Z"},"Media":{"Duration":46}}""",
            VerificationStatus.Success,
        ),
        shareInvitationCount = null,
        shareMemberCount = null,
        shareUser = null,
        sharePermissions = Permissions.admin
    )

    private val photoDriveLink = fileDriveLink.copy(
        link = fileLink.copy(
            photoCaptureTime = TimestampS(0),
            photoContentHash = "",
            mainPhotoLinkId = "MAIN_ID"
        )
    )
}
