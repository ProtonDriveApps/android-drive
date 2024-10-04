package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.standardShareByMe
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.user.data.api.request.CreateShareInvitationRequest
import me.proton.core.drive.share.user.domain.entity.ShareUserInvitation
import me.proton.core.drive.share.user.domain.entity.ShareUsersInvitation
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.createInvitation
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.request
import me.proton.core.drive.test.api.toResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CreateShareInvitationsTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var createShareInvitations: CreateShareInvitations

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")

    @Before
    fun setUp() = runTest {
        driveRule.db.user {
            volume {
                standardShareByMe(standardShareId.id)
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
    fun `happy path`() = runTest {
        driveRule.server.run {
            getPublicAddressKeysAll()
            createInvitation {
                val request = request<CreateShareInvitationRequest>()
                when (request.invitation.inviteeEmail) {
                    "invitee@proton.me" -> jsonResponse { request.toResponse() }
                    else -> errorResponse()
                }
            }
        }
        val inviteeInvitation = ShareUserInvitation(
            email = "invitee@proton.me",
            permissions = Permissions(),
        )
        val errorInvitation = ShareUserInvitation(
            email = "error@proton.me",
            permissions = Permissions(),
        )
        val result = createShareInvitations(
            shareId = standardShareId,
            invitation = ShareUsersInvitation(
                linkId = folderId,
                members = listOf(
                    inviteeInvitation,
                    errorInvitation,
                )
            ),
        ).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(setOf(errorInvitation), result.failures.keys)
        assertEquals(setOf(inviteeInvitation), result.successes.keys)
    }
}
