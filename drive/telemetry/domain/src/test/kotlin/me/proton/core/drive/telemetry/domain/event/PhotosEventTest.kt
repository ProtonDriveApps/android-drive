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

package me.proton.core.drive.telemetry.domain.event

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotosEventTest {
    private val events = listOf(
        PhotosEvent.SettingDisabled(),
        PhotosEvent.SettingEnabled(),
        PhotosEvent.UploadDone(0, 0, PhotosEvent.Reason.COMPLETED),
        PhotosEvent.BackupStopped(0, 0, 0, PhotosEvent.Reason.COMPLETED, true),
    )

    @Test
    fun sameGroup() = events.forEach { event ->
        assertEquals(PhotosEvent.group, event.group)
    }

    @Test
    fun uniqueName() = events.map { it.name }.let { names ->
        assertEquals(names, names.distinct())
    }
}
