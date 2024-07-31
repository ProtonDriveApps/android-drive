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

package me.proton.core.drive.label.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.transformSuccess
import me.proton.core.drive.contact.domain.usecase.GetContactEmails
import me.proton.core.drive.label.domain.entity.LabelWithContacts
import me.proton.core.label.domain.entity.Label
import me.proton.core.util.kotlin.containsNoCase
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
class SearchLabelsWithContacts @Inject constructor(
    private val getLabels: GetLabels,
    private val getContactEmails: GetContactEmails,
) {

    operator fun invoke(userId: UserId, query: String): Flow<DataResult<List<LabelWithContacts>>> =
        getLabels(userId).distinctUntilChanged().transformSuccess { (_, labels) ->
            emitAll(
                getContactEmails(userId).distinctUntilChanged().transformSuccess { (_, contacts) ->
                    query.trim().takeIfNotBlank()?.run {
                        emit(search(labels, contacts, query).asSuccess)
                    }
                })
        }.distinctUntilChanged()

    private fun search(
        labels: List<Label>,
        contacts: List<ContactEmail>,
        query: String,
    ): List<LabelWithContacts> = labels.mapNotNull { label ->
        if (label.name.containsNoCase(query)) {
            val contactEmails = contacts.filter { contact -> label.labelId.id in contact.labelIds }
            if (contactEmails.isEmpty()) {
                null
            } else {
                LabelWithContacts(label, contactEmails)
            }
        } else {
            null
        }
    }
}
