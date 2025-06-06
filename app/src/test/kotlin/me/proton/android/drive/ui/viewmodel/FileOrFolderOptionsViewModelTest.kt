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
import me.proton.android.drive.photos.domain.usecase.AddPhotosToStream
import me.proton.android.drive.photos.domain.usecase.RemovePhotosFromAlbum
import me.proton.android.drive.ui.viewmodel.FileOrFolderOptionsViewModel.Companion.KEY_ALBUM_ID
import me.proton.android.drive.usecase.LeaveShare
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.android.drive.usecase.OpenProtonDocumentInBrowser
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.documentsprovider.domain.usecase.ExportTo
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.offline.domain.usecase.ToggleOffline
import me.proton.core.drive.drivelink.photo.domain.usecase.ToggleFavorite
import me.proton.core.drive.drivelink.photo.domain.usecase.UpdateAlbumCover
import me.proton.core.drive.drivelink.trash.domain.usecase.ToggleTrashState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.AddToAlbumsFileEntry
import me.proton.core.drive.files.presentation.entry.DownloadFileEntry
import me.proton.core.drive.files.presentation.entry.FileInfoEntry
import me.proton.core.drive.files.presentation.entry.ManageAccessEntry
import me.proton.core.drive.files.presentation.entry.MoveFileEntry
import me.proton.core.drive.files.presentation.entry.OpenInBrowserProtonDocsEntry
import me.proton.core.drive.files.presentation.entry.RemoveFromAlbumFileEntry
import me.proton.core.drive.files.presentation.entry.RemoveMeEntry
import me.proton.core.drive.files.presentation.entry.RenameFileEntry
import me.proton.core.drive.files.presentation.entry.SendFileEntry
import me.proton.core.drive.files.presentation.entry.SetAsAlbumCoverEntry
import me.proton.core.drive.files.presentation.entry.ShareViaInvitationsEntry
import me.proton.core.drive.files.presentation.entry.ToggleFavoriteFileEntry
import me.proton.core.drive.files.presentation.entry.ToggleOfflineEntry
import me.proton.core.drive.files.presentation.entry.ToggleTrashEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.SharingDetails
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import me.proton.core.drive.volume.domain.usecase.HasPhotoVolume
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileOrFolderOptionsViewModelTest {
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDriveLink = mockk<GetDecryptedDriveLink>()
    private val toggleOffline = mockk<ToggleOffline>()
    private val toggleTrashState = mockk<ToggleTrashState>()
    private val leaveShare = mockk<LeaveShare>()
    private val configurationProvider = mockk<ConfigurationProvider>()
    private val broadcastMessages = mockk<BroadcastMessages>()
    private val exportTo = mockk<ExportTo>()
    private val notifyActivityNotFound = mockk<NotifyActivityNotFound>()
    private val getFeatureFlagFlow = mockk<GetFeatureFlagFlow>()
    private val openProtonDocumentInBrowser = mockk<OpenProtonDocumentInBrowser>()
    private val updateAlbumCover = mockk<UpdateAlbumCover>()
    private val removePhotosFromAlbum = mockk<RemovePhotosFromAlbum>()
    private val deselectLinks = mockk<DeselectLinks>()
    private val toggleFavorite = mockk<ToggleFavorite>()
    private val getOldestActiveVolume = mockk<GetOldestActiveVolume>()
    private val addPhotosToStream = mockk<AddPhotosToStream>()
    private val hasPhotoVolume = mockk<HasPhotoVolume>()
    private val selectLinks = mockk<SelectLinks>()

    @Before
    fun before() {
        coEvery { savedStateHandle.get<String>(any()) } answers {
            val key: String = arg(0)
            if (key == KEY_ALBUM_ID) null else "value"
        }

        coEvery { getFeatureFlagFlow.invoke(any(), any(), any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.NOT_FOUND))
        }
        coEvery { getFeatureFlagFlow.refreshAfterDuration} answers {
            { false }
        }
        coEvery { configurationProvider.albumsFeatureFlag} returns true
        coEvery { deselectLinks(any()) } returns Unit
        coEvery { getOldestActiveVolume(any(), any()) } returns flowOf(DataResult.Success(
            source = ResponseSource.Local,
            value = Volume(
                id = volumeId,
                shareId = "",
                linkId = "",
                usedSpace = 0.bytes,
                state = 0,
                createTime = TimestampS(),
                type = Volume.Type.PHOTO
            )
        ))
        coEvery { hasPhotoVolume.invoke(any()) } returns flowOf(true)
        coEvery { selectLinks(any()) } returns Result.success(SelectionId("selection-id"))
    }

    @Test
    fun `file options on main share no sharing`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.copy(
            link = fileLink.copy(
                isShared = false,
                sharingDetails = null,
            )
        ).asSuccess)

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                MoveFileEntry::class,
                RenameFileEntry::class,
                FileInfoEntry::class,
                ToggleTrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on main share no sharing no parent`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.copy(
            link = fileLink.copy(
                parentId = null,
                isShared = false,
                sharingDetails = null,
            )
        ).asSuccess)

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                FileInfoEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on main share url sharing only`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.asSuccess)

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                MoveFileEntry::class,
                RenameFileEntry::class,
                FileInfoEntry::class,
                ToggleTrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on main share url and member sharing only and owner permissions`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.asSuccess)
        coEvery { getFeatureFlagFlow.invoke(any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(
                if (id == FeatureFlagId.driveAlbumsTempDisabledOnRelease(UserId("value"))) {
                    FeatureFlag(id, State.NOT_FOUND)
                } else {
                    FeatureFlag(id, State.ENABLED)
                }
            )
        }
        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                MoveFileEntry::class,
                RenameFileEntry::class,
                FileInfoEntry::class,
                ToggleTrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on main share url and member sharing only and editor permissions`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(
            fileDriveLink.copy(
                sharePermissions = Permissions.editor,
                shareUser = shareUser.copy(permissions = Permissions.editor)
            ).asSuccess
        )
        coEvery { getFeatureFlagFlow.invoke(any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.ENABLED))
        }

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                MoveFileEntry::class,
                RenameFileEntry::class,
                FileInfoEntry::class,
                RemoveMeEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on main share url and member sharing only and viewer permissions`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(
            fileDriveLink.copy(
                sharePermissions = Permissions.viewer,
                shareUser = shareUser.copy(permissions = Permissions.viewer)
            ).asSuccess
        )
        coEvery { getFeatureFlagFlow.invoke(any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.ENABLED))
        }

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                FileInfoEntry::class,
                RemoveMeEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on photo share without photo volume`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(photoDriveLink.asSuccess)
        coEvery { hasPhotoVolume.invoke(any()) } returns flowOf(false)

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                FileInfoEntry::class,
                ToggleTrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on photo share with photo volume from album`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(photoDriveLink.asSuccess)
        val driveAlbumsFlagId = FeatureFlagId.driveAlbums(UserId("value"))
        coEvery { getFeatureFlagFlow(driveAlbumsFlagId, any(), any()) } returns
                flowOf( FeatureFlag(driveAlbumsFlagId, State.ENABLED))
        val driveAlbumsDisabledFlagId = FeatureFlagId.driveAlbumsDisabled(UserId("value"))
        coEvery { getFeatureFlagFlow(driveAlbumsDisabledFlagId, any(), any()) } returns
                flowOf( FeatureFlag(driveAlbumsDisabledFlagId, State.NOT_FOUND))
        coEvery { savedStateHandle.get<String>(KEY_ALBUM_ID) } returns "album-id"

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                SetAsAlbumCoverEntry::class,
                RemoveFromAlbumFileEntry::class,
                ToggleOfflineEntry::class,
                ToggleFavoriteFileEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                FileInfoEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on photo share with photo volume from photo stream`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(photoDriveLink.asSuccess)
        coEvery { savedStateHandle.get<String>(KEY_ALBUM_ID) } returns null
        coEvery { getFeatureFlagFlow.invoke(any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(
                if (id == FeatureFlagId.driveAlbumsDisabled(UserId("value"))) {
                    FeatureFlag(id, State.NOT_FOUND)
                } else {
                    FeatureFlag(id, State.ENABLED)
                }
            )
        }

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ToggleOfflineEntry::class,
                ToggleFavoriteFileEntry::class,
                AddToAlbumsFileEntry::class,
                SendFileEntry::class,
                DownloadFileEntry::class,
                FileInfoEntry::class,
                ToggleTrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `file options on main share for non-shared proton document`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.copy(
            link = fileLink.copy(
                isShared = false,
                sharingDetails = null,
                mimeType = "application/vnd.proton.doc"
            )
        ).asSuccess)

        // When
        val entries = fileOptionEntries()

        // Then
        assertEquals(
            listOf(
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                MoveFileEntry::class,
                RenameFileEntry::class,
                OpenInBrowserProtonDocsEntry::class,
                FileInfoEntry::class,
                ToggleTrashEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    private suspend fun fileOptionEntries() =
        fileOrFolderOptionsViewModel().entries<DriveLink.File>(
            runAction = {},
            navigateToInfo = { _: LinkId -> },
            navigateToMove = { _: LinkId, _: FolderId? -> },
            navigateToRename = { _: LinkId -> },
            navigateToDelete = { _: LinkId -> },
            navigateToSendFile = { _: FileId -> },
            navigateToManageAccess = { _: LinkId -> },
            navigateToShareViaInvitations = { _: LinkId -> },
            navigateToAddToAlbumsOptions = {},
            dismiss = {},
        ).filterNotNull().first()

    private fun fileOrFolderOptionsViewModel() = FileOrFolderOptionsViewModel(
        appContext = ApplicationProvider.getApplicationContext(),
        savedStateHandle = savedStateHandle,
        getDriveLink = getDriveLink,
        toggleOffline = toggleOffline,
        toggleTrashState = toggleTrashState,
        exportTo = exportTo,
        notifyActivityNotFound = notifyActivityNotFound,
        getFeatureFlagFlow = getFeatureFlagFlow,
        leaveShare = leaveShare,
        configurationProvider = configurationProvider,
        broadcastMessages = broadcastMessages,
        openProtonDocumentInBrowser = openProtonDocumentInBrowser,
        updateAlbumCover = updateAlbumCover,
        removePhotosFromAlbum = removePhotosFromAlbum,
        deselectLinks = deselectLinks,
        toggleFavorite = toggleFavorite,
        getOldestActiveVolume = getOldestActiveVolume,
        addPhotosToStream = addPhotosToStream,
        hasPhotoVolume = hasPhotoVolume,
        selectLinks = selectLinks,
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
        volumeId = volumeId,
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

    private val shareUser = ShareUser.Member(
        id ="id",
        inviter = "",
        email = "",
        createTime = TimestampS(),
        permissions = Permissions.owner,
        keyPacket = "",
        keyPacketSignature = null,
        sessionKeySignature = null,
    )

    private val photoDriveLink = fileDriveLink.copy(
        link = fileLink.copy(
            photoCaptureTime = TimestampS(0),
            photoContentHash = "",
            mainPhotoLinkId = "MAIN_ID"
        )
    )
}
