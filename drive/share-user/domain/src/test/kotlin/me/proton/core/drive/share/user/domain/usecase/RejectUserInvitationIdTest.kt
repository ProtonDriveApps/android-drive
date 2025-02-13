package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.data.api.response.Response
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.standardShareWithMe
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.userInvitation
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.rejectUserInvitation
import me.proton.core.drive.test.api.jsonResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RejectUserInvitationIdTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var rejectUserInvitation: RejectUserInvitation

    private val standardShareId = standardShareId()
    private val invitationId = "user-invitation-id-member@proton.me"
    private val id = UserInvitationId(volumeId, standardShareId, invitationId)

    @Before
    fun setUp() = runTest {
        driveRule.db.standardShareWithMe(standardShareId.id) {
            userInvitation(invitationId)
        }
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.server.rejectUserInvitation()

        rejectUserInvitation(id).toResult().getOrThrow()
    }

    @Test(expected = RuntimeException::class)
    fun `server error`() = runTest {
        driveRule.server.rejectUserInvitation {
            jsonResponse(422) {
                Response(ProtonApiCode.INVALID_VALUE.toLong())
            }
        }

        rejectUserInvitation(id).toResult().getOrThrow()
    }
}
