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

package me.proton.core.drive.share.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.PASSPHRASE_NODE_KEY_PACKET
import me.proton.core.drive.base.data.api.Dto.PASSPHRASE_NODE_KEY_PACKETS
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.UNREADABLE_SHARE_IDS


@Serializable
data class ShareAccessWithNodeRequest(
    @SerialName(PASSPHRASE_NODE_KEY_PACKETS)
    val passphraseNodeKeyPackets: List<PassphraseNodeKeyPacket>,
    @SerialName(UNREADABLE_SHARE_IDS)
    val unreadableShareIds: List<String>,
)

@Serializable
data class PassphraseNodeKeyPacket(
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(PASSPHRASE_NODE_KEY_PACKET)
    val passphraseNodeKeyPacket: String,
)
