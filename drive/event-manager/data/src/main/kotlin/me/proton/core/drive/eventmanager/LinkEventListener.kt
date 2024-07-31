/*
 * Copyright (c) 2021-2023 Proton AG.
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

import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import me.proton.core.drive.base.data.api.Dto
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.eventmanager.api.response.CreateLinksEvent
import me.proton.core.drive.eventmanager.api.response.DeleteLinksEvent
import me.proton.core.drive.eventmanager.api.response.Events
import me.proton.core.drive.eventmanager.api.response.LinksEvent
import me.proton.core.drive.eventmanager.api.response.UnknownLinksEvent
import me.proton.core.drive.eventmanager.api.response.UpdateLinksEvent
import me.proton.core.drive.eventmanager.api.response.UpdateMetadataLinksEvent
import me.proton.core.drive.eventmanager.api.response.WithLinkDto
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.eventmanager.usecase.OnCreateEvent
import me.proton.core.drive.eventmanager.usecase.OnDeleteEvent
import me.proton.core.drive.eventmanager.usecase.OnEventEndpointFetchError
import me.proton.core.drive.eventmanager.usecase.OnResetAllEvent
import me.proton.core.drive.eventmanager.usecase.OnUpdateContentEvent
import me.proton.core.drive.eventmanager.usecase.OnUpdateMetadataEvent
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.FindLinkIds
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.Action
import me.proton.core.eventmanager.domain.entity.Event
import me.proton.core.eventmanager.domain.entity.EventsResponse
import me.proton.core.eventmanager.domain.entity.RefreshType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkEventListener @Inject constructor(
    private val onCreateEvent: OnCreateEvent,
    private val onUpdateContentEvent: OnUpdateContentEvent,
    private val onUpdateMetadataEvent: OnUpdateMetadataEvent,
    private val onDeleteEvent: OnDeleteEvent,
    private val onResetAllEvent: OnResetAllEvent,
    private val findLinkIds: FindLinkIds,
    private val getShare: GetShare,
    private val onEventEndpointFetchError: OnEventEndpointFetchError,
) : EventListener<LinkId, LinkEventVO>() {
    internal var onFailure: (Throwable, String) -> Unit = { error, body ->
        error.log(LogTag.EVENTS, "Cannot parse event from response: $body")
    }
    // User -> Share -> Link
    override val order: Int = 3
    override val type: Type = Type.Drive

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse,
    ): List<Event<LinkId, LinkEventVO>>? {
        return runCatching {
            json.decodeFromString<Events>(response.body).events.flatMap { event ->
                when (event) {
                    is DeleteLinksEvent -> event.getLinkIds(config).map { linkId ->
                        Event<LinkId, LinkEventVO>(
                            action = Action.Delete,
                            key = linkId,
                            entity = null,
                        )
                    }

                    is CreateLinksEvent ->  listOf(
                        event.getEvent(
                            config.getVolumeId(),
                            ShareId(config.userId, event.contextShareId),
                        )
                    )

                    is UpdateLinksEvent ->  listOf(
                        event.getEvent(
                            config.getVolumeId(),
                            ShareId(config.userId, event.contextShareId),
                        )
                    )

                    is UpdateMetadataLinksEvent ->  listOf(
                        event.getEvent(
                            config.getVolumeId(),
                            ShareId(config.userId, event.contextShareId),
                        )
                    )

                    is UnknownLinksEvent ->  listOf()
                }
            }
        }.onFailure { error -> onFailure(error, response.body) }.getOrNull()
    }

    private fun WithLinkDto.getEvent(volumeId: VolumeId, shareId: ShareId): Event<LinkId, LinkEventVO> {
        val vo = LinkEventVO(
            volumeId = volumeId,
            link = link.toLinkWithProperties(shareId).toLink(),
            deletedShareUrlIds = data?.deletedUrlId ?: emptyList(),
        )
        return Event(action, vo.link.id, vo)
    }

    // We need to do things (like deleting local content or filter entities) which could result in a deadlock or block
    // the database for too long so we prefer to leave the transaction responsibility to the use-cases
    override suspend fun <R> inTransaction(block: suspend () -> R): R = block()

    override suspend fun onCreate(config: EventManagerConfig, entities: List<LinkEventVO>) =
        onCreateEvent(entities)

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<LinkEventVO>) =
        onUpdateContentEvent(entities)

    // Partial is "Update Metadata" in Drive
    override suspend fun onPartial(config: EventManagerConfig, entities: List<LinkEventVO>) =
        onUpdateMetadataEvent(entities)

    override suspend fun onDelete(config: EventManagerConfig, keys: List<LinkId>) =
        onDeleteEvent(keys)

    override suspend fun onResetAll(config: EventManagerConfig) {
        if (getEventMetadata(config).refresh == RefreshType.Mail) {
            // Drive BE sent refresh: 1 i.e. clients need to refresh all data
            when (config) {
                is EventManagerConfig.Drive.Share -> onResetAllEvent(ShareId(config.userId, config.shareId))
                is EventManagerConfig.Drive.Volume -> onResetAllEvent(config.userId, VolumeId(config.volumeId))
                else -> error("Unexpected event manager config")
            }
        }
    }

    override suspend fun onFetchError(config: EventManagerConfig, error: Throwable) {
        (config as? EventManagerConfig.Drive.Volume)
            ?.let { driveVolumeConfig ->
                onEventEndpointFetchError(driveVolumeConfig.userId, VolumeId(driveVolumeConfig.volumeId), error)
                    .getOrNull(LogTag.EVENTS)
            }
    }

    private suspend fun DeleteLinksEvent.getLinkIds(
        config: EventManagerConfig,
    ): List<LinkId> = when (config) {
        is EventManagerConfig.Drive.Share -> listOf(
            FileId(ShareId(config.userId, config.shareId), link.id),
        )

        is EventManagerConfig.Drive.Volume -> contextShareId?.let { shareId ->
            listOf(FileId(ShareId(config.userId, shareId), link.id))
        } ?: findLinkIds(
            userId = config.userId,
            volumeId = VolumeId(config.volumeId),
            linkId = link.id
        ).getOrThrow()

        else -> error("Unexpected event manager config")
    }

    private suspend fun EventManagerConfig.getVolumeId(): VolumeId = when (this) {
        is EventManagerConfig.Drive.Volume -> VolumeId(volumeId)
        is EventManagerConfig.Drive.Share -> getShare(
            shareId = ShareId(userId, shareId),
            refresh = flowOf(false),
        ).toResult().getOrThrow().volumeId

        else -> error("Unexpected event manager config")
    }

    companion object {
        internal val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            classDiscriminator = Dto.EVENT_TYPE
            serializersModule = SerializersModule {
                polymorphic(LinksEvent::class) {
                    subclass(DeleteLinksEvent::class)
                    subclass(CreateLinksEvent::class)
                    subclass(UpdateLinksEvent::class)
                    subclass(UpdateMetadataLinksEvent::class)
                    defaultDeserializer { UnknownLinksEvent.serializer() }
                }
            }
        }
    }
}
