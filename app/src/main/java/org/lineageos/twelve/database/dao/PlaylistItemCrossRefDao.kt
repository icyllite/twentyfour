/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
@Suppress("FunctionName")
interface PlaylistItemCrossRefDao {
    /**
     * Add an item to a playlist (creates a cross-reference).
     */
    @Transaction
    @Query(
        """
            INSERT INTO PlaylistItemCrossRef (playlist_id, item_id, last_modified)
            VALUES (:playlistId, :itemId, :lastModified)
        """
    )
    suspend fun _addItemToPlaylist(
        playlistId: Long,
        itemId: Long,
        lastModified: Long = System.currentTimeMillis()
    )

    /**
     * Remove an item from a playlist (deletes the cross-reference).
     */
    @Query("DELETE FROM PlaylistItemCrossRef WHERE playlist_id = :playlistId AND item_id = :itemId")
    suspend fun _removeItemFromPlaylist(playlistId: Long, itemId: Long)
}
