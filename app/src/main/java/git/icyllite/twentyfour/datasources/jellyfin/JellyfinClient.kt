/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.jellyfin

import androidx.core.net.toUri
import okhttp3.Cache
import okhttp3.OkHttpClient
import git.icyllite.twentyfour.datasources.jellyfin.models.CreatePlaylist
import git.icyllite.twentyfour.datasources.jellyfin.models.CreatePlaylistResult
import git.icyllite.twentyfour.datasources.jellyfin.models.Item
import git.icyllite.twentyfour.datasources.jellyfin.models.Lyrics
import git.icyllite.twentyfour.datasources.jellyfin.models.PlaylistItems
import git.icyllite.twentyfour.datasources.jellyfin.models.QueryResult
import git.icyllite.twentyfour.datasources.jellyfin.models.SystemInfo
import git.icyllite.twentyfour.datasources.jellyfin.models.UpdatePlaylist
import git.icyllite.twentyfour.models.SortingRule
import git.icyllite.twentyfour.models.SortingStrategy
import git.icyllite.twentyfour.utils.Api
import git.icyllite.twentyfour.utils.ApiRequest
import git.icyllite.twentyfour.utils.mapToError
import java.util.UUID

/**
 * Jellyfin client.
 *
 * @param server The base URL of the server
 * @param username The login username of the server
 * @param password The corresponding password of the user
 * @param deviceIdentifier The device identifier
 * @param packageName The package name of the app
 * @param tokenGetter A function to get the token
 * @param tokenSetter A function to set the token
 * @param cache OkHttp's [Cache]
 */
