/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.search.SearchView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.SettingsActivity
import git.icyllite.twentyfour.ext.getViewProperty
import git.icyllite.twentyfour.ext.isLandscape
import git.icyllite.twentyfour.ext.navigateSafe
import git.icyllite.twentyfour.ext.scheduleHideSoftInput
import git.icyllite.twentyfour.ext.setProgressCompat
import git.icyllite.twentyfour.ext.updatePadding
import git.icyllite.twentyfour.models.Album
import git.icyllite.twentyfour.models.Artist
import git.icyllite.twentyfour.models.Audio
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.Genre
import git.icyllite.twentyfour.models.MediaItem
import git.icyllite.twentyfour.models.Playlist
import git.icyllite.twentyfour.models.Result
import git.icyllite.twentyfour.models.areContentsTheSame
import git.icyllite.twentyfour.models.areItemsTheSame
import git.icyllite.twentyfour.ui.recyclerview.SimpleListAdapter
import git.icyllite.twentyfour.ui.views.ListItem
import git.icyllite.twentyfour.ui.views.NowPlayingBar
import git.icyllite.twentyfour.viewmodels.NowPlayingViewModel
import git.icyllite.twentyfour.viewmodels.ProvidersViewModel
import git.icyllite.twentyfour.viewmodels.SearchViewModel

/**
 * The home page.
 */
class MainFragment : Fragment(R.layout.fragment_main) {
    // View models
    private val viewModel by viewModels<NowPlayingViewModel>()
    private val providersViewModel by viewModels<ProvidersViewModel>()
    private val searchViewModel by viewModels<SearchViewModel>()

