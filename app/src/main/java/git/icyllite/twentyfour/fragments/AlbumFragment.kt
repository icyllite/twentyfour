/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.ext.getParcelable
import git.icyllite.twentyfour.ext.getViewProperty
import git.icyllite.twentyfour.ext.loadThumbnail
import git.icyllite.twentyfour.ext.navigateSafe
import git.icyllite.twentyfour.ext.setProgressCompat
import git.icyllite.twentyfour.ext.updateMargin
import git.icyllite.twentyfour.ext.updatePadding
import git.icyllite.twentyfour.models.Error
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.ui.recyclerview.SimpleListAdapter
import git.icyllite.twentyfour.ui.recyclerview.UniqueItemDiffCallback
import git.icyllite.twentyfour.ui.views.ListItem
import git.icyllite.twentyfour.utils.PermissionsChecker
import git.icyllite.twentyfour.utils.PermissionsUtils
import git.icyllite.twentyfour.utils.TimestampFormatter
import git.icyllite.twentyfour.viewmodels.AlbumViewModel

/**
 * Single music album viewer.
 */
class AlbumFragment : Fragment(R.layout.fragment_album) {
    // View models
    private val viewModel by viewModels<AlbumViewModel>()

    // Views
    private val albumTitleTextView by getViewProperty<TextView>(R.id.albumTitleTextView)
    private val artistNameTextView by getViewProperty<TextView>(R.id.artistNameTextView)
    private val fileTypeMaterialCardView by getViewProperty<MaterialCardView>(R.id.fileTypeMaterialCardView)
    private val fileTypeTextView by getViewProperty<TextView>(R.id.fileTypeTextView)
    private val infoNestedScrollView by getViewProperty<NestedScrollView?>(R.id.infoNestedScrollView)
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsNestedScrollView by getViewProperty<NestedScrollView>(R.id.noElementsNestedScrollView)
    private val playAllExtendedFloatingActionButton by getViewProperty<ExtendedFloatingActionButton>(
        R.id.playAllExtendedFloatingActionButton
    )
    private val playButtonsLinearLayout by getViewProperty<LinearLayout>(R.id.playButtonsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val shufflePlayExtendedFloatingActionButton by getViewProperty<ExtendedFloatingActionButton>(
        R.id.shufflePlayExtendedFloatingActionButton
    )
    private val thumbnailImageView by getViewProperty<ImageView>(R.id.thumbnailImageView)
    private val toolbar by getViewProperty<MaterialToolbar>(R.id.toolbar)
    private val tracksInfoTextView by getViewProperty<TextView>(R.id.tracksInfoTextView)
    private val yearTextView by getViewProperty<TextView>(R.id.yearTextView)

    // Recyclerview
    private val adapter by lazy {
        object : SimpleListAdapter<AlbumViewModel.AlbumContent, ListItem>(
            UniqueItemDiffCallback(),
            ::ListItem,
        ) {
            private val ViewHolder.trackTextView
                get() = view.leadingView!!.findViewById<TextView>(R.id.trackTextView)

            override fun ViewHolder.onPrepareView() {
                view.setLeadingView(R.layout.audio_track_index)
            }

            override fun ViewHolder.onBindView(item: AlbumViewModel.AlbumContent) {
                when (item) {
                    is AlbumViewModel.AlbumContent.DiscHeader -> {
                        view.setLeadingIconImage(R.drawable.ic_album)
                        view.leadingViewIsVisible = false
                        view.setHeadlineText(
                            R.string.album_disc_header,
                            item.discNumber,
                        )
                        view.supportingText = null
                        view.trailingSupportingText = null
                        view.isClickable = false
                        view.isLongClickable = false
                    }

                    is AlbumViewModel.AlbumContent.AudioItem -> {
                        view.setOnClickListener {
                            viewModel.playAlbum(item.audio)

                            findNavController().navigateSafe(
                                R.id.action_albumFragment_to_fragment_now_playing
                            )
                        }
                        view.setOnLongClickListener {
                            findNavController().navigateSafe(
                                R.id.action_albumFragment_to_fragment_media_item_bottom_sheet_dialog,
                                MediaItemBottomSheetDialogFragment.createBundle(
                                    item.audio.uri,
                                    fromAlbum = true,
                                )
                            )

                            true
                        }

                        item.audio.trackNumber?.also {
                            view.leadingIconImage = null
                            trackTextView.text = getString(
                                R.string.track_number,
                                it
                            )
                            view.leadingViewIsVisible = true
                        } ?: run {
                            view.setLeadingIconImage(R.drawable.ic_music_note)
                            view.leadingViewIsVisible = false
                        }

                        view.headlineText = item.audio.title
                        item.audio.artistName?.also {
                            view.supportingText = it
                        } ?: view.setSupportingText(R.string.artist_unknown)
                        view.trailingSupportingText = item.audio.durationMs?.let {
                            TimestampFormatter.formatTimestampMillis(it)
                        }
                        view.isClickable = true
                        view.isLongClickable = true
                    }
                }
            }
        }
    }

    // Arguments
    private val albumUri: Uri
        get() = requireArguments().getParcelable(ARG_ALBUM_URI, Uri::class)!!

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            v.updatePadding(
                insets,
                start = true,
                end = true,
            )

            windowInsets
        }

