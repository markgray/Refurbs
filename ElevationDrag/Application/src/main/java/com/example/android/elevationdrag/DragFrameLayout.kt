/*
 * Copyright (C) 2014 The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.elevationdrag

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper

/**
 * A [FrameLayout] that allows the user to drag and reposition child views. Perform inflation from
 * XML and apply a class-specific base style from a theme attribute or style resource. First we call
 * our super's constructor, then in our `init` block we initialize [MutableList] of [View] field
 * [mDragViews] with a new instance of [ArrayList]. We initialize our [ViewDragHelper] field
 * [mDragHelper] with a new instance created using this as the Parent view to monitor, a sensitivity
 * of 1.0f (normal) and a new anonymous [ViewDragHelper.Callback] whose overrides do what needs doing.
 *
 * @param context The [Context] the view is running in, through which it can access the current
 * theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 * resource that supplies default values for the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that supplies default values for
 * the view, used only if `defStyleAttr` is 0 or can not be found in the theme. Can be 0 to not
 * look for defaults.
 */
class DragFrameLayout @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context!!, attrs, defStyleAttr, defStyleRes) {
    /**
     * The list of [View]s that will be draggable.
     */
    @Suppress("JoinDeclarationAndAssignment") // Easier to debug and document this way
    private val mDragViews: MutableList<View>

    /**
     * The [DragFrameLayoutController] that will be notified on drag.
     */
    private var mDragFrameLayoutController: DragFrameLayoutController? = null

    /**
     * Our [ViewDragHelper]. A utility class for writing custom ViewGroups. It offers a number
     * of useful operations and state tracking for allowing a user to drag and reposition views
     * within their parent ViewGroup.
     */
    private val mDragHelper: ViewDragHelper

    init {
        mDragViews = ArrayList()

        /*
         * Create the `ViewDragHelper` and set its callback.
         */
        mDragHelper = ViewDragHelper.create(
            /* forParent = */ this,
            /* sensitivity = */ 1.0f,
            /* cb = */ object : ViewDragHelper.Callback() {
                /**
                 * Called when the user's input indicates that they want to capture the given child
                 * view with the pointer indicated by [Int] parameter [pointerId]. The callback should
                 * return `true` if the user is permitted to drag the given view with the indicated
                 * pointer. [ViewDragHelper] may call this method multiple times for the same view
                 * even if the view is already captured; this indicates that a new pointer is trying
                 * to take control of the view. If this method returns `true`, a call to
                 * [onViewCaptured] will follow if the capture is successful.
                 *
                 * We return true if our [MutableList] of [View] field [mDragViews] contains our [View]
                 * parameter [child].
                 *
                 * @param child Child [View] the user is attempting to capture
                 * @param pointerId ID of the pointer attempting the capture
                 * @return `true` if capture should be allowed, `false` otherwise
                 */
                override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                    return mDragViews.contains(child)
                }

                /**
                 * Called when the captured view's position changes as the result of a drag or settle.
                 * We just call our super's implementation of `onViewPositionChanged`.
                 *
                 * @param changedView [View] whose position changed
                 * @param left New X coordinate of the left edge of the view
                 * @param top New Y coordinate of the top edge of the view
                 * @param dx Change in X position from the last call
                 * @param dy Change in Y position from the last call
                 */
                @Suppress("RedundantOverride")
                override fun onViewPositionChanged(
                    changedView: View,
                    left: Int,
                    top: Int,
                    dx: Int,
                    dy: Int
                ) {
                    super.onViewPositionChanged(changedView, left, top, dx, dy)
                }

                /**
                 * Restrict the motion of the dragged child view along the horizontal axis.
                 * The default implementation does not allow horizontal motion; the extending
                 * class must override this method and provide the desired clamping. We just
                 * return our parameter [left], allowing full horizontal motion.
                 *
                 * @param child Child view being dragged
                 * @param left Attempted motion along the X axis
                 * @param dx Proposed change in position for left
                 * @return The new clamped position for left
                 */
                override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                    return left
                }

                /**
                 * Restrict the motion of the dragged child view along the vertical axis.
                 * The default implementation does not allow vertical motion; the extending
                 * class must override this method and provide the desired clamping. We just
                 * return our parameter [top], allowing full vertical motion.
                 *
                 * @param child Child view being dragged
                 * @param top Attempted motion along the Y axis
                 * @param dy Proposed change in position for top
                 * @return The new clamped position for top
                 */
                override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
                    return top
                }

