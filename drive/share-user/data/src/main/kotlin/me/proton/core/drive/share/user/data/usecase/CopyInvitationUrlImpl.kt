/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.share.user.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.domain.usecase.CopyToClipboard
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.user.domain.usecase.CopyInvitationUrl
import me.proton.core.drive.share.user.domain.usecase.GetInvitationUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class CopyInvitationUrlImpl @Inject constructor(
    private val getInvitationUrl: GetInvitationUrl,
    private val copyToClipboard: CopyToClipboard,
    @ApplicationContext private val appContext: Context,
) : CopyInvitationUrl {
    override suspend operator fun invoke(
        volumeId: VolumeId,
        linkId: LinkId,
        invitationId: String,
    ) = coRunCatching {
        val url = getInvitationUrl(volumeId, linkId, invitationId).getOrThrow()
        copyToClipboard(
            userId = linkId.userId,
            label = appContext.getString(I18N.string.common_link),
            text = url,
        ).getOrThrow()
    }
}
