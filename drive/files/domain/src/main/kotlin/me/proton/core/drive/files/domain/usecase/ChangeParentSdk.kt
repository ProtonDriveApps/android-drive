/*
 * Copyright (c) 2026 Proton AG.
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
package me.proton.core.drive.files.domain.usecase

import kotlinx.coroutines.flow.toList
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.entity.NodeMoveItem
import me.proton.drive.sdk.entity.NodeResultPair
import me.proton.drive.sdk.entity.NodeUid
import javax.inject.Inject

class ChangeParentSdk @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(
        userId: UserId,
        nodeUids: List<NodeUid>,
        newParentFolderUid: NodeUid,
    ): Result<List<NodeResultPair>> = coRunCatching {
        updateEventAction(
            userId = userId,
            nodeUid = newParentFolderUid,
        ) {
            val client = protonDriveClientProvider
                .getOrCreate(userId)
                .getOrThrow()
            val nodeMoveItems = mutableListOf<NodeMoveItem>()
            val failures = mutableListOf<NodeResultPair>()
            nodeUids.forEach { nodeUid ->
                coRunCatching {
                    // TODO replace by getNodes
                    val node = checkNotNull(client.getNode(nodeUid)) { "Node not found for move" }
                    val parentUid =
                        checkNotNull(node.parentUid) { "Node without parent cannot be moved" }
                    val name = node.name.getOrThrow()
                    NodeMoveItem(
                        nodeUid = nodeUid,
                        currentParentUid = parentUid,
                        currentName = name,
                        targetName = name,
                    )
                }.fold(
                    onSuccess = { nodeMoveItems += it },
                    onFailure = { error ->
                        failures += NodeResultPair.Failure(
                            nodeUid = nodeUid,
                            error = error as? ProtonDriveSdkException
                                ?: ProtonDriveSdkException(message = error.message, cause = error),
                        )
                    },
                )
            }
            failures + if (nodeMoveItems.isEmpty()) {
                emptyList()
            } else {
                client.moveNodes(
                    nodeMoveItems,
                    targetParentFolderUid = newParentFolderUid
                ).toList()
            }
        }
    }
}