                /**
                 * Called when a child view is captured for dragging or settling. The ID of the
                 * pointer currently dragging the captured view is supplied. If [Int] parameter
                 * [activePointerId] is identified as [ViewDragHelper.INVALID_POINTER] the capture
                 * is programmatic instead of pointer-initiated. We call our super's implementation
                 * of `onViewCaptured`, then if our [DragFrameLayoutController] field
                 * [mDragFrameLayoutController] is not `null` we call its
                 * [DragFrameLayoutController.onDragDrop] method with `true` to Animate the
                 * translationZ of the [View] by 50px to indicate that the view is captured. Note
                 * that the translation is being modified, not the elevation, the translationZ is
                 * added to the elevation.
                 *
                 * @param capturedChild Child [View] that was captured
                 * @param activePointerId Pointer id tracking the child capture
                 */
                override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
                    super.onViewCaptured(capturedChild, activePointerId)
                    if (mDragFrameLayoutController != null) {
                        mDragFrameLayoutController!!.onDragDrop(true)
                    }
                }

                /**
                 * Called when the child view is no longer being actively dragged. The fling velocity
                 * is also supplied, if relevant. The velocity values may be clamped to system
                 * minimums or maximums. Calling code may decide to fling or otherwise release the
                 * view to let it settle into place. It should do so using
                 * [ViewDragHelper.settleCapturedViewAt] or [ViewDragHelper.flingCapturedView].
                 * If the Callback invokes one of these methods, the [ViewDragHelper] will enter
                 * [ViewDragHelper.STATE_SETTLING] and the view capture will not fully end until it
                 * comes to a complete stop. If neither of these methods is invoked before
                 * [ViewDragHelper.Callback.onViewReleased] returns (as is our case), the view will
                 * stop in place and the [ViewDragHelper] will return to [ViewDragHelper.STATE_IDLE].
                 *
                 * We call our super's implementation of `onViewReleased`, then if our
                 * [DragFrameLayoutController] field [mDragFrameLayoutController] is not `null` we
                 * call its [DragFrameLayoutController.onDragDrop] method with `false` to Animate the
                 * translationZ of the [View] by 0 to indicate that the view has been dropped (it was
                 * +50px while being dragged). Note that the translation is being modified, not the
                 * elevation, the translationZ is added to the elevation.
                 *
                 * @param releasedChild The captured child [View] now being released
                 * @param xvel X velocity of the pointer as it left the screen in pixels per second.
                 * @param yvel Y velocity of the pointer as it left the screen in pixels per second.
                 */
                override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                    super.onViewReleased(releasedChild, xvel, yvel)
                    if (mDragFrameLayoutController != null) {
                        mDragFrameLayoutController!!.onDragDrop(false)
                    }
                }
            })
    }

    /**
     * Implement this method to intercept all touch screen motion events. This allows you to watch
     * events as they are dispatched to your children, and take ownership of the current gesture at
     * any point. We initialize [Int] variable `val action` with the masked off action of our
     * [MotionEvent] parameter [ev] and if `action` is [MotionEvent.ACTION_CANCEL] or
     * [MotionEvent.ACTION_UP] we call the [ViewDragHelper.cancel] method of our [ViewDragHelper]
     * field [mDragHelper] to cancel the drag and return `false` so that each following event (up
     * to and including the final up) will be delivered first here and then to the target's
     * [onTouchEvent]. If `action` is not [MotionEvent.ACTION_CANCEL] or [MotionEvent.ACTION_UP]
     * we return the value returned by the [ViewDragHelper.shouldInterceptTouchEvent] method of
     * [ViewDragHelper] field [mDragHelper] for the [MotionEvent] parameter [ev] which checks if
     * this event as provided our onInterceptTouchEvent should cause the us to intercept the touch
     * event stream.
     *
     * @param ev The motion event being dispatched down the hierarchy.
     * @return Return `true` to steal motion events from the children and have them dispatched
     * to this [ViewGroup] through [onTouchEvent]. The current target will receive an
     * [MotionEvent.ACTION_CANCEL] event, and no further messages will be delivered here.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action: Int = ev.actionMasked
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel()
            return false
        }
        return mDragHelper.shouldInterceptTouchEvent(ev)
    }

    /**
     * Implement this method to handle touch screen motion events. We call the
     * [ViewDragHelper.processTouchEvent] method of our [ViewDragHelper] field [mDragHelper] with
     * [MotionEvent] parameter [ev] and return `true` to consume the event here.
     *
     * @param ev The motion event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mDragHelper.processTouchEvent(ev)
        return true
    }

    /**
     * Adds a new [View] to the list of views that are draggable within the container. We just
     * add our [View] parameter [dragView] to our [MutableList] of [View] field [mDragViews].
     *
     * @param dragView the [View] to make draggable
     */
    fun addDragView(dragView: View) {
        mDragViews.add(dragView)
    }

    /**
     * Sets the [DragFrameLayoutController] that will receive the drag events. We just store
     * our [DragFrameLayoutController] parameter [dragFrameLayoutController] in our field
     * [mDragFrameLayoutController].
     *
     * @param dragFrameLayoutController a [DragFrameLayoutController] to use for
     * [DragFrameLayoutController.onDragDrop] callbacks.
     */
    fun setDragFrameController(dragFrameLayoutController: DragFrameLayoutController?) {
        mDragFrameLayoutController = dragFrameLayoutController
    }

    /**
     * A controller that will receive the drag events.
     */
    interface DragFrameLayoutController {
        /**
         * Handles `inDragDrop` events for our [ViewDragHelper] field [mDragHelper], override to
         * do whatever animations you wish to happen when our views are captured or dropped.
         *
         * @param captured `true` when called from `onViewCaptured` override of our [ViewDragHelper]
         * field [mDragHelper] (Called when a child view is captured for dragging or settling)
         * `false` when called from its `onViewReleased` override (Called when the child view is no
         * longer being actively dragged).
         */
        fun onDragDrop(captured: Boolean)
    }
}