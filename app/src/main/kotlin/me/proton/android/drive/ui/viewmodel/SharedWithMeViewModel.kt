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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.drivelink.shared.domain.usecase.GetPagedSharedWithMeLinkIds
import me.proton.core.drive.drivelink.shared.domain.usecase.SharedDriveLinks
import me.proton.core.drive.drivelink.shared.presentation.entity.SharedItem
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDisabled
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.usecase.GetAllSharedWithMeIds
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class SharedWithMeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext appContext: Context,
    configurationProvider: ConfigurationProvider,
    sharedDriveLinks: SharedDriveLinks,
    getPagedSharedWithMeLinkIds: GetPagedSharedWithMeLinkIds,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    private val getAllSharedWithMeIds: GetAllSharedWithMeIds,
) : CommonSharedViewModel(
    savedStateHandle = savedStateHandle,
    appContext = appContext,
    configurationProvider = configurationProvider,
    sharedDriveLinks = sharedDriveLinks,
) {
    private val killSwitch = getFeatureFlagFlow(driveSharingDisabled(userId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlag(driveSharingDisabled(userId), NOT_FOUND)
        )
    private val emptyStateImageResId: Int = getThemeDrawableId(
        light = BasePresentation.drawable.empty_shared_with_me_light,
        dark = BasePresentation.drawable.empty_shared_with_me_dark,
        dayNight = BasePresentation.drawable.empty_shared_with_me_daynight,
    )
    override val emptyState: ListContentState.Empty = ListContentState.Empty(
        imageResId = emptyStateImageResId,
        titleId = I18N.string.shared_with_me_empty_title,
        descriptionResId = I18N.string.shared_with_me_empty_description,
        actionResId = null,
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    override val driveLinks: Flow<PagingData<SharedItem>> = killSwitch.transformLatest { featureFlag ->
        if (featureFlag.state == ENABLED) {
            emit(PagingData.empty())
        } else {
            emitAll(
                getPagedSharedWithMeLinkIds(userId)
                    .map { pagingData ->
                        pagingData.map { sharedLinkId ->
                            SharedItem.Listing(
                                linkId = sharedLinkId.linkId,
                            ) as SharedItem
                        }
                    }.cachedIn(viewModelScope)
            )
        }
    }

    override suspend fun getAllIds(): Result<List<SharedLinkId>> = getAllSharedWithMeIds(userId)

}
