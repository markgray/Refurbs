/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused", "HasPlatformType", "DEPRECATION", "UNUSED_CHANGED_VALUE", "PublicApiImplicitType", "PublicApiImplicitType", "KDocMissingDocumentation", "ReplaceJavaStaticMethodWithKotlinAnalog", "JoinDeclarationAndAssignment", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package com.example.android.interactivechart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.widget.OverScroller
import androidx.core.os.ParcelableCompat
import androidx.core.os.ParcelableCompatCreatorCallbacks
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.EdgeEffectCompat

/**
 * A view representing a simple yet interactive line chart for the function `x^3 - x/4`.
 *
 *
 * This view isn't all that useful on its own; rather it serves as an example of how to correctly
 * implement these types of gestures to perform zooming and scrolling with interesting content
 * types.
 *
 *
 * The view is interactive in that it can be zoomed and panned using
 * typical [gestures](http://developer.android.com/design/patterns/gestures.html) such
 * as double-touch, drag, pinch-open, and pinch-close. This is done using the
 * [ScaleGestureDetector], [GestureDetector], and [OverScroller] classes. Note
 * that the platform-provided view scrolling behavior (e.g. [View.scrollBy] is NOT
 * used.
 *
 *
 * The view also demonstrates the correct use of
 * [touch feedback](http://developer.android.com/design/style/touch-feedback.html) to
 * indicate to users that they've reached the content edges after a pan or fling gesture. This
 * is done using the `EdgeEffectCompat` class.
 *
 *
 * Finally, this class demonstrates the basics of creating a custom view, including support for
 * custom attributes (see the constructors), a simple implementation for
 * [.onMeasure], an implementation for [.onSaveInstanceState] and a fairly
 * straightforward [Canvas]-based rendering implementation in
 * [.onDraw].
 *
 *
 * Note that this view doesn't automatically support directional navigation or other accessibility
 * methods. Activities using this view should generally provide alternate navigation controls.
 * Activities using this view should also present an alternate, text-based representation of this
 * view's content for vision-impaired users.
 */
