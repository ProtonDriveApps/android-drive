/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.link.domain.extension

import junit.framework.TestCase.assertEquals
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkIdKtTest {
    @Test
    fun compareAlbumsIds() {
        val albumId1 = AlbumId(shareId = photoShareId, id = "album-id")
        val albumId2 = AlbumId(shareId = photoShareId, id = "album-id")

        assertEquals(albumId1, albumId2)
        assertTrue(albumId1.equalsAsLinkId(albumId2))
    }
    @Test
    fun compareAlbumsIdAndFolderId() {
        val albumId = AlbumId(shareId = photoShareId, id = "album-id")
        val folderId = FolderId(shareId = photoShareId, id = "album-id")

        assertNotEquals(albumId, folderId)
        assertTrue(albumId.equalsAsLinkId(folderId))
    }
}
