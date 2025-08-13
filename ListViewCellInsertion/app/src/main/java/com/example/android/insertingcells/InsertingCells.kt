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
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This application creates a [ListView] to which new elements can be added from the top. When a new
 * element is added, it is animated from above the bounds of the [ListView] to the top of the
 * [ListView]. When the list is scrolled all the way to the top and a new element is added, the row
 * animation is accompanied by an image animation that pops out of the round view and pops into the
 * correct position in the top cell.
 */
class InsertingCells : ComponentActivity(), OnRowAdditionAnimationListener {
    /**
     * Contains the [ListItemObject] objects we cycle through round robin in our method
     * [addRow], which then clones the chosen one, and adds the clone to our [ListView].
     */
    private lateinit var mValues: Array<ListItemObject>

    /**
     * The [InsertionListView] with id `R.id.list_view` in our layout that we add our
     * [ListItemObject] objects to the top of, animating the addition if the current
     * top item is at the top of the [InsertionListView].
     */
    private var mListView: InsertionListView? = null

    /**
     * The [Button] with id `R.id.add_row_button` in our layout, its android:onClick="addRow"
     * attribute makes it call our [addRow] method when it is clicked.
     */
    private var mButton: Button? = null

    /**
     * Index into the [Array] of [ListItemObject] field [mValues] (modulo the length of [mValues])
     * of the last [ListItemObject] that our [addRow] method cloned and added to [InsertionListView]
     * field [mListView], it is incremented before each use when adding the next.
     */
    private var mItemNum = 0

    /**
     * The [RoundView] with id `R.id.round_view` in our layout, an image animation emerges from
     * it when the list is scrolled all the way to the top and a new [ListItemObject] is added.
     */
    private var mRoundView: RoundView? = null

