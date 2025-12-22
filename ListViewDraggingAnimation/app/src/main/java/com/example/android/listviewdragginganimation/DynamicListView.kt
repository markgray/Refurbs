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
@file:Suppress(
    "ReplaceNotNullAssertionWithElvisReturn",
    "UNUSED_ANONYMOUS_PARAMETER",
    "MemberVisibilityCanBePrivate",
    "UnusedImport"
)

package com.example.android.listviewdragginganimation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView

/**
 * The [DynamicListView] is an extension of [ListView] that supports cell dragging and swapping.
 * This layout is in charge of positioning the hover cell in the correct location on the screen in
 * response to user touch events. It uses the position of the hover cell to determine when two cells
 * should be swapped. If two cells should be swapped, all the corresponding data set and layout
 * changes are handled here.
 *
 * If no cell is selected, all the touch events are passed down to the [ListView] and behave
 * normally. If one of the items in the [ListView] experiences a long press event, the contents of
 * its current visible state are captured as a bitmap and its visibility is set to INVISIBLE. A
 * hover cell is then created and added to this layout as an overlaying [BitmapDrawable] above the
 * [ListView]. Once the hover cell is translated some distance to signify an item swap, a data set
 * change accompanied by animation takes place. When the user releases the hover cell, it animates
 * into its corresponding position in the [ListView].
 *
 * When the hover cell is either above or below the bounds of the [ListView], this [ListView] also
 * scrolls on its own so as to reveal additional content.
 */
class DynamicListView : ListView {

    /**
     * Our dataset of cheese names.
     */
    var mCheeseList: ArrayList<String>? = null

    /**
     * Y coordinate of the current ACTION_MOVE event being processed by our [onTouchEvent] override.
     */
    private var mLastEventY: Int = -1

    /**
     * Y coordinate of the ACTION_DOWN event which initiated the current item drag, it is also
     * updated to [mLastEventY] whenever the drag causes a item cell to switch positions
     * in our [handleCellSwitch] method.
     */
    private var mDownY: Int = -1

    /**
     * X coordinate of the ACTION_DOWN event which initiated the current item drag, it is used in
     * a call to the [pointToPosition] method to find the position of the item which is long clicked
     * in our [OnItemLongClickListener.onItemLongClick] override.
     */
    private var mDownX: Int = -1

    /**
     * Total amount that the dragged cell has been dragged in the Y direction from its initial
     * position on the screen.
     */
    private var mTotalOffset: Int = 0

    /**
     * Flag to indicate that a cell has been long clicked and is now being dragged somewhere.
     */
    private var mCellIsMobile: Boolean = false

    /**
     * Flag indicating that this [ListView] is in a scrolling state invoked by the fact that
     * the hover cell is out of the bounds of the [ListView], it is set to the value returned
     * by [handleMobileCellScroll] method for the value in [Rect] field [mHoverCellCurrentBounds]
     * by our zero argument version of [handleMobileCellScroll], and set to `false` whenever the
     * current touch event suggests the scrolling state should end.
     */
    private var mIsMobileScrolling: Boolean = false

    /**
     * Number of pixels to smooth scroll the [ListView] when the hover item is at the edge of
     * the [ListView], it is calculated by dividing [SMOOTH_SCROLL_AMOUNT_AT_EDGE] (15) by the
     * logical density of the display.
     */
    private var mSmoothScrollAmountAtEdge: Int = 0

    /**
     * ID of the item that is currently above the hover cell being dragged.
     */
    private var mAboveItemId: Long = INVALID_ID.toLong()

    /**
     * ID of the item that is currently being dragged.
     */
    private var mMobileItemId: Long = INVALID_ID.toLong()

    /**
     * ID of the item that is currently below the hover cell being dragged.
     */
    private var mBelowItemId: Long = INVALID_ID.toLong()

    /**
     * [BitmapDrawable] created from the cell that has been long clicked, it is used as the
     * view that the user drags, the original view remains in the [ListView] but is invisible
     * until the drag ends.
     */
    private var mHoverCell: BitmapDrawable? = null

    /**
     * Current location of the hover cell on the screen as it is being dragged.
     */
    private var mHoverCellCurrentBounds: Rect? = null

    /**
     * Original location of the hover cell on the screen when it is first long clicked.
     */
    private var mHoverCellOriginalBounds: Rect? = null

    /**
     * Pointer identifier associated with the pointer data index 0 of the ACTION_DOWN event captured
     * by our [onTouchEvent] override, it is used to determine if the ACTION_POINTER_UP event
     * later received is for the same 'touch' of a multi-touch event (apparently).
     */
    private var mActivePointerId: Int = INVALID_POINTER_ID

    /**
     * Flag indicating that the [ListView] is currently scrolling
     */
    private var mIsWaitingForScrollFinish: Boolean = false

    /**
     * Current scroll state as received by the [OnScrollListener.onScrollStateChanged] override of
     * our [OnScrollListener] field [mScrollListener]
     */
    private var mScrollState: Int = OnScrollListener.SCROLL_STATE_IDLE

    /**
     * Our one argument constructor. First we call our super's constructor, then we call our method
     * [initialize] to initialize this instance. UNUSED.
     *
     * @param context The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     */
    constructor(context: Context) : super(context) {
        initialize(context)
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. First we call our super's constructor, then we call our method [initialize]
     * to initialize this instance. UNUSED.
     *
     * @param context  The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     * resource that supplies default values for the view.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        initialize(context)
    }

    /**
     * Perform inflation from XML. First we call our super's constructor, then we call our method
     * [initialize] to initialize this instance. This is the constructor that is used.
     *
     * @param context The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    /**
     * Called by our constructors to initialize this instance. First we set our
     * [OnItemLongClickListener] to our [OnItemLongClickListener] field [mOnItemLongClickListener],
     * then we set our [OnScrollListener] to our [OnScrollListener] field [mScrollListener]. We
     * initialize [DisplayMetrics] variable `val metrics` with the current display metrics that are
     * in effect for a [Resources] instance for the application's package. We then initialize our
     * [Int] field [mSmoothScrollAmountAtEdge] by dividing [SMOOTH_SCROLL_AMOUNT_AT_EDGE] by the
     * [DisplayMetrics.density] field of `metrics` (the `density` field is the logical density of
     * the display, and we use it here to scale the DIP value in [SMOOTH_SCROLL_AMOUNT_AT_EDGE] to
     * pixels).
     *
     * @param context The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     */
    fun initialize(context: Context) {
        onItemLongClickListener = mOnItemLongClickListener
        setOnScrollListener(mScrollListener)
        val metrics: DisplayMetrics = context.resources.displayMetrics
        mSmoothScrollAmountAtEdge = (SMOOTH_SCROLL_AMOUNT_AT_EDGE / metrics.density).toInt()
    }

