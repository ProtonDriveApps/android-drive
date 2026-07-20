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

package me.proton.core.drive.link.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlreadyExistsErrorResponseDetails(
    @SerialName("ConflictLinkID")
    val conflictLinkId: String? = null,
    @SerialName("ConflictRevisionID")
    val conflictRevisionId: String? = null,
    @SerialName("ConflictDraftRevisionID")
    val conflictDraftRevisionId: String? = null,
    @SerialName("ConflictDraftClientUID")
    val conflictDraftClientUid: String? = null,
    @SerialName("RevisionID")
    val revisionId: String? = null,
)
