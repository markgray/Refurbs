/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.customchoicelist

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This sample demonstrates how to create custom single- or multi-choice
 * [ListView] UIs. The most interesting bits are in
 * the `res/layout/` directory of this sample.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.sample_main` (which consists of a [LinearLayout] containing
     * a [TextView] which explains the example and a [ListView] with id "@android:id/list" which
     * this [Activity] will use to display its list of Cheeses).
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `insets` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument, then it updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `insets.left`, the right margin
     * set to `insets.right`, the top margin set to `insets.top`, and the bottom margin set to
     * `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that
     * the window insets will not keep passing down to descendant views).
     *
     * Finally we use [findViewById] to find the [ListView] with ID [android.R.id.list] and call
     * its [ListView.setAdapter] method (aka kotlin `adapter` property) with a new instance of
     * [MyAdapter].
     *
     * @param savedInstanceState We do not override `onSaveInstanceState` so do not use.
     */
    @Suppress("ReplaceNotNullAssertionWithElvisReturn")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top+actionBar!!.height/2
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        findViewById<ListView>(android.R.id.list).adapter = MyAdapter()
    }

    /**
     * A simple array adapter that creates a list of cheeses.
     */
    private inner class MyAdapter : BaseAdapter() {
        /**
         * How many items are in the data set represented by this Adapter. This is just the length
         * of the [Array] of [String] field [Cheeses.CHEESES].
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            return Cheeses.CHEESES.size
        }

        /**
         * Get the data item associated with the specified position in the data set. We return the
         * [String] at position [position] in the [Array] of [String] field [Cheeses.CHEESES].
         *
         * @param position Position of the item whose data we want within the adapter's data set.
         * @return The data at the specified position.
         */
        override fun getItem(position: Int): String {
            return Cheeses.CHEESES[position]
        }

        /**
         * Get the row id associated with the specified position in the list. We return the hashcode
         * of the [String] at position [position] in the [Array] of [String] field [Cheeses.CHEESES].
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            return Cheeses.CHEESES[position].hashCode().toLong()
        }

        /**
         * Get a View that displays the data at the specified position in the data set. If our [View]
         * parameter [convertView] is `null` we set it to the view that the [LayoutInflater] for our
         * activities window inflates from the layout file `R.layout.list_item` using our [ViewGroup]
         * parameter [container] for the LayoutParams without attaching to it (this layout file
         * consists of a custom [CheckableLinearLayout] which is central to this example). We
         * find the [TextView] in [convertView] with id [android.R.id.text1] and set its text
         * to the [String] returned by our [getItem] method for position [position] and return
         * [convertView] to the caller.
         *
         * @param position The position of the item within the adapter's data set whose view we want.
         * @param convertView The old view to reuse, if possible.
         * @param container The parent that this view will eventually be attached to
         * @return A [View] corresponding to the data at the specified position.
         */
        override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
            var convertViewLocal: View? = convertView
            if (convertViewLocal == null) {
                convertViewLocal = layoutInflater.inflate(R.layout.list_item, container, false)
            }
            (convertViewLocal!!.findViewById<View>(android.R.id.text1) as TextView).text = getItem(position)
            return convertViewLocal
        }
    }
}
