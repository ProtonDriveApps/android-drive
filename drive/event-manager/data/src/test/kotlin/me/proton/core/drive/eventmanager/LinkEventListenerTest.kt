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

package me.proton.core.drive.eventmanager

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.eventmanager.api.response.CreateLinksEvent
import me.proton.core.drive.eventmanager.api.response.DeleteLinksEvent
import me.proton.core.drive.eventmanager.api.response.Events
import me.proton.core.drive.eventmanager.api.response.LinksEvent
import me.proton.core.drive.eventmanager.api.response.UpdateLinksEvent
import me.proton.core.drive.eventmanager.api.response.UpdateMetadataLinksEvent
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.eventmanager.usecase.OnCreateEvent
import me.proton.core.drive.eventmanager.usecase.OnDeleteEvent
import me.proton.core.drive.eventmanager.usecase.OnResetAllEvent
import me.proton.core.drive.eventmanager.usecase.OnUpdateContentEvent
import me.proton.core.drive.eventmanager.usecase.OnUpdateMetadataEvent
import me.proton.core.drive.link.data.api.entity.LinkActiveRevisionDto
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.entity.LinkFilePropertiesDto
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.usecase.FindLinkIds
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.EventMetadata
import me.proton.core.eventmanager.domain.entity.EventsResponse
import me.proton.core.eventmanager.domain.entity.RefreshType
import org.junit.Before
import org.junit.Test

class LinkEventListenerTest {
    private val onCreateEvent = mockk<OnCreateEvent>(relaxed = true)
    private val onUpdateContentEvent = mockk<OnUpdateContentEvent>(relaxed = true)
    private val onUpdateMetadataEvent = mockk<OnUpdateMetadataEvent>(relaxed = true)
    private val onDeleteEvent = mockk<OnDeleteEvent>(relaxed = true)
    private val onResetAllEvent = mockk<OnResetAllEvent>(relaxed = true)
    private val findLinkIds = mockk<FindLinkIds>(relaxed = true)
    private val getShare = mockk<GetShare>(relaxed = true)

    private val listener = LinkEventListener(
        onCreateEvent = onCreateEvent,
        onUpdateContentEvent = onUpdateContentEvent,
        onUpdateMetadataEvent = onUpdateMetadataEvent,
        onDeleteEvent = onDeleteEvent,
        onResetAllEvent = onResetAllEvent,
        findLinkIds = findLinkIds,
        getShare = getShare,
    ).apply {
        onFailure = { error, _ -> throw error }
    }

    private val userId = UserId("user-id")
    private val shareId = ShareId(userId, "share-id")
    private val volumeId = VolumeId("volume-id")
    private val shareConfig = EventManagerConfig.Drive.Share(userId, shareId.id)
    private val volumeConfig = EventManagerConfig.Drive.Volume(userId, volumeId.id)

    @Before
    fun setUp() {
        coEvery {
            getShare(shareId, any())
        } returns flowOf(
            DataResult.Success(
                ResponseSource.Local, Share(
                    id = shareId,
                    volumeId = volumeId,
                    rootLinkId = "main-root-id",
                    addressId = null,
                    isMain = true,
                    isLocked = false,
                    key = "",
                    passphrase = "",
                    passphraseSignature = "",
                    creationTime = null,
                    type = Share.Type.MAIN
                )
            )
        )
    }

    @Test
    fun onCreateFromShare() = runTest {
        val linkDto = linkDto()
        listener.notifyEvents(
            config = shareConfig,
            metadata = shareConfig.eventMetadata,
            response = eventsResponse(
                CreateLinksEvent(
                    linkDto, shareId.id, null
                ),
            ),
        )

        coVerify {
            onCreateEvent(
                listOf(
                    LinkEventVO(
                        volumeId = volumeId,
                        link = linkDto.toLinkWithProperties(shareId).toLink(),
                        deletedShareUrlIds = emptyList(),
                    )
                )
            )
        }
    }

    @Test
    fun onUpdateFromShare() = runTest {
        val linkDto = linkDto()
        listener.notifyEvents(
            config = shareConfig,
            metadata = shareConfig.eventMetadata,
            response = eventsResponse(
                UpdateLinksEvent(
                    linkDto, shareId.id, null
                ),
            ),
        )

        coVerify {
            onUpdateContentEvent(
                listOf(
                    LinkEventVO(
                        volumeId = volumeId,
                        link = linkDto.toLinkWithProperties(shareId).toLink(),
                        deletedShareUrlIds = emptyList(),
                    )
                )
            )
        }
    }

    @Test
    fun onPartialFromShare() = runTest {
        val linkDto = linkDto()
        listener.notifyEvents(
            config = shareConfig,
            metadata = shareConfig.eventMetadata,
            response = eventsResponse(
                UpdateMetadataLinksEvent(
                    linkDto, shareId.id, null
                ),
            ),
        )

        coVerify {
            onUpdateMetadataEvent(
                listOf(
                    LinkEventVO(
                        volumeId = volumeId,
                        link = linkDto.toLinkWithProperties(shareId).toLink(),
                        deletedShareUrlIds = emptyList(),
                    )
                )
            )
        }
    }

