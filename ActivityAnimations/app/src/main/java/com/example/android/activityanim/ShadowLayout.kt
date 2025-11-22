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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.activityanim

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation

/**
 * This custom layout paints a drop shadow behind all children. The size and opacity
 * of the drop shadow is determined by a "depth" factor that can be set and animated.
 */
class ShadowLayout : RelativeLayout {
    /**
     * [Paint] used in our [onDraw] override to draw the shadows on the canvas.
     */
    var mShadowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Value used to calculate the alpha to use for [Paint] property [mShadowPaint] as well as
     * the offset to move the [Canvas] before drawing in order to simulate a shadow effect.
     */
    var mShadowDepth: Float = 0f

    /**
     * Black rounded rectangle used as our shadow image.
     */
    var mShadowBitmap: Bitmap? = null

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We call our super's constructor then call our method [init] to initialize
     * our instance. UNUSED
     *
     * @param context The Context the view is running in, through which it can
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
        init()
    }

    /**
     * Perform inflation from XML. We call our super's constructor then call our method `init`
     * to initialize our instance. This is the constructor that is used by our layout file.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    /**
     * Our one argument constructor We call our super's constructor then call our method `init`
     * to initialize our instance.  UNUSED
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context) {
        init()
    }

    /**
     * TODO: Continue here.
     * Called by the constructors - sets up the drawing parameters for the drop shadow. We set the
     * color of our field `Paint mShadowPaint` to BLACK, and its style to FILL. We call the
     * `setWillNotDraw(false)` to clear the will not draw flag since our `onDraw` override
     * will do some drawing. We initialize our field `Bitmap mShadowBitmap` with an instance that
     * is the same size as our field `Rect sShadowRect` and uses ARGB_8888 as its config. We
     * initialize `Canvas c` with an instance which will draw into `mShadowBitmap`. We
     * set the `MaskFilter` of `mShadowPaint` to a new instance of `BlurMaskFilter`
     * whose blur radius is BLUR_RADIUS and whose blur style is NORMAL (blur inside and outside the
     * original border). We translate `Canvas c` by BLUR_RADIUS horizontally and vertically,
     * then draw a round rectangle on it (and thus into `mShadowBitmap`) whose rectangular bounds
     * are given by `sShadowRectF`, with the width of `sShadowRectF` divided by 40 for the
     * x-radius of the corners ovals, and the height of  `sShadowRectF` divided by 40 for the
     * y-radius of the corners ovals.
     */
    private fun init() {
        mShadowPaint.color = Color.BLACK
        mShadowPaint.style = Paint.Style.FILL
        setWillNotDraw(false)
        mShadowBitmap = createBitmap(sShadowRect.width(), sShadowRect.height())
        val c = Canvas(mShadowBitmap!!)
        mShadowPaint.maskFilter = BlurMaskFilter(BLUR_RADIUS.toFloat(), Blur.NORMAL)
        c.translate(BLUR_RADIUS.toFloat(), BLUR_RADIUS.toFloat())
        c.drawRoundRect(
            sShadowRectF, sShadowRectF.width() / 40,
            sShadowRectF.height() / 40, mShadowPaint
        )
    }

    /**
     * The "depth" factor determines the offset distance and opacity of the shadow (shadows that
     * are further away from the source are offset greater and are more translucent). If the new
     * `depth` is equal to our field `float mShadowDepth` we do nothing, otherwise we
     * set `mShadowDepth` to `depth`, and set the alpha of our field `Paint mShadowPaint`
     * to 1 minus `mShadowDepth` times 150 plus 100, then we call the `invalidate` method
     * so that our `onDraw` override will be called to redraw.
     *
     * @param depth offset distance and opacity of the shadow
     */
    // it is actually used by the animations
    @Suppress("unused")
    fun setShadowDepth(depth: Float) {
        if (depth != mShadowDepth) {
            mShadowDepth = depth
            mShadowPaint.alpha = (100 + 150 * (1 - mShadowDepth)).toInt()
            invalidate() // We need to redraw when the shadow attributes change
        }
    }

    /**
     * We implement this to do our drawing. Overriding onDraw allows us to draw shadows behind every
     * child of this container. onDraw() is called to draw a layout's content before the children are
     * drawn, so the shadows will be drawn first, behind the children (which is what we want). We loop
     * over `int i` for each of our children:
     *
     *  *
     * We initialize `View child` with the `i`'th child, and if the visibility
     * of the `child` is not VISIBLE, or its alpha is 0 we skip it.
     *
     *  *
     * We initialize `int depthFactor` with 80 times our field `mShadowDepth`.
     *
     *  *
     * We save the state of our parameter `Canvas canvas` on its private stack, then
     * translate it to the left side of `child` plus `depthFactor` in the X
     * direction, and the top side of `child` plus `depthFactor` in the X direction.
     *
     *  *
     * We pre-concatenate the current matrix of `canvas` with the transform matrix of
     * `child`.
     *
     *  *
     * We set the `right` field of our field `RectF tempShadowRectF` to the
     * width of `child` and its `bottom` field to the height of `child`
     *
     *  *
     * We draw our field `Bitmap mShadowBitmap` on `canvas` using `Rect sShadowRect`
     * to define the subset of the bitmap to be drawn (the entire bitmap), `tempShadowRectF`
     * as the rectangle that the bitmap will be scaled/translated to fit into, and using our
     * field `Paint mShadowPaint` as the paint.
     *
     *  *
     * Then we loop around to handle the next child.
     *
     *
     *
     * @param canvas the canvas on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            if (child.visibility != VISIBLE || child.alpha == 0f) {
                continue
            }
            val depthFactor: Int = (80 * mShadowDepth).toInt()
            canvas.withTranslation(
                x = (child.left + depthFactor).toFloat(),
                y = (child.top + depthFactor).toFloat()
            ) {
                concat(child.matrix)
                tempShadowRectF.right = child.width.toFloat()
                tempShadowRectF.bottom = child.height.toFloat()
                drawBitmap(mShadowBitmap!!, sShadowRect, tempShadowRectF, mShadowPaint)
            }
        }
    }

    companion object {
        /**
         * Radius to extend the blur from the original mask when constructing the `BlurMaskFilter`
         * we use as the `MaskFilter` object for `Paint mShadowPaint` (a `BlurMaskFilter`
         * blurs the edge of the alpha-channel mask before drawing it).
         */
        const val BLUR_RADIUS: Int = 6

        /**
         * `RectF` we use when drawing a round rect offset by BLUR_RADIUS into `Bitmap mShadowBitmap`
         */
        val sShadowRectF: RectF = RectF(
            /* left = */ 0f,
            /* top = */ 0f,
            /* right = */ 200f,
            /* bottom = */ 200f
        )

        /**
         * The `Rect` which includes the blur radius, it is used to size `Bitmap mShadowBitmap`
         */
        val sShadowRect: Rect = Rect(
            /* left = */ 0,
            /* top = */ 0,
            /* right = */ 200 + 2 * BLUR_RADIUS,
            /* bottom = */ 200 + 2 * BLUR_RADIUS
        )

        /**
         * `RectF` we use as the rectangle that the `Bitmap mShadowBitmap` will be scaled or
         * translated to fit into when drawing it in our `onDraw` override, its coordinates are
         * calculated for each of our children.
         */
        var tempShadowRectF: RectF = RectF(
            /* left = */ 0f,
            /* top = */ 0f,
            /* right = */ 0f,
            /* bottom = */ 0f
        )
    }
}