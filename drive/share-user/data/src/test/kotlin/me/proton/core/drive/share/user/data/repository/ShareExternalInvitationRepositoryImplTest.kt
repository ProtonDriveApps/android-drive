package me.proton.core.drive.share.user.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.externalInvitation
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareExternalInvitationRepository
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.clear
import me.proton.core.drive.test.api.createExternalInvitation
import me.proton.core.drive.test.api.deleteExternalInvitation
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getExternalInvitations
import me.proton.core.drive.test.api.sendExternalEmail
import me.proton.core.drive.test.api.updateExternalInvitation
import me.proton.core.network.domain.ApiException
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ShareExternalInvitationRepositoryImplTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var repository: ShareExternalInvitationRepository

    private val standardShareId = standardShareId()

    @Test
    fun hasExternalInvitations_empty() = runTest {
        driveRule.db.run {
            standardShare(standardShareId.id)
        }
        assertFalse(repository.hasExternalInvitations(standardShareId))
    }

    @Test
    fun hasExternalInvitations_one() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            externalInvitation("invitee@external.com")
        }

        assertTrue(repository.hasExternalInvitations(standardShareId))
    }

    @Test
    fun getExternalInvitationsFlow_empty() = runTest {
        driveRule.db.standardShare(standardShareId.id)
        assertEquals(
            emptyList<ShareUser>(),
            repository.getExternalInvitationsFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun getExternalInvitationsFlow_one() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            externalInvitation("invitee@external.com")
        }

        assertEquals(
            listOf(
                ShareUser.ExternalInvitee(
                    id = "invitation-id-invitee@external.com",
                    inviter = "inviter@proton.me",
                    email = "invitee@external.com",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    signature = "invitation-signature",
                    state = ShareUser.ExternalInvitee.State.PENDING,
                )
            ), repository.getExternalInvitationsFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun getExternalInvitationFlow() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            externalInvitation("invitee@external.com")
        }

        assertEquals(
            ShareUser.ExternalInvitee(
                id = "invitation-id-invitee@external.com",
                inviter = "inviter@proton.me",
                email = "invitee@external.com",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                signature = "invitation-signature",
                state = ShareUser.ExternalInvitee.State.PENDING,
            ),
            repository.getExternalInvitationFlow(
                shareId = standardShareId,
                invitationId = "invitation-id-invitee@external.com",
            ).first(),
        )
    }

    @Test
    fun fetchExternalInvitations() = runTest {
        driveRule.db.standardShare(standardShareId.id)
        driveRule.server.getExternalInvitations("invitee@external.com")
        val fetchInvitations = repository.fetchExternalInvitations(standardShareId)
        assertEquals(
            listOf(
                ShareUser.ExternalInvitee(
                    id = "invitation-id-invitee@external.com",
                    inviter = "inviter@proton.me",
                    email = "invitee@external.com",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    signature = "invitation-signature",
                    state = ShareUser.ExternalInvitee.State.PENDING,
                )
            ), fetchInvitations
        )
    }

    @Test
    fun `fetchExternalInvitations should delete all previous invitations`() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.getExternalInvitations("invitee1@external.com")
        repository.fetchExternalInvitations(standardShareId)
        driveRule.server.clear()

        driveRule.server.getExternalInvitations("invitee2@external.com")
        repository.fetchExternalInvitations(standardShareId)

        assertEquals(
            listOf(
                ShareUser.ExternalInvitee(
                    id = "invitation-id-invitee2@external.com",
                    inviter = "inviter@proton.me",
                    email = "invitee2@external.com",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    signature = "invitation-signature",
                    state = ShareUser.ExternalInvitee.State.PENDING,
                )
            ), repository.getExternalInvitationsFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun createExternalInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.createExternalInvitation("inviter@proton.me")
        val shareUserInvitee = repository.createExternalInvitation(
            standardShareId, ShareInvitationRequest.External(
                inviterAddressId = AddressId("address-id"),
                inviteeEmail = "invitee@external.com",
                permissions = Permissions(0L),
                invitationSignature = "invitation-signature",
            )
        )
        assertEquals(
            ShareUser.ExternalInvitee(
                id = "invitation-id-invitee@external.com",
                inviter = "inviter@proton.me",
                email = "invitee@external.com",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                signature = "invitation-signature",
                state = ShareUser.ExternalInvitee.State.PENDING,
            ), shareUserInvitee
        )
    }

    @Test(expected = ApiException::class)
    fun createExternalInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.createExternalInvitation { errorResponse() }

        repository.createExternalInvitation(
            standardShareId, ShareInvitationRequest.External(
                inviterAddressId = AddressId("address-id"),
                inviteeEmail = "invitee@external.com",
                permissions = Permissions(0L),
                invitationSignature = "invitation-signature",
            )
        )
    }

    @Test
    fun updateExternalInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            externalInvitation("invitee@external.com")
        }

        driveRule.server.updateExternalInvitation()

        repository.updateExternalInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
            permissions = Permissions.viewer,
        )
    }

    @Test(expected = ApiException::class)
    fun updateExternalInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.updateExternalInvitation { errorResponse() }

        repository.updateExternalInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
            permissions = Permissions.viewer,
        )
    }

    @Test
    fun deleteExternalInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.deleteExternalInvitation()

        repository.deleteExternalInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
        )
    }

    @Test(expected = ApiException::class)
    fun deleteExternalInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.deleteExternalInvitation { errorResponse() }

        repository.deleteExternalInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
        )
    }

    @Test
    fun resendExternalInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.sendExternalEmail()

        repository.resendExternalInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
        )
    }

    @Test(expected = ApiException::class)
    fun resendExternalInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.sendExternalEmail { errorResponse() }

        repository.resendExternalInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
        )
    }
}
