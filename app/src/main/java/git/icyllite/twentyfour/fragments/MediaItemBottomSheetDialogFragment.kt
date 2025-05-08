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
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil3.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.ext.getParcelable
import git.icyllite.twentyfour.ext.getViewProperty
import git.icyllite.twentyfour.ext.navigateSafe
import git.icyllite.twentyfour.models.Album
import git.icyllite.twentyfour.models.Artist
import git.icyllite.twentyfour.models.Audio
import git.icyllite.twentyfour.models.Error
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.Genre
import git.icyllite.twentyfour.models.Playlist
import git.icyllite.twentyfour.ui.views.FullscreenLoadingProgressBar
import git.icyllite.twentyfour.ui.views.ListItem
import git.icyllite.twentyfour.utils.PermissionsChecker
import git.icyllite.twentyfour.utils.PermissionsUtils
import git.icyllite.twentyfour.viewmodels.MediaItemViewModel

/**
 * Audio information.
 */
class MediaItemBottomSheetDialogFragment : BottomSheetDialogFragment(
    R.layout.fragment_media_item_bottom_sheet_dialog
) {
    // View models
    private val viewModel by viewModels<MediaItemViewModel>()

    // Views
    private val addOrRemoveFromFavoritesListItem by getViewProperty<ListItem>(R.id.addOrRemoveFromFavoritesListItem)
    private val addOrRemoveFromPlaylistsListItem by getViewProperty<ListItem>(R.id.addOrRemoveFromPlaylistsListItem)
    private val addToQueueListItem by getViewProperty<ListItem>(R.id.addToQueueListItem)
    private val artistNameTextView by getViewProperty<TextView>(R.id.artistNameTextView)
    private val albumTitleTextView by getViewProperty<TextView>(R.id.albumTitleTextView)
    private val fullscreenLoadingProgressBar by getViewProperty<FullscreenLoadingProgressBar>(R.id.fullscreenLoadingProgressBar)
    private val openAlbumListItem by getViewProperty<ListItem>(R.id.openAlbumListItem)
    private val openArtistListItem by getViewProperty<ListItem>(R.id.openArtistListItem)
    private val openGenreListItem by getViewProperty<ListItem>(R.id.openGenreListItem)
    private val placeholderImageView by getViewProperty<ImageView>(R.id.placeholderImageView)
    private val playNowListItem by getViewProperty<ListItem>(R.id.playNowListItem)
    private val playNextListItem by getViewProperty<ListItem>(R.id.playNextListItem)
    private val removeFromPlaylistListItem by getViewProperty<ListItem>(R.id.removeFromPlaylistListItem)
    private val thumbnailImageView by getViewProperty<ImageView>(R.id.thumbnailImageView)
    private val titleTextView by getViewProperty<TextView>(R.id.titleTextView)

    // Arguments
    private val uri: Uri
        get() = requireArguments().getParcelable(ARG_URI, Uri::class)!!
    private val fromAlbum: Boolean
        get() = requireArguments().getBoolean(ARG_FROM_ALBUM)
    private val fromArtist: Boolean
        get() = requireArguments().getBoolean(ARG_FROM_ARTIST)
    private val fromGenre: Boolean
        get() = requireArguments().getBoolean(ARG_FROM_GENRE)
    private val fromNowPlaying: Boolean
        get() = requireArguments().getBoolean(ARG_FROM_NOW_PLAYING)
    private val playlistUri: Uri?
        get() = requireArguments().getParcelable(ARG_PLAYLIST_URI, Uri::class)

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setUri(uri)
        viewModel.setFromAlbum(fromAlbum)
        viewModel.setFromArtist(fromArtist)
        viewModel.setFromGenre(fromGenre)
        viewModel.setFromNowPlaying(fromNowPlaying)
        viewModel.setPlaylistUri(playlistUri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playNowListItem.setOnClickListener {
            viewModel.playNow()

            findNavController().navigateUp()
        }

        addToQueueListItem.setOnClickListener {
            viewModel.addToQueue()

            findNavController().navigateUp()
        }

        playNextListItem.setOnClickListener {
            viewModel.playNext()

            findNavController().navigateUp()
        }

        addOrRemoveFromFavoritesListItem.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                fullscreenLoadingProgressBar.withProgress {
                    viewModel.toggleFavorites()
                }
            }
        }

        removeFromPlaylistListItem.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                fullscreenLoadingProgressBar.withProgress {
                    viewModel.removeAudioFromPlaylist()

                    findNavController().navigateUp()
                }
            }
        }

        addOrRemoveFromPlaylistsListItem.setOnClickListener {
            viewModel.uri.value?.let {
                findNavController().navigateSafe(
                    R.id.action_mediaItemBottomSheetDialogFragment_to_fragment_add_or_remove_from_playlists,
                    AddOrRemoveFromPlaylistsFragment.createBundle(it)
                )
            }
        }

        openAlbumListItem.setOnClickListener {
            viewModel.albumUri.value?.let { albumUri ->
                findNavController().navigateSafe(
                    R.id.action_mediaItemBottomSheetDialogFragment_to_fragment_album,
                    AlbumFragment.createBundle(albumUri)
                )
            }
        }

        openArtistListItem.setOnClickListener {
            viewModel.artistUri.value?.let { artistUri ->
                findNavController().navigateSafe(
                    R.id.action_mediaItemBottomSheetDialogFragment_to_fragment_artist,
                    ArtistFragment.createBundle(artistUri)
                )
            }
        }

        openGenreListItem.setOnClickListener {
            viewModel.genreUri.value?.let { genreUri ->
                findNavController().navigateSafe(
                    R.id.action_mediaItemBottomSheetDialogFragment_to_fragment_genre,
                    GenreFragment.createBundle(genreUri)
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }

    private fun CoroutineScope.loadData() {
        launch {
            viewModel.mediaItem.collectLatest {
                when (it) {
                    is FlowResult.Loading -> {
                        // Do nothing
                    }

                    is FlowResult.Success -> {
                        val mediaItem = it.data

                        titleTextView.text = when (mediaItem) {
                            is Album -> mediaItem.title ?: getString(R.string.album_unknown)
                            is Artist -> mediaItem.name ?: getString(R.string.artist_unknown)
                            is Audio -> mediaItem.title
                            is Genre -> mediaItem.name ?: getString(R.string.genre_unknown)
                            is Playlist -> mediaItem.name ?: getString(
                                when (mediaItem.type) {
                                    Playlist.Type.PLAYLIST -> R.string.playlist_unknown
                                    Playlist.Type.FAVORITES -> R.string.favorites_playlist
                                }
                            )
                        }
                        artistNameTextView.text = when (mediaItem) {
                            is Album -> mediaItem.artistName
                            is Audio -> mediaItem.artistName
                            else -> null
                        }.also { artistName ->
                            artistNameTextView.isVisible = artistName != null
                        }
                        albumTitleTextView.text = when (mediaItem) {
                            is Audio -> mediaItem.albumTitle
                            else -> null
                        }.also { albumTitle ->
                            albumTitleTextView.isVisible = albumTitle != null
                        }

                        placeholderImageView.setImageResource(
                            when (mediaItem) {
                                is Album -> R.drawable.ic_album
                                is Artist -> R.drawable.ic_person
                                is Audio -> R.drawable.ic_music_note
                                is Genre -> R.drawable.ic_genres
                                is Playlist -> when (mediaItem.type) {
                                    Playlist.Type.PLAYLIST -> R.drawable.ic_playlist_play
                                    Playlist.Type.FAVORITES -> R.drawable.ic_favorite
                                }
                            }
                        )

                        mediaItem.thumbnail?.also { thumbnail ->
                            thumbnailImageView.load(thumbnail) {
                                listener(
                                    onCancel = {
                                        placeholderImageView.isVisible = true
                                        thumbnailImageView.isVisible = false
                                    },
                                    onSuccess = { _, _ ->
                                        placeholderImageView.isVisible = false
                                        thumbnailImageView.isVisible = true
                                    },
                                    onError = { _, _ ->
                                        placeholderImageView.isVisible = true
                                        thumbnailImageView.isVisible = false
                                    }
                                )
                            }
                        } ?: run {
                            placeholderImageView.isVisible = true
                            thumbnailImageView.isVisible = false
                        }

                        when (mediaItem) {
                            is Audio -> {
                                addOrRemoveFromFavoritesListItem.setHeadlineText(
                                    when (mediaItem.isFavorite) {
                                        true -> R.string.remove_from_favorites
                                        false -> R.string.add_to_favorites
                                    }
                                )
                                addOrRemoveFromFavoritesListItem.setLeadingIconImage(
                                    when (mediaItem.isFavorite) {
                                        true -> R.drawable.ic_heart_filled
                                        false -> R.drawable.ic_heart_unfilled
                                    }
                                )
                            }

                            else -> {}
                        }
                    }

                    is FlowResult.Error -> {
                        Log.e(
                            LOG_TAG,
                            "Failed to load media item, error: ${it.error}",
                            it.throwable
                        )

                        if (it.error == Error.NOT_FOUND) {
                            // Get out of here
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }

        launch {
            viewModel.showQueueButtons.collectLatest { showQueueButtons ->
                playNowListItem.isVisible = showQueueButtons
                addToQueueListItem.isVisible = showQueueButtons
                playNextListItem.isVisible = showQueueButtons
            }
        }

        launch {
            viewModel.canToggleFavorite.collectLatest { canToggleFavorite ->
                addOrRemoveFromFavoritesListItem.isVisible = canToggleFavorite
            }
        }

        launch {
            viewModel.canAddOrRemoveFromPlaylists.collectLatest { canAddOrRemoveFromPlaylists ->
                addOrRemoveFromPlaylistsListItem.isVisible = canAddOrRemoveFromPlaylists
            }
        }

        launch {
            viewModel.canRemoveFromPlaylist.collectLatest { canRemoveFromPlaylist ->
                removeFromPlaylistListItem.isVisible = canRemoveFromPlaylist
            }
        }

        launch {
            viewModel.albumUri.collectLatest { albumUri ->
                openAlbumListItem.isVisible = albumUri != null
            }
        }

        launch {
            viewModel.artistUri.collectLatest { artistUri ->
                openArtistListItem.isVisible = artistUri != null
            }
        }

        launch {
            viewModel.genreUri.collectLatest { genreUri ->
                openGenreListItem.isVisible = genreUri != null
            }
        }
    }

    companion object {
        private val LOG_TAG = MediaItemBottomSheetDialogFragment::class.simpleName!!

        private const val ARG_URI = "uri"
        private const val ARG_FROM_ALBUM = "from_album"
        private const val ARG_FROM_ARTIST = "from_artist"
        private const val ARG_FROM_GENRE = "from_genre"
        private const val ARG_FROM_NOW_PLAYING = "from_now_playing"
        private const val ARG_PLAYLIST_URI = "playlist_uri"

        /**
         * Create a [Bundle] to use as the arguments for this fragment.
         * @param uri The URI of the media item to display
         * @param fromAlbum Whether this fragment was opened from an album
         * @param fromArtist Whether this fragment was opened from an artist
         * @param fromGenre Whether this fragment was opened from a genre
         * @param fromNowPlaying Whether this fragment was opened from the now playing fragment
         * @param playlistUri If the audio has been opened from a playlist, the URI of the playlist
         */
        fun createBundle(
            uri: Uri,
            fromAlbum: Boolean = false,
            fromArtist: Boolean = false,
            fromGenre: Boolean = false,
            fromNowPlaying: Boolean = false,
            playlistUri: Uri? = null,
        ) = bundleOf(
            ARG_URI to uri,
            ARG_FROM_ALBUM to fromAlbum,
            ARG_FROM_ARTIST to fromArtist,
            ARG_FROM_GENRE to fromGenre,
            ARG_FROM_NOW_PLAYING to fromNowPlaying,
            ARG_PLAYLIST_URI to playlistUri,
        )
    }
}
