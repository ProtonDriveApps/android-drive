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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.link.domain.usecase.HasLink
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class OnCreateEvent @Inject constructor(
    private val hasLink: HasLink,
    private val handleEventLinks: HandleCreateOrUpdateLinksEvent,
) {

    suspend operator fun invoke(vos: List<LinkEventVO>) {
        CoreLogger.d(LogTag.EVENTS, "onCreateEvent: ${vos.joinToString { vo -> vo.link.id.id.logId() }}")
        handleEventLinks(
            vos = vos.filter { vo ->
                vo.link.parentId?.let { parentId -> hasLink(parentId).first() } == true
            },
        )
    }
}
