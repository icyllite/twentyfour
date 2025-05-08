/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.database.TwentyfourDatabase
import git.icyllite.twentyfour.datasources.jellyfin.JellyfinClient
import git.icyllite.twentyfour.datasources.jellyfin.models.Item
import git.icyllite.twentyfour.datasources.jellyfin.models.ItemType
import git.icyllite.twentyfour.ext.isRelativeTo
import git.icyllite.twentyfour.models.ActivityTab
import git.icyllite.twentyfour.models.Album
import git.icyllite.twentyfour.models.Artist
import git.icyllite.twentyfour.models.ArtistWorks
import git.icyllite.twentyfour.models.Audio
import git.icyllite.twentyfour.models.DataSourceInformation
import git.icyllite.twentyfour.models.Error
import git.icyllite.twentyfour.models.Genre
import git.icyllite.twentyfour.models.GenreContent
import git.icyllite.twentyfour.models.LocalizedString
import git.icyllite.twentyfour.models.Lyrics
import git.icyllite.twentyfour.models.MediaType
import git.icyllite.twentyfour.models.Playlist
import git.icyllite.twentyfour.models.ProviderArgument
import git.icyllite.twentyfour.models.ProviderArgument.Companion.requireArgument
import git.icyllite.twentyfour.models.ProviderIdentifier
import git.icyllite.twentyfour.models.ProviderType
import git.icyllite.twentyfour.models.Result
import git.icyllite.twentyfour.models.Result.Companion.flatMap
import git.icyllite.twentyfour.models.Result.Companion.getOrNull
import git.icyllite.twentyfour.models.Result.Companion.map
import git.icyllite.twentyfour.models.SortingRule
import git.icyllite.twentyfour.models.SortingStrategy
import git.icyllite.twentyfour.models.Thumbnail
import git.icyllite.twentyfour.repositories.ProvidersRepository
import java.util.UUID

