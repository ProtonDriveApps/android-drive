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
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.key.data.api.response.ActivePublicKeysResponse
import me.proton.core.key.data.api.response.AddressDataResponse
import me.proton.core.key.data.api.response.PublicAddressKeyResponse
import me.proton.core.key.data.api.response.PublicAddressKeysResponse
import me.proton.core.key.domain.entity.key.KeyFlags
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
            getPublicAddressKeysAll()
        }
        createShareInvitationRequest(
            shareId = standardShareId,
            inviteeEmail = "invitee@proton.me",
            permissions = Permissions(),
        ).getOrThrow()
    }

    @Test(expected = CryptoException::class)
    fun `fails crypto`() = runTest {
        driveRule.server.run {
            getPublicAddressKeysAll {
                jsonResponse {
                    val email = recordedRequest.requestUrl?.queryParameter("Email")
                    ActivePublicKeysResponse(
                        address = AddressDataResponse(
                            keys = listOf(
                                PublicAddressKeyResponse(
                                    flags = KeyFlags.EmailNoSign,
                                    publicKey = "public-key-$email",
                                )
                            )
                        ),
                        warnings = emptyList(),
                        protonMx = false,
                        isProton = 1,
                    )
                }
            }
        }
        createShareInvitationRequest(
            shareId = standardShareId,
            inviteeEmail = "invitee@proton.me",
            permissions = Permissions(),
        ).getOrThrow()
    }
}
