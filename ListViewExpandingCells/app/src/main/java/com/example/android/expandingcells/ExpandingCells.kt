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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.expandingcells

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This activity creates a ListView whose items can be clicked to expand and show
 * additional content.
 *
 * In this specific demo, each item in a ListView displays an image and a corresponding title. These
 * two items are centered in the default (collapsed) state of the [ExpandingListView]'s item. When
 * the item is clicked, it expands to display text of some varying length. The item persists in this
 * expanded state (even if the user scrolls away and then scrolls back to the same location) until
 * it is clicked again, at which point the cell collapses back to its default state.
 *
 * See: [ListViewExpandingCells](https://www.youtube.com/watch?v=mwE61B56pVQ)
 */
class ExpandingCells : ComponentActivity() {
    /**
     * [ExpandingListView] in our layout with id `R.id.main_list_view` (it is the only widget in
     * our layout, and extends [ListView]).
     */
    private var mListView: ExpandingListView? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [ExpandingListView] variable `rootView` to the view with ID
     * `R.id.main_list_view` then call [ViewCompat.setOnApplyWindowInsetsListener] to take
     * over the policy for applying window insets to `rootView`, with the `listener` argument
     * a lambda that accepts the [View] passed the lambda in variable `v` and the
     * [WindowInsetsCompat] passed the lambda in variable `windowInsets`. It initializes its
     * [Insets] variable `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the IME
     * (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of
     * `v` to be a [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`,
     * the right margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the
     * bottom margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [Array] of [ExpandableListItem] variable `val values` with three different
     * instances. We initialize our [MutableList] of [ExpandableListItem] variable `val mData` with
     * a new instance of [ArrayList]. We then loop over [Int] variable `var i` adding [NUM_OF_CELLS]
     * (30) of [ExpandableListItem] objects constructed by copying the contents of entries of
     * `values` in a round robin order. We initialize [CustomArrayAdapter] variable `val adapter`
     * with a new instance which uses `mData` as its dataset, and the layout file
     * `R.layout.list_view_item` to display each item in that dataset. We initialize
     * [ExpandingListView] field [mListView] to `rootView`, set its adapter to be `adapter` and
     * call its [ExpandingListView.setDivider] method (kotlin `divider` property) to set its divider
     * to `null`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<ExpandingListView>(R.id.main_list_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        val values: Array<ExpandableListItem> = arrayOf(
            ExpandableListItem(
                "Chameleon", R.drawable.chameleon, CELL_DEFAULT_HEIGHT,
                resources.getString(R.string.short_lorem_ipsum)
            ),
            ExpandableListItem(
                "Rock", R.drawable.rock, CELL_DEFAULT_HEIGHT,
                resources.getString(R.string.medium_lorem_ipsum)
            ),
            ExpandableListItem(
                "Flower", R.drawable.flower, CELL_DEFAULT_HEIGHT,
                resources.getString(R.string.long_lorem_ipsum)
            )
        )
        val mData: MutableList<ExpandableListItem> = ArrayList()
        for (i in 0 until NUM_OF_CELLS) {
            val obj = values[i % values.size]
            mData.add(
                ExpandableListItem(
                    obj.title, obj.imgResource,
                    obj.collapsedHeight, obj.text
                )
            )
        }
        val adapter = CustomArrayAdapter(
            context = this,
            mLayoutViewResourceId = R.layout.list_view_item,
            mData = mData
        )
        mListView = rootView
        mListView!!.adapter = adapter
        mListView!!.divider = null
    }

    companion object {
        /**
         * Used as the collapsed height of the [ExpandableListItem] objects we create.
         */
        private const val CELL_DEFAULT_HEIGHT = 200

        /**
         * Number of [ExpandableListItem] objects we add to our dataset.
         */
        private const val NUM_OF_CELLS = 30
    }
}
