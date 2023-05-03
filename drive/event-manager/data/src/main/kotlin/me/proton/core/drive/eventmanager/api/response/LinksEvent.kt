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

package me.proton.core.drive.eventmanager.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.proton.core.drive.base.data.api.Dto.CONTEXT_SHARE_ID
import me.proton.core.drive.base.data.api.Dto.DATA
import me.proton.core.drive.base.data.api.Dto.DELETED_URL_ID
import me.proton.core.drive.base.data.api.Dto.LINK
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.eventmanager.domain.entity.Action

@Serializable
sealed class LinksEvent

@Serializable
@SerialName("0")
data class DeleteLinksEvent(
    @SerialName(LINK)
    val link: Link,
    @SerialName(CONTEXT_SHARE_ID)
    val contextShareId: String? = null,
) : LinksEvent() {

    @Serializable
    data class Link(
        @SerialName(LINK_ID)
        val id: String,
    )
}

interface WithLinkDto {
    val link: LinkDto
    val data: Data?
    val action: Action
}

@Serializable
data class Data(
    @SerialName(DELETED_URL_ID)
    val deletedUrlId: List<String> = emptyList(),
)

@Serializable
@SerialName("1")
data class CreateLinksEvent(
    @SerialName(LINK)
    override val link: LinkDto,
    @SerialName(CONTEXT_SHARE_ID)
    val contextShareId: String,
    @SerialName(DATA)
    override val data: Data? = null,
) : LinksEvent(), WithLinkDto {
    @Transient
    override val action: Action = Action.Create
}

@Serializable
@SerialName("2")
data class UpdateLinksEvent(
    @SerialName(LINK)
    override val link: LinkDto,
    @SerialName(CONTEXT_SHARE_ID)
    val contextShareId: String,
    @SerialName(DATA)
    override val data: Data? = null,
) : LinksEvent(), WithLinkDto {
    @Transient
    override val action: Action = Action.Update
}


@Serializable
@SerialName("3")
data class UpdateMetadataLinksEvent(
    @SerialName(LINK)
    override val link: LinkDto,
    @SerialName(CONTEXT_SHARE_ID)
    val contextShareId: String,
    @SerialName(DATA)
    override val data: Data? = null,
) : LinksEvent(), WithLinkDto {
    @Transient
    override val action: Action = Action.Partial
}

@Serializable
object UnknownLinksEvent : LinksEvent()
