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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import com.example.android.common.view.SlidingTabLayout.TabColorizer

/**
 * Horizontal `LinearLayout` which holds all the tabs for our sliding tab UI.
 */
internal class SlidingTabStrip
/**
 * Perform inflation from XML and apply a class-specific base style from a theme attribute or
 * style resource. First we call our super's constructor, then we call the `setWillNotDraw`
 * method to clear the "will not draw flag" so that our `onDraw` override will be called.
 * We initialize `float density` with the logical density of the display in order to use it
 * to scale DIPS to pixels. We initialize `TypedValue outValue` with a new instance then
 * store the value that the `resolveAttribute` method of the Theme object associated with
 * `Context context` resolves for the attribute colorForeground in it. We then initialize
 * `int themeForegroundColor` with the `data` field of `outValue`. We initialize
 * our field `int mDefaultBottomBorderColor` by calling our method `setColorAlpha` to
 * create a new color by replacing the alpha component of `themeForegroundColor` with
 * DEFAULT_BOTTOM_BORDER_COLOR_ALPHA (0x26). We initialize `SimpleTabColorizer mDefaultTabColorizer`
 * with a new instance, then set its indicator colors to DEFAULT_SELECTED_INDICATOR_COLOR (0xFF33B5E5),
 * and its divider colors to the color our method `setColorAlpha` creates by replacing the alpha
 * component of `themeForegroundColor` with DEFAULT_DIVIDER_COLOR_ALPHA (0x20). We initialize
 * our field `mBottomBorderThickness` by multiplying DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS by
 * `density`, then initialize our field `Paint mBottomBorderPaint` with a new instance
 * and set its color to `mDefaultBottomBorderColor`. We initialize our field `mSelectedIndicatorThickness`
 * by multiplying SELECTED_INDICATOR_THICKNESS_DIPS by `density`, and initialize our field
 * `Paint mSelectedIndicatorPaint` with a new instance. We initialize our field `mDividerHeight`
 * to DEFAULT_DIVIDER_HEIGHT (0.5f), initialize our field `Paint mDividerPaint` with a new
 * instance and set its stroke width to DEFAULT_DIVIDER_THICKNESS_DIPS multiplied by `density`.
 *
 * @param context The Context the view is running in, through which it can
 * access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    /**
     * DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS converted to pixels, used as the bottom border thickness.
     */
    private val mBottomBorderThickness: Int

    /**
     * `Paint` used to draw the bottom border, uses `mDefaultBottomBorderColor` as its color.
     */
    private val mBottomBorderPaint: Paint

    /**
     * SELECTED_INDICATOR_THICKNESS_DIPS in pixels, used as the height of the rectangle at the bottom
     * of the tabs which indicates the selected tab.
     */
    private val mSelectedIndicatorThickness: Int

    /**
     * `Paint` used to draw the rectangle at the bottom of the tabs which indicates the selected
     * tab (color varies depending on the tab, and whether it is selected or not).
     */
    private val mSelectedIndicatorPaint: Paint

    /**
     * Color used to draw the bottom border, `colorForeground` with DEFAULT_BOTTOM_BORDER_COLOR_ALPHA
     * as its alpha.
     */
    private val mDefaultBottomBorderColor: Int

    /**
     * `Paint` used to draw the separator between tabs
     */
    private val mDividerPaint: Paint

    /**
     * Just a copy of DEFAULT_DIVIDER_HEIGHT
     */
    private val mDividerHeight: Float

    /**
     * Which position in our associated `ViewPager` is currently selected.
     */
    private var mSelectedPosition = 0

    /**
     * Offset of the partially onscreen selected page in our associated `ViewPager`
     */
    private var mSelectionOffset = 0f

    /**
     * Custom `TabColorizer` to use to set the colors for each tab, set by our
     * `setCustomTabColorizer` method, called by the `setCustomTabColorizer`
     * method of `SlidingTabLayout` which is not used by this sample app.
     */
    private var mCustomTabColorizer: TabColorizer? = null

    /**
     * `TabColorizer` which uses only a single color for selected indicator, and another
     * single color for the divider.
     */
    private val mDefaultTabColorizer: SimpleTabColorizer

    init {
        setWillNotDraw(false)
        val density = resources.displayMetrics.density
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorForeground, outValue, true)
        val themeForegroundColor = outValue.data
        mDefaultBottomBorderColor = setColorAlpha(themeForegroundColor, DEFAULT_BOTTOM_BORDER_COLOR_ALPHA)
        mDefaultTabColorizer = SimpleTabColorizer()
        mDefaultTabColorizer.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR)
        mDefaultTabColorizer.setDividerColors(setColorAlpha(themeForegroundColor, DEFAULT_DIVIDER_COLOR_ALPHA))
        mBottomBorderThickness = (DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density).toInt()
        mBottomBorderPaint = Paint()
        mBottomBorderPaint.color = mDefaultBottomBorderColor
        mSelectedIndicatorThickness = (SELECTED_INDICATOR_THICKNESS_DIPS * density).toInt()
        mSelectedIndicatorPaint = Paint()
        mDividerHeight = DEFAULT_DIVIDER_HEIGHT
        mDividerPaint = Paint()
        mDividerPaint.strokeWidth = (DEFAULT_DIVIDER_THICKNESS_DIPS * density).toInt().toFloat()
    }

    /**
     * Setter for our `TabColorizer mCustomTabColorizer` field. We save our parameter in our
     * field `mCustomTabColorizer` then call the `invalidate` method to invalidate the
     * whole view so that our `onDraw` method will be called.
     *
     * @param customTabColorizer `TabColorizer` that we should use.
     */
    fun setCustomTabColorizer(customTabColorizer: TabColorizer?) {
        mCustomTabColorizer = customTabColorizer
        invalidate()
    }

    /**
     * Set the array of colors that our `SimpleTabColorizer mDefaultTabColorizer` will use to
     * draw the tabs. First we set our field `TabColorizer mCustomTabColorizer` to null so that
     * it will no longer be used, then we call the `setIndicatorColors` method of our field
     * `SimpleTabColorizer mDefaultTabColorizer` to set its indicator colors to our parameter
     * `colors`. Finally we call the `invalidate` method to invalidate the whole view so
     * that our `onDraw` method will be called.
     *
     * @param colors array or varargs containing 1 or more colors.
     */
    fun setSelectedIndicatorColors(vararg colors: Int) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null
        mDefaultTabColorizer.setIndicatorColors(*colors)
        invalidate()
    }

    /**
     * Set the array of colors that our `SimpleTabColorizer mDefaultTabColorizer` will use to
     * draw the dividers. First we set our field `TabColorizer mCustomTabColorizer` to null so
     * that it will no longer be used, then we call the `setDividerColors` method of our field
     * `SimpleTabColorizer mDefaultTabColorizer` to set its indicator colors to our parameter
     * `colors`. Finally we call the `invalidate` method to invalidate the whole view so
     * that our `onDraw` method will be called.
     *
     * @param colors array or varargs containing 1 or more colors.
     */
    fun setDividerColors(vararg colors: Int) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null
        mDefaultTabColorizer.setDividerColors(*colors)
        invalidate()
    }

    /**
     * Called when the current page of our associated `ViewPager` is scrolled. We save our parameter
     * `position` in our field `mSelectedPosition`, and our parameter `positionOffset`
     * in our field `mSelectionOffset` then call the `invalidate` method to invalidate the
     * whole view so that our `onDraw` method will be called.
     *
     * @param position       Position index of the first page currently being displayed.
     * Page position+1 will be visible if positionOffset is nonzero.
     * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
     */
    fun onViewPagerPageChanged(position: Int, positionOffset: Float) {
        mSelectedPosition = position
        mSelectionOffset = positionOffset
        invalidate()
    }

    /**
     * We implement this to do our drawing. We initialize `int height` with the height of our
     * view, and `int childCount` with the number of children in our group. We initialize
     * `int dividerHeightPx` with the int value of `height` times the minimum of our
     * field `mDividerHeight` and 1.0f (assuming `mDividerHeight` is larger than 0f).
     * If our field `mCustomTabColorizer` is not null we we `TabColorizer tabColorizer`
     * to it, otherwise we set it to `mDefaultTabColorizer`.
     *
     *
     * If `childCount` is greater than 0, we initialize `View selectedTitle` with our
     * child view at `mSelectedPosition`, `int left` with the left edge of this view, in
     * pixels and `int right` with the right edge of this view, in pixels. We initialize
     * `int color` with the color returned by the `getIndicatorColor` method of
     * `tabColorizer` for `mSelectedPosition`. If `mSelectionOffset` is greater than
     * 0 (page is partially off the screen) and `mSelectedPosition` is less than the last of our
     * children tabs, we initialize `int nextColor` with the color that the `getIndicatorColor`
     * method of `tabColorizer` returns for the next tab and if `color` is not equal to
     * `nextColor` we set color to the color that our `blendColors` calculates when it blends
     * `nextColor` and `color` at the ratio given by `mSelectionOffset`. We then
     * initialize `View nextTitle` with the view for the next tab after `mSelectedPosition`,
     * and recalculate `left` and `right` so that they accurately portray the fact that the
     * page is offset by `mSelectionOffset`.
     *
     *
     * Now we set the color of `mSelectedIndicatorPaint` to `color` and then we use it to
     * draw a rectangle on our parameter `Canvas canvas` whose top left corner is at
     * (`left`, `height-mSelectedIndicatorThickness`) and whose bottom right corner is at
     * (`right`,`height`) (our indicator for the selected page). We then draw a thin underline
     * along the entire bottom edge of `Canvas canvas`, a rectangle whose top left corner is at
     * (0,`height-mBottomBorderThickness`), whose right bottom corner is at (`right`,`height`)
     * and whose `Paint` is `mBottomBorderPaint`.
     *
     *
     * We initialize `int separatorTop` to the quantity one half of the difference between `height`
     * and `dividerHeightPx`. We then loop over `int i` for all of our children tabs, setting
     * `View child` to each child in turn, setting the color of `Paint mDividerPaint` to the
     * color that the `getDividerColor` method of `tabColorizer` specifies for child `i`,
     * and then drawing a vertical line on `Canvas canvas` along the right edge of  `child`
     * from `separatorTop` down to `separatorTop+dividerHeightPx` using `mDividerPaint`
     * as the `Paint`.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        val height = height
        val childCount = childCount
        val dividerHeightPx = (Math.min(Math.max(0f, mDividerHeight), 1f) * height).toInt()
        val tabColorizer = if (mCustomTabColorizer != null) mCustomTabColorizer!! else mDefaultTabColorizer

        // Thick colored underline below the current selection
        if (childCount > 0) {
            val selectedTitle = getChildAt(mSelectedPosition)
            var left = selectedTitle.left
            var right = selectedTitle.right
            var color = tabColorizer.getIndicatorColor(mSelectedPosition)
            if (mSelectionOffset > 0f && mSelectedPosition < getChildCount() - 1) {
                val nextColor = tabColorizer.getIndicatorColor(mSelectedPosition + 1)
                if (color != nextColor) {
                    color = blendColors(nextColor, color, mSelectionOffset)
                }

                // Draw the selection partway between the tabs
                val nextTitle = getChildAt(mSelectedPosition + 1)
                left = (mSelectionOffset * nextTitle.left +
                    (1.0f - mSelectionOffset) * left).toInt()
                right = (mSelectionOffset * nextTitle.right +
                    (1.0f - mSelectionOffset) * right).toInt()
            }
            mSelectedIndicatorPaint.color = color
            canvas.drawRect(left.toFloat(), (height - mSelectedIndicatorThickness).toFloat(), right.toFloat(),
                height.toFloat(), mSelectedIndicatorPaint)
        }

        // Thin underline along the entire bottom edge
        canvas.drawRect(0f, (height - mBottomBorderThickness).toFloat(), width.toFloat(), height.toFloat(), mBottomBorderPaint)

        // Vertical separators between the titles
        val separatorTop = (height - dividerHeightPx) / 2
        for (i in 0 until childCount - 1) {
            val child = getChildAt(i)
            mDividerPaint.color = tabColorizer.getDividerColor(i)
            canvas.drawLine(child.right.toFloat(), separatorTop.toFloat(), child.right.toFloat(),
                (
                    separatorTop + dividerHeightPx).toFloat(), mDividerPaint)
        }
    }

    /**
     * A basic `TabColorizer`
     */
    private class SimpleTabColorizer : TabColorizer {
        /**
         * The colors to use for the indicators, used in a round robin method depending on the tab
         */
        private lateinit var mIndicatorColors: IntArray

        /**
         * The colors to use for the dividers, used in a round robin method depending on the tab
         */
        private lateinit var mDividerColors: IntArray

        /**
         * Returns the color of the indicator for the given `position` position.
         *
         * @param position position we are to provide the color for.
         * @return return the color of the indicator used when `position` is selected.
         */
        override fun getIndicatorColor(position: Int): Int {
            return mIndicatorColors[position % mIndicatorColors.size]
        }

        /**
         * Returns the color of the divider drawn to the right of `position`.
         *
         * @param position position we are to provide the color for.
         * @return return the color of the divider drawn to the right of `position`.
         */
        override fun getDividerColor(position: Int): Int {
            return mDividerColors[position % mDividerColors.size]
        }

        /**
         * Setter for our field `int[] mIndicatorColors`.
         *
         * @param colors array or varags of `Colors`
         */
        fun setIndicatorColors(vararg colors: Int) {
            mIndicatorColors = colors
        }

        /**
         * Setter for our field `int[] mDividerColors`.
         *
         * @param colors array or varags of `Colors`
         */
        fun setDividerColors(vararg colors: Int) {
            mDividerColors = colors
        }
    }

    companion object {
        /**
         * Bottom border thickness for the thin underline at the bottom edge of the strip in DIPS
         */
        private const val DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 2

        /**
         * Alpha value used for the bottom border, used to modify the `colorForeground` resource.
         */
        private const val DEFAULT_BOTTOM_BORDER_COLOR_ALPHA: Byte = 0x26

        /**
         * Thickness of the rectangle at the bottom of the each tab which is used to indicate the
         * selected one in DIPS
         */
        private const val SELECTED_INDICATOR_THICKNESS_DIPS = 8

        /**
         * Color used to indicate which tab is selected (a shade of blue)
         */
        private const val DEFAULT_SELECTED_INDICATOR_COLOR = -0xcc4a1b

        /**
         * Stroke width of the divider line between tabs.
         */
        private const val DEFAULT_DIVIDER_THICKNESS_DIPS = 1

        /**
         * Alpha value used for the divider line between tabs, used to modify the `colorForeground`
         * resource.
         */
        private const val DEFAULT_DIVIDER_COLOR_ALPHA: Byte = 0x20

        /**
         * Default multiplier to use when calculating the height of the divider line in pixels
         */
        private const val DEFAULT_DIVIDER_HEIGHT = 0.5f

        /**
         * Set the alpha value of the `color` to be the given `alpha` value.
         *
         * @param color `Color` to start with
         * @param alpha alpha value to use instead
         * @return a `Color` whose alpha value is `alpha` and whose RGB values are those of `color`
         */
        private fun setColorAlpha(color: Int, alpha: Byte): Int {
            return Color.argb(alpha.toInt(), Color.red(color), Color.green(color), Color.blue(color))
        }

        /**
         * Blend `color1` and `color2` using the given ratio.
         *
         * @param color1 First color
         * @param color2 Second color
         * @param ratio of which to blend. 1.0 will return `color1`, 0.5 will give an even blend,
         * 0.0 will return `color2`.
         * @return color which is a ratio of `color1` blended wiht `color2`
         */
        private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRation = 1f - ratio
            val r = Color.red(color1) * ratio + Color.red(color2) * inverseRation
            val g = Color.green(color1) * ratio + Color.green(color2) * inverseRation
            val b = Color.blue(color1) * ratio + Color.blue(color2) * inverseRation
            return Color.rgb(r.toInt(), g.toInt(), b.toInt())
        }
    }
}