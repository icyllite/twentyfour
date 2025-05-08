/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.models

import androidx.media3.common.MediaItem

data class QueueItem(
    val mediaItem: MediaItem,
    val isCurrent: Boolean,
) : UniqueItem<QueueItem> {
    override fun areItemsTheSame(other: QueueItem) = mediaItem.mediaId == other.mediaItem.mediaId

    override fun areContentsTheSame(other: QueueItem) =
        mediaItem == other.mediaItem && isCurrent == other.isCurrent
}
