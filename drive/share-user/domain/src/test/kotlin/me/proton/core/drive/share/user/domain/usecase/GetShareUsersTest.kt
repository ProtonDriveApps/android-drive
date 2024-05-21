package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.contactEmail
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getInvitations
import me.proton.core.drive.test.api.getMembers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetShareUsersTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getShareUsers: GetShareUsers

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")

    @Before
    fun setUp() = runTest {
        driveRule.db.user {
            contactEmail("invitee")
            volume {
                standardShare(standardShareId.id)
                mainShare {
                    folder(
                        id = folderId.id,
                        sharingDetailsShareId = standardShareId.id,
                    )
                }
            }
        }
    }

    @Test
    fun `not shared`() = runTest {
        driveRule.db.myFiles {
            folder(
                id = folderId.id,
                sharingDetailsShareId = null,
            )
        }

        val shareUsers =
            getShareUsers(folderId).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(emptyList<ShareUser>(), shareUsers)
    }

    @Test
    fun `no invitees no members`() = runTest {
        driveRule.db.user {
            contactEmail("invitee")
            volume {
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
            getInvitations()
            getMembers()
        }

        val shareUsers =
            getShareUsers(folderId).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(emptyList<ShareUser>(), shareUsers)
    }

    @Test
    fun `one invitation one member`() = runTest {
        driveRule.db.user {
            contactEmail("invitee")
            contactEmail("member")
            volume {
                standardShare(standardShareId.id) {
                    //invitation("invitee@proton.me")
                    //member("member@proton.me")
                }
                mainShare {
                    folder(
                        id = folderId.id,
                        sharingDetailsShareId = standardShareId.id,
                    )
                }
            }
        }
        // Replace mock api by database insertion with events are implemented
        driveRule.server.run {
            getInvitations("invitee@proton.me")
            getMembers("member@proton.me")
        }

        val shareUsers = getShareUsers(folderId).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(
            listOf(
                ShareUser.Invitee(
                    id = "invitation-id-invitee@proton.me",
                    inviter = "inviter@proton.me",
                    email = "invitee@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    displayName = "invitee",
                    keyPacket = "invitation-key-packet",
                    keyPacketSignature = "invitation-key-packet-signature",
                ),
                ShareUser.Member(
                    id = "member-id-member@proton.me",
                    inviter = "inviter@proton.me",
                    email = "member@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    displayName = "member",
                    keyPacket = "member-key-packet",
                    keyPacketSignature = "member-key-packet-signature",
                    sessionKeySignature = "member-session-key-signature",
                )
            ), shareUsers
        )
    }

}
