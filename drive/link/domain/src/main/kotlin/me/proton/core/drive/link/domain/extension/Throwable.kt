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

package me.proton.core.drive.link.domain.extension

import me.proton.core.drive.base.domain.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.domain.extension.findThrowable
import me.proton.core.drive.base.domain.extension.firstErrorOrNull
import me.proton.core.drive.base.domain.extension.onProtonHttpException
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError
import me.proton.drive.sdk.entity.NodeUid

/**
 * True when the Drive SDK reports that [nodeUid] itself, not another node, no longer exists. The
 * volume is compared too, as a link id alone does not identify a node.
 */
fun Throwable.isNodeNotFound(nodeUid: NodeUid): Boolean =
    findThrowable<ProtonDriveSdkException>()
        ?.error
        .firstErrorOrNull { error ->
            (error.additionalData as? ProtonSdkError.Data.NodeNotFound)?.nodeUid?.value == nodeUid.value
        } != null

/** True when the request failed with [NOT_EXISTS] (the API, or the Drive SDK reporting the API code). */
val Throwable.isNotExists: Boolean
    get() = onProtonHttpException { protonData -> protonData.code == NOT_EXISTS } == true