open class InteractiveLineGraphView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {
    /**
     * The current viewport. This rectangle represents the currently visible chart domain
     * and range. The currently visible chart X values are from this rectangle's left to its right.
     * The currently visible chart Y values are from this rectangle's top to its bottom.
     *
     *
     * Note that this rectangle's top is actually the smaller Y value, and its bottom is the larger
     * Y value. Since the chart is drawn onscreen in such a way that chart Y values increase
     * towards the top of the screen (decreasing pixel Y positions), this rectangle's "top" is drawn
     * above this rectangle's "bottom" value.
     *
     * @see .mContentRect
     */
    private var mCurrentViewport: RectF? = RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX)

    /**
     * The current destination rectangle (in pixel coordinates) into which the chart data should
     * be drawn. Chart labels are drawn outside this area.
     *
     * @see .mCurrentViewport
     */
    private val mContentRect = Rect()

    // Current attribute values and Paints.
    private var mLabelTextSize = 0f
    private var mLabelSeparation = 0
    private var mLabelTextColor = 0
    private var mLabelTextPaint: Paint? = null
    private var mMaxLabelWidth = 0
    private var mLabelHeight = 0
    private var mGridThickness = 0f
    private var mGridColor = 0
    private var mGridPaint: Paint? = null
    private var mAxisThickness = 0f
    private var mAxisColor = 0
    private var mAxisPaint: Paint? = null
    var dataThickness = 0f
    var dataColor = 0
    private var mDataPaint: Paint? = null

    // State objects and values related to gesture tracking.
    private val mScaleGestureDetector: ScaleGestureDetector
    private val mGestureDetector: GestureDetectorCompat
    private val mScroller: OverScroller =OverScroller(context)
    private val mZoomer: Zoomer = Zoomer(context)
    private val mZoomFocalPoint = PointF()
    private val mScrollerStartViewport = RectF() // Used only for zooms and flings.

    // Edge effect / overscroll tracking objects.
    private val mEdgeEffectTop: EdgeEffectCompat = EdgeEffectCompat(context)
    private val mEdgeEffectBottom: EdgeEffectCompat = EdgeEffectCompat(context)
    private val mEdgeEffectLeft: EdgeEffectCompat = EdgeEffectCompat(context)
    private val mEdgeEffectRight: EdgeEffectCompat = EdgeEffectCompat(context)
    private var mEdgeEffectTopActive = false
    private var mEdgeEffectBottomActive = false
    private var mEdgeEffectLeftActive = false
    private var mEdgeEffectRightActive = false

    // Buffers for storing current X and Y stops. See the computeAxisStops method for more details.
    private val mXStopsBuffer = AxisStops()
    private val mYStopsBuffer = AxisStops()

    // Buffers used during drawing. These are defined as fields to avoid allocation during
    // draw calls.
    private var mAxisXPositionsBuffer = floatArrayOf()
    private var mAxisYPositionsBuffer = floatArrayOf()
    private var mAxisXLinesBuffer = floatArrayOf()
    private var mAxisYLinesBuffer = floatArrayOf()
    private val mSeriesLinesBuffer = FloatArray((DRAW_STEPS + 1) * 4)
    private val mLabelBuffer = CharArray(100)
    private val mSurfaceSizeBuffer = Point()

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private val mScaleGestureListener: OnScaleGestureListener = object : SimpleOnScaleGestureListener() {
        /**
         * This is the active focal point in terms of the viewport. Could be a local
         * variable but kept here to minimize per-frame allocations.
         */
        private val viewportFocus = PointF()
        private var lastSpanX = 0f
        private var lastSpanY = 0f
        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            lastSpanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector)
            lastSpanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector)
            return true
        }

        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            val spanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector)
            val spanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector)
            val newWidth = lastSpanX / spanX * mCurrentViewport!!.width()
            val newHeight = lastSpanY / spanY * mCurrentViewport!!.height()
            val focusX = scaleGestureDetector.focusX
            val focusY = scaleGestureDetector.focusY
            hitTest(focusX, focusY, viewportFocus)
            mCurrentViewport!![viewportFocus.x
                - newWidth * (focusX - mContentRect.left)
                / mContentRect.width(), viewportFocus.y
                - newHeight * (mContentRect.bottom - focusY)
                / mContentRect.height(), 0f] = 0f
            mCurrentViewport!!.right = mCurrentViewport!!.left + newWidth
            mCurrentViewport!!.bottom = mCurrentViewport!!.top + newHeight
            constrainViewport()
            ViewCompat.postInvalidateOnAnimation(this@InteractiveLineGraphView)
            lastSpanX = spanX
            lastSpanY = spanY
            return true
        }
    }
    /**
     * The gesture listener, used for handling simple gestures such as double touches, scrolls,
     * and flings.
     */
    private val mGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            releaseEdgeEffects()
            mScrollerStartViewport.set(mCurrentViewport!!)
            mScroller.forceFinished(true)
            ViewCompat.postInvalidateOnAnimation(this@InteractiveLineGraphView)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            mZoomer.forceFinished(true)
            if (hitTest(e.x, e.y, mZoomFocalPoint)) {
                mZoomer.startZoom(ZOOM_AMOUNT)
            }
            ViewCompat.postInvalidateOnAnimation(this@InteractiveLineGraphView)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Scrolling uses math based on the viewport (as opposed to math using pixels).
            /**
             * Pixel offset is the offset in screen pixels, while viewport offset is the
             * offset within the current viewport. For additional information on surface sizes
             * and pixel offsets, see the docs for []. For
             * additional information about the viewport, see the comments for
             * [mCurrentViewport].
             */
            val viewportOffsetX = distanceX * mCurrentViewport!!.width() / mContentRect.width()
            val viewportOffsetY = -distanceY * mCurrentViewport!!.height() / mContentRect.height()
            computeScrollSurfaceSize(mSurfaceSizeBuffer)
            val scrolledX = (mSurfaceSizeBuffer.x
                * (mCurrentViewport!!.left + viewportOffsetX - AXIS_X_MIN)
                / (AXIS_X_MAX - AXIS_X_MIN)).toInt()
            val scrolledY = (mSurfaceSizeBuffer.y
                * (AXIS_Y_MAX - mCurrentViewport!!.bottom - viewportOffsetY)
                / (AXIS_Y_MAX - AXIS_Y_MIN)).toInt()
            val canScrollX = (mCurrentViewport!!.left > AXIS_X_MIN
                || mCurrentViewport!!.right < AXIS_X_MAX)
            val canScrollY = (mCurrentViewport!!.top > AXIS_Y_MIN
                || mCurrentViewport!!.bottom < AXIS_Y_MAX)
            setViewportBottomLeft(
                mCurrentViewport!!.left + viewportOffsetX,
                mCurrentViewport!!.bottom + viewportOffsetY)
            if (canScrollX && scrolledX < 0) {
                mEdgeEffectLeft.onPull(scrolledX / mContentRect.width().toFloat())
                mEdgeEffectLeftActive = true
            }
            if (canScrollY && scrolledY < 0) {
                mEdgeEffectTop.onPull(scrolledY / mContentRect.height().toFloat())
                mEdgeEffectTopActive = true
            }
            if (canScrollX && scrolledX > mSurfaceSizeBuffer.x - mContentRect.width()) {
                mEdgeEffectRight.onPull((scrolledX - mSurfaceSizeBuffer.x + mContentRect.width())
                    / mContentRect.width().toFloat())
                mEdgeEffectRightActive = true
            }
            if (canScrollY && scrolledY > mSurfaceSizeBuffer.y - mContentRect.height()) {
                mEdgeEffectBottom.onPull((scrolledY - mSurfaceSizeBuffer.y + mContentRect.height())
                    / mContentRect.height().toFloat())
                mEdgeEffectBottomActive = true
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            fling(-velocityX.toInt(), -velocityY.toInt())
            return true
        }
    }

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs, R.styleable.InteractiveLineGraphView, defStyle, defStyle)
        try {
            mLabelTextColor = a.getColor(
                R.styleable.InteractiveLineGraphView_labelTextColor, mLabelTextColor)
            mLabelTextSize = a.getDimension(
                R.styleable.InteractiveLineGraphView_labelTextSize, mLabelTextSize)
            mLabelSeparation = a.getDimensionPixelSize(
                R.styleable.InteractiveLineGraphView_labelSeparation, mLabelSeparation)
            mGridThickness = a.getDimension(
                R.styleable.InteractiveLineGraphView_gridThickness, mGridThickness)
            mGridColor = a.getColor(
                R.styleable.InteractiveLineGraphView_gridColor, mGridColor)
            mAxisThickness = a.getDimension(
                R.styleable.InteractiveLineGraphView_axisThickness, mAxisThickness)
            mAxisColor = a.getColor(
                R.styleable.InteractiveLineGraphView_axisColor, mAxisColor)
            dataThickness = a.getDimension(
                R.styleable.InteractiveLineGraphView_dataThickness, dataThickness)
            dataColor = a.getColor(
                R.styleable.InteractiveLineGraphView_dataColor, dataColor)
        } finally {
            a.recycle()
        }
        initPaints()

        // Sets up interactions
        mScaleGestureDetector = ScaleGestureDetector(context, mScaleGestureListener)
        mGestureDetector = GestureDetectorCompat(context, mGestureListener)
    }

    /**
     * (Re)initializes [Paint] objects based on current attribute values.
     */
    private fun initPaints() {
        mLabelTextPaint = Paint()
        mLabelTextPaint!!.isAntiAlias = true
        mLabelTextPaint!!.textSize = mLabelTextSize
        mLabelTextPaint!!.color = mLabelTextColor
        mLabelHeight = Math.abs(mLabelTextPaint!!.fontMetrics.top).toInt()
        mMaxLabelWidth = mLabelTextPaint!!.measureText("0000").toInt()
        mGridPaint = Paint()
        mGridPaint!!.strokeWidth = mGridThickness
        mGridPaint!!.color = mGridColor
        mGridPaint!!.style = Paint.Style.STROKE
        mAxisPaint = Paint()
        mAxisPaint!!.strokeWidth = mAxisThickness
        mAxisPaint!!.color = mAxisColor
        mAxisPaint!!.style = Paint.Style.STROKE
        mDataPaint = Paint()
        mDataPaint!!.strokeWidth = dataThickness
        mDataPaint!!.color = dataColor
        mDataPaint!!.style = Paint.Style.STROKE
        mDataPaint!!.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mContentRect[paddingLeft + mMaxLabelWidth + mLabelSeparation, paddingTop, width - paddingRight] = height - paddingBottom - mLabelHeight - mLabelSeparation
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minChartSize = resources.getDimensionPixelSize(R.dimen.min_chart_size)
        setMeasuredDimension(
            Math.max(suggestedMinimumWidth,
                resolveSize(minChartSize + paddingLeft + mMaxLabelWidth
                    + mLabelSeparation + paddingRight,
                    widthMeasureSpec)),
            Math.max(suggestedMinimumHeight,
                resolveSize(minChartSize + paddingTop + mLabelHeight
                    + mLabelSeparation + paddingBottom,
                    heightMeasureSpec)))
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods and objects related to drawing
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draws axes and text labels
        drawAxes(canvas)

        // Clips the next few drawing operations to the content area
        val clipRestoreCount = canvas.save()
        canvas.clipRect(mContentRect)
        drawDataSeriesUnclipped(canvas)
        drawEdgeEffectsUnclipped(canvas)

        // Removes clipping rectangle
        canvas.restoreToCount(clipRestoreCount)

        // Draws chart container
        canvas.drawRect(mContentRect, mAxisPaint!!)
    }

    /**
     * Draws the chart axes and labels onto the canvas.
     */
    private fun drawAxes(canvas: Canvas) {
        // Computes axis stops (in terms of numerical value and position on screen)
        var i: Int
        computeAxisStops(
            mCurrentViewport!!.left,
            mCurrentViewport!!.right,
            mContentRect.width() / mMaxLabelWidth / 2,
            mXStopsBuffer)
        computeAxisStops(
            mCurrentViewport!!.top,
            mCurrentViewport!!.bottom,
            mContentRect.height() / mLabelHeight / 2,
            mYStopsBuffer)

        // Avoid unnecessary allocations during drawing. Re-use allocated
        // arrays and only reallocate if the number of stops grows.
        if (mAxisXPositionsBuffer.size < mXStopsBuffer.numStops) {
            mAxisXPositionsBuffer = FloatArray(mXStopsBuffer.numStops)
        }
        if (mAxisYPositionsBuffer.size < mYStopsBuffer.numStops) {
            mAxisYPositionsBuffer = FloatArray(mYStopsBuffer.numStops)
        }
        if (mAxisXLinesBuffer.size < mXStopsBuffer.numStops * 4) {
            mAxisXLinesBuffer = FloatArray(mXStopsBuffer.numStops * 4)
        }
        if (mAxisYLinesBuffer.size < mYStopsBuffer.numStops * 4) {
            mAxisYLinesBuffer = FloatArray(mYStopsBuffer.numStops * 4)
        }

        // Compute positions
        i = 0
        while (i < mXStopsBuffer.numStops) {
            mAxisXPositionsBuffer[i] = getDrawX(mXStopsBuffer.stops[i])
            i++
        }
        i = 0
        while (i < mYStopsBuffer.numStops) {
            mAxisYPositionsBuffer[i] = getDrawY(mYStopsBuffer.stops[i])
            i++
        }

        // Draws grid lines using drawLines (faster than individual drawLine calls)
        i = 0
        while (i < mXStopsBuffer.numStops) {
            mAxisXLinesBuffer[i * 4 + 0] = Math.floor(mAxisXPositionsBuffer[i].toDouble()).toFloat()
            mAxisXLinesBuffer[i * 4 + 1] = mContentRect.top.toFloat()
            mAxisXLinesBuffer[i * 4 + 2] = Math.floor(mAxisXPositionsBuffer[i].toDouble()).toFloat()
            mAxisXLinesBuffer[i * 4 + 3] = mContentRect.bottom.toFloat()
            i++
        }
        canvas.drawLines(mAxisXLinesBuffer, 0, mXStopsBuffer.numStops * 4, mGridPaint!!)
        i = 0
        while (i < mYStopsBuffer.numStops) {
            mAxisYLinesBuffer[i * 4 + 0] = mContentRect.left.toFloat()
            mAxisYLinesBuffer[i * 4 + 1] = Math.floor(mAxisYPositionsBuffer[i].toDouble()).toFloat()
            mAxisYLinesBuffer[i * 4 + 2] = mContentRect.right.toFloat()
            mAxisYLinesBuffer[i * 4 + 3] = Math.floor(mAxisYPositionsBuffer[i].toDouble()).toFloat()
            i++
        }
        canvas.drawLines(mAxisYLinesBuffer, 0, mYStopsBuffer.numStops * 4, mGridPaint!!)

        // Draws X labels
        var labelOffset: Int
        var labelLength: Int
        mLabelTextPaint!!.textAlign = Paint.Align.CENTER
        i = 0
        while (i < mXStopsBuffer.numStops) {

            // Do not use String.format in high-performance code such as onDraw code.
            labelLength = formatFloat(mLabelBuffer, mXStopsBuffer.stops[i], mXStopsBuffer.decimals)
            labelOffset = mLabelBuffer.size - labelLength
            canvas.drawText(
                mLabelBuffer, labelOffset, labelLength,
                mAxisXPositionsBuffer[i],
                (
                    mContentRect.bottom + mLabelHeight + mLabelSeparation).toFloat(),
                mLabelTextPaint!!)
            i++
        }

        // Draws Y labels
        mLabelTextPaint!!.textAlign = Paint.Align.RIGHT
        i = 0
        while (i < mYStopsBuffer.numStops) {

            // Do not use String.format in high-performance code such as onDraw code.
            labelLength = formatFloat(mLabelBuffer, mYStopsBuffer.stops[i], mYStopsBuffer.decimals)
            labelOffset = mLabelBuffer.size - labelLength
            canvas.drawText(
                mLabelBuffer, labelOffset, labelLength,
                (
                    mContentRect.left - mLabelSeparation).toFloat(),
                mAxisYPositionsBuffer[i] + mLabelHeight / 2,
                mLabelTextPaint!!)
            i++
        }
    }

    /**
     * Computes the pixel offset for the given X chart value. This may be outside the view bounds.
     */
    private fun getDrawX(x: Float): Float {
        return (mContentRect.left
            + mContentRect.width()
            * (x - mCurrentViewport!!.left) / mCurrentViewport!!.width())
    }

    /**
     * Computes the pixel offset for the given Y chart value. This may be outside the view bounds.
     */
    private fun getDrawY(y: Float): Float {
        return (mContentRect.bottom
            - mContentRect.height()
            * (y - mCurrentViewport!!.top) / mCurrentViewport!!.height())
    }

    /**
     * Draws the currently visible portion of the data series defined by [.fun] to the
     * canvas. This method does not clip its drawing, so users should call [ before calling this method.][Canvas.clipRect]
     */
    private fun drawDataSeriesUnclipped(canvas: Canvas) {
        mSeriesLinesBuffer[0] = mContentRect.left.toFloat()
        mSeriesLinesBuffer[1] = getDrawY(`fun`(mCurrentViewport!!.left))
        mSeriesLinesBuffer[2] = mSeriesLinesBuffer[0]
        mSeriesLinesBuffer[3] = mSeriesLinesBuffer[1]
        var x: Float
        for (i in 1..DRAW_STEPS) {
            mSeriesLinesBuffer[i * 4 + 0] = mSeriesLinesBuffer[(i - 1) * 4 + 2]
            mSeriesLinesBuffer[i * 4 + 1] = mSeriesLinesBuffer[(i - 1) * 4 + 3]
            x = mCurrentViewport!!.left + mCurrentViewport!!.width() / DRAW_STEPS * i
            mSeriesLinesBuffer[i * 4 + 2] = getDrawX(x)
            mSeriesLinesBuffer[i * 4 + 3] = getDrawY(`fun`(x))
        }
        canvas.drawLines(mSeriesLinesBuffer, mDataPaint!!)
    }

    /**
     * Draws the overscroll "glow" at the four edges of the chart region, if necessary. The edges
     * of the chart region are stored in [.mContentRect].
     *
     * @see EdgeEffectCompat
     */
    private fun drawEdgeEffectsUnclipped(canvas: Canvas) {
        // The methods below rotate and translate the canvas as needed before drawing the glow,
        // since EdgeEffectCompat always draws a top-glow at 0,0.
        var needsInvalidate = false
        if (!mEdgeEffectTop.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate(mContentRect.left.toFloat(), mContentRect.top.toFloat())
            mEdgeEffectTop.setSize(mContentRect.width(), mContentRect.height())
            if (mEdgeEffectTop.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (!mEdgeEffectBottom.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate((2 * mContentRect.left - mContentRect.right).toFloat(), mContentRect.bottom.toFloat())
            canvas.rotate(180f, mContentRect.width().toFloat(), 0f)
            mEdgeEffectBottom.setSize(mContentRect.width(), mContentRect.height())
            if (mEdgeEffectBottom.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (!mEdgeEffectLeft.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate(mContentRect.left.toFloat(), mContentRect.bottom.toFloat())
            canvas.rotate(-90f, 0f, 0f)
            mEdgeEffectLeft.setSize(mContentRect.height(), mContentRect.width())
            if (mEdgeEffectLeft.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (!mEdgeEffectRight.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate(mContentRect.right.toFloat(), mContentRect.top.toFloat())
            canvas.rotate(90f, 0f, 0f)
            mEdgeEffectRight.setSize(mContentRect.height(), mContentRect.width())
            if (mEdgeEffectRight.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods and objects related to gesture handling
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Finds the chart point (i.e. within the chart's domain and range) represented by the
     * given pixel coordinates, if that pixel is within the chart region described by
     * [.mContentRect]. If the point is found, the "dest" argument is set to the point and
     * this function returns true. Otherwise, this function returns false and "dest" is unchanged.
     */
    private fun hitTest(x: Float, y: Float, dest: PointF): Boolean {
        if (!mContentRect.contains(x.toInt(), y.toInt())) {
            return false
        }
        dest[mCurrentViewport!!.left
            + mCurrentViewport!!.width()
            * (x - mContentRect.left) / mContentRect.width()] = (
            mCurrentViewport!!.top
                + mCurrentViewport!!.height()
                * (y - mContentRect.bottom) / -mContentRect.height())
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retVal = mScaleGestureDetector.onTouchEvent(event)
        retVal = mGestureDetector.onTouchEvent(event) || retVal
        return retVal || super.onTouchEvent(event)
    }

    /**
     * Ensures that current viewport is inside the viewport extremes defined by [.AXIS_X_MIN],
     * [.AXIS_X_MAX], [.AXIS_Y_MIN] and [.AXIS_Y_MAX].
     */
    private fun constrainViewport() {
        mCurrentViewport!!.left = Math.max(AXIS_X_MIN, mCurrentViewport!!.left)
        mCurrentViewport!!.top = Math.max(AXIS_Y_MIN, mCurrentViewport!!.top)
        mCurrentViewport!!.bottom = Math.max(Math.nextUp(mCurrentViewport!!.top),
            Math.min(AXIS_Y_MAX, mCurrentViewport!!.bottom))
        mCurrentViewport!!.right = Math.max(Math.nextUp(mCurrentViewport!!.left),
            Math.min(AXIS_X_MAX, mCurrentViewport!!.right))
    }


    private fun releaseEdgeEffects() {
        mEdgeEffectBottomActive = false
        @Suppress("KotlinConstantConditions")
        mEdgeEffectRightActive = mEdgeEffectBottomActive
        @Suppress("KotlinConstantConditions")
        mEdgeEffectTopActive = mEdgeEffectRightActive
        @Suppress("KotlinConstantConditions")
        mEdgeEffectLeftActive = mEdgeEffectTopActive
        mEdgeEffectLeft.onRelease()
        mEdgeEffectTop.onRelease()
        mEdgeEffectRight.onRelease()
        mEdgeEffectBottom.onRelease()
    }

    private fun fling(velocityX: Int, velocityY: Int) {
        releaseEdgeEffects()
        // Flings use math in pixels (as opposed to math based on the viewport).
        computeScrollSurfaceSize(mSurfaceSizeBuffer)
        mScrollerStartViewport.set(mCurrentViewport!!)
        val startX = (mSurfaceSizeBuffer.x * (mScrollerStartViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN)).toInt()
        val startY = (mSurfaceSizeBuffer.y * (AXIS_Y_MAX - mScrollerStartViewport.bottom) / (AXIS_Y_MAX - AXIS_Y_MIN)).toInt()
        mScroller.forceFinished(true)
        mScroller.fling(
            startX,
            startY,
            velocityX,
            velocityY,
            0, mSurfaceSizeBuffer.x - mContentRect.width(),
            0, mSurfaceSizeBuffer.y - mContentRect.height(),
            mContentRect.width() / 2,
            mContentRect.height() / 2)
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Computes the current scrollable surface size, in pixels. For example, if the entire chart
     * area is visible, this is simply the current size of [.mContentRect]. If the chart
     * is zoomed in 200% in both directions, the returned size will be twice as large horizontally
     * and vertically.
     */
    private fun computeScrollSurfaceSize(out: Point) {
        out[(mContentRect.width() * (AXIS_X_MAX - AXIS_X_MIN)
            / mCurrentViewport!!.width()).toInt()] = (mContentRect.height() * (AXIS_Y_MAX - AXIS_Y_MIN)
            / mCurrentViewport!!.height()).toInt()
    }

    override fun computeScroll() {
        super.computeScroll()
        var needsInvalidate = false
        if (mScroller.computeScrollOffset()) {
            // The scroller isn't finished, meaning a fling or programmatic pan operation is
            // currently active.
            computeScrollSurfaceSize(mSurfaceSizeBuffer)
            val currX = mScroller.currX
            val currY = mScroller.currY
            val canScrollX = (mCurrentViewport!!.left > AXIS_X_MIN
                || mCurrentViewport!!.right < AXIS_X_MAX)
            val canScrollY = (mCurrentViewport!!.top > AXIS_Y_MIN
                || mCurrentViewport!!.bottom < AXIS_Y_MAX)
            if (canScrollX && currX < 0 && mEdgeEffectLeft.isFinished
                && !mEdgeEffectLeftActive) {
                mEdgeEffectLeft.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectLeftActive = true
                needsInvalidate = true
            } else if (canScrollX
                && currX > (mSurfaceSizeBuffer.x - mContentRect.width())
                && mEdgeEffectRight.isFinished && !mEdgeEffectRightActive) {
                mEdgeEffectRight.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectRightActive = true
                needsInvalidate = true
            }
            if (canScrollY && currY < 0 && mEdgeEffectTop.isFinished
                && !mEdgeEffectTopActive) {
                mEdgeEffectTop.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectTopActive = true
                needsInvalidate = true
            } else if (canScrollY && currY > mSurfaceSizeBuffer.y - mContentRect.height() && mEdgeEffectBottom.isFinished
                && !mEdgeEffectBottomActive) {
                mEdgeEffectBottom.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectBottomActive = true
                needsInvalidate = true
            }
            val currXRange = AXIS_X_MIN + (AXIS_X_MAX - AXIS_X_MIN) * currX / mSurfaceSizeBuffer.x
            val currYRange = AXIS_Y_MAX - (AXIS_Y_MAX - AXIS_Y_MIN) * currY / mSurfaceSizeBuffer.y
            setViewportBottomLeft(currXRange, currYRange)
        }
        if (mZoomer.computeZoom()) {
            // Performs the zoom since a zoom is in progress (either programmatically or via
            // double-touch).
            val newWidth = (1f - mZoomer.currZoom) * mScrollerStartViewport.width()
            val newHeight = (1f - mZoomer.currZoom) * mScrollerStartViewport.height()
            val pointWithinViewportX = ((mZoomFocalPoint.x - mScrollerStartViewport.left)
                / mScrollerStartViewport.width())
            val pointWithinViewportY = ((mZoomFocalPoint.y - mScrollerStartViewport.top)
                / mScrollerStartViewport.height())
            mCurrentViewport!![mZoomFocalPoint.x - newWidth * pointWithinViewportX, mZoomFocalPoint.y - newHeight * pointWithinViewportY, mZoomFocalPoint.x + newWidth * (1 - pointWithinViewportX)] = mZoomFocalPoint.y + newHeight * (1 - pointWithinViewportY)
            constrainViewport()
            needsInvalidate = true
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /**
     * Sets the current viewport (defined by [.mCurrentViewport]) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position, and thus
     * the bottom of the [.mCurrentViewport] rectangle. For more details on why top and
     * bottom are flipped, see [.mCurrentViewport].
     */
    private fun setViewportBottomLeft(x: Float, y: Float) {
        /**
         * Constrains within the scroll range. The scroll range is simply the viewport extremes
         * (AXIS_X_MAX, etc.) minus the viewport size. For example, if the extrema were 0 and 10,
         * and the viewport size was 2, the scroll range would be 0 to 8.
         */
        var xLocal = x
        var yLocal = y
        val curWidth = mCurrentViewport!!.width()
        val curHeight = mCurrentViewport!!.height()
        xLocal = Math.max(AXIS_X_MIN, Math.min(xLocal, AXIS_X_MAX - curWidth))
        yLocal = Math.max(AXIS_Y_MIN + curHeight, Math.min(yLocal, AXIS_Y_MAX))
        mCurrentViewport!![xLocal, yLocal - curHeight, xLocal + curWidth] = yLocal
        ViewCompat.postInvalidateOnAnimation(this)
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods for programmatically changing the viewport
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Returns the current viewport (visible extremes for the chart domain and range.)
     */
    /**
     * Sets the chart's current viewport.
     *
     * @see .getCurrentViewport
     */
    var currentViewport: RectF?
        get() = RectF(mCurrentViewport)
        set(viewport) {
            mCurrentViewport = viewport
            constrainViewport()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Smoothly zooms the chart in one step.
     */
    fun zoomIn() {
        mScrollerStartViewport.set(mCurrentViewport!!)
        mZoomer.forceFinished(true)
        mZoomer.startZoom(ZOOM_AMOUNT)
        mZoomFocalPoint[(mCurrentViewport!!.right + mCurrentViewport!!.left) / 2] = (mCurrentViewport!!.bottom + mCurrentViewport!!.top) / 2
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Smoothly zooms the chart out one step.
     */
    fun zoomOut() {
        mScrollerStartViewport.set(mCurrentViewport!!)
        mZoomer.forceFinished(true)
        mZoomer.startZoom(-ZOOM_AMOUNT)
        mZoomFocalPoint[(mCurrentViewport!!.right + mCurrentViewport!!.left) / 2] = (mCurrentViewport!!.bottom + mCurrentViewport!!.top) / 2
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Smoothly pans the chart left one step.
     */
    fun panLeft() {
        fling((-PAN_VELOCITY_FACTOR * width).toInt(), 0)
    }

    /**
     * Smoothly pans the chart right one step.
     */
    fun panRight() {
        fling((PAN_VELOCITY_FACTOR * width).toInt(), 0)
    }

    /**
     * Smoothly pans the chart up one step.
     */
    fun panUp() {
        fling(0, (-PAN_VELOCITY_FACTOR * height).toInt())
    }

    /**
     * Smoothly pans the chart down one step.
     */
    fun panDown() {
        fling(0, (PAN_VELOCITY_FACTOR * height).toInt())
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods related to custom attributes
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    var labelTextSize: Float
        get() = mLabelTextSize
        set(labelTextSize) {
            mLabelTextSize = labelTextSize
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    var labelTextColor: Int
        get() = mLabelTextColor
        set(labelTextColor) {
            mLabelTextColor = labelTextColor
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    var gridThickness: Float
        get() = mGridThickness
        set(gridThickness) {
            mGridThickness = gridThickness
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    var gridColor: Int
        get() = mGridColor
        set(gridColor) {
            mGridColor = gridColor
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    var axisThickness: Float
        get() = mAxisThickness
        set(axisThickness) {
            mAxisThickness = axisThickness
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    var axisColor: Int
        get() = mAxisColor
        set(axisColor) {
            mAxisColor = axisColor
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods and classes related to view state persistence.
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.viewport = mCurrentViewport
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        @Suppress("UnnecessaryVariable")
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        mCurrentViewport = ss.viewport
    }

    /**
     * Persistent state that is saved by InteractiveLineGraphView.
     */
    class SavedState : BaseSavedState {
        var viewport: RectF? = null

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(viewport!!.left)
            out.writeFloat(viewport!!.top)
            out.writeFloat(viewport!!.right)
            out.writeFloat(viewport!!.bottom)
        }

        override fun toString(): String {
            return ("InteractiveLineGraphView.SavedState{"
                + Integer.toHexString(System.identityHashCode(this))
                + " viewport=" + viewport.toString() + "}")
        }

        internal constructor(`in`: Parcel) : super(`in`) {
            viewport = RectF(`in`.readFloat(), `in`.readFloat(), `in`.readFloat(), `in`.readFloat())
        }

        companion object {
            /**
             * TODO: Add kdoc
             */
            @Suppress("RedundantNullableReturnType")
            @JvmField
            val CREATOR = ParcelableCompat.newCreator(object : ParcelableCompatCreatorCallbacks<SavedState?> {
                override fun createFromParcel(`in`: Parcel, loader: ClassLoader): SavedState? {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            })
        }
    }

    /**
     * A simple class representing axis label values.
     *
     * @see .computeAxisStops
     */
    private class AxisStops {
        var stops = floatArrayOf()
        var numStops = 0
        var decimals = 0
    }

    companion object {
        private const val TAG = "InteractiveLineGraphView"

        /**
         * The number of individual points (samples) in the chart series to draw onscreen.
         */
        private const val DRAW_STEPS = 30

        /**
         * Initial fling velocity for pan operations, in screen widths (or heights) per second.
         *
         * @see .panLeft
         * @see .panRight
         * @see .panUp
         * @see .panDown
         */
        private const val PAN_VELOCITY_FACTOR = 2f

        /**
         * The scaling factor for a single zoom 'step'.
         *
         * @see .zoomIn
         * @see .zoomOut
         */
        private const val ZOOM_AMOUNT = 0.25f

        // Viewport extremes. See mCurrentViewport for a discussion of the viewport.
        private const val AXIS_X_MIN = -1f
        private const val AXIS_X_MAX = 1f
        private const val AXIS_Y_MIN = -1f
        private const val AXIS_Y_MAX = 1f

        /**
         * The simple math function Y = fun(X) to draw on the chart.
         * @param x The X value
         * @return The Y value
         */
        private fun `fun`(x: Float): Float {
            return Math.pow(x.toDouble(), 3.0).toFloat() - x / 4
        }

        /**
         * Rounds the given number to the given number of significant digits. Based on an answer on
         * [Stack Overflow](http://stackoverflow.com/questions/202302).
         */
        private fun roundToOneSignificantFigure(num: Double): Float {
            val d = Math.ceil(Math.log10(if (num < 0) -num else num).toFloat().toDouble()).toFloat()
            val power = 1 - d.toInt()
            val magnitude = Math.pow(10.0, power.toDouble()).toFloat()
            val shifted = Math.round(num * magnitude)
            return shifted / magnitude
        }

        private val POW10 = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000)

        /**
         * Formats a float value to the given number of decimals. Returns the length of the string.
         * The string begins at out.length - [return value].
         */
        private fun formatFloat(out: CharArray, value: Float, digits: Int): Int {
            var valueLocal = `value`
            var digitsLocal = digits
            var negative = false
            if (valueLocal == 0f) {
                out[out.size - 1] = '0'
                return 1
            }
            if (valueLocal < 0) {
                negative = true
                valueLocal = -valueLocal
            }
            if (digitsLocal > POW10.size) {
                digitsLocal = POW10.size - 1
            }
            valueLocal *= POW10[digitsLocal].toFloat()
            var lval = Math.round(valueLocal).toLong()
            var index = out.size - 1
            var charCount = 0
            while (lval != 0L || charCount < digitsLocal + 1) {
                val digit = (lval % 10).toInt()
                lval /= 10
                out[index--] = (digit + '0'.code).toChar()
                charCount++
                if (charCount == digitsLocal) {
                    out[index--] = '.'
                    charCount++
                }
            }
            if (negative) {
                out[index--] = '-'
                charCount++
            }
            return charCount
        }

        /**
         * Computes the set of axis labels to show given start and stop boundaries and an ideal number
         * of stops between these boundaries.
         *
         * @param start The minimum extreme (e.g. the left edge) for the axis.
         * @param stop The maximum extreme (e.g. the right edge) for the axis.
         * @param steps The ideal number of stops to create. This should be based on available screen
         * space; the more space there is, the more stops should be shown.
         * @param outStops The destination [AxisStops] object to populate.
         */
        private fun computeAxisStops(start: Float, stop: Float, steps: Int, outStops: AxisStops) {
            val range = (stop - start).toDouble()
            if (steps == 0 || range <= 0) {
                outStops.stops = floatArrayOf()
                outStops.numStops = 0
                return
            }
            val rawInterval = range / steps
            var interval = roundToOneSignificantFigure(rawInterval).toDouble()
            val intervalMagnitude = Math.pow(10.0, Math.log10(interval).toInt().toDouble())
            val intervalSigDigit = (interval / intervalMagnitude).toInt()
            if (intervalSigDigit > 5) {
                // Use one order of magnitude higher, to avoid intervals like 0.9 or 90
                interval = Math.floor(10 * intervalMagnitude)
            }
            val first = Math.ceil(start / interval) * interval
            val last = Math.nextUp(Math.floor(stop / interval) * interval)
            var f: Double
            var i: Int
            var n = 0
            f = first
            while (f <= last) {
                ++n
                f += interval
            }
            outStops.numStops = n
            if (outStops.stops.size < n) {
                // Ensure stops contains at least numStops elements.
                outStops.stops = FloatArray(n)
            }
            f = first
            i = 0
            while (i < n) {
                outStops.stops[i] = f.toFloat()
                f += interval
                ++i
            }
            if (interval < 1) {
                outStops.decimals = Math.ceil(-Math.log10(interval)).toInt()
            } else {
                outStops.decimals = 0
            }
        }
    }
}