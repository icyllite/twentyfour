/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import me.icy.twentyfour.R

class MediaItemGridItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : BaseMediaItemView(context, attrs, defStyleAttr, R.layout.item_media_item_grid)
