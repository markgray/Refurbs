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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "JoinDeclarationAndAssignment", "ReplaceJavaStaticMethodWithKotlinAnalog",
    "UnusedImport"
)

package com.example.android.expandingcells

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.core.view.size

/**
 * A custom [ListView] which supports the preview of extra content corresponding to each cell
 * by clicking on the cell to hide and show the extra content.
 */
class ExpandingListView : ListView {
    /**
     * Flag used by the [OnPreDrawListener.onPreDraw] override of our [OnPreDrawListener] to
     * determine if this is the first or second pass (the [OnPreDrawListener] should be removed on
     * the second pass so that the method does not keep getting called -- the two passes are used
     * for different stages of the animation).
     */
    private var mShouldRemoveObserver: Boolean = false

    /**
     * List of child views which were on the screen when a view is either expanded or collapsed but
     * which are no longer on the screen (we need this list to animate their movement when their
     * sibling changes size).
     */
    private val mViewsToDraw: MutableList<View> = ArrayList()

    /**
     * Array of top and bottom bound changes for the items on the screen.
     */
    private lateinit var mTranslate: IntArray

    /**
     * Our one argument constructor. First we call or super's constructor, then we call our method
     * [initialize] to set our [OnItemClickListener] to our [OnItemClickListener] field
     * [mItemClickListener]. UNUSED
     *
     * @param context The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     */
    constructor(context: Context?) : super(context) {
        initialize()
    }

