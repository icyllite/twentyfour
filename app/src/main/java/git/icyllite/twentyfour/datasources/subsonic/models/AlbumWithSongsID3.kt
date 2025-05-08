/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.subsonic.models

import androidx.annotation.IntRange
import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class AlbumWithSongsID3(
    // AlbumID3 start

    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int,
    val duration: Int,
    val playCount: Long? = null,
    val created: InstantAsString,
    val starred: InstantAsString? = null,
    val year: Int? = null,
    val genre: String? = null,

    // OpenSubsonic
    val played: InstantAsString? = null,
    @IntRange(from = 1, to = 5) val userRating: Int? = null,
    val recordLabels: List<RecordLabel>? = null,
    val musicBrainzId: String? = null,
    val genres: List<ItemGenre>? = null,
    val artists: List<ArtistID3>? = null,
    val displayArtist: String? = null,
    val releaseTypes: List<String>? = null,
    val moods: List<String>? = null,
    val sortName: String? = null,
    val originalReleaseDate: ItemDate? = null,
    val releaseDate: ItemDate? = null,
    val isCompilation: Boolean? = null,
    val discTitles: List<DiscTitle>? = null,

    // Navidrome
    val album: String? = null,
    val bpm: Int? = null,
    val channelCount: Int? = null,
    val comment: String? = null,
    val isDir: Boolean? = null,
    val isVideo: Boolean? = null,
    val mediaType: String? = null,
    val parent: String? = null,
    val replayGain: ReplayGain? = null,
    val samplingRate: Int? = null,
    val title: String? = null,

    // AlbumID3 end

    val song: List<Child>,
) {
    fun toAlbumID3() = AlbumID3(
        id = id,
        name = name,
        artist = artist,
        artistId = artistId,
        coverArt = coverArt,
        songCount = songCount,
        duration = duration,
        playCount = playCount,
        created = created,
        starred = starred,
        year = year,
        genre = genre,
        played = played,
        userRating = userRating,
        recordLabels = recordLabels,
        musicBrainzId = musicBrainzId,
        genres = genres,
        artists = artists,
        displayArtist = displayArtist,
        releaseTypes = releaseTypes,
        moods = moods,
        sortName = sortName,
        originalReleaseDate = originalReleaseDate,
        releaseDate = releaseDate,
        isCompilation = isCompilation,
        discTitles = discTitles,
        album = album,
        bpm = bpm,
        channelCount = channelCount,
        comment = comment,
        isDir = isDir,
        isVideo = isVideo,
        mediaType = mediaType,
        parent = parent,
        replayGain = replayGain,
        samplingRate = samplingRate,
        title = title,
    )
}
