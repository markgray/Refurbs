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
@file:Suppress(
    "unused",
    "DEPRECATION",
    "UNUSED_CHANGED_VALUE",
    "ReplaceJavaStaticMethodWithKotlinAnalog",
    "JoinDeclarationAndAssignment",
    "ReplaceNotNullAssertionWithElvisReturn",
    "MemberVisibilityCanBePrivate"
)

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
    private var mCurrentViewport: RectF? = RectF(
        /* left = */ AXIS_X_MIN,
        /* top = */ AXIS_Y_MIN,
        /* right = */ AXIS_X_MAX,
        /* bottom = */ AXIS_Y_MAX
    )

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
     * It is set by the attribute `R.styleable.InteractiveLineGraphView_labelTextSize` of
     * `R.styleable.InteractiveLineGraphView` to app:labelTextSize="14sp" in the layout file
     * `activity_main.xml`
     */
    private var mLabelTextSize = 0f

    /**
     * The separation between axis labels, it is used by the [drawAxes] method. It is set by the
     * attribute `R.styleable.InteractiveLineGraphView_labelSeparation` of
     * `R.styleable.InteractiveLineGraphView` to app:labelSeparation="10dp" in the layout file
     * `activity_main.xml`
     */
    private var mLabelSeparation = 0

    /**
     * The color of the text drawn by [Paint] field [mLabelTextPaint], it is used in the call to
     * its [Paint.setColor] method (kotlin `color` property) in our [initPaints] method. It is set
     * by the attribute `R.styleable.InteractiveLineGraphView_labelTextColor` of
     * `R.styleable.InteractiveLineGraphView` to app:labelTextColor="#d000" in the layout file
     * `activity_main.xml`
     */
    private var mLabelTextColor = 0

    /**
     * The [Paint] used by our [drawAxes] method to draw text. It is constructed and configured in
     * our [initPaints] method.
     */
    private var mLabelTextPaint: Paint? = null

    /**
     * Maximum length of a label, used when an estimated size for it is needed, it is set in our
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
     * `R.styleable.InteractiveLineGraphView_gridThickness` of `R.styleable.InteractiveLineGraphView`
     * to app:gridThickness="1dp" in the layout file `activity_main.xml`
     */
    private var mGridThickness = 0f

    /**
     * This is used in a call to the [Paint.setColor] method of [Paint] field [mGridPaint]
     * (aka kotlin `color` property) to set the paint's color.  It is set by the attribute
     * `R.styleable.InteractiveLineGraphView_gridColor` of `R.styleable.InteractiveLineGraphView`
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
     * `R.styleable.InteractiveLineGraphView_axisThickness` of `R.styleable.InteractiveLineGraphView`
     * to app:axisThickness="2dp" in the layout file `activity_main.xml`
     */
    private var mAxisThickness = 0f

    /**
     * This is used in a call to the [Paint.setColor] method of [Paint] field [mAxisPaint]
     * (aka kotlin `color` property) to set the paint's color.  It is set by the attribute
     * `R.styleable.InteractiveLineGraphView_axisColor` of `R.styleable.InteractiveLineGraphView`
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
     * `R.styleable.InteractiveLineGraphView_dataThickness` of `R.styleable.InteractiveLineGraphView`
     * to app:dataThickness="8dp" in the layout file `activity_main.xml`
     */
    var dataThickness: Float = 0f

    /**
     * This is used in a call to the [Paint.setColor] method of [Paint] field [mDataPaint]
     * (aka kotlin `color` property) to set the paint's color.  It is set by the attribute
     * `R.styleable.InteractiveLineGraphView_dataColor` of `R.styleable.InteractiveLineGraphView`
     * to app:dataColor="#a6c" in the layout file `activity_main.xml`
     */
    var dataColor: Int = 0

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
    private val mXStopsBuffer: AxisStops = AxisStops()

    /**
     * Holds the Y axis label values.
     */
    private val mYStopsBuffer: AxisStops = AxisStops()

    /////////////////////////////////////////////////////
    // Buffers used during drawing. These are defined  //
    // as fields to avoid allocation during draw calls.//
    /////////////////////////////////////////////////////

    /**
     * This buffer holds all of the X coordinates of the vertical grid lines.
     */
    private var mAxisXPositionsBuffer: FloatArray = floatArrayOf()

    /**
     * This buffer holds all of the Y coordinates of the horizontal grid lines.
     */
    private var mAxisYPositionsBuffer: FloatArray = floatArrayOf()

    /**
     * This buffer holds the (x,y) end points of all of the X axis verical grid lines (4 values per
     * line), it is used by our [drawAxes] method in its call to [Canvas.drawLines] to draw all the
     * lines at once.
     */
    private var mAxisXLinesBuffer: FloatArray = floatArrayOf()

    /**
     * This buffer holds the (x,y) end points of all of the Y axis horizontal grid lines (4 values
     * per line), it is used by our [drawAxes] method in its call to [Canvas.drawLines] to draw all
     * the lines at once.
     */
    private var mAxisYLinesBuffer: FloatArray = floatArrayOf()

    /**
     * This buffer holds the (x,y) end points of all of the lines that are used to draw the curve
     * produced by our [fofX] method, it is used by our [drawDataSeriesUnclipped] method in its call
     * to [Canvas.drawLines] to draw all the lines at once.
     */
    private val mSeriesLinesBuffer: FloatArray = FloatArray((DRAW_STEPS + 1) * 4)

    /**
     * Holds the text that is used to hold that labels for the values of points along both axis.
     */
    private val mLabelBuffer: CharArray = CharArray(100)

    /**
     * Holds the current scrollable surface size, in pixels computed by [computeScrollSurfaceSize].
     * If the entire chart area is visible, this is simply the current size of [mContentRect]. If
     * the chart is zoomed in 200% in both directions, the returned size will be twice as large
     * horizontally and vertically.
     */
    private val mSurfaceSizeBuffer: Point = Point()

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private val mScaleGestureListener: OnScaleGestureListener =
        object : SimpleOnScaleGestureListener() {
            /**
             * This is the active focal point in terms of the viewport. Could be a local
             * variable but kept here to minimize per-frame allocations.
             */
            private val viewportFocus: PointF = PointF()

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
             * TODO: Continue here.
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
            e1: MotionEvent?,
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
            val viewportOffsetX: Float =
                distanceX * mCurrentViewport!!.width() / mContentRect.width()
            val viewportOffsetY: Float =
                -distanceY * mCurrentViewport!!.height() / mContentRect.height()
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
            e1: MotionEvent?,
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
     * for the dimension with resource ID `R.dimen.min_chart_size` (this is set to 100dp in the
     * file values/dimens.xml). Then we call [setMeasuredDimension] with the `measuredWidth`
     * argument the maximum of the value returned by the [getSuggestedMinimumWidth] method (aka
     * kotlin `suggestedMinimumWidth` property) and the value returned by the [View.resolveSize]
     * method when passed the quantity `minChartSize` plus the value returned by [getPaddingLeft]
     * (kotlin `paddingLeft` property) plus [mMaxLabelWidth] plus [mLabelSeparation] plus the value
     * returned by [getPaddingRight] (kotlin `paddingRight` property) and our [Int] parameter
     * [widthMeasureSpec]. The `measuredHeight` argument of [setMeasuredDimension] is the maximum
     * of the value returned by the [getSuggestedMinimumHeight] method (aka kotlin
     * `suggestedMinimumHeight` property) and the value returned by the [View.resolveSize]
     * method when passed the quantity `minChartSize` plus the value returned by [getPaddingTop]
     * (kotlin `paddingTop` property) plus [mLabelHeight] plus [mLabelSeparation] plus the value
     * returned by [getPaddingBottom] (kotlin `paddingBottom` property) and our [Int] parameter
     * [heightMeasureSpec].
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent. The
     * requirements are encoded with [android.view.View.MeasureSpec].
     * @param heightMeasureSpec vertical space requirements as imposed by the parent. The
     * requirements are encoded with [android.view.View.MeasureSpec].
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minChartSize: Int = resources.getDimensionPixelSize(R.dimen.min_chart_size)
        setMeasuredDimension(
            /* measuredWidth = */ Math.max(
                suggestedMinimumWidth,
                resolveSize(
                    minChartSize + paddingLeft + mMaxLabelWidth + mLabelSeparation + paddingRight,
                    widthMeasureSpec
                )
            ),
            /* measuredHeight = */ Math.max(
                suggestedMinimumHeight,
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

    /**
     * We implement this to do our drawing. First we call our super's implementation of `onDraw`.
     * Next we call our [drawAxes] method with our [Canvas] parameter [canvas] to have it draw the
     * axes and text labels of our graph. Next we initialize our [Int] variable `val clipRestoreCount`
     * to the value returned when we call the [Canvas.save] method of our [Canvas] parameter [canvas]
     * to save its current matrix and clip onto a private stack. We then call our method
     * [drawDataSeriesUnclipped] with [canvas] as its argument to have it draw the currently visible
     * portion of the data series defined by [fofX] to the [Canvas] parameter [canvas], followed by
     * a call to our [drawEdgeEffectsUnclipped] metod to have it draw the overscroll "glow" at the
     * four edges of the chart region, if necessary. Now that we have finished drawing our contents
     * we call the [Canvas.restoreToCount] method of [canvas] to have it restore its state to the
     * one it hqd before our call to [Canvas.save]. Finally we call the [Canvas.drawRect] method
     * of [canvas] to have it draw the [Rect] field [mContentRect] on [canvas] using [Paint] field
     * [mAxisPaint] as the [Paint].
     *
     * @param canvas the [Canvas] on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draws axes and text labels
        drawAxes(canvas)

        // Clips the next few drawing operations to the content area
        @SuppressLint("UseKtx") // TODO: Canvas.withClip(){} Quick fix is broken
        val clipRestoreCount: Int = canvas.save()
        canvas.clipRect(mContentRect)
        drawDataSeriesUnclipped(canvas)
        drawEdgeEffectsUnclipped(canvas)

        // Removes clipping rectangle
        canvas.restoreToCount(clipRestoreCount)

        // Draws chart container
        canvas.drawRect(mContentRect, mAxisPaint!!)
    }

    /**
     * Draws the chart axes and labels onto the canvas. First we calculate the "stops" along the X
     * axis by calling our [computeAxisStops] method with the `start` argument the [RectF.left]
     * coordinate of [RectF] field [mCurrentViewport] (left side of the currently visible chart),
     * with the `stop` argument the [RectF.right] coordinate of [RectF] field [mCurrentViewport]
     * (right side of the currently visible chart), the `steps` argument the [Rect.width] of our
     * [Rect] field [mContentRect] (current destination rectangle into which the chart data should
     * be drawn) divided by our [Int] field [mMaxLabelWidth] (Maximum length of a label), divided
     * by 2 (ideal number of stops to create). The `outStops` argument (destination AxisStops object
     * to populate) is our [AxisStops] field [mXStopsBuffer] (Holds the X axis label values). Next
     * we calculate the "stops" along the Y axis by calling our [computeAxisStops] method with the
     * `start` argument the [RectF.top] coordinate of [RectF] field [mCurrentViewport] (top side of
     * the currently visible chart), with the `stop` argument the [RectF.bottom] coordinate of
     * [RectF] field [mCurrentViewport] (bottom side of the currently visible chart), the `steps`
     * argument the [Rect.height] of our [Rect] field [mContentRect] (current destination rectangle
     * into which the chart data should be drawn) divided by our [Int] field [mLabelHeight] (height
     * of the text drawn), divided by 2 (ideal number of stops to create). The `outStops` argument
     * (destination [AxisStops] object to populate) is our [AxisStops] field [mYStopsBuffer] (Holds
     * the Y axis label values).
     *
     * Next we check to see if we need to allocate larger [FloatArray]s for our four [FloatArray]
     * fields: [mAxisXPositionsBuffer] (holds all of the X coordinates of the vertical grid lines,
     * it needs to be at least the size of the [AxisStops.numStops] property of [mXStopsBuffer]),
     * [mAxisYPositionsBuffer] (holds all of the Y coordinates of the horizontal grid lines,
     * it needs to be at least the size of the [AxisStops.numStops] property of [mYStopsBuffer]),
     * [mAxisXLinesBuffer] (holds the (x,y) end points of all of the X axis verical grid lines,
     * four values per line, so it needs to be at least 4 times the size of the [AxisStops.numStops]
     * property of [mXStopsBuffer]), and [mAxisYLinesBuffer] (holds the (x,y) end points of all of
     * the Y axis horizontal grid lines, four values per line, so it needs to be at least 4 times
     * the size of the [AxisStops.numStops] property of [mYStopsBuffer]).
     *
     * Next we loop over [Int] variable `i` for all of the [AxisStops.numStops] of [mXStopsBuffer]
     * setting the `i`'th entry in [mAxisXPositionsBuffer] to the value returned by our [getDrawX]
     * method for the `x` argument the `i`'th entry in the [AxisStops.stops] array of [mXStopsBuffer].
     * After this we loop over [Int] variable `i` for all of the [AxisStops.numStops] of [mYStopsBuffer]
     * setting the `i`'th entry in [mAxisYPositionsBuffer] to the value returned by our [getDrawY]
     * method for the `y` argument the `i`'th entry in the [AxisStops.stops] array of [mYStopsBuffer].
     *
     * Next we have to fill our [FloatArray] field [mAxisXLinesBuffer] with four values for each of
     * the [AxisStops.numStops] entries in our [mXStopsBuffer] field:
     *
     *  0: The X coordinate of the start of the line is the [Math.floor] of the `i`'th entry in
     *  [FloatArray] field [mAxisXPositionsBuffer].
     *
     *  1: The Y coordinate of the start of the line is the [Rect.top] of [mContentRect].
     *
     *  2: The X coordinate of the end of the line is the [Math.floor] of the `i`'th entry in
     *  [FloatArray] field [mAxisXPositionsBuffer].
     *
     *  3: The Y coordinate of the end of the line is the [Rect.top] of [mContentRect].
     *
     * Having filled [mAxisXLinesBuffer] with the coordinates of the grid lines to be drawn we call
     * the [Canvas.drawLines] method of our [Canvas] parameter [canvas] to draw all the lines at
     * once using [Paint] field [mGridPaint] as the [Paint].
     *
     * Next we have to fill our [FloatArray] field [mAxisYLinesBuffer] with four values for each of
     * the [AxisStops.numStops] entries in our [mYStopsBuffer] field:
     *
     *  0: The X coordinate of the start of the line is the [Rect.left] of [mContentRect].
     *
     *  1: The Y coordinate of the start of the line is the [Math.floor] of the `i`'th entry in
     *  [FloatArray] field [mAxisYPositionsBuffer].
     *
     *  2: The X coordinate of the end of the line is the [Rect.right] of [mContentRect].
     *
     *  3: The Y coordinate of the end of the line is the [Math.floor] of the `i`'th entry in
     *  [FloatArray] field [mAxisYPositionsBuffer].
     *
     * Having filled [mAxisYLinesBuffer] with the coordinates of the grid lines to be drawn we call
     * the [Canvas.drawLines] method of our [Canvas] parameter [canvas] to draw all the lines at
     * once using [Paint] field [mGridPaint] as the [Paint].
     *
     * Now we want to draw all the labels. We start by declaring our [Int] variables `var labelOffset`
     * and `var labelLength`. We use the [Paint.setTextAlign] method (aka kotlin `textAlign` property)
     * to set the text alignment or [Paint] field [mLabelTextPaint] to [Paint.Align.CENTER] (text is
     * drawn centered horizontally on the x,y origin). We set `i` to 0 and loop over `i` while `i`
     * is less than the [AxisStops.numStops] property of [mXStopsBuffer]:
     *
     *  - We set `labelLength` to the value returned by our [formatFloat] method (the length of the
     *  string it created) when we call it with its `out` argument our [CharArray] field [mLabelBuffer]
     *  (destination of the formatted result), its `value` argument the `i`'th entry in the
     *  [AxisStops.stops] array of [mXStopsBuffer], and its `digits` argument the [AxisStops.decimals]
     *  field of [mXStopsBuffer].
     *  - We set `labelOffset` to the [CharArray.size] of [mLabelBuffer] minus `labelLength`
     *  - We call the [Canvas.drawText] method of [Canvas] parameter [canvas] to draw the text in
     *  [mLabelBuffer] from `index` starting at `labelOffset`, `count` of character `labelLength`,
     *  `x` coordinate the `i`'th entry in [FloatArray] field [mAxisXPositionsBuffer], `y` coordinate
     *  the [Rect.bottom] of [Rect] field [mContentRect] plus our [Int] field [mLabelHeight] plus
     *  our [Int] field [mLabelSeparation], and with our [Paint] field [mLabelTextPaint] used as the
     *  [Paint].
     *  - We then increment `i` and loop around for the next entry in [AxisStops] field [mXStopsBuffer].
     *
     * To draw the Y labels we first use the [Paint.setTextAlign] method (aka kotlin `textAlign`
     * property) to set the text alignment or [Paint] field [mLabelTextPaint] to [Paint.Align.RIGHT]
     * (text is drawn to the left of the x,y origin). We set `i` to 0 and loop over `i` while `i`
     * is less than the [AxisStops.numStops] property of [mYStopsBuffer]:
     *
     *  - We set `labelLength` to the value returned by our [formatFloat] method (the length of the
     *  string it created) when we call it with its `out` argument our [CharArray] field [mLabelBuffer]
     *  (destination of the formatted result), its `value` argument the `i`'th entry in the
     *  [AxisStops.stops] array of [mYStopsBuffer], and its `digits` argument the [AxisStops.decimals]
     *  field of [mYStopsBuffer].
     *  - We set `labelOffset` to the [CharArray.size] of [mLabelBuffer] minus `labelLength`
     *  - We call the [Canvas.drawText] method of [Canvas] parameter [canvas] to draw the text in
     *  [mLabelBuffer] from `index` starting at `labelOffset`, `count` of character `labelLength`,
     *  `x` coordinate the [Rect.left] of [Rect] field [mContentRect] minus [Int] field
     *  [mLabelSeparation], `y` coordinate the `i`'th entry in [FloatArray] field [mAxisYPositionsBuffer]
     *  plus half of [Int] field [mLabelHeight], and with our [Paint] field [mLabelTextPaint] used
     *  as the [Paint].
     *  - We then increment `i` and loop around for the next entry in [AxisStops] field [mYStopsBuffer].
     *
     * @param canvas the [Canvas] on which we are to draw our chart axes and labels
     */
    private fun drawAxes(canvas: Canvas) {
        // Computes axis stops (in terms of numerical value and position on screen)
        var i: Int
        computeAxisStops(
            start = mCurrentViewport!!.left,
            stop = mCurrentViewport!!.right,
            steps = mContentRect.width() / mMaxLabelWidth / 2,
            outStops = mXStopsBuffer
        )
        computeAxisStops(
            start = mCurrentViewport!!.top,
            stop = mCurrentViewport!!.bottom,
            steps = mContentRect.height() / mLabelHeight / 2,
            outStops = mYStopsBuffer
        )

        // Avoid unnecessary allocations during drawing. Re-use allocated
        // arrays and only reallocate if the number of stops grows.
        if (mAxisXPositionsBuffer.size < mXStopsBuffer.numStops) {
            mAxisXPositionsBuffer = FloatArray(size = mXStopsBuffer.numStops)
        }
        if (mAxisYPositionsBuffer.size < mYStopsBuffer.numStops) {
            mAxisYPositionsBuffer = FloatArray(size = mYStopsBuffer.numStops)
        }
        if (mAxisXLinesBuffer.size < mXStopsBuffer.numStops * 4) {
            mAxisXLinesBuffer = FloatArray(size = mXStopsBuffer.numStops * 4)
        }
        if (mAxisYLinesBuffer.size < mYStopsBuffer.numStops * 4) {
            mAxisYLinesBuffer = FloatArray(size = mYStopsBuffer.numStops * 4)
        }

        // Compute positions
        i = 0
        while (i < mXStopsBuffer.numStops) {
            mAxisXPositionsBuffer[i] = getDrawX(x = mXStopsBuffer.stops[i])
            i++
        }
        i = 0
        while (i < mYStopsBuffer.numStops) {
            mAxisYPositionsBuffer[i] = getDrawY(y = mYStopsBuffer.stops[i])
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
            labelLength = formatFloat(
                out = mLabelBuffer,
                value = mXStopsBuffer.stops[i],
                digits = mXStopsBuffer.decimals
            )
            labelOffset = mLabelBuffer.size - labelLength
            canvas.drawText(
                /* text = */ mLabelBuffer,
                /* index = */ labelOffset,
                /* count = */ labelLength,
                /* x = */ mAxisXPositionsBuffer[i],
                /* y = */ (mContentRect.bottom + mLabelHeight + mLabelSeparation).toFloat(),
                /* paint = */ mLabelTextPaint!!
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
                /* text = */ mLabelBuffer,
                /* index = */ labelOffset,
                /* count = */ labelLength,
                /* x = */ (mContentRect.left - mLabelSeparation).toFloat(),
                /* y = */ mAxisYPositionsBuffer[i] + mLabelHeight / 2,
                /* paint = */ mLabelTextPaint!!
            )
            i++
        }
    }

    /**
     * Computes the pixel offset for the given X chart value. This may be outside the view bounds.
     * We initialize our [Float] variable `val offset` to our [Float] parameter [x] minus the
     * [RectF.left] property of [RectF] field [mCurrentViewport], with that quantity divided by
     * the [RectF.width] property of [mCurrentViewport]. We add the [Rect.left] property of [Rect]
     * field [mContentRect] to the [Rect.width] of [mContentRect] times `offset` and return this
     * value to the caller.
     *
     * @param x the X coordinate of a point in chart space.
     * @return the X coordinate in the device screen space.
     */
    private fun getDrawX(x: Float): Float {
        val offset: Float = (x - mCurrentViewport!!.left) / mCurrentViewport!!.width()
        return (mContentRect.left + mContentRect.width() * offset)
    }

    /**
     * Computes the pixel offset for the given Y chart value. This may be outside the view bounds.
     * We initialize our [Float] variable `val offset` to our [Float] parameter [y] minus the
     * [RectF.top] property of [RectF] field [mCurrentViewport], with that quantity divided by the
     * [RectF.height] property of [mCurrentViewport]. We subtract from the [Rect.bottom] property of
     * [Rect] field [mContentRect] the [Rect.height] of [mContentRect] times `offset` and return this
     * value to the caller.
     *
     * @param y the Y coordinate of a point in chart space.
     * @return the Y coordinate in the device screen space.
     */
    private fun getDrawY(y: Float): Float {
        val offset: Float = (y - mCurrentViewport!!.top) / mCurrentViewport!!.height()
        return (mContentRect.bottom - mContentRect.height() * offset)
    }

    /**
     * Draws the currently visible portion of the data series defined by [fofX] to the
     * canvas. This method does not clip its drawing, so users should call [Canvas.clipRect]
     * before calling this method. Our [FloatArray] field [mSeriesLinesBuffer] has four entries
     * for each line of the curve we draw: the X coordinate of the start of the line, the Y
     * coordinate of the start of the line, the X coordinate of the end of the line, and the Y
     * coordinate of the end of the line. We start by setting the values for the first line:
     *  0: X coordinate of the start of the line is the [Rect.left] of [mContentRect] as a [Float]
     *  1: Y coordinate of the start of the line is the value returned by [getDrawY] when passed
     *  the results of calling [fofX] with the [RectF.left] property of [mCurrentViewport]
     *  2: is a copy of the 0'th  entry in [mSeriesLinesBuffer].
     *  3: is a copy of the 1'th  entry in [mSeriesLinesBuffer].
     *
     * We then declare [Float] variable `var x`, and loop over `i` from 1 until [DRAW_STEPS]:
     *  - we set the X coordinate of the start of the line to the X coordinate of the end of the
     *  line of the previous (`i` minus 1) line.
     *  - we set the Y coordinate of the start of the line to the Y coordinate of the end of the
     *  line of the previous (`i` minus 1) line.
     *  - we set our [Float] variable `x` to the value of the [RectF.left] property of [mCurrentViewport]
     *  plus the [RectF.width] of [mCurrentViewport] divided by [DRAW_STEPS] times `i`.
     *  - we set the X coordinate of the end of the line to the value returned by [getDrawX] when
     *  passed `x`
     *  - we set the Y coordinate of the end of the line to the value returned by [getDrawY] when
     *  passed the results of calling [fofX] with `x` as its argument.
     *
     * Having filled [mSeriesLinesBuffer] with all of the line coordinates needed, we call the
     * [Canvas.drawLines] method of [canvas] with [mSeriesLinesBuffer] as it `pts` argument and
     * [Paint] field [mDataPaint] as the [Paint] to use.
     *
     * @param canvas the [Canvas] we should draw on.
     */
    private fun drawDataSeriesUnclipped(canvas: Canvas) {
        mSeriesLinesBuffer[0] = mContentRect.left.toFloat()
        mSeriesLinesBuffer[1] = getDrawY(y = fofX(x = mCurrentViewport!!.left))
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
     * of the chart region are stored in [mContentRect]. We start by initializing our [Boolean]
     * variable `var needsInvalidate` to `false`. We then check each of our four [EdgeEffectCompat]
     * to see if they need drawing:
     *  - [mEdgeEffectTop] if its [EdgeEffectCompat.isFinished] returns `false` we initialize our
     *  [Int] variable `val restoreCount` to the value returned by the [Canvas.save] method of
     *  [canvas] (saves the current matrix and clip onto a private stack, and returns the value to
     *  use when calling [Canvas.restoreToCount] to restore the [Canvas] to its previous state).
     *  Then we call the [Canvas.translate] method of [canvas] to translate the [Canvas] to X
     *  coordinate [Rect.left] of [Rect] field [mContentRect] and Y coordinate [Rect.top]. We call
     *  the [EdgeEffectCompat.setSize] method of [mEdgeEffectTop] to set its size to [Rect.width]
     *  of [mContentRect] by its [Rect.height]. We then call the [EdgeEffectCompat.draw] method of
     *  [mEdgeEffectTop] with [canvas] and if it returns `true` we set `needsInvalidate` to `true`.
     *  Finally we call the [Canvas.restoreToCount] method of [canvas] with `restoreCount` to
     *  restore its state to the state it had before we called its [Canvas.save] method.
     *
     *  - [mEdgeEffectBottom] if its [EdgeEffectCompat.isFinished] returns `false` we initialize our
     *  [Int] variable `val restoreCount` to the value returned by the [Canvas.save] method of
     *  [canvas] (saves the current matrix and clip onto a private stack, and returns the value to
     *  use when calling [Canvas.restoreToCount] to restore the [Canvas] to its previous state).
     *  Then we call the [Canvas.translate] method of [canvas] to translate the [Canvas] to X
     *  coordinate [Rect.left] of [Rect] field [mContentRect] and Y coordinate [Rect.top]. Next we
     *  call the [Canvas.rotate] method of [canvas] to rotate the canvas 180 degrees with the pivot
     *  point X coordinate the [Rect.width] of  [mContentRect] and the Y coordinate 0. Then we call
     *  the [EdgeEffectCompat.setSize] method of [mEdgeEffectBottom] to set its size to [Rect.width]
     *  of [mContentRect] by its [Rect.height]. We then call the [EdgeEffectCompat.draw] method of
     *  [mEdgeEffectBottom] with [canvas] and if it returns `true` we set `needsInvalidate` to `true`.
     *  Finally we call the [Canvas.restoreToCount] method of [canvas] with `restoreCount` to
     *  restore its state to the state it had before we called its [Canvas.save] method.
     *
     *  - [mEdgeEffectLeft] if its [EdgeEffectCompat.isFinished] returns `false` we initialize our
     *  [Int] variable `val restoreCount` to the value returned by the [Canvas.save] method of
     *  [canvas] (saves the current matrix and clip onto a private stack, and returns the value to
     *  use when calling [Canvas.restoreToCount] to restore the [Canvas] to its previous state).
     *  Then we call the [Canvas.translate] method of [canvas] to translate the [Canvas] to X
     *  coordinate [Rect.left] of [Rect] field [mContentRect] and Y coordinate [Rect.bottom]. Next
     *  we call the [Canvas.rotate] method of [canvas] to rotate the canvas 90 degrees with the
     *  pivot point X coordinate 0f and the Y coordinate 0f. Then we call the [EdgeEffectCompat.setSize]
     *  method of [mEdgeEffectLeft] to set its size to [Rect.height] of [mContentRect] by its
     *  [Rect.width]. We then call the [EdgeEffectCompat.draw] method of [mEdgeEffectLeft] with
     *  [canvas] and if it returns `true` we set `needsInvalidate` to `true`. Finally we call the
     *  [Canvas.restoreToCount] method of [canvas] with `restoreCount` to restore its state to the
     *  state it had before we called its [Canvas.save] method.
     *
     *  - [mEdgeEffectRight] if its [EdgeEffectCompat.isFinished] returns `false` we initialize our
     *  [Int] variable `val restoreCount` to the value returned by the [Canvas.save] method of
     *  [canvas] (saves the current matrix and clip onto a private stack, and returns the value to
     *  use when calling [Canvas.restoreToCount] to restore the [Canvas] to its previous state).
     *  Then we call the [Canvas.translate] method of [canvas] to translate the [Canvas] to X
     *  coordinate [Rect.right] of [Rect] field [mContentRect] and Y coordinate [Rect.top]. Next
     *  we call the [Canvas.rotate] method of [canvas] to rotate the canvas 90 degrees with the
     *  pivot point X coordinate 0f and the Y coordinate 0f. Then we call the [EdgeEffectCompat.setSize]
     *  method of [mEdgeEffectRight] to set its size to [Rect.height] of [mContentRect] by its
     *  [Rect.width]. We then call the [EdgeEffectCompat.draw] method of [mEdgeEffectRight] with
     *  [canvas] and if it returns `true` we set `needsInvalidate` to `true`. Finally we call the
     *  [Canvas.restoreToCount] method of [canvas] with `restoreCount` to restore its state to the
     *  state it had before we called its [Canvas.save] method.
     *
     * Finally if `needsInvalidate` is `true` we call the [ViewCompat.postInvalidateOnAnimation]
     * method to cause an invalidate to happen for `this` [View] on the next animation time step,
     * typically the next display frame.
     *
     * @param canvas the [Canvas] we should draw on.
     * @see EdgeEffectCompat
     */
    private fun drawEdgeEffectsUnclipped(canvas: Canvas) {
        // The methods below rotate and translate the canvas as needed before drawing the glow,
        // since EdgeEffectCompat always draws a top-glow at 0,0.
        var needsInvalidate = false
        if (!mEdgeEffectTop.isFinished) {
            @SuppressLint("UseKtx") // TODO: Canvas.withTransition(){} Quick fix is broken
            val restoreCount: Int = canvas.save()
            canvas.translate(
                mContentRect.left.toFloat(),
                mContentRect.top.toFloat()
            )
            mEdgeEffectTop.setSize(mContentRect.width(), mContentRect.height())
            if (mEdgeEffectTop.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (!mEdgeEffectBottom.isFinished) {
            @SuppressLint("UseKtx") // TODO: Canvas.withTransition(){} Quick fix is broken
            val restoreCount = canvas.save()
            canvas.translate(
                (2 * mContentRect.left - mContentRect.right).toFloat(),
                mContentRect.bottom.toFloat()
            )
            canvas.rotate(180f, mContentRect.width().toFloat(), 0f)
            mEdgeEffectBottom.setSize(mContentRect.width(), mContentRect.height())
            if (mEdgeEffectBottom.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (!mEdgeEffectLeft.isFinished) {
            @SuppressLint("UseKtx") // TODO: Canvas.withTransition(){} Quick fix is broken
            val restoreCount = canvas.save()
            canvas.translate(
                mContentRect.left.toFloat(),
                mContentRect.bottom.toFloat()
            )
            canvas.rotate(-90f, 0f, 0f)
            mEdgeEffectLeft.setSize(mContentRect.height(), mContentRect.width())
            if (mEdgeEffectLeft.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }
        if (!mEdgeEffectRight.isFinished) {
            @SuppressLint("UseKtx") // TODO: Canvas.withTransition(){} Quick fix is broken
            val restoreCount = canvas.save()
            canvas.translate(
                mContentRect.right.toFloat(),
                mContentRect.top.toFloat()
            )
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
     *
     * If ([x], [y]) is not in [mContentRect] we return `false`, otherwise we call the [PointF.set]
     * method of [PointF] parameter [dest] to set its `x` coordinate to the [RectF.left] of [RectF]
     * field [mCurrentViewport] plus the quantity [RectF.width] of [mCurrentViewport] times [x] minus
     * [Rect.left] of [mContentRect] divided by the [Rect.width] of [mContentRect], and we set the
     * [PointF.y] of [dest] to the [RectF.top] of [RectF] field [mCurrentViewport] plus the quantity
     * [RectF.height] of [mCurrentViewport] times [y] minus [Rect.bottom] of [mContentRect] divided
     * by minus the [Rect.height] of [mContentRect].
     *
     * @param x the X coordinate of the point within [Rect] field [mContentRect].
     * @param y the Y coordinate of the point within [Rect] field [mContentRect].
     * @param dest the [PointF] within [mCurrentViewport] for ([x], [y]) if ([x], [y]) is within
     * [mContentRect].
     * @return `true` if the point is found in [mContentRect], `false` otherwise.
     */
    private fun hitTest(x: Float, y: Float, dest: PointF): Boolean {
        if (!mContentRect.contains(x.toInt(), y.toInt())) {
            return false
        }
        dest.set(
            mCurrentViewport!!.left + mCurrentViewport!!.width() * (x - mContentRect.left) / mContentRect.width(),
            mCurrentViewport!!.top + mCurrentViewport!!.height() * (y - mContentRect.bottom) / -mContentRect.height()
        )
        return true
    }

    /**
     * We implement this method to handle touch screen motion events. We initialize our [Boolean]
     * variable `var retVal` to the value returned by the [ScaleGestureDetector.onTouchEvent] of
     * our [ScaleGestureDetector] field [mScaleGestureDetector] (its [OnScaleGestureListener]
     * Detects scaling transformation, and returns `true` if the event was processed). Then we set
     * `retVal` to the inclusive `or` of `retVal` and the value returned by the
     * [GestureDetectorCompat.onTouchEvent] method of [GestureDetectorCompat] field [mGestureDetector]
     * (its [SimpleOnGestureListener] is used for handling simple gestures such as double touches,
     * scrolls, and flings, `true` if it consumed the event, else `false`). If `retVal` is `true`
     * we return it to the caller, otherwise we return the value returned by our super's implementation
     * of `onTouchEvent`.
     *
     * @param event The motion event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retVal: Boolean = mScaleGestureDetector.onTouchEvent(event)
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
        mCurrentViewport!!.bottom = Math.max(
            Math.nextUp(mCurrentViewport!!.top),
            Math.min(AXIS_Y_MAX, mCurrentViewport!!.bottom)
        )
        mCurrentViewport!!.right = Math.max(
            Math.nextUp(mCurrentViewport!!.left),
            Math.min(AXIS_X_MAX, mCurrentViewport!!.right)
        )
    }

    /**
     * Terminates all [EdgeEffectCompat] animations that may be in progress.
     */
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

    /**
     * Does the actual call to [OverScroller.fling] of [OverScroller] field [mScroller] in response
     * to a `fling` gesture. First we call our [releaseEdgeEffects] method to have it terminate all
     * [EdgeEffectCompat] animations that may be in progress. Then we call our [computeScrollSurfaceSize]
     * method to have it compute the current scrollable surface size in pixels and store the result
     * in [Point] field [mSurfaceSizeBuffer]. We call the [RectF.set] method of [RectF] field
     * [mScrollerStartViewport] to set it to [RectF] field [mCurrentViewport] (the currently visible
     * chart domain and range). We initialize [Int] variable `val startX` to the [Point.x] property
     * of [Point] field [mSurfaceSizeBuffer] times the quantity of the [RectF.left] property of
     * [RectF] field [mScrollerStartViewport] minus [AXIS_X_MIN] (-1f) all divided by the quantity
     * [AXIS_X_MAX] (1f) minus [AXIS_X_MIN], and we initialize our [Int] variable `val startY` to
     * the [Point.y] property of [Point] field [mSurfaceSizeBuffer] times the quantity of [AXIS_Y_MAX]
     * (1f) minus the [RectF.bottom] property of [RectF] field [mScrollerStartViewport] all divided
     * by [AXIS_Y_MAX] minus [AXIS_Y_MIN] (-1f).
     *
     * We call the [OverScroller.forceFinished] method of [OverScroller] field [mScroller] with
     * `true` to force it to be "finished", then we call the [OverScroller.fling] method of
     * [mScroller] to fling it from starting X value `startX`, starting Y value `startY`, with an
     * initial velocity in the X direction of our [Int] parameter [velocityX] and an initial
     * velocity in the Y direction of our [Int] parameter [velocityY]. Its `minX` value is 0,
     * its `maxX` value is the [Point.x] property of [Point] field [mSurfaceSizeBuffer] minus the
     * [Rect.width] property of [Rect] field [mContentRect], its `minY` value is 0, its `maxY` value
     * is the [Point.y] property of [Point] field [mSurfaceSizeBuffer] minus the [Rect.height]
     * property of [Rect] field [mContentRect]. Its `overX` (Overfling range) is the [Rect.width]
     * of [mContentRect] divided by 2, and its `overY` (Overfling range) is the [Rect.height] of
     * [mContentRect] divided by 2. Finally we call the [ViewCompat.postInvalidateOnAnimation] method
     * to cause an invalidate of our view to happen on the next animation time step.
     *
     * @param velocityX The velocity of this fling measured in pixels per second
     * along the x axis.
     * @param velocityY The velocity of this fling measured in pixels per second
     * along the y axis.
     */
    private fun fling(velocityX: Int, velocityY: Int) {
        releaseEdgeEffects()
        // Flings use math in pixels (as opposed to math based on the viewport).
        computeScrollSurfaceSize(mSurfaceSizeBuffer)
        mScrollerStartViewport.set(mCurrentViewport!!)
        val startX: Int =
            (mSurfaceSizeBuffer.x * (mScrollerStartViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN)).toInt()
        val startY: Int =
            (mSurfaceSizeBuffer.y * (AXIS_Y_MAX - mScrollerStartViewport.bottom) / (AXIS_Y_MAX - AXIS_Y_MIN)).toInt()
        mScroller.forceFinished(true)
        mScroller.fling(
            startX,
            startY,
            velocityX,
            velocityY,
            0, mSurfaceSizeBuffer.x - mContentRect.width(),
            0, mSurfaceSizeBuffer.y - mContentRect.height(),
            mContentRect.width() / 2,
            mContentRect.height() / 2
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Computes the current scrollable surface size, in pixels. For example, if the entire chart
     * area is visible, this is simply the current size of [mContentRect]. If the chart
     * is zoomed in 200% in both directions, the returned size will be twice as large horizontally
     * and vertically. We set the [Point.x] property of [Point] parameter [out] to the [Rect.width]
     * of [Rect] field [mContentRect] times the quantity [AXIS_X_MAX] (1f) minus [AXIS_X_MIN] (-1f)
     * all divided by the [RectF.width] property of of [RectF] field [mCurrentViewport], and we set
     * the [Point.y] property of [Point] parameter [out] to the [Rect.height] property of [Rect]
     * field [mContentRect] times the quantity [AXIS_Y_MAX] (1f) minus [AXIS_Y_MIN] (-1f) all
     * divided by the [RectF.height] property of of [RectF] field [mCurrentViewport]
     *
     * @param out the [Point] which we should use to return the current scrollable surface size,
     * in pixels.
     */
    private fun computeScrollSurfaceSize(out: Point) {
        out.set(
            (mContentRect.width() * (AXIS_X_MAX - AXIS_X_MIN) / mCurrentViewport!!.width()).toInt(),
            (mContentRect.height() * (AXIS_Y_MAX - AXIS_Y_MIN) / mCurrentViewport!!.height()).toInt()
        )
    }

    /**
     * Called by a parent to request that a child update its values for `mScrollX` and `mScrollY`
     * if necessary. This will typically be done if the child is animating a scroll using a
     * [OverScroller]. First we call our super's implementation of `computeScroll`, then we
     * initialize our [Boolean] variable `var needsInvalidate` to `false` (if our code does
     * anything that requires a re-draw of our view, it will be set to `true` and the method
     * [ViewCompat.postInvalidateOnAnimation] will be called before returning). Then we call the
     * [OverScroller.computeScrollOffset] method of [OverScroller] field [mScroller] and if it
     * returns `true` indicating that the current animation is not yet finished we:
     *  - Call our [computeScrollSurfaceSize] method to have it compute the current scrollable
     *  surface size and store that value in our [Point] field [mSurfaceSizeBuffer].
     *  - We initialize our [Int] variable `val currX` to the value returned by the method
     *  [OverScroller.getCurrX] of [mScroller] (kotlin `currX` property), and our [Int] variable
     *  `val currY` to the value returned by the method [OverScroller.getCurrY] of [mScroller]
     *  (kotlin `currY` property).
     *  - We initialize our [Boolean] variable `val canScrollX` to `true` if the [RectF.left]
     *  property of [RectF] field [mCurrentViewport] is greater than [AXIS_X_MIN], or if the
     *  [RectF.right] property of [RectF] field [mCurrentViewport] is less than [AXIS_X_MAX]
     *  - We initialize our [Boolean] variable `val canScrollY` to `true` if the [RectF.top]
     *  property of [RectF] field [mCurrentViewport] is greater than [AXIS_Y_MIN], or if the
     *  [RectF.bottom] property of [RectF] field [mCurrentViewport] is less than [AXIS_Y_MAX].
     *  - Then if `canScrollX` is `true` and `currX` is less than 0, and the method
     *  [EdgeEffectCompat.isFinished] of [EdgeEffectCompat] field [mEdgeEffectLeft] and our
     *  [Boolean] field [mEdgeEffectLeftActive] is `false` (the [EdgeEffectCompat] field
     *  [mEdgeEffectLeft] is not running) we call the [EdgeEffectCompat.onAbsorb] method of
     *  [mEdgeEffectLeft] with the value that the [OverScrollerCompat.getCurrVelocity] method
     *  returns for [OverScroller] field [mScroller]. We then set [Boolean] field
     *  [mEdgeEffectLeftActive] and [Boolean] variable `needsInvalidate` both to `true`. If the
     *  conditions of the `if` are not met we check if `canScrollX` is `true` and `currX` is
     *  greater than the [Point.x] property of [mSurfaceSizeBuffer] minus the [Rect.width] of
     *  [Rect] field [mContentRect] and the [EdgeEffectCompat.isFinished] of [EdgeEffectCompat]
     *  field [mEdgeEffectRight] and our [Boolean] field [mEdgeEffectRightActive] is `false`,
     *  we call the [EdgeEffectCompat.onAbsorb] method of [mEdgeEffectRight] with the value that
     *  the [OverScrollerCompat.getCurrVelocity] method returns for [OverScroller] field [mScroller].
     *  We then set [Boolean] field [mEdgeEffectRightActive] and [Boolean] variable `needsInvalidate`
     *  both to `true`.
     *
     * Next we deal with Y axis scrolling:
     *   - If `canScrollY` is `true` and `currY` is less than 0, and the method
     *   [EdgeEffectCompat.isFinished] of [EdgeEffectCompat] field [mEdgeEffectTop] and our
     *   [Boolean] field [mEdgeEffectTopActive] is `false` (the [EdgeEffectCompat] field
     *   [mEdgeEffectTop] is not running) we call the [EdgeEffectCompat.onAbsorb] method of
     *   [mEdgeEffectTop] with the value that the [OverScrollerCompat.getCurrVelocity] method
     *   returns for [OverScroller] field [mScroller]. We then set [Boolean] field
     *   [mEdgeEffectTopActive] and [Boolean] variable `needsInvalidate` both to `true`.  If the
     *   conditions of the `if` are not met we check if `canScrollY` is `true` and `currY` is greater
     *   than the [Point.y] property of [mSurfaceSizeBuffer] minus the [Rect.height] of [Rect] field
     *   [mContentRect] and the [EdgeEffectCompat.isFinished] of [EdgeEffectCompat] field
     *   [mEdgeEffectBottom] and our [Boolean] field [mEdgeEffectBottomActive] is `false`, we call
     *   the [EdgeEffectCompat.onAbsorb] method of [mEdgeEffectBottom] with the value that the
     *   [OverScrollerCompat.getCurrVelocity] method returns for [OverScroller] field [mScroller].
     *   We then set [Boolean] field [mEdgeEffectBottomActive] and [Boolean] variable `needsInvalidate`
     *   both to `true`.
     *   - Having dealt with both X and Y scrolling we initialize our [Float] variable `val currXRange`
     *   to [AXIS_X_MIN] plus the quantity [AXIS_X_MAX] minus [AXIS_X_MIN] times `currX` divided by
     *   the [Point.x] property of [Point] field [mSurfaceSizeBuffer], and we initialize our [Float]
     *   variable `val currYRange` to [AXIS_Y_MAX] plus the quantity [AXIS_Y_MAX] minus [AXIS_Y_MIN]
     *   times `currY` divided by the [Point.y] property of [Point] field [mSurfaceSizeBuffer]. We
     *   then call our [setViewportBottomLeft] method with `currXRange` as its `x`
     *
     * Having dealt with scrolling if necessary, we next call the [Zoomer.computeZoom] method of our
     * [Zoomer] field [mZoomer] and if it returns `true` (indicating that the zoom is still active)
     * we perform the zoom since a zoom is in progress (either programmatically or via double-touch):
     *  - We initialize our [Float] variable `val newWidth` to the quantity 1f minus the
     *  [Zoomer.currZoom] property of [mZoomer] times the [RectF.width] of [RectF] field
     *  [mScrollerStartViewport], and we initialize our [Float] variable `val newHeight` to the
     *  quantity 1f minus the [Zoomer.currZoom] property of [mZoomer] times the [RectF.height] of
     *  [RectF] field [mScrollerStartViewport].
     *  - We initialize our [Float] variable `val pointWithinViewportX` to the quantity of the
     *  [PointF.x] property of [PointF] field [mZoomFocalPoint] minus the [RectF.left] property of
     *  [RectF] field [mScrollerStartViewport] all divided by the [RectF.width] of
     *  [mScrollerStartViewport], and we initialize our [Float] variable `val pointWithinViewportY`
     *  to the quantity of the [PointF.y] property of [PointF] field [mZoomFocalPoint] minus the
     *  [RectF.top] property of [RectF] field [mScrollerStartViewport] all divided by the
     *  [RectF.height] of [mScrollerStartViewport].
     *  - We then call the [RectF.set] method of [mCurrentViewport] to set its `left` to the
     *  [PointF.x] property of [PointF] field [mZoomFocalPoint] minus `newWidth` times
     *  `pointWithinViewportX`, to set its `top` to the [PointF.y] property of [PointF] field
     *  [mZoomFocalPoint] minus `newHeight` times `pointWithinViewportY`, to set its `right` to
     *  the [PointF.x] property of [PointF] field [mZoomFocalPoint] plus `newWidth` times the
     *  quantity of 1 minus `pointWithinViewportX`, to set its `bottom` to the [PointF.y] property
     *  of [PointF] field [mZoomFocalPoint] plus `newHeight` times the quantity 1 minus
     *  `pointWithinViewportY`.
     *  - We then call our [constrainViewport] method to ensure that current viewport that we just
     *  set [mCurrentViewport] to is inside the viewport extremes defined by [AXIS_X_MIN],
     *  [AXIS_X_MAX], [AXIS_Y_MIN] and [AXIS_Y_MAX], then we set our [Boolean] variable
     *  `needsInvalidate` to `true`.
     *
     * Before returning we check if `needsInvalidate` is `true` and if it is we call the method
     * [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate of `this` [View] to
     * happen on the next animation time step, typically the next display frame.
     */
    override fun computeScroll() {
        super.computeScroll()
        var needsInvalidate = false
        if (mScroller.computeScrollOffset()) {
            // The scroller isn't finished, meaning a fling or programmatic pan operation is
            // currently active.
            computeScrollSurfaceSize(out = mSurfaceSizeBuffer)
            val currX: Int = mScroller.currX
            val currY: Int = mScroller.currY
            val canScrollX: Boolean = (mCurrentViewport!!.left > AXIS_X_MIN
                || mCurrentViewport!!.right < AXIS_X_MAX)
            val canScrollY: Boolean = (mCurrentViewport!!.top > AXIS_Y_MIN
                || mCurrentViewport!!.bottom < AXIS_Y_MAX)
            if (canScrollX && currX < 0 && mEdgeEffectLeft.isFinished
                && !mEdgeEffectLeftActive
            ) {
                mEdgeEffectLeft.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectLeftActive = true
                needsInvalidate = true
            } else if (canScrollX
                && currX > (mSurfaceSizeBuffer.x - mContentRect.width())
                && mEdgeEffectRight.isFinished && !mEdgeEffectRightActive
            ) {
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
                && !mEdgeEffectBottomActive
            ) {
                mEdgeEffectBottom.onAbsorb(OverScrollerCompat.getCurrVelocity(mScroller).toInt())
                mEdgeEffectBottomActive = true
                needsInvalidate = true
            }
            val currXRange: Float =
                AXIS_X_MIN + (AXIS_X_MAX - AXIS_X_MIN) * currX / mSurfaceSizeBuffer.x
            val currYRange: Float =
                AXIS_Y_MAX - (AXIS_Y_MAX - AXIS_Y_MIN) * currY / mSurfaceSizeBuffer.y
            setViewportBottomLeft(x = currXRange, y = currYRange)
        }
        if (mZoomer.computeZoom()) {
            // Performs the zoom since a zoom is in progress (either programmatically or via
            // double-touch).
            val newWidth: Float = (1f - mZoomer.currZoom) * mScrollerStartViewport.width()
            val newHeight: Float = (1f - mZoomer.currZoom) * mScrollerStartViewport.height()
            val pointWithinViewportX: Float = ((mZoomFocalPoint.x - mScrollerStartViewport.left)
                / mScrollerStartViewport.width())
            val pointWithinViewportY: Float = ((mZoomFocalPoint.y - mScrollerStartViewport.top)
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
     * Sets the current viewport (defined by [mCurrentViewport]) to the given X and Y positions.
     * Note that the Y value represents the topmost pixel position, and thus the bottom of the
     * [mCurrentViewport] rectangle. For more details on why top and bottom are flipped, see
     * [mCurrentViewport]. First we initialize our [Float] variable `var xLocal` to our [Float]
     * parameter [x], and our [Float] variable `var yLocal` to our [Float] parameter [y]. We
     * initialize our [Float] variable `val curWidth` to the current [RectF.width] of [RectF] field
     * [mCurrentViewport], and our [Float] variable `val curHeight` to its current [RectF.height].
     * We set `xLocal` to the max of [AXIS_X_MIN] and the min of `xLocal` and [AXIS_X_MAX] minus
     * `curWidth`. We set `yLocal` to the max of the quantity [AXIS_Y_MIN] plus `curHeight` and
     * the min of `yLocal` and [AXIS_Y_MAX]. Then we use the [RectF.set] method of [mCurrentViewport]
     * to set its [RectF.left] to `xLocal`, its [RectF.top] to `yLocal` minus `curHeight`, its
     * [RectF.right] to `xLocal` plus `curWidth`, and its [RectF.bottom] to `yLocal`. Then we call
     * the [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate of `this` [View] to
     * happen on the next animation time step, typically the next display frame.
     *
     * @param x Used to set the [RectF.left] of [RectF] field [mCurrentViewport].
     * @param y Used to set the [RectF.bottom] of [RectF] field [mCurrentViewport].
     */
    private fun setViewportBottomLeft(x: Float, y: Float) {
        /**
         * Constrains within the scroll range. The scroll range is simply the viewport extremes
         * (AXIS_X_MAX, etc.) minus the viewport size. For example, if the extrema were 0 and 10,
         * and the viewport size was 2, the scroll range would be 0 to 8.
         */
        var xLocal: Float = x
        var yLocal: Float = y
        val curWidth: Float = mCurrentViewport!!.width()
        val curHeight: Float = mCurrentViewport!!.height()
        xLocal = Math.max(AXIS_X_MIN, Math.min(xLocal, AXIS_X_MAX - curWidth))
        yLocal = Math.max(AXIS_Y_MIN + curHeight, Math.min(yLocal, AXIS_Y_MAX))
        mCurrentViewport!!.set(
            /* left = */ xLocal,
            /* top = */ yLocal - curHeight,
            /* right = */ xLocal + curWidth,
            /* bottom = */ yLocal
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods for programmatically changing the viewport
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a copy of [RectF] field [mCurrentViewport] (the current viewport, visible extremes
     * for the chart domain and range). Its "setter" sets the chart's current viewport, calls our
     * [constrainViewport] method to ensure that the new current viewport is inside the viewport
     * extremes defined by [AXIS_X_MIN], [AXIS_X_MAX], [AXIS_Y_MIN] and [AXIS_Y_MAX] and then calls
     * the [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate of `this` [View] to
     * happen on the next animation time step, typically the next display frame.
     */
    var currentViewport: RectF?
        get() = RectF(mCurrentViewport)
        set(viewport) {
            mCurrentViewport = viewport
            constrainViewport()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Smoothly zooms the chart in one step. First we use the [RectF.set] method of [RectF] field
     * [mScrollerStartViewport] to set it to [RectF] field [mCurrentViewport] (for use by our
     * [Zoomer] field [mZoomer]). The we call the [Zoomer.forceFinished] method of [mZoomer] with
     * `true` to force any ongoing zoom animation to "finish". We call the [Zoomer.startZoom] method
     * of [mZoomer] with [ZOOM_AMOUNT] (0.25f) to start a zoom from 1.0 to 1.25 in motion. Next we
     * use the [PointF.set] method of [PointF] field [mZoomFocalPoint] to set the focal point of the
     * zoom to the center point of [mCurrentViewport]: X coordinate the [RectF.right] of [RectF]
     * field [mCurrentViewport] plus its [RectF.left] all over 2, and Y coordinate the [RectF.bottom]
     * of [mCurrentViewport] plus its [RectF.top] all over 2.
     *
     * Finally we call the [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate of
     * `this` [View] to happen on the next animation time step, typically the next display frame.
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
     * Smoothly zooms the chart out one step. First we use the [RectF.set] method of [RectF] field
     * [mScrollerStartViewport] to set it to [RectF] field [mCurrentViewport] (for use by our
     * [Zoomer] field [mZoomer]). The we call the [Zoomer.forceFinished] method of [mZoomer] with
     * `true` to force any ongoing zoom animation to "finish". We call the [Zoomer.startZoom] method
     * of [mZoomer] with minus [ZOOM_AMOUNT] (-0.25f) to start a zoom from 1.0 to 0.75 in motion.
     * Next we use the [PointF.set] method of [PointF] field [mZoomFocalPoint] to set the focal
     * point of the zoom to the center point of [mCurrentViewport]: X coordinate the [RectF.right]
     * of [RectF] field [mCurrentViewport] plus its [RectF.left] all over 2, and Y coordinate the
     * [RectF.bottom] of [mCurrentViewport] plus its [RectF.top] all over 2.
     *
     * Finally we call the [ViewCompat.postInvalidateOnAnimation] method to cause an invalidate of
     * `this` [View] to happen on the next animation time step, typically the next display frame.
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
     * Smoothly pans the chart left one step. We just call our [fling] method with its `velocityX`
     * argument minus [PAN_VELOCITY_FACTOR] (2f) times our [View.getWidth] (kotlin `width` property)
     * and its `velocityY` argument 0.
     */
    fun panLeft() {
        fling(velocityX = (-PAN_VELOCITY_FACTOR * width).toInt(), velocityY = 0)
    }

    /**
     * Smoothly pans the chart right one step. We just call our [fling] method with its `velocityX`
     * argument [PAN_VELOCITY_FACTOR] (2f) times our [View.getWidth] (kotlin `width` property) and
     * its `velocityY` argument 0.
     */
    fun panRight() {
        fling(velocityX = (PAN_VELOCITY_FACTOR * width).toInt(), velocityY = 0)
    }

    /**
     * Smoothly pans the chart up one step. We just call our [fling] method with its `velocityX`
     * argument 0 and its `velocityY` argument minus [PAN_VELOCITY_FACTOR] (2f) times our
     * [View.getHeight] (kotlin `height` property).
     */
    fun panUp() {
        fling(velocityX = 0, velocityY = (-PAN_VELOCITY_FACTOR * height).toInt())
    }

    /**
     * Smoothly pans the chart down one step. We just call our [fling] method with its `velocityX`
     * argument 0 and its `velocityY` argument [PAN_VELOCITY_FACTOR] (2f) times our [View.getHeight]
     * (kotlin `height` property).
     */
    fun panDown() {
        fling(0, (PAN_VELOCITY_FACTOR * height).toInt())
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods related to custom attributes
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Unused public access to our private [Float] field [mLabelTextSize]. Notice the clever side
     * effects caused by the `set` method.
     */
    var labelTextSize: Float
        get() = mLabelTextSize
        set(labelTextSize) {
            mLabelTextSize = labelTextSize
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Unused public access to our private [Float] field [mLabelTextColor]. Notice the clever side
     * effects caused by the `set` method.
     */
    var labelTextColor: Int
        get() = mLabelTextColor
        set(labelTextColor) {
            mLabelTextColor = labelTextColor
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Unused public access to our private [Float] field [mGridThickness]. Notice the clever side
     * effects caused by the `set` method.
     */
    var gridThickness: Float
        get() = mGridThickness
        set(gridThickness) {
            mGridThickness = gridThickness
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Unused public access to our private [Float] field [mGridColor]. Notice the clever side
     * effects caused by the `set` method.
     */
    var gridColor: Int
        get() = mGridColor
        set(gridColor) {
            mGridColor = gridColor
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Unused public access to our private [Float] field [mAxisThickness]. Notice the clever side
     * effects caused by the `set` method.
     */
    var axisThickness: Float
        get() = mAxisThickness
        set(axisThickness) {
            mAxisThickness = axisThickness
            initPaints()
            ViewCompat.postInvalidateOnAnimation(this)
        }

    /**
     * Unused public access to our private [Float] field [mAxisColor]. Notice the clever side
     * effects caused by the `set` method.
     */
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

    /**
     * Hook allowing a view to generate a representation of its internal state that can later be
     * used to create a new instance with that same state. This state should only contain information
     * that is not persistent or can not be reconstructed later. For example, you will never store
     * your current position on screen because that will be computed again when a new instance of
     * the view is placed in its view hierarchy. Some examples of things you may store here: the
     * current cursor position in a text view (but usually not the text itself since that is stored
     * in a content provider or other persistent storage), the currently selected item in a list
     * view. We initialize our [Parcelable] variable `val superState` to the [Parcelable] returned
     * by our super's implementation of `onSaveInstanceState`, then we initialize our [SavedState]
     * variable `val ss` to a new instance constructed using `superState` as its `superState`
     * argument. We set the [SavedState.viewport] property of `ss` to our [RectF] field
     * [mCurrentViewport] and return `ss` to our caller.
     *
     * @return a [Parcelable] object containing the view's current dynamic state, or `null` if there
     * is nothing interesting to save.
     */
    public override fun onSaveInstanceState(): Parcelable? {
        val superState: Parcelable? = super.onSaveInstanceState()
        val ss = SavedState(superState = superState)
        ss.viewport = mCurrentViewport
        return ss
    }

    /**
     * Hook allowing a view to re-apply a representation of its internal state that had previously
     * been generated by [onSaveInstanceState]. This function will never be called with a null
     * state. If our [Parcelable] parameter [state] is not an instance of [SavedState] we just call
     * our super's implementation of `onRestoreInstanceState` with our [state] parameter and return.
     * Otherwise we call our super's implementation of `onRestoreInstanceState` with the value
     * returned the [SavedState.getSuperState] method of [state] (kotlin `superState` property),
     * and set our [RectF] field [mCurrentViewport] to the [SavedState.viewport] property of [state].
     *
     * @param state – The frozen state that had previously been returned by [onSaveInstanceState].
     */
    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        mCurrentViewport = state.viewport
    }

    /**
     * Persistent state that is saved by [InteractiveLineGraphView].
     */
    class SavedState : BaseSavedState {
        /**
         * This is where our [onSaveInstanceState] override saves our [RectF] field [mCurrentViewport]
         */
        var viewport: RectF? = null

        /**
         * Our constructor. We just call our `BaseSavedState` super with our [Parcelable] parameter
         * [superState].
         *
         * @param superState the [Parcelable] that is passed to our [onSaveInstanceState] override.
         */
        constructor(superState: Parcelable?) : super(superState)

        /**
         * Flatten this object in to a Parcel. First we call our super's implementation of
         * `writeToParcel`. Then we call the [Parcel.writeFloat] method of our [Parcel] parameter
         * [out] four times to write the [RectF.left], [RectF.top], [RectF.right], and [RectF.bottom]
         * of [RectF] field [viewport] to the [Parcel].
         *
         * @param out – The Parcel in which the object should be written.
         * @param flags – Additional flags about how the object should be written. May be 0 or
         * [Parcelable.PARCELABLE_WRITE_RETURN_VALUE].
         */
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(viewport!!.left)
            out.writeFloat(viewport!!.top)
            out.writeFloat(viewport!!.right)
            out.writeFloat(viewport!!.bottom)
        }

        /**
         * Returns a string representation of the object.
         *
         * @return a [String] displaying the contents of our [RectF] field [viewport], and other
         * text of possible interest for debugging.
         */
        override fun toString(): String {
            return ("InteractiveLineGraphView.SavedState{"
                + Integer.toHexString(System.identityHashCode(this))
                + " viewport=" + viewport.toString() + "}")
        }

        /**
         * Constructor used when reading from a [Parcel]. We call our super's constructor with our
         * [Parcel] parameter [inParcel], then we initialize our [RectF] field [viewport] with a
         * new instance constructed using the [Parcel.readFloat] method of [inParcel] four times to
         * read its `left`, `top`, `right`, and `bottom` arguments from the [Parcel].
         *
         * @param inParcel the [Parcel] to read from.
         */
        internal constructor(inParcel: Parcel) : super(inParcel) {
            viewport = RectF(
                /* left = */ inParcel.readFloat(),
                /* top = */ inParcel.readFloat(),
                /* right = */ inParcel.readFloat(),
                /* bottom = */ inParcel.readFloat()
            )
        }

        companion object {
            /**
             * Interface that must be implemented and provided as a public [CREATOR] field that
             * generates instances of your [Parcelable] class from a [Parcel].
             */
            @Suppress("RedundantNullableReturnType")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> = ParcelableCompat.newCreator(
                object : ParcelableCompatCreatorCallbacks<SavedState?> {
                    /**
                     * Create a new instance of the Parcelable class, instantiating it from the given
                     * Parcel whose data had previously been written by [Parcelable.writeToParcel].
                     * We just return a new instance of [SavedState] constructed using our [Parcel]
                     * parameter [inParcel] as its `inParcel` argument.
                     *
                     * @param inParcel – The [Parcel] to read the object's data from.
                     * @return a new instance of the [Parcelable] class.
                     */
                    override fun createFromParcel(
                        inParcel: Parcel,
                        loader: ClassLoader
                    ): SavedState? {
                        return SavedState(inParcel = inParcel)
                    }

                    /**
                     * Create a new array of the Parcelable class.
                     *
                     * @param size – Size of the array.
                     * @return an array of the [Parcelable] class, with every entry initialized to
                     * `null`.
                     */
                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
            )
        }
    }

    /**
     * A simple class representing axis label values.
     *
     * @see [computeAxisStops]
     */
    private class AxisStops {
        /**
         * Coordinates on the Axis where a label will be drawn.
         */
        var stops: FloatArray = floatArrayOf()

        /**
         * Number of elements in our [FloatArray] property [stops]
         */
        var numStops: Int = 0

        /**
         * Number of decimals used for the label.
         */
        var decimals: Int = 0
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "InteractiveLineGraphView"

        /**
         * The number of individual points (samples) in the chart series to draw onscreen.
         */
        private const val DRAW_STEPS = 30

        /**
         * Initial fling velocity for pan operations, in screen widths (or heights) per second.
         *
         * @see panLeft
         * @see panRight
         * @see panUp
         * @see panDown
         */
        private const val PAN_VELOCITY_FACTOR = 2f

        /**
         * The scaling factor for a single zoom 'step'.
         *
         * @see zoomIn
         * @see zoomOut
         */
        private const val ZOOM_AMOUNT = 0.25f

        /**
         * Viewport extremes. See [mCurrentViewport] for a discussion of the viewport.
         */
        private const val AXIS_X_MIN = -1f
        private const val AXIS_X_MAX = 1f
        private const val AXIS_Y_MIN = -1f
        private const val AXIS_Y_MAX = 1f

        /**
         * The simple math function Y = fun(X) to draw on the chart.
         *
         * @param x The X value
         * @return The Y value
         */
        private fun fofX(x: Float): Float {
            return Math.pow(x.toDouble(), 3.0).toFloat() - x / 4
        }

        /**
         * Rounds the given number to the given number of significant digits. Based on an answer on
         * [Stack Overflow](http://stackoverflow.com/questions/202302).
         *
         * @param num the number to round to one significant digit.
         * @return a [Float] which is the result of rounding our [Double] parameter [num] to one
         * significant digit.
         */
        private fun roundToOneSignificantFigure(num: Double): Float {
            val d = Math.ceil(Math.log10(if (num < 0) -num else num).toFloat().toDouble()).toFloat()
            val power = 1 - d.toInt()
            val magnitude = Math.pow(10.0, power.toDouble()).toFloat()
            val shifted = Math.round(num * magnitude)
            return shifted / magnitude
        }

        /**
         * Constants used by our [formatFloat] method to format numbers into strings.
         */
        private val POW10: IntArray = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000)

        /**
         * Formats a float value to the given number of decimals. Returns the length of the string.
         * The string begins at out.length - (return value).
         *
         * @param out the [CharArray] into which we write the formatted string of [Float] parameter
         * [value]
         * @param value the [Float] that we should format into its string value
         * @param digits the number of digits to use in the string.
         */
        private fun formatFloat(out: CharArray, value: Float, digits: Int): Int {
            var valueLocal = value
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
                @Suppress("AssignedValueIsNeverRead")
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
            val range: Double = (stop - start).toDouble()
            if (steps == 0 || range <= 0) {
                outStops.stops = floatArrayOf()
                outStops.numStops = 0
                return
            }
            val rawInterval: Double = range / steps
            var interval: Double = roundToOneSignificantFigure(rawInterval).toDouble()
            val intervalMagnitude: Double = Math.pow(10.0, Math.log10(interval).toInt().toDouble())
            val intervalSigDigit: Int = (interval / intervalMagnitude).toInt()
            if (intervalSigDigit > 5) {
                // Use one order of magnitude higher, to avoid intervals like 0.9 or 90
                interval = Math.floor(10 * intervalMagnitude)
            }
            val first: Double = Math.ceil(start / interval) * interval
            val last: Double = Math.nextUp(Math.floor(stop / interval) * interval)
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
