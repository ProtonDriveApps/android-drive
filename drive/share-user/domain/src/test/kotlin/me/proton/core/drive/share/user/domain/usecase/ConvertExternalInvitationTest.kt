package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.createInvitation
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getExternalInvitations
import me.proton.core.drive.test.api.getLink
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.getShareBootstrap
import me.proton.core.drive.test.entity.NullableFolderDto
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ConvertExternalInvitationTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var convertExternalInvitation: ConvertExternalInvitation

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")

    @Before
    fun setUp() = runTest {
        driveRule.db.myFiles { }
        driveRule.server.run {
            getPublicAddressKeysAll()
        }
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.server.run {
            getLink(NullableFolderDto(id = folderId.id, shareId = standardShareId.id))
            getShareBootstrap("address-id-user-id@proton.test")
            getExternalInvitations("external@mail.com", 2)
            createInvitation()
        }

        convertExternalInvitation(folderId, "invitation-id-external@mail.com").getOrThrow()
    }

    @Test(expected = RuntimeException::class)
    fun `fails when creating the invitation`() = runTest {
        driveRule.server.run {
            getLink(NullableFolderDto(id = folderId.id, shareId = standardShareId.id))
            getShareBootstrap("address-id-user-id@proton.test")
            getExternalInvitations("external@mail.com", 2)
            createInvitation { errorResponse() }
        }

        convertExternalInvitation(folderId, "invitation-id-external@mail.com").getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `fails when invitation state is not register user`() = runTest {
        driveRule.server.run {
            getLink(NullableFolderDto(id = folderId.id, shareId = standardShareId.id))
            getShareBootstrap("address-id-user-id@proton.test")
            getExternalInvitations("external@mail.com", 1)
        }

        convertExternalInvitation(folderId, "invitation-id-external@mail.com").getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `fails when invitation is not found`() = runTest {
        driveRule.server.run {
            getLink(NullableFolderDto(id = folderId.id, shareId = standardShareId.id))
            getShareBootstrap("address-id-user-id@proton.test")
            getExternalInvitations()
        }

        convertExternalInvitation(folderId, "invitation-id-external@mail.com").getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `fails when link is not shared`() = runTest {
        driveRule.server.run {
            getLink(NullableFolderDto(id = folderId.id, shareId = null))
        }

        convertExternalInvitation(folderId, "invitation-id-external@mail.com").getOrThrow()
    }


}
