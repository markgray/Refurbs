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

package com.example.android.listviewremovalanimation

import android.content.Context
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ArrayAdapter

/**
 * The stable `ArrayAdapter` used by our `ListView`
 * Our constructor. First we call our super's 3 argument constructor, then we save our parameter
 * `OnTouchListener listener` in our field `mTouchListener`. We then loop over `int i`
 * for all of the strings in our parameter `List<String> objects` storing `i` under the
 * key of the string fetched from position `i` in `objects` in our field
 * `HashMap<String, Integer> mIdMap`.
 *
 * @param context            The current context.
 * @param textViewResourceId The resource ID for a layout file containing a TextView to use when
 * instantiating views.
 * @param objects            The objects to represent in the ListView.
 * @param mTouchListener           `OnTouchListener` to use for each item view that our
 * `getView` override returns.
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

    /**
     *
     */
    init {
        for (i in objects.indices) {
            mIdMap[objects[i]] = i
        }
    }

    /**
     * Returns the row id associated with the specified position in the list. We initialize `String item`
     * with the string at position `position` in our dataset, then return the `Integer`
     * stored under the key `item` in our field `HashMap<String, Integer> mIdMap`.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return mIdMap[item]!!.toLong()
    }

    /**
     * Indicates whether the item ids are stable across changes to the underlying data. Our item ids
     * are stable, so we return true.
     *
     * @return True if the same id always refers to the same object.
     */
    override fun hasStableIds(): Boolean {
        return true
    }

    /**
     * Get a View that displays the data at the specified position in the data set. We initialize
     * `View view` with the `View` that our super's implementation of `getView`
     * returns, and if this `View` is not the view passed us in `View convertView` we
     * set its `OnTouchListener` to our field `OnTouchListener`. Finally we return
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