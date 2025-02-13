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

package me.proton.core.drive.link.domain.usecase

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.share.domain.entity.ShareId
import org.junit.Test

class SortLinksByParentsTest {

    private val sortLinksByParents = SortLinksByParents()
    private val userId = UserId("USER_ID")

    @Test
    fun `already sorted`() {
        // region Arrange
        val list = listOf(
            link("10"),
            link("2", "10"),
            link("3", "2"),
            link("4", "2"),
        )
        // endregion
        // region Act
        val sorted = sortLinksByParents(list)
        // endregion
        // region Assert
        assertEquals(list, sorted)
        // endregion
    }

    @Test
    fun `already sorted but older has parent`() {
        // region Arrange
        val list = listOf(
            link("10", "parent"),
            link("2", "10"),
            link("3", "2"),
            link("4", "2"),
        )
        // endregion
        // region Act
        val sorted = sortLinksByParents(list, mapOf(ShareId(userId, "SHARE_1") to "parent"))
        // endregion
        // region Assert
        assertEquals(list, sorted)
        // endregion
    }

    @Test
    fun `simple list not sorted`() {
        // region Arrange
        val list = listOf(
            link("3", "2"),
            link("2", "10"),
            link("4", "2"),
            link("10", "parent"),
        )
        // endregion
        // region Act
        val sorted = sortLinksByParents(list, mapOf(ShareId(userId, "SHARE_1") to "parent"))
        // endregion
        // region Assert
        assertEquals(listOf(list[3], list[1], list[0], list[2]), sorted)
        // endregion
    }


    @Test
    fun `two shares list not sorted`() {
        // region Arrange
        val list = listOf(
            link("3", "2"), // 3
            link("3", "2", "SHARE_2"), // 7
            link("2", "10"), // 2
            link("2", "10", "SHARE_2"), // 6
            link("4", "2"), // 4
            link("4", "2", "SHARE_2"), // 8
            link("10", "parent", "SHARE_2"), // 5
            link("10", null), // 1
        )
        // endregion
        // region Act
        val sorted = sortLinksByParents(list, mapOf(ShareId(userId, "SHARE_2") to "parent"))
        // endregion
        // region Assert
        assertEquals(listOf(list[7], list[2], list[0], list[4], list[6], list[3], list[1], list[5]), sorted)
        // endregion
    }

    @Test
    fun `throw exception is list don't match (wrong parent)`() {
        // region Arrange
        val list = listOf(
            link("3", "2"),
            link("2", "10"),
            link("4", "2"),
            link("10", "parent"),
        )
        // endregion
        // region Act
        val exception = try {
            sortLinksByParents(list, mapOf(ShareId(userId, "SHARE_1") to "parent2"))
            null
        } catch (e: IllegalStateException) {
            e
        }
        // endregion
        // region Assert
        assertNotNull(exception)
        assertEquals("Lists' sizes don't match: 4 != 0", exception!!.message)
        // endregion
    }

    @Test
    fun `throw exception is list don't match (chain broken)`() {
        // region Arrange
        val list = listOf(
            link("3", "2"),
            link("2", "10"),
            link("4", "5"),
            link("10", "parent"),
        )
        // endregion
        // region Act
        val exception = try {
            sortLinksByParents(list, mapOf(ShareId(userId, "SHARE_1") to "parent"))
            null
        } catch (e: IllegalStateException) {
            e
        }
        // endregion
        // region Assert
        assertNotNull(exception)
        assertEquals("Lists' sizes don't match: 4 != 3", exception!!.message)
        // endregion
    }

    @Test
    fun `original bug`() {
        // region Arrange
        val list = listOf(
            link("l8vWAXHB", null),
            link("q6fRrEIn", "l8vWAXHB"),
            link("0WjWEbOm", "l8vWAXHB"),
            link("js51I0ca", "l8vWAXHB"),
            link("FMbrHWqH", "l8vWAXHB"),
            link("5NjNMvvA", "l8vWAXHB"),
            link("qZ40omCA", "l8vWAXHB"),
            link("p-Sol9x2", "be8otiam"),
            link("dVUkJlDc", "l8vWAXHB"),
            link("WPgkJK7x", "l8vWAXHB"),
            link("tf5lVD1b", "WPgkJK7x"),
            link("YwlxB1Vj", "-p2A7Xyc"),
            link("be8otiam", "l8vWAXHB"),
            link("-p2A7Xyc", "ABjkEfZ8"),
            link("ABjkEfZ8", "MoJNL9-D"),
            link("MoJNL9-D", "l8vWAXHB"),
        )
        // endregion
        // region Act
        val sorted = sortLinksByParents(list)
        // endregion
        // region Assert
        assertEquals(listOf(
            list[0],
            list[1],
            list[2],
            list[3],
            list[4],
            list[5],
            list[6],
            list[8],
            list[9],
            list[12],
            list[15],
            list[10],
            list[7],
            list[14],
            list[13],
            list[11],
        ), sorted)
        // endregion
    }

    private fun link(linkId: String, parent: String? = null, shareId: String = "SHARE_1") =
        mockk<Link.File>(name = shareId + "_" + linkId).apply {
            every { id } returns FileId(ShareId(userId, shareId), linkId)
            every { parentId } returns parent?.let { FolderId(ShareId(userId, shareId), parent) }
        }
}
