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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import org.junit.Assert.assertEquals
import org.junit.Test

private val testUserId = UserId("user-id")

class SearchContactsTest {

    private val getContacts = mockk<GetContacts> {
        coEvery { this@mockk.invoke(testUserId) } returns flowOf(DataResult.Success(
            ResponseSource.Local,
            ContactTestData.contacts,
        ))
    }

    private val searchContacts = SearchContacts(getContacts)

    @Test
    fun `when there are multiple matching contacts, they are emitted`() = runTest {
        val query = "cont"

        val contacts = searchContacts(testUserId, query).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(ContactTestData.contacts, contacts)
    }

    @Test
    fun `when there is contact matched only by name, it is emitted with all ContactEmails`() = runTest {
        // Given
        val query = "impo"

        val contact = ContactTestData.buildContactWith(
            userId = testUserId,
            name = "important contact display name", // <-- match
            contactEmails = listOf(
                ContactTestData.buildContactEmailWith(
                    name = "name 1",
                    address = "address1@proton.ch"
                ),
                ContactTestData.buildContactEmailWith(
                    name = "name 2",
                    address = "address2@protonmail.ch"
                )
            )
        )
        coEvery { getContacts(testUserId) } returns flowOf(DataResult.Success(
            ResponseSource.Local,
            ContactTestData.contacts + contact,
        ))

        val contacts = searchContacts(testUserId, query).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(listOf(contact), contacts)
    }

    @Test
    fun `when there is contact matched only by ContactEmail, it is emitted with only matching ContactEmails`() =
        runTest {
            // Given
            val query = "mail"

            val contact = ContactTestData.buildContactWith(
                userId = testUserId,
                name = "important contact display name",
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "name 1",
                        address = "address1@proton.ch"
                    ),
                    ContactTestData.buildContactEmailWith(
                        name = "name 2",
                        address = "address2@protonmail.ch" // <-- match
                    )
                )
            )
            coEvery { getContacts(testUserId) } returns flowOf(DataResult.Success(
                ResponseSource.Local,
                ContactTestData.contacts + contact,
            ))

            val contacts = searchContacts(testUserId, query).filterSuccessOrError().toResult().getOrThrow()

            assertEquals(1, contacts.size)

            val matchedContact = contacts.first()

            assertEquals(contact.userId, matchedContact.userId)
            assertEquals(contact.id, matchedContact.id)
            assertEquals(contact.name, matchedContact.name)
            assertEquals(
                listOf(contact.contactEmails[1]), // return only 2nd ContactEmail
                listOf(matchedContact.contactEmails.first())
            )
        }

    @Test
    fun `when there are contacts matched, they are sorted by last usage`() =
        runTest {
            // Given
            val query = "mail"

            val contact1 = ContactTestData.buildContactWith(
                userId = testUserId,
                name = "important contact display name",
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "name 1",
                        address = "address1@protonmail.ch",
                        lastUsedTime = 4000,
                    ),
                    ContactTestData.buildContactEmailWith(
                        name = "name 2",
                        address = "address2@protonmail.ch",
                        lastUsedTime = 6000,
                    )
                )
            )

            val contact2 = ContactTestData.buildContactWith(
                userId = testUserId,
                name = "important contact display name",
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "name 3",
                        address = "address3@protonmail.ch",
                        lastUsedTime = 10000,
                    ),
                    ContactTestData.buildContactEmailWith(
                        name = "name 4",
                        address = "address4@protonmail.ch",
                        lastUsedTime = 2000,
                    )
                )
            )
            coEvery { getContacts(testUserId) } returns flowOf(DataResult.Success(
                ResponseSource.Local,
                ContactTestData.contacts + contact1 + contact2,
            ))

            val contacts = searchContacts(testUserId, query).filterSuccessOrError().toResult().getOrThrow()

            assertEquals(listOf(
                ContactTestData.buildContactWith(
                    userId = testUserId,
                    name = "important contact display name",
                    contactEmails = listOf(
                        ContactTestData.buildContactEmailWith(
                            name = "name 3",
                            address = "address3@protonmail.ch",
                            lastUsedTime = 10000,
                        ),
                        ContactTestData.buildContactEmailWith(
                            name = "name 4",
                            address = "address4@protonmail.ch",
                            lastUsedTime = 2000,
                        ),
                    )
                ),
                ContactTestData.buildContactWith(
                    userId = testUserId,
                    name = "important contact display name",
                    contactEmails = listOf(
                        ContactTestData.buildContactEmailWith(
                            name = "name 2",
                            address = "address2@protonmail.ch",
                            lastUsedTime = 6000,
                        ),
                        ContactTestData.buildContactEmailWith(
                            name = "name 1",
                            address = "address1@protonmail.ch",
                            lastUsedTime = 4000,
                        ),
                    )
                )
            ), contacts)
        }

    @Test
    fun `when there are no matching contacts, empty list is emitted`() = runTest {
        val query = "there is no contact like this"

        val contacts = searchContacts(testUserId, query).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(emptyList<Contact>(), contacts)
    }
}

object ContactTestData {

    private val contact1 = Contact(testUserId, ContactIdTestData.contactId1, "first contact", emptyList())
    private val contact2 = Contact(testUserId, ContactIdTestData.contactId2, "second contact", emptyList())

    val contacts = listOf(
        contact1,
        contact2
    )

    fun buildContactWith(
        userId: UserId = testUserId,
        contactId: ContactId = ContactIdTestData.contactId1,
        contactEmails: List<ContactEmail>,
        name: String? = null
    ) = Contact(
        userId = userId,
        id = contactId,
        name = name ?: "contact name",
        contactEmails = contactEmails
    )

    fun buildContactEmailWith(
        userId: UserId = testUserId,
        contactEmailId: ContactEmailId = ContactIdTestData.contactEmailId1,
        contactId: ContactId = ContactIdTestData.contactId1,
        name: String,
        address: String,
        lastUsedTime: Long = 0,
    ) = ContactEmail(
        userId = userId,
        id = contactEmailId,
        name = name,
        email = address,
        defaults = 0,
        order = 0,
        contactId = contactId,
        canonicalEmail = address,
        labelIds = emptyList(),
        isProton = null,
        lastUsedTime = lastUsedTime
    )
}

object ContactIdTestData {

    val contactId1 = ContactId("1")
    val contactId2 = ContactId("2")

    val contactEmailId1 = ContactEmailId("1")
}