    /**
     * Perform inflation from XML. First we call or super's constructor, then we call our method
     * [initialize] to set our [OnItemClickListener] to our [OnItemClickListener] field
     * [mItemClickListener]. This is the constructor that is used.
     *
     * @param context The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. This constructor of [View] allows subclasses to use their own base style when
     * they are inflating. First we call or super's constructor, then we call our method [initialize]
     * to set our [OnItemClickListener] to our [OnItemClickListener] field [mItemClickListener].
     * UNUSED
     *
     * @param context The [Context] the [View] is running in, through which it can access the
     * current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     * resource that supplies default values for the view. Can be 0 to not look for defaults.
     */
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        initialize()
    }

    /**
     * Convenience method used by our constructors to set our [OnItemClickListener] to our
     * [OnItemClickListener] field [mItemClickListener].
     */
    private fun initialize() {
        onItemClickListener = mItemClickListener
    }

    /**
     * Listens for item clicks and expands or collapses the selected view depending on its current
     * state. Its [OnItemClickListener.onItemClick] lambda is invoked when an item in our [ListView]
     * has been clicked. First we initialize [ExpandableListItem] variable `val viewObject` with the
     * data associated with the the position within the adapter's data set that is displayed in our
     * [View] parameter `view`. Then if the [ExpandableListItem.isExpanded] method of `viewObject`
     * returns `false` (the view is collapsed) we call our method [expandView] to expand the view,
     * and if it returns `true` (the view is expanded) we call our method [collapseView] to collapse
     * the view.
     *
     * *Parameter* parent The AdapterView where the click happened.
     *
     * *Parameter* view The view within the AdapterView that was clicked (this
     * will be a view provided by the adapter)
     *
     * *Parameter* position The position of the view in the adapter.
     *
     * *Parameter* id The row id of the item that was clicked.
     */
    private val mItemClickListener = OnItemClickListener { parent, view, position, id ->

        val viewObject = getItemAtPosition(getPositionForView(view)) as ExpandableListItem
        if (!viewObject.isExpanded) {
            expandView(view)
        } else {
            collapseView(view)
        }
    }

    /**
     * Calculates the top and bottom bound changes of the selected item. These values are also used
     * to move the bounds of the items around the one that is actually being expanded or collapsed.
     *
     * This method can be modified to achieve different user experiences depending on how you want
     * the cells to expand or collapse. In this specific demo, the cells always try to expand
     * downwards (leaving top bound untouched), and similarly, collapse upwards (leaving top bound
     * untouched). If the change in bounds results in the complete disappearance of a cell, its lower
     * bound is moved to the top of the screen so as not to hide any additional content that the user
     * has not interacted with yet. Furthermore, if the collapsed cell is partially off screen when
     * it is first clicked, it is translated such that its full contents are visible. Lastly, this
     * behaviour varies slightly near the bottom of the [ListView] in order to account for the fact
     * that the bottom bounds of the actual [ListView] cannot be modified.
     *
     * First we initialize [Int] variable `var yTranslateTop` to 0, and initialize [Int] variable
     * `var yTranslateBottom` to our [Int] parameter [yDelta]. We then initialize [Int] variable
     * `val height` to our [Int] parameter [bottom] minus our [Int] parameter [top]. Next we branch
     * on the value of our [Boolean] parameter [isExpanding]:
     *
     *  * `true`: (the view is expanding from its collapsed state) We set [Boolean] variable
     *  `val isOverTop` to `true` if our [Int] parameter [top] is less than 0 (the top of the view
     *  that is to expand is above the screen), and we set [Boolean] variable `val isBelowBottom` to
     *  `true` if the sum of [top] plus `height` plus [Int] parameter [yDelta] is greater than the
     *  height of our [ExpandingListView] (part of the view that is to expand is below the screen).
     *  If `isOverTop` is `true` we set `yTranslateTop` to [top] and `yTranslateBottom` to [yDelta]
     *  minus `yTranslateTop`. On the other hand if `isOverTop` is `false` but `isBelowBottom` is
     *  `true` we initialize [Int] variable `val deltaBelow` to [top] plus `height` plus [yDelta]
     *  minus the height of our [ExpandingListView]. Then if [top] minus `deltaBelow` is less than 0
     *  we set `yTranslateTop` to [top] otherwise we set it to `deltaBelow`, and set `yTranslateBottom`
     *  to [yDelta] minus `yTranslateTop`.
     *
     *  * `false`: (the view is collapsing from its expanded state) We set [Int] variable `val offset`
     *  to the value of the vertical offset of our vertical scrollbar's thumb within the horizontal
     *  range returned by the method [computeVerticalScrollOffset], we set [Int] variable `val range`
     *  to the vertical range that the vertical scrollbar represents returned by the method
     *  [computeVerticalScrollRange], and [Int] variable `val extent` to the vertical extent of our
     *  vertical scrollbar's thumb within the vertical range returned by the [computeVerticalScrollExtent]
     *  method. We then set [Int] variable `val leftoverExtent` to `range` minus `offset` minus
     *  `extent`. We set [Boolean] variable `val isCollapsingBelowBottom` to `true` if `yTranslateBottom`
     *  is greater than `leftoverExtent`, and set [Boolean] variable `val isCellCompletelyDisappearing`
     *  to `true` if `bottom` minus `yTranslateBottom` is less than 0. If `isCollapsingBelowBottom`
     *  is `true` we set `yTranslateTop` to `yTranslateBottom` minus `leftoverExtent` and set
     *  `yTranslateBottom` to `yDelta`minus `yTranslateTop`. If `isCollapsingBelowBottom` is `false`
     *  but `isCellCompletelyDisappearing` is `true` we set `yTranslateBottom` to `bottom` and
     *  `yTranslateTop` to `yDelta` minus `yTranslateBottom`.
     *
     * Finally we return a new [IntArray] array initialized to contain `yTranslateTop` and
     * `yTranslateBottom` to the caller.
     *
     * @param top old top of the clicked on view
     * @param bottom old bottom  of the clicked on view
     * @param yDelta change in the height of the clicked on view
     * @param isExpanding flag to indicate whether the view clicked is expanding (`true`) or
     * collapsing (`false`).
     * @return [IntArray] of two [Int]'s, the 0'th entry containing how far the top of the clicked
     * view should be translated, and the 1'th entry containing how far the bottom of the clicked
     * view should be translated.
     */
    private fun getTopAndBottomTranslations(
        top: Int,
        bottom: Int,
        yDelta: Int,
        isExpanding: Boolean
    ): IntArray {
        var yTranslateTop = 0
        var yTranslateBottom: Int = yDelta
        val height: Int = bottom - top
        if (isExpanding) {
            val isOverTop: Boolean = top < 0
            val isBelowBottom: Boolean = top + height + yDelta > getHeight()
            if (isOverTop) {
                yTranslateTop = top
                yTranslateBottom = yDelta - yTranslateTop
            } else if (isBelowBottom) {
                val deltaBelow = top + height + yDelta - getHeight()
                yTranslateTop = if (top - deltaBelow < 0) top else deltaBelow
                yTranslateBottom = yDelta - yTranslateTop
            }
        } else {
            val offset: Int = computeVerticalScrollOffset()
            val range: Int = computeVerticalScrollRange()
            val extent: Int = computeVerticalScrollExtent()
            val leftoverExtent: Int = range - offset - extent
            val isCollapsingBelowBottom: Boolean = yTranslateBottom > leftoverExtent
            val isCellCompletelyDisappearing: Boolean = bottom - yTranslateBottom < 0
            if (isCollapsingBelowBottom) {
                yTranslateTop = yTranslateBottom - leftoverExtent
                yTranslateBottom = yDelta - yTranslateTop
            } else if (isCellCompletelyDisappearing) {
                yTranslateBottom = bottom
                yTranslateTop = yDelta - yTranslateBottom
            }
        }
        return intArrayOf(yTranslateTop, yTranslateBottom)
    }

    /**
     * This method expands the view that was clicked and animates all the views around it to make
     * room for the expanding view. There are several steps required to do this which are outlined
     * below.
     *
     *  - Store the current top and bottom bounds of each visible item in the [ListView].
     *
     *  - Update the layout parameters of the selected view. In the context of this method, the
     *  view should be originally collapsed and set to some custom height. The layout parameters
     *  are updated so as to wrap the content of the additional text that is to be displayed.
     *
     * After invoking a layout to take place, the [ListView] will order all the items such that
     * there is space for each view. This layout will be independent of what the bounds of the
     * items were prior to the layout so two pre-draw passes will be made. This is necessary
     * because after the layout takes place, some views that were visible before the layout may
     * now be off bounds but a reference to these views is required so the animation completes
     * as intended.
     *
     *  - The first pre-draw pass will set the bounds of all the visible items to their original
     *  location before the layout took place and then force another layout. Since the bounds of
     *  the cells cannot be set directly, the method [setSelectionFromTop] can be used to achieve
     *  a very similar effect.
     *
     *  - The expanding view's bounds are animated to what the final values should be from the
     *  original bounds.
     *
     *  - The bounds above the expanding view are animated upwards while the bounds below the
     *  expanding view are animated downwards.
     *
     *  - The extra text is faded in as its contents become visible throughout the animation process.
     *
     * It is important to note that the [ListView] is disabled during the animation because the
     * scrolling behaviour is unpredictable if the bounds of the items within the [ListView] are
     * not constant during the scroll.
     *
     * First we initialize [ExpandableListItem] variable `val viewObject` with the item whose position
     * is that which is displayed in our [View] parameter [view]. We then initialize [Int] variable
     * `val oldTop` with the [View.getTop] Y coordinate (kotlin `top` property) of `view` and [Int]
     * variable `val oldBottom` with the [View.getBottom] Y coordinate (kotlin `bottom` property).
     * We initialize [HashMap] of [View] to [IntArray] variable `val oldCoordinates` with a new
     * instance, and [Int] variable `val childCount` with the number of children in our [ViewGroup].
     * We then loop over [Int] variable `var i` for those `childCount` children setting [View] variable
     * `val v` to the i'th child, setting the fact that `v` is currently tracking transient state that
     * the framework should attempt to preserve when possible, and storing an array of 2 [Int]
     * containing the top Y coordinate and the bottom Y coordinate of `v` under the key `v` in
     * `oldCoordinates`.
     *
     * When we have finished filling `oldCoordinates` with the Y boundaries of our children we
     * initialize [View] variable `val expandingLayout` by finding the [View] in `view` with id
     * `R.id.expanding_layout` (the extra content) and set its visibility to visible. We
     * [ViewTreeObserver] `val observer` with the [ViewTreeObserver] for this view's hierarchy, and
     * add an anonymous [OnPreDrawListener] whose [OnPreDrawListener.onPreDraw] override calculates
     * and sets in motion the animations which result from expanding `view` in two passes.
     *
     * @param view the [View] that is to be expanded because it was clicked while collapsed
     */
    private fun expandView(view: View) {
        val viewObject: ExpandableListItem = getItemAtPosition(getPositionForView(view)) as ExpandableListItem

        /* Store the original top and bottom bounds of all the cells.*/
        val oldTop = view.top
        val oldBottom = view.bottom
        val oldCoordinates = HashMap<View, IntArray>()
        val childCount = childCount
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            v.setHasTransientState(true)
            oldCoordinates[v] = intArrayOf(v.top, v.bottom)
        }

        /* Update the layout so the extra content becomes visible.*/
        val expandingLayout = view.findViewById<View>(R.id.expanding_layout)
        expandingLayout.visibility = VISIBLE

        /* Add an onPreDraw Listener to the ListView. onPreDraw will get invoked after onLayout
        * and onMeasure have run but before anything has been drawn. This
        * means that the final post layout properties for all the items have already been
        * determined, but still have not been rendered onto the screen.*/
        val observer: ViewTreeObserver = viewTreeObserver
        observer.addOnPreDrawListener(object : OnPreDrawListener {
            /**
             * Callback method to be invoked when the view tree is about to be drawn. At this point,
             * all views in the tree have been measured and given a frame. We branch on the value of
             * our [Boolean] field [mShouldRemoveObserver]:
             *
             *  * `false`: (this is the first pass) first we set [mShouldRemoveObserver] to `true`,
             *  then we initialize [Int] variable `val newTop` with the new top Y of `view`, and
             *  [Int] variable `val newBottom` with the new bottom Y (these values now reflect that
             *  the expanded content has gone from `GONE` to `VISIBLE`). We set [Int] variable
             *  `val newHeight` to `newBottom` minus `newTop`, [Int] variable `val oldHeight` to
             *  `oldBottom` minus `oldTop`, and [Int] variable `val delta` to `newHeight` minus
             *  `oldHeight`. We then set our [IntArray] field [mTranslate] to the [IntArray] array
             *  containing the top and bottom translations for `view` by our [getTopAndBottomTranslations]
             *  method when passed `oldTop` for the old top Y of our view, `oldBottom` for the old
             *  bottom Y of our view, `delta` for the change in the height of the clicked on view,
             *  and `true` for the `isExpanding` argument to indicate we want the values for an
             *  expanding view. We then initialize [Int] variable `val currentTop` with the current
             *  top Y of `view`, and [Int] variable `val futureTop` with `oldTop` minus `mTranslate`
             *  at index 0. We set [Int] variable `var firstChildStartTop` to the top Y of our child
             *  at index 0, [Int] variable `var firstVisiblePosition` to the position within the
             *  adapter's data set for the first item displayed on screen, and [Int] variable
             *  `var deltaTop` to `currentTop` minus `futureTop`. We declare [Int] variable `var i`,
             *  set [Int] variable `val childCountLocal` to the number of children in the group,
             *  then loop over `i` for all these children:
             *
             *  * We set [View] variable `val v` to the child at index `i`, and [Int] variable
             *  `val height` to the bottom Y of `v` minus the maximum of 0 and the top Y of `v`.
             *
             *  * If `deltaTop` minus `height` is greater than 0, we increment `firstVisiblePosition`
             *  subtract `height` from `deltaTop` and loop around for the next i'th child, otherwise
             *  we break out of the loop.
             *
             * If `i` is greater than 0 we set `firstChildStartTop` to 0. We then call
             * [setSelectionFromTop] to select the position `firstVisiblePosition` and
             * position it `firstChildStartTop` minus `deltaTop` pixels from the top edge
             * of our [ListView]. We then call the [requestLayout] method to request another
             * layout to update the layout parameters of the cells and return `false` so that
             * the current drawing pass will be canceled.
             *
             *  * `true`: (this is the second pass) we set our [Boolean] field [mShouldRemoveObserver]
             *  to `false` and remove ourselves as a pre-draw listener so this method does not keep
             *  getting called. We then set [Int] variable `val yTranslateTop` to `mTranslate[0]`,
             *  `int yTranslateBottom` to `mTranslate[1]`, and we set [Int] variable `val index` to
             *  the child index in our [ViewGroup] of `view`. We then loop over all the [View] variable
             *  `var v` in the key set of `oldCoordinates`:
             *
             *  * We set [IntArray] variable `val old` to the array stored in `oldCoordinates` under
             *  the key `v`, set the top of `v` to `old[0]` and the bottom of `v` to `old[1]`.
             *
             *  * If `v` now has no parent (it has been forced off the screen) we add it to our
             *  [List] of [View] field [mViewsToDraw], set [Int] variable `val delta` to
             *  `-yTranslateTop` if `old[0]` is less than `oldTop` or to `yTranslateBottom` if it
             *  is not. We then add the [Animator] created by our [getAnimation] method for animating
             *  the translation of the top and bottom Y of `v` both by `delta` to our [ArrayList] of
             *  [Animator] variable `animations`. On the other hand if `v` still has a parent
             *  we set [Int] variable `val i` to the child index of `v` and if `v` is not equal to
             *  `view` we set [Int] variable `val delta` to `yTranslateBottom` if `i` is greater
             *  than `index` or to `-yTranslateTop` if it is not then add the [Animator] created by
             *  our [getAnimation] method for animating the translation of the top and bottom Y of
             *  `v` both by `delta` to our [ArrayList] of [Animator] variable `animations`. We then
             *  clear the "has transient state" flag of `v`.
             *
             * Having finished with all the siblings of the expanding cell we add an [Animator]
             * created by our [getAnimation] method for animating the translation of the top Y of
             * `view` (the clicked on cell) by `-yTranslateTop` and the bottom Y by `yTranslateBottom`.
             * Then we add an object animator to `animations` which animates the alpha property of
             * the view in `view` with id `R.id.expanding_layout` (the expanding extra content) from
             * 0 to 1. We then disable our [ListView] for the duration of the animation, and set it
             * to not be clickable. We initialize [AnimatorSet] variable `val s` with a new instance,
             * set it to play the animations in `animations` at the same time, and add to it an
             * anonymous [AnimatorListenerAdapter] whose [AnimatorListenerAdapter.onAnimationEnd]
             * override sets the [ExpandableListItem.isExpanded] property of `viewObject` to `true`
             * to set it to an expanded state, enables our [ListView] and sets it to be clickable,
             * then if there are any views in our [MutableList] of [View] field [mViewsToDraw] loops
             * through them clearing their "has transient state" flag. When done doing this we clear
             * the contents of [mViewsToDraw]. Having created and configured our [AnimatorSet] variable
             * `s` we start it running and return true so that this drawing pass will continue.
             *
             * @return Return `true` to proceed with the current drawing pass, or `false` to cancel,
             * on the first pass we return `false` so that the [ListView] does not redraw its contents
             * on this layout pass but only updates all the parameters associated with its children,
             * and on the second pass we return `true` so that the [ListView] proceed to draw itself.
             */
            override fun onPreDraw(): Boolean {
                /* Determine if this is the first or second pass.*/
                if (!mShouldRemoveObserver) {
                    mShouldRemoveObserver = true

                    /* Calculate what the parameters should be for setSelectionFromTop.
                    * The ListView must be offset in a way, such that after the animation
                    * takes place, all the cells that remain visible are rendered completely
                    * by the ListView.*/
                    val newTop: Int = view.top
                    val newBottom: Int = view.bottom
                    val newHeight: Int = newBottom - newTop
                    val oldHeight: Int = oldBottom - oldTop
                    val delta: Int = newHeight - oldHeight
                    mTranslate = getTopAndBottomTranslations(oldTop, oldBottom, delta, true)
                    val currentTop: Int = view.top
                    val futureTop: Int = oldTop - mTranslate[0]
                    var firstChildStartTop: Int = getChildAt(0).top
                    var firstVisiblePosition: Int = firstVisiblePosition
                    var deltaTop: Int = currentTop - futureTop
                    var i: Int
                    val childCountLocal: Int = size
                    i = 0
                    while (i < childCountLocal) {
                        val v = getChildAt(i)
                        val height = v.bottom - Math.max(0, v.top)
                        deltaTop -= if (deltaTop - height > 0) {
                            firstVisiblePosition++
                            height
                        } else {
                            break
                        }
                        i++
                    }
                    if (i > 0) {
                        firstChildStartTop = 0
                    }
                    setSelectionFromTop(firstVisiblePosition, firstChildStartTop - deltaTop)

                    /* Request another layout to update the layout parameters of the cells.*/
                    requestLayout()

                    /* Return false such that the ListView does not redraw its contents on
                     * this layout but only updates all the parameters associated with its
                     * children.*/
                    return false
                }

                /* Remove the pre-draw listener so this method does not keep getting called. */
                mShouldRemoveObserver = false
                observer.removeOnPreDrawListener(this)
                val yTranslateTop: Int = mTranslate[0]
                val yTranslateBottom: Int = mTranslate[1]
                val animations = ArrayList<Animator>()
                val index: Int = indexOfChild(view)

                /* Loop through all the views that were on the screen before the cell was
                *  expanded. Some cells will still be children of the ListView while
                *  others will not. The cells that remain children of the ListView
                *  simply have their bounds animated appropriately. The cells that are no
                *  longer children of the ListView also have their bounds animated, but
                *  must also be added to a list of views which will be drawn in dispatchDraw.*/
                for (v in oldCoordinates.keys) {
                    val old: IntArray? = oldCoordinates[v]
                    v.top = old!![0]
                    v.bottom = old[1]
                    if (v.parent == null) {
                        mViewsToDraw.add(v)
                        val delta: Int = if (old[0] < oldTop) -yTranslateTop else yTranslateBottom
                        animations.add(getAnimation(v, delta.toFloat(), delta.toFloat()))
                    } else {
                        val i: Int = indexOfChild(v)
                        if (v !== view) {
                            val delta: Int = if (i > index) yTranslateBottom else -yTranslateTop
                            animations.add(getAnimation(v, delta.toFloat(), delta.toFloat()))
                        }
                        v.setHasTransientState(false)
                    }
                }

                /* Adds animation for expanding the cell that was clicked. */
                animations.add(getAnimation(view, -yTranslateTop.toFloat(), yTranslateBottom.toFloat()))

                /* Adds an animation for fading in the extra content. */
                animations.add(
                    ObjectAnimator.ofFloat(
                        /* target = */ view.findViewById(R.id.expanding_layout),
                        /* property = */ ALPHA,
                        /* ...values = */ 0f,
                        1f
                    )
                )

                /* Disabled the ListView for the duration of the animation.*/
                isEnabled = false
                isClickable = false

                /* Play all the animations created above together at the same time. */
                val s = AnimatorSet()
                s.playTogether(animations)
                s.addListener(object : AnimatorListenerAdapter() {
                    /**
                     * Notifies the end of the animation. First we set the [ExpandableListItem.isExpanded]
                     * property of `viewObject` to `true` to set it to an expanded state, then we enable
                     * our [ListView] and set it to be clickable, then if there are any views in our
                     * [MutableList] of [View] field [mViewsToDraw] we loop through them clearing their
                     * "has transient state" flag. Finally we clear the contents of [mViewsToDraw].
                     *
                     * @param animation The animation which reached its end.
                     */
                    override fun onAnimationEnd(animation: Animator) {
                        viewObject.isExpanded = true
                        isEnabled = true
                        isClickable = true
                        if (mViewsToDraw.isNotEmpty()) {
                            for (v in mViewsToDraw) {
                                v.setHasTransientState(false)
                            }
                        }
                        mViewsToDraw.clear()
                    }
                })
                s.start()
                return true
            }
        })
    }

    /**
     * Called by [draw] to draw the child views. By overriding [dispatchDraw], we can draw the cells
     * that disappear during the expansion process. When the cell expands, some items below or above
     * the expanding cell may be moved off screen and are thus no longer children of the [ListView]'s
     * layout. By storing a reference to these views prior to the layout, and guaranteeing that these
     * cells do not get recycled (by setting their has transient state flag), the cells can be drawn
     * directly onto the canvas during the animation process. After the animation completes, the
     * references to the extra views can then be discarded. First we call our super's implementation
     * of `dispatchDraw`, then if there are no views in [MutableList] of [View] field [mViewsToDraw]
     * we return having done nothing. Otherwise we loop over the [View] variable `var v` in
     * [mViewsToDraw] translating our [Canvas] parameter [canvas] to the top Y of `v`, then
     * instructing `v` to draw itself on [canvas], and then translating the canvas back to where it
     * started before we loop around for the next [View] in [mViewsToDraw].
     *
     * @param canvas the canvas on which to draw the view
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mViewsToDraw.isEmpty()) {
            return
        }
        for (v in mViewsToDraw) {
            canvas.translate(0f, v.top.toFloat())
            v.draw(canvas)
            canvas.translate(0f, -v.top.toFloat())
        }
    }

    /**
     * This method collapses the [View] that was clicked and animates all the views around it to
     * close around the collapsing view. There are several steps required to do this which are
     * outlined below.
     *
     *  * Update the layout parameters of the [View] clicked so as to minimize its height to the
     *  original collapsed (default) state.
     *
     *  * After invoking a layout, the [ListView] will shift all the cells so as to display them
     *  most efficiently. Therefore, during the first pre-draw pass, the [ListView] must be offset
     *  by some amount such that given the custom bound change upon collapse, all the cells that
     *  need to be on the screen after the layout are rendered by the [ListView].
     *
     *  * On the second pre-draw pass, all the items are first returned to their original location
     *  (before the first layout).
     *
     *  * The collapsing view's bounds are animated to what the final values should be.
     *
     *  * The bounds above the collapsing view are animated downwards while the bounds below the
     *  collapsing view are animated upwards.
     *
     *  * The extra text is faded out as its contents become visible throughout the animation process.
     *
     * First we initialize [ExpandableListItem] variable `val viewObject` with the item whose position
     * is that which is displayed in our [View] parameter [view]. We then initialize [Int] variable
     * `val oldTop` with the top Y coordinate of [view] and [Int] variable `val oldBottom` with the
     * bottom Y coordinate. We initialize [HashMap] of [View] to [IntArray] variable
     * `val oldCoordinates` with a new instance, and [Int] variable `val childCount` with the number
     * of children in our [ViewGroup]. We then loop over [Int] variable `var i` for those `childCountLocal`
     * children setting [View] variable `val v` to the i'th child, setting the fact that `v` is
     * currently tracking transient state that the framework should attempt to preserve when possible,
     * and storing an array of 2 [Int]s containing the top Y coordinate and the bottom Y coordinate
     * of `v` under the key `v` in `oldCoordinates`.
     *
     * We set the layout params of [view] to a new instance whose X dimension is MATCH_PARENT and
     * whose Y dimension is the collapsed height of `viewObject` returned by its
     * [ExpandableListItem.collapsedHeight] property. We initialize [ViewTreeObserver] variable
     * `val observer` with the [ViewTreeObserver] for this view's hierarchy, and add an anonymous
     * [OnPreDrawListener] to it whose [OnPreDrawListener.onPreDraw] override calculates and sets
     * in motion the animations which result from collapsing [view] in two passes.
     *
     * @param view the [View] that was clicked which we need to collapse.
     */
    private fun collapseView(view: View) {
        val viewObject: ExpandableListItem = getItemAtPosition(getPositionForView(view)) as ExpandableListItem

        /* Store the original top and bottom bounds of all the cells.*/
        val oldTop: Int = view.top
        val oldBottom: Int = view.bottom
        val oldCoordinates: HashMap<View, IntArray> = HashMap()
        val childCount: Int = childCount
        for (i in 0 until childCount) {
            val v: View = getChildAt(i)
            v.setHasTransientState(true)
            oldCoordinates[v] = intArrayOf(v.top, v.bottom)
        }

        /* Update the layout so the extra content becomes invisible.*/
        view.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            viewObject.collapsedHeight
        )

        /* Add an onPreDraw listener. */
        val observer: ViewTreeObserver = viewTreeObserver
        observer.addOnPreDrawListener(object : OnPreDrawListener {
            /**
             * Callback method to be invoked when the view tree is about to be drawn. At this point,
             * all views in the tree have been measured and given a frame. We branch on the value of
             * our [Boolean] field [mShouldRemoveObserver]:
             *
             *  * `false`: (this is the first pass) first we set [mShouldRemoveObserver] to `true`,
             *  then we initialize [Int] variable `val newTop` with the new top Y of [view], and
             *  [Int] variable `val newBottom` with the new bottom Y (these values now reflect that
             *  the expanded content has gone from visible to gone). We set [Int] variable
             *  `val newHeight` to `newBottom` minus `newTop`, [Int] variable `val oldHeight` to
             *  `oldBottom` minus `oldTop`, and [Int] variable `val deltaHeight` to `newHeight`
             *  minus `oldHeight`. We then set our [IntArray] field [mTranslate] to the [IntArray]
             *  containing the top and bottom translations for `view` returned by our
             *  [getTopAndBottomTranslations] method when passed `oldTop` for the old top Y of our
             *  view, `oldBottom` for the old bottom Y of our view, `deltaHeight` for the change in
             *  the height of the clicked on view, and `false` for the `isExpanding` argument to
             *  indicate we want the values for an collapsing view. We then initialize [Int] variable
             *  `val currentTop` with the current top Y of `view`, and [Int] variable `val futureTop`
             *  with `oldTop` plus `mTranslate[0]`. We set [Int] variable `var firstChildStartTop`
             *  to the top Y of our child at index 0, [Int] variable `var firstVisiblePosition` to
             *  the position within the adapter's data set for the first item displayed on screen,
             *  and [Int] variable `var deltaTop` to `currentTop` minus `futureTop`. We declare [Int]
             *  variable `var i`, set [Int] variable `val childCount` to the number of children in
             *  the group, then loop over `i` for all these children:
             *
             *  * We set [View] variable `val v` to the child at index `i`, and [Int] variable
             *  `val height` to the bottom Y of `v` minus the maximum of 0 and the top Y of `v`.
             *
             *  * If `deltaTop` minus `height` is greater than 0, we increment `firstVisiblePosition`
             *  subtract `height` from `deltaTop` and loop around for the next i'th child, otherwise
             *  we break out of the loop.
             *
             *  * If `i` is greater than 0 we set `firstChildStartTop` to 0. We then call
             *  [setSelectionFromTop] to select the position `firstVisiblePosition` and position it
             *  `firstChildStartTop` minus `deltaTop` pixels from the top edge of our [ListView]. We
             *  then call the [requestLayout] method to request another layout to update the layout
             *  parameters of the cells and return `false` so that the current drawing pass will be
             *  canceled.
             *
             *  * `true`: (this is the second pass) we set our [Boolean] field [mShouldRemoveObserver]
             *  to `false` and remove ourselves as a pre-draw listener so this method does not keep
             *  getting called. We then set [Int] variable `val yTranslateTop` to `mTranslate[0]`,
             *  [Int] variable `val yTranslateBottom` to `mTranslate[1]`, and we set [Int] variable
             *  `val index` to the child index in our [ViewGroup] of `view`. We then loop over [Int]
             *  variable `var i` for all our `childCountLocal` children:
             *
             *  * We initialize [View] variable `val v` with the child at index `i` and [IntArray]
             *  variable `val old` with the array stored under the key `v` in [HashMap] of [View] to
             *  [IntArray] variable `oldCoordinates`.
             *
             *  * If `old` is not equal to `null` (the [View] variable `v` was present before and
             *  after the collapse) we set the top Y of `v` to `old[0]` and clear the has transient
             *  state flag of `v`. Otherwise the cell is present in the [ListView] after the collapse
             *  but not before the collapse so the bounds are calculated using the bottom and top
             *  translation of the collapsing cell: We set [Int] variable `val delta` to
             *  `yTranslateBottom` if `i` is greater than `index` (it is after the collapsing cell)
             *  or to `-yTranslateTop` if is before it. We then set the top Y of `v` to the current
             *  top plus `delta` and the bottom Y to the current bottom plus delta.
             *
             * We initialize [View] variable `val expandingLayout` by finding the view in `view`
             * with id `R.id.expanding_layout` and initialize [ArrayList] of [Animator] variable
             * `val animations` with a new instance. Then we loop over [Int] variable `var i` for
             * all our `childCountLocal` children:
             *
             *  * We initialize [View] variable `val v` with our child at index `i` and if `v` is
             *  not equal to `view` (the collapsing view) we set [Float] variable `val diff` to
             *  `-yTranslateBottom` if `i` is greater than `index` (it is after the collapsing view)
             *  or to `yTranslateTop` if it is before it.
             *
             *  * We add the [Animator] returned by our [getAnimation] method that will animate the
             *  top and bottom Y of `v` by `diff` to `animations`.
             *
             * When done looping through our children we add the [Animator] returned by our
             * [getAnimation] method that will animate the top Y or `view` by `yTranslateTop` and
             * the bottom Y by `-yTranslateBottom` to `animations`. We then add an [ObjectAnimator]
             * that will animate the alpha of `expandingLayout` from 1 to 0 (fade it out). We then
             * disable our [ListView] and set it to be un-clickable. We initialize [AnimatorSet]
             * variable `val s` with a new instance, set it to play `animations` at the same time,
             * and add an anonymous [AnimatorListenerAdapter] to it whose
             * [AnimatorListenerAdapter.onAnimationEnd] override sets the visibility of
             * `expandingLayout` to GONE, and sets the layout params of `view` to MATCH_PARENT (`w`)
             * by WRAP_CONTENT (`h`). It then sets the [ExpandableListItem.isExpanded] property of
             * `viewObject` to `false` (collapsed), enables our [ListView] and sets it to be clickable.
             * Finally it sets the alpha of `expandingLayout` back to 1 just in case the view is
             * reused by a cell that was expanded.
             *
             * Having set up [AnimatorSet] variable `s` we start it running and return `true` so
             * that the drawing will proceed.
             *
             * @return Return `true` to proceed with the current drawing pass, or `false` to cancel.
             */
            override fun onPreDraw(): Boolean {
                if (!mShouldRemoveObserver) {
                    /*Same as for expandingView, the parameters for setSelectionFromTop must
                    * be determined such that the necessary cells of the ListView are rendered
                    * and added to it.*/
                    mShouldRemoveObserver = true
                    val newTop: Int = view.top
                    val newBottom: Int = view.bottom
                    val newHeight: Int = newBottom - newTop
                    val oldHeight: Int = oldBottom - oldTop
                    val deltaHeight: Int = oldHeight - newHeight
                    mTranslate = getTopAndBottomTranslations(
                        top = oldTop,
                        bottom = oldBottom,
                        yDelta = deltaHeight,
                        isExpanding = false
                    )
                    val currentTop: Int = view.top
                    val futureTop: Int = oldTop + mTranslate[0]
                    var firstChildStartTop: Int = getChildAt(0).top
                    var firstVisiblePosition: Int = firstVisiblePosition
                    var deltaTop: Int = currentTop - futureTop
                    var i: Int
                    val childCountLocal: Int = size
                    i = 0
                    while (i < childCountLocal) {
                        val v: View = getChildAt(i)
                        val height: Int = v.bottom - Math.max(0, v.top)
                        deltaTop -= if (deltaTop - height > 0) {
                            firstVisiblePosition++
                            height
                        } else {
                            break
                        }
                        i++
                    }
                    if (i > 0) {
                        firstChildStartTop = 0
                    }
                    setSelectionFromTop(firstVisiblePosition, firstChildStartTop - deltaTop)
                    requestLayout()
                    return false
                }
                mShouldRemoveObserver = false
                observer.removeOnPreDrawListener(this)
                val yTranslateTop: Int = mTranslate[0]
                val yTranslateBottom: Int = mTranslate[1]
                val index: Int = indexOfChild(view)
                val childCountLocal: Int = size
                for (i in 0 until childCountLocal) {
                    val v: View = getChildAt(i)
                    val old: IntArray? = oldCoordinates[v]
                    if (old != null) {
                        /* If the cell was present in the ListView before the collapse and
                        * after the collapse then the bounds are reset to their old values.*/
                        v.top = old[0]
                        v.bottom = old[1]
                        v.setHasTransientState(false)
                    } else {
                        /* If the cell is present in the ListView after the collapse but
                         * not before the collapse then the bounds are calculated using
                         * the bottom and top translation of the collapsing cell.*/
                        val delta: Int = if (i > index) yTranslateBottom else -yTranslateTop
                        v.top += delta
                        v.bottom += delta
                    }
                }
                val expandingLayout: View = view.findViewById(R.id.expanding_layout)

                /* Animates all the cells present on the screen after the collapse. */
                val animations: ArrayList<Animator> = ArrayList()
                for (i in 0 until childCountLocal) {
                    val v: View = getChildAt(i)
                    if (v !== view) {
                        val diff: Float = (if (i > index) -yTranslateBottom else yTranslateTop).toFloat()
                        animations.add(getAnimation(v, diff, diff))
                    }
                }


                /* Adds animation for collapsing the cell that was clicked. */
                animations.add(getAnimation(view, yTranslateTop.toFloat(), -yTranslateBottom.toFloat()))

                /* Adds an animation for fading out the extra content. */
                animations.add(ObjectAnimator.ofFloat(expandingLayout, ALPHA, 1f, 0f))

                /* Disabled the ListView for the duration of the animation.*/
                isEnabled = false
                isClickable = false

                /* Play all the animations created above together at the same time. */
                val s = AnimatorSet()
                s.playTogether(animations)
                s.addListener(object : AnimatorListenerAdapter() {
                    /**
                     * Notifies the end of the animation. We set the visibility of `expandingLayout`
                     * GONE, and set the layout params of `view` to be MATCH_PARENT (`w`) by
                     * WRAP_CONTENT (`h`). We set the [ExpandableListItem.isExpanded] property of
                     * `viewObject` to `false` (collapsed). We enable our [ListView] and set it to
                     * be clickable. Finally we set the alpha of `expandingLayout` back to 1 just in
                     * case the view is reused to display a cell that is in the expanded state.
                     *
                     * @param animation The animation which reached its end.
                     */
                    override fun onAnimationEnd(animation: Animator) {
                        expandingLayout.visibility = GONE
                        view.layoutParams = LayoutParams(
                            /* w = */ LayoutParams.MATCH_PARENT,
                            /* h = */ LayoutParams.WRAP_CONTENT
                        )
                        viewObject.isExpanded = false
                        isEnabled = true
                        isClickable = true
                        /* Note that alpha must be set back to 1 in case this view is reused
                        * by a cell that was expanded, but not yet collapsed, so its state
                        * should persist in an expanded state with the extra content visible.*/
                        expandingLayout.alpha = 1f
                    }
                })
                s.start()
                return true
            }
        })
    }

    /**
     * This method takes some [View] parameter [view] and the values by which its top and bottom
     * bounds should be changed by. Given these parameters, an animation which will animate these
     * bound changes is created and returned. We initialize [Int] variables `val top` with the top Y
     * coordinate of [view] and `val bottom` with the bottom Y coordinate. We initialize [Int]
     * variable `val endTop` to `top` plus `translateTop` and [Int] variable `val endBottom` to
     * `bottom` plus `translateBottom`. We initialize [PropertyValuesHolder] variable
     * `val translationTop` with an instance which will animate the "top" property from `top` to
     * `endTop` and [PropertyValuesHolder] variable `val translationBottom` with an instance which
     * will animate the "bottom" property from `bottom` to `endBottom`. Finally we return an
     * [ObjectAnimator] which will apply the animations `translationTop` and `translationBottom` to
     * [view].
     *
     * @param view the [View] whose top and bottom Y value is to be animated
     * @param translateTop how much the current top of [view] should be translated
     * @param translateBottom how much the current bottom of [view] should be translated
     * @return an [Animator] which will animate the "top" and "bottom" properties of [view] by the
     * correct amount.
     */
    private fun getAnimation(view: View, translateTop: Float, translateBottom: Float): Animator {
        val top: Int = view.top
        val bottom: Int = view.bottom
        val endTop: Int = (top + translateTop).toInt()
        val endBottom: Int = (bottom + translateBottom).toInt()
        val translationTop: PropertyValuesHolder = PropertyValuesHolder.ofInt(
            /* propertyName = */ "top",
            /* ...values = */ top, endTop
        )
        val translationBottom: PropertyValuesHolder = PropertyValuesHolder.ofInt(
            /* propertyName = */ "bottom",
            /* ...values = */ bottom, endBottom
        )
        return ObjectAnimator.ofPropertyValuesHolder(view, translationTop, translationBottom)
    }
}
