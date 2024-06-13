package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.deleteMember
import me.proton.core.drive.test.api.errorResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LeaveShareTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var leaveShare: LeaveShare

    private val standardShareId = standardShareId()

    @Test
    fun `happy path`() = runTest {
        val folderId = driveRule.db.user {
            volume {
                standardShare(standardShareId.id) {
                    folder("shared-folder") {}
                }
            }
        }.let { FolderId(it, "shared-folder") }
        driveRule.server.run {
            deleteMember()
        }

        leaveShare(
            volumeId = volumeId,
            memberId = "member-id-${standardShareId.id}",
            linkId = folderId,
        ).filterSuccessOrError().last().toResult().getOrThrow()
    }

    @Test(expected = RuntimeException::class)
    fun `fails with server error`() = runTest {
        val folderId = driveRule.db.user {
            volume {
                standardShare(standardShareId.id) {
                    folder("shared-folder") {}
                }
            }
        }.let { FolderId(it, "shared-folder") }
        driveRule.server.run {
            deleteMember { errorResponse() }
        }

        leaveShare(
            volumeId = volumeId,
            memberId = "member-id-${standardShareId.id}",
            linkId = folderId,
        ).filterSuccessOrError().last().toResult().getOrThrow()
    }
}
