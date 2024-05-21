package me.proton.core.drive.share.crypto.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getPublicAddressKeys
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.key.data.api.response.PublicAddressKeyResponse
import me.proton.core.key.data.api.response.PublicAddressKeysResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CreateShareInvitationRequestTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var createShareInvitationRequest: CreateShareInvitationRequest

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")

    @Before
    fun setUp() = runTest{
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
    fun `happy path`() = runTest {
        driveRule.server.run {
            getPublicAddressKeys()
        }
        createShareInvitationRequest(
            shareId = standardShareId,
            inviterEmail = "inviter@proton.me",
            inviteeEmail = "invitee@proton.me",
            permissions = Permissions(),
        ).getOrThrow()
    }

    @Test(expected = CryptoException::class)
    fun `fails crypto`() = runTest {
        driveRule.server.run {
            getPublicAddressKeys {
                jsonResponse {
                    val email = recordedRequest.requestUrl?.queryParameter("Email")
                    PublicAddressKeysResponse(
                        recipientType = 1,
                        keys = listOf(
                            PublicAddressKeyResponse(
                                flags = 8,
                                publicKey = "public-key-$email"
                            )
                        )
                    )
                }
            }
        }
        createShareInvitationRequest(
            shareId = standardShareId,
            inviterEmail = "inviter@proton.me",
            inviteeEmail = "invitee@proton.me",
            permissions = Permissions(),
        ).getOrThrow()
    }
}
