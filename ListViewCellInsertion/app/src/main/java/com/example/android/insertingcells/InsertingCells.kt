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

package com.example.android.insertingcells

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout

/**
 * This application creates a ListView to which new elements can be added from the
 * top. When a new element is added, it is animated from above the bounds
 * of the list to the top. When the list is scrolled all the way to the top and a new
 * element is added, the row animation is accompanied by an image animation that pops
 * out of the round view and pops into the correct position in the top cell.
 */
class InsertingCells : Activity(), OnRowAdditionAnimationListener {
    /**
     * Contains the `ListItemObject` objects we cycle through round robin in our method
     * `addRow`, then clone the chosen one, and add the clone to our list.
     */
    private lateinit var mValues: Array<ListItemObject>

    /**
     * The `InsertionListView` with id R.id.list_view in our layout that we add our
     * `ListItemObject` objects to the top of, animating the addition if the current
     * top item is at the top of the `InsertionListView`.
     */
    private var mListView: InsertionListView? = null

    /**
     * The `Button` with id R.id.add_row_button in our layout, its android:onClick="addRow"
     * attribute makes it call our `addRow` method when it is clicked.
     */
    private var mButton: Button? = null

    /**
     * Index into the `ListItemObject mValues[]` array (modulo the length of `mValues`)
     * of the last `ListItemObject` that our `addRow` method cloned and added to
     * `InsertionListView mListView`, it is incremented before each use when adding the next.
     */
    private var mItemNum = 0

    /**
     * The `RoundView` with id R.id.round_view in our layout, an image animation emerges from
     * it when the list is scrolled all the way to the top and a new `ListItemObject` is added.
     */
    private var mRoundView: RoundView? = null

    /**
     * Set from the resource with id R.dimen.cell_height (150dp), it is used when creating a clone
     * of an `ListItemObject` in our `addRow` method, the `ListItemObject` constructor
     * saves this value in its `mHeight` field for use by the `getView` override of our
     * `CustomArrayAdapter` adapter when configuring the layout params for the item's view.
     */
    private var mCellHeight = 0

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.activity_main. We initialize our field
     * `ListItemObject mValues[]` with a new instance, filled with three `ListItemObject`
     * instances constructed to contain "Chameleon", "Rock", and "Flower" text views as well as the
     * resource id's for jpg images to use for the associated `ImageView` (R.drawable.chameleon,
     * R.drawable.rock, and R.drawable.flower respectively). We initialize our field `mCellHeight`
     * by retrieving the dimensional for the resource id R.dimen.cell_height from a `Resources`
     * instance for our application's package. We initialize `List<ListItemObject> mData` with
     * a new instance, and `CustomArrayAdapter mAdapter` with an instance which will display
     * the `ListItemObject` items in `mData` using our layout file R.layout.list_view_item.
     * We initialize `RelativeLayout mLayout` by finding the view with id R.id.relative_layout
     * (it contains all our other views). We then initialize our fields `RoundView mRoundView`
     * by finding the view with id R.id.round_view, `Button mButton` by finding the view with
     * id R.id.add_row_button ("Add Row"), and `InsertionListView mListView` by finding the view
     * with id R.id.list_view. We then set the adapter of `mListView` to `mAdapter`, its
     * data to `mData`, its parent `RelativeLayout` to `mLayout` and its
     * `OnRowAdditionAnimationListener` to 'this'.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mValues = arrayOf(
            ListItemObject("Chameleon", R.drawable.chameleon, 0),
            ListItemObject("Rock", R.drawable.rock, 0),
            ListItemObject("Flower", R.drawable.flower, 0))
        mCellHeight = resources.getDimension(R.dimen.cell_height).toInt()
        val mData: List<ListItemObject> = ArrayList()
        val mAdapter = CustomArrayAdapter(this, R.layout.list_view_item, mData)
        val mLayout = findViewById<RelativeLayout>(R.id.relative_layout)
        mRoundView = findViewById(R.id.round_view)
        mButton = findViewById(R.id.add_row_button)
        mListView = findViewById(R.id.list_view)
        mListView!!.adapter = mAdapter
        mListView!!.setData(mData)
        mListView!!.setLayout(mLayout)
        mListView!!.setRowAdditionAnimationListener(this)
    }

    /**
     * Called when the `Button` with id R.id.add_row_button ("Add Row") is clicked, we add a
     * new `ListItemObject` cloned from one of those in `mValues` to our layout's
     * `InsertionListView mListView`. First we disable our field `Button mButton`, then
     * we increment our field `Integer mItemNum`. We initialize `ListItemObject obj` with
     * the `ListItemObject` in our field `ListItemObject mValues[]` with index `mItemNum`
     * modulo the length of `mValues`. We then initialize `ListItemObject newObj` with a
     * new instance created using the title of `obj`, the resource id of its jpg, and our field
     * `mCellHeight` as the height to use when layout parameters are created to display this item.
     * We initialize `boolean shouldAnimateInNewImage` with the value returned by the
     * `shouldAnimateInNewImage` method of `mListView` (it returns true if there are no
     * items in the list, or if the first item in the list is at the very top of the view, if the
     * first item is even slightly off screen it returns false).
     *
     *
     * If `shouldAnimateInNewImage` is false we just call the `addRow` method of `mListView`
     * to add `newObj` to the list then return. Otherwise we disable `mListView` and initialize
     * `ObjectAnimator animator` with the `ObjectAnimator` created by the `getScalingAnimator`
     * method of our field `RoundView mRoundView` that will animate the scale of `mRoundView`,
     * making it shrink to 0.3 its starting size then grow back to its original size. We then add an
     * `AnimatorListenerAdapter` to `animator` whose `onAnimationRepeat` calls the
     * `addRow` method of `mListView` to add `newObj` to the list.
     *
     *
     * Finally we start `animator` running.
     *
     * @param view view that was clicked (the `Button` with id R.id.add_row_button ("Add Row")
     * calls us when it is clicked thanks to an android:onClick="addRow" attribute).
     */
    @Suppress("UNUSED_PARAMETER")
    fun addRow(view: View?) {
        mButton!!.isEnabled = false
        mItemNum++
        val obj = mValues[mItemNum % mValues.size]
        val newObj = ListItemObject(obj.title, obj.imgResource,
            mCellHeight)
        val shouldAnimateInNewImage = mListView!!.shouldAnimateInNewImage()
        if (!shouldAnimateInNewImage) {
            mListView!!.addRow(newObj)
            return
        }
        mListView!!.isEnabled = false
        val animator = mRoundView!!.scalingAnimator
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationRepeat(animation: Animator) {
                mListView!!.addRow(newObj)
            }
        })
        animator.start()
    }

    /**
     * Called when the animation of a new row addition begins from the `addRow` method of
     * `InsertionListView mListView`. We just disable our button `Button mButton`.
     */
    override fun onRowAdditionAnimationStart() {
        mButton!!.isEnabled = false
    }

    /**
     * Called when the animation of a new row addition ends from the `onAnimationEnd` override
     * of the `AnimatorSet` which animates the addition of a new row when the `addRow`
     * method of `InsertionListView mListView` is called. We just enable our `Button mButton`.
     */
    override fun onRowAdditionAnimationEnd() {
        mButton!!.isEnabled = true
    }
}