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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.DeleteAlbumEntry
import me.proton.core.drive.files.presentation.entry.LeaveAlbumEntry
import me.proton.core.drive.files.presentation.entry.ManageAccessEntry
import me.proton.core.drive.files.presentation.entry.RenameFileEntry
import me.proton.core.drive.files.presentation.entry.ShareViaInvitationsEntry
import me.proton.core.drive.files.presentation.entry.ToggleOfflineEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.SharingDetails
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumOptionsViewModelTest {
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDriveLink = mockk<GetDecryptedDriveLink>()
    private val getFeatureFlagFlow = mockk<GetFeatureFlagFlow>()
    private val broadcastMessages = mockk<BroadcastMessages>()
    private val albumOptionsViewModel get() =
        AlbumOptionsViewModel(
            savedStateHandle = savedStateHandle,
            getDriveLink = getDriveLink,
            getFeatureFlagFlow = getFeatureFlagFlow,
            broadcastMessages = broadcastMessages,
        )

    @Before
    fun before() {
        coEvery { savedStateHandle.get<String>(any()) } returns "value"
        coEvery { getFeatureFlagFlow.invoke(any(), any(), any())} answers {
            val id: FeatureFlagId = arg(0)
            flowOf(FeatureFlag(id, State.NOT_FOUND))
        }
        coEvery { getFeatureFlagFlow.refreshAfterDuration} answers {
            { false }
        }
    }

    @Test
    fun `private album`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<AlbumId>(), any()) } returns flowOf(album.copy(
            link = albumLink.copy(
                isShared = false,
                sharingDetails = null,
            )
        ).asSuccess)

        // When
        val entries = albumOptionEntries()

        // Then
        assertEquals(
            listOf(
                //ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                RenameFileEntry::class,
                DeleteAlbumEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `album shared by me`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<AlbumId>(), any()) } returns flowOf(album.asSuccess)

        // When
        val entries = albumOptionEntries()

        // Then
        assertEquals(
            listOf(
                //ToggleOfflineEntry::class,
                ShareViaInvitationsEntry::class,
                ManageAccessEntry::class,
                RenameFileEntry::class,
                DeleteAlbumEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `album shared with me as viewer`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<AlbumId>(), any()) } returns flowOf(
            album.copy(
                sharePermissions = Permissions.viewer,
                shareUser = shareUser.copy(permissions = Permissions.viewer)
            ).asSuccess
        )

        // When
        val entries = albumOptionEntries()

        // Then
        assertEquals(
            listOf(
                //ToggleOfflineEntry::class,
                LeaveAlbumEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    @Test
    fun `album shared with me as editor`() = runTest {
        // Given
        coEvery { getDriveLink.invoke(any<AlbumId>(), any()) } returns flowOf(
            album.copy(
                sharePermissions = Permissions.viewer,
                shareUser = shareUser.copy(permissions = Permissions.editor)
            ).asSuccess
        )

        // When
        val entries = albumOptionEntries()

        // Then
        assertEquals(
            listOf(
                //ToggleOfflineEntry::class,
                LeaveAlbumEntry::class,
            ),
            entries.map { it.javaClass.kotlin }
        )
    }

    private suspend fun albumOptionEntries() =
        albumOptionsViewModel.entries(
            runAction = {},
            navigateToShareViaInvitations = { _: LinkId -> },
            navigateToManageAccess = { _: LinkId -> },
            navigateToRename = { _: LinkId -> },
            navigateToDelete = { _: AlbumId -> },
            navigateToLeave = { _: AlbumId -> },
            dismiss = {},
        ).filterNotNull().first()

    private val albumLink = Link.Album(
        id = AlbumId(ShareId(UserId("USER_ID"), "SHARE_ID"), "ID"),
        parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
        size = Bytes(123),
        lastModified = TimestampS(System.currentTimeMillis() / 1000),
        mimeType = "video/mp4",
        numberOfAccesses = 2,
        isShared = true,
        uploadedBy = "m4@proton.black",
        name = "Link name",
        key = "key",
        passphrase = "passphrase",
        passphraseSignature = "signature",
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
        nodeHashKey = "nodehashkey",
        isLocked = false,
        lastActivityTime = TimestampS(0),
        photoCount = 2,
    )

    val album = DriveLink.Album(
        link = albumLink,
        volumeId = volumeId,
        isMarkedAsOffline = true,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = DownloadState.Downloaded(),
        trashState = null,
        cryptoName = CryptoProperty.Decrypted("Album", VerificationStatus.Success),
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
}
