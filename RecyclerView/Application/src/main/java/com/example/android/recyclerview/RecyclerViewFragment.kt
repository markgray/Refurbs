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
@file:Suppress(
    "ReplaceNotNullAssertionWithElvisReturn",
    "UNUSED_ANONYMOUS_PARAMETER",
    "MemberVisibilityCanBePrivate",
    "unused"
)

package com.example.android.recyclerview

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager

/**
 * Demonstrates the use of [RecyclerView] with a [LinearLayoutManager] and a [GridLayoutManager].
 */
class RecyclerViewFragment : Fragment() {
    /**
     * [LayoutManager] types that can be used for our [RecyclerView], used in our
     * [setRecyclerViewLayoutManager] in a switch to choose between [GridLayoutManager]
     * and [LinearLayoutManager].
     */
    enum class LayoutManagerType {
        /**
         * enum to select [GridLayoutManager]
         */
        GRID_LAYOUT_MANAGER,
        /**
         * enum to select [LinearLayoutManager]
         */
        LINEAR_LAYOUT_MANAGER
    }

    /**
     * Currently selected [LayoutManagerType]
     */
    var mCurrentLayoutManagerType: LayoutManagerType? = null

    /**
     * [RadioButton] in our UI that is used to select [LinearLayoutManager] for our
     * [RecyclerView].
     */
    var mLinearLayoutRadioButton: RadioButton? = null

    /**
     * [RadioButton] in our UI that is used to select [GridLayoutManager] for our
     * [RecyclerView].
     */
    var mGridLayoutRadioButton: RadioButton? = null

    /**
     * [RecyclerView] in our layout with ID `R.id.recyclerView`.
     */
    var mRecyclerView: RecyclerView? = null

    /**
     * [CustomAdapter] instance that we use as the adapter for [RecyclerView] field [mRecyclerView].
     */
    var mAdapter: CustomAdapter? = null

    /**
     * Current [RecyclerView.LayoutManager] used by our [RecyclerView].
     */
    var mLayoutManager: LayoutManager? = null

    /**
     * Our dataset, consists of 60 strings each formed by concatenating the element number to the
     * end of the string "This is element #"
     */
    lateinit var mDataset: Array<String?>

