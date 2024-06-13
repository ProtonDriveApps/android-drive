package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.Permissions.Permission.READ
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.db.test.member
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.updateMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UpdateMemberPermissionsTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var updateMemberPermissions: UpdateMemberPermissions

    @Inject
    lateinit var getMemberFlow: GetMemberFlow

    private val standardShareId = standardShareId()
    private val memberId = "member-id-member@proton.me"

    @Before
    fun setUp() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            member("member@proton.me")
        }
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.server.run {
            updateMember()
        }

        val result = updateMemberPermissions(
            shareId = standardShareId,
            memberId = memberId,
            permissions = Permissions().add(READ),
        ).filterSuccessOrError().last()

        assertTrue("${result.javaClass} should be Success", result is DataResult.Success)
        assertEquals(
            Permissions().add(READ),
            getMemberFlow(standardShareId, memberId).first().permissions,
        )
    }

    @Test
    fun fails() = runTest {
        driveRule.server.run {
            updateMember { errorResponse()}
        }

        val result = updateMemberPermissions(
            shareId = standardShareId,
            memberId = memberId,
            permissions = Permissions().add(READ),
        ).filterSuccessOrError().last()

        assertTrue("${result.javaClass} should be Error", result is DataResult.Error)
        assertEquals(
            Permissions(),
            getMemberFlow(standardShareId, memberId).first().permissions,
        )
    }
}
