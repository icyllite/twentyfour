/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.ext.getViewProperty
import git.icyllite.twentyfour.ext.navigateSafe
import git.icyllite.twentyfour.ext.setProgressCompat
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.Playlist
import git.icyllite.twentyfour.models.SortingStrategy
import git.icyllite.twentyfour.ui.recyclerview.SimpleListAdapter
import git.icyllite.twentyfour.ui.recyclerview.UniqueItemDiffCallback
import git.icyllite.twentyfour.ui.views.ListItem
import git.icyllite.twentyfour.ui.views.SortingChip
import git.icyllite.twentyfour.utils.PermissionsChecker
import git.icyllite.twentyfour.utils.PermissionsUtils
import git.icyllite.twentyfour.viewmodels.PlaylistsViewModel

/**
 * View all music playlists.
 */
class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {
    // View models
    private val viewModel by viewModels<PlaylistsViewModel>()

    // Views
    private val createNewPlaylistButton by getViewProperty<Button>(R.id.createNewPlaylistButton)
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val sortingChip by getViewProperty<SortingChip>(R.id.sortingChip)

    // Recyclerview
    private val addNewPlaylistItem = Playlist.Builder(Uri.EMPTY).build()
    private val adapter = object : SimpleListAdapter<Playlist, ListItem>(
        UniqueItemDiffCallback(),
        ::ListItem,
    ) {
        override fun ViewHolder.onBindView(item: Playlist) {
            when (item === addNewPlaylistItem) {
                true -> {
                    view.setOnClickListener {
                        findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_create_playlist_dialog,
                            CreatePlaylistDialogFragment.createBundle(
                                providerIdentifier = viewModel.navigationProvider.value
                            )
                        )
                    }
                    view.setOnLongClickListener(null)

                    view.setLeadingIconImage(R.drawable.ic_playlist_add)
                    view.setHeadlineText(R.string.create_playlist)
                }

                false -> {
                    view.setOnClickListener {
                        findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_playlist,
                            PlaylistFragment.createBundle(item.uri)
                        )
                    }
                    view.setOnLongClickListener {
                        findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                            MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                        )
                        true
                    }

                    view.setLeadingIconImage(
                        when (item.type) {
                            Playlist.Type.PLAYLIST -> R.drawable.ic_playlist_play
                            Playlist.Type.FAVORITES -> R.drawable.ic_favorite
                        }
                    )
                    view.headlineText = item.name ?: getString(
                        when (item.type) {
                            Playlist.Type.PLAYLIST -> R.string.playlist_unknown
                            Playlist.Type.FAVORITES -> R.string.favorites_playlist
                        }
                    )
                }
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
                SortingStrategy.CREATION_DATE to R.string.sort_by_creation_date,
                SortingStrategy.MODIFICATION_DATE to R.string.sort_by_last_modified,
                SortingStrategy.NAME to R.string.sort_by_name,
                SortingStrategy.PLAY_COUNT to R.string.sort_by_play_count,
            )
        )
        sortingChip.setOnSortingRuleSelectedListener {
            viewModel.setSortingRule(it)
        }

        recyclerView.adapter = adapter

        createNewPlaylistButton.setOnClickListener {
            findNavController().navigateSafe(
                R.id.action_mainFragment_to_fragment_create_playlist_dialog,
                CreatePlaylistDialogFragment.createBundle(
                    providerIdentifier = viewModel.navigationProvider.value
                )
            )
        }

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

        super.onDestroyView()
    }

    private suspend fun loadData() {
        coroutineScope {
            launch {
                viewModel.playlists.collectLatest {
                    linearProgressIndicator.setProgressCompat(it)

                    when (it) {
                        is FlowResult.Loading -> {
                            // Do nothing
                        }

                        is FlowResult.Success -> {
                            val isEmpty = it.data.isEmpty()

                            adapter.submitList(
                                when (isEmpty) {
                                    true -> emptyList()
                                    false -> listOf(
                                        addNewPlaylistItem,
                                        *it.data.toTypedArray(),
                                    )
                                }
                            )

                            recyclerView.isVisible = !isEmpty
                            noElementsLinearLayout.isVisible = isEmpty
                        }

                        is FlowResult.Error -> {
                            Log.e(
                                LOG_TAG,
                                "Failed to load playlists, error: ${it.error}",
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
        private val LOG_TAG = PlaylistsFragment::class.simpleName!!
    }
}