class JellyfinClient(
    server: String,
    private val username: String,
    private val password: String,
    private val deviceIdentifier: String,
    private val packageName: String,
    tokenGetter: () -> String?,
    tokenSetter: (String) -> Unit,
    cache: Cache? = null,
) {
    private val serverUri = server.toUri()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(JellyfinAuthInterceptor(tokenGetter))
        .authenticator(
            JellyfinAuthenticator(
                serverUri,
                username,
                password,
                deviceIdentifier,
                packageName,
                tokenGetter,
                tokenSetter,
            )
        )
        .cache(cache)
        .build()

    private val api = Api(okHttpClient, serverUri)

    suspend fun getAlbums(sortingRule: SortingRule) = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "IncludeItemTypes" to "MusicAlbum",
            "Recursive" to true,
        ) + getSortParameter(sortingRule),
    ).execute(api).mapToError()

    suspend fun getArtists(sortingRule: SortingRule) = ApiRequest.get<QueryResult>(
        listOf("Artists"),
        queryParameters = listOf(
            "IncludeItemTypes" to "Audio",
            "Recursive" to true,
        ) + getSortParameter(sortingRule),
    ).execute(api).mapToError()

    suspend fun getGenres(sortingRule: SortingRule) = ApiRequest.get<QueryResult>(
        listOf("Genres"),
        queryParameters = listOf(
            "IncludeItemTypes" to "Audio",
            "Recursive" to true,
        ) + getSortParameter(sortingRule),
    ).execute(api).mapToError()

    suspend fun getPlaylists(sortingRule: SortingRule) = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "IncludeItemTypes" to "Playlist",
            "Recursive" to true,
        ) + getSortParameter(sortingRule),
    ).execute(api).mapToError()

    suspend fun getItems(query: String) = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "SearchTerm" to query,
            "IncludeItemTypes" to "Playlist,MusicAlbum,MusicArtist,MusicGenre,Audio",
            "Recursive" to true,
        ),
    ).execute(api).mapToError()

    suspend fun getFavorites() = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "Filters" to "IsFavorite",
            "IncludeItemTypes" to "Audio",
            "Recursive" to true,
        ),
    ).execute(api).mapToError()

    suspend fun getAlbum(id: UUID) = getItem(id)

    suspend fun getArtist(id: UUID) = getItem(id)

    suspend fun getPlaylist(id: UUID) = getItem(id)

    suspend fun getGenre(id: UUID) = getItem(id)

    fun getAlbumThumbnail(id: UUID) = getItemThumbnail(id)

    fun getArtistThumbnail(id: UUID) = getItemThumbnail(id)

    fun getPlaylistThumbnail(id: UUID) = getItemThumbnail(id)

    fun getGenreThumbnail(id: UUID) = getItemThumbnail(id)

    suspend fun getAlbumTracks(id: UUID) = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "ParentId" to id,
            "IncludeItemTypes" to "Audio",
            "Recursive" to true,
        ),
    ).execute(api).mapToError()

    suspend fun getArtistWorks(id: UUID) = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "ArtistIds" to id,
            "IncludeItemTypes" to "MusicAlbum",
            "Recursive" to true,
        ),
    ).execute(api).mapToError()

    suspend fun getPlaylistItemIds(id: UUID) = ApiRequest.get<PlaylistItems>(
        listOf(
            "Playlists",
            id.toString(),
        ),
    ).execute(api).mapToError()

    suspend fun getPlaylistTracks(id: UUID) = ApiRequest.get<QueryResult>(
        listOf(
            "Playlists",
            id.toString(),
            "Items",
        ),
    ).execute(api).mapToError()

    suspend fun getGenreContent(id: UUID) = ApiRequest.get<QueryResult>(
        listOf("Items"),
        queryParameters = listOf(
            "GenreIds" to id,
            "IncludeItemTypes" to "MusicAlbum,Playlist,Audio",
            "Recursive" to true,
        ),
    ).execute(api).mapToError()

    suspend fun getAudio(id: UUID) = getItem(id)

    fun getAudioPlaybackUrl(id: UUID) = api.buildUrl(
        listOf(
            "Audio",
            id.toString(),
            "stream",
        ),
        queryParameters = listOf(
            "static" to true,
        ),
    )

    suspend fun getLyrics(id: UUID) = ApiRequest.get<Lyrics>(
        listOf(
            "Audio",
            id.toString(),
            "lyrics",
        ),
    ).execute(api).mapToError()

    suspend fun createPlaylist(name: String) =
        ApiRequest.post<CreatePlaylist, CreatePlaylistResult>(
            listOf("Playlists"),
            data = CreatePlaylist(
                name = name,
                ids = listOf(),
                users = listOf(),
                isPublic = true,
            ),
        ).execute(api).mapToError()

    suspend fun renamePlaylist(id: UUID, name: String) = ApiRequest.post<UpdatePlaylist, Unit>(
        listOf(
            "Playlists",
            id.toString(),
        ),
        data = UpdatePlaylist(
            name = name,
        ),
    ).execute(api).mapToError()

    suspend fun addItemToPlaylist(id: UUID, audioId: UUID) = ApiRequest.post<Any, Unit>(
        listOf(
            "Playlists",
            id.toString(),
            "Items",
        ),
        queryParameters = listOf(
            "Ids" to audioId,
        ),
    ).execute(api).mapToError()

    suspend fun removeItemFromPlaylist(id: UUID, audioId: UUID) = ApiRequest.delete<Unit>(
        listOf(
            "Playlists",
            id.toString(),
            "Items",
        ),
        queryParameters = listOf(
            "EntryIds" to audioId,
        ),
    ).execute(api).mapToError()

    suspend fun getSystemInfo() = ApiRequest.get<SystemInfo>(
        listOf(
            "System",
            "Info",
            "Public",
        ),
    ).execute(api).mapToError()

    suspend fun addToFavorites(id: UUID) = ApiRequest.post<Any, Unit>(
        listOf(
            "UserFavoriteItems",
            id.toString(),
        ),
    ).execute(api).mapToError()

    suspend fun removeFromFavorites(id: UUID) = ApiRequest.delete<Unit>(
        listOf(
            "UserFavoriteItems",
            id.toString(),
        ),
    ).execute(api).mapToError()

    private suspend fun getItem(id: UUID) = ApiRequest.get<Item>(
        listOf(
            "Items",
            id.toString(),
        ),
    ).execute(api).mapToError()

    private fun getItemThumbnail(id: UUID) = api.buildUrl(
        listOf(
            "Items",
            id.toString(),
            "Images",
            "Primary",
        ),
    )

    private fun getSortParameter(sortingRule: SortingRule) = buildList {
        add(
            "sortBy" to when (sortingRule.strategy) {
                SortingStrategy.ARTIST_NAME -> "AlbumArtist,Artist"
                SortingStrategy.CREATION_DATE -> "DateCreated"
                SortingStrategy.MODIFICATION_DATE -> "DateLastContentAdded"
                SortingStrategy.NAME -> "Name"
                SortingStrategy.PLAY_COUNT -> "PlayCount"
            }
        )

        add(
            "sortOrder" to when (sortingRule.reverse) {
                true -> "Descending"
                false -> "Ascending"
            }
        )
    }

    companion object {
        const val JELLYFIN_API_VERSION = "10.10.3"
    }
}
