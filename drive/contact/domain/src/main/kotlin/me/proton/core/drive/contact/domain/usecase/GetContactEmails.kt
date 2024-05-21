/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.contact.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetContactEmails @Inject constructor(
    private val contactRepository: ContactRepository,
) {
    operator fun invoke(userId: UserId): Flow<DataResult<List<ContactEmail>>> =
        contactRepository.observeAllContactEmails(userId)
}