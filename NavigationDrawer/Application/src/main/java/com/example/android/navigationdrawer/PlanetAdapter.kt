/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.navigationdrawer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the planet data used in our drawer menu. Our constructor just initializes its fields
 * from its parameters.
 *
 * @param mDataset dataset containing list of planet names in an [Array] of [String]
 * @param mListener the [OnItemClickListener] whose [OnItemClickListener.onClick] override is
 * called whenever an item in our list is clicked.
 */
class PlanetAdapter(
    /**
     * Our data set of planet names.
     */
    val mDataset: Array<String>,
    /**
     * [OnItemClickListener] we are constructed to use, its [OnItemClickListener.onClick] override
     * is called from the [OnClickListener.onClick] override of each [TextView] in the [ViewHolder]
     * for the data items in our [RecyclerView]
     */
    val mListener: OnItemClickListener
) : RecyclerView.Adapter<PlanetAdapter.ViewHolder>() {
    /**
     * Interface for receiving click events from cells.
     */
    interface OnItemClickListener {
        /**
         * Called from the [OnClickListener.onClick] override of each TextView in the [ViewHolder]
         * for the data items in our [RecyclerView]
         *
         * @param view the [View] that was clicked
         * @param position position in our dataset that was selected
         */
        fun onClick(view: View, position: Int)
    }

    /**
     * Custom [ViewHolder] for our planet views. Our constructor just initializes our field to its
     * parameter.
     *
     * @param mTextView the [TextView] displaying the planet name
     */
    class ViewHolder(
        /**
         * [TextView] in the view we inflate from `R.layout.drawer_list_item` which displays the
         * planet name, and whose [OnClickListener] calls the [OnItemClickListener.onClick] override
         * of our [OnItemClickListener] field  [mListener]
         */
        val mTextView: TextView
    ) : RecyclerView.ViewHolder(mTextView)

    /**
     * Called when [RecyclerView] needs a new [RecyclerView.ViewHolder] of the given type to
     * represent an item. We initialize [LayoutInflater] variable `val vi` with the [LayoutInflater]
     * from the context of our [ViewGroup] parameter [parent] then use it to initialize [View]
     * variable `val v` with the view it inflates from our layout file `R.layout.drawer_list_item`
     * using [parent] for the layout params without attaching to it. We initialize [TextView]
     * variable `val tv` by finding the view in `v` with id [android.R.id.text1], then return a
     * [ViewHolder] constructed from `tv` to the caller.
     *
     * @param parent The [ViewGroup] into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [ViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vi = LayoutInflater.from(parent.context)
        val v = vi.inflate(R.layout.drawer_list_item, parent, false)
        val tv = v.findViewById<TextView>(android.R.id.text1)
        return ViewHolder(tv)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the [RecyclerView.ViewHolder.itemView] to reflect the item at the
     * given position. We set the text of the [TextView] field [ViewHolder.mTextView] field of our
     * [ViewHolder] parameter [holder] to the string at the index of our [Int] parameter [position]
     * in our [Array] of [String] dataset field [mDataset], then set its [OnClickListener] to an
     * anonymous class whose [OnClickListener.onClick] override calls the [OnItemClickListener.onClick]
     * override of our [OnItemClickListener] field [mListener].
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTextView.text = mDataset[position]
        holder.mTextView.setOnClickListener { view: View ->
            /**
             * Called when the [TextView] is clicked, we just call the [OnItemClickListener.onClick]
             * override of our [OnItemClickListener] field [mListener].
             *
             * @param view the [View] that was clicked
             */
            mListener.onClick(view, position)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter, which is the size of
     * our [Array] of [String] field [mDataset].
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return mDataset.size
    }
}
