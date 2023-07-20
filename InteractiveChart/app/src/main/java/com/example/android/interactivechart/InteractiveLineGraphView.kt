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
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
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
 * This view isn't all that useful on its own; rather it serves as an example of how to correctly
 * implement these types of gestures to perform zooming and scrolling with interesting content
 * types.
 *
 * The view is interactive in that it can be zoomed and panned using typical
 * [gestures](http://developer.android.com/design/patterns/gestures.html) such
 * as double-touch, drag, pinch-open, and pinch-close. This is done using the
 * [ScaleGestureDetector], [GestureDetector], and [OverScroller] classes. Note that
 * the platform-provided view scrolling behavior (e.g. [View.scrollBy] is NOT used.
 *
 * The view also demonstrates the correct use of
 * [touch feedback](http://developer.android.com/design/style/touch-feedback.html) to indicate to
 * users that they've reached the content edges after a pan or fling gesture. This is done using
 * the [EdgeEffectCompat] class.
 *
 * Finally, this class demonstrates the basics of creating a custom view, including support for
 * custom attributes (see the constructors), a simple implementation for [onMeasure], an
 * implementation for [onSaveInstanceState] and a fairly straightforward [Canvas]-based rendering
 * implementation in [onDraw].
 *
 * Note that this view doesn't automatically support directional navigation or other accessibility
 * methods. Activities using this view should generally provide alternate navigation controls.
 * Activities using this view should also present an alternate, text-based representation of this
 * view's content for vision-impaired users.
 */
open class InteractiveLineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    /**
     * The current viewport. This rectangle represents the currently visible chart domain and range.
     * The currently visible chart X values are from this rectangle's left to its right.
     * The currently visible chart Y values are from this rectangle's top to its bottom.
     *
     * Note that this rectangle's top is actually the smaller Y value, and its bottom is the larger
     * Y value. Since the chart is drawn onscreen in such a way that chart Y values increase
     * towards the top of the screen (decreasing pixel Y positions), this rectangle's "top" is drawn
     * above this rectangle's "bottom" value.
     *
     * @see [mContentRect]
     */
    private var mCurrentViewport: RectF? = RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX)

    /**
     * The current destination rectangle (in pixel coordinates) into which the chart data should
     * be drawn. Chart labels are drawn outside this area.
     *
     * @see [mCurrentViewport]
     */
    private val mContentRect = Rect()

    /////////////////////////////////////////
    // Current attribute values and Paints.//
    /////////////////////////////////////////

    /**
     * The size of the text drawn by [Paint] field [mLabelTextPaint], it is used in the call to
     * its [Paint.setTextSize] method (kotlin `textSize` property) in our [initPaints] method.
     * It is set by the attribute [R.styleable.InteractiveLineGraphView_labelTextSize] of
     * [R.styleable.InteractiveLineGraphView] to app:labelTextSize="14sp" in the layout file
     * `activity_main.xml`
     */
    private var mLabelTextSize = 0f

    /**
     * The separation between axis labels, it is used by the [drawAxes] method. It is set by the
     * attribute [R.styleable.InteractiveLineGraphView_labelSeparation] of
     * [R.styleable.InteractiveLineGraphView] to app:labelSeparation="10dp" in the layout file
     * `activity_main.xml`
     */
    private var mLabelSeparation = 0

    /**
     * The color of the text drawn by [Paint] field [mLabelTextPaint], it is used in the call to
     * its [Paint.setColor] method (kotlin `color` property) in our [initPaints] method. It is set
     * by the attribute [R.styleable.InteractiveLineGraphView_labelTextColor] of
     * [R.styleable.InteractiveLineGraphView] to app:labelTextColor="#d000" in the layout file
     * `activity_main.xml`
     */
    private var mLabelTextColor = 0

    /**
     * The [Paint] used by our [drawAxes] method to draw text. It is constructed and configured in
     * our [initPaints] method.
     */
    private var mLabelTextPaint: Paint? = null

    /**
     * Maximum length of a label, used when an estimated size for is needed, it is set in our
     * [initPaints] method to the value that the [Paint.measureText] method of [mLabelTextPaint]
     * returns for the text "0000" converted to an [Int].
     */
    private var mMaxLabelWidth = 0

    /**
     * Absolute value of the value returned for the [FontMetrics.top] field of the [FontMetrics]
     * returned by the [Paint.getFontMetrics] method (kotlin `fontMetrics` property) of [Paint]
     * field [mLabelTextPaint] converted to [Int]. It is used as the height of the text drawn by
     * [mLabelTextPaint] when size estimates are needed.
     */
    private var mLabelHeight = 0

    /**
     * This is used in a call to the [Paint.setStrokeWidth] method of [Paint] field [mGridPaint]
     * (aka kotlin `strokeWidth` property) to set the width for stroking. It is set by the attribute
     * [R.styleable.InteractiveLineGraphView_gridThickness] of [R.styleable.InteractiveLineGraphView]
     * to app:gridThickness="1dp" in the layout file `activity_main.xml`
     */
    private var mGridThickness = 0f

    /**
     * This is used in a call to the [Paint.setColor] method of [Paint] field [mGridPaint]
     * (aka kotlin `color` property) to set the paint's color.  It is set by the attribute
     * [R.styleable.InteractiveLineGraphView_gridColor] of [R.styleable.InteractiveLineGraphView]
     * to app:gridColor="#2000" in the layout file `activity_main.xml`
     */
    private var mGridColor = 0

    /**
     * Used as the [Paint] in the calls to [Canvas.drawLines] that draw the horozontal and vertical
     * grid lines of the graph.
     */
    private var mGridPaint: Paint? = null

    /**
     * This is used in a call to the [Paint.setStrokeWidth] method of [Paint] field [mAxisPaint]
     * (aka kotlin `strokeWidth` property) to set the width for stroking. It is set by the attribute
     * [R.styleable.InteractiveLineGraphView_axisThickness] of [R.styleable.InteractiveLineGraphView]
     * to app:axisThickness="2dp" in the layout file `activity_main.xml`
     */
    private var mAxisThickness = 0f

    /**
     * This is used in a call to the [Paint.setColor] method of [Paint] field [mAxisPaint]
     * (aka kotlin `color` property) to set the paint's color.  It is set by the attribute
     * [R.styleable.InteractiveLineGraphView_axisColor] of [R.styleable.InteractiveLineGraphView]
     * to app:axisColor="#d000" in the layout file `activity_main.xml`
     */
    private var mAxisColor = 0

    /**
     * This is the [Paint] used in the call to [Canvas.drawRect] that draws the chart container,
     * [Rect] field [mContentRect]
     */
    private var mAxisPaint: Paint? = null

    /**
     * This is used in a call to the [Paint.setStrokeWidth] method of [Paint] field [mDataPaint]
     * (aka kotlin `strokeWidth` property) to set the width for stroking. It is set by the attribute
     * [R.styleable.InteractiveLineGraphView_dataThickness] of [R.styleable.InteractiveLineGraphView]
     * to app:dataThickness="8dp" in the layout file `activity_main.xml`
     */
    var dataThickness = 0f

    /**
     * This is used in a call to the [Paint.setColor] method of [Paint] field [mDataPaint]
     * (aka kotlin `color` property) to set the paint's color.  It is set by the attribute
     * [R.styleable.InteractiveLineGraphView_dataColor] of [R.styleable.InteractiveLineGraphView]
     * to app:dataColor="#a6c" in the layout file `activity_main.xml`
     */
    var dataColor = 0

    /**
     * Used as the [Paint] in the call to [Canvas.drawLines] that draws the data on the graph.
     */
    private var mDataPaint: Paint? = null

    //////////////////////////////////////////////////////////
    // State objects and values related to gesture tracking.//
    //////////////////////////////////////////////////////////

    /**
     * Detects scaling transformation gestures using the supplied [MotionEvent]'s. Its
     * [ScaleGestureDetector.OnScaleGestureListener] callback will notify users when a
     * particular gesture event has occurred.
     */
    private val mScaleGestureDetector: ScaleGestureDetector

    /**
     * Detects various gestures and events using the supplied [MotionEvent]'s. Its
     * [GestureDetector.OnGestureListener] callback will notify users when a particular
     * motion event has occurred.
     */
    private val mGestureDetector: GestureDetectorCompat

    /**
     * Creates an [OverScroller] with a viscous fluid scroll interpolator and flywheel. This class
     * encapsulates scrolling with the ability to overshoot the bounds of a scrolling operation.
     * This class is a drop-in replacement for `Scroller` in most cases.
     */
    private val mScroller: OverScroller = OverScroller(context)

    /**
     * Animates double-touch zoom gestures.
     */
    private val mZoomer: Zoomer = Zoomer(context)

    /**
     * The point in the viewport which is the center of the zoom being performed
     */
    private val mZoomFocalPoint: PointF = PointF()

    /**
     * Used to hold the current value of [RectF] field [mCurrentViewport] during zooms and flings.
     */
    private val mScrollerStartViewport = RectF() // Used only for zooms and flings.

    ///////////////////////////////////////////////
    // Edge effect / overscroll tracking objects.//
    ///////////////////////////////////////////////

    /**
     * Used to produce the overscroll "glow" at the Top of the scrollable widget when the user
     * scrolls beyond the content bounds in 2D space.
     */
    private val mEdgeEffectTop: EdgeEffectCompat = EdgeEffectCompat(context)

    /**
     * Used to produce the overscroll "glow" at the Bottom of the scrollable widget when the user
     * scrolls beyond the content bounds in 2D space.
     */
    private val mEdgeEffectBottom: EdgeEffectCompat = EdgeEffectCompat(context)

    /**
     * Used to produce the overscroll "glow" at the Left of the scrollable widget when the user
     * scrolls beyond the content bounds in 2D space.
     */
    private val mEdgeEffectLeft: EdgeEffectCompat = EdgeEffectCompat(context)

    /**
     * Used to produce the overscroll "glow" at the Right of the scrollable widget when the user
     * scrolls beyond the content bounds in 2D space.
     */
    private val mEdgeEffectRight: EdgeEffectCompat = EdgeEffectCompat(context)

    /**
     * Flag used to inidicate that the [EdgeEffectCompat] field [mEdgeEffectTop] is currently running.
     */
    private var mEdgeEffectTopActive = false

    /**
     * Flag used to inidicate that the [EdgeEffectCompat] field [mEdgeEffectBottom] is currently running.
     */
    private var mEdgeEffectBottomActive = false

    /**
     * Flag used to inidicate that the [EdgeEffectCompat] field [mEdgeEffectLeft] is currently running.
     */
    private var mEdgeEffectLeftActive = false

    /**
     * Flag used to inidicate that the [EdgeEffectCompat] field [mEdgeEffectRight] is currently running.
     */
    private var mEdgeEffectRightActive = false

    /////////////////////////////////////////////////////////////////////////////////////////////////
    // Buffers for storing current X and Y stops. See the computeAxisStops method for more details.//
    /////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Holds the X axis label values.
     */
    private val mXStopsBuffer = AxisStops()

    /**
     * Holds the Y axis label values.
     */
    private val mYStopsBuffer = AxisStops()

    /////////////////////////////////////////////////////
    // Buffers used during drawing. These are defined  //
    // as fields to avoid allocation during draw calls.//
    /////////////////////////////////////////////////////

    /**
     * This buffer holds all of the X coordinates of the vertical grid lines.
     */
    private var mAxisXPositionsBuffer = floatArrayOf()

    /**
     * This buffer holds all of the Y coordinates of the horizontal grid lines.
     */
    private var mAxisYPositionsBuffer = floatArrayOf()

    /**
     * This buffer holds the (x,y) end points of all of the X axis verical grid lines (4 values per
     * line), it is used by our [drawAxes] method in its call to [Canvas.drawLines] to draw all the
     * lines at once.
     */
    private var mAxisXLinesBuffer = floatArrayOf()

    /**
     * This buffer holds the (x,y) end points of all of the Y axis horizontal grid lines (4 values
     * per line), it is used by our [drawAxes] method in its call to [Canvas.drawLines] to draw all
     * the lines at once.
     */
    private var mAxisYLinesBuffer = floatArrayOf()

    /**
     * This buffer holds the (x,y) end points of all of the lines that are used to draw the curve
     * produced by our [fofX] method, it is used by our [drawDataSeriesUnclipped] method in its call
     * to [Canvas.drawLines] to draw all the lines at once.
     */
    private val mSeriesLinesBuffer = FloatArray((DRAW_STEPS + 1) * 4)

    /**
     * Holds the text that is used to hold that labels for the values of points along both axis.
     */
    private val mLabelBuffer = CharArray(100)

    /**
     * Holds the current scrollable surface size, in pixels computed by [computeScrollSurfaceSize].
     * If the entire chart area is visible, this is simply the current size of [mContentRect]. If
     * the chart is zoomed in 200% in both directions, the returned size will be twice as large
     * horizontally and vertically.
     */
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

        /**
         * The average X distance in pixels between each of the pointers forming the gesture in
         * progress through the focal point.
         */
        private var lastSpanX: Float = 0f

        /**
         * The average Y distance in pixels between each of the pointers forming the gesture in
         * progress through the focal point.
         */
        private var lastSpanY: Float = 0f

        /**
         * Responds to the beginning of a scaling gesture. Reported by new pointers going down.
         * We set our [Float] variable [lastSpanX] to the value of the average X distance in pixels
         * between each of the pointers forming the gesture in progress through the focal point, and
         * our [Float] variable [lastSpanY] to the value of the average Y distance in pixels between
         * each of the pointers forming the gesture in progress through the focal point
         *
         * @param scaleGestureDetector The detector reporting the event - use this to retrieve
         * extended info about event state.
         * @return Whether or not the detector should continue recognizing this gesture. For
         * example, if a gesture is beginning with a focal point outside of a region where it
         * makes sense, onScaleBegin() may return `false` to ignore the rest of the gesture. We
         * always return `true`.
         */
        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            lastSpanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector)
            lastSpanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector)
            return true
        }

        /**
         * Responds to scaling events for a gesture in progress. Reported by pointer motion. We
         * initialize our [Float] variable `val spanX` to the average X distance between each of
         * the pointers forming the gesture in progress through the focal point that our
         * [ScaleGestureDetectorCompat.getCurrentSpanX] method returns when passed our
         * [ScaleGestureDetector] parameter [scaleGestureDetector], and initialize our [Float]
         * variable `val spanY` to the average Y distance between each of the pointers forming the
         * gesture in progress through the focal point that our [ScaleGestureDetectorCompat.getCurrentSpanX]
         * method returns when passed our [ScaleGestureDetector] parameter [scaleGestureDetector].
         * We initialize our [Float] variable `val newWidth` to our [Float] field [lastSpanX] divided
         * by `spanX` times the `width` of our [RectF] field [mCurrentViewport], and we initialize
         * our [Float] variable `val newHeight` to our [Float] field [lastSpanY] divided
         * by `spanY` times the `height` of our [RectF] field [mCurrentViewport]. We initialize our
         * [Float] variable `val focusX` to the X coordinate of the current gesture's focal point,
         * and our [Float] variable `val focusY` to the Y coordinate of the current gesture's focal
         * point. We then call our [hitTest] method to check if the point (`focusX`,`focusY`) is
         * contained in [mContentRect] and if it is it will set [PointF] field [viewportFocus] to
         * the point. We then update the value of [RectF] field [mCurrentViewport] based on the
         * resulting scaling requested by the gesture, and call our [constrainViewport] method to
         * have it ensure that the viewport size requested for [mCurrentViewport] is inside the
         * viewport extremes defined by [AXIS_X_MIN], [AXIS_X_MAX], [AXIS_Y_MIN] and [AXIS_Y_MAX].
         * We then call the [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate
         * of `this` [View] to happen on the next animation time step, typically the next display
         * frame. We then set our [lastSpanX] field to `spanX` and our [lastSpanY] field to `spanY`
         * and return `true` so that the detector will consider this event as handled.
         *
         * @param scaleGestureDetector The detector reporting the event - use this to retrieve
         * extended info about event state.
         * @return Whether or not the detector should consider this event as handled. If an event
         * was not handled, the detector will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example, only wants to update scaling
         * factors if the change is greater than 0.01. We always return `true`
         */
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            val spanX: Float = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector)
            val spanY: Float = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector)
            val newWidth: Float = lastSpanX / spanX * mCurrentViewport!!.width()
            val newHeight: Float = lastSpanY / spanY * mCurrentViewport!!.height()
            val focusX: Float = scaleGestureDetector.focusX
            val focusY: Float = scaleGestureDetector.focusY
            hitTest(focusX, focusY, viewportFocus)
            mCurrentViewport!!.set(
                viewportFocus.x - newWidth * (focusX - mContentRect.left) / mContentRect.width(),
                viewportFocus.y - newHeight * (mContentRect.bottom - focusY) / mContentRect.height(),
                0f,
                0f
            )
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
        /**
         * Notified when a tap occurs with the down [MotionEvent] that triggered it. This will be
         * triggered immediately for every down event. All other events should be preceded by this.
         * First we call our [releaseEdgeEffects] method to have it terminate all [EdgeEffectCompat]
         * that may be in progress. Next we set our [RectF] field [mScrollerStartViewport] to the
         * current value of [mCurrentViewport], and call the [OverScroller.forceFinished] method
         * method of our [OverScroller] field [mScroller] to force its finished field to `true`.
         * We then call the [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate
         * of `this` [View] to happen on the next animation time step, typically the next display
         * frame, and finally we return `true` to make sure we get matching events for this down
         * event.
         *
         * @param e The down motion event.
         * @return Must return `true` to get matching events for this down event.
         */
        override fun onDown(e: MotionEvent): Boolean {
            releaseEdgeEffects()
            mScrollerStartViewport.set(mCurrentViewport!!)
            mScroller.forceFinished(true)
            ViewCompat.postInvalidateOnAnimation(this@InteractiveLineGraphView)
            return true
        }

        /**
         * Notified when a double-tap occurs. Triggered on the down event of second tap. We call the
         * [Zoomer.forceFinished] method of [Zoomer] field [mZoomer] with `true` to force the zoom
         * finished state to `true`, then is our [hitTest] method returns `true` indicating that the
         * (`x`,`y`) of [MotionEvent] parameter [e] is inside [Rect] field [mContentRect] we call
         * the [Zoomer.startZoom] method of [mZoomer] with its `endZoom` argument [ZOOM_AMOUNT]
         * (0.25f) (note that the call to [hitTest] sets [PointF] field [mZoomFocalPoint] to the
         * appropriate value). We then call the [ViewCompat.postInvalidateOnAnimation] method to
         * cause an invalidate of `this` [View] to happen on the next animation time step, typically
         * the next display frame, and finally we return `true` to consume the event.
         *
         * @param e The down motion event of the first tap of the double-tap.
         * @return `true` if the event is consumed, else `false`, we always return `true`
         */
        override fun onDoubleTap(e: MotionEvent): Boolean {
            mZoomer.forceFinished(finished = true)
            if (hitTest(x = e.x, y = e.y, dest = mZoomFocalPoint)) {
                mZoomer.startZoom(endZoom = ZOOM_AMOUNT)
            }
            ViewCompat.postInvalidateOnAnimation(this@InteractiveLineGraphView)
            return true
        }

        /**
         * Notified when a scroll occurs with the initial on down [MotionEvent] and the current move
         * [MotionEvent]. The distance in x and y is also supplied for convenience. We initialize
         * our [Float] variable `val viewportOffsetX` to our [Float] parameter [distanceX] times the
         * `width` of our [RectF] field [mCurrentViewport] divided by the `width` of our [Rect] field
         * [mContentRect], and our [Float] variable `val viewportOffsetY` to minus our [Float]
         * parameter [distanceY] times the `height` of our [RectF] field [mCurrentViewport] divided
         * by the `height` of our [Rect] field [mContentRect]. We then call our [computeScrollSurfaceSize]
         * method to have it compute the current scrollable surface size, in pixels and store the
         * value in our [Point] field [mSurfaceSizeBuffer]. We then initialize our [Int] variable
         * `val scrolledX` to the [Point.x] value of [mSurfaceSizeBuffer] times the quantity of the
         * [RectF.left] value of [mCurrentViewport] plus `viewportOffsetX` minus [AXIS_X_MIN] (-1f)
         * divided by the quantity [AXIS_X_MAX] (1f) minus [AXIS_X_MIN] with the result converted to
         * an [Int]. We initialize our [Int] variable `val scrolledY` to the [Point.y] value of
         * [mSurfaceSizeBuffer] times the quantity of [AXIS_Y_MAX] minus the [RectF.bottom] value of
         * [mCurrentViewport] minus `viewportOffsetY` divided by the quantity [AXIS_Y_MAX] (1f) minus
         * [AXIS_Y_MIN (-1f) with the result converted to an [Int]. We initialize [Boolean] variable
         * `val canScrollX` to `true` if the [RectF.left] value of [mCurrentViewport] is greater
         * than [AXIS_X_MIN] or the [RectF.right] value of [mCurrentViewport] is less than [AXIS_X_MAX].
         * We initialize [Boolean] variable `val canScrollY` to `true` if the [RectF.top] value of
         * [mCurrentViewport] is greater than [AXIS_Y_MIN] or the [RectF.bottom] value of
         * [mCurrentViewport] is less than [AXIS_Y_MAX]. We call our method [setViewportBottomLeft]
         * to set the current viewport's bottom left point to the `x` coordinate formed by adding
         * `viewportOffsetX` to the [RectF.left] of [mCurrentViewport] and `y` coordinate formed by
         * adding `viewportOffsetY` to the [RectF.bottom] of [mCurrentViewport]. If `canScrollX` is
         * `true` and `scrolledX` is less than 0 we call the [EdgeEffectCompat.onPull] method of
         * [mEdgeEffectLeft] with the `deltaDistance` (Change in distance since the last call) the
         * [Float] value of `scrolledX` divided by the [Rect.width] of [mContentRect] and set our
         * [Boolean] field [mEdgeEffectLeftActive] to `true` to indicate that a "left edge" overscroll
         * animation is in progress. If `canScrollY` is `true` and `scrolledY` is less than 0 we call
         * the [EdgeEffectCompat.onPull] method of [mEdgeEffectTop] with the `deltaDistance` (Change
         * in distance since the last call) the [Float] value of `scrolledY` divided by the [Rect.height]
         * of [mContentRect] and set our [Boolean] field [mEdgeEffectTopActive] to `true` to indicate
         * that a "top edge" overscroll animation is in progress. If `canScrollX` is `true` and
         * `scrolledX` is greater than the [Point.x] value of [Point] field [mSurfaceSizeBuffer]
         * minus the [Rect.width] of [Rect] field [mContentRect] we call the [EdgeEffectCompat.onPull]
         * method of [mEdgeEffectRight] with the `deltaDistance` (Change in distance since the last
         * call) the [Float] value of `scrolledX` minus the [Point.x] value of [Point] field
         * [mSurfaceSizeBuffer] plus the [Rect.width] of [Rect] field [mContentRect] divided by the
         * [Rect.width] value of [mContentRect] and set our [Boolean] field [mEdgeEffectRightActive]
         * to `true` to indicate that a "right edge" overscroll animation is in progress. If
         * `canScrollY` is `true` and `scrolledY` is greater than the [Point.y] value of [Point]
         * field [mSurfaceSizeBuffer] minus the [Rect.height] value of [Rect] field [mContentRect]
         * we call the [EdgeEffectCompat.onPull] method of [mEdgeEffectBottom] with the `deltaDistance`
         * (Change in distance since the last call) the [Float] value of `scrolledY` minus the
         * [Point.y] value of [Point] field [mSurfaceSizeBuffer] plus the [Rect.height] value of
         * [Rect] field [mContentRect] divided by the [Rect.height] of [mContentRect] and set our
         * [Boolean] field [mEdgeEffectBottomActive] to `true` to indicate that a "bottom edge"
         * overscroll animation is in progress. Finally we return `true` to indicate that we have
         * consumed the event.
         *
         * @param e1 The first down [MotionEvent] that started the scrolling.
         * @param e2 The move [MotionEvent] that triggered the current [onScroll].
         * @param distanceX The distance along the X axis that has been scrolled since the last call
         * to onScroll. This is NOT the distance between [e1] and [e2].
         * @param distanceY The distance along the Y axis that has been scrolled since the last call
         * to onScroll. This is NOT the distance between [e1] and [e2].
         * @return `true` if the event is consumed, else `false`, we always return `true`
         */
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Scrolling uses math based on the viewport (as opposed to math using pixels).
            /**
             * Pixel offset is the offset in screen pixels, while viewport offset is the
             * offset within the current viewport. For additional information on surface sizes
             * and pixel offsets, see the docs for [computeScrollSurfaceSize]. For
             * additional information about the viewport, see the comments for
             * [mCurrentViewport].
             */
            val viewportOffsetX: Float = distanceX * mCurrentViewport!!.width() / mContentRect.width()
            val viewportOffsetY: Float = -distanceY * mCurrentViewport!!.height() / mContentRect.height()
            computeScrollSurfaceSize(out = mSurfaceSizeBuffer)
            val scrolledX: Int = (mSurfaceSizeBuffer.x
                * (mCurrentViewport!!.left + viewportOffsetX - AXIS_X_MIN)
                / (AXIS_X_MAX - AXIS_X_MIN)).toInt()
            val scrolledY: Int = (mSurfaceSizeBuffer.y
                * (AXIS_Y_MAX - mCurrentViewport!!.bottom - viewportOffsetY)
                / (AXIS_Y_MAX - AXIS_Y_MIN)).toInt()
            val canScrollX: Boolean = (mCurrentViewport!!.left > AXIS_X_MIN
                || mCurrentViewport!!.right < AXIS_X_MAX)
            val canScrollY: Boolean = (mCurrentViewport!!.top > AXIS_Y_MIN
                || mCurrentViewport!!.bottom < AXIS_Y_MAX)
            setViewportBottomLeft(
                x = mCurrentViewport!!.left + viewportOffsetX,
                y = mCurrentViewport!!.bottom + viewportOffsetY
            )
            if (canScrollX && scrolledX < 0) {
                mEdgeEffectLeft.onPull(scrolledX / mContentRect.width().toFloat())
                mEdgeEffectLeftActive = true
            }
            if (canScrollY && scrolledY < 0) {
                mEdgeEffectTop.onPull(scrolledY / mContentRect.height().toFloat())
                mEdgeEffectTopActive = true
            }
            if (canScrollX && scrolledX > mSurfaceSizeBuffer.x - mContentRect.width()) {
                mEdgeEffectRight.onPull(
                    (scrolledX - mSurfaceSizeBuffer.x + mContentRect.width())
                    / mContentRect.width().toFloat()
                )
                mEdgeEffectRightActive = true
            }
            if (canScrollY && scrolledY > mSurfaceSizeBuffer.y - mContentRect.height()) {
                mEdgeEffectBottom.onPull(
                    (scrolledY - mSurfaceSizeBuffer.y + mContentRect.height())
                    / mContentRect.height().toFloat()
                )
                mEdgeEffectBottomActive = true
            }
            return true
        }

        /**
         * Called when a fling event occurs with the initial down [MotionEvent] and the matching up
         * [MotionEvent]. The calculated velocity is supplied along the x and y axis in pixels per
         * second. We just call our [fling] method with minus the [Int] value of our [Float]
         * parameter [velocityX], and minus the [Int] value of our [Float] parameter [velocityY].
         *
         * @param e1 The first down motion event that started the fling.
         * @param e2 The move motion event that triggered the current [onFling].
         * @param velocityX The velocity of this fling measured in pixels per second
         * along the x axis.
         * @param velocityY The velocity of this fling measured in pixels per second
         * along the y axis.
         * @return `true` if the event is consumed, else `false`, we always return `true`
         */
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            fling(-velocityX.toInt(), -velocityY.toInt())
            return true
        }
    }

    init {
        val a: TypedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.InteractiveLineGraphView,
            defStyle,
            defStyle
        )
        try {
            mLabelTextColor = a.getColor(
                R.styleable.InteractiveLineGraphView_labelTextColor,
                mLabelTextColor
            )
            mLabelTextSize = a.getDimension(
                R.styleable.InteractiveLineGraphView_labelTextSize,
                mLabelTextSize
            )
            mLabelSeparation = a.getDimensionPixelSize(
                R.styleable.InteractiveLineGraphView_labelSeparation,
                mLabelSeparation
            )
            mGridThickness = a.getDimension(
                R.styleable.InteractiveLineGraphView_gridThickness,
                mGridThickness
            )
            mGridColor = a.getColor(
                R.styleable.InteractiveLineGraphView_gridColor,
                mGridColor
            )
            mAxisThickness = a.getDimension(
                R.styleable.InteractiveLineGraphView_axisThickness,
                mAxisThickness
            )
            mAxisColor = a.getColor(
                R.styleable.InteractiveLineGraphView_axisColor,
                mAxisColor
            )
            dataThickness = a.getDimension(
                R.styleable.InteractiveLineGraphView_dataThickness,
                dataThickness
            )
            dataColor = a.getColor(
                R.styleable.InteractiveLineGraphView_dataColor,
                dataColor
            )
        } finally {
            a.recycle()
        }
        initPaints()

        //////////////////////////
        // Sets up interactions //
        //////////////////////////

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

    /**
     * This is called during layout when the size of this [View] has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0. First we call our super's
     * implementation of `onSizeChanged`. Then we use the [Rect.set] method of our [Rect] field
     * [mContentRect] to set its `left` coordinate to the left padding of this view (the value
     * returned by [getPaddingLeft], aka kotlin `paddingLeft` property) plus our [Int] field
     * [mMaxLabelWidth] (Maximum length of a label) plus our [Int] field [mLabelSeparation]
     * (separation between axis labels), and set its `top` coordinate to the top padding of this
     * view (the value returned by [getPaddingTop], aka kotlin `paddingTop` property), and set its
     * `right` coordinate to the width of our view (the value returned by [getWidth], aka kotlin
     * `width` property) minus the right padding of this view (the value returned by [getPaddingRight],
     * aka kotlin `paddingRight` property), and set its `bottom` coordinate to the height of our
     * view (the value returned by [getHeight], aka kotlin `height` property) minus the bottom
     * padding of this view (the value returned by [getPaddingBottom], aka kotlin `paddingBottom`
     * property) minus the height of the text (our [Int] field [mLabelHeight]), minus the separation
     * between axis labels (our [Int] field [mLabelSeparation])
     *
     * @param w Current width of this [View].
     * @param h Current height of this [View].
     * @param oldw Old width of this [View].
     * @param oldh Old height of this [View].
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mContentRect.set(
            /* left = */ paddingLeft + mMaxLabelWidth + mLabelSeparation,
            /* top = */ paddingTop,
            /* right = */ width - paddingRight,
            /* bottom = */ height - paddingBottom - mLabelHeight - mLabelSeparation
        )

    }

    /**
     * Measure the view and its content to determine the measured width and the measured height.
     * This method is invoked by [measure] and should be overridden by subclasses to provide
     * accurate and efficient measurement of their contents. When overriding this method, you
     * *must* call [setMeasuredDimension] to store the measured width and height of this view.
     * We initialize our [Int] variable `val minChartSize` to the value stored in our resources
     * for the dimension with resource ID [R.dimen.min_chart_size] (this is set to 100dp in the
     * file values/dimens.xml).
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent. The
     * requirements are encoded with [android.view.View.MeasureSpec].
     * @param heightMeasureSpec vertical space requirements as imposed by the parent. The
     * requirements are encoded with [android.view.View.MeasureSpec].
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minChartSize: Int = resources.getDimensionPixelSize(R.dimen.min_chart_size)
        setMeasuredDimension(
            Math.max(suggestedMinimumWidth,
                resolveSize(
                    minChartSize + paddingLeft + mMaxLabelWidth + mLabelSeparation + paddingRight,
                    widthMeasureSpec
                )
            ),
            Math.max(suggestedMinimumHeight,
                resolveSize(
                    minChartSize + paddingTop + mLabelHeight + mLabelSeparation + paddingBottom,
                    heightMeasureSpec
                )
            )
        )
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
            mXStopsBuffer
        )
        computeAxisStops(
            mCurrentViewport!!.top,
            mCurrentViewport!!.bottom,
            mContentRect.height() / mLabelHeight / 2,
            mYStopsBuffer
        )

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
                (mContentRect.bottom + mLabelHeight + mLabelSeparation).toFloat(),
                mLabelTextPaint!!
            )
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
                (mContentRect.left - mLabelSeparation).toFloat(),
                mAxisYPositionsBuffer[i] + mLabelHeight / 2,
                mLabelTextPaint!!
            )
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
     * Draws the currently visible portion of the data series defined by [`fun`] to the
     * canvas. This method does not clip its drawing, so users should call [Canvas.clipRect]
     * before calling this method.
     */
    private fun drawDataSeriesUnclipped(canvas: Canvas) {
        mSeriesLinesBuffer[0] = mContentRect.left.toFloat()
        mSeriesLinesBuffer[1] = getDrawY(fofX(mCurrentViewport!!.left))
        mSeriesLinesBuffer[2] = mSeriesLinesBuffer[0]
        mSeriesLinesBuffer[3] = mSeriesLinesBuffer[1]
        var x: Float
        for (i in 1..DRAW_STEPS) {
            mSeriesLinesBuffer[i * 4 + 0] = mSeriesLinesBuffer[(i - 1) * 4 + 2]
            mSeriesLinesBuffer[i * 4 + 1] = mSeriesLinesBuffer[(i - 1) * 4 + 3]
            x = mCurrentViewport!!.left + mCurrentViewport!!.width() / DRAW_STEPS * i
            mSeriesLinesBuffer[i * 4 + 2] = getDrawX(x)
            mSeriesLinesBuffer[i * 4 + 3] = getDrawY(fofX(x))
        }
        canvas.drawLines(mSeriesLinesBuffer, mDataPaint!!)
    }

    /**
     * Draws the overscroll "glow" at the four edges of the chart region, if necessary. The edges
     * of the chart region are stored in [mContentRect].
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
     * Finds the chart point (i.e. within the chart's domain and range) represented by the given
     * pixel coordinates, if that pixel is within the chart region described by [mContentRect]. If
     * the point is found, the "dest" argument is set to the point and this function returns `true`.
     * Otherwise, this function returns `false` and "dest" is unchanged.
     */
    private fun hitTest(x: Float, y: Float, dest: PointF): Boolean {
        if (!mContentRect.contains(x.toInt(), y.toInt())) {
            return false
        }
        dest.set(
            mCurrentViewport!!.left + mCurrentViewport!!.width() * (x - mContentRect.left) / mContentRect.width(),
            mCurrentViewport!!.top+ mCurrentViewport!!.height() * (y - mContentRect.bottom) / -mContentRect.height()
        )
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retVal = mScaleGestureDetector.onTouchEvent(event)
        retVal = mGestureDetector.onTouchEvent(event) || retVal
        return retVal || super.onTouchEvent(event)
    }

    /**
     * Ensures that current viewport is inside the viewport extremes defined by [AXIS_X_MIN],
     * [AXIS_X_MAX], [AXIS_Y_MIN] and [AXIS_Y_MAX].
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
        mEdgeEffectRightActive = false
        mEdgeEffectTopActive = false
        mEdgeEffectLeftActive = false
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
     * area is visible, this is simply the current size of [mContentRect]. If the chart
     * is zoomed in 200% in both directions, the returned size will be twice as large horizontally
     * and vertically.
     */
    private fun computeScrollSurfaceSize(out: Point) {
        out.set(
            (mContentRect.width() * (AXIS_X_MAX - AXIS_X_MIN) / mCurrentViewport!!.width()).toInt(),
            (mContentRect.height() * (AXIS_Y_MAX - AXIS_Y_MIN) / mCurrentViewport!!.height()).toInt()
        )
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
            if (canScrollY && currY < 0 && mEdgeEffectTop.isFinished && !mEdgeEffectTopActive) {
                mEdgeEffectTop.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectTopActive = true
                needsInvalidate = true
            } else if (canScrollY && currY > mSurfaceSizeBuffer.y - mContentRect.height()
                && mEdgeEffectBottom.isFinished
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
            mCurrentViewport!!.set(
                mZoomFocalPoint.x - newWidth * pointWithinViewportX,
                mZoomFocalPoint.y - newHeight * pointWithinViewportY,
                mZoomFocalPoint.x + newWidth * (1 - pointWithinViewportX),
                mZoomFocalPoint.y + newHeight * (1 - pointWithinViewportY)
            )
            constrainViewport()
            needsInvalidate = true
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /**
     * Sets the current viewport (defined by [mCurrentViewport]) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position, and thus
     * the bottom of the [mCurrentViewport] rectangle. For more details on why top and
     * bottom are flipped, see [mCurrentViewport].
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
        mCurrentViewport!!.set(
            xLocal,
            yLocal - curHeight,
            xLocal + curWidth,
            yLocal
        )
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
        mZoomFocalPoint.set(
            (mCurrentViewport!!.right + mCurrentViewport!!.left) / 2,
            (mCurrentViewport!!.bottom + mCurrentViewport!!.top) / 2
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Smoothly zooms the chart out one step.
     */
    fun zoomOut() {
        mScrollerStartViewport.set(mCurrentViewport!!)
        mZoomer.forceFinished(true)
        mZoomer.startZoom(-ZOOM_AMOUNT)
        mZoomFocalPoint.set(
            (mCurrentViewport!!.right + mCurrentViewport!!.left) / 2,
            (mCurrentViewport!!.bottom + mCurrentViewport!!.top) / 2
        )
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

        internal constructor(inParcel: Parcel) : super(inParcel) {
            viewport = RectF(
                inParcel.readFloat(),
                inParcel.readFloat(),
                inParcel.readFloat(),
                inParcel.readFloat()
            )
        }

        companion object {
            /**
             * TODO: Add kdoc
             */
            @Suppress("RedundantNullableReturnType")
            @JvmField
            val CREATOR = ParcelableCompat.newCreator(object : ParcelableCompatCreatorCallbacks<SavedState?> {
                override fun createFromParcel(inParcel: Parcel, loader: ClassLoader): SavedState? {
                    return SavedState(inParcel)
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
        private fun fofX(x: Float): Float {
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
         * Computes the set of axis labels to show given start and stop boundaries and an ideal
         * number of stops between these boundaries.
         *
         * @param start The minimum extreme (e.g. the left edge) for the axis.
         * @param stop The maximum extreme (e.g. the right edge) for the axis.
         * @param steps The ideal number of stops to create. This should be based on available
         * screen space; the more space there is, the more stops should be shown.
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