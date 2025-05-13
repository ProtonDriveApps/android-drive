/*
 * Copyright (c) 2023-2024 Proton AG.
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
package me.proton.core.drive.trash.data.test.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.onSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.eventmanager.usecase.HandleOnDeleteEvent
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.trash.data.manager.worker.EmptyTrashSuccessWorker
import me.proton.core.drive.trash.data.manager.worker.EmptyTrashWorker
import me.proton.core.drive.trash.data.manager.worker.PermanentlyDeleteFileNodesWorker
import me.proton.core.drive.trash.data.manager.worker.RestoreFileNodesWorker
import me.proton.core.drive.trash.data.manager.worker.TrashFileNodesWorker
import me.proton.core.drive.trash.domain.TrashManager
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("LongParameterList")
class StubbedTrashManager @Inject constructor(
    private val driveTrashRepository: DriveTrashRepository,
    private val linkTrashRepository: LinkTrashRepository,
    private val broadcastMessages: BroadcastMessages,
    private val updateEventAction: UpdateEventAction,
    private val linkRepository: LinkRepository,
    private val handleOnDeleteEvent: HandleOnDeleteEvent,
    private val getShare: GetShare,
    private val getMainShare: GetMainShare,
) : TrashManager {
    private val context by lazy { ApplicationProvider.getApplicationContext<Context>() }

    private val emptyTrashState = MutableStateFlow(false)

    override suspend fun trash(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<LinkId>,
    ): DataResult<String> = linkTrashRepository.insertWork(linkIds).onSuccess { workId ->
        TestListenableWorkerBuilder<TrashFileNodesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = TrashFileNodesWorker(
                    driveTrashRepository = driveTrashRepository,
                    linkTrashRepository = linkTrashRepository,
                    broadcastMessages = broadcastMessages,
                    updateEventAction = updateEventAction,
                    linkRepository = linkRepository,
                    handleOnDeleteEvent = handleOnDeleteEvent,
                    appContext = appContext,
                    params = workerParameters,
                )

            })
            .setInputData(
                TrashFileNodesWorker.workDataOf(userId, volumeId, workId)
            )
            .build()
            .doWork()
    }

    override suspend fun restore(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<LinkId>,
    ): DataResult<String> = linkTrashRepository.insertWork(linkIds).onSuccess { workId ->
        TestListenableWorkerBuilder<RestoreFileNodesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = RestoreFileNodesWorker(
                    driveTrashRepository = driveTrashRepository,
                    linkTrashRepository = linkTrashRepository,
                    broadcastMessages = broadcastMessages,
                    updateEventAction = updateEventAction,
                    appContext = appContext,
                    params = workerParameters,
                )

            })
            .setInputData(
                RestoreFileNodesWorker.workDataOf(userId, volumeId, workId)
            )
            .build()
            .doWork()

    }

    override suspend fun delete(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<LinkId>,
    ): DataResult<String> = linkTrashRepository.insertWork(linkIds).onSuccess { workId ->
        TestListenableWorkerBuilder<PermanentlyDeleteFileNodesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = PermanentlyDeleteFileNodesWorker(
                    driveTrashRepository = driveTrashRepository,
                    linkRepository = linkRepository,
                    trashRepository = linkTrashRepository,
                    broadcastMessages = broadcastMessages,
                    updateEventAction = updateEventAction,
                    linkTrashRepository = linkTrashRepository,
                    appContext = appContext,
                    params = workerParameters,
                )

            })
            .setInputData(
                PermanentlyDeleteFileNodesWorker.workDataOf(userId, volumeId, workId)
            )
            .build()
            .doWork()
    }

    override suspend fun emptyTrash(
        userId: UserId,
        volumeId: VolumeId,
    ) {
        emptyTrashState.value = true
        val result = TestListenableWorkerBuilder<EmptyTrashWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = EmptyTrashWorker(
                    appContext = appContext,
                    params = workerParameters,
                    driveTrashRepository = driveTrashRepository,
                    broadcastMessages = broadcastMessages,
                    updateEventAction = updateEventAction,
                    getMainShare = getMainShare,
                )

            })
            .setInputData(
                EmptyTrashWorker.workDataOf(userId, volumeId)
            )
            .build()
            .doWork()
        if (result == ListenableWorker.Result.success()) {
            TestListenableWorkerBuilder<EmptyTrashSuccessWorker>(context)
                .setWorkerFactory(object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = EmptyTrashSuccessWorker(
                        appContext = appContext,
                        params = workerParameters,
                        broadcastMessages = broadcastMessages,
                    )

                })
                .setInputData(
                    EmptyTrashSuccessWorker.workDataOf(userId)
                )
                .build()
                .doWork()
        }
        emptyTrashState.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getEmptyTrashState(
        userId: UserId,
        volumeId: VolumeId,
    ): Flow<TrashManager.EmptyTrashState> {
        return emptyTrashState.transformLatest { trashing ->
            if (trashing) {
                emit(TrashManager.EmptyTrashState.TRASHING)
            } else {
                emitAll(
                    linkTrashRepository.hasTrashContent(userId, volumeId).map { hasTrashContent ->
                        if (hasTrashContent) {
                            TrashManager.EmptyTrashState.INACTIVE
                        } else {
                            TrashManager.EmptyTrashState.NO_FILES_TO_TRASH
                        }
                    }
                )
            }
        }
    }
}
