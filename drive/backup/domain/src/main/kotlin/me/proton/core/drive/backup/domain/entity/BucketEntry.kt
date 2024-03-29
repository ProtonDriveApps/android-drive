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

package me.proton.core.drive.backup.domain.entity

data class BucketEntry(
    val bucketId: Int,
    val bucketName: String?,
    val lastItemUriString: String? = null,
    var imageCount: Int = 0,
    var videoCount: Int = 0,
) {
    override fun equals(other: Any?): Boolean =
        other is BucketEntry && bucketId == other.bucketId

    override fun hashCode(): Int = bucketId
}
