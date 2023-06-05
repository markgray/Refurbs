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
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView

/**
 * A simple launcher activity offering access to the individual samples in this project.
 */
class MainActivity : Activity(), AdapterView.OnItemClickListener {
    /**
     * Data set of the sample activities we can launch, in our case only 1 `Sample` which
     * launches the `NavigationDrawerActivity` activity.
     */
    private lateinit var mSamples: Array<Sample>

    /**
     * `GridView` in our layout file with ID android.R.id.list
     */
    private var mGridView: GridView? = null

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_main. We
     * initialize `Sample[] mSamples` with a new instance containing one `Sample` which
     * is constructed to display the string with resource id R.string.navigationdraweractivity_title
     * as the title, R.string.navigationdraweractivity_description as the description and the class
     * of `NavigationDrawerActivity` as the hard-coded activity that will be launched if the
     * item is clicked. We initialize our field `GridView mGridView` by finding the view with
     * ID android.R.id.list, set its adapter to a new instance of `SampleAdapter`, and set its
     * `OnItemClickListener` to this.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
     * Callback method to be invoked when an item in our `GridView mGridView` has been clicked.
     * We call the method `startActivity` to launch the activity specified by the `Intent`
     * in the `intent` field of the `Sample` in the `position` entry of our field
     * `Sample[] mSamples`.
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
     * The adapter for our `GridView mGridView`.
     */
    private inner class SampleAdapter : BaseAdapter() {
        /**
         * How many items are in the data set represented by this Adapter. This is just the length
         * of our array `Sample[] mSamples`.
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            return mSamples.size
        }

        /**
         * Get the data item associated with the specified position in the data set. We return the
         * entry in position `position` of our array `Sample[] mSamples`.
         *
         * @param position Position of the item whose data we want within the adapter's data set.
         * @return The data at the specified position.
         */
        override fun getItem(position: Int): Any {
            return mSamples[position]
        }

        /**
         * Get the row id associated with the specified position in the list. We return the hashcode
         * of the entry in position `position` of our array `Sample[] mSamples`.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            return mSamples[position].hashCode().toLong()
        }

        /**
         * Get a View that displays the data at the specified position in the data set. If our parameter
         * `convertView` is null, we set it to the view that the `LayoutInflater` instance
         * for our context inflates from the layout file R.layout.sample_dashboard_item using our parameter
         * `container` for the layout params without attaching to it. We find the view in `convertView`
         * with id android.R.id.text1 and set its text to the resource id stored in the `titleResId`
         * field of the entry in position `position` of our array `Sample[] mSamples`, and
         * we find the view in `convertView` with id android.R.id.text2 and set its text to the
         * resource id stored in the `descriptionResId` field of the entry in position `position`
         * of our array `Sample[] mSamples`. Finally we return `convertView` to the caller.
         *
         * @param position The position of the item within the adapter's data set whose view we want.
         * @param convertView The old view to reuse, if possible.
         * @param container The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
            var convertViewLocal = convertView
            if (convertViewLocal == null) {
                convertViewLocal = layoutInflater
                    .inflate(R.layout.sample_dashboard_item, container, false)
            }
            (convertViewLocal!!.findViewById<View>(android.R.id.text1) as TextView).setText(
                mSamples[position].titleResId)
            (convertViewLocal.findViewById<View>(android.R.id.text2) as TextView).setText(
                mSamples[position].descriptionResId)
            return convertViewLocal
        }
    }

    /**
     * Class that holds the title, description and `Intent` for an activity that the user can
     * launch by clicking on its entry in our `GridView mGridView`.
     */
    private inner class Sample
    /**
     * This constructor just saves its arguments in the fields of the same name.
     *
     * @param titleResId Resource id for the string to use as the title of this `Sample`
     * @param descriptionResId Resource id for the string to use as the description of this `Sample`
     * @param intent `Intent` that can be used to launch the activity of this `Sample`
     */ private constructor(
        /**
         * Resource id for the string to use as the title of this `Sample`
         */
        var titleResId: Int,
        /**
         * Resource id for the string to use as the description of this `Sample`
         */
        var descriptionResId: Int,
        /**
         * `Intent` that can be used to launch the activity of this `Sample`
         */
        var intent: Intent) {
        /**
         * This constructor creates an intent for the specific component specified by its `activityClass`
         * and then calls our other constructor using that intent for its `Intent intent` parameter.
         *
         * @param titleResId Resource id for the string to use as the title of this `Sample`
         * @param descriptionResId Resource id for the string to use as the description of this `Sample`
         * @param activityClass `Class` of the `Activity` our `Intent` field should launch
         */
        constructor(titleResId: Int, descriptionResId: Int, activityClass: Class<out Activity>) : this(titleResId, descriptionResId, Intent(this@MainActivity, activityClass))
    }
}