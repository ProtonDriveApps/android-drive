package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.db.test.externalInvitation
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.sendExternalEmail
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ResendExternalInvitationTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var resendExternalInvitation: ResendExternalInvitation

    private val standardShareId = standardShareId()
    private val invitationId = "invitation-id-invitee@external.com"

    @Before
    fun setUp() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            externalInvitation("invitee@external.com")
        }
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.server.sendExternalEmail()

        val result = resendExternalInvitation(
            shareId = standardShareId,
            invitationId = invitationId,
        ).filterSuccessOrError().last()

        assertTrue("${result.javaClass} should be Success", result is DataResult.Success)
    }

    @Test
    fun fails() = runTest {
        driveRule.server.sendExternalEmail { errorResponse() }

        val result = resendExternalInvitation(
            shareId = standardShareId,
            invitationId = invitationId,
        ).filterSuccessOrError().last()

        assertTrue("${result.javaClass} should be Error", result is DataResult.Error)
    }
}
