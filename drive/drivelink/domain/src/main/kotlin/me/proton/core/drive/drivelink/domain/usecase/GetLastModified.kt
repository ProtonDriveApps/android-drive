/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.domain.usecase

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.file.base.domain.extension.toXAttr
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class GetLastModified @Inject constructor(
    private val dateTimeFormatter: DateTimeFormatter,
) {
    operator fun invoke(driveLink: DriveLink): TimestampS {
        if (driveLink.cryptoXAttr !is CryptoProperty.Decrypted) return driveLink.lastModified
        val xAttrString = driveLink.cryptoXAttr.value ?: return driveLink.lastModified
        xAttrString.toXAttr().onSuccess { xAttr ->
            return dateTimeFormatter.parseFromIso8601String(iso8601 = xAttr.common.modificationTime)
                .getOrNull(
                    LogTag.ENCRYPTION,
                    "There was an error parsing modification time of drive link: ${driveLink.id.id.logId()}"
                )
                ?: driveLink.lastModified
        }.onFailure { error ->
            CoreLogger.w(
                LogTag.ENCRYPTION,
                error,
                "There was an error parsing xAttr of drive link: ${driveLink.id.id.logId()}"
            )
        }
        return driveLink.lastModified


    }
}
