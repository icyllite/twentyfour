/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.fragments

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
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.ext.getViewProperty
import git.icyllite.twentyfour.ext.navigateSafe
import git.icyllite.twentyfour.ext.setProgressCompat
import git.icyllite.twentyfour.models.Artist
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.SortingStrategy
import git.icyllite.twentyfour.ui.recyclerview.SimpleListAdapter
import git.icyllite.twentyfour.ui.recyclerview.UniqueItemDiffCallback
import git.icyllite.twentyfour.ui.views.ListItem
import git.icyllite.twentyfour.ui.views.SortingChip
import git.icyllite.twentyfour.utils.PermissionsChecker
import git.icyllite.twentyfour.utils.PermissionsUtils
import git.icyllite.twentyfour.viewmodels.ArtistsViewModel

/**
 * View all music artists.
 */
class ArtistsFragment : Fragment(R.layout.fragment_artists) {
    // View models
    private val viewModel by viewModels<ArtistsViewModel>()

    // Views
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val sortingChip by getViewProperty<SortingChip>(R.id.sortingChip)

    // Recyclerview
    private val adapter by lazy {
        object : SimpleListAdapter<Artist, ListItem>(
            UniqueItemDiffCallback(),
            ::ListItem,
        ) {
            override fun ViewHolder.onPrepareView() {
                view.setLeadingIconImage(R.drawable.ic_person)
            }

            override fun ViewHolder.onBindView(item: Artist) {
                view.setOnClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_artist,
                        ArtistFragment.createBundle(item.uri)
                    )
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }

                item.name?.also {
                    view.headlineText = it
                } ?: view.setHeadlineText(R.string.unknown)
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
                SortingStrategy.MODIFICATION_DATE to R.string.sort_by_last_activity,
                SortingStrategy.NAME to R.string.sort_by_name,
                SortingStrategy.PLAY_COUNT to R.string.sort_by_play_count,
            )
        )
        sortingChip.setOnSortingRuleSelectedListener {
            viewModel.setSortingRule(it)
        }

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

        super.onDestroyView()
    }

    private suspend fun loadData() {
        coroutineScope {
            launch {
                viewModel.artists.collectLatest {
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
                                "Failed to load artists, error: ${it.error}",
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
        private val LOG_TAG = ArtistsFragment::class.simpleName!!
    }
}
