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
@file:Suppress("KDocUnresolvedReference", "ReplaceJavaStaticMethodWithKotlinAnalog", "ReplaceNotNullAssertionWithElvisReturn", "JoinDeclarationAndAssignment", "ProtectedInFinal", "MemberVisibilityCanBePrivate")

package com.example.android.basicmultitouch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import androidx.core.util.size

/**
 * View that shows touch events and their history. This view demonstrates the use of [onTouchEvent]
 * and [MotionEvent]'s to keep track of touch pointers across events.
 *
 * Constructor that is called when inflating a view from XML. This is called when a view is being
 * constructed from an XML file, supplying attributes that were specified in the XML file. This
 * version uses a default style of 0, so the only attribute values applied are those in the
 * [Context]'s Theme and the given [AttributeSet]. First we call our super's constructor, then in
 * our init block we initialize our [SparseArray] of [TouchHistory] field [mTouches] with a new
 * instance whose initial capacity is 10. Also in our init block we call our method [initialisePaint]
 * to set up the required [Paint] objects for the screen density of this device.
 *
 * @param context The [Context] the view is running in, through which it can access the current theme,
 * resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
class TouchDisplayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * Holds data for active touch pointer IDs
     */
    private val mTouches: SparseArray<TouchHistory>

    /**
     * `true` if we are tracking at least one finger, `false` if there are no fingers touching.
     */
    private var mHasTouch = false

    /**
     * Holds data related to a touch pointer, including its current position, pressure and
     * historical positions. Objects are allocated through an object pool using `obtain()` and
     * `recycle()` to reuse existing objects.
     */
    class TouchHistory {
        /**
         * Current X coordinate of the touch we represent
         */
        var x: Float = 0f

        /**
         * Current Y coordinate of the touch we represent
         */
        var y: Float = 0f

        /**
         * Current pressure of the touch we represent
         */
        var pressure: Float = 0f

        /**
         * Label of the touch we represent, of the form "id: " with the pointer ID
         * of the touch appended
         */
        var label: String? = null

        /**
         * current position in history array
         */
        var historyIndex: Int = 0

        /**
         * Number of points in our [Array] of [PointF] field [history].
         */
        var historyCount: Int = 0

        /**
         * [Array] of pointer position history (can hold [HISTORY_COUNT] (20) [PointF]'s)
         */
        var history: Array<PointF?> = arrayOfNulls(HISTORY_COUNT)

        /**
         * Part of our constructor. We initialize all [HISTORY_COUNT] entries in the [Array] of
         * [PointF] field [history] with a new instance of [PointF].
         */
        init {

            // initialise history array
            for (i in 0 until HISTORY_COUNT) {
                history[i] = PointF()
            }
        }

        /**
         * Setter for our `x`, `y`, and `pressure` fields.
         *
         * @param x X coordinate we are to hold
         * @param y Y coordinate we are to hold
         * @param pressure pressure we are to hold
         */
        fun setTouch(x: Float, y: Float, pressure: Float) {
            this.x = x
            this.y = y
            this.pressure = pressure
        }

        /**
         * Recycles this instance back to our [SimplePool] of [TouchHistory] field [sPool] for reuse.
         */
        fun recycle() {
            historyIndex = 0
            historyCount = 0
            sPool.release(this)
        }

        /**
         * Add a point to the [history] of this [TouchHistory]. Overwrites oldest point if the
         * maximum number of historical points is already stored. We initialize [PointF] variable
         * `val p` with the [PointF] at index [historyIndex] of our array of [PointF] pointer
         * position history field [history], then set its `x` field to our parameter `x` and its
         * `y` field to our parameter `y`. We increment [historyIndex] modulo the size of [history]
         * (circular buffer style), then if [historyCount] is less than [HISTORY_COUNT] we increment
         * it.
         *
         * @param x x coordinate
         * @param y y coordinate
         */
        fun addHistory(x: Float, y: Float) {
            val p = history[historyIndex]
            p!!.x = x
            p.y = y
            historyIndex = (historyIndex + 1) % history.size
            if (historyCount < HISTORY_COUNT) {
                historyCount++
            }
        }

        companion object {
            /**
             * number of historical points to store in a [TouchHistory]
             */
            const val HISTORY_COUNT: Int = 20

            /**
             * Maximum number of historical [TouchHistory] positions we remember
             */
            private const val MAX_POOL_SIZE = 10

            /**
             * Our [Pool] of [MAX_POOL_SIZE] instances of [TouchHistory].
             */
            private val sPool = Pools.SimplePool<TouchHistory>(MAX_POOL_SIZE)

            /**
             * Returns a [TouchHistory] instance initialized to hold our parameters, recycling from
             * our [SimplePool] of [TouchHistory] field [sPool] if possible. We initialize [TouchHistory]
             * `var data` by attempting to acquire an existing [TouchHistory] from [sPool]. If this
             * is `null` we set `data` to a new instance of [TouchHistory]. We then call the
             * [TouchHistory.setTouch] method of `data` to initialize it to hold all of our parameters.
             * Finally we return `data` to the caller.
             *
             * @param x X coordinate of the event
             * @param y Y coordinate of the event
             * @param pressure pressure of the event
             * @return A [TouchHistory] object holding our parameters.
             */
            fun obtain(x: Float, y: Float, pressure: Float): TouchHistory {
                var data = sPool.acquire()
                if (data == null) {
                    data = TouchHistory()
                }
                data.setTouch(x, y, pressure)
                return data
            }
        }
    }

    /**
     * Implement this method to handle touch screen motion events. We initialize [Int] variable
     * `val action` with the kind of action being performed by our [MotionEvent] parameter [event],
     * then switch on the results of masking off the ACTION_MASK bits of `action`:
     *
     *  * ACTION_DOWN: (first pressed gesture has started) We retrieve the pointer identifier
     *  associated with this event from pointer index 0 (the first pointer that is down) of
     *  [event] in order to initialize [Int] variable `val id`. We initialize [TouchHistory]
     *  variable `val data` with an instance (from the pool, or new) which has its x, y, and
     *  pressure components set to the values of the x, y, and pressure components from pointer
     *  index 0 of [event]. We then set the `label` field of `data` to a string formed by
     *  concatenating the string value of 0 to the string "id: ". We then store `data` under
     *  the index`id` to our [SparseArray] of [TouchHistory] field [mTouches], and set our
     *  [Boolean]  field [hasTouch] to `true`.
     *
     *  * ACTION_POINTER_DOWN: (A non-primary pointer has gone down, after an event for the primary
     *  pointer (ACTION_DOWN) has already been received) We initialize [Int] variable `val index`
     *  with the associated pointer index of [event], and [Int] variable `val id` with the pointer
     *  identifier associated the pointer data index `index` in this event (the [MotionEvent]
     *  object contains multiple pointers, so we need to find the index at which the data for
     *  the ACTION_POINTER_DOWN action is stored). We then initialize [TouchHistory] variable
     *  `val data` with an instance (from the pool, or new) which has its x, y, and pressure
     *  components set to the values of the x, y, and pressure components from pointer index
     *  `index` of [event]. We then set the `label` field of `data` to a string formed by
     *  concatenating the string value of `id` to the string "id: ". We then store `data` under
     *  the index `id` to our [SparseArray] of [TouchHistory] field [mTouches].
     *
     *  * ACTION_UP: (Final pointer has gone up and has ended the last pressed gesture) We retrieve
     *  the pointer identifier associated with this event from pointer index 0 (it will be the only
     *  event stored in the [MotionEvent] object) of [event] in order to initialize [Int] variable
     *  `val id`. We initialize [TouchHistory] variable `val data` with the [TouchHistory] object
     *  stored under key `id` in [mTouches], remove that entry from [mTouches] and then recycle
     *  `data`. We then set [mHasTouch] to `false`.
     *
     *  * ACTION_POINTER_UP: (A non-primary pointer has gone up and other pointers are still active)
     *  We initialize [Int] variable `val index` with the associated pointer index of [event], and
     *  [Int] variable `val id` with the pointer identifier associated the pointer data index `index`
     *  in this event (the [MotionEvent] object contains multiple pointers, so we need to find the
     *  index at which the data for the ACTION_POINTER_UP action is stored). We initialize
     *  [TouchHistory] variable `val data` with the [TouchHistory] object stored under key `id`
     *  in [mTouches], remove that entry from [mTouches], and recycle `data`.
     *
     *  * ACTION_MOVE: (A change event happened during a pressed gesture. (Between ACTION_DOWN and
     *  ACTION_UP or ACTION_POINTER_DOWN and ACTION_POINTER_UP)) We loop over `index` from 0 for
     *  the number of pointers of data contained in [event] (the value returned by the method
     *  [MotionEvent.getPointerCount] of [event], aka kotlin `pointerCount` property) initializing
     *  [Int] variable `val id` to the pointer identifier associated with the pointer data index
     *  `index`. We initialize [TouchHistory] variable `val data` by fetching the object stored at
     *  `id` in our [SparseArray] of [TouchHistory] field [mTouches]. We then add the previous X and
     *  Y coordinates of `data` (the `x` and `y` fields) to the history of `data`. Finally we update
     *  the `x`, `y`, and `pressure` fields of `data` with the X, Y, and pressure values for [event]
     *  for the pointer index `index` before looping back for the next pointer index.
     *
     * When done dealing with the [MotionEvent] we invalidate ourselves to trigger redraw on the UI
     * thread and return `true` to the caller to consume the event.
     *
     * @param event The motion event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action: Int = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // first pressed gesture has started
                /*
                 * Only one touch event is stored in the MotionEvent. Extract
                 * the pointer identifier of this touch from the first index
                 * within the MotionEvent object.
                 */
                val id = event.getPointerId(0)
                val data = TouchHistory.obtain(
                    x = event.getX(0),
                    y = event.getY(0),
                    pressure = event.getPressure(0)
                )
                data.label = "id: " + 0

                /*
                 * Store the data under its pointer identifier. The pointer
                 * number stays consistent for the duration of a gesture,
                 * accounting for other pointers going up or down.
                 */
                mTouches.put(id, data)
                mHasTouch = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                /*
                 * A non-primary pointer has gone down, after an event for the
                 * primary pointer (ACTION_DOWN) has already been received.
                 */

                /*
                 * The MotionEvent object contains multiple pointers. Need to
                 * extract the index at which the data for this particular event
                 * is stored.
                 */
                val index: Int = event.actionIndex
                val id: Int = event.getPointerId(index)
                val data: TouchHistory = TouchHistory.obtain(
                    event.getX(index),
                    event.getY(index),
                    event.getPressure(index)
                )
                data.label = "id: $id"

                /*
                 * Store the data under its pointer identifier. The index of
                 * this pointer can change over multiple events, but this
                 * pointer is always identified by the same identifier for this
                 * active gesture.
                 */
                mTouches.put(id, data)
            }

            MotionEvent.ACTION_UP -> {

                /*
                 * Final pointer has gone up and has ended the last pressed
                 * gesture.
                 */

                /*
                 * Extract the pointer identifier for the only event stored in
                 * the MotionEvent object and remove it from the list of active
                 * touches.
                 */
                val id: Int = event.getPointerId(0)
                val data: TouchHistory = mTouches[id]
                mTouches.remove(id)
                data.recycle()
                mHasTouch = false
            }

            MotionEvent.ACTION_POINTER_UP -> {

                /*
                 * A non-primary pointer has gone up and other pointers are
                 * still active.
                 */

                /*
                 * The MotionEvent object contains multiple pointers. Need to
                 * extract the index at which the data for this particular event
                 * is stored.
                 */
                val index: Int = event.actionIndex
                val id: Int = event.getPointerId(index)
                val data: TouchHistory = mTouches[id]
                mTouches.remove(id)
                data.recycle()
            }

            MotionEvent.ACTION_MOVE -> {

                /*
                 * A change event happened during a pressed gesture. (Between
                 * ACTION_DOWN and ACTION_UP or ACTION_POINTER_DOWN and
                 * ACTION_POINTER_UP)
                 */

                /*
                 * Loop through all active pointers contained within this event.
                 * Data for each pointer is stored in a MotionEvent at an index
                 * (starting from 0 up to the number of active pointers). This
                 * loop goes through each of these active pointers, extracts its
                 * data (position and pressure) and updates its stored data. A
                 * pointer is identified by its pointer number which stays
                 * constant across touch events as long as it remains active.
                 * This identifier is used to keep track of a pointer across
                 * events.
                 */
                var index = 0
                while (index < event.pointerCount) {

                    // get pointer id for data stored at this index
                    val id: Int = event.getPointerId(index)

                    // get the data stored externally about this pointer.
                    val data: TouchHistory = mTouches[id]

                    // add previous position to history and add new values
                    data.addHistory(data.x, data.y)
                    data.setTouch(event.getX(index), event.getY(index), event.getPressure(index))
                    index++
                }
            }
        }

        // trigger redraw on UI thread
        this.postInvalidate()
        return true
    }

    /**
     * Implement this to do your drawing. First we call our super's implementation of `onDraw`. If
     * our field [mHasTouch] is `true` (at least one finger is touching the screen) we fill the
     * entire bitmap of the [Canvas] parameter [canvas]  with the color [BACKGROUND_ACTIVE] (WHITE),
     * if `false` (no fingers touching) we draw a rectangle using [Paint] field [mBorderPaint]
     * around the entire [canvas]. [mBorderPaint] is an [INACTIVE_BORDER_COLOR] (0xFFffd060) colored
     * line (a light-ish orange) whose width is [mBorderWidth] pixels and the size of the rectangle
     * is inset by [mBorderWidth] pixels so the result is a frame around the entire canvas.
     *
     * Next we loop over [Int] variable `i` for all the entries in our [SparseArray] of [TouchHistory]
     * field [mTouches], setting [Int] variable `val id` to the pointer id of the key of the i'th
     * key-value mapping that the [SparseArray] stores, and setting [TouchHistory] variable `val data`
     * to the value from the i'th key-value mapping that the [SparseArray] stores. We then call our
     * method `drawCircle` to draw the data encapsulated by [TouchHistory] `data` to `canvas` using
     * the color index `id`.
     *
     * @param canvas the [Canvas] on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Canvas background color depends on whether there is an active touch
        if (mHasTouch) {
            canvas.drawColor(BACKGROUND_ACTIVE)
        } else {
            // draw inactive border
            canvas.drawRect(
                mBorderWidth,
                mBorderWidth,
                width - mBorderWidth,
                height - mBorderWidth,
                mBorderPaint
            )
        }

        // loop through all active touches and draw them
        for (i in 0 until mTouches.size) {

            // get the pointer id and associated data for this index
            val id: Int = mTouches.keyAt(i)
            val data: TouchHistory = mTouches.valueAt(i)

            // draw the data and its history to the canvas
            drawCircle(canvas, id, data)
        }
    }

    /**
     * calculated active touch radius in px
     */
    private var mCircleRadius = 0f

    /**
     * calculated historical circle radius in px
     */
    private var mCircleHistoricalRadius = 0f

    /**
     * [Paint] used to draw both active touch, and historical circles
     */
    private val mCirclePaint = Paint()

    /**
     * [Paint] used to draw the label next to the main circle
     */
    private val mTextPaint = Paint()

    /**
     * [Paint] used for inactive border
     */
    private val mBorderPaint = Paint()

    /**
     * calculated inactive border width in px
     */
    private var mBorderWidth = 0f


    init {
        // Allocate space for our SparseArray of touch events, indexed by touch id
        mTouches = SparseArray(10)
        initialisePaint()
    }

    /**
     * Sets up the required [Paint] objects for the screen density of this device. We initialize
     * [Float] variable `val density` with the logical density of the display (a scaling factor for
     * the Density Independent Pixel unit, where one DIP is one pixel on an approximately 160 dpi
     * screen). We use `density` to scale [CIRCLE_RADIUS_DP] to initialize our field [mCircleRadius],
     * and to scale [CIRCLE_HISTORICAL_RADIUS_DP] to initialize our field [mCircleHistoricalRadius].
     * We set the text size of [Paint] field [mTextPaint] to 27f and its color to [Color.BLACK]. We
     * use `density` to scale [INACTIVE_BORDER_DP] to initialize our field [mBorderWidth], set the
     * stroke width of [Paint] field [mBorderPaint] to [mBorderWidth], set its color to
     * [INACTIVE_BORDER_COLOR] (a light-ish orange), and set its style to STROKE (Geometry and text
     * drawn with this style will be stroked, respecting the stroke-related fields on the paint
     * without filling, so that the text of the [TextView] we share the [FrameLayout] with shows).
     */
    private fun initialisePaint() {

        // Calculate radiuses in px from dp based on screen density
        val density: Float = resources.displayMetrics.density
        mCircleRadius = CIRCLE_RADIUS_DP * density
        mCircleHistoricalRadius = CIRCLE_HISTORICAL_RADIUS_DP * density

        // Setup text paint for circle label
        mTextPaint.textSize = 27f
        mTextPaint.color = Color.BLACK

        // Setup paint for inactive border
        mBorderWidth = INACTIVE_BORDER_DP * density
        mBorderPaint.strokeWidth = mBorderWidth
        mBorderPaint.color = INACTIVE_BORDER_COLOR
        mBorderPaint.style = Paint.Style.STROKE
    }

    /**
     * Draws the data encapsulated by a [TouchDisplayView.TouchHistory] object to a canvas. A large
     * circle indicates the current position held by the [TouchDisplayView.TouchHistory] object,
     * while a smaller circle is drawn for each entry in its history. The size of the large circle
     * is scaled depending on its pressure, clamped to a maximum of `1.0`.
     *
     * We initialize [Int] variable `val color` by selecting a color based on the [id] (our parameter
     * [id] modulo the length of [COLORS] is used as an index into [COLORS]) and set the `color` of
     * [Paint] field [mCirclePaint] to it. We initialize [Float] variable `val pressure` to be the
     * minimum of the `pressure` field of `data` and 1.0f, and initialize [Float] variable
     * `val radius` to our field [mCircleRadius] multiplied by `pressure`. We then draw a circle on
     * [Canvas] parameter [canvas] using [mCirclePaint] as the [Paint] of radius `radius` using the
     * `x` field of `data` for the x coordinate of the center of the circle, and the `y` field of
     * `data` shifted by half `radius` up for the y coordinate of the center (no idea why). We then
     * set the alpha value of [mCirclePaint] to 125 to draw all historical points with a lower alpha
     * value. We then loop over [Int] variable `var j` for all of the [PointF] entries in the array
     * of [PointF] `history` field of `data` (the `historyCount` field of `data` is the number
     * currently in the array, but for extra safety we also limit ourselves to the total size of
     * the `history` field). We initialize [PointF] variable `val p` with the `j`'th entry in
     * the `data.history` array and draw a circle on [canvas] using [mCirclePaint] as the [Paint]
     * of radius [mCircleHistoricalRadius] located using the `x` field of `p` for the x coordinate
     * and the `y` field of `p` for the y coordinate.
     *
     * When done drawing the history circles we draw text using [Paint] field [mTextPaint] with the
     * `label` field as the text located using the `x` field of `data` offset to the right by
     * `radius` for the x coordinate, and the `y` field of `data` offset up by `radius` for the y
     * coordinate.
     *
     * @param canvas [Canvas] we are to draw on.
     * @param id     Index of the color we are to use, modulo the length of [COLORS]
     * @param data   [TouchHistory] of the circle we are to draw
     */
    protected fun drawCircle(canvas: Canvas, id: Int, data: TouchHistory) {
        // select the color based on the id
        val color: Int = COLORS[id % COLORS.size]
        mCirclePaint.color = color

        /*
         * Draw the circle, size scaled to its pressure. Pressure is clamped to
         * 1.0 max to ensure proper drawing. (Reported pressure values can
         * exceed 1.0, depending on the calibration of the touch screen).
         */
        val pressure: Float = Math.min(data.pressure, 1f)
        val radius: Float = pressure * mCircleRadius
        canvas.drawCircle(data.x, data.y - radius / 2f, radius, mCirclePaint)

        // draw all historical points with a lower alpha value
        mCirclePaint.alpha = 125
        var j = 0
        while (j < data.history.size && j < data.historyCount) {
            val p: PointF? = data.history[j]
            canvas.drawCircle(p!!.x, p.y, mCircleHistoricalRadius, mCirclePaint)
            j++
        }

        // draw its label next to the main circle
        canvas.drawText(data.label!!, data.x + radius, data.y - radius, mTextPaint)
    }

    companion object {
    /*
     * Below are only helper methods and variables required for drawing.
     */
        /**
         * radius of active touch circle in dp
         */
        private const val CIRCLE_RADIUS_DP = 75f

        /**
         * radius of historical circle in dp
         */
        private const val CIRCLE_HISTORICAL_RADIUS_DP = 7f

        /**
         * Background color of canvas when there is at least on finger touching the screen
         * ([mHasTouch] is `true`)
         */
        private const val BACKGROUND_ACTIVE = Color.WHITE

        /**
         * inactive border width in dp
         */
        private const val INACTIVE_BORDER_DP = 15f

        /**
         * inactive border color (a light-ish orange)
         */
        private const val INACTIVE_BORDER_COLOR = -0x2fa0

        /**
         * Colors used for the 10 possible touch circles, and their history traces.
         */
        val COLORS: IntArray = intArrayOf(
            -0xcc4a1b, -0x559934, -0x663400, -0x44cd, -0xbbbc,
            -0xff6634, -0x66cc34, -0x996700, -0x7800, -0x340000
        )
    }
}