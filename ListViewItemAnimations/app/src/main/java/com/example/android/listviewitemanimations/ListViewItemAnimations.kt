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
    "SameParameterValue",
    "ReplaceNotNullAssertionWithElvisReturn",
    "ReplaceJavaStaticMethodWithKotlinAnalog",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.listviewitemanimations

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
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
 * This example shows how to use a swipe effect to remove items from a [ListView], and how to use
 * animations to complete the swipe as well as to animate the other items in the list into their
 * final places. This code works on run times back to Gingerbread (Android 2.3), by using the
 * android.view.animation classes on earlier releases.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * [DevBytes](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0).
 * [ListViewItemAnimations](https://www.youtube.com/watch?v=PeuVuoa13S8&list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0&index=74&t=0s)
 */
@SuppressLint("UseSparseArrays")
class ListViewItemAnimations : ComponentActivity() {
    /**
     * [StableArrayAdapter] holding the list of cheeses for our [ListView]
     */
    var mAdapter: StableArrayAdapter? = null

    /**
     * [ListView] with id `R.id.list_view` in our layout which displays our cheeses.
     */
    var mListView: ListView? = null

    /**
     * [BackgroundContainer] with id `R.id.listViewBackground`, holds our [ListView]
     */
    var mBackgroundContainer: BackgroundContainer? = null

    /**
     * Flag indicating that the user is swiping an item displayed in our [ListView] - ACTION_MOVE
     */
    var mSwiping: Boolean = false

    /**
     * Flag indicating that the user has pressed an item displayed in our [ListView] - ACTION_DOWN
     */
    var mItemPressed: Boolean = false

    /**
     * Maps the item id of a child of our [ListView] to its top Y coordinate (does not include
     * the item being removed).
     */
    var mItemIdTopMap: HashMap<Long, Int> = HashMap()

    /**
     * Flag indicating that the animation of a swipe is in progress.
     */
    var mAnimating: Boolean = false

    /**
     * Current X position of the swipe motion, used only for Gingerbread api
     */
    var mCurrentX: Float = 0f

    /**
     * Current alpha value of the swiped view, used only for Gingerbread api
     */
    var mCurrentAlpha: Float = 1f

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.activity_list_view_item_animations`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [BackgroundContainer] field [mBackgroundContainer] by finding the view with
     * id `R.id.listViewBackground`, and [ListView] field [mListView] by finding the view with id
     * `R.id.list_view`. We allocate a new instance for [ArrayList] of [String] to initialize variable
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
        setContentView(R.layout.activity_list_view_item_animations)
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
        val cheeseList = ArrayList<String>()
        Collections.addAll(cheeseList, *Cheeses.sCheeseStrings)
        mAdapter = StableArrayAdapter(
            context = this,
            textViewResourceId = R.layout.opaque_text_view,
            objects = cheeseList,
            mTouchListener = mTouchListener
        )
        mListView!!.adapter = mAdapter
    }

    /**
     * Returns true if the current runtime is Honeycomb or later
     */
    @get:SuppressLint("ObsoleteSdkInt") // Left in to remind us when reusing the code.
    private val isRuntimePostGingerbread: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB

    /**
     * [OnTouchListener] used for every view that the [StableArrayAdapter.getView] override of our
     * [StableArrayAdapter] returns to its caller.
     */
    private val mTouchListener: OnTouchListener = object : OnTouchListener {
        /**
         * X coordinate of the initial ACTION_DOWN event we received for the "swiping" that is
         * currently being done.
         */
        var mDownX: Float = 0f

        /**
         * Distance in pixels a touch can wander before we think the user is scrolling, set by a
         * call to the [ViewConfiguration.getScaledTouchSlop] method of the [ViewConfiguration]
         * of our current [ListViewItemAnimations] instance.
         */
        private var mSwipeSlop: Int = -1

        /**
         * Called when a touch event is dispatched to a view. If our [Int] field [mSwipeSlop] is
         * less than 0 we initialize it to the value returned by the
         * [ViewConfiguration.getScaledTouchSlop] method of the [ViewConfiguration] of our current
         * [ListViewItemAnimations] instance. Then we switch on the action of our [MotionEvent]
         * parameter [event]:
         *
         *  * [MotionEvent.ACTION_DOWN]: if our [Boolean] flag field [mAnimating] is `true` we
         *  return true having done nothing (we are already responding to a swipe, and multi-item
         *  swipes are not handled). Otherwise we set our [Boolean] flag field [mItemPressed] to
         *  `true`, and set [Float] field [mDownX] to the X coordinate of [event].
         *
         *  * [MotionEvent.ACTION_CANCEL]: We call our method [setSwipePosition] to set the X
         *  translation of [View] parameter [v] to 0, and set our [Boolean] field [mItemPressed]
         *  to `false`.
         *
         *  * [MotionEvent.ACTION_MOVE]: if our [Boolean] field [mAnimating] is `true` we return
         *  `true` having done nothing (I do not see how this can happen, but if you are fast enough
         *  I suppose you could conceivably start another swipe while the animation of a previous
         *  one is in progress, but since the [ListView] is disabled and un-clickable during the
         *  animation I do not think it possible). Otherwise we initialize [Float] variable
         *  `val x` with the X coordinate of [event] and if we are running on a device newer than
         *  Gingerbread we add the X translation of [View] parameter [v] to `x`. We set [Float]
         *  variable `val deltaX` to `x` minus [Float] field [mDownX], and [Float] variable
         *  `val deltaXAbs` to the absolute value of `deltaX`. If our [Boolean] field [mSwiping]
         *  is `false` (we are not already in the middle of a swipe) we check if `deltaXAbs` is
         *  greater than [Float] field [mSwipeSlop] and if so we set [mSwiping] to `true`, call the
         *  [ListView.requestDisallowInterceptTouchEvent] method of  method of [ListView] field
         *  [mListView] with `true` to prevent it from intercepting touch events until this one is
         *  over, and then call the [BackgroundContainer.showBackground] method of
         *  [mBackgroundContainer] to have it start to show through the [ListView] from the top Y
         *  coordinate of [View] paramter [v] for the height of [v]. Then if [mSwiping] is now
         *  `true` we call our [setSwipePosition] method to translate the X position on the screen
         *  of [v] to `deltaX`.
         *
         *  * [MotionEvent.ACTION_UP]: if our [Boolean] field [mAnimating] is `true` we return
         *  `true` having done nothing (we are already animating the movement of a view off or
         *  back onto the screen). Otherwise we branch on the value of our [Boolean] flag field
         *  [mSwiping]:
         *
         *  * `true`: (the item has been moved already) We set [Float] variable `val x` to the X
         *  coordinate of [MotionEvent] parameter [event] and if our device is post Gingerbread we
         *  add the X translation of [View] parameter [v] to `x`. We set [Float] variable
         *  `val deltaX` to `x` minus [Float] field [mDownX], and [Float] variable `val deltaXAbs`
         *  to the absolute value of `deltaX`. We declare [Float] variable `val fractionCovered`,
         *  [Float] variable `val endX`, and [Boolean] variable `val remove`. If `deltaXAbs` is
         *  greater than one quarter of the width of [View] parameter [v] we animate it off the
         *  screen by setting `fractionCovered` to `deltaXAbs` divided by the width of [v], setting
         *  `endX` to minus the width of [v] if `deltaX` is less than 0, or to the width of [v] if
         *  it is not, and setting `remove` to `true`. If it is less than a quarter off the screen
         *  we animate it back by setting `fractionCovered` to 1 minus `deltaXAbs` divided by the
         *  width of [v], setting `endX` 0, and setting `remove` to `false`. In either case we set
         *  [Long] variable `val duration` to the quantity 1 minus `fractionCovered` times
         *  [SWIPE_DURATION] then call our [animateSwipe] method to animate the X coordinate of [v]
         *  to `endX` with a duration of `duration`, and remove it if our flag `remove` is true.
         *
         *  * `false`: (the item has not been moved yet) We set our [Boolean] flag field
         *  [mItemPressed] to `false`.
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
        @SuppressLint("NewApi", "ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(this@ListViewItemAnimations).scaledTouchSlop
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (mAnimating) {
                        // Multi-item swipes not handled
                        return true
                    }
                    mItemPressed = true
                    mDownX = event.x
                }

                MotionEvent.ACTION_CANCEL -> {
                    setSwipePosition(v, 0f)
                    mItemPressed = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mAnimating) {
                        return true
                    }
                    var x: Float = event.x
                    if (isRuntimePostGingerbread) {
                        x += v.translationX
                    }
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
                        setSwipePosition(v, deltaX)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (mAnimating) {
                        return true
                    }
                    // User let go - figure out whether to animate the view out, or back into place
                    if (mSwiping) {
                        var x: Float = event.x
                        if (isRuntimePostGingerbread) {
                            x += v.translationX
                        }
                        val deltaX: Float = x - mDownX
                        val deltaXAbs: Float = Math.abs(deltaX)
                        val fractionCovered: Float
                        val endX: Float
                        val remove: Boolean
                        if (deltaXAbs > v.width / 4f) {
                            // Greater than a quarter of the width - animate it out
                            fractionCovered = deltaXAbs / v.width
                            endX = (if (deltaX < 0) -v.width else v.width).toFloat()
                            remove = true
                        } else {
                            // Not far enough - animate it back
                            fractionCovered = 1 - (deltaXAbs / v.width)
                            endX = 0f
                            remove = false
                        }
                        // Animate position and alpha
                        val duration: Long = ((1 - fractionCovered) * SWIPE_DURATION).toInt().toLong()
                        animateSwipe(
                            view = v,
                            endX = endX,
                            duration = duration,
                            remove = remove
                        )
                    } else {
                        mItemPressed = false
                    }
                }

                else -> return false
            }
            return true
        }
    }

    /**
     * Animates a swipe of the item either back into place or out of the [ListView] container.
     * *NOTE:* This is a simplified version of swipe behavior, for the purposes of this demo
     * about animation. A real version should use velocity (via the `VelocityTracker` class)
     * to send the item off or back at an appropriate speed. First we set our [Boolean] field
     * [mAnimating] to `true`, then we disable our [ListView] field [mListView]. We then branch on
     * whether our device is running Gingerbread or newer:
     *
     *  * Post Gingerbread: We obtain a [ViewPropertyAnimator] for [View] parameter [view], set its
     *  duration to our [Long] parameter [duration], cause it to animate the alpha of [view] to 0 if
     *  [Boolean] parameter [remove] is `true` or to 1 if it is `false`, cause it to animate the X
     *  translation to [Float] parameter [endX] and set its [AnimatorListener] to an anonymous
     *  [AnimatorListenerAdapter] whose [AnimatorListenerAdapter.onAnimationEnd] override sets the
     *  alpha of [view] back to 1, and its X translation to 0. Then if [remove] is `true` calls our
     *  [animateOtherViews] method to animate the other views in the [ListView] container (not
     *  including [view]) into their final positions. If [remove] is false we call the
     *  [BackgroundContainer.hideBackground] method of [mBackgroundContainer] to hide the background
     *  again, set the [Boolean] flag fields [mSwiping], and [mAnimating] to `false` and enable
     *  [ListView] field [mListView] again. In either case we then set [Boolean] field [mItemPressed]
     *  to `false`.
     *
     *  * Before Gingerbread: We initialize [TranslateAnimation] variable `val swipeAnim` with an
     *  instance which will animate the X coordinate from [Float] field [mCurrentX] to [Float]
     *  parameter [endX] with no change to the Y coordinate. We initialize [AlphaAnimation] variable
     *  `val alphaAnim` with an instance which will animate the alpha from [Float] field
     *  [mCurrentAlpha] to 0 if [Boolean] parameter [remove] is `true` or to 1 if it is `false`. We
     *  initialize [AnimationSet] variable `val set` with an instance which will share the
     *  interpolator with all the animations in it. We then add `swipeAnim`, and `alphaAnim` to
     *  `set`, set its duration to [Long] parameter [duration], and start it running now on [view].
     *  We then call our [setAnimationEndAction] method to set the [AnimationListener] of `set` to
     *  an anonymous [AnimationListenerAdapter] whose [AnimationListenerAdapter.onAnimationEnd]
     *  override runs an anonymous [Runnable] we pass it which if [remove] is `true` calls
     *  [animateOtherViews] to animate the other views in [mListView] to the positions they should
     *  occupy now that [view] has been removed, and if [remove] is `false` calls the
     *  [BackgroundContainer.hideBackground] method of [mBackgroundContainer] to hide it, sets
     *  [mSwiping] and [mAnimating] both to `false` and re-enables [mListView]. In either case it
     *  then sets [mItemPressed] to `false`.
     *
     * @param view the [View] that is being swiped
     * @param endX the X coordinate that it is being swiped to (0 or plus or minus the width of view)
     * @param duration duration in milliseconds of the animation
     * @param remove `true` if the view is being swiped out of the [ListView], `false` if back into it
     */
    @SuppressLint("NewApi")
    private fun animateSwipe(view: View, endX: Float, duration: Long, remove: Boolean) {
        mAnimating = true
        mListView!!.isEnabled = false
        if (isRuntimePostGingerbread) {
            view.animate().setDuration(duration).alpha((if (remove) 0 else 1).toFloat())
                .translationX(endX)
                .setListener(object : AnimatorListenerAdapter() {
                    /**
                     * Notifies the end of the animation. First we restore the animated values by
                     * setting the alpha of `view` to 1 and its X translation to 0. Then we
                     * branch on the value of `remove`:
                     *
                     *  * `true`: (`view` was removed) We call our method [animateOtherViews]
                     *  to remove the item displayed in `view` from our adapter and animate the
                     *  other views into place to close the gap left where `view` used to be.
                     *
                     *  * `false`: (`view` was animated back to its original position) we call the
                     *  [BackgroundContainer.hideBackground] method of [mBackgroundContainer] to
                     *  have it hide the area underneath `view`, set [Boolean] fields [mSwiping]
                     *  and [mAnimating] to `false` and enable our [ListView] field [mListView].
                     *
                     * In both cases we set our [Boolean] flag field [mItemPressed] to `false`.
                     *
                     * @param animation The animation which reached its end.
                     */
                    override fun onAnimationEnd(animation: Animator) {
                        // Restore animated values
                        view.alpha = 1f
                        view.translationX = 0f
                        if (remove) {
                            animateOtherViews(mListView, view)
                        } else {
                            mBackgroundContainer!!.hideBackground()
                            mSwiping = false
                            mAnimating = false
                            mListView!!.isEnabled = true
                        }
                        mItemPressed = false
                    }
                })
        } else {
            val swipeAnim = TranslateAnimation(
                /* fromXDelta = */ mCurrentX,
                /* toXDelta = */ endX,
                /* fromYDelta = */ 0f,
                /* toYDelta = */ 0f
            )
            val alphaAnim = AlphaAnimation(
                /* fromAlpha = */ mCurrentAlpha,
                /* toAlpha = */ (if (remove) 0 else 1).toFloat()
            )
            val set = AnimationSet(true)
            set.addAnimation(swipeAnim)
            set.addAnimation(alphaAnim)
            set.duration = duration
            view.startAnimation(set)
            setAnimationEndAction(set) {
                if (remove) {
                    animateOtherViews(listView = mListView, viewToRemove = view)
                } else {
                    mBackgroundContainer!!.hideBackground()
                    mSwiping = false
                    mAnimating = false
                    mListView!!.isEnabled = true
                }
                mItemPressed = false
            }
        }
    }

    /**
     * Sets the horizontal position and translucency of the view being swiped. We set [Float] variable
     * `val fraction` to the absolute value of [Float] parameter [deltaX] divided by the width of
     * [View] parameter [view] then branch on the SDK version of the device we are running on:
     *
     *  * Post Gingerbread: We translate the X position of [view] by [deltaX] and set its alpha to
     *  1 minus `fraction`
     *
     *  * Gingerbread (or earlier): We initialize [TranslateAnimation] variable `val swipeAnim` with
     *  an instance which will move the X coordinate to [deltaX] without moving the Y coordinate. We
     *  then set [Float] field [mCurrentX] to [deltaX] and [Float] field [mCurrentAlpha] to 1 minus
     *  `fraction`. We initialize [AlphaAnimation] variable `val alphaAnim` to an instance which will
     *  animate alpha to [mCurrentAlpha], and initialize [AnimationSet] variable `val set` to an
     *  instance which will share its interpolator between its animations. We add `swipeAnim` and
     *  `alphaAnim` to `set`, call its [AnimationSet.setFillAfter] method with `true` (kotlin
     *  `fillAfter` property) so that transformation that this animation performs will persist when
     *  it is finished, and call its [AnimationSet.setFillEnabled] method with `true` (kolin
     *  `isFillEnabled` property) so that the animation will take the [Boolean] value set by a call
     *  to [AnimationSet.setFillBefore] into account (the start value of the animation will not be
     *  set). Finally we start `set` running on [View] parameter [view].
     *
     * @param view the [View] that is being swiped.
     * @param deltaX How far [View] parametr [view] has moved from its original position.
     */
    @SuppressLint("NewApi")
    private fun setSwipePosition(view: View, deltaX: Float) {
        val fraction: Float = Math.abs(deltaX) / view.width
        if (isRuntimePostGingerbread) {
            view.translationX = deltaX
            view.alpha = 1 - fraction
        } else {
            // Hello, Gingerbread!
            val swipeAnim = TranslateAnimation(
                /* fromXDelta = */ deltaX,
                /* toXDelta = */ deltaX,
                /* fromYDelta = */ 0f,
                /* toYDelta = */ 0f
            )
            mCurrentX = deltaX
            mCurrentAlpha = (1 - fraction)
            val alphaAnim = AlphaAnimation(
                /* fromAlpha = */ mCurrentAlpha,
                /* toAlpha = */ mCurrentAlpha
            )
            val set = AnimationSet(/* shareInterpolator = */ true)
            set.addAnimation(swipeAnim)
            set.addAnimation(alphaAnim)
            set.fillAfter = true
            set.isFillEnabled = true
            view.startAnimation(set)
        }
    }

    /**
     * TODO: Continue here.
     * This method animates all other views in the [ListView] parameter [listView] container (not
     * including [View] parameter [viewToRemove]) into their final positions. It is called before
     * [viewToRemove] has been removed from the adapter, and before layout has been run. The approach
     * here is to figure out where everything is now, remove [viewToRemove] from the adapter, allow
     * layout to run, figure out where everything is after layout, and then to run animations between
     * all of those start/end positions. First we initialize [Int] variable `val firstVisiblePosition`
     * with the position of the first visible view in our [ListView] parameter [listView]. Then we
     * loop over [Int] variable `var i` for all the children in [listView], setting [View] variable
     * `val child` to the child view at position `i`, setting [Int] variable `val position` to
     * `firstVisiblePosition` plus `i`, and setting [Long] variable `val itemId` to the item id of
     * the item at position `position` in our [StableArrayAdapter] field [mAdapter]. Then if `child`
     * is not equal to our [View] parameter [viewToRemove] we add the top Y coordinate of `child` to
     * our [HashMap] of [Long] to [Int] field [mItemIdTopMap] under the key `itemId` and loop back
     * to try the next child.
     *
     * When done locating the top Y coordinates of the children that will remain, we set [Int]
     * variable `val position` to the position in [ListView] field [mListView] that [View] parameter
     * [viewToRemove] occupies, then remove the item displayed there from [StableArrayAdapter] field
     * [mAdapter]. We then initialize [ViewTreeObserver] variable `val observer` with the
     * [ViewTreeObserver] of [ListView] parameter [listView] and add an anonymous [OnPreDrawListener]
     * to it whose [OnPreDrawListener.onPreDraw] override animates the remaining views to close the
     * gap left when [viewToRemove] was removed.
     *
     * @param listView the [ListView] whose views we are animating.
     * @param viewToRemove the [View] containing the item being removed.
     */
    private fun animateOtherViews(listView: ListView?, viewToRemove: View) {
        val firstVisiblePosition: Int = listView!!.firstVisiblePosition
        for (i in 0 until listView.childCount) {
            val child: View = listView.getChildAt(i)
            val position: Int = firstVisiblePosition + i
            val itemId: Long = mAdapter!!.getItemId(position)
            if (child !== viewToRemove) {
                mItemIdTopMap[itemId] = child.top
            }
        }
        // Delete the item from the adapter
        val position: Int = mListView!!.getPositionForView(viewToRemove)
        mAdapter!!.remove(mAdapter!!.getItem(position))

        // After layout runs, capture position of all itemIDs, compare to pre-layout
        // positions, and animate changes
        val observer: ViewTreeObserver = listView.viewTreeObserver
        observer.addOnPreDrawListener(object : OnPreDrawListener {
            /**
             * Callback method to be invoked when the view tree is about to be drawn. At this point,
             * all views in the tree have been measured and given a frame. First we remove ourselves
             * as a [OnPreDrawListener]. We initialize [Boolean] variable `var firstAnimation` to
             * `true`, and initialize [Int] variable `val firstVisiblePositionLocal` with the position
             * in the adapter of the first visible item in [listView]. We then loop over [Int] variable
             * `var i` for all the children in [listView]:
             *
             *  * We initialize [View] variable `val child` with the `i`'th child of [listView],
             *  initialize [Int] variable `val positionLocal` to `firstVisiblePositionLocal` plus
             *  `i`, initialize [Long] variable `val itemId` with the item id of the item at position
             *  `positionLocal` in [StableArrayAdapter] field [mAdapter], initialize [Int] variable
             *  `var startTop` to the value stored under the key `itemId` in [HashMap] of [Long] to
             *  [Int] field [mItemIdTopMap], and initialize [Int] variable `val top` with the top Y
             *  coordinate of `child`.
             *
             *  * If `startTop` is `null` the child is a new view so we initialize [Int] variable
             *  `val childHeight` to the height of `child` plus the divider height used by `listView`.
             *  We then set `startTop` to `top` plus `childHeight` if `i` is greater than 0 or to
             *  `top` minus `childHeight` if it is zero.
             *
             *  * We then initialize [Int] variable `val delta` to `startTop` minus `top` and if
             *  `delta` is not equal to 0 we need to move the child:
             *
             *  * If `firstAnimation` is `true` we initialize [Runnable] variable `val endAction`
             *  with an anonymous [Runnable] whose [Runnable.run] override hides the background of
             *  [BackgroundContainer] field [mBackgroundContainer], sets [Boolean] fields [mSwiping]
             *  and [mAnimating] to `false` and enables [ListView] field [mListView]. If it is `false`
             *  we initialize `endAction` to null.
             *
             *  * We set `firstAnimation` to `false`
             *
             *  * We call our [moveView] method to animate the translation of `child` from `delta`
             *  to 0 in its Y coordinate, running `endAction` when done if it is not `null`.
             *
             * Having animated the child views into their new position we clear [HashMap] of [Long]
             * to [Int] field [mItemIdTopMap] and return `true` to the caller to proceed with the
             * current drawing pass.
             *
             * @return Return `true` to proceed with the current drawing pass, or `false` to cancel.
             */
            override fun onPreDraw(): Boolean {
                observer.removeOnPreDrawListener(this)
                var firstAnimation = true
                val firstVisiblePositionLocal: Int = listView.firstVisiblePosition
                for (i in 0 until listView.childCount) {
                    val child: View = listView.getChildAt(i)
                    val positionLocal: Int = firstVisiblePositionLocal + i
                    val itemId: Long = mAdapter!!.getItemId(positionLocal)
                    var startTop: Int? = mItemIdTopMap[itemId]
                    val top: Int = child.top
                    if (startTop == null) {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on whether they're coming in from the bottom (i > 0) or top.
                        val childHeight: Int = child.height + listView.dividerHeight
                        startTop = top + (if (i > 0) childHeight else -childHeight)
                    }
                    val delta: Int = startTop - top
                    if (delta != 0) {
                        @Suppress("ObjectLiteralToLambda")
                        val endAction: Runnable? = if (firstAnimation) {
                            object : Runnable {
                                /**
                                 * This `Runnable` is used to return our `ListView` to normalcy
                                 * after the removal of an item is animated. First we call the `hideBackground`
                                 * method of `mBackgroundContainer`, then we set `mSwiping` and
                                 * `mAnimating` to false and enable `mListView`.
                                 */
                                override fun run() {
                                    mBackgroundContainer!!.hideBackground()
                                    mSwiping = false
                                    mAnimating = false
                                    mListView!!.isEnabled = true
                                }
                            }
                        } else null
                        firstAnimation = false
                        moveView(child, 0f, 0f, delta.toFloat(), 0f, endAction)
                    }
                }
                mItemIdTopMap.clear()
                return true
            }
        })
    }

    /**
     * Animate a view between start and end X/Y locations, using either old (pre-3.0) or new animation
     * APIs. We copy our [Runnable] parameter [endAction] to initialize our [Runnable] variables
     * `var endActionLocal` and  `val finalEndAction`. We branch on whether we are running on a post
     * Gingerbread device or not:
     *
     *  * Post Gingerbread device: we set the duration of the underlying animator that animates
     *  properties of [View] parameter [view] to MOVE_DURATION (150ms) (Uh? this does nothing?) If
     *  our [Float] parametr [startX] is not equal to our parameter [Float] parameter [endX] (never
     *  happens?) we initialize [ObjectAnimator] variable `val anim` with an instance which will
     *  animate the TRANSLATION_X property of [view] from [startX] to [endX], set its duration to
     *  [MOVE_DURATION], start it running and call our [setAnimatorEndAction] method to set the
     *  [AnimatorListener] of `anim` to `endActionLocal` if it is not `null`, then we set
     *  `endActionLocal` to `null`. If our [Float] parameter [startY] is not equal to our [Float]
     *  parameter [endY] we initialize [ObjectAnimator] `val anim` with an instance which will
     *  animate the TRANSLATION_Y property of [view] from [startY] to [endY], set its duration to
     *  [MOVE_DURATION], start it running and call our [setAnimatorEndAction] method to set the
     *  [AnimatorListener] of `anim` to `endActionLocal` if it is not null.
     *
     *  * Gingerbread (or earlier?) device: We initialize [TranslateAnimation] variable`val translator`
     *  with an instance which will translate from [startX] to [endX] in the X dimension and from
     *  [startY] to [endY] in the Y dimension, set its duration to [MOVE_DURATION] and instruct
     *  [view] to start the animation. Then if `endActionLocal` is not `null` we fetch the animation
     *  currently associated with [view] and set its [AnimationListener] to an anonymous
     *  [AnimationListenerAdapter] whose [AnimationListenerAdapter.onAnimationEnd] override runs
     *  `finalEndAction` by calling its [Runnable.run] override.
     *
     * @param view   the [View] that is to be moved
     * @param startX starting X coordinate
     * @param endX   ending  X coordinate
     * @param startY starting Y coordinate
     * @param endY   ending  Y coordinate
     * @param endAction [Runnable] to run when the animation is done.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("NewApi")
    private fun moveView(
        view: View,
        startX: Float,
        endX: Float,
        startY: Float,
        endY: Float,
        endAction: Runnable?
    ) {
        var endActionLocal: Runnable? = endAction
        val finalEndAction: Runnable? = endActionLocal
        if (isRuntimePostGingerbread) {
            view.animate().duration = MOVE_DURATION.toLong()
            if (startX != endX) {
                val anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startX, endX)
                anim.duration = MOVE_DURATION.toLong()
                anim.start()
                setAnimatorEndAction(anim, endActionLocal)
                endActionLocal = null
            }
            if (startY != endY) {
                val anim: ObjectAnimator =
                    ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
                anim.duration = MOVE_DURATION.toLong()
                anim.start()
                setAnimatorEndAction(anim, endActionLocal)
            }
        } else {
            val translator = TranslateAnimation(startX, endX, startY, endY)
            translator.duration = MOVE_DURATION.toLong()
            view.startAnimation(translator)
            if (endActionLocal != null) {
                view.animation.setAnimationListener(object : AnimationListenerAdapter() {
                    /**
                     * Notifies the end of the animation. We just call the `run` method of
                     * `Runnable finalEndAction`.
                     *
                     * @param animation The animation which reached its end.
                     */
                    override fun onAnimationEnd(animation: Animation) {
                        finalEndAction!!.run()
                    }
                })
            }
        }
    }

    /**
     * If our [Runnable] parameter [endAction] is not `null` we add an anonymous [AnimatorListener]
     * to our [Animator] parameter [animator] which will call the [Runnable.run] override of
     * [endAction].
     *
     * @param animator the [Animator] whose [AnimatorListener] list we wish to add to
     * @param endAction the [Runnable] to run in the [AnimatorListener.onAnimationEnd] override of
     * anonymous [AnimatorListener] we add to [animator]
     */
    @SuppressLint("NewApi")
    private fun setAnimatorEndAction(animator: Animator, endAction: Runnable?) {
        if (endAction != null) {
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    endAction.run()
                }
            })
        }
    }

    /**
     * If our [Runnable] parameter [endAction] is not `null` we set an anonymous [AnimatorListener]
     * on our [Animation] parameter [animation] which will call the [Runnable.run] override of our
     * parameter [endAction].
     *
     * @param animation the [Animation] whose [AnimatorListener] list we wish to add to
     * @param endAction the [Runnable] to run in the [AnimatorListener.onAnimationEnd] override of
     * the anonymous [AnimatorListener] we add to [animator]
     */
    private fun setAnimationEndAction(animation: Animation, endAction: Runnable?) {
        if (endAction != null) {
            animation.setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation) {
                    endAction.run()
                }
            })
        }
    }

    /**
     * Utility, to avoid having to implement every method in [AnimationListener] in
     * every implementation class
     */
    internal open class AnimationListenerAdapter : AnimationListener {
        /**
         * Notifies the end of the animation.
         *
         * @param animation The animation which reached its end.
         */
        override fun onAnimationEnd(animation: Animation) {}

        /**
         * Notifies the repetition of the animation.
         *
         * @param animation The animation which was repeated.
         */
        override fun onAnimationRepeat(animation: Animation) {}

        /**
         * Notifies the start of the animation.
         *
         * @param animation The started animation.
         */
        override fun onAnimationStart(animation: Animation) {}
    }

    companion object {
        /**
         * Value used to calculate the duration of the X translation animation when animating the
         * swiping out of a view when the user released the view with more than a quarter of its
         * width off the screen or its return to the list when the user released the view before
         * a quarter of its width.
         */
        private const val SWIPE_DURATION = 250

        /**
         * Duration of the animation used to move up the items that are below the item removed
         */
        private const val MOVE_DURATION = 150
    }
}
