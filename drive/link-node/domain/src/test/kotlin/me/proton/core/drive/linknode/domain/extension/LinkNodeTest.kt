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

package me.proton.core.drive.linknode.domain.extension

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.linknode.domain.entity.LinkNode
import me.proton.core.drive.share.domain.entity.ShareId
import org.junit.Test

class LinkNodeTest {

    private val shareId = ShareId(UserId("USER_ID"),"MainShare")
    private val links = (1..10).map { index ->
        mockk<Link>().apply {
            every { this@apply.id } returns FileId(shareId, index.toString())
        }
    }
    private val linkNodeMap = mutableMapOf<Link, LinkNode>().also { linkNodeMap ->
        links.fold(null as? LinkNode?) { parent, link ->
            LinkNode(parent, null, link).also { child ->
                linkNodeMap[link] = child
                parent?.child = child
            }
        }
    }

    @Test
    fun `leaf is the last item`() {
        // region Given
        val node = linkNodeMap[links[1]]!!
        // endregion
        // region When
        val leaf = node.leaf.link
        // endregion
        // region Then
        assertEquals(links.last(), leaf)
        // endregion
    }

    @Test
    fun `withAncestorsFromRoot loop from root to current element in the middle`() {
        // region Given
        val node = linkNodeMap[links[4]]!!
        val loopedItems = mutableListOf<Link>()
        // endregion
        // region When
        node.withAncestorsFromRoot { link -> loopedItems.add(link) }
        // endregion
        // region Then
        assertEquals(links.subList(0, 5), loopedItems)
        // endregion
    }

    @Test
    fun `withAncestorsFromRoot loop from root to root`() {
        // region Given
        val node = linkNodeMap[links.first()]!!
        val loopedItems = mutableListOf<Link>()
        // endregion
        // region When
        node.withAncestorsFromRoot { link -> loopedItems.add(link) }
        // endregion
        // region Then
        assertEquals(listOf(links.first()), loopedItems)
        // endregion
    }

    @Test
    fun `withAncestorsFromRoot loop from root to leaf`() {
        // region Given
        val node = linkNodeMap[links.last()]!!
        val loopedItems = mutableListOf<Link>()
        // endregion
        // region When
        node.withAncestorsFromRoot { link -> loopedItems.add(link) }
        // endregion
        // region Then
        assertEquals(links, loopedItems)
        // endregion
    }

    @Test
    fun `withAncestors loop through all ancestors`() {
        // region Given
        val node = linkNodeMap[links[4]]!!
        val loopedItems = mutableListOf<Link>()
        // endregion
        // region When
        node.withAncestors { link -> loopedItems.add(link) }
        // endregion
        // region Then
        assertEquals(links.subList(0, 4).reversed(), loopedItems)
        // endregion
    }

    @Test
    fun `withDescendants loop through all descendants`() {
        // region Given
        val node = linkNodeMap[links[4]]!!
        val loopedItems = mutableListOf<Link>()
        // endregion
        // region When
        node.withDescendants { link -> loopedItems.add(link) }
        // endregion
        // region Then
        assertEquals(links.subList(5, 10), loopedItems)
        // endregion
    }
}
