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

package me.proton.core.drive.db.test

import me.proton.core.contact.data.local.db.entity.ContactEmailEntity
import me.proton.core.contact.data.local.db.entity.ContactEntity
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId

suspend fun UserContext.contactEmail(
    name: String,
    isProton: Boolean = true,
    email: String = name.toEmail(isProton),
) {
    contactEmail(
        NullableContactEmailEntity(
            name = name,
            isProton = isProton,
            email = email,
        )
    )
}

suspend fun UserContext.contactEmail(
    contactEmailEntity: ContactEmailEntity,
) {
    db.contactDao().insertOrUpdate(
        NullableContactEntity(
            contactId = contactEmailEntity.contactId,
            name = contactEmailEntity.name
        )
    )
    db.contactEmailDao().insertOrUpdate(contactEmailEntity)
}

@Suppress("FunctionName")
fun UserContext.NullableContactEmailEntity(
    name: String,
    isProton: Boolean = true,
    email: String = name.toEmail(isProton),
) =
    ContactEmailEntity(
        userId = user.userId,
        contactEmailId = ContactEmailId("contact-email-id-$name"),
        name = name,
        email = email,
        defaults = 0,
        order = 0,
        contactId = ContactId("contact-id-$name"),
        canonicalEmail = null,
        isProton = null
    )

@Suppress("FunctionName")
fun UserContext.NullableContactEntity(
    name: String,
    contactId: ContactId = ContactId("contact-id-$name")
) = ContactEntity(
    userId = user.userId,
    name = name,
    contactId = contactId,
)

private fun String.toEmail(isProton: Boolean) =
    "$this@${if (isProton) "proton.me" else "external.com"}"
