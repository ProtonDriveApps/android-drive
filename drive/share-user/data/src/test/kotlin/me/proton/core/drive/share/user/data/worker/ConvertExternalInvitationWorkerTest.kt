package me.proton.core.drive.share.user.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.standardShareByMe
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.user.domain.usecase.ConvertExternalInvitation
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.createInvitation
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getExternalInvitations
import me.proton.core.drive.test.api.getLink
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.retryableErrorResponse
import me.proton.core.drive.test.entity.NullableFolderDto
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
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
class ConvertExternalInvitationWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var convertExternalInvitation: ConvertExternalInvitation

    @Inject
    lateinit var canRun: CanRun

    @Inject
    lateinit var done: Done

    @Inject
    lateinit var run: Run

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")
    private val externalInvitationId = "invitation-id-external@mail.com"

    @Before
    fun setUp() = runTest {
        driveRule.db.run {
            standardShareByMe(standardShareId.id)
            myFiles {
                folder(
                    id = folderId.id,
                    sharingDetailsShareId = standardShareId.id,
                )
            }
        }
        driveRule.server.run {
            getLink(NullableFolderDto(id = folderId.id, shareId = standardShareId.id))
            getExternalInvitations("external@mail.com", 2)
            getPublicAddressKeysAll()
        }
    }

    @Test
    fun success() = runTest {
        driveRule.server.run {
            createInvitation()
        }

        val worker = convertExternalInvitationWorker(folderId, externalInvitationId)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }


    @Test
    fun retry() = runTest {
        driveRule.server.run {
            createInvitation { retryableErrorResponse() }
        }

        val worker = convertExternalInvitationWorker(folderId, externalInvitationId)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }


    @Test
    fun failure() = runTest {
        driveRule.server.run {
            createInvitation { errorResponse() }
        }

        val worker = convertExternalInvitationWorker(folderId, externalInvitationId)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }


    @Test
    fun config() = runTest {
        val request = ConvertExternalInvitationWorker.getWorkRequest(folderId, externalInvitationId)

        assertEquals(
            Constraints(requiredNetworkType = NetworkType.CONNECTED),
            request.workSpec.constraints,
        )

        assertTrue("Tags contains userId", userId.id in request.tags)
        assertTrue("Tags contains external invitation id", externalInvitationId in request.tags)
    }


    private fun convertExternalInvitationWorker(
        linkId: LinkId,
        externalInvitationId: String
    ): ConvertExternalInvitationWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<ConvertExternalInvitationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = ConvertExternalInvitationWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    convertExternalInvitation = convertExternalInvitation,
                    canRun = canRun,
                    run = run,
                    done = done,
                )

            })
            .setInputData(
                ConvertExternalInvitationWorker.workDataOf(
                    linkId, externalInvitationId
                )
            )
            .build()
    }

}
