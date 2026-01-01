/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.swiperefreshmultipleviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * A descendant of [SwipeRefreshLayout] which supports multiple child views triggering a refresh
 * gesture. You set the views which can trigger the gesture via [setSwipeableChildren], providing
 * it the child ids.
 */
class MultiSwipeRefreshLayout : SwipeRefreshLayout {
    /**
     * The [View]'s we implement [SwipeRefreshLayout] for.
     */
    lateinit var mSwipeableChildren: Array<View?>

    /**
     * Our one argument constructor, we just call our super's constructor. UNUSED
     *
     * @param context The [Context] the [View] is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context!!)

    /**
     * Constructor that is called when inflating [MultiSwipeRefreshLayout] from XML (as is our case).
     * We just call our super's constructor.
     *
     * @param context The [Context] the [View] is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    /**
     * Set the children which can trigger a refresh by swiping down when they are visible. These
     * [View]'s need to be a descendant of this [View]. We allocate enough storage to hold our
     * [IntArray] parameter [ids] for our [Array] of [View] field [mSwipeableChildren]. Then we
     * loop over [Int] variable `var i` for all the [Int]'s in [ids] setting the [View] in
     * [mSwipeableChildren] at index `i` by finding the [View] in our [View] with the resource id
     * of the [Int] at index `i` in [ids].
     *
     * @param ids array or varargs of resource ID's of the views we are to control
     */
    fun setSwipeableChildren(vararg ids: Int) {
        // Iterate through the ids and find the Views
        mSwipeableChildren = arrayOfNulls(ids.size)
        for (i in ids.indices) {
            mSwipeableChildren[i] = findViewById(ids[i])
        }
    }

    /**
     * This method controls when the swipe-to-refresh gesture is triggered. By returning `false`
     * here we are signifying that the view is in a state where a refresh gesture can start. As
     * [SwipeRefreshLayout] only supports one direct child by default, we need to manually iterate
     * through our swipeable children to see if any are in a state to trigger the gesture. If so we
     * return `false` to start the gesture.
     *
     * If our field [Array] of [View] field [mSwipeableChildren] is not `null` and has more than 0
     * entries, we loop through each of the [View] variable `var view` in [mSwipeableChildren] and
     * if `view` is not `null` and its [View.isShown] method reports that the view and all of its
     * ancestors are `VISIBLE`, and our method [canViewScrollUp] determines that the view cannot
     * scroll up we return `false` to start the refresh gesture. Otherwise we return `true`.
     *
     * @return Whether it is possible for the child view of this layout to scroll up.
     */
    override fun canChildScrollUp(): Boolean {
        @Suppress("SENSELESS_COMPARISON", "UNNECESSARY_NOT_NULL_ASSERTION")
        if (mSwipeableChildren != null && mSwipeableChildren.isNotEmpty()) {
            // Iterate through the scrollable children and check if any of them can not scroll up
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            for (view in mSwipeableChildren!!) {
                if (view != null && view.isShown && !canViewScrollUp(view = view)) {
                    // If the view is shown, and can not scroll upwards, return false and start the
                    // gesture.
                    return false
                }
            }
        }
        return true
    }

    companion object {
        /**
         * Utility method to check whether a [View] can scroll up from it's current position. We
         * return the value returned by the [View.canScrollVertically] method of our [View] parameter
         * [view] for the direction -1 (negative to check scrolling up).
         *
         * @param view Whether it is possible for the [View] parameter [view] to scroll up.
         */
        private fun canViewScrollUp(view: View): Boolean {
            // For ICS and above we can call canScrollVertically() to determine this
            return view.canScrollVertically(/*direction=*/ -1)
        }
    }
}