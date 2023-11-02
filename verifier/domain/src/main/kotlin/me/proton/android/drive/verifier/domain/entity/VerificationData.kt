/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.verifier.domain.entity

data class VerificationData(
    val contentKeyPacket: String,
    val verificationCode: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VerificationData

        if (contentKeyPacket != other.contentKeyPacket) return false
        if (!verificationCode.contentEquals(other.verificationCode)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentKeyPacket.hashCode()
        result = 31 * result + verificationCode.contentHashCode()
        return result
    }
}