/**
 * Jellyfin backed data source.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JellyfinDataSource(
    context: Context,
    coroutineScope: CoroutineScope,
    providersRepository: ProvidersRepository,
    database: TwentyfourDatabase,
    deviceIdentifier: String,
    cache: Cache? = null,
) : MediaDataSource {
    private class JellyfinInstance(
        val client: JellyfinClient,
        val server: String,
    ) : ProvidersManager.Instance {
        private val dataSourceBaseUri = server.toUri()

        val albumsUri: Uri = dataSourceBaseUri.buildUpon()
            .appendPath(ALBUMS_PATH)
            .build()
        val artistsUri: Uri = dataSourceBaseUri.buildUpon()
            .appendPath(ARTISTS_PATH)
            .build()
        val audiosUri: Uri = dataSourceBaseUri.buildUpon()
            .appendPath(AUDIOS_PATH)
            .build()
        val genresUri: Uri = dataSourceBaseUri.buildUpon()
            .appendPath(GENRES_PATH)
            .build()
        val playlistsUri: Uri = dataSourceBaseUri.buildUpon()
            .appendPath(PLAYLISTS_PATH)
            .build()

        val favoritesUri: Uri = dataSourceBaseUri.buildUpon()
            .appendPath(FAVORITES_PATH)
            .build()
        val favoritesPlaylist = Playlist.Builder(favoritesUri)
            .setType(Playlist.Type.FAVORITES)
            .build()

        /**
         * This flow is used to signal a change in the playlists.
         */
        val playlistsChanged = MutableStateFlow(Any())

        /**
         * This flow is used to signal a change in the favorites.
         */
        val favoritesChanged = MutableStateFlow(Any())

        override suspend fun isMediaItemCompatible(
            mediaItemUri: Uri
        ) = mediaItemUri.isRelativeTo(dataSourceBaseUri)

        fun Item.toMediaItemAlbum() = Album.Builder(getAlbumUri(id.toString()))
            .setThumbnail(
                Thumbnail.Builder()
                    .setUri(client.getAlbumThumbnail(id).toUri())
                    .build()
            )
            .setTitle(name)
            .setArtistUri(getArtistUri(id.toString()))
            .setArtistName(artists?.firstOrNull())
            .setYear(productionYear)
            .build()

        fun Item.toMediaItemArtist() = Artist.Builder(getArtistUri(id.toString()))
            .setThumbnail(
                Thumbnail.Builder()
                    .setUri(client.getArtistThumbnail(id).toUri())
                    .build()
            )
            .setName(name)
            .build()

        fun Item.toMediaItemAudio() = Audio.Builder(getAudioUri(id.toString()))
            .setPlaybackUri(client.getAudioPlaybackUrl(id).toUri())
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
            .setIsFavorite(isFavorite == true)
            .build()

        fun Item.toMediaItemGenre() = Genre.Builder(getGenreUri(id.toString()))
            .setThumbnail(
                Thumbnail.Builder()
                    .setUri(client.getGenreThumbnail(id).toUri())
                    .build()
            )
            .setName(name)
            .build()

        fun Item.toMediaItemPlaylist() = Playlist.Builder(getPlaylistUri(id.toString()))
            .setThumbnail(
                Thumbnail.Builder()
                    .setUri(client.getPlaylistThumbnail(id).toUri())
                    .build()
            )
            .setName(name)
            .build()

        fun git.icyllite.twentyfour.datasources.jellyfin.models.Lyrics.toModel() = lyrics?.let {
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

        fun getAlbumUri(albumId: String): Uri = albumsUri.buildUpon()
            .appendPath(albumId)
            .build()

        fun getArtistUri(artistId: String): Uri = artistsUri.buildUpon()
            .appendPath(artistId)
            .build()

        fun getAudioUri(audioId: String): Uri = audiosUri.buildUpon()
            .appendPath(audioId)
            .build()

        fun getGenreUri(genre: String): Uri = genresUri.buildUpon()
            .appendPath(genre)
            .build()

        fun getPlaylistUri(playlistId: String): Uri = playlistsUri.buildUpon()
            .appendPath(playlistId)
            .build()

        fun onPlaylistsChanged() {
            playlistsChanged.value = Any()
        }
    }

    private val packageName = context.packageName

    private val providersManager = ProvidersManager(
        coroutineScope,
        providersRepository,
        ProviderType.JELLYFIN,
    ) { provider, arguments ->
        val server = arguments.requireArgument(ARG_SERVER)
        val username = arguments.requireArgument(ARG_USERNAME)
        val password = arguments.requireArgument(ARG_PASSWORD)

        JellyfinInstance(
            client = JellyfinClient(
                server,
                username,
                password,
                deviceIdentifier,
                packageName,
                { database.getJellyfinProviderDao().getToken(provider.typeId) },
                { database.getJellyfinProviderDao().updateToken(provider.typeId, it) },
                cache
            ),
            server = server,
        )
    }

    override fun status(
        providerIdentifier: ProviderIdentifier
    ) = providersManager.mapWithInstanceOf(providerIdentifier) {
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
    }

    override suspend fun mediaTypeOf(
        mediaItemUri: Uri,
    ) = providersManager.doWithInstanceOf(mediaItemUri) {
        with(mediaItemUri.toString()) {
            when {
                startsWith(albumsUri.toString()) -> MediaType.ALBUM
                startsWith(artistsUri.toString()) -> MediaType.ARTIST
                startsWith(audiosUri.toString()) -> MediaType.AUDIO
                startsWith(genresUri.toString()) -> MediaType.GENRE
                startsWith(playlistsUri.toString()) -> MediaType.PLAYLIST
                else -> null
            }
        }?.let {
            Result.Success(it)
        } ?: Result.Error(Error.NOT_FOUND)
    }.getOrNull()

    override fun providerOf(mediaItemUri: Uri) = providersManager.providerOf(mediaItemUri)

    override fun activity(
        providerIdentifier: ProviderIdentifier,
    ) = flowOf(Result.Success<_, Error>(listOf<ActivityTab>()))

    override fun albums(
        providerIdentifier: ProviderIdentifier,
        sortingRule: SortingRule,
    ) = providersManager.mapWithInstanceOf(providerIdentifier) {
        client.getAlbums(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemAlbum() }
        }
    }

    override fun artists(
        providerIdentifier: ProviderIdentifier,
        sortingRule: SortingRule,
    ) = providersManager.mapWithInstanceOf(providerIdentifier) {
        client.getArtists(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemArtist() }
        }
    }

    override fun genres(
        providerIdentifier: ProviderIdentifier,
        sortingRule: SortingRule,
    ) = providersManager.mapWithInstanceOf(providerIdentifier) {
        client.getGenres(sortingRule).map { queryResult ->
            queryResult.items.map { it.toMediaItemGenre() }
        }
    }

    override fun playlists(
        providerIdentifier: ProviderIdentifier,
        sortingRule: SortingRule,
    ) = providersManager.flatMapWithInstanceOf(providerIdentifier) {
        playlistsChanged.mapLatest {
            client.getPlaylists(sortingRule).map { queryResult ->
                buildList {
                    add(favoritesPlaylist)

                    queryResult.items.forEach { add(it.toMediaItemPlaylist()) }
                }
            }
        }
    }

    override fun search(
        providerIdentifier: ProviderIdentifier,
        query: String,
    ) = providersManager.mapWithInstanceOf(providerIdentifier) {
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
    }

    override fun audio(audioUri: Uri) = providersManager.mapWithInstanceOf(audioUri) {
        val id = UUID.fromString(audioUri.lastPathSegment!!)
        client.getAudio(id).map {
            it.toMediaItemAudio()
        }
    }

    override fun album(albumUri: Uri) = providersManager.mapWithInstanceOf(albumUri) {
        val id = UUID.fromString(albumUri.lastPathSegment!!)
        client.getAlbum(id).map { item ->
            val tracks = client.getAlbumTracks(id).map { queryResult ->
                queryResult.items.map { it.toMediaItemAudio() }
            }.getOrNull().orEmpty()

            item.toMediaItemAlbum() to tracks
        }
    }

    override fun artist(
        artistUri: Uri,
    ) = providersManager.mapWithInstanceOf(artistUri) {
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
    }

    override fun genre(genreUri: Uri) = providersManager.mapWithInstanceOf(genreUri) {
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
    }

    override fun playlist(
        playlistUri: Uri,
    ) = providersManager.flatMapWithInstanceOf(playlistUri) {
        when {
            playlistUri == favoritesUri -> favoritesChanged.mapLatest {
                client.getFavorites().map { queryResult ->
                    favoritesPlaylist to queryResult.items.map { it.toMediaItemAudio() }
                }
            }

            else -> playlistsChanged.mapLatest {
                val id = UUID.fromString(playlistUri.lastPathSegment!!)
                client.getPlaylist(id).map { item ->
                    val tracks = client.getPlaylistTracks(id).map { queryResult ->
                        queryResult.items.map { it.toMediaItemAudio() }
                    }.getOrNull().orEmpty()

                    item.toMediaItemPlaylist() to tracks
                }
            }
        }
    }

    override fun audioPlaylistsStatus(
        audioUri: Uri
    ) = UUID.fromString(audioUri.lastPathSegment!!).let { audioId ->
        providersManager.flatMapWithInstanceOf(audioUri) {
            combine(
                favoritesChanged.mapLatest {
                    val isFavorite = client.getAudio(audioId).getOrNull()?.isFavorite
                    favoritesPlaylist to (isFavorite == true)
                },
                playlistsChanged.mapLatest {
                    val sortingRule = SortingRule(SortingStrategy.NAME)

                    client.getPlaylists(sortingRule).map { queryResult ->
                        queryResult.items.map { playlist ->
                            val playlistItems =
                                client.getPlaylistItemIds(playlist.id).map {
                                    it.itemIds
                                }.getOrNull().orEmpty()

                            playlist.toMediaItemPlaylist() to (audioId in playlistItems)
                        }
                    }
                },
            ) { favoriteToAudio, playlistToAudio ->
                playlistToAudio.map { playlists ->
                    buildList {
                        add(favoriteToAudio)

                        addAll(playlists)
                    }
                }
            }
        }
    }

    override fun lyrics(audioUri: Uri) = providersManager.mapWithInstanceOf(audioUri) {
        val id = UUID.fromString(audioUri.lastPathSegment!!)
        client.getLyrics(id).map { it.toModel() }.flatMap { lyrics ->
            lyrics?.let { Result.Success(it) } ?: Result.Error(Error.NOT_FOUND)
        }
    }

    override suspend fun createPlaylist(
        providerIdentifier: ProviderIdentifier,
        name: String,
    ) = providersManager.doWithInstanceOf(providerIdentifier) {
        client.createPlaylist(name).map { createPlaylistResult ->
            onPlaylistsChanged()

            getPlaylistUri(createPlaylistResult.id.toString())
        }
    }

    override suspend fun renamePlaylist(
        playlistUri: Uri,
        name: String,
    ) = providersManager.doWithInstanceOf(playlistUri) {
        when {
            playlistUri == favoritesUri -> Result.Error(Error.IO)
            else -> client.renamePlaylist(
                UUID.fromString(playlistUri.lastPathSegment!!), name
            ).map {
                onPlaylistsChanged()
            }
        }
    }

    override suspend fun deletePlaylist(
        playlistUri: Uri,
    ) = providersManager.doWithInstanceOf(playlistUri) {
        when {
            playlistUri == favoritesUri -> Result.Error(Error.IO)
            else -> Result.Error<Unit, _>(Error.NOT_IMPLEMENTED)
        }
    }

    override suspend fun addAudioToPlaylist(
        playlistUri: Uri,
        audioUri: Uri,
    ) = providersManager.doWithInstanceOf(playlistUri, audioUri) {
        when {
            playlistUri == favoritesUri -> setFavorite(audioUri, true)
            else -> {
                val playlistId = UUID.fromString(playlistUri.lastPathSegment!!)
                val audioId = UUID.fromString(audioUri.lastPathSegment!!)
                client.addItemToPlaylist(playlistId, audioId).map {
                    onPlaylistsChanged()
                }
            }
        }
    }

    override suspend fun removeAudioFromPlaylist(
        playlistUri: Uri,
        audioUri: Uri,
    ) = providersManager.doWithInstanceOf(playlistUri, audioUri) {
        when {
            playlistUri == favoritesUri -> setFavorite(audioUri, false)
            else -> {
                val playlistId = UUID.fromString(playlistUri.lastPathSegment!!)
                val audioId = UUID.fromString(audioUri.lastPathSegment!!)
                client.removeItemFromPlaylist(playlistId, audioId).map {
                    onPlaylistsChanged()
                }
            }
        }
    }

    override suspend fun onAudioPlayed(audioUri: Uri) = Result.Success<Unit, Error>(Unit)

    override suspend fun setFavorite(
        audioUri: Uri,
        isFavorite: Boolean
    ) = providersManager.doWithInstanceOf(audioUri) {
        when (isFavorite) {
            true -> client.addToFavorites(UUID.fromString(audioUri.lastPathSegment!!))
            false -> client.removeFromFavorites(UUID.fromString(audioUri.lastPathSegment!!))
        }
    }

    companion object {
        private const val ALBUMS_PATH = "albums"
        private const val ARTISTS_PATH = "artists"
        private const val AUDIOS_PATH = "audio"
        private const val GENRES_PATH = "genres"
        private const val PLAYLISTS_PATH = "playlists"

        private const val FAVORITES_PATH = "favorites"

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
