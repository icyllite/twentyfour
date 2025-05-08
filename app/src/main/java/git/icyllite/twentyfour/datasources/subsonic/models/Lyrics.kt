/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.subsonic.models

import kotlinx.serialization.Serializable

/**
 * Lyrics.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Lyrics(
    val value: String,
    val artist: String? = null,
    val title: String? = null,
)
