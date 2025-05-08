/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.ui.coil

import coil3.map.Mapper
import coil3.request.Options
import git.icyllite.twentyfour.models.Thumbnail

object ThumbnailMapper : Mapper<Thumbnail, Any> {
    override fun map(data: Thumbnail, options: Options) = data.bitmap ?: data.uri
}
