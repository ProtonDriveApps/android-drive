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
package me.proton.core.drive.shareurl.crypto.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.CopyToClipboard
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.shareurl.base.domain.usecase.GetShareUrl
import me.proton.core.drive.shareurl.crypto.domain.usecase.CopyPublicUrl
import me.proton.core.drive.shareurl.crypto.domain.usecase.GetPublicUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class CopyPublicUrlImpl @Inject constructor(
    private val getShareUrl: GetShareUrl,
    private val getLink: GetLink,
    private val getPublicUrl: GetPublicUrl,
    private val copyToClipboard: CopyToClipboard,
    @ApplicationContext private val appContext: Context,
) : CopyPublicUrl {
    override suspend operator fun invoke(volumeId: VolumeId, linkId: LinkId) = coRunCatching {
        val userId = linkId.shareId.userId
        val link = getLink(linkId).toResult().getOrThrow()
        val shareUrlId = requireNotNull(link.shareUrlId) { "ShareUrlId must not be null" }
        val shareUrl = getShareUrl(volumeId, shareUrlId, flowOf { false }).toResult().getOrThrow()
        val publicUrl = getPublicUrl(userId, shareUrl).getOrThrow()
        copyToClipboard(
            userId = userId,
            label = appContext.getString(I18N.string.common_link),
            text = publicUrl,
        ).getOrThrow()
    }
}
