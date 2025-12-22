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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.insertingcells

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.OvershootInterpolator
import android.animation.TimeInterpolator
import android.view.Display
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.android.insertingcells.CustomArrayAdapter.Companion.getCroppedBitmap
import androidx.core.view.isEmpty
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * This [ListView] displays a list of [ListItemObject]. Calling [addRow] with a new [ListItemObject]
 * adds it to the top of the [ListView] and the new row is animated in. If the [ListView] content is
 * at the top (the scroll offset is 0), the animation of the new row is accompanied by an extra
 * image animation that pops an [ImageView] into its place in its corresponding item in the
 * [ListView].
 */
class InsertionListView : ListView {
    /**
     * [OvershootInterpolator] which is in used as the [TimeInterpolator] for the scaling
     * animation applied to the image.
     */
    private var sOvershootInterpolator: OvershootInterpolator? = null

    /**
     * Parent [RelativeLayout] of this [ListView], set by our [setLayout] method which is called
     * from the `onCreate` override of [InsertingCells]. It is required in order to add the custom
     * animated overlaying [Bitmap] when adding a new row.
     */
    private var mLayout: RelativeLayout? = null

    /**
     * [Context] we were constructed for, set by our [inititialize] method which is called by
     * each of our constructors, and is used to access application resources. We are constructed
     * by an element in the layout file layout/activity_main.xml so it is the context of the
     * [Activity] which inflates that file (the `onCreate` override of [InsertingCells]).
     */
    private var mContext: Context? = null

    /**
     * [OnRowAdditionAnimationListener] whose [OnRowAdditionAnimationListener.onRowAdditionAnimationStart]
     * override we will call at the start of the of a row addition animation, and whose
     * [OnRowAdditionAnimationListener.onRowAdditionAnimationEnd] override we will call at the end
     * of that animation. It is set by our [setRowAdditionAnimationListener] method which is called
     * with 'this' from the `onCreate` override of [InsertingCells].
     */
    private var mRowAdditionAnimationListener: OnRowAdditionAnimationListener? = null

    /**
     * Our dataset.
     */
    private var mData: MutableList<ListItemObject>? = null

    /**
     * [BitmapDrawable] objects of all the cells that were visible before the data set changed but
     * not after. This is a bitmap created from the entire view of the item, both the image and
     * the text.
     */
    private var mCellBitmapDrawables: MutableList<BitmapDrawable?>? = null

    /**
     * Our one argument constructor. We just call our [inititialize] method with our [Context]
     * parameter [context]. UNUSED
     *
     * @param context  The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     */
    constructor(context: Context?) : super(context) {
        inititialize(context)
    }

    /**
     * Perform inflation from XML. We just call our [inititialize] method with our [Context]
     * parameter [context]. This is the constructor that is used by our application's layout
     * file layout/activity_main.xml
     *
     * @param context  The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        inititialize(context)
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We just call our [inititialize] method with our [Context] parameter [context].
     * UNUSED
     *
     * @param context  The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     * resource that supplies default values for the view. Can be 0 to not look for defaults.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        inititialize(context)
    }

    /**
     * Initializes this instance. First we call the [ListView.setDivider] method (kotlin `divider`
     * property) to set the drawable that will be drawn between each item in the list to `null`.
     * Then we save our [Context] parameter [context] in our [Context] field [mContext]. We
     * initialize our [MutableList] of [BitmapDrawable] field [mCellBitmapDrawables] with a new
     * instance of [ArrayList], and initialize our [OvershootInterpolator] field [sOvershootInterpolator]
     * with a new instance which will overshoot by [OVERSHOOT_INTERPOLATOR_TENSION] (5).
     *
     * @param context The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     */
    fun inititialize(context: Context?) {
        divider = null
        mContext = context
        mCellBitmapDrawables = ArrayList()
        sOvershootInterpolator = OvershootInterpolator(OVERSHOOT_INTERPOLATOR_TENSION.toFloat())
    }

