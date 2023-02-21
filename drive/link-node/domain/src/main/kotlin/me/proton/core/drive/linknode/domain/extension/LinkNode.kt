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

import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.linknode.domain.entity.LinkNode

val LinkNode.leaf: LinkNode
    get() = descendants.last()

inline fun LinkNode.withAncestorsFromRoot(onAncestor: (Link) -> Unit) =
    (listOf(root) + root.descendants).takeWhile { node -> node.parent?.link?.id != link.id }.forEach { linkNode ->
        onAncestor(linkNode.link)
    }

inline fun LinkNode.withDescendants(onDescendant: (Link) -> Unit) = descendants.forEach { linkNode ->
    onDescendant(linkNode.link)
}

inline fun LinkNode.withAncestors(onAncestor: (Link) -> Unit) = ancestors.forEach { linkNode ->
    onAncestor(linkNode.link)
}

val LinkNode.descendants: Sequence<LinkNode>
    get() = sequenceOf { child }

val LinkNode.ancestors: Sequence<LinkNode>
    get() = sequenceOf { parent }

private inline fun LinkNode.sequenceOf(crossinline nextNode: LinkNode.() -> LinkNode?) = sequence {
    var current = nextNode()
    while (current != null) {
        yield(current)
        current = current.nextNode()
    }
}