    /**
     * Listens for long clicks on any items in the [ListView]. When a cell has been selected, the
     * hover cell is created and set up.
     */
    @Suppress("unused")
    private val mOnItemLongClickListener =
        OnItemLongClickListener { arg0: AdapterView<*>, arg1: View, pos: Int, id: Long ->
            /**
             * Callback method to be invoked when an item in this view has been clicked and held.
             * First we initialize our [Int] field [mTotalOffset] to 0. We initialize [Int] variable
             * `val position` to the position of the item which contains the point ([mDownX], [mDownY])
             * (this is the coordinates of the `onTouch` event which initiated this long click and
             * `position` should be the same as our[Int] parameter [pos] since any scrolling of the
             * [ListView] eats the long click events until the user lifts his finger, then long clicks
             * with the scrolling stopped, but might as well make sure I guess). We then initialize
             * [Int] variable `val itemNum` by subtracting the position number of the first visible
             * item on the screen from `position`. We initialize [View] variable `val selectedView` by
             * fetching our `itemNum`'th child, initialize our [Long] field [mMobileItemId] with the
             * item id of the item at `position` in our dataset, and initialize our [BitmapDrawable]
             * field [mHoverCell] with the [BitmapDrawable] that our method [getAndAddHoverView]
             * creates from `selectedView` (this is the [BitmapDrawable] that our [dispatchDraw]
             * override will draw on the screen when it is called). We then set the visibility of
             * `selectedView` to INVISIBLE (note that we then need to set it to VISIBLE again before
             * it is recycled by our adapter). We set our [Boolean] field [mCellIsMobile] to `true`
             * and call our method [updateNeighborViewsForID] to store a reference to the views
             * above and below the item currently corresponding to the hover cell in our [Long]
             * fields [mAboveItemId] and [mBelowItemId]. Finally we return `true` to consume the
             * long click.
             *
             * @param arg0 The AbsListView where the click happened
             * @param arg1 The view within the AbsListView that was clicked
             * @param pos  The position of the view in the list
             * @param id   The row id of the item that was clicked
             *
             * @return `true` if the callback consumed the long click, `false` otherwise
             */
            mTotalOffset = 0
            val position: Int = pointToPosition(mDownX, mDownY)
            val itemNum: Int = position - firstVisiblePosition
            val selectedView: View = getChildAt(itemNum)
            mMobileItemId = adapter.getItemId(position)
            mHoverCell = getAndAddHoverView(selectedView)
            selectedView.visibility = INVISIBLE
            mCellIsMobile = true
            updateNeighborViewsForID(mMobileItemId)
            true
        }

    /**
     * Creates the hover cell with the appropriate bitmap and of appropriate size. The hover cell's
     * [BitmapDrawable] is drawn on top of the [ListView] every single time an invalidate
     * call is made. We initialize [Int] variable `val w` with the width of our [View] parameter [v]
     * and [Int] variable `val h` with its height. We initialize [Int] variable `val top` with the
     * [View.getTop] top Y coordinate (kotlin `top` property) of [v] relative to its parent, and [Int]
     * variable `val left` with the [View.getLeft] left X coordinate (kotlin `left` property) both
     * in pixels. We initialize [Bitmap] variable `val b` with the black line bordered [Bitmap]
     * version of [v] returned by our method [getBitmapWithBorder]. We create [BitmapDrawable]
     * variable `val drawable` from `b`. We initialize our [Rect] field [mHoverCellOriginalBounds]
     * with a [Rect] whose left top corner is at (`left`, `top`) and whose right bottom corner is at
     * (`left`+`w`, `top`+`h`), which is of course the location of [v] on the screen when we were
     * called. We then initialize our [Rect] field [mHoverCellCurrentBounds] with a copy of
     * [mHoverCellOriginalBounds]. Finally we set the bounding rectangle of `drawable` to
     * [mHoverCellCurrentBounds] (this is where the drawable will draw when its draw() method is
     * called), and return `drawable` to the caller.
     *
     * @param v the view that has been long clicked, and needs to start hovering.
     * @return a `BitmapDrawable` version of our parameter `View v`
     */
    private fun getAndAddHoverView(v: View): BitmapDrawable {
        val w: Int = v.width
        val h: Int = v.height
        val top: Int = v.top
        val left: Int = v.left
        val b: Bitmap = getBitmapWithBorder(v = v)
        val drawable = BitmapDrawable(/* res = */ resources, /* bitmap = */ b)
        mHoverCellOriginalBounds = Rect(
            /* left = */ left,
            /* top = */ top,
            /* right = */ left + w,
            /* bottom = */ top + h
        )
        mHoverCellCurrentBounds = Rect(/* r = */ mHoverCellOriginalBounds)
        drawable.bounds = mHoverCellCurrentBounds!!
        return drawable
    }

    /**
     * Draws a black border over the screenshot of the view passed in. First we initialize our
     * [Bitmap] variable `val bitmap` with the [Bitmap] created from our [View] parameter [v]
     * by our method [getBitmapFromView]. We then initialize [Canvas] variable `val can` with an
     * instance which will draw into `bitmap`. We initialize [Rect] variable `val rect` with an
     * instance that is the same size as `bitmap`. We initialize [Paint] variable `val paint` with
     * a new instance, set its style to [Paint.Style.STROKE], set its stroke width to
     * [LINE_THICKNESS] (15), and set its color to [Color.BLACK]. We then draw the rectangle `rect`
     * on `can` using `paint` as the [Paint]. Finally we return `bitmap` to the caller.
     *
     * @param v `View` we want to create a screenshot of.
     * @return screenshot of our parameter `View v` with a black border.
     */
    private fun getBitmapWithBorder(v: View): Bitmap {
        val bitmap: Bitmap = getBitmapFromView(v = v)
        val can = Canvas(/* bitmap = */ bitmap)
        val rect = Rect(
            /* left = */ 0,
            /* top = */ 0,
            /* right = */ bitmap.width,
            /* bottom = */ bitmap.height
        )
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = LINE_THICKNESS.toFloat()
        paint.color = Color.BLACK

        // can.drawBitmap(bitmap, 0, 0, null); this is not necessary!
        can.drawRect(/* r = */ rect, /* paint = */ paint)
        return bitmap
    }