    // Views
    private val navigationBarView by getViewProperty<NavigationBarView>(R.id.navigationBarView)
    private val nowPlayingBar by getViewProperty<NowPlayingBar>(R.id.nowPlayingBar)
    private val providerMaterialButton by getViewProperty<MaterialButton>(R.id.providerMaterialButton)
    private val searchLinearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.searchLinearProgressIndicator)
    private val searchNoElementsLinearLayout by getViewProperty<LinearLayout>(R.id.searchNoElementsLinearLayout)
    private val searchRecyclerView by getViewProperty<RecyclerView>(R.id.searchRecyclerView)
    private val searchView by getViewProperty<SearchView>(R.id.searchView)
    private val settingsMaterialButton by getViewProperty<MaterialButton>(R.id.settingsMaterialButton)
    private val toolbar by getViewProperty<MaterialToolbar>(R.id.toolbar)
    private val viewPager2 by getViewProperty<ViewPager2>(R.id.viewPager2)

    // System services
    private val inputMethodManager: InputMethodManager
        get() = requireContext().getSystemService(InputMethodManager::class.java)

    // ViewPager2
    private val onPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                var offset = 0

                // Search button
                if (position >= 1) {
                    offset += 1
                }

                navigationBarView.menu[position + offset].isChecked = true
            }
        }
    }

    // RecyclerView
    private val searchAdapter by lazy {
        object : SimpleListAdapter<MediaItem<*>, ListItem>(
            searchDiffCallback,
            ::ListItem
        ) {
            override fun ViewHolder.onBindView(item: MediaItem<*>) {
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }

                when (item) {
                    is Album -> {
                        view.setOnClickListener {
                            findNavController().navigateSafe(
                                R.id.action_mainFragment_to_fragment_album,
                                AlbumFragment.createBundle(item.uri)
                            )
                        }

                        view.setTrailingIconImage(R.drawable.ic_album)
                        view.headlineText = item.title
                        view.supportingText = item.artistName
                    }

                    is Artist -> {
                        view.setOnClickListener {
                            findNavController().navigateSafe(
                                R.id.action_mainFragment_to_fragment_artist,
                                ArtistFragment.createBundle(item.uri)
                            )
                        }

                        view.setTrailingIconImage(R.drawable.ic_person)
                        view.headlineText = item.name
                        view.supportingText = null
                    }

                    is Audio -> {
                        view.setOnClickListener {
                            findNavController().navigateSafe(
                                R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                                MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                            )
                        }

                        view.setTrailingIconImage(R.drawable.ic_music_note)
                        view.headlineText = item.title
                        view.supportingText = item.artistName
                    }

                    is Genre -> {
                        view.setOnClickListener {
                            findNavController().navigateSafe(
                                R.id.action_mainFragment_to_fragment_genre,
                                GenreFragment.createBundle(item.uri)
                            )
                        }

                        view.setTrailingIconImage(R.drawable.ic_genres)
                        view.headlineText = item.name
                        view.supportingText = null
                    }

                    is Playlist -> {
                        view.setOnClickListener {
                            findNavController().navigateSafe(
                                R.id.action_mainFragment_to_fragment_playlist,
                                PlaylistFragment.createBundle(item.uri)
                            )
                        }

                        view.setTrailingIconImage(R.drawable.ic_playlist_play)
                        view.headlineText = item.name
                        view.supportingText = null
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            v.updatePadding(
                insets,
                start = !resources.configuration.isLandscape,
                end = true,
            )

            windowInsets
        }

        if (resources.configuration.isLandscape) {
            ViewCompat.setOnApplyWindowInsetsListener(navigationBarView) { v, windowInsets ->
                // This is a navigation rail
                val insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )

                v.updatePadding(
                    insets,
                    start = true,
                    top = true,
                    bottom = true,
                )

                windowInsets
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(viewPager2) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            v.updatePadding(
                insets,
                start = !resources.configuration.isLandscape,
                end = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(searchRecyclerView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                start = true,
                end = true,
                bottom = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(searchNoElementsLinearLayout) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                start = true,
                end = true,
                bottom = true,
            )

            windowInsets
        }

        // On back pressed
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(searchView.isShowing) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    searchView.startBackProgress(backEvent)
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    searchView.updateBackProgress(backEvent)
                }

                override fun handleOnBackPressed() {
                    searchView.handleBackInvoked()
                }

                override fun handleOnBackCancelled() {
                    searchView.cancelBackProgress()
                }
            }.also {
                searchView.addTransitionListener { searchView, _, newState ->
                    val isShowing = newState in listOf(
                        SearchView.TransitionState.SHOWN,
                        SearchView.TransitionState.SHOWING,
                    )

                    it.isEnabled = isShowing

                    // Clear search query if hidden
                    if (!isShowing) {
                        searchView.clearText()
                    }
                }
            }
        )

        toolbar.setupWithNavController(findNavController())

        providerMaterialButton.setOnClickListener {
            findNavController().navigateSafe(
                R.id.action_mainFragment_to_fragment_provider_selector_dialog
            )
        }
        providerMaterialButton.setOnLongClickListener {
            providersViewModel.navigationProvider.value?.let {
                findNavController().navigateSafe(
                    R.id.action_mainFragment_to_fragment_provider_information_bottom_sheet_dialog,
                    ManageProviderFragment.createBundle(providerIdentifier = it),
                )
                true
            } ?: false
        }

        settingsMaterialButton.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
        }

        // View pager
        viewPager2.isUserInputEnabled = false
        viewPager2.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]()
        }
        viewPager2.offscreenPageLimit = fragments.size
        viewPager2.registerOnPageChangeCallback(onPageChangeCallback)

        navigationBarView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.activityFragment -> {
                    viewPager2.currentItem = 0
                    true
                }

                R.id.searchFragment -> {
                    searchView.show()
                    false
                }

                R.id.libraryFragment -> {
                    viewPager2.currentItem = 1
                    true
                }

                else -> false
            }
        }

        // Now playing bar
        nowPlayingBar.setOnPlayPauseClickListener {
            viewModel.togglePlayPause()
        }

        nowPlayingBar.setOnNowPlayingClickListener {
            findNavController().navigateSafe(R.id.action_mainFragment_to_fragment_now_playing)
        }

        // Search
        searchRecyclerView.adapter = searchAdapter

        searchView.editText.addTextChangedListener { text ->
            searchViewModel.setSearchQuery(text.toString())
        }
        searchView.editText.setOnEditorActionListener { _, _, _ ->
            inputMethodManager.scheduleHideSoftInput(searchView.editText, 0)
            searchView.editText.clearFocus()
            searchViewModel.setSearchQuery(searchView.editText.text.toString(), true)
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    providersViewModel.navigationProvider.collectLatest {
                        it?.let {
                            providerMaterialButton.text = it.name
                            providerMaterialButton.setIconResource(it.type.iconDrawableResId)
                        } ?: run {
                            providerMaterialButton.setText(R.string.no_provider)
                            providerMaterialButton.setIconResource(R.drawable.ic_warning)
                        }
                    }
                }

                launch {
                    viewModel.durationCurrentPositionMs.collectLatest {
                        nowPlayingBar.updateDurationCurrentPositionMs(it.first, it.second)
                    }
                }

                launch {
                    viewModel.isPlaying.collectLatest {
                        nowPlayingBar.updateIsPlaying(it)
                    }
                }

                launch {
                    viewModel.mediaItem.collectLatest {
                        nowPlayingBar.updateMediaItem(it)
                    }
                }

                launch {
                    viewModel.mediaMetadata.collectLatest {
                        nowPlayingBar.updateMediaMetadata(it)
                    }
                }

                launch {
                    viewModel.mediaArtwork.collectLatest {
                        when (it) {
                            null -> {
                                // Do nothing
                            }

                            is Result.Success -> {
                                nowPlayingBar.updateMediaArtwork(it.data)
                            }

                            is Result.Error -> {
                                Log.e(
                                    LOG_TAG,
                                    "Error while getting media artwork: ${it.error}",
                                    it.throwable
                                )
                            }
                        }
                    }
                }

                launch {
                    searchViewModel.searchResults.collectLatest {
                        searchLinearProgressIndicator.setProgressCompat(it)

                        when (it) {
                            is FlowResult.Loading -> {
                                // Do nothing
                            }

                            is FlowResult.Success -> {
                                searchAdapter.submitList(it.data)

                                val isEmpty = it.data.isEmpty()
                                searchRecyclerView.isVisible = !isEmpty
                                searchNoElementsLinearLayout.isVisible =
                                    isEmpty && searchView.editText.text.isNotEmpty()
                            }

                            is FlowResult.Error -> {
                                Log.e(
                                    LOG_TAG,
                                    "Failed to load search results, error: ${it.error}",
                                    it.throwable
                                )

                                searchAdapter.submitList(listOf())

                                searchRecyclerView.isVisible = false
                                searchNoElementsLinearLayout.isVisible = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        // View pager
        viewPager2.adapter = null

        // Search
        searchRecyclerView.adapter = null

        super.onDestroyView()
    }

    companion object {
        private val LOG_TAG = MainFragment::class.simpleName!!

        // Keep in sync with the BottomNavigationView menu
        private val fragments = arrayOf(
            { ActivityFragment() },
            { LibraryFragment() },
        )

        private val searchDiffCallback = object : DiffUtil.ItemCallback<MediaItem<*>>() {
            override fun areItemsTheSame(
                oldItem: MediaItem<*>,
                newItem: MediaItem<*>
            ) = when (oldItem) {
                is Album -> oldItem.areItemsTheSame(newItem)
                is Artist -> oldItem.areItemsTheSame(newItem)
                is Audio -> oldItem.areItemsTheSame(newItem)
                is Genre -> oldItem.areItemsTheSame(newItem)
                is Playlist -> oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(
                oldItem: MediaItem<*>,
                newItem: MediaItem<*>
            ) = when (oldItem) {
                is Album -> oldItem.areContentsTheSame(newItem)
                is Artist -> oldItem.areContentsTheSame(newItem)
                is Audio -> oldItem.areContentsTheSame(newItem)
                is Genre -> oldItem.areContentsTheSame(newItem)
                is Playlist -> oldItem.areContentsTheSame(newItem)
            }
        }
    }
}
