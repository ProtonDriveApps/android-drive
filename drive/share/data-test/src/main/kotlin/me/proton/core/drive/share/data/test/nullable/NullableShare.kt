/*
 * Copyright (c) 2023 Proton AG.
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

@file:Suppress("FunctionName")
/*
 * Copyright (c) 2023 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.share.data.test.nullable

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.user.domain.entity.AddressId

fun NullableShare(
    id: ShareId = ShareId(UserId("user-id"), "share-id"),
    volumeId: VolumeId = VolumeId("volume-id"),
    rootLinkId: String = "",
    addressId: AddressId? = null,
    isMain: Boolean = false,
    isLocked: Boolean = false,
    key: String = "",
    passphrase: String = "",
    passphraseSignature: String = "",
    creationTime: TimestampS? = null
) = Share(
    id = id,
    volumeId = volumeId,
    rootLinkId = rootLinkId,
    addressId = addressId,
    isMain = isMain,
    isLocked = isLocked,
    key = key,
    passphrase = passphrase,
    passphraseSignature = passphraseSignature,
    creationTime = creationTime
)