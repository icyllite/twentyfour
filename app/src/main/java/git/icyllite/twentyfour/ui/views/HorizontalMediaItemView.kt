/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import git.icyllite.twentyfour.R

class HorizontalMediaItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : BaseMediaItemView(context, attrs, defStyleAttr, R.layout.horizontal_media_item_view)
