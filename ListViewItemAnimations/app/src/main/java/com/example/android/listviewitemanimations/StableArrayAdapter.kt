/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.listviewitemanimations

import android.content.Context
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

/**
 * The stable [ArrayAdapter] used by our [ListView].
 *
 * Our constructor. First we call our super's 3 argument constructor, then in our `init` block we
 * loop over [Int] variable `var i` for all of the strings in our [List] of [String] parameter
 * [objects] storing `i` under the key of the [String] fetched from position `i` in [objects] in
 * our [HashMap] of [String] to [Int] field [mIdMap].
 *
 * @param context            The current [Context].
 * @param textViewResourceId The resource ID for a layout file containing a [TextView] to use when
 * instantiating views.
 * @param objects            The objects to represent in the [ListView].
 * @param mTouchListener     [OnTouchListener] to use for each item view that our [getView]
 * override returns.
 */
class StableArrayAdapter(
    context: Context?,
    textViewResourceId: Int,
    objects: List<String?>,
    var mTouchListener: OnTouchListener
) : ArrayAdapter<String?>(context!!, textViewResourceId, objects) {
    /**
     * Map of the cheese strings in our dataset to an unique stable id assigned to each.
     */
    var mIdMap: HashMap<String?, Int> = HashMap()

    init {
        for (i in objects.indices) {
            mIdMap[objects[i]] = i
        }
    }

    /**
     * Returns the row id associated with the specified position in the list. We initialize [String]
     * variable `val item` with the string at position [position] in our dataset, then return the
     * [Int] stored under the key `item` in our [HashMap] of [String] to [Int] field [mIdMap].
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    override fun getItemId(position: Int): Long {
        val item: String? = getItem(position)
        return mIdMap[item]!!.toLong()
    }

    /**
     * Indicates whether the item ids are stable across changes to the underlying data. Our item ids
     * are stable, so we return `true`.
     *
     * @return `true` if the same id always refers to the same object.
     */
    override fun hasStableIds(): Boolean {
        return true
    }

    /**
     * Get a [View] that displays the data at the specified position in the data set. We initialize
     * [View] variable `val view` with the [View] that our super's implementation of `getView`
     * returns, and if this [View] is not the view passed us in [View] parameter [convertView] we
     * set its [OnTouchListener] to our [OnTouchListener] field [mTouchListener]. Finally we return
     * `view` to the caller.
     *
     * @param position The position of the item within the adapter's data set whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (view !== convertView) {
            // Add touch listener to every new view to track swipe motion
            view.setOnTouchListener(mTouchListener)
        }
        return view
    }
}