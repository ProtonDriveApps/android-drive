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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.contact.domain.usecase.GetContactEmails
import me.proton.core.drive.label.domain.entity.LabelWithContacts
import me.proton.core.drive.label.domain.usecase.ContactTestData.contact1
import me.proton.core.drive.label.domain.usecase.ContactTestData.contact2
import me.proton.core.drive.label.domain.usecase.LabelTestData.label1
import me.proton.core.drive.label.domain.usecase.LabelTestData.label2
import me.proton.core.drive.label.domain.usecase.LabelTestData.labelId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Assert.assertEquals
import org.junit.Test

private val testUserId = UserId("user-id")

class SearchLabelsWithContactsTest {

    private val getLabels = mockk<GetLabels> {
        coEvery { this@mockk.invoke(testUserId) } returns flowOf(
            DataResult.Success(
                ResponseSource.Local,
                LabelTestData.labels,
            )
        )
    }

    private val getContactEmails = mockk<GetContactEmails> {
        coEvery { this@mockk.invoke(testUserId) } returns flowOf(
            DataResult.Success(
                ResponseSource.Local,
                ContactTestData.contacts,
            )
        )
    }

    private val searchLabelsWithContacts = SearchLabelsWithContacts(getLabels, getContactEmails)

    @Test
    fun all() = runTest {
        val query = "label"

        val labelWithContacts =
            searchLabelsWithContacts(testUserId, query).filterSuccessOrError().toResult()
                .getOrThrow()

        assertEquals(
            listOf(
                LabelWithContacts(label1, listOf(contact1, contact2)),
                LabelWithContacts(label2, listOf(contact2)),
            ),
            labelWithContacts,
        )
    }

    @Test
    fun none() = runTest {
        val query = "none"

        val labelWithContacts =
            searchLabelsWithContacts(testUserId, query).filterSuccessOrError().toResult()
                .getOrThrow()

        assertEquals(emptyList<LabelWithContacts>(), labelWithContacts)
    }

    @Test
    fun one() = runTest {
        val query = "el-1"

        val labelWithContacts =
            searchLabelsWithContacts(testUserId, query).filterSuccessOrError().toResult()
                .getOrThrow()

        assertEquals(
            listOf(
                LabelWithContacts(label1, listOf(contact1, contact2)),
            ),
            labelWithContacts,
        )
    }

    @Test
    fun `no contact`() = runTest {
        val query = "label-3"

        val labelWithContacts =
            searchLabelsWithContacts(testUserId, query).filterSuccessOrError().toResult()
                .getOrThrow()

        assertEquals(
            emptyList<LabelWithContacts>(),
            labelWithContacts,
        )
    }
}

object ContactTestData {

    val contact1 = buildContactEmailWith(1, listOf(labelId(1)))
    val contact2 = buildContactEmailWith(2, listOf(labelId(1), labelId(2)))

    val contacts = listOf(
        contact1,
        contact2
    )

    private fun buildContactEmailWith(
        index: Int,
        labelsIds: List<LabelId> = emptyList()
    ) = ContactEmail(
        userId = testUserId,
        id = ContactEmailId(index.toString()),
        name = "name$index",
        email = "email$index@email.com",
        defaults = 0,
        order = 0,
        contactId = ContactId(index.toString()),
        canonicalEmail = "address$index",
        labelIds = labelsIds.map { labelId -> labelId.id },
        isProton = null,
        lastUsedTime = 0,
    )
}

object LabelTestData {

    val label1 = buildLabelWith(1, "label-1")
    val label2 = buildLabelWith(2, "label-2")
    val label3 = buildLabelWith(3, "label-3")

    val labels = listOf(
        label1,
        label2,
        label3,
    )

    fun labelId(index: Int) = LabelId("label-id-$index")

    private fun buildLabelWith(
        index: Int,
        name: String,
    ) = Label(
        userId = testUserId,
        labelId = labelId(index),
        name = name,
        order = 0,
        parentId = null,
        type = LabelType.ContactGroup,
        path = "",
        color = "",
        isNotified = null,
        isExpanded = null,
        isSticky = null,
    )
}
