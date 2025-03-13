/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.ui.views

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.icy.twentyfour.R
import me.icy.twentyfour.models.ActivityTab
import me.icy.twentyfour.models.Album
import me.icy.twentyfour.models.Artist
import me.icy.twentyfour.models.Audio
import me.icy.twentyfour.models.Genre
import me.icy.twentyfour.models.MediaItem
import me.icy.twentyfour.models.Playlist
import me.icy.twentyfour.models.areContentsTheSame
import me.icy.twentyfour.models.areItemsTheSame
import me.icy.twentyfour.ui.recyclerview.SimpleListAdapter

class ActivityTabView(context: Context) : FrameLayout(context) {
    // Views
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recyclerView) }
    private val titleTextView by lazy { findViewById<TextView>(R.id.titleTextView) }

    // RecyclerView
    private val adapter = object : SimpleListAdapter<MediaItem<*>, HorizontalMediaItemView>(
        mediaItemDiffCallback,
        ::HorizontalMediaItemView,
    ) {
        override fun ViewHolder.onPrepareView() {
            view.setOnClickListener {
                item?.let {
                    onItemClickListener(it)
                }
            }
            view.setOnLongClickListener {
                item?.let {
                    onItemLongClickListener(it)
                } ?: false
            }
        }

        override fun ViewHolder.onBindView(item: MediaItem<*>) {
            view.setItem(item)
        }
    }

    // Callbacks
    private var onItemClickListener: (item: MediaItem<*>) -> Unit = { _ -> }
    private var onItemLongClickListener: (item: MediaItem<*>) -> Boolean = { _ -> false }

    init {
        inflate(context, R.layout.view_activity_tab, this)

        recyclerView.adapter = adapter
    }

    fun setOnItemClickListener(listener: ((item: MediaItem<*>) -> Unit)?) {
        onItemClickListener = listener ?: {}
    }

    fun setOnItemLongClickListener(
        listener: ((item: MediaItem<*>) -> Boolean)?
    ) {
        onItemLongClickListener = listener ?: { false }
    }

    fun setActivityTab(activityTab: ActivityTab) {
        titleTextView.text = activityTab.title.getString(resources)

        adapter.submitList(activityTab.items)
        recyclerView.isVisible = activityTab.items.isNotEmpty()
    }

    companion object {
        private val mediaItemDiffCallback = object : DiffUtil.ItemCallback<MediaItem<*>>() {
            override fun areItemsTheSame(
                oldItem: MediaItem<*>,
                newItem: MediaItem<*>,
            ) = when (oldItem) {
                is Album -> oldItem.areItemsTheSame<Album>(newItem)
                is Artist -> oldItem.areItemsTheSame<Artist>(newItem)
                is Audio -> oldItem.areItemsTheSame<Audio>(newItem)
                is Genre -> oldItem.areItemsTheSame<Genre>(newItem)
                is Playlist -> oldItem.areItemsTheSame<Playlist>(newItem)
            }

            override fun areContentsTheSame(
                oldItem: MediaItem<*>,
                newItem: MediaItem<*>,
            ) = when (oldItem) {
                is Album -> oldItem.areContentsTheSame<Album>(newItem)
                is Artist -> oldItem.areContentsTheSame<Artist>(newItem)
                is Audio -> oldItem.areContentsTheSame<Audio>(newItem)
                is Genre -> oldItem.areContentsTheSame<Genre>(newItem)
                is Playlist -> oldItem.areContentsTheSame<Playlist>(newItem)
            }
        }
    }
}
