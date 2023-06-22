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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.toongame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

/**
 * This custom TextView can be skewed to the left or right to enable anticipation and
 * follow-through effects
 */
class SkewableTextView : TextView {
    /**
     * Our "skewX" property, we use it in our `onDraw` override to skew the canvas.
     */
    private var mSkewX = 0f

    /**
     * `RectF` used by our `invalidateSkewedBounds` method to calculate the proper area
     * of our parent to invalidate given our skewed bounds.
     */
    var mTempRect: RectF = RectF()

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We just call our super's constructor.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a
     * reference to a style resource that supplies default values for
     * the view. Can be 0 to not look for defaults.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We just call our super's constructor.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Our one argument constructor. We just call our super's constructor.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context)

    /**
     * We implement this to do our drawing. If our field `float mSkewX` is not 0 we pre-concat
     * the current matrix with a translation by our height in the Y direction, pre-concat a matrix
     * which skews X by `mSkewX` and then translate the canvas by minus our height. When our
     * field `mSkewX` is 0 or non-zero we then call our super;s implementation of `onDraw`.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        if (mSkewX != 0f) {
            canvas.translate(0f, height.toFloat())
            canvas.skew(mSkewX, 0f)
            canvas.translate(0f, -height.toFloat())
        }
        super.onDraw(canvas)
    }
    /**
     * Getter for our field `float mSkewX`.
     *
     * @return current value of our field `float mSkewX`.
     */
    var skewX: Float
        get() = mSkewX
        /**
         * Setter for our field `float mSkewX`. If our parameter `float value` is not equal
         * to our field `float mSkewX` we set `mSkewX` to `value`, invalidate our view,
         * then call our method `invalidateSkewedBounds` to invalidate the appropriate area of our
         * parent.
         *
         * @param value new value for our field `float mSkewX`.
         */
        set(value) {
            if (value != mSkewX) {
                mSkewX = value
                invalidate() // force redraw with new skew value
                invalidateSkewedBounds() // also invalidate appropriate area of parent
            }
        }

    /**
     * Need to invalidate proper area of parent for skewed bounds. If our field `float mSkewX`
     * is not 0, we initialize `Matrix matrix` with a new instance, and set it to skew by
     * `mSkewX` in the X dimension and 0 in the Y. We then set `RectF mTempRect` to
     * the dimensions of our view. We apply then `matrix` to `mTempRect`. We then offset
     * `mTempRect` to our left side plus our translationX property in the X direction, and
     * our top size plus our translationY property in the X direction. We then fetch our parent view
     * and call its `invalidate` method to invalidate the area from the left top corner of
     * `mTempRect` to the right bottom of `mTempRect` (after adding .5 to round it up).
     */
    private fun invalidateSkewedBounds() {
        if (mSkewX != 0f) {
            val matrix = Matrix()
            matrix.setSkew(-mSkewX, 0f)
            mTempRect[0f, 0f, right.toFloat()] = bottom.toFloat()
            matrix.mapRect(mTempRect)
            mTempRect.offset(left + translationX, top + translationY)
            @Suppress("DEPRECATION") // Should just call invalidate() here, but WTH
            (parent as View).invalidate(
                mTempRect.left.toInt(),
                mTempRect.top.toInt(),
                (mTempRect.right + .5f).toInt(),
                (mTempRect.bottom + .5f).toInt()
            )
        }
    }
}