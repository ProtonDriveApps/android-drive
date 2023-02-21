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
package me.proton.core.drive.link.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.share.data.db.ShareEntity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LinkWithPropertiesTest {
    private lateinit var db: TestDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = buildDatabase(context)
        runBlocking { prepareDb(db) }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetFileLink() = runBlocking {
        // region Given
        val fileProperties = testFileProperties(testParentLink.id)
        val fileLink = LinkWithProperties(testParentLink, fileProperties)
        // endregion
        // region When
        val link = insertAndGetLink(fileLink)
        // endregion
        //region Then
        assert(link.link == testParentLink)
        assert(link.properties is LinkFilePropertiesEntity)
        assert((link.properties as LinkFilePropertiesEntity) == fileProperties)
        // endregion
    }

    @Test
    fun insertAndGetFolderLink() = runBlocking {
        // region Given
        val folderProperties = testFolderProperties(testParentLink.id)
        val folderLink = LinkWithProperties(testParentLink, folderProperties)
        // endregion
        // region When
        val link = insertAndGetLink(folderLink)
        // endregion
        //region Then
        assert(link.link == testParentLink)
        assert(link.properties is LinkFolderPropertiesEntity)
        assert((link.properties as LinkFolderPropertiesEntity) == folderProperties)
        // endregion
    }

    @Test
    fun deleteLink() = runBlocking {
        // region Given
        val link = insertAndGetLink(LinkWithProperties(testParentLink, testFolderProperties(testParentLink.id)))
        // endregion
        // region When
        db.linkDao().delete(link)
        // endregion
        //region Then
        assertFalse(db.linkDao().hasLinkEntity(testParentLink.userId, testParentLink.shareId, testParentLink.id).first())
        // endregion
    }

    @Test
    fun deleteParentLink() = runBlocking {
        // region Given
        val parentLink = insertAndGetLink(LinkWithProperties(testParentLink, testFolderProperties(testParentLink.id)))
        insertAndGetLink(LinkWithProperties(testLink, testFileProperties(testLink.id)))
        // endregion
        // region When
        db.linkDao().delete(parentLink)
        // endregion
        //region Then
        assertFalse(db.linkDao().hasLinkEntity(testLink.userId, testLink.shareId, testLink.id).first())
        // endregion
    }

    private suspend fun insertAndGetLink(linkWithProperties: LinkWithProperties) = with (db.linkDao()) {
        insertOrUpdate(linkWithProperties)
        getLinkWithPropertiesFlow(
            userId = linkWithProperties.link.userId,
            shareId = linkWithProperties.link.shareId,
            linkId = linkWithProperties.link.id
        )
            .filterNotNull()
            .first()
    }

    private suspend fun prepareDb(db: TestDatabase) {
        db.accountDao().insertOrUpdate(testAccount)
        db.shareDao().insertOrUpdate(testShare)
    }

    private val testAccount =
        AccountEntity(
            userId = UserId("111"),
            username = "test",
            email = "test@proton.me",
            state = AccountState.Ready,
            sessionId = null,
            sessionState = null
        )

    private val testShare =
        ShareEntity(
            id = "222",
            userId = testAccount.userId,
            volumeId = "",
            flags = 1L,
            linkId = "333",
            isLocked = false,
            key = "share_key",
            passphrase = "share_passphrase",
            passphraseSignature = "share_passphrase_signature",
            creationTime = 0,
        )

    private val testParentLink =
        LinkEntity(
            id = "444",
            shareId = testShare.id,
            userId = testAccount.userId,
            parentId = null,
            type = 1L,
            name = "encrypted",
            nameSignatureEmail = requireNotNull(testAccount.email),
            hash = "hash",
            state = 1L,
            expirationTime = null,
            size = 100L,
            mimeType = "",
            attributes = 0L,
            permissions = 0L,
            nodeKey = "key",
            nodePassphrase = "passphrase",
            nodePassphraseSignature = "passphrase_signature",
            signatureAddress = requireNotNull(testAccount.email),
            creationTime = 0L,
            lastModified = 0L,
            trashedTime = null,
            shared = 0L,
            numberOfAccesses = 0L,
            shareUrlExpirationTime = null,
        )

    private val testLink =
        LinkEntity(
            id = "555",
            shareId = testShare.id,
            userId = testAccount.userId,
            parentId = testParentLink.id,
            type = 1L,
            name = "encrypted",
            nameSignatureEmail = requireNotNull(testAccount.email),
            hash = "hash",
            state = 1L,
            expirationTime = null,
            size = 100L,
            mimeType = "",
            attributes = 0L,
            permissions = 0L,
            nodeKey = "key",
            nodePassphrase = "passphrase",
            nodePassphraseSignature = "passphrase_signature",
            signatureAddress = requireNotNull(testAccount.email),
            creationTime = 0L,
            lastModified = 0L,
            trashedTime = null,
            shared = 0L,
            numberOfAccesses = 0L,
            shareUrlExpirationTime = null,
        )

    private fun testFolderProperties(linkId: String) =
        LinkFolderPropertiesEntity(
            userId = testAccount.userId,
            shareId = testShare.id,
            linkId = linkId,
            nodeHashKey = "nodeHashKey"
        )

    private fun testFileProperties(linkId: String) =
        LinkFilePropertiesEntity(
            userId = testAccount.userId,
            shareId = testShare.id,
            linkId = linkId,
            activeRevisionId = "5555",
            hasThumbnail = false,
            contentKeyPacket = "content_key_packet",
            contentKeyPacketSignature = "content_key_packet_signature"
        )
}
