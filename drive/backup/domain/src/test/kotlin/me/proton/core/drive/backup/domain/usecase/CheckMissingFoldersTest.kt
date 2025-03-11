package me.proton.core.drive.backup.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BucketEntry
import me.proton.core.drive.backup.domain.repository.TestBucketRepository
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CheckMissingFoldersTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder1: BackupFolder
    private lateinit var backupFolder2: BackupFolder

    @Inject
    lateinit var checkMissingFolders: CheckMissingFolders

    @Inject
    lateinit var testBucketRepository: TestBucketRepository

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var getAllFolders: GetAllFolders

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }

        backupFolder1 = BackupFolder(
            bucketId = 1,
            folderId = folderId
        )
        addFolder(backupFolder1).getOrThrow()

        backupFolder2 = BackupFolder(
            bucketId = 1,
            folderId = folderId
        )
        addFolder(backupFolder2).getOrThrow()
    }

    @Test
    fun `happy path`() = runTest {
        testBucketRepository.bucketEntries = listOf(BucketEntry(1, null))

        checkMissingFolders(userId).getOrThrow()

        assertEquals(listOf(backupFolder1), getAllFolders(userId).getOrThrow())
    }
}