    /**
     * Set from the resource with id `R.dimen.cell_height` (150dp), it is used when creating a clone
     * of an [ListItemObject] in our [addRow] method, the [ListItemObject] constructor saves this
     * value in its `mHeight` field for use by the `getView` override of our [CustomArrayAdapter]
     * adapter when configuring the layout params for the item's view.
     */
    private var mCellHeight: Int = 0

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_main`. We initialize our
     * [Array] of [ListItemObject] field [mValues] with a new instance, filled with three
     * [ListItemObject] instances constructed to contain "Chameleon", "Rock", and "Flower" text
     * views as well as the resource id's for jpg images to use for the associated [ImageView]
     * (`R.drawable.chameleon`, `R.drawable.rock`, and `R.drawable.flower` respectively). We
     * initialize our [Int] field [mCellHeight] by retrieving the dimensional for the resource
     * id `R.dimen.cell_height` from a [Resources] instance for our application's package. We
     * initialize [List] of [ListItemObject] variable `val mData` with a new instance, and
     * [CustomArrayAdapter] variable `val mAdapter` with an instance which will display the
     * [ListItemObject] items in `mData` using our layout file `R.layout.list_view_item`. We
     * initialize [RelativeLayout] variable `val mLayout` by finding the view with id
     * `R.id.relative_layout` (it contains all our other views). We then initialize our fields
     * [RoundView] field [mRoundView] by finding the view with id `R.id.round_view`, [Button] field
     * [mButton] by finding the view with id `R.id.add_row_button` ("Add Row"), and
     * [InsertionListView] field [mListView] by finding the view with id `R.id.list_view`. We then
     * set the adapter of [mListView] to `mAdapter`, its data to `mData`, its parent [RelativeLayout]
     * to `mLayout` and its [OnRowAdditionAnimationListener] to 'this'.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<RelativeLayout>(R.id.relative_layout)
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
        mValues = arrayOf(
            ListItemObject("Chameleon", R.drawable.chameleon, 0),
            ListItemObject("Rock", R.drawable.rock, 0),
            ListItemObject("Flower", R.drawable.flower, 0)
        )
        mCellHeight = resources.getDimension(R.dimen.cell_height).toInt()
        val mData: List<ListItemObject> = ArrayList()
        val mAdapter = CustomArrayAdapter(this, R.layout.list_view_item, mData)
        mRoundView = findViewById(R.id.round_view)
        mButton = findViewById(R.id.add_row_button)
        mListView = findViewById(R.id.list_view)
        mListView!!.adapter = mAdapter
        mListView!!.setData(mData)
        mListView!!.setLayout(rootView)
        mListView!!.setRowAdditionAnimationListener(this)
    }

    /**
     * Called when the [Button] with id `R.id.add_row_button` ("Add Row") is clicked, we add a new
     * [ListItemObject] cloned from one of those in [Array] of [ListItemObject] field [mValues] to
     * our layout's [InsertionListView] field [mListView]. First we disable our [Button] field
     * [mButton], then we increment our [Int] field [mItemNum]. We initialize [ListItemObject]
     * variable `val obj` with the [ListItemObject] in our [Array] of [ListItemObject] field
     * [mValues] with index [mItemNum] modulo the length of [mValues]. We then initialize
     * [ListItemObject] variable `val newObj` with a new instance created using the
     * [ListItemObject.title] title of `obj`, the resource id of its jpg [ListItemObject.imgResource],
     * and our [Int] field [mCellHeight] as the height to use when layout parameters are created to
     * display this item. We initialize [Boolean] variable `val shouldAnimateInNewImage` with the
     * value returned by the [InsertionListView.shouldAnimateInNewImage] method of [mListView] (it
     * returns `true` if there are no items in the list, or if the first item in the list is at the
     * very top of the view, if the first item is even slightly off screen it returns `false`).
     *
     * If `shouldAnimateInNewImage` is `false` we just call the [InsertionListView.addRow] method of
     * [mListView] to add `newObj` to the list then return. Otherwise we disable [mListView] and
     * initialize [ObjectAnimator] variable `val animator` with the [ObjectAnimator] created by the
     * [RoundView.scalingAnimator] property of our [RoundView] field [mRoundView] that will animate
     * the scale of [mRoundView], making it shrink to 0.3 of its starting size then grow back to its
     * original size. We then add an [AnimatorListenerAdapter] to `animator` whose
     * [AnimatorListenerAdapter.onAnimationRepeat] override calls the [InsertionListView.addRow]
     * method of [mListView] to add `newObj` to the list. Finally we start `animator` running.
     *
     * @param view the [View] that was clicked (the [Button] with id `R.id.add_row_button`
     * ("Add Row") calls us when it is clicked thanks to an android:onClick="addRow" attribute).
     */
    @Suppress("UNUSED_PARAMETER", "RedundantSuppression")
    fun addRow(view: View?) {
        mButton!!.isEnabled = false
        mItemNum++
        val obj: ListItemObject = mValues[mItemNum % mValues.size]
        val newObj = ListItemObject(obj.title, obj.imgResource, mCellHeight)
        val shouldAnimateInNewImage: Boolean = mListView!!.shouldAnimateInNewImage()
        if (!shouldAnimateInNewImage) {
            mListView!!.addRow(newObj)
            return
        }
        mListView!!.isEnabled = false
        val animator: ObjectAnimator = mRoundView!!.scalingAnimator
        animator.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the repetition of the animation.
             *
             * @param animation – The animation which was repeated.
             */
            override fun onAnimationRepeat(animation: Animator) {
                mListView!!.addRow(newObj)
            }
        })
        animator.start()
    }

    /**
     * Called when the animation of a new row addition begins from the [InsertionListView.addRow]
     * method of [InsertionListView] field [mListView]. We just disable our [Button] field [mButton].
     */
    override fun onRowAdditionAnimationStart() {
        mButton!!.isEnabled = false
    }

    /**
     * Called when the animation of a new row addition ends from the
     * [AnimatorListenerAdapter.onAnimationEnd] override of the [AnimatorSet] which animates the
     * addition of a new row when the [InsertionListView.addRow] method of [InsertionListView]
     * [mListView] is called. We just enable our [Button] field [mButton].
     */
    override fun onRowAdditionAnimationEnd() {
        mButton!!.isEnabled = true
    }
}
