package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.api.ProtonApiCode
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
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.createExternalInvitation
import me.proton.core.drive.test.api.createInvitation
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getPublicAddressKeys
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.network.data.protonApi.ProtonErrorData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CreateShareInvitationTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var createShareInvitation: CreateShareInvitation

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")

    @Before fun setUp() = runTest{
        driveRule.db.user {
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
    fun `internal invitation`() = runTest {
        driveRule.server.run {
            getPublicAddressKeysAll()
            createInvitation()
        }
        createShareInvitation(
            shareId = standardShareId,
            invitation = ShareUserInvitation(
                email = "invitee@proton.me",
                permissions = Permissions(),
            )
        ).filterSuccessOrError().toResult().getOrThrow()
    }

    @Test
    fun `external invitation`() = runTest {
        driveRule.server.run {
            getPublicAddressKeysAll {
                jsonResponse(422) {
                    ProtonErrorData(
                        code = ProtonApiCode.KEY_GET_DOMAIN_EXTERNAL,
                        error = "This address does not exist. Please try again",
                    )
                }
            }
            createExternalInvitation("inviter@proton.me")
        }
        createShareInvitation(
            shareId = standardShareId,
            invitation = ShareUserInvitation(
                email = "invitee@external.com",
                permissions = Permissions(),
            )
        ).filterSuccessOrError().toResult().getOrThrow()
    }

    @Test(expected = RuntimeException::class)
    fun fails() = runTest {
        driveRule.server.run {
            getPublicAddressKeysAll()
            createInvitation { errorResponse() }
        }
        createShareInvitation(
            shareId = standardShareId,
            invitation = ShareUserInvitation(
                email = "invitee@proton.me",
                permissions = Permissions(),
            )
        ).filterSuccessOrError().toResult().getOrThrow()
    }
}
