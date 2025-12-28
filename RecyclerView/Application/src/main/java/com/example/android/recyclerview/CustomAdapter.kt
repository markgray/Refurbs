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
package com.example.android.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.android.common.logger.Log

/**
 * Provide views to RecyclerView with data from mDataSet. Our constructor. We just initialize the
 * the [Array] of [String] dataset [mDataSet] of the Adapter to our parameter.
 *
 * @param mDataSet the [Array] of [String] containing the data to populate views to be used by
 * [RecyclerView].
 *  */
class CustomAdapter(
    /**
     * Our dataset field.
     */
    private val mDataSet: Array<String?>
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom [ViewHolder]).
     * Our constructor. First we call our super's constructor, then in our `init` block we  set the
     * [View.OnClickListener] of our [View] parameter [v] to an anonymous class whose
     * [View.OnClickListener.onClick] override logs the position in the adapter of the view that
     * was clicked, and toasts the same message. Finally we initialize our [TextView] field
     * [textView] by finding the view with id `R.id.textView` in [v].
     *
     * @param v the [View] we are to "hold"
     */
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        /**
         * [TextView] in our view with ID `R.id.textView`.
         */
        val textView: TextView

        init {
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener { viewClicked: View ->

                /**
                 * Called when our [View] parameter [v] is clicked. We initialize [String] variable
                 * `msg` with a string formed by concatenating the string "Element " followed by the
                 * Adapter position of the item represented by this ViewHolder, followed by the
                 * string " clicked.". We then log `msg` and toast it to the user as well.
                 *
                 * param `viewClicked` View that was clicked.
                 */
                val msg = "Element $bindingAdapterPosition clicked."
                Log.d(TAG, msg)
                Toast.makeText(viewClicked.context, msg, Toast.LENGTH_LONG).show()
            }
            textView = v.findViewById(R.id.textView)
        }
    }

    /**
     * Called when [RecyclerView] needs a new [RecyclerView.ViewHolder] of the given type to
     * represent an item. Create new views (invoked by the layout manager). We initialize our
     * [View] variable `v` to the [View] that the [LayoutInflater] from the [ViewGroup.getContext]
     * of our [ViewGroup] parameter [viewGroup] inflates from our layout file `R.layout.text_row_item`
     * using [viewGroup] for the layout parameters without attaching to it. Then we return a
     * [ViewHolder] instance constructed to hold `v` to the caller.
     *
     * @param viewGroup The [ViewGroup] into which the new [View] will be added after it is bound to
     * an adapter position.
     * @param viewType  The view type of the new [View].
     * @return A new [ViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view.
        val v: View = LayoutInflater.from(viewGroup.context)
            .inflate(
                /* resource = */ R.layout.text_row_item,
                /* root = */ viewGroup,
                /* attachToRoot = */ false
            )
        return ViewHolder(v = v)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the [RecyclerView.ViewHolder.itemView] to reflect the item at the
     * given position. Replace the contents of a view (invoked by the layout manager). First we log
     * the [Int] parameter [position] of the item within the adapter's data set. Then we fetch the
     * [TextView] of our [ViewHolder] parameter [viewHolder] and set its text to the string at
     * [position] in our [Array] of [String] dataset [mDataSet].
     *
     * @param viewHolder The [ViewHolder] which should be updated to represent the contents of the
     * item at the given [position] in the data set.
     * @param position   The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        Log.d(TAG, "Element $position set.")

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.textView.text = mDataSet[position]
    }

    /**
     * Returns the total number of items in the data set held by the adapter. We just return the
     * length of our [Array] of [String] dataset field [mDataSet].
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return mDataSet.size
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "CustomAdapter"
    }
}
