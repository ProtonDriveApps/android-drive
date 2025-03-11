/*
 * Copyright (c) 2023 Proton AG.
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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.drivelink.selection.domain.usecase.SelectAll
import me.proton.core.drive.i18n.R
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
open class SelectionViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val selectLinks: SelectLinks,
    private val deselectLinks: DeselectLinks,
    private val selectAll: SelectAll,
    private val getSelectedDriveLinks: GetSelectedDriveLinks,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    protected open val driveLinkFilter: (DriveLink) -> Boolean = { true }

    protected val selectionId = MutableStateFlow(
        savedStateHandle.get<String?>(KEY_SELECTION_ID)?.let { SelectionId(it) }
    )
    protected val parentFolderId = MutableStateFlow<FolderId?>(null)
    protected val selected: StateFlow<Set<LinkId>> = selectionId
        .filterNotNull()
        .transformLatest { id ->
            val parentId = parentFolderId.filterNotNull().first()
            emitAll(
                getSelectedDriveLinks(id, parentId).map { driveLinks ->
                    driveLinks.map { driveLink -> driveLink.id }.toSet()
                }
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    protected val selectAllAction = Action(
        iconResId = CorePresentation.drawable.ic_proton_check_triple,
        contentDescriptionResId = I18N.string.content_description_select_all,
        onAction = {
            viewModelScope.launch {
                selectAll(
                    parentId = parentFolderId.filterNotNull().first(),
                    selectionId = selectionId.value,
                    driveLinkFilter = driveLinkFilter,
                )
            }
        }
    )

    override fun onCleared() {
        super.onCleared()
        selectionId.value?.let { selectionId ->
            CoroutineScope(Dispatchers.Main).launch {
                deselectLinks(selectionId)
            }
        }
    }

    protected fun selectedOptionsAction(onAction: (() -> Unit)?) = Action(
        iconResId = CorePresentation.drawable.ic_proton_three_dots_vertical,
        contentDescriptionResId = R.string.content_description_selected_options,
        onAction = { onAction?.invoke() }
    )

    protected fun onTopAppBarNavigation(nonSelectedBlock: () -> Unit): () -> Unit = {
        Unit.also {
            if (selected.value.isNotEmpty()) {
                selectionId.value?.let { viewModelScope.launch { deselectLinks(it) } }
            } else {
                nonSelectedBlock()
            }
        }
    }

    protected fun onDriveLink(driveLink: DriveLink, nonSelectedBlock: () -> Unit) {
        if (selected.value.isNotEmpty()) {
            if (selected.value.contains(driveLink.id)) {
                removeSelected(listOf(driveLink.id))
            } else {
                addSelected(listOf(driveLink.id))
            }
        } else {
            nonSelectedBlock()
        }
    }

    protected inline fun <reified T : LinkId> onSelectedOptions(
        navigateToFileOrFolderOptions: (linkId: T) -> Unit,
        navigateToMultipleFileOrFolderOptions: (selectionId: SelectionId) -> Unit,
    ) {
        if (selected.value.size == 1) {
            navigateToFileOrFolderOptions(selected.value.first() as T)
        } else {
            selectionId.value?.let { selectionId -> navigateToMultipleFileOrFolderOptions(selectionId) }
        }
    }

    protected fun onSelectDriveLink(driveLink: DriveLink) = addSelected(listOf(driveLink.id))

    protected fun onDeselectDriveLink(driveLink: DriveLink) = removeSelected(listOf(driveLink.id))

    protected fun onBack() { removeAllSelected() }

    private fun addSelected(linkIds: List<LinkId>) {
        viewModelScope.launch {
            selectionId.value?.let { selectionId ->
                selectLinks(selectionId, linkIds)
            } ?: setSelectionId(selectLinks(linkIds).getOrNull())
        }
    }

    private fun removeSelected(linkIds: List<LinkId>) {
        viewModelScope.launch {
            selectionId.value?.let { selectionId ->
                deselectLinks(selectionId, linkIds)
            }
        }
    }

    private fun removeAllSelected() {
        if (selected.value.isNotEmpty()) {
            viewModelScope.launch {
                selectionId.value?.let { selectionId -> deselectLinks(selectionId) }
            }
        }
    }

    private fun setSelectionId(selectionId: SelectionId?) {
        this.selectionId.value = selectionId
        savedStateHandle[KEY_SELECTION_ID] = selectionId?.id
    }

    companion object {
        private const val KEY_SELECTION_ID = "key.selectionId"
    }
}
