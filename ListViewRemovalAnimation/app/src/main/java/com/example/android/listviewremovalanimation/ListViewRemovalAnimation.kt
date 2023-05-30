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
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.ListView
import java.util.Collections

/**
 * This example shows what goes wrong when a view that is chosen to be deleted when that view is on
 * the screen is recycled when that view has been scrolled off the screen (the views that are animated
 * no longer contain the items that were selected to be deleted, but are animated anyway).
 *
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [ListViewRemovalAnimation](https://www.youtube.com/watch?v=NewCSg2JKLk).
 */
class ListViewRemovalAnimation : Activity() {
    /**
     * The `StableArrayAdapter` we use for our `ListView`
     */
    var mAdapter: StableArrayAdapter? = null

    /**
     * The `ListView` in our layout with id R.id.list_view which displays our cheeses.
     */
    var mListView: ListView? = null

    /**
     * The `BackgroundContainer` in our layout with id R.id.listViewBackground
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
     * TODO: Add kdoc
     */
    @SuppressLint("UseSparseArrays")
    var mItemIdTopMap: HashMap<Long, Int> = HashMap()
    /**
     * TODO: Add kdoc
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view_deletion)
        mBackgroundContainer = findViewById(R.id.listViewBackground)
        mListView = findViewById(R.id.list_view)
        Log.d("Debug", "d=" + mListView!!.divider)
        val cheeseList = ArrayList<String>()
        Collections.addAll(cheeseList, *Cheeses.sCheeseStrings)
        mAdapter = StableArrayAdapter(this, R.layout.opaque_text_view, cheeseList,
            mTouchListener)
        mListView!!.adapter = mAdapter
    }

    /**
     * Handle touch events to fade/move dragged items as they are swiped out
     */
    private val mTouchListener: OnTouchListener = object : OnTouchListener {
        var mDownX = 0f
        private var mSwipeSlop = -1
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
                    val x = event.x + v.translationX
                    val deltaX = x - mDownX
                    val deltaXAbs = Math.abs(deltaX)
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
                            val x = event.x + v.translationX
                            val deltaX = x - mDownX
                            val deltaXAbs = Math.abs(deltaX)
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
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
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
        private const val SWIPE_DURATION = 250
        private const val MOVE_DURATION = 150
    }
}