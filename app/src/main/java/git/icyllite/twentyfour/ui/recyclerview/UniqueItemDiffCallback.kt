/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.ui.recyclerview

import androidx.recyclerview.widget.DiffUtil
import git.icyllite.twentyfour.models.UniqueItem

class UniqueItemDiffCallback<T : UniqueItem<T>> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem.areItemsTheSame(newItem)

    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem.areContentsTheSame(newItem)
}
