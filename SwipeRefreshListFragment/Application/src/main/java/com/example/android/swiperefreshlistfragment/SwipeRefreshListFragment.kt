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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.swiperefreshlistfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.ListFragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.core.view.isVisible

/**
 * Subclass of [ListFragment] which provides automatic support for providing the 'swipe-to-refresh'
 * UX gesture by wrapping the the content view in a [SwipeRefreshLayout].
 */
open class SwipeRefreshListFragment : ListFragment() {
    /**
     * Our instance of our [ListFragmentSwipeRefreshLayout] class which is a sub-class of
     * [SwipeRefreshLayout] designed to hold a [ListFragment].
     */
    var swipeRefreshLayout: SwipeRefreshLayout? = null
        private set

    /**
     * Called to have the fragment instantiate its user interface view. First we initialize our
     * [View] variable `val listFragmentView` to the view our super's implementation of `onCreateView`
     * returns. We initialize our [SwipeRefreshLayout] field [swipeRefreshLayout] with a new instance
     * using the [Context] of our [ViewGroup] parameter [container] for the LayoutParams. We add the
     * view `listFragmentView` to [swipeRefreshLayout] using `MATCH_PARENT` for both the width and
     * the height. We set the LayoutParams of [swipeRefreshLayout] to a new instance of
     * [ViewGroup.LayoutParams] whose width and height are both `MATCH_PARENT` to make sure that
     * the [SwipeRefreshLayout] will fill our fragment and return `[swipeRefreshLayout] to the
     * caller to have it use it for our fragments UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Create the list fragment's content view by calling the super method
        val listFragmentView = super.onCreateView(inflater, container, savedInstanceState)

        // Now create a SwipeRefreshLayout to wrap the fragment's content view
        swipeRefreshLayout = ListFragmentSwipeRefreshLayout(container!!.context)

        // Add the list fragment's content view to the SwipeRefreshLayout, making sure that it fills
        // the SwipeRefreshLayout
        swipeRefreshLayout!!.addView(
            listFragmentView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Make sure that the SwipeRefreshLayout will fill the fragment
        swipeRefreshLayout!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Now return the SwipeRefreshLayout as this fragment's content view
        return swipeRefreshLayout
    }

    /**
     * Set the [SwipeRefreshLayout.OnRefreshListener] of our [SwipeRefreshLayout] field
     * [swipeRefreshLayout] that will listen for initiated refreshes.
     *
     * @param listener `OnRefreshListener` to use when a refresh gesture is detected.
     */
    fun setOnRefreshListener(listener: OnRefreshListener?) {
        swipeRefreshLayout!!.setOnRefreshListener(listener)
    }

    /**
     * Returns whether the [SwipeRefreshLayout] we contain is currently refreshing or not.
     *
     * @return `true` if our [SwipeRefreshLayout] field [swipeRefreshLayout] is currently refreshing
     * @see SwipeRefreshLayout.isRefreshing
     */
    var isRefreshing: Boolean
        get() = swipeRefreshLayout!!.isRefreshing
        /**
         * Set whether the [SwipeRefreshLayout] should be displaying
         * that it is refreshing or not.
         *
         * @param `refreshing` Whether or not the view should show refresh progress.
         * @see SwipeRefreshLayout.setRefreshing
         */
        set(refreshing) {
            swipeRefreshLayout!!.isRefreshing = refreshing
        }

    /**
     * Set the color scheme for the [SwipeRefreshLayout]. We just call the
     * [SwipeRefreshLayout.setColorSchemeResources] method of [SwipeRefreshLayout] field
     * [swipeRefreshLayout] with our parameters. The four colors will be used round robin to
     * color the rotating circle.
     *
     * @param colorRes1 First color to use for indeterminate progress animation
     * @param colorRes2 Second color to use for indeterminate progress animation
     * @param colorRes3 Third color to use for indeterminate progress animation
     * @param colorRes4 Fourth color to use for indeterminate progress animation
     */
    fun setColorScheme(colorRes1: Int, colorRes2: Int, colorRes3: Int, colorRes4: Int) {
        swipeRefreshLayout!!.setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4)
    }

    /**
     * Sub-class of [SwipeRefreshLayout] for use in this [ListFragment]. The reason that this is
     * needed is because [SwipeRefreshLayout] only supports a single child, which it expects to be
     * the one which triggers refreshes. In our case the layout's child is the content view returned
     * from [ListFragment.onCreateView] which is a [android.view.ViewGroup].
     *
     * To enable 'swipe-to-refresh' support via the [android.widget.ListView] we need to override
     * the default behavior and properly signal when a gesture is possible. This is done by
     * overriding [canChildScrollUp].
     */
    private inner class ListFragmentSwipeRefreshLayout
    /**
     * Our constructor, we just call our super's constructor.
     *
     * @param context the [Context] of the container we are to be attached to.
     */
    (context: Context?) : SwipeRefreshLayout(context!!) {
        /**
         * As mentioned above, we need to override this method to properly signal when a
         * 'swipe-to-refresh' is possible. We initialize [ListView] variable `val listView` by
         * retrieving a reference to our fragment's list view widget. We return `true` if
         * `listView` is visible and our [canListViewScrollUp] method determines that it can
         * scroll up.
         *
         * @return true if the [android.widget.ListView] is visible and can scroll up.
         */
        override fun canChildScrollUp(): Boolean {
            val listView = listView
            return listView.isVisible && canListViewScrollUp(listView)
        }
    }

    companion object {
        /**
         * Utility method to check whether a [ListView] can scroll up from it's current position.
         * We just return the value returned by the [ListView.canScrollVertically] method of our
         * [ListView] parameter [listView] when passed the `direction` argument of -1.
         *
         * @param listView `ListView` we are to check to see if it can scroll up.
         * @return true is our parameter `ListView listView` can scroll up
         */
        private fun canListViewScrollUp(listView: ListView): Boolean {
            return listView.canScrollVertically(/* direction = */ -1)
        }
    }
}