    /**
     * Called when the [Fragment] is starting. First we call our super's implementation of `onCreate`,
     * then we call our method [initDataset] to allocate storage for our [Array] of [String] dataset
     * field [mDataset] and fill it with sample strings.
     *
     * @param savedInstanceState we do not use in this override, but use it in our [onCreateView]
     * override instead.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        initDataset()
    }

    /**
     * Called to have the fragment instantiate its user interface view. We initialize [View] variable
     * `val rootView` to the view inflated by our [LayoutInflater] parameter [inflater] from our
     * layout file `R.layout.recycler_view_frag` using our [ViewGroup] parameter [container] for its
     * LayoutParams without attaching to it. We then set the tag of `rootView` to [TAG]
     * ("RecyclerViewFragment") for no reason I can see. We initialize [RecyclerView] field
     * [mRecyclerView] by finding the view with ID `R.id.recyclerView`. We initialize
     * [RecyclerView.LayoutManager] field [mLayoutManager] with a new instance of [LinearLayoutManager]
     * using our [FragmentActivity] to access resources, and set our [LayoutManagerType] field
     * [mCurrentLayoutManagerType] to [LayoutManagerType.LINEAR_LAYOUT_MANAGER].
     *
     * If our [Bundle] parameter [savedInstanceState] is not `null`, we set [mCurrentLayoutManagerType]
     * to the [LayoutManagerType] stored in it under the key [KEY_LAYOUT_MANAGER] (using the overload
     * of [Bundle.getSerializable] that includes a `clazz` parameter on `TIRAMISU` and newer SDK's).
     * We then call our method [setRecyclerViewLayoutManager] to set the [RecyclerView.LayoutManager]
     * of our [RecyclerView] to the type now specified by [mCurrentLayoutManagerType] (note that the
     * [LinearLayoutManager] created before we call this method is forgotten). Next we initialize
     * [CustomAdapter] field [mAdapter] with a new instance constructed to use our [Array] of [String]
     * dataset field [mDataset] as its dataset. We then set the adapter of [RecyclerView] field
     * [mRecyclerView] to [mAdapter]. We initialize [RadioButton] field [mLinearLayoutRadioButton] by
     * finding the view with id `R.id.linear_layout_rb`, and set its [View.OnClickListener] to an
     * anonymous class whose [View.OnClickListener.onClick] override calls our method
     * [setRecyclerViewLayoutManager] to set the [LayoutManagerType] of our [RecyclerView] to
     * [LayoutManagerType.LINEAR_LAYOUT_MANAGER]. We initialize [RadioButton] field
     * [mGridLayoutRadioButton] by finding the view with id `R.id.grid_layout_rb`, and set its
     * [View.OnClickListener] to an anonymous class whose [View.OnClickListener.onClick] override
     * calls our method [setRecyclerViewLayoutManager] to set the [LayoutManagerType] of our
     * [RecyclerView] to [LayoutManagerType.GRID_LAYOUT_MANAGER]. Finally we return `rootView` to
     * the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or `null`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            /* resource = */ R.layout.recycler_view_frag,
            /* root = */ container,
            /* attachToRoot = */ false
        )
        rootView.tag = TAG

        mRecyclerView = rootView.findViewById(R.id.recyclerView)

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mLayoutManager = LinearLayoutManager(activity)
        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER
        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mCurrentLayoutManagerType = savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER, LayoutManagerType::class.java)
            } else {
                @Suppress("DEPRECATION") // Needed for SDK older than TIRAMISU
                mCurrentLayoutManagerType = savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER) as LayoutManagerType?
            }
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType)
        mAdapter = CustomAdapter(mDataset)
        // Set CustomAdapter as the adapter for RecyclerView.
        mRecyclerView!!.adapter = mAdapter
        // END_INCLUDE(initializeRecyclerView)
        mLinearLayoutRadioButton = rootView.findViewById(R.id.linear_layout_rb)
        mLinearLayoutRadioButton!!.setOnClickListener { v: View? ->
            setRecyclerViewLayoutManager(LayoutManagerType.LINEAR_LAYOUT_MANAGER)
        }
        mGridLayoutRadioButton = rootView.findViewById(R.id.grid_layout_rb)
        mGridLayoutRadioButton!!.setOnClickListener { v: View? ->
            setRecyclerViewLayoutManager(LayoutManagerType.GRID_LAYOUT_MANAGER)
        }
        return rootView
    }

    /**
     * Set [RecyclerView]'s LayoutManager to the one specified by [LayoutManagerType] parameter
     * [layoutManagerType]. First we initialize [Int] variable `var scrollPosition` to 0. If the
     * [LayoutManager] currently responsible for layout policy for the [RecyclerView] field
     * [mRecyclerView] is not `null`, we retrieve the adapter position of the first fully visible
     * view from it to set `scrollPosition`. Then if the value of our [LayoutManagerType] parameter
     * [layoutManagerType] is:
     *
     *  * [LayoutManagerType.GRID_LAYOUT_MANAGER]: we set our [LayoutManager] field [mLayoutManager]
     *  to a new instance of [GridLayoutManager] constructed to have [SPAN_COUNT] (2) columns in its
     *  grid, and set our [LayoutManagerType] field [mCurrentLayoutManagerType] to
     *  [LayoutManagerType.GRID_LAYOUT_MANAGER]
     *
     *  * [LayoutManagerType.LINEAR_LAYOUT_MANAGER] (or any other type): we set our [LayoutManager]
     *  field [mLayoutManager] to a new instance of [LinearLayoutManager], and set our
     *  [LayoutManagerType] field [mCurrentLayoutManagerType] to
     *  [LayoutManagerType.LINEAR_LAYOUT_MANAGER].
     *
     * We set the [LayoutManager] that the [RecyclerView] field [mRecyclerView] will use to our
     * [LayoutManager] field [mLayoutManager], then scroll [mRecyclerView] to the position
     * `scrollPosition`.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    fun setRecyclerViewLayoutManager(layoutManagerType: LayoutManagerType?) {
        var scrollPosition = 0

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView!!.layoutManager != null) {
            scrollPosition = (mRecyclerView!!.layoutManager as LinearLayoutManager?)!!
                .findFirstCompletelyVisibleItemPosition()
        }
        if (layoutManagerType == LayoutManagerType.GRID_LAYOUT_MANAGER) {
            mLayoutManager = GridLayoutManager(activity, SPAN_COUNT)
            mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER
        } else {
            mLayoutManager = LinearLayoutManager(activity)
            mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER
        }
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.scrollToPosition(scrollPosition)
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it can later be reconstructed
     * in a new instance if its process is restarted. We save our [LayoutManagerType] field
     * [mCurrentLayoutManagerType] as a Serializable value in our [Bundle] parameter
     * [savedInstanceState] under the key [KEY_LAYOUT_MANAGER], then call our super's implementation
     * of `onSaveInstanceState`.
     *
     * @param savedInstanceState Bundle in which to place your saved state.
     */
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType)
        super.onSaveInstanceState(savedInstanceState)
    }

    /**
     * Generates Strings for [RecyclerView]'s adapter. This data would usually come from a local
     * content provider or remote server. We allocate [DATASET_COUNT] (60) strings for our [Array]
     * of [String] field [mDataset]. We then fill it with [DATASET_COUNT] strings each formed by
     * concatenating the element number to the end of the string "This is element #".
     */
    private fun initDataset() {
        mDataset = arrayOfNulls(DATASET_COUNT)
        for (i in 0 until DATASET_COUNT) {
            mDataset[i] = "This is element #$i"
        }
    }

    companion object {
        /**
         * TAG we set on the root view of our [Fragment] (for some reason?)
         */
        private const val TAG = "RecyclerViewFragment"

        /**
         * Key for saving the serializable [LayoutManagerType] field [mCurrentLayoutManagerType] in
         * the bundle passed to our [onSaveInstanceState] override in order to restore it when our
         * [onCreateView] override is called.
         */
        private const val KEY_LAYOUT_MANAGER = "layoutManager"

        /**
         * The number of columns in the grid when a [GridLayoutManager] type [LayoutManager] is used
         */
        private const val SPAN_COUNT = 2

        /**
         * Number of entries in our [Array] of [String] dataset [mDataset].
         */
        private const val DATASET_COUNT = 60
    }
}
