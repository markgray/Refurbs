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

package com.example.android.listviewdragginganimation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

/**
 * Our stable id's `ArrayAdapter<String>` subclass.
 */

/**
 * Our constructor. First we call our super's constructor, then we loop over `int i` for the
 * size of our parameter `List<String> objects` storing the current value of `i` using
 * the string in position `i` of `objects` as the key.
 *
 * @param context            The current context.
 * @param textViewResourceId The resource ID for a layout file containing a TextView to use when
 * instantiating views.
 * @param objects            The objects to represent in the ListView.
 */
class StableArrayAdapter(
    context: Context?,
    textViewResourceId: Int,
    objects: List<String?>
) : ArrayAdapter<String?>(context!!, textViewResourceId, objects) {

    /**
     * Maps strings in our dataset to a unique id.
     */
    var mIdMap: HashMap<String?, Int> = HashMap()

    init {
        for (i in objects.indices) {
            mIdMap[objects[i]] = i
        }
    }

    /**
     * Get the row id associated with the specified position in the list. If our parameter `position`
     * is less than 0 or greater than or equal to the size of our field `HashMap<String, Integer> mIdMap`
     * we just return INVALID_ID (the item's position is outside of the bounds of our dataset). Otherwise
     * we initialize `String item` with the item in position `position` in our dataset then
     * return the `Integer` stored under the key `item` in `mIdMap` to the caller.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= mIdMap.size) {
            return INVALID_ID.toLong()
        }
        val item = getItem(position)
        return mIdMap[item]!!.toLong()
    }

    /**
     * Indicates whether the item ids are stable across changes to the underlying data. We return true,
     * our id's are stable.
     *
     * @return True if the same id always refers to the same object.
     */
    override fun hasStableIds(): Boolean {
        return true
    }

    /**
     * Get a View that displays the data at the specified position in the data set. If our parameter
     * `View convertView` is not null we set its visibility to VISIBLE (just in case it is a
     * recycled view which was made invisible). Then we return the view our super's implementation
     * of `getView` returns to the caller.
     *
     * @param position The position of the item within the adapter's data set whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (convertView != null) {
            convertView.visibility = View.VISIBLE
        }
        return super.getView(position, convertView, parent)
    }

    companion object {
        /**
         * Item id value we return if the position that our `getItemId` override is called for is
         * outside the bounds of the `HashMap<String, Integer> mIdMap` we use to map strings in our
         * dataset to their id's.
         */
        const val INVALID_ID: Int = -1
    }
}