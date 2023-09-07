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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog")

package com.example.android.listviewremovalanimation

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.ListView
import java.util.Collections

/**
 * This example shows what goes wrong when a view that is chosen to be deleted when that view is on
 * the screen is recycled when that view has been scrolled off the screen (the views that are animated
 * no longer contain the items that were selected to be deleted, but are animated anyway).
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [ListViewRemovalAnimation](https://www.youtube.com/watch?v=NewCSg2JKLk).
 */
class ListViewRemovalAnimation : Activity() {
    /**
     * The [StableArrayAdapter] we use for our [ListView]
     */
    var mAdapter: StableArrayAdapter? = null

    /**
     * The [ListView] in our layout with id [R.id.list_view] which displays our cheeses.
     */
    var mListView: ListView? = null

    /**
     * The [BackgroundContainer] in our layout with id [R.id.listViewBackground]
     */
    var mBackgroundContainer: BackgroundContainer? = null
    /**
     * Is the user swiping an item
     */
    var mSwiping: Boolean = false
    /**
     * Has the user pressed an item
     */
    var mItemPressed: Boolean = false

    /**
     * Map of the cheese strings in our dataset to an unique stable id assigned to each.
     */
    @SuppressLint("UseSparseArrays")
    var mItemIdTopMap: HashMap<Long, Int> = HashMap()

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_list_view_deletion]. We
     * initialize our [BackgroundContainer] field [mBackgroundContainer] by finding the view with id
     * [R.id.listViewBackground], and [ListView] field [mListView] by finding the view with id
     * [R.id.list_view]. We allocate a new instance of [ArrayList] of [String] to initialize variable
     * `val cheeseList` then add all the cheeses in the [Array] of [String] field [Cheeses.sCheeseStrings]
     * to it. We initialize our [StableArrayAdapter] field [mAdapter] with a new instance which will
     * use `cheeseList` as its dataset, displaying them using the layout [R.layout.opaque_text_view]
     * with our [OnTouchListener] field [mTouchListener] as its [OnTouchListener]. Finally we set the
     * adapter of [mListView] to be [mAdapter].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view_deletion)
        mBackgroundContainer = findViewById(R.id.listViewBackground)
        mListView = findViewById(R.id.list_view)
        Log.d("Debug", "d=" + mListView!!.divider)
        val cheeseList = ArrayList<String>()
        Collections.addAll(cheeseList, *Cheeses.sCheeseStrings)
        mAdapter = StableArrayAdapter(this, R.layout.opaque_text_view, cheeseList, mTouchListener)
        mListView!!.adapter = mAdapter
    }

    /**
     * [OnTouchListener] used for every view that the [StableArrayAdapter.getView] override of our
     * [StableArrayAdapter] returns to its caller.
     */
    private val mTouchListener: OnTouchListener = object : OnTouchListener {
        /**
         * X coordinate of the initial ACTION_DOWN event we received for the "swiping" that is
         * currently being done.
         */
        var mDownX = 0f

        /**
         * Distance in pixels a touch can wander before we think the user is scrolling, set by a
         * call to the [ViewConfiguration.getScaledTouchSlop] method of the [ViewConfiguration]
         * of our current [ListViewRemovalAnimation] instance.
         */
        private var mSwipeSlop = -1

        /**
         * Called when a touch event is dispatched to a view. If our [Int] field [mSwipeSlop] is
         * less than 0 we initialize it to the value returned by the
         * [ViewConfiguration.getScaledTouchSlop] method of the [ViewConfiguration] of our current
         * [ListViewRemovalAnimation] instance. Then we switch on the action of our [MotionEvent]
         * parameter [event]:
         *
         *  * [MotionEvent.ACTION_DOWN]: if our [Boolean] flag field [mItemPressed] is `true` we
         *  return true having done nothing (we are already responding to a swipe, and multi-item
         *  swipes are not handled). Otherwise we set our [Boolean] flag field [mItemPressed] to
         *  `true`, and set [Float] field [mDownX] to the X coordinate of [event].
         *
         *  * [MotionEvent.ACTION_CANCEL]: We set the `alpha` of our [View] parameter [v] to 1f, the
         *  `translationX` of [v] to 0f, and our [Boolean] field [mItemPressed] to `false`
         *
         *  * [MotionEvent.ACTION_MOVE]: We initialize [Float] variable `val x` with the X coordinate
         *  of [event] plus the X translation of [View] parameter [v]. We set [Float] variable
         *  `val deltaX` to `x` minus [Float] field [mDownX], and [Float] variable `val deltaXAbs`
         *  to the absolute value of `deltaX`. If our [Boolean] field [mSwiping] is `false` (we are
         *  not already in the middle of a swipe) we check if `deltaXAbs` is greater than [Float]
         *  field [mSwipeSlop] and if so we set [mSwiping] to `true`, call the
         *  [ListView.requestDisallowInterceptTouchEvent] method of  method of [ListView] field
         *  [mListView] with `true` to prevent it from intercepting touch events until this one is
         *  over, and then call the [BackgroundContainer.showBackground] method of
         *  [mBackgroundContainer] to have it start to show through the [ListView] from the top Y
         *  coordinate of [View] paramter [v] for the height of [v]. Then if [mSwiping] is now
         *  `true` we set the `translationX` property of [v] to `x` minus [mDownX] and the `alpha`
         *  property of `x` to 1 minus the quantity `deltaXAbs` divided by the `width` of [v]
         *
         *  * [MotionEvent.ACTION_UP]: We branch on the value of our [Boolean] flag field [mSwiping]:
         *
         *  * `true`: (the item has been moved already) We set [Float] variable `val x` to the X
         *  coordinate of [MotionEvent] parameter [event] plus the X translation of [View] parameter
         *  [v]. We set [Float] variable `val deltaX` to `x` minus [Float] field [mDownX], and [Float]
         *  variable `val deltaXAbs` to the absolute value of `deltaX`. We declare [Float] variable
         *  `val fractionCovered`, [Float] variable `val endX`, [Float] variable `val endAlpha` and
         *  [Boolean] variable `val remove`. If `deltaXAbs` is greater than one quarter of the width
         *  of [View] parameter [v] we animate it off the screen by setting `fractionCovered` to
         *  `deltaXAbs` divided by the width of [v], setting `endX` to minus the width of [v] if
         *  `deltaX` is less than 0, or to the width of [v] if it is not, and setting `remove` to
         *  `true`. If it is less than a quarter off the screen we animate it back by setting
         *  `fractionCovered` to 1 minus `deltaXAbs` divided by the width of [v], setting `endX`
         *  to 0, and setting `remove` to `false`. In either case we set
         *  [Long] variable `val duration` to the quantity 1 minus `fractionCovered` times
         *  [SWIPE_DURATION] and disable [ListView] field [mListView]. We fetch a [ViewPropertyAnimator]
         *  for [v], set its duration to `duration`, have it cause the View's `alpha` property to be
         *  animated to `endAlpha`, have it cause its `translationX` property to be animated to
         *  `endX` and specifies a lambda to take place when the animation ends in which the `alpha`
         *  of [v] is set to 1f, its `translationX` to 0f, and if `remove` is `true` calls the
         *  [animateRemoval] method to animate the removal of [v] from [ListView] field [mListView],
         *  but when `remove` is `false` it call the [BackgroundContainer.hideBackground] method of
         *  [mBackgroundContainer], sets [mSwiping] to `false` and reenables [mListView]. When done
         *  we set our [Boolean] field [mItemPressed] to `false`
         *
         *  * default: We return `false`.
         *
         * If we reach the end of our `when` switch without returning we return `true` to consume
         * the event.
         *
         * @param v The [View] the touch event has been dispatched to.
         * @param event The [MotionEvent] object containing full information about the event.
         * @return `true` if the listener has consumed the event, `false` otherwise.
         */
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(this@ListViewRemovalAnimation).scaledTouchSlop
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (mItemPressed) {
                        // Multi-item swipes not handled
                        return false
                    }
                    mItemPressed = true
                    mDownX = event.x
                }

                MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    v.translationX = 0f
                    mItemPressed = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val x: Float = event.x + v.translationX
                    val deltaX: Float = x - mDownX
                    val deltaXAbs: Float = Math.abs(deltaX)
                    if (!mSwiping) {
                        if (deltaXAbs > mSwipeSlop) {
                            mSwiping = true
                            mListView!!.requestDisallowInterceptTouchEvent(true)
                            mBackgroundContainer!!.showBackground(v.top, v.height)
                        }
                    }
                    if (mSwiping) {
                        v.translationX = x - mDownX
                        v.alpha = 1 - deltaXAbs / v.width
                    }
                }

                MotionEvent.ACTION_UP -> {
                    run {
                        // User let go - figure out whether to animate the view out, or back into place
                        if (mSwiping) {
                            val x: Float = event.x + v.translationX
                            val deltaX: Float = x - mDownX
                            val deltaXAbs: Float = Math.abs(deltaX)
                            val fractionCovered: Float
                            val endX: Float
                            val endAlpha: Float
                            val remove: Boolean
                            if (deltaXAbs > v.width / 4f) {
                                // Greater than a quarter of the width - animate it out
                                fractionCovered = deltaXAbs / v.width
                                endX = (if (deltaX < 0) -v.width else v.width).toFloat()
                                endAlpha = 0f
                                remove = true
                            } else {
                                // Not far enough - animate it back
                                fractionCovered = 1 - deltaXAbs / v.width
                                endX = 0f
                                endAlpha = 1f
                                remove = false
                            }
                            // Animate position and alpha of swiped item
                            // NOTE: This is a simplified version of swipe behavior, for the
                            // purposes of this demo about animation. A real version should use
                            // velocity (via the VelocityTracker class) to send the item off or
                            // back at an appropriate speed.
                            val duration = ((1 - fractionCovered) * SWIPE_DURATION).toInt().toLong()
                            mListView!!.isEnabled = false
                            v.animate().setDuration(duration).alpha(endAlpha).translationX(endX).withEndAction {

                                // Restore animated values
                                v.alpha = 1f
                                v.translationX = 0f
                                if (remove) {
                                    animateRemoval(mListView, v)
                                } else {
                                    mBackgroundContainer!!.hideBackground()
                                    mSwiping = false
                                    mListView!!.isEnabled = true
                                }
                            }
                        }
                    }
                    mItemPressed = false
                }

                else -> return false
            }
            return true
        }
    }

    /**
     * This method animates all other views in the [ListView] container (not including [viewToRemove])
     * into their final positions. It is called after [viewToRemove] has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     *
     * @param listview the [ListView] that we are removing [viewToRemove] from.
     * @param viewToRemove the [View] that is being removed.
     */
    private fun animateRemoval(listview: ListView?, viewToRemove: View) {
        val firstVisiblePosition = listview!!.firstVisiblePosition
        for (i in 0 until listview.childCount) {
            val child = listview.getChildAt(i)
            if (child !== viewToRemove) {
                val position = firstVisiblePosition + i
                val itemId = mAdapter!!.getItemId(position)
                mItemIdTopMap[itemId] = child.top
            }
        }
        // Delete the item from the adapter
        val position = mListView!!.getPositionForView(viewToRemove)
        mAdapter!!.remove(mAdapter!!.getItem(position))
        val observer = listview.viewTreeObserver
        observer.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                observer.removeOnPreDrawListener(this)
                var firstAnimation = true
                val firstVisiblePositionLocal = listview.firstVisiblePosition
                for (i in 0 until listview.childCount) {
                    val child = listview.getChildAt(i)
                    val positionLocal = firstVisiblePositionLocal + i
                    val itemId = mAdapter!!.getItemId(positionLocal)
                    var startTop = mItemIdTopMap[itemId]
                    val top = child.top
                    if (startTop != null) {
                        if (startTop != top) {
                            val delta = startTop - top
                            child.translationY = delta.toFloat()
                            child.animate().setDuration(MOVE_DURATION.toLong()).translationY(0f)
                            if (firstAnimation) {
                                child.animate().withEndAction {
                                    mBackgroundContainer!!.hideBackground()
                                    mSwiping = false
                                    mListView!!.isEnabled = true
                                }
                                firstAnimation = false
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        val childHeight = child.height + listview.dividerHeight
                        startTop = top + if (i > 0) childHeight else -childHeight
                        val delta = startTop - top
                        child.translationY = delta.toFloat()
                        child.animate().setDuration(MOVE_DURATION.toLong()).translationY(0f)
                        if (firstAnimation) {
                            child.animate().withEndAction {
                                mBackgroundContainer!!.hideBackground()
                                mSwiping = false
                                mListView!!.isEnabled = true
                            }
                            firstAnimation = false
                        }
                    }
                }
                mItemIdTopMap.clear()
                return true
            }
        })
    }

    companion object {
        /**
         * Duration of animation when a swiped item is animated the rest of the way off the screen,
         * or back on the screen if the user lifts his finger before the 1/4 width threshhold.
         */
        private const val SWIPE_DURATION = 250

        /**
         * Duration of move animation of the views that remain after one is removed
         */
        private const val MOVE_DURATION = 150
    }
}