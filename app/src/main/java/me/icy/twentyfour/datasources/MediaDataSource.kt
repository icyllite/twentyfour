/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.datasources

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import me.icy.twentyfour.models.ActivityTab
import me.icy.twentyfour.models.Album
import me.icy.twentyfour.models.Artist
import me.icy.twentyfour.models.ArtistWorks
import me.icy.twentyfour.models.Audio
import me.icy.twentyfour.models.DataSourceInformation
import me.icy.twentyfour.models.Error
import me.icy.twentyfour.models.Genre
import me.icy.twentyfour.models.GenreContent
import me.icy.twentyfour.models.Lyrics
import me.icy.twentyfour.models.MediaItem
import me.icy.twentyfour.models.MediaType
import me.icy.twentyfour.models.Playlist
import me.icy.twentyfour.models.Result
import me.icy.twentyfour.models.SortingRule

typealias MediaRequestStatus<T> = Result<T, Error>

/**
 * A data source for media.
 */
interface MediaDataSource {
    /**
     * Get the current status of the data source.
     *
     * @return [Result.Success] with a list of [DataSourceInformation] if everything is fine,
     *   else [Result.Error]
     */
    fun status(): Flow<MediaRequestStatus<List<DataSourceInformation>>>

    /**
     * Given a compatible media item URI, get its type.
     *
     * @param mediaItemUri The media item to check
     * @return A [MediaType] if success, null if this media item cannot be handled
     */
    suspend fun mediaTypeOf(mediaItemUri: Uri): MediaType?

    /**
     * Home page content.
     */
    fun activity(): Flow<MediaRequestStatus<List<ActivityTab>>>

    /**
     * Get all the albums. All albums must have at least one audio associated with them.
     */
    fun albums(sortingRule: SortingRule): Flow<MediaRequestStatus<List<Album>>>

    /**
     * Get all the artists. All artists must have at least one audio associated with them.
     */
    fun artists(sortingRule: SortingRule): Flow<MediaRequestStatus<List<Artist>>>

    /**
     * Get all the genres. All genres must have at least one audio associated with them.
     */
    fun genres(sortingRule: SortingRule): Flow<MediaRequestStatus<List<Genre>>>

    /**
     * Get all the playlists. A playlist can be empty.
     */
    fun playlists(sortingRule: SortingRule): Flow<MediaRequestStatus<List<Playlist>>>

    /**
     * Start a search for the given query.
     * Only the following items can be returned: [Album], [Artist], [Audio], [Genre], [Playlist].
     */
    fun search(query: String): Flow<MediaRequestStatus<List<MediaItem<*>>>>

    /**
     * Get the audio information of the given audio.
     */
    fun audio(audioUri: Uri): Flow<MediaRequestStatus<Audio>>

    /**
     * Get the album information and all the tracks of the given album.
     */
    fun album(albumUri: Uri): Flow<MediaRequestStatus<Pair<Album, List<Audio>>>>

    /**
     * Get the artist information and all the works associated with them.
     */
    fun artist(artistUri: Uri): Flow<MediaRequestStatus<Pair<Artist, ArtistWorks>>>

    /**
     * Get the genre information and all the tracks of the given genre.
     */
    fun genre(genreUri: Uri): Flow<MediaRequestStatus<Pair<Genre, GenreContent>>>

    /**
     * Get the playlist information and all the tracks of the given playlist.
     */
    fun playlist(playlistUri: Uri): Flow<MediaRequestStatus<Pair<Playlist, List<Audio>>>>

    /**
     * Get an audio status within all playlists.
     * @param audioUri The URI of the audio
     */
    fun audioPlaylistsStatus(audioUri: Uri): Flow<MediaRequestStatus<List<Pair<Playlist, Boolean>>>>

    /**
     * Get the lyrics of an audio.
     * @param audioUri The URI of the audio
     */
    fun lyrics(audioUri: Uri): Flow<Result<Lyrics, Error>>

    /**
     * Create a new playlist. Note that the name shouldn't be considered unique if possible, but
     * this may vary per data source.
     * @param name The name of the playlist
     * @return A [Result] with the [Uri] of the new playlist if succeeded, an error otherwise
     */
    suspend fun createPlaylist(name: String): MediaRequestStatus<Uri>

    /**
     * Rename a playlist.
     * @param playlistUri The URI of the playlist
     * @param name The new name of the playlist
     * @return [Result.Success] if success, [Result.Error] with an error otherwise
     */
    suspend fun renamePlaylist(playlistUri: Uri, name: String): MediaRequestStatus<Unit>

    /**
     * Delete a playlist.
     * @param playlistUri The URI of the playlist
     * @return [Result.Success] if success, [Result.Error] with an error otherwise
     */
    suspend fun deletePlaylist(playlistUri: Uri): MediaRequestStatus<Unit>

    /**
     * Add an audio to a playlist.
     * @param playlistUri The URI of the playlist
     * @param audioUri The URI of the audio
     * @return [Result.Success] if success, [Result.Error] with an error otherwise
     */
    suspend fun addAudioToPlaylist(playlistUri: Uri, audioUri: Uri): MediaRequestStatus<Unit>

    /**
     * Remove an audio from a playlist.
     * @param playlistUri The URI of the playlist
     * @param audioUri The URI of the audio
     * @return [Result.Success] if success, [Result.Error] with an error otherwise
     */
    suspend fun removeAudioFromPlaylist(playlistUri: Uri, audioUri: Uri): MediaRequestStatus<Unit>

    /**
     * Notify the source about an audio item being played.
     * @param audioUri The URI of the audio
     * @return [Result.Success] if success, [Result.Error] with an error otherwise
     */
    suspend fun onAudioPlayed(audioUri: Uri): MediaRequestStatus<Unit>

    /**
     * Set the favorite status of an audio.
     * @param audioUri The URI of the audio
     * @param isFavorite The new favorite status
     * @return [Result.Success] if success, [Result.Error] with an error otherwise
     */
    suspend fun setFavorite(audioUri: Uri, isFavorite: Boolean): MediaRequestStatus<Unit>
}
