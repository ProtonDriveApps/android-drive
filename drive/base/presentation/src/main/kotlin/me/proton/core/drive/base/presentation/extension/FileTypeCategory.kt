/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.base.presentation.extension

import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Audio
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Calendar
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Doc
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Image
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Keynote
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Numbers
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Pages
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Pdf
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Ppt
import me.proton.core.drive.base.domain.entity.FileTypeCategory.ProtonDoc
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Text
import me.proton.core.drive.base.domain.entity.FileTypeCategory.TrustedKey
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Unknown
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Video
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Xls
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Xml
import me.proton.core.drive.base.domain.entity.FileTypeCategory.Zip
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.i18n.R as I18N

val FileTypeCategory.iconResId: Int
    get() = when (this) {
        Audio -> R.drawable.ic_audio_48
        Calendar -> R.drawable.ic_calendar_48
        Doc -> R.drawable.ic_doc_48
        Image -> R.drawable.ic_image_48
        Keynote -> R.drawable.ic_keynote_48
        Numbers -> R.drawable.ic_numbers_48
        Pages -> R.drawable.ic_pages_48
        Pdf -> R.drawable.ic_pdf_48
        Ppt -> R.drawable.ic_ppt_48
        ProtonDoc -> R.drawable.ic_proton_docs_48
        Text -> R.drawable.ic_text_48
        TrustedKey -> R.drawable.ic_trust_key_48
        Unknown -> R.drawable.ic_unknown_48
        Video -> R.drawable.ic_video_48
        Xls -> R.drawable.ic_xls_48
        Xml -> R.drawable.ic_xml_48
        Zip -> R.drawable.ic_zip_48
    }

val FileTypeCategory.labelResId: Int
    get() = when (this) {
        Audio -> I18N.string.common_type_audio
        Calendar -> I18N.string.common_type_calendar
        Doc -> I18N.string.common_type_doc
        Image -> I18N.string.common_type_image
        Keynote -> I18N.string.common_type_keynote
        Numbers -> I18N.string.common_type_numbers
        Pages -> I18N.string.common_type_pages
        Pdf -> I18N.string.common_type_pdf
        Ppt -> I18N.string.common_type_ppt
        ProtonDoc -> I18N.string.common_type_proton_doc
        Text -> I18N.string.common_type_text
        TrustedKey -> I18N.string.common_type_trustedkey
        Unknown -> I18N.string.common_type_unknown
        Video -> I18N.string.common_type_video
        Xls -> I18N.string.common_type_xls
        Xml -> I18N.string.common_type_xml
        Zip -> I18N.string.common_type_zip
    }
