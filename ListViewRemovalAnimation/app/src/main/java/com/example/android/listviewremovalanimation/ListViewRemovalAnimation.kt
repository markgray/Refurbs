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
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.util.Collections

/**
 * This example shows what goes wrong when a view that is chosen to be deleted when that view is on
 * the screen is recycled when that view has been scrolled off the screen (the views that are animated
 * no longer contain the items that were selected to be deleted, but are animated anyway).
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [ListViewRemovalAnimation](https://www.youtube.com/watch?v=NewCSg2JKLk).
 */
class ListViewRemovalAnimation : ComponentActivity() {
    /**
     * The [StableArrayAdapter] we use for our [ListView]
     */
    var mAdapter: StableArrayAdapter? = null

    /**
     * The [ListView] in our layout with id `R.id.list_view` which displays our cheeses.
     */
    var mListView: ListView? = null

    /**
     * The [BackgroundContainer] in our layout with id `R.id.listViewBackground`
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
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_list_view_deletion`.
     *
     * We initialize our [LinearLayout] variable `rootView`
     * to the view with ID `R.id.root_view` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [BackgroundContainer] field [mBackgroundContainer] by finding the view with
     * id `R.id.listViewBackground`, and [ListView] field [mListView] by finding the view with id
     * `R.id.list_view`. We allocate a new instance of [ArrayList] of [String] to initialize variable
     * `val cheeseList` then add all the cheeses in the [Array] of [String] field
     * [Cheeses.sCheeseStrings] to it. We initialize our [StableArrayAdapter] field [mAdapter] with
     * a new instance which will use `cheeseList` as its dataset, displaying them using the layout
     * `R.layout.opaque_text_view` with our [OnTouchListener] field [mTouchListener] as its
     * [OnTouchListener]. Finally we set the adapter of [mListView] to be [mAdapter].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view_deletion)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
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
     * We initialize [Int] variable `val firstVisiblePosition` to the position within the adapter's
     * data set of [ListView] parameter [listview] of the first item displayed on screen. Then we
     * loop over [Int] variable `var i` for all of the [ListView.getChildCount] (kotlin `childCount`
     * property) children of [listview] setting [View] variable `val child` to the `i`'th child of
     * [listview]. Then is `child` is not equal to [viewToRemove] we initialize [Int] variable
     * `val position` to `firstVisiblePosition` plus `i`, and initialize [Long] variable `val itemId`
     * to the row id associated with the position `position` that is returned by the
     * [StableArrayAdapter.getItemId] method of [StableArrayAdapter] field [mAdapter]. We then store
     * `top` Y coordinate of `child` under the key `itemId` in our [HashMap] of [Long] to [Int] field
     * [mItemIdTopMap]. When done storing all of the `top` Y coordinates of all of the children apart
     * from [viewToRemove] we initialize [Int] variable `val position` to the position of [viewToRemove]
     * within the adapter's data set of [listview], then call the [StableArrayAdapter.remove] method
     * of [mAdapter] to have it remove the data object in position `position` from its dataset. We
     * initialize [ViewTreeObserver] variable `val observer` with the [ViewTreeObserver] for the
     * hierarchy of [listview]. Then we add an anonymous [OnPreDrawListener] to `observer` whose
     * [OnPreDrawListener.onPreDraw] override will be invoked when the view tree is about to be
     * drawn, and it will animate all of the children that remain after [viewToRemove] is removed
     * into their new positions.
     *
     * @param listview the [ListView] that we are removing [viewToRemove] from.
     * @param viewToRemove the [View] that is being removed.
     */
    private fun animateRemoval(listview: ListView?, viewToRemove: View) {
        val firstVisiblePosition: Int = listview!!.firstVisiblePosition
        for (i in 0 until listview.childCount) {
            val child: View = listview.getChildAt(i)
            if (child !== viewToRemove) {
                val position: Int = firstVisiblePosition + i
                val itemId: Long = mAdapter!!.getItemId(position)
                mItemIdTopMap[itemId] = child.top
            }
        }
        // Delete the item from the adapter
        val position: Int = mListView!!.getPositionForView(viewToRemove)
        mAdapter!!.remove(mAdapter!!.getItem(position))
        val observer: ViewTreeObserver = listview.viewTreeObserver
        observer.addOnPreDrawListener(object : OnPreDrawListener {
            /**
             * Callback method to be invoked when the view tree is about to be drawn. At this point,
             * all views in the tree have been measured and given a frame. Clients can use this to
             * adjust their scroll bounds or even to request a new layout before drawing occurs.
             *
             * First we remove ourselves as a [OnPreDrawListener] from [ViewTreeObserver] variable
             * `observer`, then we initialize our [Int] variable `val firstVisiblePositionLocal` to
             * the position within the adapter's data set of [ListView] parameter [listview] of the
             * first item displayed on screen. Then we loop over [Int] variable `var i` for all of
             * the [ListView.getChildCount] (kotlin `childCount` property) children of [listview]
             * setting [View] variable `val child` to the `i`'th child of [listview]. We then
             * initialize [Int] variable `val positionLocal` to `firstVisiblePositionLocal` plus `i`,
             * and initialize [Long] variable `val itemId` to the row id associated with the position
             * `positionLocal` that is returned by the [StableArrayAdapter.getItemId] method of
             * [StableArrayAdapter] field [mAdapter]. We initialze [Int] variable `var startTop`
             * to the [Int] stored under the key `itemId` in [HashMap] of [Long] to [Int] field
             * [mItemIdTopMap] (this the the `top` Y coordinate of `child` before [viewToRemove] was
             * removed). Then we initialize [Int] variable `val top` to the current `top` Y coordinate
             * of `child` (now that [viewToRemove] is gone). Now we branch on whether `startTop` is
             * `null`:
             *
             *  * `startTop` is NOT `null` (the `child` [View] was visible before [viewToRemove] was
             *  removed) if `startTop` is not equal to `top` we need to move the `child` [View] so
             *  we initialize our [Int] variable `val delta` to `startTop` minus `top`. Then we set
             *  the `translationY` property of `child` to `delta`, and add a [ViewPropertyAnimator]
             *  to `child` of duration [MOVE_DURATION] (150ms) which will animate the `translationY`
             *  property back to 0f (thus moving it to the position that `child` will be in when the
             *  [ListView] is drawn without [viewToRemove] in it). If `firstAnimation` is `true` we
             *  add [Runnable] to take place when the animation ends and in that [Runnable] we call
             *  the [BackgroundContainer.hideBackground] method of [mBackgroundContainer] to have it
             *  stop drawing its shadowed background, then we set [Boolean] field [mSwiping] to
             *  `false`, and enable [ListView] field [mListView]. Having added the end action we
             *  we `firstAnimation` to `false` to avoid adding the end action again.
             *
             *  * `startTop` is `null` (`child` is a new [View] which was not on the screen before
             *  [viewToRemove] was removed) We initialize our [Int] variable `val childHeight` to
             *  the `height` property of `child` plus the `dividerHeight` property of [ListView]
             *  parameter [listview]. Then if `i` is greater than 0 we set `startTop` to `top`
             *  plus `childHeight`, or the `top` minus `childHeight` if it is no greater than 0.
             *  Next we initialize our [Int] variable `val delta` to `startTop` minus `top` and set
             *  the `translationY` property of `child` to `delta`, and add a [ViewPropertyAnimator]
             *  to `child` of duration [MOVE_DURATION] (150ms) which will animate the `translationY`
             *  property back to 0f (thus moving it to the position that `child` will be in when the
             *  [ListView] is drawn without [viewToRemove] in it). If `firstAnimation` is `true` we
             *  add [Runnable] to take place when the animation ends and in that [Runnable] we call
             *  the [BackgroundContainer.hideBackground] method of [mBackgroundContainer] to have it
             *  stop drawing its shadowed background, then we set [Boolean] field [mSwiping] to
             *  `false`, and enable [ListView] field [mListView]. Having added the end action we
             *  we `firstAnimation` to `false` to avoid adding the end action again.
             *
             * Having animated the movement of all the other views when [viewToRemove] is removed
             * we call the `HashMap.clear` method of [HashMap] of [Long] to [Int] field [mItemIdTopMap]
             * to remove all of the mappings from the map, and return `true` to proceed with the
             * current drawing pass.
             *
             * @return `true` to proceed with the current drawing pass, or `false` to cancel.
             */
            override fun onPreDraw(): Boolean {
                observer.removeOnPreDrawListener(this)
                var firstAnimation = true
                val firstVisiblePositionLocal: Int = listview.firstVisiblePosition
                for (i in 0 until listview.childCount) {
                    val child: View = listview.getChildAt(i)
                    val positionLocal: Int = firstVisiblePositionLocal + i
                    val itemId: Long = mAdapter!!.getItemId(positionLocal)
                    var startTop: Int? = mItemIdTopMap[itemId]
                    val top: Int = child.top
                    if (startTop != null) {
                        if (startTop != top) {
                            val delta: Int = startTop - top
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
                        val childHeight: Int = child.height + listview.dividerHeight
                        startTop = top + if (i > 0) childHeight else -childHeight
                        val delta: Int = startTop - top
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
