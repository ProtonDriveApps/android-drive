/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.background

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.list.domain.usecase.GetDecryptedDriveLinks
import me.proton.core.drive.drivelink.list.domain.usecase.GetFolderChildrenDriveLinks
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.entity.ShareUserInvitation
import me.proton.core.drive.share.user.domain.entity.ShareUsersInvitation
import me.proton.core.drive.share.user.domain.usecase.ConvertExternalInvitation
import me.proton.core.drive.share.user.domain.usecase.GetExternalInvitationsFlow
import me.proton.core.drive.share.user.domain.usecase.GetInvitationsFlow
import me.proton.core.drive.share.user.domain.usecase.InviteMembers
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.userCreate
import me.proton.core.util.kotlin.random
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ExternalInvitationTest : AbstractBaseTest() {
    val testUser = User(name = "proton_drive_${getRandomString(15)}")

    @get:Rule(order = 1)
    val userLoginRule: UserLoginRule =
        UserLoginRule(testUser, quarkCommands = quarkRule.quarkCommands)

    @Inject
    lateinit var inviteMembers: InviteMembers

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var getMainShare: GetMainShare

    @Inject
    lateinit var getLink: GetLink

    @Inject
    lateinit var getDecryptedDriveLinks: GetDecryptedDriveLinks

    @Inject
    lateinit var getFolderChildrenDriveLinks: GetFolderChildrenDriveLinks

    @Inject
    lateinit var getExternalInvitationsFlow: GetExternalInvitationsFlow

    @Inject
    lateinit var getInvitationsFlow: GetInvitationsFlow

    @Inject
    lateinit var convertExternalInvitation: ConvertExternalInvitation

    @Test
    @Scenario(2)
    fun givenExternalInvitationWhenAccountCreatedShouldConvertItToInternalInvitation() = runTest {
        val file = "image.jpg"
        val email = "external_${String.random()}@mail.com"
        val invitee = User(
            name = "",
            email = email,
            isExternal = true
        )
        val userId = requireNotNull(accountManager.getPrimaryAccount().first()).userId
        val share = getMainShare(userId).firstSuccessOrError().toResult().getOrThrow()
        val folderId = FolderId(share.id, share.rootLinkId)
        getLink(folderId).toResult().getOrThrow()
        val link = getDecryptedDriveLinks(folderId).getOrThrow().first { it.name == file }
        val linkId = link.id

        inviteMembers(
            ShareUsersInvitation(
                linkId = linkId,
                members = listOf(ShareUserInvitation(invitee.email, Permissions.viewer))
            )
        ).toResult().getOrThrow()

        quarkRule.quarkCommands.userCreate(invitee)

        val linkShared = getLink(
            linkId = linkId,
            refresh = flowOf(true),
        ).toResult().getOrThrow()

        val shareId = linkShared.sharingDetails!!.shareId

        val invitation = getExternalInvitationsFlow(
            shareId = shareId,
            refresh = flowOf(true),
        ).toResult().getOrThrow().first()

        convertExternalInvitation(linkId, invitation.id).getOrThrow()

        assertEquals(
            listOf<ShareUser>(),
            getExternalInvitationsFlow(shareId, flowOf(true)).toResult().getOrThrow()
        )
        assertEquals(
            listOf(email),
            getInvitationsFlow(shareId, flowOf(true)).toResult().getOrThrow().map { it.email },
        )
    }
}
