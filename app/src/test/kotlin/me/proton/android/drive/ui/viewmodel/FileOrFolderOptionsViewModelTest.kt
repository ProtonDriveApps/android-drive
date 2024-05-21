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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.ui.options.OptionsFilter
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.documentsprovider.domain.usecase.ExportTo
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.offline.domain.usecase.ToggleOffline
import me.proton.core.drive.drivelink.trash.domain.usecase.ToggleTrashState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.DownloadFileEntity
import me.proton.core.drive.files.presentation.entry.FileInfoEntry
import me.proton.core.drive.files.presentation.entry.ManageAccessEntity
import me.proton.core.drive.files.presentation.entry.MoveFileEntry
import me.proton.core.drive.files.presentation.entry.RenameFileEntry
import me.proton.core.drive.files.presentation.entry.SendFileEntry
import me.proton.core.drive.files.presentation.entry.ShareViaInvitationsEntity
import me.proton.core.drive.files.presentation.entry.ShareViaLinkEntry
import me.proton.core.drive.files.presentation.entry.ToggleOfflineEntry
import me.proton.core.drive.files.presentation.entry.ToggleTrashEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.crypto.domain.usecase.CopyPublicUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert
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
    private val copyPublicUrl = mockk<CopyPublicUrl>()
    private val exportTo = mockk<ExportTo>()
    private val notifyActivityNotFound = mockk<NotifyActivityNotFound>()
    private val getFeatureFlagFlow = mockk<GetFeatureFlagFlow>()
    private lateinit var fileOrFolderOptionsViewModel: FileOrFolderOptionsViewModel

    @Before
    fun before() {
        coEvery { savedStateHandle.get<String>(any()) } returns "value"
        coEvery { savedStateHandle.get<OptionsFilter>("optionsFilter") } returns OptionsFilter.FILES
        coEvery { getFeatureFlagFlow.invoke(any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.NOT_FOUND))
        }
    }

    @Test
    fun `file options on main share url sharing only`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.asSuccess)
        fileOrFolderOptionsViewModel = FileOrFolderOptionsViewModel(
            savedStateHandle = savedStateHandle,
            getDriveLink = getDriveLink,
            toggleOffline = toggleOffline,
            toggleTrashState = toggleTrashState,
            copyPublicUrl = copyPublicUrl,
            exportTo = exportTo,
            notifyActivityNotFound = notifyActivityNotFound,
            getFeatureFlagFlow = getFeatureFlagFlow,
        )

        // When
        val entries = fileOrFolderOptionsViewModel.entries<DriveLink.File>(
            runAction = {},
            navigateToInfo = { _: LinkId -> },
            navigateToMove = { _: LinkId, _: FolderId? -> },
            navigateToRename = { _: LinkId -> },
            navigateToDelete = { _: LinkId -> },
            navigateToSendFile = { _: FileId -> },
            navigateToStopSharing = { _: LinkId -> },
            navigateToShareViaLink = { _: LinkId -> },
            navigateToManageAccess = { _: LinkId -> },
            navigateToShareViaInvitations = { _: LinkId -> },
            dismiss = {},
        ).filterNotNull().first()

        // Then
        Assert.assertTrue(entries.any { it is ToggleOfflineEntry })
        Assert.assertTrue(entries.any { it is ShareViaLinkEntry })
        Assert.assertFalse(entries.any { it is ManageAccessEntity })
        Assert.assertFalse(entries.any { it is ShareViaInvitationsEntity })
        Assert.assertTrue(entries.any { it is SendFileEntry })
        Assert.assertTrue(entries.any { it is DownloadFileEntity })
        Assert.assertTrue(entries.any { it is MoveFileEntry })
        Assert.assertTrue(entries.any { it is RenameFileEntry })
        Assert.assertTrue(entries.any { it is FileInfoEntry })
        Assert.assertTrue(entries.any { it is ToggleTrashEntry })
    }

    @Test
    fun `file options on main share url and member sharing only`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(fileDriveLink.asSuccess)

        coEvery { getFeatureFlagFlow.invoke(any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.ENABLED))
        }
        fileOrFolderOptionsViewModel = FileOrFolderOptionsViewModel(
            savedStateHandle = savedStateHandle,
            getDriveLink = getDriveLink,
            toggleOffline = toggleOffline,
            toggleTrashState = toggleTrashState,
            copyPublicUrl = copyPublicUrl,
            exportTo = exportTo,
            notifyActivityNotFound = notifyActivityNotFound,
            getFeatureFlagFlow = getFeatureFlagFlow,
        )

        // When
        val entries = fileOrFolderOptionsViewModel.entries<DriveLink.File>(
            runAction = {},
            navigateToInfo = { _: LinkId -> },
            navigateToMove = { _: LinkId, _: FolderId? -> },
            navigateToRename = { _: LinkId -> },
            navigateToDelete = { _: LinkId -> },
            navigateToSendFile = { _: FileId -> },
            navigateToStopSharing = { _: LinkId -> },
            navigateToShareViaLink = { _: LinkId -> },
            navigateToManageAccess = { _: LinkId -> },
            navigateToShareViaInvitations = { _: LinkId -> },
            dismiss = {},
        ).filterNotNull().first()

        // Then
        Assert.assertTrue(entries.any { it is ToggleOfflineEntry })
        Assert.assertFalse(entries.any { it is ShareViaLinkEntry })
        Assert.assertTrue(entries.any { it is ManageAccessEntity })
        Assert.assertTrue(entries.any { it is ShareViaInvitationsEntity })
        Assert.assertTrue(entries.any { it is SendFileEntry })
        Assert.assertTrue(entries.any { it is DownloadFileEntity })
        Assert.assertTrue(entries.any { it is MoveFileEntry })
        Assert.assertTrue(entries.any { it is RenameFileEntry })
        Assert.assertTrue(entries.any { it is FileInfoEntry })
        Assert.assertTrue(entries.any { it is ToggleTrashEntry })
    }

    @Test
    fun `file options on photo share`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<LinkId>(), any()) } returns flowOf(photoDriveLink.asSuccess)
        fileOrFolderOptionsViewModel = FileOrFolderOptionsViewModel(
            savedStateHandle = savedStateHandle,
            getDriveLink = getDriveLink,
            toggleOffline = toggleOffline,
            toggleTrashState = toggleTrashState,
            copyPublicUrl = copyPublicUrl,
            exportTo = exportTo,
            notifyActivityNotFound = notifyActivityNotFound,
            getFeatureFlagFlow = getFeatureFlagFlow,
        )

        // When
        val entries = fileOrFolderOptionsViewModel.entries<DriveLink.File>(
            runAction = {},
            navigateToInfo = { _: LinkId -> },
            navigateToMove = { _: LinkId, _: FolderId? -> },
            navigateToRename = { _: LinkId -> },
            navigateToDelete = { _: LinkId -> },
            navigateToSendFile = { _: FileId -> },
            navigateToStopSharing = { _: LinkId -> },
            navigateToShareViaLink = { _: LinkId -> },
            navigateToManageAccess = { _: LinkId -> },
            navigateToShareViaInvitations = { _: LinkId -> },
            dismiss = {},
        ).filterNotNull().first()

        // Then
        Assert.assertTrue(entries.any { it is ToggleOfflineEntry })
        Assert.assertTrue(entries.any { it is ShareViaLinkEntry })
        Assert.assertTrue(entries.any { it is SendFileEntry })
        Assert.assertTrue(entries.any { it is DownloadFileEntity })
        Assert.assertFalse(entries.any { it is MoveFileEntry })
        Assert.assertFalse(entries.any { it is RenameFileEntry })
        Assert.assertTrue(entries.any { it is FileInfoEntry })
        Assert.assertTrue(entries.any { it is ToggleTrashEntry })
    }

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
        isFavorite = false,
        attributes = Attributes(0),
        permissions = Permissions(0),
        state = Link.State.ACTIVE,
        nameSignatureEmail = "",
        hash = "",
        expirationTime = null,
        nodeKey = "",
        nodePassphrase = "",
        nodePassphraseSignature = "",
        signatureAddress = "",
        creationTime = TimestampS(0),
        trashedTime = null,
        shareUrlExpirationTime = null,
        xAttr = null,
        sharingDetails = null,
        photoCaptureTime = null,
        photoContentHash = null,
        mainPhotoLinkId = null,
    )

    private val fileDriveLink = DriveLink.File(
        link = fileLink,
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = true,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = DownloadState.Downloaded(emptyList()),
        trashState = null,
        cryptoName = CryptoProperty.Decrypted("Link name", VerificationStatus.Success),
        cryptoXAttr = CryptoProperty.Decrypted(
            """{"Common":{"ModificationTime":"2023-07-27T13:52:23.636Z"},"Media":{"Duration":46}}""",
            VerificationStatus.Success,
        ),
    )

    private val photoDriveLink = fileDriveLink.copy(
        link = fileLink.copy(
            photoCaptureTime = TimestampS(0),
            photoContentHash = "",
            mainPhotoLinkId = "MAIN_ID"
        )
    )
}
