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
package com.example.android.listviewanimations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import java.util.Collections

/**
 * This example shows how animating ListView items can lead to problems as views are recycled,
 * and how to perform these types of animations correctly with new API added in Jellybean.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [ListViewAnimations](https://www.youtube.com/watch?v=8MIfSxgsHIs).
 */
class ListViewAnimations : Activity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.activity_list_view_animations. We
     * then initialize `CheckBox vpaCB` by finding the view with id R.id.vpaCB
     * ("ViewPropertyAnimator") initialize `CheckBox setTransientStateCB` by finding the view with
     * id R.id.setTransientStateCB ("Transient State") and initialize `ListView listView` by finding
     * the view with id R.id.list_view. We initialize `ArrayList<String> cheeseList` with a new
     * instance then add all of the strings in the array `Cheeses.sCheeseStrings` to it. We
     * initialize `StableArrayAdapter adapter` with an instance which will represent the objects in
     * `cheeseList` using the layout file android.R.layout.simple_list_item_1 as the `TextView` when
     * instantiating views, and set it to be the adapter for `listView`. We then set the
     * `OnItemClickListener` of `listView` to an anonymous class whose `onItemClick` removes the
     * item clicked from the `listView` and animates the removal by fading the alpha of the item to
     * 0 before removing it. The two checkboxes enable or disable workarounds for the continued
     * alpha animation of views that have been recycled, with their unchecked state being used to
     * branch around code in the override which "does the right thing" using the workaround selected
     * by the checkboxes.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view_animations)
        val vpaCB = findViewById<CheckBox>(R.id.vpaCB)
        val setTransientStateCB = findViewById<CheckBox>(R.id.setTransientStateCB)
        val listView = findViewById<ListView>(R.id.list_view)
        val cheeseList = ArrayList<String>()
        Collections.addAll(cheeseList, *Cheeses.sCheeseStrings)
        val adapter = StableArrayAdapter(this,
            android.R.layout.simple_list_item_1, cheeseList)
        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            /**
             * Callback method to be invoked when an item in this AdapterView has been clicked. We
             * initialize `String item` with the string returned by the `getItemAtPosition`
             * method of our parameter `AdapterView<?> parent` for the item in position
             * `position`. We then branch on whether `CheckBox vpaCB` is checked:
             *
             *  *
             * Checked: we retrieve a `ViewPropertyAnimator` object for the clicked
             * `View view`, set its duration to 1000ms, configure it to animate the
             * alpha property to 0, and set an an anonymous `Runnable` to run when the
             * animation finishes whose `run` override removes `item` from the
             * dataset `ArrayList<String> cheeseList`, notifies `adapter` that the
             * dataset has changed, and sets the alpha of `view` to 1.
             *
             *  *
             * Unchecked: We initialize `ObjectAnimator anim` with an instance which will
             * animate the ALPHA property of `View view`, and set its duration to 1000ms.
             * Then if checkbox `setTransientStateCB` is checked we call the
             * `setHasTransientState(true)` method of `view` to set the transient state flag
             * (A view with transient state cannot be trivially rebound from an external data
             * source, such as an adapter binding item views in a list). Then we add an anonymous
             * `AnimatorListenerAdapter` whose `onAnimationEnd` override removes `item` from the
             * dataset `ArrayList<String> cheeseList`, notifies `adapter` that the dataset
             * has changed, and sets the alpha of `view` to 1. Then if checkbox `setTransientStateCB`
             * is checked we call the `setHasTransientState(false)` method of `view` to
             * clear the transient state flag (this flag is reference counted, so every call to
             * setHasTransientState(true) should be paired with a later call to
             * setHasTransientState(false)). Having configured `ObjectAnimator anim` we start
             * it running.
             *
             *
             *
             * param parent   The AdapterView where the click happened.
             * param view     The view within the AdapterView that was clicked (this
             * will be a view provided by the adapter)
             * param position The position of the view in the adapter.
             * param id       The row id of the item that was clicked.
             */
            val item = parent.getItemAtPosition(position) as String
            if (vpaCB.isChecked) {
                view.animate().setDuration(1000).alpha(0f).withEndAction {
                    cheeseList.remove(item)
                    adapter.notifyDataSetChanged()
                    view.alpha = 1f
                }
            } else {
                // Here's where the problem starts - this animation will animate a View object.
                // But that View may get recycled if it is animated out of the container,
                // and the animation will continue to fade a view that now contains unrelated
                // content.
                val anim = ObjectAnimator.ofFloat(view, View.ALPHA, 0f)
                anim.duration = 1000
                if (setTransientStateCB.isChecked) {
                    // Here's the correct way to do this: if you tell a view that it has
                    // transientState, then ListView ill avoid recycling it until the
                    // transientState flag is reset.
                    // A different approach is to use ViewPropertyAnimator, which sets the
                    // transientState flag internally.
                    view.setHasTransientState(true)
                }
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cheeseList.remove(item)
                        adapter.notifyDataSetChanged()
                        view.alpha = 1f
                        if (setTransientStateCB.isChecked) {
                            view.setHasTransientState(false)
                        }
                    }
                })
                anim.start()
            }
        }
    }

    /**
     * An `ArrayAdapter<String>` subclass with stable id's (Stable IDs allow the ListView to
     * optimize for the case when items remain the same between notifyDataSetChanged calls. The IDs
     * it refers to are the ones returned from getItemId).
     * Our constructor. First we call our super's constructor. Then in our `init` block we loop over
     * `int i` for all the strings in our parameter `List<String> objects` storing the current value
     * of `i` under the key of the string at position `i` in `objects` in our field
     * `HashMap<String, Integer> mIdMap`.
     *
     * @param context The current [Context].
     * @param textViewResourceId The resource ID for a layout file containing a [TextView] to use
     * when instantiating views.
     * @param objects The objects to represent in the [ListView].
     */
    private class StableArrayAdapter(
        context: Context?,
        textViewResourceId: Int,
        objects: List<String>
    ) : ArrayAdapter<String?>(context!!, textViewResourceId, objects) {
        /**
         * `HashMap` mapping the strings in our dataset to a unique integer id.
         */
        var mIdMap = HashMap<String?, Int>()

        init {
            for (i in objects.indices) {
                mIdMap[objects[i]] = i
            }
        }

        /**
         * Get the row id associated with the specified position in the list. We initialize
         * `String item` with the item in position `position` of our dataset, then return the
         * `Integer` stored in our field `HashMap<String, Integer> mIdMap` under that key to
         * the caller.
         *
         * @param position The position of the item within the adapter's data set whose row id
         * we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            val item = getItem(position)
            return mIdMap[item]!!.toLong()
        }

        /**
         * Indicates whether the item ids are stable across changes to the underlying data.
         * In our case we always return true.
         *
         * @return True if the same id always refers to the same object.
         */
        override fun hasStableIds(): Boolean {
            return true
        }
    }
}