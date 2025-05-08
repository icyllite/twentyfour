/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.subsonic.models

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class SearchResult3(
    val artist: List<ArtistID3>? = null,
    val album: List<AlbumID3>? = null,
    val song: List<Child>? = null,
)
