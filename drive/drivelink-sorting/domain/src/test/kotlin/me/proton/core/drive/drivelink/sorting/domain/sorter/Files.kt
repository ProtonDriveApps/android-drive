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

package me.proton.core.drive.drivelink.sorting.domain.sorter

import io.mockk.every
import io.mockk.mockk
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.drivelink.domain.entity.DriveLink

fun file(name: String, type: String = "", lastModified: Long = 0L, size: Long = 0L) =
    mockk<DriveLink.File>()
        .apply(name, type, lastModified, size)

fun cryptedFile(name: String, type: String, lastModified: Long, size: Long) =
    mockk<DriveLink.File>()
        .apply(name, type, lastModified, size)
        .apply { every { cryptoName } returns CryptoProperty.Encrypted(name) }

fun folder(name: String, lastModified: Long, size: Long) = mockk<DriveLink.Folder>()
    .apply(name, "Folder", lastModified, size)

fun cryptedFolder(name: String, lastModified: Long, size: Long) =
    mockk<DriveLink.Folder>()
        .apply(name, "Folder", lastModified, size)
        .apply { every { cryptoName } returns CryptoProperty.Encrypted(name) }

fun <T : DriveLink> T.apply(
    name: String,
    type: String,
    lastModifiedS: Long,
    sizeB: Long
) = apply {
    every { cryptoName } returns CryptoProperty.Decrypted(name, VerificationStatus.Success)
    every { this@apply.name } returns name
    every { mimeType } returns type
    every { lastModified } returns TimestampS(lastModifiedS)
    every { size } returns Bytes(sizeB)
}