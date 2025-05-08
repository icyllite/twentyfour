/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.subsonic.models

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class ArtistID3(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val artistImageUrl: UriAsString? = null,
    val albumCount: Int? = null,
    val starred: InstantAsString? = null,

    // OpenSubsonic
    val musicBrainzId: String? = null,
    val sortName: String? = null,
    val roles: List<String>? = null,

    // Navidrome
    val userRating: Int? = null,
)
