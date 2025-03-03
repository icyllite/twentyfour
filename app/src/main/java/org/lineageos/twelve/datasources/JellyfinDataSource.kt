/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.datasources

import android.content.Context
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.lineageos.twelve.R
import org.lineageos.twelve.datasources.jellyfin.JellyfinClient
import org.lineageos.twelve.datasources.jellyfin.models.Item
import org.lineageos.twelve.datasources.jellyfin.models.ItemType
import org.lineageos.twelve.models.ActivityTab
import org.lineageos.twelve.models.Album
import org.lineageos.twelve.models.Artist
import org.lineageos.twelve.models.ArtistWorks
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.DataSourceInformation
import org.lineageos.twelve.models.Error
import org.lineageos.twelve.models.Genre
import org.lineageos.twelve.models.GenreContent
import org.lineageos.twelve.models.LocalizedString
import org.lineageos.twelve.models.Lyrics
import org.lineageos.twelve.models.MediaItem
import org.lineageos.twelve.models.MediaType
import org.lineageos.twelve.models.Playlist
import org.lineageos.twelve.models.ProviderArgument
import org.lineageos.twelve.models.ProviderArgument.Companion.requireArgument
import org.lineageos.twelve.models.Result
import org.lineageos.twelve.models.Result.Companion.fold
import org.lineageos.twelve.models.Result.Companion.getOrNull
import org.lineageos.twelve.models.Result.Companion.map
import org.lineageos.twelve.models.SortingRule
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.models.Thumbnail
import java.util.UUID