    /**
     * Modifies the underlying data set and adapter through the addition of the new object as the
     * first item of the [ListView]. The new cell is then animated into place from above the bounds
     * of the [ListView]. We initialize [CustomArrayAdapter] variable `val adapter` with a handle to
     * the adapter currently in use by this [ListView]. We initialize [HashMap] of [Long] to [Rect]
     * variable `val listViewItemBounds` with a new instance, and [HashMap] of [Long] to [BitmapDrawable]
     * variable `val listViewItemDrawables` with a new instance. We initialize [Int] variable
     * `val firstVisiblePosition` to the position within our data set of the first item displayed
     * on screen. We then loop over [Int] variable `var i` for the number of children in this
     * [ViewGroup] (the number of children is 1 more than the number currently on screen in our
     * [ListView]):
     *
     *  * We initialize [View] variable `val child` with our `i`'th child
     *
     *  * We initialize [Int] variable `val position` to `firstVisiblePosition` plus `i`
     *
     *  * We initialize [Long] variable `val itemID` with the stable id returned by the
     *  [CustomArrayAdapter.getItemId] method of `adapter`.
     *
     *  * We initialize [Rect] variable `val startRect` with an instance which matches the size and
     *  position of `child` relative to its parent and store it in `listViewItemBounds` under the
     *  key `itemID`.
     *
     *  * We store the [BitmapDrawable] returned by our method [getBitmapDrawableFromView] which
     *  contains the contents of `child` in `listViewItemDrawables` under the key `itemID`.
     *
     * When done with the loop we add our [ListItemObject] parameter [newObj] to our [MutableList]
     * of [ListItemObject] field [mData], call the [CustomArrayAdapter.addStableIdForDataAtPosition]
     * method of `adapter` to have it generate and add a new stable id for the new [ListItemObject]
     * at position 0 (Note that we share access with `adapter` to [mData]). We then call
     * the [CustomArrayAdapter.notifyDataSetChanged] method of `adapter` notify it that the
     * underlying data has been changed and any [View] reflecting the data set should refresh itself.
     *
     * We initialize [ViewTreeObserver] variable `val observer` with a handle to the [ViewTreeObserver]
     * for our view's hierarchy and add an anonymous [ViewTreeObserver.OnPreDrawListener] whose
     * [ViewTreeObserver.OnPreDrawListener.onPreDraw] override causes some fancy animations to occur
     * when the [ListView] is redrawn.
     *
     * @param newObj the [ListItemObject] to add to our dataset (and animate into its place in our
     * [ListView]).
     */
    fun addRow(newObj: ListItemObject) {
        val adapter = adapter as CustomArrayAdapter

        /*
         * Stores the starting bounds and the corresponding bitmap drawables of every
         * cell presently visible in the ListView before the data set change takes place.
         */
        @SuppressLint("UseSparseArrays")
        val listViewItemBounds = HashMap<Long, Rect>()

        @SuppressLint("UseSparseArrays")
        val listViewItemDrawables = HashMap<Long, BitmapDrawable>()
        val firstVisiblePosition = firstVisiblePosition
        Log.i(TAG, "Child count: $childCount First position: $firstVisiblePosition")
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            val position: Int = firstVisiblePosition + i
            val itemID: Long = adapter.getItemId(position)
            val startRect = Rect(child.left, child.top, child.right, child.bottom)
            listViewItemBounds[itemID] = startRect
            listViewItemDrawables[itemID] = getBitmapDrawableFromView(v = child)
        }

