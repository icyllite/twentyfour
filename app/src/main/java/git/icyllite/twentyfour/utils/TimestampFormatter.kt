/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.utils

import java.util.Locale

object TimestampFormatter {
    fun formatTimestampSecs(timestampSecs: Long): String {
        val minutes = timestampSecs / 60
        val seconds = timestampSecs % 60
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }

    fun formatTimestampSecs(
        timestampSecs: Number
    ) = formatTimestampSecs(timestampSecs.toLong())

    fun formatTimestampMillis(
        timestampMillis: Number
    ) = formatTimestampSecs(timestampMillis.toLong() / 1000)
}
