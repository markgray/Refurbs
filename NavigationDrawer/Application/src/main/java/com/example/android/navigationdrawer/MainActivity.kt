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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.navigationdrawer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * A simple launcher activity offering access to the individual samples in this project.
 */
class MainActivity : ComponentActivity(), AdapterView.OnItemClickListener {
    /**
     * Data set of the sample activities we can launch, in our case only 1 [Sample] which
     * launches the [NavigationDrawerActivity] activity.
     */
    private lateinit var mSamples: Array<Sample>

    /**
     * [GridView] in our layout file with ID [android.R.id.list]
     */
    private var mGridView: GridView? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID
     * `R.id.root_view` then call [ViewCompat.setOnApplyWindowInsetsListener] to
     * take over the policy for applying window insets to `rootView`, with the
     * `listener` argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views)
     *
     * We initialize [Array] of [Sample] field [mSamples] with a new instance containing one
     * [Sample] which is constructed to display the string with resource id
     * `R.string.navigationdraweractivity_title` as the title,
     * `R.string.navigationdraweractivity_description` as the description and the class
     * of [NavigationDrawerActivity] as the hard-coded activity that will be launched if the
     * item is clicked. We initialize our [GridView] field [mGridView] by finding the view with
     * ID [android.R.id.list], set its adapter to a new instance of [SampleAdapter], and set its
     * [AdapterView.OnItemClickListener] to this.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        // TODO: Move text down autmatically
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top+actionBar!!.height
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // Prepare list of samples in this dashboard.
        mSamples = arrayOf(
            Sample(R.string.navigationdraweractivity_title, R.string.navigationdraweractivity_description,
                NavigationDrawerActivity::class.java))

        // Prepare the GridView
        mGridView = findViewById(android.R.id.list)
        mGridView!!.adapter = SampleAdapter()
        mGridView!!.onItemClickListener = this
    }

    /**
     * Callback method to be invoked when an item in our [GridView] field [mGridView] has been
     * clicked. We call the method [startActivity] to launch the activity specified by the [Intent]
     * in the [Sample.intent] field of the [Sample] in the entry of our [Array] of [Sample] field
     * [mSamples] whose index is our [Int] parameter [position].
     *
     * @param container The AdapterView where the click happened.
     * @param view The view within the `GridView` that was clicked
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was clicked.
     */
    override fun onItemClick(container: AdapterView<*>?, view: View, position: Int, id: Long) {
        startActivity(mSamples[position].intent)
    }

    /**
     * The adapter for our [GridView] field [mGridView].
     */
    private inner class SampleAdapter : BaseAdapter() {
        /**
         * How many items are in the data set represented by this Adapter. This is just the size
         * of our [Array] of [Sample] field [mSamples].
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            return mSamples.size
        }

        /**
         * Get the data item associated with the specified position in the data set. We return the
         * entry at index [position] in our [Array] of [Sample] field [mSamples].
         *
         * @param position Position of the item whose data we want within the adapter's data set.
         * @return The data at the specified position.
         */
        override fun getItem(position: Int): Any {
            return mSamples[position]
        }

        /**
         * Get the row id associated with the specified position in the list. We return the hashcode
         * of the entry at index [position] of our [Array] of [Sample] field [mSamples].
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            return mSamples[position].hashCode().toLong()
        }

        /**
         * Get a View that displays the data at the specified position in the data set. If our [View]
         * parameter [convertView] is `null`, we set it to the view that the [LayoutInflater] instance
         * for our context inflates from the layout file `R.layout.sample_dashboard_item` using our
         * [ViewGroup] parameter [container] for the layout params without attaching to it. We find
         * the view in [convertView] with id [android.R.id.text1] and set its text to the [String]
         * whose resource id is found in the [Sample.titleResId] field of the entry in position
         * [position] in our [Array] of [Sample] field [mSamples], and we find the view in [convertView]
         * with resource id [android.R.id.text2] and set its text to the [String] whose resource id
         * is found in the [Sample.descriptionResId] field of the entry in position [position] of
         * our [Array] of [Sample] field [mSamples]. Finally we return [convertView] to the caller.
         *
         * @param position The position of the item within the adapter's data set whose view we want.
         * @param convertView The old view to reuse, if possible.
         * @param container The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
            var convertViewLocal = convertView
            if (convertViewLocal == null) {
                convertViewLocal = layoutInflater.inflate(
                    R.layout.sample_dashboard_item,
                    container,
                    false
                )
            }
            (convertViewLocal!!
                .findViewById<View>(android.R.id.text1) as TextView)
                .setText(mSamples[position].titleResId)
            (convertViewLocal
                .findViewById<View>(android.R.id.text2) as TextView)
                .setText(mSamples[position].descriptionResId)
            return convertViewLocal
        }
    }

    /**
     * Class that holds the title, description and [Intent] for an activity that the user can
     * launch by clicking on its entry in our [GridView] field [mGridView].
     */
    private inner class Sample
    /**
     * This constructor just saves its arguments in the fields of the same name.
     *
     * @param titleResId Resource id for the string to use as the title of this [Sample]
     * @param descriptionResId Resource id for the string to use as the description of this [Sample]
     * @param intent the [Intent] that can be used to launch the activity of this [Sample]
     */
    private constructor(
        /**
         * Resource id for the string to use as the title of this [Sample]
         */
        var titleResId: Int,
        /**
         * Resource id for the string to use as the description of this [Sample]
         */
        var descriptionResId: Int,
        /**
         * `Intent` that can be used to launch the activity of this [Sample]
         */
        var intent: Intent) {
        /**
         * This constructor creates an intent for the specific component specified by its
         * [activityClass] parameter and then calls our other constructor using that [Intent]
         * for its [Intent] parameter [intent].
         *
         * @param titleResId Resource id for the string to use as the title of this [Sample]
         * @param descriptionResId Resource id for the string to use as the description of this [Sample]
         * @param activityClass the [Class] of the [Activity] our [Intent] field should launch
         */
        constructor(
            titleResId: Int,
            descriptionResId: Int,
            activityClass: Class<out Activity>
        ) : this(titleResId, descriptionResId, Intent(this@MainActivity, activityClass))
    }
}
