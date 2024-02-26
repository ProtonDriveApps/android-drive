/*
 * Copyright (c) 2023-2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.domain.usecase

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.data.usecase.EnablePhotosBackupImpl
import me.proton.android.drive.photos.domain.manager.StubbedBackupConnectivityManager
import me.proton.android.drive.photos.domain.manager.StubbedEventHandler
import me.proton.android.drive.photos.domain.provider.PhotosDefaultConfigurationProvider
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Backup.BackupState.PAUSED_DISABLED
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.manager.BackupPermissionsManagerImpl
import me.proton.core.drive.backup.data.repository.BackupConfigurationRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.entity.BucketEntry
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.backup.domain.repository.BucketRepository
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.backup.domain.usecase.DeleteFolders
import me.proton.core.drive.backup.domain.usecase.GetAllBuckets
import me.proton.core.drive.backup.domain.usecase.GetBackupState
import me.proton.core.drive.backup.domain.usecase.GetBackupStatus
import me.proton.core.drive.backup.domain.usecase.GetConfiguration
import me.proton.core.drive.backup.domain.usecase.GetErrors
import me.proton.core.drive.backup.domain.usecase.StartBackup
import me.proton.core.drive.backup.domain.usecase.UpdateConfiguration
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.IsBackgroundRestricted
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.drivelink.data.extension.toEncryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoBackupStateTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private val appContext = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var enablePhotosBackup: EnablePhotosBackup
    private lateinit var disablePhotosBackup: DisablePhotosBackup
    private lateinit var backupState: Flow<BackupState>

    private lateinit var repository: BackupFolderRepository
    private lateinit var backupManager: StubbedBackupManager
    private val permissionsManager: BackupPermissionsManager =
        BackupPermissionsManagerImpl(appContext)
    private val connectivityManager: BackupConnectivityManager = StubbedBackupConnectivityManager
    private val handler = StubbedEventHandler()

    @Before
    fun setUp() = runTest {
        repository = BackupFolderRepositoryImpl(database.db)
        backupManager = StubbedBackupManager(repository)
        folderId = database.photo {}
        val errorRepository = BackupErrorRepositoryImpl(database.db)
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        val fileRepository = BackupFileRepositoryImpl(database.db)

        val pm = mockk<BackupPermissionsManager>()
        every { pm.getBackupPermissions(true) } returns BackupPermissions.Granted

        val getPhotosDriveLink = mockk<GetPhotosDriveLink>()
        every { getPhotosDriveLink(userId) } returns database.db.driveLinkDao.getLinkWithPropertiesFlow(
            userId = userId,
            shareId = folderId.shareId.id,
            linkId = folderId.id
        ).map { linkWithProperties ->
            (linkWithProperties?.toLink()?.toEncryptedDriveLink(
                volumeId = VolumeId(volumeId),
                isMarkedAsOffline = false,
                downloadState = null,
                trashState = null
            ) as DriveLink.Folder?).asSuccessOrNullAsError()
        }
        val announceEvent = AnnounceEvent(setOf(handler))

        val backupConfigurationRepository = BackupConfigurationRepositoryImpl(database.db)
        val getConfiguration = GetConfiguration(backupConfigurationRepository)
        enablePhotosBackup = EnablePhotosBackupImpl(
            appContext = appContext,
            setupPhotosBackup = SetupPhotosBackup(
                setupPhotosConfigurationBackup = SetupPhotosConfigurationBackup(
                    getConfiguration = getConfiguration,
                    updateConfiguration = UpdateConfiguration(
                        repository = backupConfigurationRepository
                    ),
                    photosDefaultConfigurationProvider = object :
                        PhotosDefaultConfigurationProvider {}
                ),
                addFolder = AddFolder(folderRepository),
                bucketRepository = object : BucketRepository {
                    override suspend fun getAll() = listOf(BucketEntry(0, "Camera"))
                }
            ),
            backupManager = backupManager,
            startBackup = StartBackup(backupManager, announceEvent),
            permissionsManager = pm,
            configurationProvider = object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader = ""
            },
            announceEvent = announceEvent,
        )

        disablePhotosBackup = DisablePhotosBackup(
            clearPhotosBackup = ClearPhotosBackup(DeleteFolders(folderRepository)),
            backupManager = backupManager,
            announceEvent = announceEvent,
        )
        val getBackupState = GetBackupState(
            getBackupStatus = GetBackupStatus(fileRepository),
            backupManager = backupManager,
            permissionsManager = permissionsManager,
            connectivityManager = connectivityManager,
            getErrors = GetErrors(errorRepository, NoNetworkConfigurationProvider.instance),
            getAllBuckets = GetAllBuckets(object : BucketRepository {
                override suspend fun getAll(): List<BucketEntry> = listOf(BucketEntry(0, "Camera"))
            }, permissionsManager),
            configurationProvider = object : ConfigurationProvider {
                override val host = ""
                override val baseUrl = ""
                override val appVersionHeader = ""
                override val backupDefaultBucketName = "Camera"
            },
            isBackgroundRestricted = object : IsBackgroundRestricted {
                override fun invoke(): Flow<Boolean> = flowOf(false)
            },
            getConfiguration = getConfiguration,
        )

        backupState = getBackupState(folderId)
    }

    @Test
    fun `blank photos status`() = runTest {
        assertEquals(
            BackupState(
                isBackupEnabled = false,
                hasDefaultFolder = null,
                backupStatus = null,
            ),
            backupState.first(),
        )
    }

    @Test
    fun `running photos status`() = runTest {
        enablePhotosBackup(folderId).getOrThrow()
        permissionsManager.onPermissionChanged(BackupPermissions.Granted)

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
                backupStatus = BackupStatus.Complete(totalBackupPhotos = 0),
            ),
            backupState.first(),
        )
        assertTrue(backupManager.started)
        assertEquals(
            mapOf(
                userId to listOf(
                    Event.BackupEnabled(folderId),
                    Event.BackupStarted(folderId),
                )
            ),
            handler.events,
        )
    }

    @Test
    fun `disabled photos status`() = runTest {
        enablePhotosBackup(folderId).getOrThrow()
        permissionsManager.onPermissionChanged(BackupPermissions.Granted)
        disablePhotosBackup(folderId).getOrThrow()

        assertEquals(
            BackupState(
                isBackupEnabled = false,
                hasDefaultFolder = true,
                backupStatus = null,
            ),
            backupState.first(),
        )
        assertTrue(backupManager.stopped)
        assertEquals(
            mapOf(
                userId to listOf(
                    Event.BackupEnabled(folderId),
                    Event.BackupStarted(folderId),
                    Event.BackupStopped(folderId, PAUSED_DISABLED),
                    Event.BackupDisabled(folderId),
                )
            ),
            handler.events,
        )
    }

    @Test
    fun `failed photos status`() = runTest {
        enablePhotosBackup(folderId).getOrThrow()
        permissionsManager.onPermissionChanged(BackupPermissions.Denied(false))

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
                backupStatus = BackupStatus.Failed(
                    errors = listOf(BackupError.Permissions()),
                    totalBackupPhotos = 0,
                    pendingBackupPhotos = 0,
                ),
            ),
            backupState.first(),
        )
        assertTrue(backupManager.started)
    }

    private class StubbedBackupManager(
        private val repository: BackupFolderRepository,
    ) : BackupManager {
        var started = false
        var stopped = false

        override suspend fun start(folderId: FolderId) {
            started = true
        }

        override suspend fun stop(folderId: FolderId) {
            stopped = true
        }

        override fun sync(backupFolder: BackupFolder, uploadPriority: Long) {
            throw NotImplementedError()
        }

        override suspend fun cancelSync(backupFolder: BackupFolder) {
            throw NotImplementedError()
        }

        override fun syncAllFolders(folderId: FolderId, uploadPriority: Long) {
            throw NotImplementedError()
        }

        override suspend fun watchFolders(userId: UserId) {
            throw NotImplementedError()

        }

        override suspend fun unwatchFolders(userId: UserId) {
            throw NotImplementedError()
        }

        override fun isEnabled(folderId: FolderId): Flow<Boolean> =
            repository.hasFolders(folderId)

        override fun isUploading(folderId: FolderId): Flow<Boolean> = flowOf(true)

    }
}
