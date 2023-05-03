/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.files.presentation.entry

import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

interface OptionEntry<in T : Any> {
    val icon: Int
    val label: Int
    val onClick: (T) -> Unit
}

class DownloadEntry(
    override val onClick: (Unit) -> Unit,
) : OptionEntry<Unit> {
    override val icon: Int = CorePresentation.drawable.ic_proton_arrow_down_line
    override val label: Int = I18N.string.common_download
}

class MoveEntry(
    override val onClick: (Unit) -> Unit,
) : OptionEntry<Unit> {
    override val icon: Int = CorePresentation.drawable.ic_proton_arrows_cross
    override val label: Int = I18N.string.files_move_file_action
}

class TrashEntry(
    override val onClick: (Unit) -> Unit,
) : OptionEntry<Unit> {
    override val icon: Int = CorePresentation.drawable.ic_proton_trash
    override val label: Int = I18N.string.files_send_to_trash_action
}
