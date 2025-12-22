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

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * This round view draws a circle from which the image pops out of and into
 * the corresponding cell in the list.
 */
class RoundView : View {

    /**
     * [Paint] we use to draw our circle in our [onDraw] override.
     */
    private var mPaint: Paint? = null

    /**
     * Our one argument constructor. First we call our super's constructor, then we call our
     * [inititialize] method to allocate and configure the [Paint] field [mPaint] that our [onDraw]
     * override uses to draw us. UNUSED
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context) {
        inititialize()
    }

    /**
     * Perform inflation from XML. First we call our super's constructor, then we call our
     * [inititialize] method to allocate and configure the [Paint] field [mPaint] our [onDraw]
     * override uses to draw us. This is the one which is used by our layout file
     * layout/activity_main.xml
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        inititialize()
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * First we call our super's constructor, then we call our [inititialize] method to allocate
     * and configure the [Paint] field [mPaint] our [onDraw] override uses to draw us. UNUSED
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a
     * reference to a style resource that supplies default values for
     * the view. Can be 0 to not look for defaults.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        inititialize()
    }

    /**
     * Called by our constructors to allocate and configure the [Paint] field [mPaint] used by our
     * [onDraw] override to draw us. We initialize [Paint] field [mPaint] with a new instance, set
     * its ANTI_ALIAS_FLAG, set its color to [Color.WHITE], set its style to [Paint.Style.STROKE],
     * and set its stroke width to [STROKE_WIDTH] (6).
     */
    private fun inititialize() {
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.color = Color.WHITE
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeWidth = STROKE_WIDTH.toFloat()
    }

    /**
     * We implement this to do our drawing. We call the [Canvas.drawCircle] method of our [Canvas]
     * parameter [canvas] to draw a circle of radius [RADIUS], centered at the center of our view
     * using [Paint] field [mPaint] as the [Paint].
     *
     * @param canvas the [Canvas] on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(
            /* cx = */ width / 2f,
            /* cy = */ height / 2f,
            /* radius = */ RADIUS.toFloat(),
            /* paint = */ mPaint!!
        )
    }

    /**
     * Creates and returns an [ObjectAnimator] which will shrink 'this' [View] down to [SCALE_FACTOR]
     * by [SCALE_FACTOR] of its original size, then back to its original size. First we initialize
     * [PropertyValuesHolder] variable `val imgViewScaleY` with an instance that will animate the
     * [View.SCALE_Y] property down to [SCALE_FACTOR], and [PropertyValuesHolder] variable
     * `val imgViewScaleX` with an instance that will animate the [View.SCALE_X] property down to
     * [SCALE_FACTOR]. Then we initialize [ObjectAnimator] variable `val imgViewScaleAnimator` with
     * an instance that will apply both of them to 'this', set its repeat count to 1, its repeat
     * mode to [ValueAnimator.REVERSE], and its duration to [ANIMATION_DURATION]. Finally we return
     * `imgViewScaleAnimator` to the caller.
     *
     * @return an [ObjectAnimator] which will shrink 'this' [View] down to [SCALE_FACTOR]
     * by [SCALE_FACTOR] of its original size, then back to its original size.
     */
    val scalingAnimator: ObjectAnimator
        get() {
            val imgViewScaleY = PropertyValuesHolder.ofFloat(
                /* property = */ SCALE_Y,
                /* ...values = */ SCALE_FACTOR
            )
            val imgViewScaleX = PropertyValuesHolder.ofFloat(
                /* property = */ SCALE_X,
                /* ...values = */ SCALE_FACTOR
            )
            val imgViewScaleAnimator =
                ObjectAnimator.ofPropertyValuesHolder(
                    /* target = */ this,
                    /* ...values = */ imgViewScaleX, imgViewScaleY
                )
            imgViewScaleAnimator.repeatCount = 1
            imgViewScaleAnimator.repeatMode = ValueAnimator.REVERSE
            imgViewScaleAnimator.duration = ANIMATION_DURATION.toLong()
            return imgViewScaleAnimator
        }

    companion object {
        /**
         * The width to use for stroking of [Paint] field [mPaint], which is used to draw our circle.
         */
        private const val STROKE_WIDTH = 6

        /**
         * The radius of our circle, used in the call to the [Canvas.drawCircle] method of the
         * [Canvas] passed to our [onDraw] override.
         */
        private const val RADIUS = 20

        /**
         * Duration of our scaling animation in milliseconds.
         */
        private const val ANIMATION_DURATION = 300

        /**
         * Factor by which to scale both X and Y sizes of our view in our scaling animation.
         */
        private const val SCALE_FACTOR = 0.3f
    }
}