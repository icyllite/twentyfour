/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.icy.twentyfour.R
import me.icy.twentyfour.ext.getViewProperty
import me.icy.twentyfour.ext.navigateSafe
import me.icy.twentyfour.ext.setProgressCompat
import me.icy.twentyfour.models.Album
import me.icy.twentyfour.models.FlowResult
import me.icy.twentyfour.models.SortingStrategy
import me.icy.twentyfour.ui.recyclerview.DisplayAwareGridLayoutManager
import me.icy.twentyfour.ui.recyclerview.SimpleListAdapter
import me.icy.twentyfour.ui.recyclerview.UniqueItemDiffCallback
import me.icy.twentyfour.ui.views.MediaItemGridItem
import me.icy.twentyfour.ui.views.SortingChip
import me.icy.twentyfour.utils.PermissionsChecker
import me.icy.twentyfour.utils.PermissionsUtils
import me.icy.twentyfour.viewmodels.AlbumsViewModel

/**
 * View all music albums.
 */
class AlbumsFragment : Fragment(R.layout.fragment_albums) {
    // View models
    private val viewModel by viewModels<AlbumsViewModel>()

    // Views
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val sortingChip by getViewProperty<SortingChip>(R.id.sortingChip)

    // Recyclerview
    private val adapter by lazy {
        object : SimpleListAdapter<Album, MediaItemGridItem>(
            UniqueItemDiffCallback(),
            ::MediaItemGridItem,
        ) {
            override fun ViewHolder.onBindView(item: Album) {
                view.setOnClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_album,
                        AlbumFragment.createBundle(item.uri)
                    )
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }

                view.setItem(item)
            }
        }
    }

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sortingChip.setSortingStrategies(
            sortedMapOf(
                SortingStrategy.ARTIST_NAME to R.string.sort_by_artist_name,
                SortingStrategy.CREATION_DATE to R.string.sort_by_release_date,
                SortingStrategy.NAME to R.string.sort_by_title,
                SortingStrategy.PLAY_COUNT to R.string.sort_by_play_count,
            )
        )
        sortingChip.setOnSortingRuleSelectedListener {
            viewModel.setSortingRule(it)
        }

        recyclerView.layoutManager = DisplayAwareGridLayoutManager(recyclerView.context, 2)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        recyclerView.layoutManager = null

        super.onDestroyView()
    }

    private suspend fun loadData() {
        coroutineScope {
            launch {
                viewModel.albums.collectLatest {
                    linearProgressIndicator.setProgressCompat(it)

                    when (it) {
                        is FlowResult.Loading -> {
                            // Do nothing
                        }

                        is FlowResult.Success -> {
                            adapter.submitList(it.data)

                            val isEmpty = it.data.isEmpty()
                            recyclerView.isVisible = !isEmpty
                            noElementsLinearLayout.isVisible = isEmpty
                        }

                        is FlowResult.Error -> {
                            Log.e(
                                LOG_TAG,
                                "Failed to load albums, error: ${it.error}",
                                it.throwable
                            )

                            adapter.submitList(emptyList())

                            recyclerView.isVisible = false
                            noElementsLinearLayout.isVisible = true
                        }
                    }
                }
            }

            launch {
                viewModel.sortingRule.collectLatest {
                    sortingChip.setSortingRule(it)
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = AlbumsFragment::class.simpleName!!
    }
}