    @Test
    fun onDeleteFromShare() = runTest {
        listener.notifyEvents(
            config = shareConfig,
            metadata = shareConfig.eventMetadata,
            response = eventsResponse(
                DeleteLinksEvent(DeleteLinksEvent.Link("link-id"), null),
            ),
        )

        coVerify { onDeleteEvent(listOf(FileId(shareId, "link-id"))) }
    }

    @Test
    fun onResetAllFromShare() = runTest {
        listener.notifyResetAll(
            config = shareConfig,
            metadata = shareConfig.eventMetadata,
            response = null,
        )

        coVerify { onResetAllEvent(shareId) }
    }

    @Test
    fun onCreateFromVolume() = runTest {
        val linkDto = linkDto()
        listener.notifyEvents(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = eventsResponse(
                CreateLinksEvent(
                    linkDto, shareId.id, null
                ),
            ),
        )

        coVerify {
            onCreateEvent(
                listOf(
                    LinkEventVO(
                        volumeId = volumeId,
                        link = linkDto.toLinkWithProperties(shareId).toLink(),
                        deletedShareUrlIds = emptyList(),
                    )
                )
            )
        }
    }

    @Test
    fun onUpdateFromVolume() = runTest {
        val linkDto = linkDto()
        listener.notifyEvents(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = eventsResponse(
                UpdateLinksEvent(
                    linkDto, shareId.id, null
                ),
            ),
        )

        coVerify {
            onUpdateContentEvent(
                listOf(
                    LinkEventVO(
                        volumeId = volumeId,
                        link = linkDto.toLinkWithProperties(shareId).toLink(),
                        deletedShareUrlIds = emptyList(),
                    )
                )
            )
        }
    }

    @Test
    fun onPartialFromVolume() = runTest {
        val linkDto = linkDto()
        listener.notifyEvents(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = eventsResponse(
                UpdateMetadataLinksEvent(
                    linkDto, shareId.id, null
                ),
            ),
        )

        coVerify {
            onUpdateMetadataEvent(
                listOf(
                    LinkEventVO(
                        volumeId = volumeId,
                        link = linkDto.toLinkWithProperties(shareId).toLink(),
                        deletedShareUrlIds = emptyList(),
                    )
                )
            )
        }
    }

    @Test
    fun onDeleteFromVolumeLinkIdFound() = runTest {
        coEvery {
            findLinkIds(userId, volumeId, "link-id")
        } returns Result.success(listOf(FileId(shareId, "link-id")))


        listener.notifyEvents(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = eventsResponse(
                DeleteLinksEvent(DeleteLinksEvent.Link("link-id"), null),
            ),
        )

        coVerify { onDeleteEvent(listOf(FileId(shareId, "link-id"))) }
    }

    @Test
    fun onDeleteFromVolumeLinkIdNotFound() = runTest {
        coEvery {
            findLinkIds(userId, volumeId, "link-id")
        } returns Result.success(emptyList())


        listener.notifyEvents(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = eventsResponse(
                DeleteLinksEvent(DeleteLinksEvent.Link("link-id"), null),
            ),
        )

        coVerify(exactly = 0) { onDeleteEvent(any()) }
    }

    @Test
    fun onDeleteFromVolumeWithContextShareId() = runTest {

        listener.notifyEvents(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = eventsResponse(
                DeleteLinksEvent(DeleteLinksEvent.Link("link-id"), shareId.id),
            ),
        )

        coVerify { onDeleteEvent(any()) }
    }

    @Test
    fun onResetAllFromVolume() = runTest {
        listener.notifyResetAll(
            config = volumeConfig,
            metadata = volumeConfig.eventMetadata,
            response = null,
        )

        coVerify { onResetAllEvent(userId, volumeId) }
    }

    private fun eventsResponse(vararg events: LinksEvent) =
        EventsResponse(
            LinkEventListener.json.encodeToString(Events(listOf(*events)))
        )

    private val EventManagerConfig.Drive.Share.eventMetadata get() =
        EventMetadata(
            userId = userId,
            eventId = null,
            config = this,
            createdAt = 0,
            refresh = RefreshType.Mail
        )

    private val EventManagerConfig.Drive.Volume.eventMetadata get() =
        EventMetadata(
            userId = userId,
            eventId = null,
            config = this,
            createdAt = 0,
            refresh = RefreshType.Mail
        )

    private fun linkDto() = LinkDto(
        id = "link-id",
        parentId = "parent-id",
        type = 2,
        name = "name",
        nameSignatureEmail = null,
        hash = "hash",
        state = 1,
        expirationTime = null,
        size = 0,
        mimeType = "",
        attributes = 0,
        permissions = 0,
        nodeKey = "",
        nodePassphrase = "",
        nodePassphraseSignature = "",
        signatureAddress = "",
        creationTime = 0,
        lastModificationTime = 0,
        trashed = null,
        shared = 0,
        numberOfUrlsAttached = 0,
        numberOfActiveUrls = 0,
        allUrlsHaveExpired = 0,
        fileProperties = LinkFilePropertiesDto(
            contentKeyPacket = "",
            contentKeyPacketSignature = null,
            activeRevision = LinkActiveRevisionDto(
                id = "revision-id",
                creationTime = 0,
                size = 0,
                manifestSignature = "",
                signatureAddress = null,
                state = 1,
                thumbnail = 0,
                photo = null,
                thumbnails = emptyList()
            ),
        ),
        folderProperties = null,
        xAttr = null,
        sharingDetails = null,
    )
}
