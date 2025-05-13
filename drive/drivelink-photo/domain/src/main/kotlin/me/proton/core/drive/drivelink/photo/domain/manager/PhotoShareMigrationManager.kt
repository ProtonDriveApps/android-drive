/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.photo.domain.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.photo.domain.usecase.PhotoShareCleanup
import me.proton.core.drive.feature.flag.domain.usecase.AlbumsFeatureFlag
import me.proton.core.drive.photo.domain.entity.PhotoShareMigrationState
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolume
import me.proton.core.drive.volume.domain.usecase.HasPhotoVolume
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class PhotoShareMigrationManager @Inject constructor(
    coroutineContext: CoroutineContext,
    albumsFeatureFlag: AlbumsFeatureFlag,
    configurationProvider: ConfigurationProvider,
    private val photoShareMigrationRepository: PhotoShareMigrationRepository,
    private val getVolume: GetVolume,
    private val photoShareCleanup: PhotoShareCleanup,
    private val hasPhotoVolume: HasPhotoVolume,
) {
    private val coroutineScope = CoroutineScope(coroutineContext)
    private var fetchingJob: Job? = null
    private val mutex = Mutex()
    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(configurationProvider.minimumPhotoShareMigrationStatusFetchInterval)
        }
    }
    private val userId: MutableStateFlow<UserId?> = MutableStateFlow(null)
    private val state: StateFlow<PhotoShareMigrationState> = userId
        .filterNotNull()
        .transform {
            emitAll(
                photoShareMigrationRepository
                    .getMigrationStateFlow(it)
                    .map { state ->
                        state ?: PhotoShareMigrationState.UNINITIALIZED
                    }
            )
        }.stateIn(coroutineScope, Eagerly, PhotoShareMigrationState.UNINITIALIZED)
    private val albumsFeatureOn: StateFlow<Boolean> = userId
        .filterNotNull()
        .transform {
            emitAll(
                albumsFeatureFlag(it)
            )
        }.stateIn(coroutineScope, Eagerly, false)

    val status: Flow<MigrationStatus> = state.map { migrationState ->
        when (migrationState) {
            PhotoShareMigrationState.PENDING -> MigrationStatus.PENDING
            PhotoShareMigrationState.IN_PROGRESS -> MigrationStatus.IN_PROGRESS
            PhotoShareMigrationState.CLEANUP -> MigrationStatus.IN_PROGRESS
            PhotoShareMigrationState.COMPLETE -> MigrationStatus.COMPLETE
            PhotoShareMigrationState.UNINITIALIZED -> MigrationStatus.INITIALIZING
        }
    }

    suspend fun initialize(
        userId: UserId,
        coroutineScope: CoroutineScope,
        inForeground: Flow<Boolean>,
    ): Result<Unit> = coRunCatching {
        this.userId.value = userId
        val initializedState = photoShareMigrationRepository.getMigrationState(userId)
            ?: PhotoShareMigrationState.UNINITIALIZED
        CoreLogger.i(
            tag = PHOTO,
            message = "PhotoShareMigrationManager initialized state = $initializedState",
        )
        if (initializedState == PhotoShareMigrationState.COMPLETE) {
            CoreLogger.d(
                tag = PHOTO,
                message = "PhotoShareMigrationManager initialize not needed as migration is complete",
            )
        } else {
            val hasPhotoVolume = hasPhotoVolume(userId).firstOrNull() ?: false
            if (hasPhotoVolume && initializedState == PhotoShareMigrationState.UNINITIALIZED) {
                CoreLogger.d(PHOTO, "PhotoShareMigrationManager initialize skipped as photo volume already exists")
            } else {
                CoreLogger.d(PHOTO, "PhotoShareMigrationManager initializing")
                fetchingJob = combine(
                    tickerFlow,
                    inForeground,
                ) { _, inFg ->
                    if (inFg) {
                        fetchMigrationStatus(userId)
                            .onSuccess { remoteMigrationStatus ->
                                handleRemoteMigrationStatus(userId, remoteMigrationStatus)
                            }
                            .onFailure { error ->
                                error.log(PHOTO, "Failed to fetch migration status")
                            }
                    }
                }.launchIn(coroutineScope)

                state.onEach { migrationState ->
                    when (migrationState) {
                        PhotoShareMigrationState.CLEANUP -> onCleanup(userId)
                        PhotoShareMigrationState.COMPLETE -> onComplete()
                        else -> Unit
                    }
                }.launchIn(coroutineScope)
            }
        }
    }

    suspend fun start(userId: UserId): Result<Unit> = coRunCatching {
        CoreLogger.i(PHOTO, "Start photo share migration")
        coRunCatching { photoShareMigrationRepository.startMigration(userId) }
            .recoverCatching { error ->
                error.onProtonHttpException { protonData ->
                    when (protonData.code) {
                        ProtonApiCode.ALREADY_EXISTS -> handleRemoteMigrationStatus(
                            userId = userId,
                            status = RemoteMigrationStatus.InProgress,
                        )
                        ProtonApiCode.NOT_EXISTS -> handleRemoteMigrationStatus(
                            userId = userId,
                            status = RemoteMigrationStatus.NotNeeded,
                        )
                        else -> throw error
                    }
                }
            }
            .onSuccess {
                handleRemoteMigrationStatus(
                    userId = userId,
                    status = RemoteMigrationStatus.InProgress,
                )
            }
            .getOrThrow()
    }

    private suspend fun fetchMigrationStatus(userId: UserId): Result<RemoteMigrationStatus> {
        val result = coRunCatching {
            photoShareMigrationRepository.fetchMigrationStatus(userId)
        }
        return if (result.isSuccess) {
            val (code, newVolumeId) = result.getOrThrow()
            when (code) {
                ProtonApiCode.SUCCESS -> if (newVolumeId == null) {
                    Result.success(RemoteMigrationStatus.Pending)
                } else {
                    Result.success(RemoteMigrationStatus.Complete(newVolumeId))
                }

                ProtonApiCode.SUCCESS_ACCEPTED -> Result.success(RemoteMigrationStatus.InProgress)
                else -> Result.failure(IllegalStateException("Unexpected code $code"))
            }
        } else {
            val error = requireNotNull(result.exceptionOrNull())
            error.onProtonHttpException { protonData ->
                if (protonData.code == ProtonApiCode.NOT_EXISTS) {
                    Result.success(RemoteMigrationStatus.NotNeeded)
                } else {
                    Result.success(RemoteMigrationStatus.Failed(error))
                }
            } ?: Result.failure(error)
        }
    }

    private suspend fun handleRemoteMigrationStatus(
        userId: UserId,
        status: RemoteMigrationStatus,
    ) = coRunCatching {
        mutex.withLock {
            CoreLogger.i(
                tag = PHOTO,
                message = "RemoteMigrationStatus = ${status}, state = ${state.value.name}, albumsFeatureOn = ${albumsFeatureOn.value}"
            )
            if (albumsFeatureOn.value) {
                when (status) {
                    is RemoteMigrationStatus.Failed -> Unit
                    RemoteMigrationStatus.NotNeeded -> onMigrationNotNeeded(userId)
                    RemoteMigrationStatus.Pending -> onMigrationPending(userId)
                    RemoteMigrationStatus.InProgress -> onMigrationInProgress(userId)
                    is RemoteMigrationStatus.Complete -> onMigrationComplete(
                        userId,
                        status.newVolumeId
                    )
                }
            }
        }
    }

    private suspend fun onMigrationNotNeeded(userId: UserId) {
        when (state.value) {
            PhotoShareMigrationState.IN_PROGRESS,
            PhotoShareMigrationState.PENDING,
            PhotoShareMigrationState.UNINITIALIZED -> setState(
                userId = userId,
                state = PhotoShareMigrationState.COMPLETE,
            )
            else -> Unit
        }
    }

    private suspend fun onMigrationPending(userId: UserId) {
        when (state.value) {
            PhotoShareMigrationState.IN_PROGRESS,
            PhotoShareMigrationState.UNINITIALIZED -> setState(userId, PhotoShareMigrationState.PENDING)
            else -> Unit
        }
    }

    private suspend fun onMigrationInProgress(userId: UserId) {
        when (state.value) {
            PhotoShareMigrationState.PENDING,
            PhotoShareMigrationState.UNINITIALIZED -> setState(userId, PhotoShareMigrationState.IN_PROGRESS)
            else -> Unit
        }
    }

    private suspend fun onMigrationComplete(userId: UserId, volumeId: VolumeId) {
        getVolume(userId, volumeId).toResult().getOrNull()
        when (state.value) {
            PhotoShareMigrationState.PENDING,
            PhotoShareMigrationState.IN_PROGRESS,
            PhotoShareMigrationState.UNINITIALIZED -> setState(userId, PhotoShareMigrationState.CLEANUP)
            else -> Unit
        }
    }

    private suspend fun onCleanup(userId: UserId) {
        photoShareCleanup(userId)
            .getOrNull(
                tag = PHOTO,
                message = "Failed to cleanup photo share",
            )
        onCleanupComplete(userId)
    }

    private suspend fun onCleanupComplete(userId: UserId) {
        when (state.value) {
            PhotoShareMigrationState.CLEANUP -> setState(userId, PhotoShareMigrationState.COMPLETE)
            else -> Unit
        }
    }

    private fun onComplete() {
        fetchingJob?.cancel()
        fetchingJob = null
    }

    private suspend fun setState(userId: UserId, state: PhotoShareMigrationState) {
        photoShareMigrationRepository.setMigrationState(userId, state)
        CoreLogger.i(PHOTO, "Setting photo migration state to $state")
    }

    enum class MigrationStatus {
        INITIALIZING, // inconclusive, UI should behave as there is no change on photo share
        PENDING, // migration is needed, but user did not yet initiate migration
        IN_PROGRESS, // migration is in progress, UI should block any photo share operations
        COMPLETE, // migration is complete, Albums are now fully available
    }

    sealed class RemoteMigrationStatus {
        data class Failed(val error: Throwable) : RemoteMigrationStatus()
        data object NotNeeded : RemoteMigrationStatus()
        data object Pending : RemoteMigrationStatus()
        data object InProgress : RemoteMigrationStatus()
        data class Complete(val newVolumeId: VolumeId) : RemoteMigrationStatus()
    }
}
