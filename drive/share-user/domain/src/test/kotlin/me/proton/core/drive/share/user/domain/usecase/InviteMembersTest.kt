package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.user.domain.entity.ShareUserInvitation
import me.proton.core.drive.share.user.domain.entity.ShareUsersInvitation
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.createInvitation
import me.proton.core.drive.test.api.getPublicAddressKeys
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class InviteMembersTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var inviteMembers: InviteMembers

    private val folderId = FolderId(mainShareId, "folder-id")

    @Test
    fun test() = runTest {
        driveRule.db.user {
            volume {
                val standardShareId = standardShareId()
                standardShare(standardShareId.id)
                mainShare {
                    folder(
                        id = folderId.id,
                        sharingDetailsShareId = standardShareId.id,
                    )
                }
            }
        }
        driveRule.server.run {
            getPublicAddressKeys()
            createInvitation()
        }
        val shareUserInvitation = ShareUserInvitation(
            email = "invitee@proton.me",
            permissions = Permissions(),
        )
        val result = inviteMembers(
            ShareUsersInvitation(
                linkId = folderId,
                members = listOf(
                    shareUserInvitation
                )
            )
        ).last().toResult().getOrThrow()

        assertEquals("failures", emptySet<ShareUserInvitation>(), result.failures.keys)
        assertEquals("successes", setOf(shareUserInvitation), result.successes.keys)
    }
}