        /* Adds the new object to the data set, thereby modifying the adapter,
         *  as well as adding a stable Id for that specified object.*/
        mData!!.add(index = 0, element = newObj)
        adapter.addStableIdForDataAtPosition(position = 0)
        adapter.notifyDataSetChanged()
        val observer: ViewTreeObserver = viewTreeObserver
        observer.addOnPreDrawListener(object : OnPreDrawListener {
            /**
             * Callback method to be invoked when the view tree is about to be drawn. At this point,
             * all views in the tree have been measured and given a frame. Clients can use this to
             * adjust their scroll bounds or even to request a new layout before drawing occurs.
             *
             * First we remove ourselves as an [OnPreDrawListener]. We initialize our [ArrayList]
             * of [Animator] variable `val animations` with a new instance, initialize [View]
             * variable `val newCell` with our 0'th child (the one with at least a bit showing at
             * the top of our view). We then initialize [ImageView] variable `val imgView` by
             * finding the view in `newCell` with id `R.id.image_view`, and initialize [ImageView]
             * variable `val copyImgView` with a new instance. We initialize [Int] variable
             * `val firstVisiblePosition` with the position within the adapter's data set for the
             * first item displayed on screen, initialize [Boolean] variable `val shouldAnimateInNewRow`
             * with the value returned by our method [shouldAnimateInNewRow] (just returns `true` if
             * the position within the adapter's data set for the first item displayed on screen is
             * 0), and initialize [Boolean] variable `val shouldAnimateInImage` with the value
             * returned by our method [shouldAnimateInNewImage] (returns `true` if the position
             * within the adapter's data set for the first item displayed on screen is 0 AND the
             * top of the 0'th child is at the very top of our view (also returns `true` if there
             * are 0 children). We now branch on the value of `shouldAnimateInNewRow`:
             *
             *  * `true`: We initialize [TextView] variable `val textView` by finding the view in
             *  `newCell` with id `R.id.text_view`, create [ObjectAnimator] variable `val textAlphaAnimator`
             *  to animate the alpha of `textView` from 0 to 1.0, and add it to our [ArrayList] of
             *  [Animator] variable `animations`.
             *
             *  * Then if `shouldAnimateInImage` is also true:
             *
             *  * We initialize [Int] variable `val width` with the width of `imgView` and [Int]
             *  variable `val height` with its height.
             *
             *  * We initialize [Point] variable `val childLoc` with the absolute x,y coordinates
             *  relative to the top left corner of the phone screen as returned by our method
             *  [getLocationOnScreen] for `newCell`, and [Point] variable `val layoutLoc` with the
             *  value that method calculates for [RelativeLayout] field [mLayout] (our parent
             *  [ViewGroup])
             *
             *  * We initialize [ListItemObject] variable `val obj` with the 0'th object in
             *  [MutableList] of [ListItemObject] field [mData], and initialize [Bitmap] variable
             *  `val bitmap` with the bitmap returned by the [CustomArrayAdapter.getCroppedBitmap]
             *  method of [CustomArrayAdapter] when it is passed the [Bitmap] decoded from the
             *  resource id that the [ListItemObject.imgResource] property of `obj` returns.
             *
             *  * We then set the content of [ImageView] variable `copyImgView` to `bitmap`, set the
             *  visibility of `imgView` to `INVISIBLE`, and set the scale type of `copyImgView` to
             *  [ImageView.ScaleType.CENTER].
             *
             *  * We initialize [ObjectAnimator] variable `val imgViewTranslation` with an animator
             *  which will animate the `Y` property of `copyImgView` to the value calculated by
             *  subtracting the `y` field of `layoutLoc` from the `y` field of `childLoc`.
             *
             *  * We initialize [PropertyValuesHolder] variable `val imgViewScaleY` with an instance
             *  which will animate the SCALE_Y property from 0 to 1.0 and initialize [PropertyValuesHolder]
             *  variable `val imgViewScaleX` with an instance which will animate the SCALE_X property
             *  from 0 to 1.0, and then initialize [ObjectAnimator] variable `val imgViewScaleAnimator`
             *  with an instance which will apply both `imgViewScaleX` and `imgViewScaleY` to
             *  `copyImgView`
             *
             *  * We then set the [TimeInterpolator] of `imgViewScaleAnimator` to [OvershootInterpolator]
             *  field [sOvershootInterpolator] and add `imgViewTranslation` and `imgViewScaleAnimator`
             *  to `animations`.
             *
             *  * We initialize [RelativeLayout.LayoutParams] variable `val params` with a `width`
             *  by `height` instance and add [ImageView] variable `copyImgView` to [RelativeLayout]
             *  field [mLayout] using `params` as the layout params.
             *
             * Now we loop over [Int] variable `var i` for all of the currently visible cells in our
             * [ListView]:
             *
             *  * We initialize [View] variable `val child` with the `i`'th child [View], and
             *  initialize [Int] variable `val position` to `firstVisiblePosition` plus `i`
             *
             *  * We initialize [Long] variable `val itemId` with the item id of the position
             *  `position` item in `adapter`, [Rect] variable `val startRect` with the [Rect] stored
             *  under the key `itemId` in [HashMap] of [Long] to [Rect] variable `listViewItemBounds`,
             *  and [Int] variable `val top` with the top position of `child` relative to its parent.
             *
             *  * We now branch on the value of `startRect`:
             *
             *  * If `startRect` is not `null` it was visible before the data set change, and after
             *  the data set change so we animate the cell between the two positions: Initializing
             *  [Int] variable `val startTop` to the top of `startRect` and [Int] variable `val delta`
             *  to `startTop` minus `top`. We create [ObjectAnimator] variable `val animation` to
             *  animate the TRANSLATION_Y property of `child` from `delta` to 0f and add it to
             *  `animations`
             *
             *  * If `startRect` is `null` it was not visible (or present) before the data set
             *  change but is visible after the data set change so we use its height to determine
             *  the delta by which it should be animated: initializing [Int] variable `val childHeight`
             *  to the height of `child` plus the divider height, [Int] variable `val startTop` to
             *  `top` plus `childHeight` if `i` is greater than 0 or to `top` minus `childHeight` if
             *  `i` is 0. We then initialize [Int] variable val delta` to `startTop` minus `top`,
             *  and create [ObjectAnimator] variable `val animation` to animate the TRANSLATION_Y
             *  property of `child` from `delta` to 0 then add `animation` to `animations`.
             *
             * We then remove the [Rect] stored under the key `itemId` in [HashMap] of [Long] to
             * [Rect] variable `listViewItemBounds` and the [BitmapDrawable] stored under that key
             * in [HashMap] of [Long] to [BitmapDrawable] variable `listViewItemDrawables`.
             *
             * Now we loop over the [Long] variable `var itemId` keys of the cells remaining in
             * `listViewItemBounds` (these are cells that were visible before the data set changed
             * but not after):
             *
             *  * We initialize [BitmapDrawable] variable `val bitmapDrawable` with the [BitmapDrawable]
             *  stored under key `itemId` in [HashMap] of [Long] to [BitmapDrawable] variable
             *  `listViewItemDrawables` and [Rect] variable `val startBounds` with the [Rect] stored
             *  under that key in [HashMap] of [Long] to [Rect] variable `listViewItemBounds`.
             *
             *  * We set the bounding rectangle of `bitmapDrawable` to `startBounds` (this is where
             *  the drawable will draw when its `draw()` method is called).
             *
             *  * We initialize [Int] variable `val childHeight` to the [Rect.bottom] property of
             *  `startBounds` minus its [Rect.top] property plus the divider height, and [Rect]
             *  variable `val endBounds` to an instance initialized to the values of `startBounds`.
             *  We then offset `endBounds` by 0 in the X direction and `childHeight` in the Y
             *  direction.
             *
             *  * We create [ObjectAnimator] variable `val animation` to animate the "bounds"
             *  property of `bitmapDrawable` using our [TypeEvaluator] of [Rect] field
             *  [sBoundsEvaluator] as the [TypeEvaluator] from `startBounds` to `endBounds`. We
             *  then add an anonymous [AnimatorUpdateListener] whose
             *  [AnimatorUpdateListener.onAnimationUpdate] override creates a union of the [Rect]
             *  which was just animated to with the accumulated bounds in order to invalidate only
             *  the area of interest (it is deprecated to do this though so it would be better to
             *  just invalidate the entire view).
             *
             *  * We then remove the [Rect] stored under the key `itemId` in [HashMap] of [Long] to
             *  [Rect] variable `listViewItemBounds` and the [BitmapDrawable] stored under that key
             *  in [HashMap] of [Long] to [BitmapDrawable] variable `listViewItemDrawables`.
             *
             *  * We add `bitmapDrawable` to [MutableList] of [BitmapDrawable]
             *  field [mCellBitmapDrawables] and `animation` to [ArrayList] of [Animator] variable
             *  `animations` and loop around for the next `itemId`.
             *
             * We now disable our [View] and call the
             * [OnRowAdditionAnimationListener.onRowAdditionAnimationStart] override of
             * [OnRowAdditionAnimationListener] field [mRowAdditionAnimationListener] to notify it
             * that the animations are about to start. We initialize [AnimatorSet] variable `val set`
             * with a new instance, set its duration to [NEW_ROW_DURATION] and set it up to play the
             * animations in `animations` at the same time. We then add an anonymous
             * [AnimatorListenerAdapter] to `set` whose [AnimatorListenerAdapter.onAnimationEnd]
             * override clears [MutableList] of [BitmapDrawable] field [mCellBitmapDrawables], sets
             * the visibility of `imgView` to VISIBLE, removes the view `copyImgView` from
             * [RelativeLayout] field [mLayout], calls
             * the [OnRowAdditionAnimationListener.onRowAdditionAnimationEnd] override of
             * [OnRowAdditionAnimationListener] field [mRowAdditionAnimationListener] to
             * notify it that the animation is over, enables this view and calls [invalidate] to
             * request a redrawing of the view.
             *
             * It is now time to start `set` running, clear the contents of `listViewItemBounds`
             * and `listViewItemDrawables` and return `true` to the caller to have it proceed with the
             * current drawing pass.
             *
             * @return Return `true` to proceed with the current drawing pass, or `false` to cancel.
             */
            override fun onPreDraw(): Boolean {
                observer.removeOnPreDrawListener(this)
                val animations = ArrayList<Animator>()
                val newCell: View = getChildAt(0)
                val imgView = newCell.findViewById<ImageView>(R.id.image_view)
                val copyImgView = ImageView(mContext)
                val firstVisiblePositionLocal: Int = getFirstVisiblePosition()
                val shouldAnimateInNewRow: Boolean = shouldAnimateInNewRow()
                val shouldAnimateInImage: Boolean = shouldAnimateInNewImage()
                if (shouldAnimateInNewRow) {
                    /* Fades in the text of the first cell. */
                    val textView = newCell.findViewById<TextView>(R.id.text_view)
                    val textAlphaAnimator = ObjectAnimator.ofFloat(textView, ALPHA, 0.0f, 1.0f)
                    animations.add(textAlphaAnimator)

                    /* Animates in the extra hover view corresponding to the image
                     * in the top row of the ListView. */
                    if (shouldAnimateInImage) {
                        val width: Int = imgView.width
                        val height: Int = imgView.height
                        val childLoc: Point = getLocationOnScreen(v = newCell)
                        val layoutLoc: Point = getLocationOnScreen(v = mLayout)
                        val obj: ListItemObject = mData!![0]
                        val bitmap: Bitmap = getCroppedBitmap(
                            BitmapFactory.decodeResource(
                                /* res = */ mContext!!.resources,
                                /* id = */ obj.imgResource,
                                /* opts = */ null
                            )
                        )
                        copyImgView.setImageBitmap(bitmap)
                        imgView.visibility = INVISIBLE
                        copyImgView.scaleType = ImageView.ScaleType.CENTER
                        val imgViewTranslation = ObjectAnimator.ofFloat(
                            /* target = */ copyImgView,
                            /* property = */ Y,
                            /* ...values = */ (childLoc.y - layoutLoc.y).toFloat()
                        )
                        val imgViewScaleY = PropertyValuesHolder.ofFloat(
                            /* property = */ SCALE_Y,
                            /* ...values = */ 0f, 1.0f
                        )
                        val imgViewScaleX = PropertyValuesHolder.ofFloat(
                            /* property = */ SCALE_X,
                            /* ...values = */ 0f, 1.0f
                        )
                        val imgViewScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(
                            /* target = */ copyImgView,
                            /* ...values = */ imgViewScaleX, imgViewScaleY
                        )
                        imgViewScaleAnimator.interpolator = sOvershootInterpolator
                        animations.add(imgViewTranslation)
                        animations.add(imgViewScaleAnimator)
                        val params = RelativeLayout.LayoutParams(width, height)
                        mLayout!!.addView(/* child = */ copyImgView, /* params = */ params)
                    }
                }

                /* Loops through all the currently visible cells in the ListView and animates
                 * all of them into their post layout positions from their original positions.*/
                for (i in 0 until childCount) {
                    val child: View = getChildAt(i)
                    val position: Int = firstVisiblePositionLocal + i
                    val itemId: Long = adapter.getItemId(position)
                    val startRect: Rect? = listViewItemBounds[itemId]
                    val top: Int = child.top
                    if (startRect != null) {
                        /* If the cell was visible before the data set change and
                         * after the data set change, then animate the cell between
                         * the two positions.*/
                        val startTop: Int = startRect.top
                        val delta: Int = startTop - top
                        val animation = ObjectAnimator.ofFloat(
                            /* target = */ child,
                            /* property = */ TRANSLATION_Y,
                            /* ...values = */ delta.toFloat(), 0f
                        )
                        animations.add(animation)
                    } else {
                        /* If the cell was not visible (or present) before the data set
                         * change but is visible after the data set change, then use its
                         * height to determine the delta by which it should be animated.*/
                        val childHeight: Int = child.height + dividerHeight
                        val startTop: Int = top + if (i > 0) childHeight else -childHeight
                        val delta: Int = startTop - top
                        val animation = ObjectAnimator.ofFloat(
                            /* target = */ child,
                            /* property = */ TRANSLATION_Y,
                            /* ...values = */ delta.toFloat(), 0f
                        )
                        animations.add(animation)
                    }
                    listViewItemBounds.remove(itemId)
                    listViewItemDrawables.remove(itemId)
                }

                /*
                 * Loops through all the cells that were visible before the data set
                 * changed but not after, and keeps track of their corresponding
                 * drawables. The bounds of each drawable are then animated from the
                 * original state to the new one (off the screen). By storing all
                 * the drawables that meet this criteria, they can be redrawn on top
                 * of the ListView via dispatchDraw as they are animating.
                 */
                for (itemId in listViewItemBounds.keys) {
                    val bitmapDrawable: BitmapDrawable? = listViewItemDrawables[itemId]
                    val startBounds: Rect? = listViewItemBounds[itemId]
                    bitmapDrawable!!.bounds = startBounds!!
                    val childHeight: Int = startBounds.bottom - startBounds.top + dividerHeight
                    val endBounds = Rect(/* r = */ startBounds)
                    endBounds.offset(/* dx = */ 0, /* dy = */ childHeight)
                    val animation = ObjectAnimator.ofObject(
                        /* target = */ bitmapDrawable,
                        /* propertyName = */ "bounds",
                        /* evaluator = */ sBoundsEvaluator,
                        /* ...values = */ startBounds, endBounds
                    )
                    animation.addUpdateListener(object : AnimatorUpdateListener {
                        private var mLastBound: Rect? = null
                        private val mCurrentBound = Rect()

                        /**
                         * Notifies the occurrence of another frame of the animation. We initialize
                         * [Rect] variable `val bounds` with most recent [Rect] calculated by our
                         * [ValueAnimator] parameter [valueAnimator], and copy its coordinates to
                         * [Rect] property [mCurrentBound]. If [Rect] property [mLastBound] is not
                         * `null` we update [mCurrentBound] to enclose itself and [mLastBound]. We
                         * then set [mLastBound] to `bounds` and call the [invalidate] method to
                         * mark the area defined by [mCurrentBound] as needing to be redrawn and
                         * scheduling it to occur.
                         *
                         * @param valueAnimator The animation which has moved to another frame.
                         */
                        override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
                            val bounds = valueAnimator.animatedValue as Rect
                            mCurrentBound.set(bounds)
                            if (mLastBound != null) {
                                mCurrentBound.union(mLastBound!!)
                            }
                            mLastBound = bounds
                            invalidate()
                        }
                    })
                    listViewItemBounds.remove(itemId)
                    listViewItemDrawables.remove(itemId)
                    mCellBitmapDrawables!!.add(bitmapDrawable)
                    animations.add(animation)
                }

                /* Animates all the cells from their old position to their new position
                 *  at the same time.*/
                isEnabled = false
                mRowAdditionAnimationListener!!.onRowAdditionAnimationStart()
                val set = AnimatorSet()
                set.duration = NEW_ROW_DURATION.toLong()
                set.playTogether(animations)
                set.addListener(object : AnimatorListenerAdapter() {
                    /**
                     * Notifies the end of the animation. We clear [MutableList] of [BitmapDrawable]
                     * field [mCellBitmapDrawables] of all entries, set `imgView` to be VISIBLE,
                     * remove the view `copyImgView` from [RelativeLayout] field [mLayout], call the
                     * [OnRowAdditionAnimationListener.onRowAdditionAnimationEnd] override of
                     * [mRowAdditionAnimationListener] to notify it that the animation has finished,
                     * enable our view, and call [invalidate] to have us be drawn again.
                     *
                     * @param animation The animation which reached its end.
                     */
                    override fun onAnimationEnd(animation: Animator) {
                        mCellBitmapDrawables!!.clear()
                        imgView.visibility = VISIBLE
                        mLayout!!.removeView(copyImgView)
                        mRowAdditionAnimationListener!!.onRowAdditionAnimationEnd()
                        isEnabled = true
                        invalidate()
                    }
                })
                set.start()
                listViewItemBounds.clear()
                listViewItemDrawables.clear()
                return true
            }
        })
    }

    /**
     * Called by [draw] to draw the child views. This may be overridden by derived classes to
     * gain control just before its children are drawn (but after its own view has been drawn). By
     * overriding [dispatchDraw], the [BitmapDrawable]s of all the cells that were on the screen
     * before (but not after) the layout are drawn and animated off the screen.
     *
     * First we call our super's implementation of `dispatchDraw`, then if the size of [MutableList]
     * of [BitmapDrawable] field [mCellBitmapDrawables] is greater than 0 we loop through all the
     * [BitmapDrawable] variable `var bitmapDrawable` in it calling the [BitmapDrawable.draw] method
     * of each [BitmapDrawable].
     *
     * @param canvas the [Canvas] on which to draw the view
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mCellBitmapDrawables!!.isNotEmpty()) {
            for (bitmapDrawable in mCellBitmapDrawables!!) {
                bitmapDrawable!!.draw(canvas)
            }
        }
    }

    /**
     * Convenience function to determine if the first visible position is equal to 0, in which case
     * we need to animate in new rows when they are added.
     *
     * @return `true` if the first visible position is equal to 0
     */
    fun shouldAnimateInNewRow(): Boolean {
        val firstVisiblePosition = firstVisiblePosition
        return firstVisiblePosition == 0
    }

    /**
     * Convenience function to determine whether the [ImageView] of a new row should be animated
     * from the top of the screen to its item's position. If our [ViewGroup] has no children yet
     * we just return `true` to the caller. Otherwise we initialize [Boolean] variable
     * `val shouldAnimateInNewRow` with the value returned by our method [shouldAnimateInNewRow],
     * and [View] variable `val topCell` with our 0'th child. If `shouldAnimateInNewRow` is `true`
     * and the top position of the view `topCell` relative to its parent is 0 we return `true` to
     * the caller otherwise we return `false`.
     *
     * @return `true` if the [ImageView] of a new row should be animated from the top of the screen,
     * which should occur only if our list view is at the very top of its list.
     */
    fun shouldAnimateInNewImage(): Boolean {
        if (isEmpty()) {
            return true
        }
        val shouldAnimateInNewRow = shouldAnimateInNewRow()
        val topCell = getChildAt(0)
        return shouldAnimateInNewRow && topCell.top == 0
    }

    /**
     * Returns a bitmap drawable showing a screenshot of the [View] parameter [v] passed in. We
     * initialize our [Bitmap] variable `val bitmap` with an instance that is the same width and
     * height as our [View] parameter [v] using ARGB_8888 as its config. We create [Canvas] variable
     * `val canvas` to draw into `bitmap` and call the [View.draw] method of [v] to have it draw
     * itself onto `canvas`. Finally we return a new instance of [BitmapDrawable] created from
     * `bitmap` using an initial target density based on the display metrics of the resources
     * of our [View].
     *
     * @param v the [View] we want to create a bitmap of.
     * @return a [BitmapDrawable] showing a screenshot of the view passed in.
     */
    private fun getBitmapDrawableFromView(v: View): BitmapDrawable {
        val bitmap = createBitmap(width = v.width, height = v.height)
        val canvas = Canvas(bitmap)
        v.draw(canvas)
        return bitmap.toDrawable(resources = resources)
    }

    /**
     * Returns the absolute x,y coordinates of our [View] parameter [v] relative to the top left
     * corner of the phone screen. We initialize [DisplayMetrics] variable `val dm` with a new
     * instance. We retrieve the context our view is running in, cast it to [Activity] in order to
     * call its [Activity.getWindowManager] method (kotlin `windowManager` property( to retrieve the
     * window manager for showing custom windows, in order to call its [WindowManager.getDefaultDisplay]
     * method (kotlin `defaultDisplay` property) to retrieve the [Display] upon which it will create
     * new windows, in order to call its [Display.getMetrics] method to load the display metrics
     * that describe its size and density into `dm` (since we do not use `dm` this is a strange
     * thing to do of course). We then allocate an [IntArray] of size 2 to initialize `val location`
     * and call the [View.getLocationOnScreen] method of [v] to load its coordinates on the screen
     * into `location`. Finally we return a [Point] constructed from the X coordinate in
     * `location[0]` and the Y coordinate in `location[1]`.
     *
     * @param v the [View] we want the coordinates of.
     * @return the absolute x,y coordinates of our [View] parameter [v] relative to the top left
     * corner of the phone screen
     */
    fun getLocationOnScreen(v: View?): Point {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") // Oddly enough `dm` does not seem to be used?
        (context as Activity).windowManager.defaultDisplay.getMetrics(dm)
        val location = IntArray(size = 2)
        v!!.getLocationOnScreen(/* outLocation = */ location)
        return Point(/* x = */ location[0], /* y = */ location[1])
    }

    /**
     * Setter for the underlying data set controlling the adapter. We just store our [List] of
     * [ListItemObject] parameter [data] in our [MutableList] of [ListItemObject] field [mData].
     *
     * @param data the `List<ListItemObject>` we are to use as our dataset.
     */
    fun setData(data: List<ListItemObject>?) {
        mData = data as MutableList<ListItemObject>?
    }

    /**
     * Setter for the parent [RelativeLayout] of this [ListView]. A reference to this [ViewGroup] is
     * required in order to add the custom animated overlaying bitmap when adding a new row. We
     * just save our [RelativeLayout] parameter [layout] in our [RelativeLayout] field [mLayout].
     *
     * @param layout the [RelativeLayout] that is our parent view.
     */
    fun setLayout(layout: RelativeLayout?) {
        mLayout = layout
    }

    /**
     * Setter for our [OnRowAdditionAnimationListener] field [mRowAdditionAnimationListener]. (we
     * need to call the [OnRowAdditionAnimationListener.onRowAdditionAnimationStart] and
     * [OnRowAdditionAnimationListener.onRowAdditionAnimationEnd] overrides of this field to notify
     * it about the state of our animations). We just save our [OnRowAdditionAnimationListener]
     * parameter [rowAdditionAnimationListener] in our [OnRowAdditionAnimationListener] field
     * [mRowAdditionAnimationListener].
     *
     * @param rowAdditionAnimationListener the [OnRowAdditionAnimationListener] whose
     * [OnRowAdditionAnimationListener.onRowAdditionAnimationStart] and
     * [OnRowAdditionAnimationListener.onRowAdditionAnimationEnd] overrides we are to call
     */
    fun setRowAdditionAnimationListener(rowAdditionAnimationListener: OnRowAdditionAnimationListener?) {
        mRowAdditionAnimationListener = rowAdditionAnimationListener
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "InsertionListView"

        /**
         * Duration of the animation which animates all the cells from their old position to their
         * new position at the same time.
         */
        private const val NEW_ROW_DURATION = 500

        /**
         * Amount of overshoot used when constructing [OvershootInterpolator] field
         * [sOvershootInterpolator] which is in turn used as the [TimeInterpolator] for the scaling
         * animation applied to the image.
         */
        private const val OVERSHOOT_INTERPOLATOR_TENSION = 5

        /**
         * This TypeEvaluator is used to animate the position of a BitmapDrawable
         * by updating its bounds.
         */
        val sBoundsEvaluator: TypeEvaluator<Rect> = object : TypeEvaluator<Rect> {
            /**
             * This function returns the result of linearly interpolating the start and end values,
             * with [Float] parameter [fraction] representing the proportion between the start and
             * end values. We return a new instance of [Rect] whose [Rect.left], [Rect.top],
             * [Rect.right], and [Rect.bottom] values are those calculated by our [interpolate]
             * method when it linearly interpolates between the same fields in our [Rect] parameters
             * [startValue] and [endValue] to take into account the value of our [Float] parameter
             * [fraction]
             *
             * @param fraction   The fraction from the starting to the ending values
             * @param startValue The start value.
             * @param endValue   The end value.
             * @return A linear interpolation between the start and end values, given the
             * [Float] parameter [fraction].
             */
            override fun evaluate(fraction: Float, startValue: Rect, endValue: Rect): Rect {
                return Rect(
                    /* left = */ interpolate(
                        start = startValue.left,
                        end = endValue.left,
                        fraction = fraction
                    ),
                    /* top = */ interpolate(
                        start = startValue.top,
                        end = endValue.top,
                        fraction = fraction
                    ),
                    /* right = */ interpolate(
                        start = startValue.right,
                        end = endValue.right,
                        fraction = fraction
                    ),
                    /* bottom = */ interpolate(
                        start = startValue.bottom,
                        end = endValue.bottom,
                        fraction = fraction
                    )
                )
            }

            /**
             * Linearly interpolates between the start and end values of a value being animated. We
             * return our [Int] parameter [start] plus [Float] parameter [fraction] times the
             * quantity [Int] parameter [end] minus [start].
             *
             * @param start    starting value
             * @param end      ending value
             * @param fraction The fraction from the starting to the ending values
             * @return a value that is [fraction] distance between the start and end.
             */
            fun interpolate(start: Int, end: Int, fraction: Float): Int {
                return (start + fraction * (end - start)).toInt()
            }
        }
    }
}
