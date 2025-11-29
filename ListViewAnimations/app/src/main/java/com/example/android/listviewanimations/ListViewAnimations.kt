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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.example.android.listviewanimations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.util.Collections

/**
 * This example shows how animating ListView items can lead to problems as views are recycled,
 * and how to perform these types of animations correctly with new API added in Jellybean.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [ListViewAnimations](https://www.youtube.com/watch?v=8MIfSxgsHIs).
 */
class ListViewAnimations : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_list_view_animations`.
     *
     * We initialize our [LinearLayout] variable `rootView`
     * to the view with ID `R.id.root_view` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We then initialize [CheckBox] variable `val vpaCB` by finding the view with id `R.id.vpaCB`
     * ("ViewPropertyAnimator") initialize [CheckBox] variable `val setTransientStateCB` by finding
     * the view with id `R.id.setTransientStateCB` ("Transient State") and initialize [ListView]
     * variable `val listView` by finding the view with id `R.id.list_view`. We initialize
     * [ArrayList] of [String] variable `val cheeseList` with a new instance then add all of the
     * strings in the array [Cheeses.sCheeseStrings] to it. We initialize [StableArrayAdapter]
     * variable `val adapter` with an instance which will represent the objects in `cheeseList`
     * using the layout file [android.R.layout.simple_list_item_1] as the [TextView] when
     * instantiating views, and set it to be the adapter for `listView`. We then set the
     * [OnItemClickListener] of `listView` to a lambda whose [OnItemClickListener.onItemClick]
     * override removes the item clicked from the `listView` and animates the removal by fading
     * the alpha of the item to 0 before removing it. The two checkboxes enable or disable
     * workarounds for the continued alpha animation of views that have been recycled, with their
     * unchecked state being used to branch around code in the override which "does the right thing"
     * using the workaround selected by the checkboxes.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view_animations)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        val vpaCB = findViewById<CheckBox>(R.id.vpaCB)
        val setTransientStateCB = findViewById<CheckBox>(R.id.setTransientStateCB)
        val listView = findViewById<ListView>(R.id.list_view)
        val cheeseList = ArrayList<String>()
        Collections.addAll(cheeseList, *Cheeses.sCheeseStrings)
        val adapter = StableArrayAdapter(
            context = this,
            textViewResourceId = android.R.layout.simple_list_item_1,
            objects = cheeseList
        )
        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { parent, view, position, _ ->
            /**
             * Callback method to be invoked when an item in this [ListView] has been clicked. We
             * initialize [String] variable `val item` with the string returned by the
             * [AdapterView.getItemAtPosition] method of our [AdapterView] parameter [parent] for
             * the item in position [position]. We then branch on whether [CheckBox] variable
             * `vpaCB` is checked:
             *
             *  * Checked: we retrieve a [ViewPropertyAnimator] object for the clicked [View]
             *  parameter [view], set its duration to 1000ms, configure it to animate the alpha
             *  property to 0, and set an an anonymous [Runnable] lambda to run when the animation
             *  finishes whose [Runnable.run] override removes `item` from the dataset in [ArrayList]
             *  of [String] variable `cheeseList`, notifies `adapter` that the dataset has changed,
             *  and sets the alpha of [view] to 1.
             *
             *  * Unchecked: We initialize [ObjectAnimator] variable `val anim` with an instance
             *  which will animate the ALPHA property of [View] parameter [view], and set its
             *  duration to 1000ms. Then if [CheckBox] variable `setTransientStateCB` is checked
             *  we call the [View.setHasTransientState] method of [view] with `true` to set its
             *  transient state flag (A view with transient state cannot be trivially rebound from
             *  an external data source, such as an adapter binding item views in a list). Then we
             *  add an anonymous [AnimatorListenerAdapter] whose [AnimatorListenerAdapter.onAnimationEnd]
             *  override removes `item` from the dataset in [ArrayList] of [String] variable `cheeseList`,
             *  notifies `adapter` that the dataset has changed, and sets the alpha of [view] to 1.
             *  Then if [CheckBox] variable `setTransientStateCB` is checked we call the
             *  [View.setHasTransientState] method of [view] with `false` to clear its transient
             *  state flag (this flag is reference counted, so every call to
             *  [View.setHasTransientState] with `true` should be paired with a later call to
             *  [View.setHasTransientState]with `false`). Having configured [ObjectAnimator] `anim`
             *  we start it running.
             *
             * param parent   The [AdapterView] where the click happened.
             * param view     The [View] within the [AdapterView] that was clicked (this will be a
             * view provided by the adapter)
             * param position The position of the [View] in the adapter.
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
     * An [ArrayAdapter] of [String] subclass with stable id's (Stable IDs allow the [ListView] to
     * optimize for the case when items remain the same between [notifyDataSetChanged] calls. The
     * IDs it refers to are the ones returned from [getItemId]). Our constructor. First we call our
     * super's constructor. Then in our `init` block we loop over [Int] variable `var i` for all the
     * strings in our [List] of [String] parameter `objects` storing the current value of `i` under
     * the key of the string at position `i` in `objects` in our [HashMap] of [String] to [Int]
     * field [mIdMap].
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
         * Get the row id associated with the specified position in the list. We initialize [String]
         * variable `val item` with the item in position [position] of our dataset, then return the
         * [Integer] stored in our [HashMap] of [String] to [Int] field [mIdMap] under that key to
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
         * In our case we always return `true`.
         *
         * @return `true` if the same id always refers to the same object.
         */
        override fun hasStableIds(): Boolean {
            return true
        }
    }
}