        infoNestedScrollView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updatePadding(
                    insets,
                    bottom = true,
                )

                windowInsets
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(noElementsNestedScrollView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            playButtonsLinearLayout
        ) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateMargin(
                insets,
                bottom = true,
            )

            windowInsets
        }

        toolbar.setupWithNavController(findNavController())
        toolbar.inflateMenu(R.menu.fragment_album_toolbar)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.addToQueue -> {
                    viewModel.addToQueue()
                    true
                }

                R.id.playNext -> {
                    viewModel.playNext()
                    true
                }

                else -> false
            }
        }

        recyclerView.adapter = adapter

        playAllExtendedFloatingActionButton.setOnClickListener {
            viewModel.playAlbum()

            findNavController().navigateSafe(R.id.action_albumFragment_to_fragment_now_playing)
        }

        shufflePlayExtendedFloatingActionButton.setOnClickListener {
            viewModel.shufflePlayAlbum()

            findNavController().navigateSafe(R.id.action_albumFragment_to_fragment_now_playing)
        }

        viewModel.loadAlbum(albumUri)

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
                viewModel.album.collectLatest {
                    linearProgressIndicator.setProgressCompat(it)

                    when (it) {
                        is FlowResult.Loading -> {
                            // Do nothing
                        }

                        is FlowResult.Success -> {
                            val (album, audios) = it.data

                            album.title?.also { albumTitle ->
                                toolbar.title = albumTitle
                                albumTitleTextView.text = albumTitle
                            } ?: run {
                                toolbar.setTitle(R.string.album_unknown)
                                albumTitleTextView.setText(R.string.album_unknown)
                            }

                            thumbnailImageView.loadThumbnail(
                                album.thumbnail,
                                placeholder = R.drawable.ic_album
                            )

                            album.artistName?.also { artistName ->
                                artistNameTextView.text = artistName
                            } ?: artistNameTextView.setText(R.string.artist_unknown)
                            artistNameTextView.setOnClickListener {
                                album.artistUri?.let { artistUri ->
                                    findNavController().navigateSafe(
                                        R.id.action_albumFragment_to_fragment_artist,
                                        ArtistFragment.createBundle(artistUri)
                                    )
                                }
                            }

                            album.year?.also { year ->
                                yearTextView.isVisible = true
                                yearTextView.text = getString(R.string.year_format, year)
                            } ?: run {
                                yearTextView.isVisible = false
                            }

                            val totalDurationMs = audios.sumOf { audio ->
                                audio.durationMs ?: 0L
                            }
                            val totalDurationMinutes = (totalDurationMs / 1000 / 60).toInt()

                            val tracksCount = resources.getQuantityString(
                                R.plurals.tracks_count,
                                audios.size,
                                audios.size
                            )
                            val tracksDuration = resources.getQuantityString(
                                R.plurals.tracks_duration,
                                totalDurationMinutes,
                                totalDurationMinutes
                            )
                            tracksInfoTextView.text = getString(
                                R.string.tracks_info,
                                tracksCount, tracksDuration
                            )
                        }

                        is FlowResult.Error -> {
                            Log.e(
                                LOG_TAG,
                                "Error loading album, error: ${it.error}",
                                it.throwable
                            )

                            toolbar.title = ""
                            albumTitleTextView.text = ""

                            if (it.error == Error.NOT_FOUND) {
                                // Get out of here
                                findNavController().navigateUp()
                            }
                        }
                    }
                }
            }

            launch {
                viewModel.albumContent.collectLatest {
                    adapter.submitList(it)

                    val isEmpty = it.isEmpty()
                    recyclerView.isVisible = !isEmpty
                    noElementsNestedScrollView.isVisible = isEmpty
                    when (isEmpty) {
                        true -> {
                            playAllExtendedFloatingActionButton.hide()
                            shufflePlayExtendedFloatingActionButton.hide()
                        }

                        false -> {
                            playAllExtendedFloatingActionButton.show()
                            shufflePlayExtendedFloatingActionButton.show()
                        }
                    }
                }
            }

            launch {
                viewModel.albumFileTypes.collectLatest {
                    fileTypeMaterialCardView.isVisible = it.isNotEmpty()
                    fileTypeTextView.text = it.joinToString(" / ")
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = AlbumFragment::class.simpleName!!

        private const val ARG_ALBUM_URI = "album_uri"

        /**
         * Create a [Bundle] to use as the arguments for this fragment.
         * @param albumUri The URI of the album to display
         */
        fun createBundle(
            albumUri: Uri,
        ) = bundleOf(
            ARG_ALBUM_URI to albumUri,
        )
    }
}