/**
 * Jellyfin backed data source.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JellyfinDataSource(
    context: Context,
    arguments: Bundle,
    deviceIdentifier: String,
    tokenGetter: () -> String?,
    tokenSetter: (String) -> Unit,
    private val lastPlayedGetter: (String) -> Flow<Uri?>,
    private val lastPlayedSetter: suspend (String, Uri) -> Long,
    cache: Cache? = null,
) : MediaDataSource {
    private val server = arguments.requireArgument(ARG_SERVER)
    private val username = arguments.requireArgument(ARG_USERNAME)
    private val password = arguments.requireArgument(ARG_PASSWORD)

    private val packageName = context.packageName

    private val client = JellyfinClient(
        server, username, password, deviceIdentifier, packageName, tokenGetter, tokenSetter, cache
    )

    private val dataSourceBaseUri = Uri.parse(server)

    private val albumsUri = dataSourceBaseUri.buildUpon()
        .appendPath(ALBUMS_PATH)
        .build()
    private val artistsUri = dataSourceBaseUri.buildUpon()
        .appendPath(ARTISTS_PATH)
        .build()
    private val audiosUri = dataSourceBaseUri.buildUpon()
        .appendPath(AUDIOS_PATH)
        .build()
    private val genresUri = dataSourceBaseUri.buildUpon()
        .appendPath(GENRES_PATH)
        .build()
    private val playlistsUri = dataSourceBaseUri.buildUpon()
        .appendPath(PLAYLISTS_PATH)
        .build()

    /**
     * This flow is used to signal a change in the playlists.
     */
    private val _playlistsChanged = MutableStateFlow(Any())

    override fun status() = suspend {
        client.getSystemInfo().map { systemInfo ->
            listOfNotNull(
                systemInfo.serverName?.takeIf { it.isNotBlank() }?.let {
                    DataSourceInformation(
                        "server_name",
                        LocalizedString.StringResIdLocalizedString(R.string.jellyfin_server_name),
                        LocalizedString.StringLocalizedString(it)
                    )
                },
                systemInfo.version?.takeIf { it.isNotBlank() }?.let {
                    DataSourceInformation(
                        "version",
                        LocalizedString.StringResIdLocalizedString(R.string.jellyfin_version),
                        LocalizedString.StringLocalizedString(it)
                    )
                },
                systemInfo.productName?.takeIf { it.isNotBlank() }?.let {
                    DataSourceInformation(
                        "product_name",
                        LocalizedString.StringResIdLocalizedString(R.string.jellyfin_product_name),
                        LocalizedString.StringLocalizedString(it)
                    )
                },
                systemInfo.operatingSystem?.takeIf { it.isNotBlank() }?.let {
                    DataSourceInformation(
                        "operating_system",
                        LocalizedString.StringResIdLocalizedString(
                            R.string.jellyfin_operating_system,
                        ),
                        LocalizedString.StringLocalizedString(it)
                    )
                },
            )
        }
    }.asFlow()

    override suspend fun mediaTypeOf(mediaItemUri: Uri) = with(mediaItemUri.toString()) {
        when {
            startsWith(albumsUri.toString()) -> MediaType.ALBUM
            startsWith(artistsUri.toString()) -> MediaType.ARTIST
            startsWith(audiosUri.toString()) -> MediaType.AUDIO
            startsWith(genresUri.toString()) -> MediaType.GENRE
            startsWith(playlistsUri.toString()) -> MediaType.PLAYLIST
            else -> null
        }
    }

    override fun activity() = lastPlayedItems().mapLatest { lastPlayedRs ->
        lastPlayedRs.map { lastPlayed ->
            listOf(
                ActivityTab(
                    "last_played",
                    LocalizedString.StringResIdLocalizedString(R.string.activity_last_played),
                    lastPlayed
                ),
            ).filter { it.items.isNotEmpty() }
        }
    }

    override fun albums(sortingRule: SortingRule) = suspend {
        client.getAlbums(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemAlbum() }
        }
    }.asFlow()

    override fun artists(sortingRule: SortingRule) = suspend {
        client.getArtists(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemArtist() }
        }
    }.asFlow()

    override fun genres(sortingRule: SortingRule) = suspend {
        client.getGenres(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemGenre() }
        }
    }.asFlow()

    override fun playlists(sortingRule: SortingRule) = _playlistsChanged.mapLatest {
        client.getPlaylists(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemPlaylist() }
        }
    }

    override fun search(query: String) = suspend {
        client.getItems(query).map { queryResult ->
            queryResult.items.mapNotNull {
                when (it.type) {
                    ItemType.MUSIC_ALBUM -> it.toMediaItemAlbum()

                    ItemType.MUSIC_ARTIST,
                    ItemType.PERSON -> it.toMediaItemArtist()

                    ItemType.AUDIO -> it.toMediaItemAudio()

                    ItemType.GENRE,
                    ItemType.MUSIC_GENRE -> it.toMediaItemGenre()

                    ItemType.PLAYLIST -> it.toMediaItemPlaylist()

                    else -> null
                }
            }
        }
    }.asFlow()

    override fun audio(audioUri: Uri) = suspend {
        val id = UUID.fromString(audioUri.lastPathSegment!!)
        client.getAudio(id).map {
            it.toMediaItemAudio()
        }
    }.asFlow()

    override fun album(albumUri: Uri) = suspend {
        val id = UUID.fromString(albumUri.lastPathSegment!!)
        client.getAlbum(id).map { item ->
            item.toMediaItemAlbum() to (client.getAlbumTracks(id).map { queryResult ->
                queryResult.items.map { it.toMediaItemAudio() }
            }.getOrNull().orEmpty())
        }
    }.asFlow()

    override fun artist(artistUri: Uri) = suspend {
        val id = UUID.fromString(artistUri.lastPathSegment!!)
        client.getArtist(id).map { item ->
            item.toMediaItemArtist() to ArtistWorks(
                albums = client.getArtistWorks(id).map { queryResult ->
                    queryResult.items.map { it.toMediaItemAlbum() }
                }.getOrNull().orEmpty(),
                appearsInAlbum = listOf(),
                appearsInPlaylist = listOf(),
            )
        }
    }.asFlow()

    override fun genre(genreUri: Uri) = suspend {
        val id = UUID.fromString(genreUri.lastPathSegment!!)
        client.getGenre(id).map { item ->
            val items = client.getGenreContent(id).map { it.items }.getOrNull().orEmpty()
            item.toMediaItemGenre() to GenreContent(
                appearsInAlbums = items.filter { it.type == ItemType.MUSIC_ALBUM }
                    .map { it.toMediaItemAlbum() },
                appearsInPlaylists = items.filter { it.type == ItemType.PLAYLIST }
                    .map { it.toMediaItemPlaylist() },
                audios = items.filter { it.type == ItemType.AUDIO }
                    .map { it.toMediaItemAudio() },
            )
        }
    }.asFlow()

    override fun playlist(playlistUri: Uri) = _playlistsChanged.mapLatest {
        val id = UUID.fromString(playlistUri.lastPathSegment!!)
        client.getPlaylist(id).map { item ->
            item.toMediaItemPlaylist() to client.getPlaylistTracks(id).map { queryResult ->
                queryResult.items.map { it.toMediaItemAudio() }
            }.getOrNull().orEmpty()
        }
    }

    override fun audioPlaylistsStatus(audioUri: Uri) = _playlistsChanged.mapLatest {
        val audioId = UUID.fromString(audioUri.lastPathSegment!!)
        val sortingRule = SortingRule(SortingStrategy.NAME)

        client.getPlaylists(sortingRule).map { queryResult ->
            queryResult.items.map { playlist ->
                val playlistItems =
                    client.getPlaylistItemIds(playlist.id).map { it.itemIds }.getOrNull().orEmpty()
                playlist.toMediaItemPlaylist() to (audioId in playlistItems)
            }
        }
    }

    override fun lastPlayedAudio() = lastPlayedGetter(lastPlayedKey())
        .flatMapLatest { uri ->
            uri?.let(this::audio) ?: flowOf(Result.Error(Error.NOT_FOUND))
        }

    override fun lyrics(audioUri: Uri) = suspend {
        val id = UUID.fromString(audioUri.lastPathSegment!!)
        client.getLyrics(id).map { lyrics ->
            lyrics.toModel()
        }.let {
            when (it) {
                is Result.Success -> it.data?.let { lyrics ->
                    Result.Success<_, Error>(lyrics)
                } ?: Result.Error(Error.NOT_FOUND)

                is Result.Error -> Result.Error(it.error, it.throwable)
            }
        }
    }.asFlow()

    override suspend fun createPlaylist(name: String) = run {
        client.createPlaylist(name).map { createPlaylistResult ->
            onPlaylistsChanged()

            getPlaylistUri(createPlaylistResult.id.toString())
        }
    }

    override suspend fun renamePlaylist(playlistUri: Uri, name: String) =
        client.renamePlaylist(UUID.fromString(playlistUri.lastPathSegment!!), name)
            .map {
                onPlaylistsChanged()
            }

    override suspend fun deletePlaylist(playlistUri: Uri) = Result.Error<Unit, _>(
        Error.NOT_IMPLEMENTED
    )

    override suspend fun addAudioToPlaylist(
        playlistUri: Uri, audioUri: Uri
    ) = run {
        val playlistId = UUID.fromString(playlistUri.lastPathSegment!!)
        val audioId = UUID.fromString(audioUri.lastPathSegment!!)
        client.addItemToPlaylist(playlistId, audioId).map {
            onPlaylistsChanged()
        }
    }

    override suspend fun removeAudioFromPlaylist(
        playlistUri: Uri, audioUri: Uri
    ) = run {
        val playlistId = UUID.fromString(playlistUri.lastPathSegment!!)
        val audioId = UUID.fromString(audioUri.lastPathSegment!!)
        client.removeItemFromPlaylist(playlistId, audioId).map {
            onPlaylistsChanged()
        }
    }

    override suspend fun onAudioPlayed(audioUri: Uri) =
        if (audioUri.lastPathSegment == "stream") {
            // When playing "stream?static=true" gets added to the audio URI.
            // We don't want to store that.
            Uri.parse(
                audioUri.toString().removeSuffix("stream?static=true")
            )
        } else {
            audioUri
        }.let {
            lastPlayedSetter(lastPlayedKey(), it)
                .let { Result.Success<Unit, Error>(Unit) }
        }

    override suspend fun setFavorite(
        audioUri: Uri,
        isFavorite: Boolean
    ) = Result.Error<Unit, _>(Error.NOT_IMPLEMENTED)

    private fun Item.toMediaItemAlbum() = Album.Builder(getAlbumUri(id.toString()))
        .setThumbnail(
            Thumbnail.Builder()
                .setUri(Uri.parse(client.getAlbumThumbnail(id)))
                .build()
        )
        .setTitle(name)
        .setArtistUri(getArtistUri(id.toString()))
        .setArtistName(artists?.firstOrNull())
        .setYear(productionYear)
        .build()

    private fun Item.toMediaItemArtist() = Artist.Builder(getArtistUri(id.toString()))
        .setThumbnail(
            Thumbnail.Builder()
                .setUri(Uri.parse(client.getArtistThumbnail(id)))
                .build()
        )
        .setName(name)
        .build()

    private fun Item.toMediaItemAudio() = Audio.Builder(getAudioUri(id.toString()))
        .setPlaybackUri(Uri.parse(client.getAudioPlaybackUrl(id)))
        .setMimeType(container ?: sourceType)
        .setTitle(name)
        .setType(Audio.Type.MUSIC)
        .setDurationMs(runTimeTicks?.let { it / 10000 })
        .setArtistUri(getArtistUri(id.toString()))
        .setArtistName(artists?.firstOrNull())
        .setAlbumUri(getAlbumUri(id.toString()))
        .setAlbumTitle(album)
        .setDiscNumber(parentIndexNumber)
        .setTrackNumber(indexNumber)
        .setGenreUri(getGenreUri(id.toString()))
        .setGenreName(genres?.firstOrNull())
        .setYear(productionYear)
        .build()

    private fun Item.toMediaItemGenre() = Genre.Builder(getGenreUri(id.toString()))
        .setThumbnail(
            Thumbnail.Builder()
                .setUri(Uri.parse(client.getGenreThumbnail(id)))
                .build()
        )
        .setName(name)
        .build()

    private fun Item.toMediaItemPlaylist() = Playlist.Builder(getPlaylistUri(id.toString()))
        .setThumbnail(
            Thumbnail.Builder()
                .setUri(Uri.parse(client.getPlaylistThumbnail(id)))
                .build()
        )
        .setName(name)
        .build()

    private fun org.lineageos.twelve.datasources.jellyfin.models.Lyrics.toModel() = lyrics?.let {
        Lyrics.Builder()
            .apply {
                it.forEach { lyrics ->
                    addLine(
                        text = lyrics.text,
                        startMs = lyrics.start / 10000,
                    )
                }
            }
            .build()
    }

    private fun getAlbumUri(albumId: String) = albumsUri.buildUpon()
        .appendPath(albumId)
        .build()

    private fun getArtistUri(artistId: String) = artistsUri.buildUpon()
        .appendPath(artistId)
        .build()

    private fun getAudioUri(audioId: String) = audiosUri.buildUpon()
        .appendPath(audioId)
        .build()

    private fun getGenreUri(genre: String) = genresUri.buildUpon()
        .appendPath(genre)
        .build()

    private fun getPlaylistUri(playlistId: String) = playlistsUri.buildUpon()
        .appendPath(playlistId)
        .build()

    private fun onPlaylistsChanged() {
        _playlistsChanged.value = Any()
    }

    private fun lastPlayedKey() = "jellyfin:$username@$server"

    /**
     * Get the latest played items (Audio and associated Album, if any).
     * @see lastPlayedAudio
     */
    private fun lastPlayedItems() = lastPlayedAudio().flatMapLatest { audioRs ->
        audioRs.fold(
            onSuccess = { audio ->
                val albumId = UUID.fromString(audio.albumUri!!.lastPathSegment!!)
                suspend {
                    client.getAlbum(albumId).map { it.toMediaItemAlbum() }
                }.asFlow().mapLatest { albumRs ->
                    val audioAsMediaItemList = listOf(audio as MediaItem<*>)
                    Result.Success<List<MediaItem<*>>, Error>(
                        albumRs.fold(
                            onSuccess = { album -> audioAsMediaItemList + album },
                            onError = { audioAsMediaItemList },
                        )
                    )
                }
            },
            onError = { flowOf(Result.Error(Error.NOT_FOUND)) },
        )
    }

    companion object {
        private const val ALBUMS_PATH = "albums"
        private const val ARTISTS_PATH = "artists"
        private const val AUDIOS_PATH = "audio"
        private const val GENRES_PATH = "genres"
        private const val PLAYLISTS_PATH = "playlists"

        val ARG_SERVER = ProviderArgument(
            "server",
            String::class,
            R.string.provider_argument_server,
            required = true,
            hidden = false,
            validate = {
                when (it.toHttpUrlOrNull()) {
                    null -> ProviderArgument.ValidationError(
                        "Invalid URL",
                        R.string.provider_argument_validation_error_malformed_http_uri,
                    )

                    else -> null
                }
            }
        )

        val ARG_USERNAME = ProviderArgument(
            "username",
            String::class,
            R.string.provider_argument_username,
            required = true,
            hidden = false,
        )

        val ARG_PASSWORD = ProviderArgument(
            "password",
            String::class,
            R.string.provider_argument_password,
            required = true,
            hidden = true,
            defaultValue = "",
        )
    }
}
