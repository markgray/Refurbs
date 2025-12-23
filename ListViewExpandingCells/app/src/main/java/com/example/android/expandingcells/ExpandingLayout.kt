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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "UnusedImport")

package com.example.android.expandingcells

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.widget.RelativeLayout

/**
 * This layout is used to contain the extra information that will be displayed when a certain cell
 * is expanded. The custom relative layout is created in order to achieve a fading affect of this
 * layout's contents as it is being expanded or collapsed as opposed to just fading the content
 * in(out) after(before) the cell expands(collapses).
 *
 * During expansion, layout takes place so the full contents of this layout can be displayed. When
 * the size changes to display the full contents of the layout, its height is stored. When the view
 * is collapsing, this layout's height becomes 0 since it is no longer in the visible part of the
 * cell. By overriding [onMeasure], and setting the height back to its max height, it is still
 * visible during the collapse animation, and so, a fade out effect can be achieved.
 */
class ExpandingLayout : RelativeLayout {
    /**
     * The [OnSizeChangedListener] whose [OnSizeChangedListener.onSizeChanged] override we call with
     * our new height when size changes are reported to our [onSizeChanged] override.
     */
    private var mSizeChangedListener: OnSizeChangedListener? = null

    /**
     * Height of our view when in the expanded state.
     */
    private var mExpandedHeight: Int = -1

    /**
     * Our one argument constructor. We just call our super's constructor. UNUSED
     *
     * @param context The [Context] the [View] is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context)

    /**
     * Perform inflation from XML. We just call our super's constructor. This is the one that is used.
     *
     * @param context The [Context] the [View] is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. This constructor of [View] allows subclasses to use their own base style when
     * they are inflating. We just call our super's constructor. UNUSED
     *
     * @param context The [Context] the [View] is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     * resource that supplies default values for the [View]. Can be 0 to not look for defaults.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    /**
     * Measure the view and its content to determine the measured width and the measured height.
     * This method is invoked by [measure] and should be overridden by subclasses to provide
     * accurate and efficient measurement of their contents. First we initialize our [Int] variable
     * `heightMeasureSpecLocal` to our [Int] parameter [widthMeasureSpec], then if
     * `heightMeasureSpecLocal` is greater than zero we set  `heightMeasureSpecLocal` to a measure
     * specification based on a size of [mExpandedHeight], and a mode of [MeasureSpec.AT_MOST]
     * (child can be as large as it wants up to the specified size). Finally we call our super's
     * implementation of `onMeasure` with our [Int] parameter [widthMeasureSpec] and our [Int]
     * variable `heightMeasureSpecLocal`.
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
     * The requirements are encoded with [MeasureSpec].
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     * The requirements are encoded with [MeasureSpec].
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpecLocal: Int = heightMeasureSpec
        if (mExpandedHeight > 0) {
            heightMeasureSpecLocal = MeasureSpec.makeMeasureSpec(
                /* size = */ mExpandedHeight,
                /* mode = */ MeasureSpec.AT_MOST
            )
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpecLocal)
    }

    /**
     * This is called during layout when the size of this view has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0. First we set our [Int] field
     * [mExpandedHeight] to our [Int] parameter [h], then we call the
     * [OnSizeChangedListener.onSizeChanged] override of our [OnSizeChangedListener] field
     * [OnSizeChangedListener] to notify the list data object corresponding to this layout that its
     * size has changed.
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mExpandedHeight = h
        //Notifies the list data object corresponding to this layout that its size has changed.
        mSizeChangedListener!!.onSizeChanged(h)
    }

    /**
     * Public access to our [Int] field [mExpandedHeight], the expanded height field of the object
     * we are to be displaying
     */
    var expandedHeight: Int
        get() = mExpandedHeight
        /**
         * Called from the [CustomArrayAdapter.getView] override of [CustomArrayAdapter] to set our
         * [Int] field [mExpandedHeight] to the value of the expanded height field of the object we
         * are to be displaying. We just save our [Int] parameter [expandedHeight] in our field
         * [mExpandedHeight].
         *
         * @param expandedHeight expanded height field of the object we are to be displaying.
         */
        set(expandedHeight) {
            if (expandedHeight != -1) {
                Log.i(TAG, "Expanded height is to be: $expandedHeight")
            }
            mExpandedHeight = expandedHeight
        }

    /**
     * Setter for our [OnSizeChangedListener] field [mSizeChangedListener].
     *
     * @param listener the [OnSizeChangedListener] whose [OnSizeChangedListener.onSizeChanged]
     * override we should call when our size changes
     */
    fun setSizeChangedListener(listener: OnSizeChangedListener?) {
        mSizeChangedListener = listener
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "ExpandingLayout"
    }
}