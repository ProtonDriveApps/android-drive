/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.drivelink.shared.domain.usecase.GetPagedSharedByMeLinkIds
import me.proton.core.drive.drivelink.shared.domain.usecase.SharedDriveLinks
import me.proton.core.drive.drivelink.shared.presentation.entity.SharedItem
import me.proton.core.drive.files.domain.usecase.ToFirstItemMetricsNotifier
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.PageType
import me.proton.core.drive.share.crypto.domain.usecase.GetOrCreateMainShare
import me.proton.core.drive.share.user.domain.usecase.GetAllSharedByMeIds
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SharedByMeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext appContext: Context,
    configurationProvider: ConfigurationProvider,
    sharedDriveLinks: SharedDriveLinks,
    getMainShare: GetOrCreateMainShare,
    toFirstItemMetricsNotifier: ToFirstItemMetricsNotifier,
    private val getPagedSharedByMeLinkIds: GetPagedSharedByMeLinkIds,
    private val getAllSharedByMeIds: GetAllSharedByMeIds,
) : CommonSharedViewModel(
    savedStateHandle = savedStateHandle,
    appContext = appContext,
    configurationProvider = configurationProvider,
    sharedDriveLinks = sharedDriveLinks,
    toFirstItemMetricsNotifier = toFirstItemMetricsNotifier,
) {
    private val emptyStateImageResId: Int = getThemeDrawableId(
        light = BasePresentation.drawable.empty_shared_by_me_light,
        dark = BasePresentation.drawable.empty_shared_by_me_dark,
        dayNight = BasePresentation.drawable.empty_shared_by_me_daynight,
    )
    override val emptyState: ListContentState.Empty = ListContentState.Empty(
        imageResId = emptyStateImageResId,
        titleId = I18N.string.shared_by_me_empty_title,
        descriptionResId = I18N.string.shared_by_me_empty_description,
        actionResId = null,
    )
    private val volumeId: StateFlow<VolumeId?> = getMainShare(userId)
        .mapSuccessValueOrNull()
        .distinctUntilChanged()
        .map { share -> share?.volumeId }
        .stateIn(viewModelScope, Eagerly, null)
    private val _unused = flowOf {
        toFirstItemMetricsNotifier.toFirstItemStart(
            userId = userId,
            pageType = getPageType(),
            startTime = TimestampMs(SystemClock.elapsedRealtime()),
        )
    }.stateIn(viewModelScope, Eagerly, Unit)
    override val driveLinks: Flow<PagingData<SharedItem>> = volumeId
        .filterNotNull()
        .distinctUntilChanged()
        .transformLatest { volumeId ->
            emitAll(
                getPagedSharedByMeLinkIds(userId, volumeId)
                    .map { pagingData ->
                        pagingData.map { sharedLinkId ->
                            SharedItem.Listing(
                                linkId = sharedLinkId.linkId,
                            ) as SharedItem
                        }
                    }
            )
        }.cachedIn(viewModelScope)

    override suspend fun getAllIds() = getAllSharedByMeIds(userId)

    override fun getPageType(): PageType = PageType.shared_by_me
}
