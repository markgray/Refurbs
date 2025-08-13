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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "MemberVisibilityCanBePrivate")

package com.example.android.listviewdeletion

import android.content.Context
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.util.Collections
import androidx.core.util.size

/**
 * This example shows how animating [ListView] views can lead to artifacts if those views are
 * recycled before you animate them.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [ListViewDeletion](https://www.youtube.com/watch?v=NewCSg2JKLk).
 */
class ListViewDeletion : ComponentActivity() {
    /**
     * List of views which have been checked.
     */
    val mCheckedViews: ArrayList<View> = ArrayList()

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_list_view_deletion`. We
     * initialize [Button] variable `val deleteButton` by finding the view with id `R.id.deleteButton`
     * ("Delete Selected"), initialize [CheckBox] variable `val usePositionsCB` by finding the view
     * with id `R.id.usePositionsCB` ("Use Positions"), and initialize [ListView] variable
     * `val listView` by finding the view with id `R.id.list_view`. We initialize [ArrayList] of
     * [String] variable `val cheeseList` with a new instance, and add all the strings in
     * [Cheeses.sCheeseStrings] to it. We initialize our [StableArrayAdapter] variable `val adapter`
     * with a new instance which uses the layout file with resource id
     * [android.R.layout.simple_list_item_multiple_choice] to instantiate item views (it consists of
     * a [CheckedTextView] with the id [android.R.id.text1]) and `cheeseList` as the objects to
     * represent in the [ListView]. We then set the adapter of `listView` to `adapter`, clear its
     * "items can focus" flag, and set its choice mode to [ListView.CHOICE_MODE_MULTIPLE] (list
     * allows multiple choices).
     *
     * We set the [OnClickListener] of the button variable `deleteButton` to an anonymous class
     * whose [OnClickListener.onClick] override loops through all the checked items in `listView`
     * deleting them using either a simple call to the [StableArrayAdapter.remove] method of `adapter`
     * if [CheckBox] variable `usePositionsCB` is unchecked, or by using the sophisticated algorithm
     * which only animates deletion of visible items if `usePositionsCB` is checked. The simple
     * method also makes the mistake of animating the deletion of items which are off the screen and
     * whose view may have been recycled for the on screen items.
     *
     * We set the [OnItemClickListener] of `listView` to an anonymous class whose
     * [OnItemClickListener.onItemClick] override adds the [View] parameter `view` which has been
     * clicked to [ArrayList] of [View] field [mCheckedViews] if the item is now checked, or removes
     * it if it is now unchecked.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view_deletion)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        val usePositionsCB = findViewById<CheckBox>(R.id.usePositionsCB)
        val listView = findViewById<ListView>(R.id.list_view)
        val cheeseList = ArrayList<String>()
        Collections.addAll(cheeseList, *Cheeses.sCheeseStrings)
        val adapter = StableArrayAdapter(this,
            android.R.layout.simple_list_item_multiple_choice, cheeseList)
        listView.adapter = adapter
        listView.itemsCanFocus = false
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Clicking the delete button fades out the currently selected views
        deleteButton.setOnClickListener { v ->

            /**
             * Called when [Button] variable `val deleteButton` ("Delete Selected") is clicked. We
             * fetch the set of checked items in the list `listView` in order to initialize our
             * [SparseBooleanArray] variable `val checkedItems`, and initialize [Int] variable
             * `val numCheckedItems` with the size of `checkedItems`. We then loop backwards over
             * [Int] variable `var i` for the `numCheckedItems` in `checkedItems`:
             *
             *  * If the value at `i` of `checkedItems` is `false` we continue (this never happens
             *  because `checkedItems` contains only the checked items it its sparse array).
             *
             *  * We initialize [Int] variable `val position` with the key of the key-value pair in
             *  location `i` in the `checkedItems` sparse array (this is the position of the checked
             *  item in the adapter of the [ListView]).
             *
             *  * We initialize [String] variable `val item` with the string item in position
             * `position` of `StableArrayAdapter adapter`.
             *
             *  * We now branch on whether [CheckBox] variable `usePositionsCB` ("Use Positions") is
             * checked or not:
             *
             *  * Unchecked: we just post a delayed anonymous [Runnable] to run in 300ms which calls
             *  the [StableArrayAdapter.remove] method of `adapter` to remove `item`.
             *
             *  * Checked: First we clear [ArrayList] of [View] field [mCheckedViews] (this is a
             *  simplistic way to skip the for loop over its contents that follows this for loop,
             *  but we clear it again for every checked item!). Then we initialize [Int] variable
             *  `val positionOnScreen` with `position` (position of the checked item in the adapter)
             *  minus the position of the first item in `listView` that is visible on the screen. If
             *  this is greater or equal to 0 (the position of the item we are considering is after
             *  the first one on the screen) and `positionOnScreen` is less than the number of
             *  children being displayed by `listView` (it is before the last item being displayed)
             *  we initialize [View] variable `val view` with the `positionOnScreen` child of
             *  `listView` and animate the value of its alpha property to 0, with an end action
             *  consisting of an anonymous [Runnable] which sets its alpha back to 1, then removes
             *  the `item` from `adapter`. If it is not currently on screen we post an anonymous
             *  [Runnable] that will run in 300ms to remove the `item` from `adapter`.
             *
             * We loop over [Int] variable `var i` for the [indices] of [ArrayList] of [View] field
             * [mCheckedViews] (Note: [mCheckedViews] will be empty if [CheckBox] `usePositionsCB`
             * is checked):
             *
             *  * We initialize [View] variable `val checkedView` with the [View] at position `i` in
             *  [mCheckedViews]
             *
             *  * We animate the value of the alpha property of `checkedView` to 0, with an end
             *  action consisting of an anonymous [Runnable] which sets its alpha back to 1.
             *
             * Having finished deleting the checked views, we [clear] the [ArrayList] of [View]
             * field [mCheckedViews], and call the [StableArrayAdapter.notifyDataSetChanged] method
             * of `adapter` to notify any attached observers that the underlying data has been
             * changed and any View reflecting the data set should refresh itself.
             *
             * parameter: v [View] that has been clicked.
             */
            val checkedItems: SparseBooleanArray = listView.checkedItemPositions
            val numCheckedItems: Int = checkedItems.size
            for (i in numCheckedItems - 1 downTo 0) {
                if (!checkedItems.valueAt(i)) {
                    continue  // this never happens, but might as well be safe?
                }
                val position: Int = checkedItems.keyAt(i)
                val item: String? = adapter.getItem(position)
                if (!usePositionsCB.isChecked) {
                    // Remove the actual data after the time period that we're going to run
                    // the fading animation
                    v.postDelayed({ adapter.remove(item) }, 300)
                } else {
                    // This is the correct way to do this: first we see whether the item is
                    // actually visible, and don't bother animating it if it's not.
                    // Next, get the view associated with the item at the time of deletion
                    // (not some old view chosen when the item was clicked).
                    mCheckedViews.clear()
                    val positionOnScreen = position - listView.firstVisiblePosition
                    if (positionOnScreen >= 0 &&
                        positionOnScreen < listView.childCount) {
                        val view = listView.getChildAt(positionOnScreen)
                        // All set to fade this view out. Using ViewPropertyAnimator accounts
                        // for possible recycling of views during the animation itself
                        // (see the ListViewAnimations example for more on this).
                        view.animate().alpha(0f).withEndAction {
                            view.alpha = 1f
                            adapter.remove(item)
                        }
                    } else {
                        // Not animating the view, but don't delete it yet to avoid making the
                        // list shift due to offscreen deletions
                        v.postDelayed({ adapter.remove(item) }, 300)
                    }
                }
            }
            // THIS IS THE WRONG WAY TO DO THIS
            // We're basing our decision of the views to be animated based on outdated
            // information at selection time. Then we're going ahead and running an animation
            // on those views even when the selected items might not even be in view (in which
            // case we'll probably be mistakenly fading out something else that is on the
            // screen and is re-using that recycled view).
            for (i in mCheckedViews.indices) {
                val checkedView = mCheckedViews[i]
                checkedView.animate().alpha(0f).withEndAction { checkedView.alpha = 1f }
            }
            mCheckedViews.clear()
            adapter.notifyDataSetChanged()
        }
        listView.onItemClickListener = OnItemClickListener { parent, view, position, id ->

            /**
             * Callback method to be invoked when an item in the [ListView] has been clicked.
             * We initialize [Boolean] variable `val checked` with the checked state of the item
             * at position `position` in [ListView] `listView` and if `checked` is `true` we add
             * `view` to [ArrayList] of [View] field [mCheckedViews], if it is false we remove it.
             *
             * Paremeter: parent   The [AdapterView] where the click happened.
             * Paremeter: view     The view within the [AdapterView] that was clicked (this
             * will be a view provided by the adapter)
             * Paremeter: position The position of the view in the adapter.
             * Paremeter: id       The row id of the item that was clicked.
             */

            val checked: Boolean = listView.isItemChecked(position)
            if (checked) {
                mCheckedViews.add(view)
            } else {
                mCheckedViews.remove(view)
            }
        }
    }

    /**
     * Our stable id [ArrayAdapter] Constructor. First we call our super's constructor, then in our
     * `init` block we loop over [Int] variable `var i` for all the strings in our [List] of [String]
     * parameter `objects` putting `i` in our [HashMap] of [String] to [Int] field [mIdMap] under
     * the key consisting of the string in position `i` in `objects`.
     *
     * Paremeter: context            The current context.
     * @param textViewResourceId The resource ID for a layout file containing a TextView to use
     * when instantiating views.
     * @param objects            The objects to represent in the ListView.
     */
    private class StableArrayAdapter(
        context: Context?,
        textViewResourceId: Int,
        objects: List<String>
    ) : ArrayAdapter<String?>(context!!, textViewResourceId, objects) {
        /**
         * Our map of string items to integer stable id's.
         */
        var mIdMap = HashMap<String?, Int>()

        init {
            for (i in objects.indices) {
                mIdMap[objects[i]] = i
            }
        }

        /**
         * Get the row id associated with the specified position in the list. We initialize
         * [String] variable `val item` with the data item at position [position] in our dataset,
         * then return the value stored in [HashMap] of [String] to [Integer] field [mIdMap] under
         * that key.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            val item = getItem(position)
            return mIdMap[item]!!.toLong()
        }

        /**
         * Indicates whether the item ids are stable across changes to the
         * underlying data, we just return `true`.
         *
         * @return `true` if the same id always refers to the same object.
         */
        override fun hasStableIds(): Boolean {
            return true
        }
    }
}
