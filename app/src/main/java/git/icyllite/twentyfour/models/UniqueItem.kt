/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.models

import kotlin.reflect.safeCast

/**
 * An item that can be uniquely identified.
 */
interface UniqueItem<T> {
    /**
     * Return whether this item is the same as the other.
     */
    fun areItemsTheSame(other: T): Boolean

    /**
     * Return whether this item has the same content as the other.
     * This is called only when [areItemsTheSame] returns true.
     */
    fun areContentsTheSame(other: T): Boolean
}

/**
 * @see UniqueItem.areItemsTheSame
 */
inline fun <reified T : Any> UniqueItem<T>.areItemsTheSame(other: Any?): Boolean =
    T::class.safeCast(other)?.let { areItemsTheSame(it) } ?: false

/**
 * @see UniqueItem.areContentsTheSame
 */
inline fun <reified T : Any> UniqueItem<T>.areContentsTheSame(other: Any?): Boolean =
    T::class.safeCast(other)?.let { areContentsTheSame(it) } ?: false