    /**
     * Returns a bitmap showing a screenshot of the view passed in. We initialize [Bitmap] variable
     * `val bitmap` with an instance which is the same width and height as our [View] parameter [v]
     * using a config of ARGB_8888, then initialize [Canvas] variable `val canvas` with an instance
     * which will draw into `bitmap`. We call the [View.draw] method of [v] to have it draw itself
     * onto `canvas` (and thus onto `bitmap`), then return `bitmap` to the caller.
     *
     * @param v the [View] whose screenshot we want.
     * @return a [Bitmap] showing a screenshot of our [View] parameter [v]
     */
    private fun getBitmapFromView(v: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            /* width = */ v.width,
            /* height = */ v.height,
            /* config = */ Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(/* bitmap = */ bitmap)
        v.draw(canvas)
        return bitmap
    }

    /**
     * Stores a reference to the views above and below the item currently corresponding to the hover
     * cell. It is important to note that if this item is either at the top or bottom of the list,
     * [mAboveItemId] or [mBelowItemId] may be set to the invalid value. First we set [Int] variable
     * `val position` to the position that our method [getPositionForID] returns for the item id of
     * the hover cell in our [Long] parameter [itemID]. Then we initialize our [StableArrayAdapter]
     * variable `val adapter` by retrieving the adapter for this [ListView]. We then set our [Long]
     * field [mAboveItemId] to the item id of the item in the position that is one less than
     * `position`, and our [Long] field [mBelowItemId] to the item id of the item in the position
     * that is one more than `position`.
     *
     * @param itemID item id of the hover cell.
     */
    private fun updateNeighborViewsForID(itemID: Long) {
        val position: Int = getPositionForID(itemID = itemID)
        val adapter = adapter as StableArrayAdapter
        mAboveItemId = adapter.getItemId(position - 1)
        mBelowItemId = adapter.getItemId(position + 1)
    }

    /**
     * Retrieves the view in the list corresponding to [Long] parameter [itemID]. We initialize
     * [Int] variable `val firstVisiblePosition` with the position in the dataset of the first
     * visible item on the screen. We then initialize [StableArrayAdapter] variable `val adapter`
     * by retrieving our [ListView]'s adapter. We loop over [Int] variable `var i` for all of our
     * children:
     *
     *  * We initialize [View] variable `val v` with our `i`'th child.
     *
     *  * We set [Int] variable `val position` to `firstVisiblePosition` plus `i` (this is the
     *  position in our dataset of this `i`'th child)
     *
     *  * We initialize [Long] variable `val id` with the item id that the [StableArrayAdapter.getItemId]
     *  method of `adapter` returns for the position `position`
     *
     *  * If `id` is equal to [itemID] we return `v` to the caller (otherwise we loop around to
     *  check the next child.
     *
     * If our loop fails to find a child with the item id [itemID] we return `null` to the caller.
     *
     * @param itemID item id of the view we want
     * @return the [View] that has item id [itemID]
     */
    fun getViewForID(itemID: Long): View? {
        val firstVisiblePosition: Int = firstVisiblePosition
        val adapter = adapter as StableArrayAdapter
        for (i in 0 until childCount) {
            val v: View = getChildAt(/* index = */ i)
            val position: Int = firstVisiblePosition + i
            val id: Long = adapter.getItemId(position = position)
            if (id == itemID) {
                return v
            }
        }
        return null
    }

    /**
     * Retrieves the position in the list corresponding to our [Long] parameter [itemID]. First we
     * initialize [View] variable `val v` with the view that our method [getViewForID] returns for
     * the item id [itemID]. If `v` is `null` we return -1, otherwise we return the position
     * returned by the method [getPositionForView] for `v`.
     *
     * @param itemID item id we are searching for
     * @return the position in the list corresponding to our [Long] parameter [itemID]
     */
    fun getPositionForID(itemID: Long): Int {
        val v: View? = getViewForID(itemID)
        return v?.let { getPositionForView(it) } ?: -1
    }

