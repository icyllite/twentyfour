/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.recyclerview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * A very basic ListAdapter that holds only one type of item.
 * @param diffCallback A [DiffUtil.ItemCallback] provided by the derived class
 * @param factory The factory of the [View]
 */
abstract class SimpleListAdapter<T, V : View>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val factory: (Context) -> V,
) : ListAdapter<T, SimpleListAdapter<T, V>.ViewHolder>(diffCallback) {
    private val differ = ListAdapter::class.java.getDeclaredField("mDiffer").apply {
        isAccessible = true
    }.get(this) as AsyncListDiffer<*>

    abstract fun ViewHolder.onBindView(item: T)

    open fun ViewHolder.onPrepareView() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        factory(parent.context),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setListWithoutDiffing(list: List<T>) {
        setOf("mList", "mReadOnlyList").forEach { fieldName ->
            differ::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(differ, list)
            }
        }
    }

    inner class ViewHolder(val view: V) : RecyclerView.ViewHolder(view) {
        var item: T? = null

        init {
            onPrepareView()
        }

        fun bind(item: T) {
            this.item = item

            onBindView(item)
        }
    }
}
