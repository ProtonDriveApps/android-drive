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

package me.proton.core.drive.db.test

import me.proton.core.drive.linkdownload.data.db.entity.DownloadBlockEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadState
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadStateEntity

data class DownloadContext(val fileContext: FileContext) : BaseContext()

suspend fun FileContext.download(
    state: LinkDownloadState = LinkDownloadState.DOWNLOADED,
    block: suspend DownloadContext.() -> Unit = {}
) {
    db.linkDownloadDao.insertOrIgnore(
        LinkDownloadStateEntity(
            userId = user.userId,
            shareId = share.id,
            linkId = link.id,
            revisionId = revisionId,
            state = state,
            manifestSignature = """
                -----BEGIN PGP SIGNATURE-----
                Version: ProtonMail

                wnUEABYKACcFAmbYVSMJEO2DvT2OWfEwFiEEq1golLRLwjzLn0Sk7YO9PY5Z
                8TAAAE0rAP483nBGOiZewbcyGwy10rUxKbHc65Lj3UOHR4b0Zaa1wAD/ejxR
                hniVi/wqf2eZe5UcOoUQX2LMxzw3g6r0Pszo1A4=
                =XsNv
                -----END PGP SIGNATURE-----

            """.trimIndent(),
            signatureAddress = account.email,
        )
    )
    DownloadContext(this).block()
}

suspend fun DownloadContext.block(index: Long) {
    with(fileContext) {
        db.linkDownloadDao.insertOrIgnore(
            DownloadBlockEntity(
                userId = user.userId,
                shareId = share.id,
                linkId = link.id,
                revisionId = revisionId,
                index = index,
                uri = "/data/user/0/me.proton.android.drive.dev/files/${user.userId.id}/${volume.id}/${revisionId.take(2)}/${revisionId.substring(2)}/$index",
                encryptedSignature = """
                    -----BEGIN PGP MESSAGE-----
                    Version: ProtonMail

                    wV4DAwrs8Q3Z0I4SAQdALT2+XUPDXMClHPIq2FOCprLHyv2Jn655jmbYzOcq
                    QxUwxbkcowpauPhb/KKTH5mCvMPNlMQWzYnNmBVvoW3+CSucJZ954/zvkUhD
                    l3mZ/IiQ0qgBQROHhb4QuWNTN4g2sALhgOrayj7vgGpcQtY+Gx6nh9/VGh2R
                    5SkHsMmaEG2++lxnoQSS4wBLTAl0sCorx2/pOgCwVdkBTF9W2A6K2y7XoNuZ
                    qoG2njtoykADRXNY/unqTIZUCVoSfaR4B4K4wbd/zjzv4djaxpcxpPePqhnY
                    el6oj4CY9TAmJzKRXKPYYW+6oKDPdwutFienpaoS6WEfVeJmOBlqPPc=
                    =4ovv
                    -----END PGP MESSAGE-----
                """.trimIndent()
            )
        )
    }
}