    /**
     * [dispatchDraw] gets invoked when all the child views are about to be drawn. By overriding
     * this method, the hover cell's [BitmapDrawable] can be drawn over the [ListView]'s items
     * whenever the [ListView] is redrawn. First we call our super's implementation of `dispatchDraw`
     * to draw all of its items. Then if our [BitmapDrawable] field [mHoverCell] is not `null` we
     * call its [BitmapDrawable.draw] method to draw itself on our [Canvas] parameter [canvas].
     *
     * @param canvas the [Canvas] on which to draw the view
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mHoverCell != null) {
            mHoverCell!!.draw(canvas)
        }
    }

    /**
     * We implement this method to handle touch screen motion events. We switch on the masked off
     * action field of our [MotionEvent] parameter [event]:
     *
     *  * [MotionEvent.ACTION_DOWN]: We initialize our [Int] field [mDownX] with the X coordinate
     *  of the first pointer index of [event], and `mDownY` with the Y coordinate. We initialize our
     *  [Int] field [mActivePointerId] with the pointer identifier associated with the 0'th pointer
     *  data index.
     *
     *  * [MotionEvent.ACTION_MOVE]: If our [Int] field [mActivePointerId] is equal to
     *  [INVALID_POINTER_ID] we do nothing. Otherwise we initialize [Int] variable `val pointerIndex`
     *  with the index of the data in [event] for pointer id [mActivePointerId], then set our [Int]
     *  field [mLastEventY] to the Y coordinate in `event` for pointer index `pointerIndex`. We
     *  initialize [Int] variable `val deltaY` to [mLastEventY] minus [mDownY]. If our [Boolean]
     *  field [mCellIsMobile] is `true` (a cell has been long clicked and is being dragged) we
     *  offset [Rect] field [mHoverCellCurrentBounds] to the X coordinate given in the [Rect.left]
     *  field of [Rect] field [mHoverCellOriginalBounds] (the hover cell does not move horizontally),
     *  and the Y coordinate calculated by adding the [Rect.top] field of [mHoverCellOriginalBounds]
     *  to `deltaY` plus our [Int] field [mTotalOffset]. We then specify [mHoverCellCurrentBounds]
     *  as the bounding rectangle for [BitmapDrawable] field [mHoverCell] (this is where the drawable
     *  will draw when its [BitmapDrawable.draw] method is called), and call [invalidate] to
     *  invalidate our whole view so that [draw] will be called sometime in the future. We then call
     *  our method [handleCellSwitch] to check whether the hover cell has been shifted far enough to
     *  invoke a cell swap, and if so, the respective cell swap candidate is determined and the data
     *  set is changed (the layout invoked by a call to [StableArrayAdapter.notifyDataSetChanged]
     *  will place the cells in the right place). We set our [Boolean] field [mIsMobileScrolling] to
     *  `false`, then call our method [handleMobileCellScroll] to determine if the hover cell is
     *  above or below the bounds of the [ListView], and if so, has the [ListView] do an appropriate
     *  upward or downward smooth scroll so as to reveal new items. Finally we return false to the
     *  caller so that regular touch event processing will proceed. On the other hand if
     *  [mCellIsMobile] is `false` we do nothing.
     *
     *  * [MotionEvent.ACTION_UP]: We call our method [touchEventsEnded] to reset all the
     *  appropriate fields to a default state while also animating the hover cell to its
     *  correct location in the [ListView].
     *
     *  * [MotionEvent.ACTION_CANCEL]: We call our method [touchEventsCancelled] to reset all the
     *  appropriate fields to a default state.
     *
     *  * [MotionEvent.ACTION_POINTER_UP]: We mask off the action pointer index from the action of
     *  [event] and normalize its shift in order to set [Int] variable `val pointerIndex`. We
     *  initialize [Int] variable `val pointerId` with the pointer id associated with `pointerIndex`
     *  and if it is equal to [Int] field [mActivePointerId] (the pointer id for the ACTION_DOWN
     *  which started the touch sequence) we call our method [touchEventsEnded] to reset all the
     *  appropriate fields to a default state while also animating the hover cell to its correct
     *  location in the [ListView] same as for an ACTION_UP event.
     *
     *  * default: we do nothing
     *
     * Finally we return the value returned by our super's implementation of [onTouchEvent] to
     * the caller.
     *
     * @param event The motion event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = event.x.toInt()
                mDownY = event.y.toInt()
                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId != INVALID_POINTER_ID) {

                    val pointerIndex = event.findPointerIndex(mActivePointerId)
                    mLastEventY = event.getY(pointerIndex).toInt()
                    val deltaY = mLastEventY - mDownY
                    if (mCellIsMobile) {
                        mHoverCellCurrentBounds!!.offsetTo(
                            /* newLeft = */ mHoverCellOriginalBounds!!.left,
                            /* newTop = */ mHoverCellOriginalBounds!!.top + deltaY + mTotalOffset
                        )
                        mHoverCell!!.bounds = mHoverCellCurrentBounds!!
                        invalidate()
                        handleCellSwitch()
                        mIsMobileScrolling = false
                        handleMobileCellScroll()
                        return false
                    }
                }
            }

            MotionEvent.ACTION_UP -> touchEventsEnded()
            MotionEvent.ACTION_CANCEL -> touchEventsCancelled()
            MotionEvent.ACTION_POINTER_UP -> {
                /* If a multitouch event took place and the original touch dictating
                 * the movement of the hover cell has ended, then the dragging event
                 * ends and the hover cell is animated to its corresponding position
                 * in the ListView. */
                val pointerIndex: Int = event.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr
                    MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    touchEventsEnded()
                }
            }

            else -> {}
        }
        return super.onTouchEvent(event)
    }

    /**
     * This method determines whether the hover cell has been shifted far enough to invoke a cell
     * swap. If so, then the respective cell swap candidate is determined and the data set is
     * changed. Upon posting a notification of the data set change, a layout is invoked to place the
     * cells in the right place. Using a [ViewTreeObserver] and a corresponding [OnPreDrawListener],
     * we can offset the cell being swapped to where it previously was and then animate it to its
     * new position.
     *
     * First we initialize [Int] variable `val deltaY` by subtracting [Int] field [mDownY] from [Int]
     * field [mLastEventY] (giving us the plus or minus distance on the screen we have moved since
     * the initial long click, or the last time a cell was swapped -- it is set to [mLastEventY]
     * every time a cell is swapped). Then we initialize [Int] variable `val deltaYTotal` by adding
     * the [Rect.top] field of [Rect] field [mHoverCellOriginalBounds] to [Int] field [mTotalOffset]
     * plus `deltaY` (this is the current position of the top of the hover cell). We initialize [View]
     * variable `val belowView` with the view with the id of [Long] field [mBelowItemId], [View]
     * variable `val mobileView` with the view with the id of [Long] field [mMobileItemId], and
     * [View] variable `val aboveView` with the view with the id of [Long] field [mAboveItemId]. We
     * initialize [Boolean] variable `val isBelow` to `true` if `belowView` is not `null` and
     * `deltaYTotal` is greater than the top Y coordinate of `belowView`. We initialize [Boolean]
     * variable `val isAbove` to `true` if `aboveView` is not `null` and `deltaYTotal` is less than
     * the top Y coordinate of `aboveView`. If either `isBelow` or `isAbove` is `true` we need to
     * switch cells:
     *
     *  * We initialize [Long] variable `val switchItemID` to [mBelowItemId] if `isBelow` is `true`
     *  or to [mAboveItemId] if it is `false`, and initialize [View] variable `val switchView` to
     *  `belowView` if `isBelow` is true or to `aboveView` if it is false.
     *
     *  * We initialize [Int] variable `val originalItem` with the position in our [AdapterView]'s
     *  dataset that the [getPositionForView] method calculates for `mobileView`.
     *
     *  * If `switchView` is `null` we call our method [updateNeighborViewsForID] to update the item
     *  id's in [mAboveItemId] and [mBelowItemId] to values for the item id [mMobileItemId] (the
     *  items before and after the hover cell's item id). I cannot find a case where `switchView`
     *  can possibly be null, the logic before this statement guaranties that it is either
     *  `belowView` or `aboveView`!
     *
     *  * We call our method [swapElements] to swap the data items at position `originalItem` (the
     *  item being dragged) with the item at the position occupied by `switchView` in our [ArrayList]
     *  of [String] dataset field [mCheeseList]. We then call the [StableArrayAdapter.notifyDataSetChanged]
     *  method of our adapter to notify it that the dataset changed and the [ListView] needs to be
     *  redrawn.
     *
     *  * We set [Int] field [mDownY] to [Int] field [mLastEventY], and initialize [Int] variable
     *  `val switchViewStartTop` to the top Y coordinate of `switchView`.
     *
     *  * We set the visibility of `mobileView` to VISIBLE and the visibility of `switchView` to
     *  INVISIBLE, then we call our [updateNeighborViewsForID] method to have it update the values
     *  of [Long] field [mAboveItemId] (the id of the item that is now above the long clicked item
     *  on the screen given the new location of the item with id of [Long] field [mMobileItemId]),
     *  and [Long] field [mBelowItemId] (the id of the item that is now below the long clicked item
     *  on the screen given the new location of the item with id [mMobileItemId]).
     *
     *  * We initialize [ViewTreeObserver] variable `val observer` with the [ViewTreeObserver] for
     *  this view's hierarchy and add to it an anonymous [OnPreDrawListener] which animates the view
     *  containing the item with id `switchItemID` into its new position on the screen, and then
     *  returns `true` to the caller so that the drawing will proceed.
     */
    private fun handleCellSwitch() {
        val deltaY: Int = mLastEventY - mDownY
        val deltaYTotal: Int = mHoverCellOriginalBounds!!.top + mTotalOffset + deltaY
        val belowView: View? = getViewForID(mBelowItemId)
        val mobileView: View? = getViewForID(mMobileItemId)
        val aboveView: View? = getViewForID(mAboveItemId)
        val isBelow: Boolean = belowView != null && deltaYTotal > belowView.top
        val isAbove: Boolean = aboveView != null && deltaYTotal < aboveView.top
        if (isBelow || isAbove) {
            Log.i(TAG, "deltaY: $deltaY")
            val switchItemID = if (isBelow) mBelowItemId else mAboveItemId
            val switchView = if (isBelow) belowView else aboveView
            val originalItem = getPositionForView(mobileView)
            if (switchView == null) {
                updateNeighborViewsForID(mMobileItemId)
                return
            }
            swapElements(
                arrayList = mCheeseList,
                indexOne = originalItem,
                indexTwo = getPositionForView(switchView)
            )
            (adapter as BaseAdapter).notifyDataSetChanged()
            mDownY = mLastEventY
            val switchViewStartTop: Int = switchView.top
            mobileView!!.visibility = VISIBLE
            switchView.visibility = INVISIBLE
            updateNeighborViewsForID(itemID = mMobileItemId)
            val observer: ViewTreeObserver = viewTreeObserver
            observer.addOnPreDrawListener(object : OnPreDrawListener {
                /**
                 * Callback method to be invoked when the view tree is about to be drawn. At this
                 * point, all views in the tree have been measured and given a frame. First we
                 * remove ourselves as an [OnPreDrawListener]. Then we initialize [View] variable
                 * `val switchViewLocal` with the view that our method [getViewForID] finds for the
                 * item id `switchItemID`. Then we add `deltaY` to our [Int] field [mTotalOffset].
                 * We initialize [Int] variable `val switchViewNewTop` with the new top Y coordinate
                 * of `switchViewLocal`, and [Int] variable `val delta` by subtracting `switchViewNewTop`
                 * from `switchViewStartTop` (this is how far the switched view has to move to get
                 * to its new location), then we set the the vertical location of this view relative
                 * to its `top` position to `delta` (this effectively positions the object post-layout,
                 * in addition to wherever the object's layout placed it). Then we initialize
                 * [ObjectAnimator] variable `val animator` with an instance which will animate the
                 * TRANSLATION_Y property of `switchViewLocal` back to 0, set its duration to
                 * [MOVE_DURATION] (150ms), and start it running. Finally we return `true` to the
                 * caller so the current drawing pass will proceed.
                 *
                 * @return Returns `true` to proceed with the current drawing pass.
                 */
                override fun onPreDraw(): Boolean {
                    observer.removeOnPreDrawListener(this)
                    val switchViewLocal: View? = getViewForID(switchItemID)
                    mTotalOffset += deltaY
                    val switchViewNewTop: Int = switchViewLocal!!.top
                    val delta: Int = switchViewStartTop - switchViewNewTop
                    switchViewLocal.translationY = delta.toFloat()
                    val animator = ObjectAnimator.ofFloat(
                        /* target = */ switchViewLocal,
                        /* property = */ TRANSLATION_Y,
                        /* ...values = */ 0f
                    )
                    animator.duration = MOVE_DURATION.toLong()
                    animator.start()
                    return true
                }
            })
        }
    }

    /**
     * Swaps the location of two items in our dataset. First we initialize [String] variable
     * `val temp` with the [Object] at the position of [Int] parameter [indexOne] in the [ArrayList]
     * of [String] parameter [arrayList], then we replace the element at position [indexOne] in this
     * list with the element in the position of [Int] parameter [indexTwo]. Finally we replace the
     * element at position [indexTwo] in this list with `temp`.
     *
     * @param arrayList the [ArrayList] of [String] holding our dataset
     * @param indexOne the first index into [ArrayList] of [String] parameter [arrayList] whose item
     * is to swapped
     * @param indexTwo the second index into [ArrayList] of [String] parameter [arrayList] whose
     * item is to swapped
     */
    private fun swapElements(arrayList: ArrayList<String>?, indexOne: Int, indexTwo: Int) {
        val temp: String = arrayList!![indexOne]
        arrayList[indexOne] = arrayList[indexTwo]
        arrayList[indexTwo] = temp
    }

    /**
     * Resets all the appropriate fields to a default state while also animating the hover cell back
     * to its correct location. First we initialize [View] variable `val mobileView` with the [View]
     * whose item id is [Long] field [mMobileItemId]. Then we branch based on whether either of our
     * [Boolean] fields [mCellIsMobile] or [mIsWaitingForScrollFinish] is `true`:
     *
     *  * Either [mCellIsMobile] is `true` (an item in our list has been long clicked and has been
     *  being dragged about) or [mIsWaitingForScrollFinish] is `true` ([Int] field [mScrollState]
     *  was not equal to [OnScrollListener.SCROLL_STATE_IDLE] on a previous call to us):
     *  We set [Boolean] fields [mCellIsMobile], [mIsWaitingForScrollFinish], and [mIsMobileScrolling]
     *  all to `false`, and set [Int] field [mActivePointerId] to [INVALID_POINTER_ID]. Then if
     *  [mScrollState] is not equal to [OnScrollListener.SCROLL_STATE_IDLE] we set [Boolean] field
     *  [mIsWaitingForScrollFinish] to `true` and just return (since the `AutoScroller` has not
     *  completed scrolling, we need to wait for it to finish in order to determine the final
     *  location of where the hover cell should be animated to). Otherwise we offset the rectangle
     *  of [Rect] field [mHoverCellCurrentBounds] to the top Y coordinate of `mobileView`. We
     *  initialize [ObjectAnimator] variable `val hoverViewAnimator` with an instance which will
     *  animate the "bounds" property of [BitmapDrawable] field [mHoverCell] to [Rect] field
     *  [mHoverCellCurrentBounds] using [TypeEvaluator] field [sBoundEvaluator] as the [TypeEvaluator],
     *  and set its [AnimatorUpdateListener] to an anonymous class whose lambda override just calls
     *  [invalidate] to have our view redrawn. Then we add an [AnimatorListenerAdapter] to
     *  `hoverViewAnimator` whose [AnimatorListenerAdapter.onAnimationStart] disables our [View]
     *  (to prevent additional touch events occurring during the animation), and whose
     *  [AnimatorListenerAdapter.onAnimationEnd] override sets everything back to the state they
     *  should be in when the dragging is over. Finally we start `hoverViewAnimator` running.
     *
     *  * If both [Boolean] fields [mCellIsMobile] and [mIsWaitingForScrollFinish] are `false` we
     *  just call our [touchEventsCancelled] method to reset all the appropriate fields to a
     *  default state.
     */
    private fun touchEventsEnded() {
        val mobileView: View? = getViewForID(mMobileItemId)
        if (mCellIsMobile || mIsWaitingForScrollFinish) {
            mCellIsMobile = false
            mIsWaitingForScrollFinish = false
            mIsMobileScrolling = false
            mActivePointerId = INVALID_POINTER_ID

            // If the AutoScroller has not completed scrolling, we need to wait for it to
            // finish in order to determine the final location of where the hover cell
            // c.
            if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mIsWaitingForScrollFinish = true
                return
            }
            mHoverCellCurrentBounds!!.offsetTo(
                /* newLeft = */ mHoverCellOriginalBounds!!.left,
                /* newTop = */ mobileView!!.top
            )
            val hoverViewAnimator = ObjectAnimator.ofObject(
                /* target = */ mHoverCell,
                /* propertyName = */ "bounds",
                /* evaluator = */ sBoundEvaluator,
                /* ...values = */ mHoverCellCurrentBounds
            )
            hoverViewAnimator.addUpdateListener { invalidate() }
            hoverViewAnimator.addListener(object : AnimatorListenerAdapter() {
                /**
                 * Notifies the start of the animation. We disable our [View] (to prevent additional
                 * touch events occurring during the animation).
                 *
                 * @param animation The started animation.
                 */
                override fun onAnimationStart(animation: Animator) {
                    isEnabled = false
                }

                /**
                 * Notifies the end of the animation. We set our [Long] fields [mAboveItemId],
                 * [mMobileItemId], and [mBelowItemId] all to [INVALID_ID], set the visibility of
                 * `mobileView` to VISIBLE, set [BitmapDrawable] field [mHoverCell] to `null`,
                 * enable our [View], and then call [invalidate] to have our [View] redrawn.
                 *
                 * @param animation The animation which reached its end.
                 */
                override fun onAnimationEnd(animation: Animator) {
                    mAboveItemId = INVALID_ID.toLong()
                    mMobileItemId = INVALID_ID.toLong()
                    mBelowItemId = INVALID_ID.toLong()
                    mobileView.visibility = VISIBLE
                    mHoverCell = null
                    isEnabled = true
                    invalidate()
                }
            })
            hoverViewAnimator.start()
        } else {
            touchEventsCancelled()
        }
    }

    /**
     * Resets all the appropriate fields to a default state. First we initialize [View] variable
     * `val mobileView` with the view whose item id is [Long] field [mMobileItemId].
     * If [Boolean] field [mCellIsMobile] is `true`:
     *
     *  * We set our [Long] fields [mAboveItemId], [mMobileItemId], and [mBelowItemId] all to
     *  [INVALID_ID], set the visibility of `mobileView` to VISIBLE, set [BitmapDrawable] field
     *  [mHoverCell] to `null`, and then call [invalidate] to have our [View] redrawn.
     *
     * We then set our [Boolean] fields [mCellIsMobile] and [mIsMobileScrolling] to `false` and set
     * [Int] field [mActivePointerId] to [INVALID_POINTER_ID].
     */
    private fun touchEventsCancelled() {
        val mobileView: View? = getViewForID(mMobileItemId)
        if (mCellIsMobile) {
            mAboveItemId = INVALID_ID.toLong()
            mMobileItemId = INVALID_ID.toLong()
            mBelowItemId = INVALID_ID.toLong()
            mobileView!!.visibility = VISIBLE
            mHoverCell = null
            invalidate()
        }
        mCellIsMobile = false
        mIsMobileScrolling = false
        mActivePointerId = INVALID_POINTER_ID
    }

    /**
     * Determines whether this [ListView] is in a scrolling state invoked by the fact that the hover
     * cell is out of the bounds of the [ListView]. We just set our [Boolean] field [mIsMobileScrolling]
     * to the value returned by our [handleMobileCellScroll] method for the current value of our
     * [Rect] field [mHoverCellCurrentBounds].
     */
    private fun handleMobileCellScroll() {
        mIsMobileScrolling = handleMobileCellScroll(r = mHoverCellCurrentBounds)
    }

    /**
     * This method is in charge of determining if the hover cell is above or below the bounds of the
     * [ListView]. If so, the [ListView] does an appropriate upward or downward smooth scroll so as
     * to reveal new items. We initialize [Int] variable `val offset` with the value that the
     * [computeVerticalScrollOffset] method returns for the vertical offset of the scrollbar's
     * thumb. We initialize [Int] variable `val height` with the height of our [View]. We initialize
     * [Int] variable `val extent` with the value that the [computeVerticalScrollExtent] method
     * returns for the vertical extent of the scrollbar's thumb. We initialize [Int] variable
     * `val range` with the value that the [computeVerticalScrollRange] method returns for the total
     * vertical range represented by the vertical scrollbar. We initialize [Int] variable
     * `val hoverViewTop` with the [Rect.top] field of our [Rect] parameter [r] (the top Y coordinate
     * of our hover cell). And we initialize [Int] variable `val hoverHeight` with the [Rect.height]
     * of our [Rect] parameter [r]. If `hoverViewTop` is less than or equal to 0 (the hover cell has
     * been dragged to the very top of the [ListView]) and `offset` is greater than 0 (part of the
     * [ListView] is above the top of the screen and can be scrolled down) we call the method
     * [smoothScrollBy] to scroll the [ListView] by minus our [Int] field [mSmoothScrollAmountAtEdge]
     * (down) with 0 as the animation duration, and then return `true` to the caller. If `hoverViewTop`
     * plus `hoverHeight` is greater than or equal to `height` (the hover cell has been dragged to
     * the bottom of the screen) and `offset` plus `extent` is less than `range` (there are still
     * cells below the bottom of the screen in the [ListView]) we call the method [smoothScrollBy]
     * to scroll the `ListView` by plus [mSmoothScrollAmountAtEdge] (up) with 0 as the animation
     * duration, and then return `true` to the caller. If the hover cell is not at top or the bottom
     * of the screen we return `false` to the caller.
     *
     * @param r current hover cell bounds (always [Rect] field [mHoverCellCurrentBounds] in our case)
     * @return `true` if the [ListView] is smooth scrolling, `false` if it did not need to.
     */
    fun handleMobileCellScroll(r: Rect?): Boolean {
        val offset: Int = computeVerticalScrollOffset()
        val height: Int = height
        val extent: Int = computeVerticalScrollExtent()
        val range: Int = computeVerticalScrollRange()
        val hoverViewTop: Int = r!!.top
        val hoverHeight: Int = r.height()
        if (hoverViewTop <= 0 && offset > 0) {
            smoothScrollBy(/* distance = */ -mSmoothScrollAmountAtEdge, /* duration = */ 0)
            return true
        }
        if (hoverViewTop + hoverHeight >= height && offset + extent < range) {
            smoothScrollBy(/* distance = */ mSmoothScrollAmountAtEdge, /* duration = */ 0)
            return true
        }
        return false
    }

    /**
     * Sets our [ArrayList] of [String] dataset field [mCheeseList] to our [ArrayList] of [String]
     * parameter [cheeseList].
     *
     * @param cheeseList List of cheeses that we should use as our dataset.
     */
    fun setCheeseList(cheeseList: ArrayList<String>?) {
        mCheeseList = cheeseList
    }

    /**
     * This scroll listener is added to the [ListView] in order to handle cell swapping
     * when the cell is either at the top or bottom edge of the [ListView]. If the hover
     * cell is at either edge of the [ListView], the [ListView] will begin scrolling. As
     * scrolling takes place, the [ListView] continuously checks if new cells became visible
     * and determines whether they are potential candidates for a cell swap.
     */
    private val mScrollListener: OnScrollListener = object : OnScrollListener {
        /**
         * Value of the index of the first visible cell on the previous call to [onScroll]
         */
        private var mPreviousFirstVisibleItem: Int = -1

        /**
         * Value of the number of visible cells on the previous call to [onScroll]
         */
        private var mPreviousVisibleItemCount: Int = -1

        /**
         * Value of the index of the first visible cell on the latest call to [onScroll]
         */
        private var mCurrentFirstVisibleItem: Int = 0

        /**
         * Value of the number of visible cells on the latest call to [onScroll]
         */
        private var mCurrentVisibleItemCount: Int = 0

        /**
         * Value of the latest scroll state reported to [onScrollStateChanged]
         */
        private var mCurrentScrollState: Int = 0

        /**
         * Callback method to be invoked when the list or grid has been scrolled. This will be called
         * after the scroll has completed. We save our [Int] parameter [firstVisibleItem] in [Int]
         * field [mCurrentFirstVisibleItem], and our [Int] parameter [visibleItemCount] in [Int]
         * field [mCurrentVisibleItemCount]. If [Int] field [mPreviousFirstVisibleItem] is equal
         * to -1 (the uninitialized state) we set it to [mCurrentFirstVisibleItem] and if [Int]
         * field [mPreviousVisibleItemCount] is equal to -1 (the uninitialized state) we set it to
         * [mCurrentVisibleItemCount]. We then call the methods [checkAndHandleFirstVisibleCellChange]
         * (which handles the situation where [mCurrentFirstVisibleItem] != [mPreviousFirstVisibleItem])
         * and [checkAndHandleLastVisibleCellChange] (which handles the situation where the [ListView]
         * has scrolled down enough to reveal a new cell at the bottom of the list). Finally we set
         * [mPreviousFirstVisibleItem] to [mCurrentFirstVisibleItem] and [mPreviousVisibleItemCount]
         * to [mCurrentVisibleItemCount].
         *
         * @param view             The [ListView] whose scroll state is being reported
         * @param firstVisibleItem the index of the first visible cell (ignore if visibleItemCount == 0)
         * @param visibleItemCount the number of visible cells
         * @param totalItemCount   the number of items in the list adaptor
         */
        override fun onScroll(
            view: AbsListView,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        ) {
            mCurrentFirstVisibleItem = firstVisibleItem
            mCurrentVisibleItemCount = visibleItemCount
            mPreviousFirstVisibleItem =
                if (mPreviousFirstVisibleItem == -1) mCurrentFirstVisibleItem else mPreviousFirstVisibleItem
            mPreviousVisibleItemCount =
                if (mPreviousVisibleItemCount == -1) mCurrentVisibleItemCount else mPreviousVisibleItemCount
            checkAndHandleFirstVisibleCellChange()
            checkAndHandleLastVisibleCellChange()
            mPreviousFirstVisibleItem = mCurrentFirstVisibleItem
            mPreviousVisibleItemCount = mCurrentVisibleItemCount
        }

        /**
         * Callback method to be invoked while the list view or grid view is being scrolled. If the
         * view is being scrolled, this method will be called before the next frame of the scroll is
         * rendered. We save our [Int] parameter [scrollState] in our [Int] field [mCurrentScrollState]
         * as well as in our [Int] field [mScrollState]. We then reference our [isScrollCompleted]
         * property to have its `get` method decide to either continue a scroll invoked by the hover
         * cell being outside the bounds of the [ListView], or if the hover cell has already been
         * released invoke the animation for the hover cell to return to its correct position after
         * the [ListView] has entered an idle scroll state ([isScrollCompleted] is a property only
         * because the java to kotlin converter was confused by the name IMO, but what the hay).
         *
         * @param view The view whose scroll state is being reported
         * @param scrollState The current scroll state. One of
         * [OnScrollListener.SCROLL_STATE_TOUCH_SCROLL] or [OnScrollListener.SCROLL_STATE_IDLE].
         */
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            mCurrentScrollState = scrollState
            mScrollState = scrollState
            isScrollCompleted
        }

        /**
         * This property is in charge of invoking 1 of 2 actions. Firstly, if the [ListView] is
         * in a state of scrolling invoked by the hover cell being outside its bounds, then this
         * scrolling event is continued. Secondly, if the hover cell has already been released, this
         * invokes the animation for the hover cell to return to its correct position after the
         * [ListView] has entered an idle scroll state. If [Int] field [mCurrentVisibleItemCount] is
         * greater than 0 (there are one or more visible items from our [ListView] on the screen)
         * and [Int] field [mCurrentScrollState] is equal to [OnScrollListener.SCROLL_STATE_IDLE]
         * (view is not scrolling) we branch on whether both [Boolean] field [mCellIsMobile] and
         * [Boolean] field [mIsMobileScrolling] are `true`:
         *
         *  * both `true`: we call the [handleMobileCellScroll] method to handle any scrolling that
         *  may be necessary when the hover cell is above or below the bounds of the [ListView].
         *
         *  * one or both `false`: if [Boolean] field [mIsWaitingForScrollFinish] is `true` we call
         *  our [touchEventsEnded] method to reset all the appropriate fields to a default state
         *  while also animating the hover cell back to its correct location.
         *
         * *NOTE:* This should not be a property, but the java to kotlin converter was confused by
         * the name (bizarre!)
         */
        private val isScrollCompleted: Unit
            get() {
                if (mCurrentVisibleItemCount > 0 && mCurrentScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    if (mCellIsMobile && mIsMobileScrolling) {
                        handleMobileCellScroll()
                    } else if (mIsWaitingForScrollFinish) {
                        touchEventsEnded()
                    }
                }
            }

        /**
         * Determines if the [ListView] scrolled up enough to reveal a new cell at the top of the
         * list. If so, then the appropriate parameters are updated. If [mCurrentFirstVisibleItem]
         * is not equal to [mPreviousFirstVisibleItem] (the first visible item has changed) we check
         * to see whether [Boolean] field [mCellIsMobile] is `true` (the hover cell is in motion)
         * and [Long] field [mMobileItemId] is not equal to [INVALID_ID] (the item id of the hover
         * cell is valid) and if these are both `true` we call our [updateNeighborViewsForID] method
         * to update the item id's of the items above and below [mMobileItemId] in the [ListView]
         * and then call our method [handleCellSwitch] to swap cells if it is necessary to do so.
         */
        fun checkAndHandleFirstVisibleCellChange() {
            if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
                if (mCellIsMobile && mMobileItemId != INVALID_ID.toLong()) {
                    updateNeighborViewsForID(mMobileItemId)
                    handleCellSwitch()
                }
            }
        }

        /**
         * Determines if the [ListView] scrolled down enough to reveal a new cell at the bottom of
         * the list. If so, then the appropriate parameters are updated. We set [Int] variable
         * `val currentLastVisibleItem` to [Int] field [mCurrentFirstVisibleItem] plus [Int] field
         * [mCurrentVisibleItemCount] (this is the index of the last visible item in the [ListView])
         * and [Int] variable `val previousLastVisibleItem` to [Int] field [mPreviousFirstVisibleItem]
         * plus [Int] field [mPreviousVisibleItemCount] (this is the index of the previous last
         * visible item in the [ListView]). If `currentLastVisibleItem` is not equal to
         * `previousLastVisibleItem` (the last visible item has changed) we check to see whether
         * [Boolean] field [mCellIsMobile] is `true` (the hover cell is in motion) and [Long] field
         * [mMobileItemId] is not equal to [INVALID_ID] (the item id of the hover cell is valid) and
         * if they are both `true` we call our [updateNeighborViewsForID] method to update the item
         * id's of the items above and below and then we call our [handleCellSwitch] method to swap
         * cells if it is necessary to do so.
         */
        fun checkAndHandleLastVisibleCellChange() {
            val currentLastVisibleItem: Int = mCurrentFirstVisibleItem + mCurrentVisibleItemCount
            val previousLastVisibleItem: Int = mPreviousFirstVisibleItem + mPreviousVisibleItemCount
            if (currentLastVisibleItem != previousLastVisibleItem) {
                if (mCellIsMobile && mMobileItemId != INVALID_ID.toLong()) {
                    updateNeighborViewsForID(mMobileItemId)
                    handleCellSwitch()
                }
            }
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        const val TAG: String = "DynamicListView"

        /**
         * This [TypeEvaluator] is used to animate the [BitmapDrawable] back to its final location
         * when the user lifts his finger by animating the modification of the [BitmapDrawable]'s
         * bounds.
         */
        private val sBoundEvaluator: TypeEvaluator<Rect> = object : TypeEvaluator<Rect> {
            /**
             * This function returns the result of linearly interpolating the start and end values,
             * with its [Float] parameter [fraction] representing the proportion between the start
             * and end values. We return a new instance of [Rect] whose [Rect.left], [Rect.top],
             * [Rect.right], and [Rect.bottom] values are calculated by our method [interpolate] by
             * interpolating between the [Rect] parameter [startValue] and [Rect] parameter [endValue]
             * values of those fields with [fraction] representing the proportion between the start
             * and end values.
             *
             * @param fraction   The fraction from the starting to the ending values
             * @param startValue The start value.
             * @param endValue   The end value.
             * @return A linear interpolation between the start and end values, given the
             * [Float] parameter [fraction].
             */
            override fun evaluate(fraction: Float, startValue: Rect, endValue: Rect): Rect {
                return Rect(
                    /* left = */ interpolate(startValue.left, endValue.left, fraction),
                    /* top = */ interpolate(startValue.top, endValue.top, fraction),
                    /* right = */ interpolate(startValue.right, endValue.right, fraction),
                    /* bottom = */ interpolate(startValue.bottom, endValue.bottom, fraction)
                )
            }

            /**
             * Returns a value calculated by linearly interpolating between the [Int] parameter
             * [start] and the [Int] parameter [end] values, with [Float] parameter [fraction]
             * representing the proportion between the [start] and [end] values.
             *
             * @param start start value
             * @param end end value
             * @param fraction proportion between the start and end values
             * @return value calculated by linearly interpolating between the [start] and [end]
             * values, with [fraction] representing the proportion between the [start] and [end]
             * values.
             */
            fun interpolate(start: Int, end: Int, fraction: Float): Int {
                return (start + fraction * (end - start)).toInt()
            }
        }

        /**
         * DP amount to scroll by, it is divided by the density of the screen in order to determine
         * how many pixels to smoothly scroll the [ListView] when the item being dragged has been
         * dragged above or below the bounds of the [ListView].
         */
        private const val SMOOTH_SCROLL_AMOUNT_AT_EDGE: Int = 15

        /**
         * Duration of the animation of item views which are moved when the view being dragged
         * passes over them.
         */
        private const val MOVE_DURATION: Int = 150

        /**
         * Stroke width of the [Paint] used by the [getBitmapWithBorder] method to draw
         * a black border over the screenshot of the view passed in
         */
        private const val LINE_THICKNESS: Int = 15

        /**
         * Value to indicate that the id of the item in question is invalid.
         */
        private const val INVALID_ID: Int = -1

        /**
         * Value to indicate that a point id is not valid.
         */
        private const val INVALID_POINTER_ID: Int = -1

    }
}