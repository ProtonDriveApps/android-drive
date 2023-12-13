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
package me.proton.core.drive.share.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ADDRESS_ID
import me.proton.core.drive.base.data.api.Dto.CREATION_TIME
import me.proton.core.drive.base.data.api.Dto.CREATOR
import me.proton.core.drive.base.data.api.Dto.FLAGS
import me.proton.core.drive.base.data.api.Dto.KEY
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.LOCKED
import me.proton.core.drive.base.data.api.Dto.PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.STATE
import me.proton.core.drive.base.data.api.Dto.TYPE
import me.proton.core.drive.base.data.api.Dto.VOLUME_ID

@Serializable
data class ShareDto(
    @SerialName(SHARE_ID)
    val id: String,
    @SerialName(TYPE)
    val type: Long,
    @SerialName(STATE)
    val state: Long,
    @SerialName(LINK_ID)
    val linkId: String,
    @SerialName(VOLUME_ID)
    val volumeId: String,
    @SerialName(CREATOR)
    val creator: String,
    @SerialName(FLAGS)
    val flags: Long,
    @SerialName(LOCKED)
    val locked: Boolean,
    @SerialName(KEY)
    val key: String? = null,
    @SerialName(PASSPHRASE)
    val passphrase: String? = null,
    @SerialName(PASSPHRASE_SIGNATURE)
    val passphraseSignature: String? = null,
    @SerialName(ADDRESS_ID)
    val addressId: String? = null,
    @SerialName(CREATION_TIME)
    val creationTime: Long?,
) {
    val isActive: Boolean get() = state == 1L

    companion object {
        const val TYPE_MAIN = 1L
        const val TYPE_STANDARD = 2L
        const val TYPE_DEVICE = 3L
        const val TYPE_PHOTO = 4L
    }
}
