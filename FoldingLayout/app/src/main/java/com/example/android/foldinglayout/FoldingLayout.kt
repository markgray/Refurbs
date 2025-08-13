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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog", "JoinDeclarationAndAssignment")

package com.example.android.foldinglayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader.TileMode
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap

/**
 * The folding layout where the number of folds, the anchor point and the orientation of the fold
 * can be specified. Each of these parameters can be modified individually and updates and resets
 * the fold to a default (unfolded) state. The fold factor varies between 0 (completely unfolded
 * flat image) to 1.0 (completely folded, non-visible image).
 *
 * This layout throws an exception if there is more than one child added to the view. For more
 * complicated view hierarchy's inside the folding layout, the views should all be nested inside
 * 1 parent layout.
 *
 * This layout folds the contents of its child in real time. By applying matrix transformations when
 * drawing to canvas, the contents of the child may change as the fold takes place. It is important
 * to note that there are jagged edges about the perimeter of the layout as a result of applying
 * transformations to a rectangle. This can be avoided by having the child of this layout wrap its
 * content inside a 1 pixel transparent border. This will cause an anti-aliasing like effect and
 * smoothen out the edges.
 */
class FoldingLayout : ViewGroup {
    /**
     * `enum` for the orientation of our folding.
     */
    enum class Orientation {
        /**
         * Vertical orientation
         */
        VERTICAL,

        /**
         * Horizontal orientation
         */
        HORIZONTAL
    }

    /**
     * Hold the segments of the view that are being folded (each [Rect] is a segment).
     */
    private lateinit var mFoldRectArray: Array<Rect?>

    /**
     * Holds the transformation matrices used to "fold" when drawing each of the segments that are
     * in [Array] of [Rect] field [mFoldRectArray].
     */
    private lateinit var mMatrix: Array<Matrix?>

    /**
     * Orientation of our folding, either [Orientation.VERTICAL] or [Orientation.HORIZONTAL].
     */
    private var mOrientation: Orientation = Orientation.HORIZONTAL

    /**
     * Location of the anchor point of our folding as a fraction of either our width or height
     * depending on our orientation (0.0 to 1.0) , set by our [anchorFactor] property.
     */
    private var mAnchorFactor: Float = 0f

    /**
     * How much are we folded, from 0 for not folded to 1 for totally folded
     */
    private var mFoldFactor: Float = 0f

    /**
     * Number of folds, set by our [numberOfFolds] property.
     */
    private var mNumberOfFolds: Int = 2

    /**
     * Flag indicating that our folding [mOrientation] orientation is [Orientation.HORIZONTAL].
     */
    private var mIsHorizontal: Boolean = true

    /**
     * Width of our view, set by a call to the method [getMeasuredWidth] in our method [prepareFold]
     */
    private var mOriginalWidth: Int = 0

    /**
     * Height of our view, set by a call to the method [getMeasuredHeight] in our method [prepareFold]
     */
    private var mOriginalHeight: Int = 0

    /**
     * Maximum width of a fold (when the view is totally unfolded), the width of the view in
     * [Orientation.VERTICAL] orientation, and the width of the view divided by the number of
     * folds in [Orientation.HORIZONTAL orientation. Set in our method [prepareFold].
     */
    private var mFoldMaxWidth: Float = 0f

    /**
     * Maximum height of a fold (when the view is totally unfolded), the height of the view in
     * [Orientation.HORIZONTAL] orientation, and the height of the view divided by the number of
     * folds in [Orientation.VERTICAL] orientation. Set in our method [prepareFold].
     */
    private var mFoldMaxHeight: Float = 0f

    /**
     * Current width of a fold given the amount of folding we have done, calculated in our method
     * [calculateMatrices]. Starts at [mFoldMaxWidth] and will go down to 0 when our orientation is
     * [Orientation.HORIZONTAL] (always the same for [Orientation.VERTICAL] of course).
     */
    private var mFoldDrawWidth: Float = 0f

    /**
     * Current height of a fold given the amount of folding we have done, calculated in our method
     * [calculateMatrices]. Starts at [mFoldMaxHeight] and will go down to 0 when our orientation is
     * [Orientation.VERTICAL] (always the same for [Orientation.HORIZONTAL] of course).
     */
    private var mFoldDrawHeight: Float = 0f

    /**
     * Flag to indicate that our [prepareFold] method has completed its initialization of the data
     * structures needed to draw our view in a folded state. Set to `false` at the beginning of the
     * method, and to `true` at the end. Used in our [dispatchDraw] override to avoid the custom
     * drawing of our child by passing the call on to our super and returning, and by our
     * [calculateMatrices] method to skip the creation of the transformation matrices.
     */
    private var mIsFoldPrepared: Boolean = false

    /**
     * Flag used by our [dispatchDraw] method to determine whether it needs to draw our child at all
     * (it returns having done nothing if it is `false`). Set to `true` at the beginning of our
     * method [calculateMatrices], then set to `false` if the view is totally folded (our field
     * [mFoldFactor] is 1) and should not be seen (the canvas can be left completely empty), and set
     * to `false` if the width or the height of a fold is 0 (the view is essentially completely
     * folded as well).
     */
    private var mShouldDraw: Boolean = true

    /**
     * The [Paint] used to draw the shadows of even numbered folds, a solid black with an alpha
     * whose value depends on the value of [mFoldFactor] (goes from 0 to 0.8*255 as we fold up).
     */
    private var mSolidShadow: Paint? = null

    /**
     * The [Paint] used to draw the shadows of odd numbered folds, uses as its gradient our
     * [LinearGradient] field [mShadowLinearGradient].
     */
    private var mGradientShadow: Paint? = null

    /**
     * Gradient used for our [Paint] field [mGradientShadow], it is created in our [prepareFold]
     * method based on our orientation, then has its local matrix calculated and set to scale it
     * according to the current fold width or height.
     */
    private var mShadowLinearGradient: LinearGradient? = null

    /**
     * Local matrix used to scale our [LinearGradient] field [mShadowLinearGradient] based on the
     * orientation and current width or height of our folding view.
     */
    private var mShadowGradientMatrix: Matrix? = null

    /**
     * Source points for our call to [Matrix.setPolyToPoly] which we use to define the transformation
     * matrices which twist each of our folds (both the shadow and bitmap). Set to a [mFoldDrawHeight]
     * by [mFoldDrawWidth] rectangle in our [calculateMatrices] method.
     */
    private lateinit var mSrc: FloatArray

    /**
     * Destination points for our call to [Matrix.setPolyToPoly] which we use to define the
     * transformation matrices which twist each of our folds (both the shadow and bitmap). Set
     * to a parallelogram for each of our folds in our [calculateMatrices] method with the location
     * of the points calculated based on the orientation, number of folds, whether it is an even or
     * odd fold, and the current fold factor.
     */
    private lateinit var mDst: FloatArray

    /**
     * The [OnFoldListener] whose [OnFoldListener.onStartFold] override we call in our
     * [calculateMatrices] method when the fold factor first goes from 0 to non-zero, and whose
     * [OnFoldListener.onEndFold] override we call when the fold factor goes from non-zero to zero.
     * Needs to be set by a call to our [setFoldListener] or we crash (we do not check for `null`
     * before calling its methods!).
     */
    private var mFoldListener: OnFoldListener? = null

    /**
     * Previous fold factor, set to zero in our [prepareFold] method, and used by our
     * [calculateMatrices] method to determine if folding has just started or just ended
     * (in order to decide whether we should call either of the overrides of our [OnFoldListener]
     * field [mFoldListener]) then set to the new fold factor [mFoldFactor].
     */
    private var mPreviousFoldFactor: Float = 0f

    /**
     * [Bitmap] used only when [FoldingLayoutActivity.IS_JBMR2] is `true` (JELLY_BEAN_MR2) ie. never!
     */
    private var mFullBitmap: Bitmap? = null

    /**
     * Destination rectangle used only in call to [Canvas.drawBitmap] for JELLY_BEAN_MR2 ie. never!
     */
    private var mDstRect: Rect? = null

    /**
     * Our one argument constructor, we just call our super's constructor.
     *
     * @param context [Context] to use to access resources.
     */
    constructor(context: Context?) : super(context)

    /**
     * Perform inflation from XML. We just call our super's constructor.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We just call our super's constructor.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a
     * reference to a style resource that supplies default values for
     * the view. Can be 0 to not look for defaults.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /**
     * Adds a view during layout. This is useful if in your [onLayout] method, you need to add more
     * views (as does the list view for example). We call our method [throwCustomException] with our
     * current child count (which will throw [NumberOfFoldingLayoutChildrenException] if we already
     * have a child). Then we call our super's implementation of `addViewInLayout`.
     *
     * @param child the [View] to add to the group
     * @param index the index at which the child must be added or -1 to add last
     * @param params the layout parameters to associate with the child
     * @param preventRequestLayout if `true`, calling this method will not trigger a
     * layout request on child
     * @return `true` if the child was added, `false` otherwise
     */
    override fun addViewInLayout(
        child: View,
        index: Int,
        params: LayoutParams,
        preventRequestLayout: Boolean
    ): Boolean {
        throwCustomException(childCount)
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    /**
     * Adds a child view with the specified layout parameters. We call our method [throwCustomException]
     * with our current child count (which will throw [NumberOfFoldingLayoutChildrenException] if we
     * already have a child. Then we call our super's implementation of `addView`.
     *
     * @param child the child [View] to add
     * @param index the position at which to add the child or -1 to add last
     * @param params the layout parameters to set on the child
     */
    override fun addView(child: View, index: Int, params: LayoutParams) {
        throwCustomException(childCount)
        super.addView(child, index, params)
    }

    /**
     * Measure the view and its content to determine the measured width and the measured height. We
     * initialize [View] variable `val child` with our one and only child view, then call the
     * [measureChild] method to have it measure itself into our [Int] parameters [widthMeasureSpec]
     * and [heightMeasureSpec]. Finally we call the [setMeasuredDimension] to store these measured
     * sizes.
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
     * The requirements are encoded with [android.view.View.MeasureSpec].
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     * The requirements are encoded with [android.view.View.MeasureSpec].
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        measureChild(child, widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Called from layout when this [View] should assign a size and position to each of its children.
     * We initialize [View] variable `val child` with our one and only child view, then call its
     * [View.layout] method to have it position itself at the top left point (0,0) and the right
     * bottom point at its measured width and height. Finally we call our [updateFold] method to
     * have it call our [prepareFold] method to update the fold's orientation, anchor point and
     * number of folds, call our [calculateMatrices] method to calculate the transformation matrices
     * used to draw each of the separate folding segments from this view, and then call the
     * [invalidate] method so that we will redraw ourselves.
     *
     * @param changed This is a new size or position for this view
     * @param l       Left position, relative to parent
     * @param t       Top position, relative to parent
     * @param r       Right position, relative to parent
     * @param b       Bottom position, relative to parent
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val child = getChildAt(0)
        child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        updateFold()
    }

    /**
     * The custom exception to be thrown so as to limit the number of views in this
     * layout to at most one.
     */
    private class NumberOfFoldingLayoutChildrenException
    /**
     * Our constructor. We just call our super's constructor.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     * `getMessage` method.
     */
    (message: String?) : RuntimeException(message)

    /**
     * Throws an exception if the number of views added to this layout exceeds one.
     *
     * @param numOfChildViews current number of child views.
     */
    private fun throwCustomException(numOfChildViews: Int) {
        if (numOfChildViews == 1) {
            throw NumberOfFoldingLayoutChildrenException(FOLDING_VIEW_EXCEPTION_MESSAGE)
        }
    }

    /**
     * Sets our [OnFoldListener] field [mFoldListener]. We call its [OnFoldListener.onStartFold]
     * override when the folding layout begins folding, and its [OnFoldListener.onEndFold] override
     * when the folding layout ends folding.
     *
     * @param foldListener The [OnFoldListener] we are to use to report `onStartFold` and
     * `onEndFold` events.
     */
    fun setFoldListener(foldListener: OnFoldListener?) {
        mFoldListener = foldListener
    }

    /**
     * anchor factor, between 0 (left or top anchor point) and 1.0 (right or bottom anchor point).
     */
    var anchorFactor: Float
        /**
         * Getter for our [Float] field [mAnchorFactor]. Unused
         *
         * @return current value of our [Float] field [mAnchorFactor]
         */
        get() = mAnchorFactor
        /**
         * Sets our new anchor factor which places our anchor point somewhere between the extremes
         * of our height and width, and calls our [updateFold] method which then calls our
         * [prepareFold] method to have update the fold's orientation, anchor point and number of
         * folds, calls our [calculateMatrices] method to calculate the transformation matrices used
         * to draw each of the separate folding segments from this view, and then calls the
         * [invalidate] method so that we will redraw ourselves.
         *
         * @param anchorFactor new anchor factor, between 0 (left or top anchor point) and 1.0
         * (right or bottom anchor point).
         */
        set(anchorFactor) {
            if (anchorFactor != mAnchorFactor) {
                mAnchorFactor = anchorFactor
                updateFold()
            }
        }

    /**
     * current value of our [Orientation] field [mOrientation]
     */
    var orientation: Orientation
        /**
         * Getter for our [Orientation] field [mOrientation].
         */
        get() = mOrientation
        /**
         * Sets our new orientation then calls our [updateFold] method to have it call our
         * [prepareFold] method to update the fold's orientation, anchor point and number of folds,
         * call our [calculateMatrices] method to calculate the transformation matrices used to draw
         * each of the separate folding segments from this view, and then call the [invalidate]
         * method so that we will redraw ourselves.
         *
         * @param orientation our new [Orientation], either [Orientation.VERTICAL] or
         * [Orientation.HORIZONTAL].
         */
        set(orientation) {
            if (orientation != mOrientation) {
                mOrientation = orientation
                updateFold()
            }
        }

    /**
     * Sets the fold factor of the folding view and updates all the corresponding matrices and
     * values to account for the new fold factor. Once that is complete, it redraws itself with
     * the new fold.
     */
    var foldFactor: Float
        /**
         * Getter for our [Float] field [mFoldFactor].
         *
         * @return current value of our [Float] field [mFoldFactor].
         */
        get() = mFoldFactor
        /**
         * Sets the fold factor of the folding view and updates all the corresponding
         * matrices and values to account for the new fold factor. Once that is complete,
         * it redraws itself with the new fold.
         *
         * @param foldFactor new fold factor, between 0 (no folding) and 1.0 (fully folded).
         */
        set(foldFactor) {
            if (foldFactor != mFoldFactor) {
                mFoldFactor = foldFactor
                calculateMatrices()
                invalidate()
            }
        }

    /**
     * the number of folds we fold in.
     */
    var numberOfFolds: Int
        /**
         * Getter for our [Int] field [mNumberOfFolds].
         *
         * @return current value of our [Int] field [mNumberOfFolds].
         */
        get() = mNumberOfFolds
        /**
         * Sets the number of folds we fold in then calls our [updateFold] method to have it
         * call our [prepareFold] method to update the fold's orientation, anchor point and number
         * of folds, call our [calculateMatrices] method to calculate the transformation matrices
         * used to draw each of the separate folding segments from this view, and then call the
         * [invalidate] method so that we will redraw ourselves.
         *
         * @param numberOfFolds new number of folds.
         */
        set(numberOfFolds) {
            if (numberOfFolds != mNumberOfFolds) {
                mNumberOfFolds = numberOfFolds
                updateFold()
            }
        }

    /**
     * Convenience method to call our [prepareFold] method to update the fold's orientation,
     * anchor point and number of folds, call our [calculateMatrices] method to calculate the
     * transformation matrices used to draw each of the separate folding segments from this view,
     * and then call the [invalidate] method so that we will redraw ourselves.
     */
    private fun updateFold() {
        prepareFold(mOrientation, mAnchorFactor, mNumberOfFolds)
        calculateMatrices()
        invalidate()
    }

    /**
     * This method is called in order to update the fold's orientation, anchor point and number of
     * folds. This creates the necessary setup in order to prepare the layout for a fold with the
     * specified parameters. Some of the dimensions required for the folding transformation are
     * also acquired here. After this method is called, it will be in a completely unfolded state
     * by default.
     *
     * We allocate [NUM_OF_POLY_POINTS] (8) [Float] for both of our [FloatArray] fields [mSrc] and
     * [mDst], initialize [Rect] field [mDstRect] with a new instance, set our field [Float] field
     * [mFoldFactor] to 0f (fully unfolded), set the [Float] field [mPreviousFoldFactor] to 0f, set
     * our [Boolean] field [mIsFoldPrepared] to `false`, allocate new instances for our [Paint]
     * fields [mSolidShadow] and [mGradientShadow], set our [Orientation] field [mOrientation] to
     * our [Orientation] parameter [orientation], and set our [Boolean] field [mIsHorizontal] to the
     * value of testing for [orientation] being equal to [Orientation.HORIZONTAL]. If [mIsHorizontal]
     * is `true` we set our [LinearGradient] field [mShadowLinearGradient] to a new instance of
     * [LinearGradient] that draws a linear gradient along a line from (0,0) to (SHADING_FACTOR, 0)
     * (ie. (0.5, 0)) transitioning from [Color.BLACK] to [Color.TRANSPARENT], using a shader tiling
     * mode of [TileMode.CLAMP] (replicate the edge color if the shader draws outside of its original
     * bounds), if it is false we initialize it to a new instance of [LinearGradient] that draws a
     * linear gradient along a line from (0,0) to (0, SHADING_FACTOR) (ie. (0, 0.5)) transitioning
     * from [Color.BLACK] to [Color.TRANSPARENT], using a shader tiling mode of [TileMode.CLAMP]
     * (replicate the edge color if the shader draws outside of its original bounds). We set the
     * style of [Paint] field [mGradientShadow] to [Paint.Style.FILL] and set its shader to
     * [mShadowLinearGradient]. We initialize our [Matrix] field [mShadowGradientMatrix] with a new
     * instance, set our [Float] field [mAnchorFactor] to our [Float] parameter [anchorFactor], set
     * our [Int] field [mNumberOfFolds] to our [Int] parameter [numberOfFolds], initialize our [Int]
     * field [mOriginalWidth] to the measured width of our view, and our [Int] field [mOriginalHeight]
     * to our measured height. We allocate a [mNumberOfFolds] array of nulls for our [Array] of [Rect]
     * field [mFoldRectArray] and allocate a [mNumberOfFolds] array of nulls for our [Array] of
     * [Matrix] field [mMatrix]. We then loop over `x` allocating a new instance of [Matrix] for
     * each of the [mNumberOfFolds] elements in [mMatrix].
     *
     * We initialize our [Int] variable `val h` to our field [mOriginalHeight] and [Int] variable
     * `val w` to our field [mOriginalWidth]. If [FoldingLayoutActivity.IS_JBMR2] is `true` (our
     * device is running JELLY_BEAN_MR2) we initialize our [Bitmap] field [mFullBitmap] with a `w`
     * by `h` ARGB_8888 bitmap, initialize [Canvas] variable `val canvas` with an instance which
     * will draw into [mFullBitmap] and instruct our (only) child to draw itself into `canvas`.
     *
     * If [mIsHorizontal] is `true` initialize [Int] variable `vla delta` to the rounded value of
     * `w` divided by [mNumberOfFolds], if it is `false` we initialize it to the rounded value of
     * `h` divided by [mNumberOfFolds]. Now we loop over [Int] variable `x` for all of the
     * [mNumberOfFolds] folds:
     *
     *  * We branch on the value of `mIsHorizontal`:
     *
     *  * `true`: We calculate the value of [Int] variable `val deltap` based on whether there is
     *  room left in our width `w` for one more segment that is `delta` wide, if there is we set
     *  `deltap` to `delta` otherwise we set it to the amount of space left which is `w` minus `x`
     *  times `delta`. We then allocate a new [Rect] instance for [mFoldRectArray] at index `x`
     *  whose left edge is at `x` times `delta`, whose top is 0, whose right edge is `x` times
     *  `delta` plus `deltap`, and whose bottom is `h`.
     *
     *  * `false`: We calculate the value of [Int] variable `val deltap` based on whether there is
     *  room left in our height `h` for one more segment that is `delta` wide, if there is we set
     *  `deltap` to `delta` otherwise we set it to the amount of space left which is `h` minus `x`
     *  times `delta`. We then allocate a new [Rect] instance for [mFoldRectArray] at index `x`
     *  whose left edge is at 0, whose top is `x` times `delta`, whose right edge is `w`, and whose
     *  bottom is `x` times `delta` plus `deltap`.
     *
     * When done with all our segments we again branch on the value of [mIsHorizontal]:
     *
     *  * `true`: we set our field [mFoldMaxHeight] to `h` and our field [mFoldMaxWidth] to `delta`.
     *
     *  * `false`: we set our field [mFoldMaxHeight] to `delta` and our field [mFoldMaxWidth] to `w`.
     *
     * Finally we set our field [mIsFoldPrepared] to true.
     *
     * @param orientation orientation we should fold in, either [Orientation.HORIZONTAL] or
     * [Orientation.VERTICAL]
     * @param anchorFactor fraction from 0 to 1.0 we use to calculate the anchor point that we fold
     * on, 0 for left or top, and 1.0 for right or bottom.
     * @param numberOfFolds number of folds we split our view into
     */
    private fun prepareFold(orientation: Orientation, anchorFactor: Float, numberOfFolds: Int) {
        mSrc = FloatArray(NUM_OF_POLY_POINTS)
        mDst = FloatArray(NUM_OF_POLY_POINTS)
        mDstRect = Rect()
        mFoldFactor = 0f
        mPreviousFoldFactor = 0f
        mIsFoldPrepared = false
        mSolidShadow = Paint()
        mGradientShadow = Paint()
        mOrientation = orientation
        mIsHorizontal = orientation == Orientation.HORIZONTAL
        mShadowLinearGradient = if (mIsHorizontal) {
            LinearGradient(0f, 0f, SHADING_FACTOR, 0f, Color.BLACK,
                Color.TRANSPARENT, TileMode.CLAMP)
        } else {
            LinearGradient(0f, 0f, 0f, SHADING_FACTOR, Color.BLACK,
                Color.TRANSPARENT, TileMode.CLAMP)
        }
        mGradientShadow!!.style = Paint.Style.FILL
        mGradientShadow!!.shader = mShadowLinearGradient
        mShadowGradientMatrix = Matrix()
        mAnchorFactor = anchorFactor
        mNumberOfFolds = numberOfFolds
        mOriginalWidth = measuredWidth
        mOriginalHeight = measuredHeight
        mFoldRectArray = arrayOfNulls(mNumberOfFolds)
        mMatrix = arrayOfNulls(mNumberOfFolds)
        for (x in 0 until mNumberOfFolds) {
            mMatrix[x] = Matrix()
        }
        val h = mOriginalHeight
        val w = mOriginalWidth
        if (FoldingLayoutActivity.IS_JBMR2) {
            mFullBitmap = createBitmap(width = w, height = h)
            val canvas = Canvas(mFullBitmap!!)
            getChildAt(0).draw(canvas)
        }
        val delta = Math.round(if (mIsHorizontal) w.toFloat() / mNumberOfFolds.toFloat() else h.toFloat() / mNumberOfFolds.toFloat())

        /* Loops through the number of folds and segments the full layout into a number
         * of smaller equal components. If the number of folds is odd, then one of the
         * components will be smaller than all the rest. Note that deltap below handles
         * the calculation for an odd number of folds.*/
        for (x in 0 until mNumberOfFolds) {
            if (mIsHorizontal) {
                val deltap = if ((x + 1) * delta > w) w - x * delta else delta
                mFoldRectArray[x] = Rect(x * delta, 0, x * delta + deltap, h)
            } else {
                val deltap = if ((x + 1) * delta > h) h - x * delta else delta
                mFoldRectArray[x] = Rect(0, x * delta, w, x * delta + deltap)
            }
        }
        if (mIsHorizontal) {
            mFoldMaxHeight = h.toFloat()
            mFoldMaxWidth = delta.toFloat()
        } else {
            mFoldMaxHeight = delta.toFloat()
            mFoldMaxWidth = w.toFloat()
        }
        mIsFoldPrepared = true
    }

    /**
     * Calculates the transformation matrices used to draw each of the separate folding segments of
     * this view. First we set our [Boolean] flag field [mShouldDraw] to `true` (our [dispatchDraw]
     * override will return without doing any drawing while this is `false`). If our [Boolean] flag
     * field [mIsFoldPrepared] is `false` ([prepareFold] has not been called) we return having done
     * nothing. If our [Float] field [mFoldFactor] is 1f (we are completely folded) we set our flag
     * [mShouldDraw] to `false` (we are invisible, so there is no need to draw) and return. If our
     * current fold factor [Float] field [mFoldFactor] is 0f and our previous fold factor [Float]
     * field [mPreviousFoldFactor] is greater than 0f (we have just become totally unfolded) we call
     * the [OnFoldListener.onEndFold] override of our [OnFoldListener] field [mFoldListener]. If our
     * previous fold factor [Float] field [mPreviousFoldFactor] is 0f, and our current fold factor
     * [Float] field [mFoldFactor] is greater than 0f (we have just begun to fold) we call the
     * [OnFoldListener.onStartFold] override of our [OnFoldListener] field [mFoldListener]. We set
     * our previous fold factor [Float] field [mPreviousFoldFactor] to our current fold factor
     * [Float] field [mFoldFactor].
     *
     * We now loop over [Int] variable `x` calling the [Matrix.reset] method of each [Matrix] in our
     * [Array] of [Matrix] field [mMatrix] to set the matrix to the identity matrix.
     *
     * We initialize [Float] variable `val cTranslationFactor` to 1f minus our fold factor [Float]
     * field [mFoldFactor]. If [Boolean] field [mIsHorizontal] is `true` we initialize [Float]
     * variable `val translatedDistance` to the value [mOriginalWidth] times `cTranslationFactor`
     * (the current width of our folded horizontal view), if it is `false` we use [mOriginalHeight]
     * times `cTranslationFactor` (the current height of our folded vertical view). We then
     * initialize [Float] variable `val translatedDistancePerFold` to the rounded value of
     * `translatedDistance` divided by `mNumberOfFolds`. Then if [mFoldMaxWidth] is less than
     * `translatedDistancePerFold` we set [Float] field [mFoldDrawWidth] to `translatedDistancePerFold`,
     * otherwise we set it to [mFoldMaxWidth] (preventing rounding error from making it from exceeding
     * [mFoldMaxWidth]). Similarly if [mFoldMaxHeight] is less than `translatedDistancePerFold` we
     * set [Float] field [mFoldDrawHeight] to `translatedDistancePerFold`, otherwise we set it to
     * [mFoldMaxHeight]. We initialize [Float] variable `val translatedDistanceFoldSquared` to
     * `translatedDistancePerFold` times `translatedDistancePerFold`, and use it to calculate the
     * depth of the fold into the screen [Float] variable `val depth` by applying the pythagorean
     * theorem. If [Boolean] field [mIsHorizontal] is `true` this is the square root of [mFoldDrawWidth]
     * times [mFoldDrawWidth] minus `translatedDistanceFoldSquared`, and if `false` it is the square
     * root of [mFoldDrawHeight] times [mFoldDrawHeight] minus `translatedDistanceFoldSquared`.
     *
     * In order to introduce perspective into our drawing we calculate [Float] variable
     * `val scaleFactor` to be [DEPTH_CONSTANT] (1500) divided by [DEPTH_CONSTANT] plus `depth`.
     * We then declare the `[Float] variables `val scaledWidth`, `val scaledHeight`,
     * `val bottomScaledPoint`, `val topScaledPoint`, `val rightScaledPoint` and
     * `val leftScaledPoint`. If [Boolean] flag field [mIsHorizontal] is `true` we set
     * `scaledWidth` to [Float] field [mFoldDrawWidth] times `cTranslationFactor`, and
     * `scaledHeight` to [Float] field [mFoldDrawHeight] times `scaleFactor`, and if `false`
     * we set `scaledWidth` to [Float] field [mFoldDrawWidth] times `scaleFactor`, and
     * `scaledHeight` to [Float] field [mFoldDrawHeight] times `cTranslationFactor`.
     *
     * We then set `topScaledPoint` to [Float] field [mFoldDrawHeight] minus `scaledHeight` divided
     * by 2.0, `bottomScaledPoint` to `topScaledPoint` times `scaledHeight`, `leftScaledPoint`
     * to [Float] field [mFoldDrawWidth] minus `scaledWidth` divided by 2.0f, and `rightScaledPoint`
     * to `leftScaledPoint` plus `scaledWidth`.
     *
     * If [Boolean] flag field [mIsHorizontal] is `true` then we initialize [Float] variable
     * `val anchorPoint` to be [Float] field [mAnchorFactor] times [Float] field [mOriginalWidth],
     * if `false` to [Float] field [mAnchorFactor] times [Float] field [mOriginalHeight]. If
     * [Boolean] flag field [mIsHorizontal] is `true` then we initialize [Float] variable
     * `val midFold` to `anchorPoint` divided by [Float] field [mFoldDrawWidth], if `false` to
     * `anchorPoint` divided by [Float] field [mFoldDrawHeight] (this is the fold along which the
     * anchor point is located).
     *
     * We now initialize the source polygon for our `setPolyToPoly` transformation, [FloatArray]
     * field [mSrc]:
     *
     *  * left top corner (0, 0)
     *
     *  * left bottom corner (0, [mFoldDrawHeight])
     *
     *  * right top corner ([mFoldDrawWidth], 0)
     *
     *  * left bottom corner ([mFoldDrawWidth], [])
     *
     * We then compute the transformation matrix for each fold by looping over [Int] variable
     * `x` for all the [mNumberOfFolds] folds:
     *
     *  * We initialize [Boolean] variable `val isEven` by testing whether `x` is even, then branch
     * on the value of [Boolean] flag field [mIsHorizontal]:
     *
     *  * true:
     *
     * `mDst[0]`: if `anchorPoint` is greater than `x` times [Float] field [mFoldDrawWidth] (the
     * anchor point is to the right of our fold) we set `mDst[0]` to `anchorPoint` plus `x` minus
     * `midFold` times `scaledWidth`, if it is to the left of our fold we set it to `anchorPoint`
     * minus the quantity `midFold` minus `x` times `scaledWidth`.
     *
     * `mDst[1]`: if `isEven` is `true` we set it to 0f, if odd we set it to `topScaledPoint`
     *
     * `mDst[2]`: we set to `mDst[0]`
     *
     * `mDst[3]`: if `isEven` is `true` we set to [Float] field [mFoldDrawHeight] if odd we set to
     * `bottomScaledPoint`
     *
     * `mDst[4]`: if `anchorPoint` is to the left of the end or our fold we set to `anchorPoint`
     * plus the quantity `x` plus 1 minus `midFold` times `scaledWidth`, if to the right we set to
     * `anchorPoint` minus the quantity `midFold` minus `x` minus 1 times `scaledWidth`.
     *
     * `mDst[5]`: if `isEven` is `true` we set `topScaledPoint`, to 0f if it is odd.
     *
     * `mDst[6]`: we set to `mDst[4]`
     *
     * `mDst[7]`: if `isEven` is `true` we set to `bottomScaledPoint`, and to [Float] field
     * [mFoldDrawHeight] if odd.
     *
     *  * `false`: (ie it's VERTICAL)
     *
     * `mDst[0]`: if `isEven` is `true` we set to 0, if odd we set to `leftScaledPoint`
     *
     * `mDst[1]`: if `anchorPoint` is greater than `x` times [Float] field [mFoldDrawHeight]
     * (we are above the anchor point) we set to `anchorPoint` plus the quantity `x`  minus
     * `midFold` times `scaledHeight`, and if below the anchor point to `anchorPoint` minus
     * the quantity `x` minus the quantity `midFold` minus `x` times `scaledHeight`.
     *
     * `mDst[2]`: if `isEven` is `true` we set to `leftScaledPoint`, to 0 if odd.
     *
     * `mDst[3]`: if `anchorPoint` is greater than `x` plus 1 times `mFoldDrawHeight` (the anchor
     * point is below the bottom of our fold) we set to `anchorPoint` plus the quantity `x` plus 1
     * minus `midFold` times `scaledHeight`, and if above the bottom of our fold to `anchorPoint`
     * minus the quantity `midFold` minus `x` minus 1 times `scaledHeight`
     *
     * `mDst[4]`: if `isEven` is `true` we set to [Float] field [mFoldDrawWidth], and to
     * `rightScaledPoint` if odd.
     *
     * `mDst[5]`: we set to `mDst[1]`
     *
     * `mDst[6]`: if `isEven` is `true` we set to `rightScaledPoint`, to [Float] field
     * [mFoldDrawWidth] if odd.
     *
     * `mDst[7]`: we set to `mDst[3]`
     *
     *  * Since pixel fractions are present for odd number of folds, we loop through all the [Float]
     *  in [FloatArray] field [mDst] rounding them off.
     *
     *  * If any of the folds have reached a point where the width or height of that fold is 0f,
     *  then nothing needs to be drawn onto the canvas because the view is essentially completely
     *  folded, so for HORIZONTAL orientation if `mDst[4]` is less than or equal to `mDst[0]` or
     *  `mDst[6]` is less than or equal to `mDst[2]` we set [Boolean] flag field [mShouldDraw] to
     *  `false` and return. Similarly for VERTICAL orientation if `mDst[3]` is less than or equal to
     *  `mDst[1]` or `mDst[7]` is less than or equal to `mDst[5]` we do likewise.
     *
     *  * We now set the transformation matrix of fold `x` (the `x` entry in [Array] of [Matrix]
     *  field [mMatrix]) to a poly to poly matrix mapping [FloatArray] field [mSrc] to [FloatArray]
     *  field [mDst].
     *
     *  * and now we loop back for the next `x + 1` fold.
     *
     * We next initialize the [Paint] that we use for the shadows, initialize [Int] variable
     * `val alpha` to the current fold factor [Float] field [mFoldFactor] times 255 times
     * [SHADING_ALPHA], and we then set the color of [Paint] field [mSolidShadow] to black with
     * an alpha of `alpha`. We now branch on the value of [Boolean] flag field [mIsHorizontal]:
     *
     *  * `true`: (HORIZONTAL folding orientation) we set [Matrix] field [mShadowGradientMatrix] to
     *  scale X by [Float] field [mFoldDrawWidth] and Y by 1f, then set the local matrix of the
     *  shader [LinearGradient] field [mShadowLinearGradient] to it.
     *
     *  * `false`: (VERTICAL folding orientation) we set [Matrix] field [mShadowGradientMatrix] to
     *  scale X by 1 and Y by [Float] field [mFoldDrawHeight], then set the local matrix of the
     *  shader [LinearGradient] field [mShadowLinearGradient] to it.
     *
     * We then set the shader of [Paint] field [mGradientShadow] to [mShadowLinearGradient] and set
     * its alpha to `alpha`.
     */
    private fun calculateMatrices() {
        mShouldDraw = true
        if (!mIsFoldPrepared) {
            return
        }

        /* If the fold factor is 1 than the folding view should not be seen
         * and the canvas can be left completely empty. */
        if (mFoldFactor == 1f) {
            mShouldDraw = false
            return
        }
        if (mFoldFactor == 0f && mPreviousFoldFactor > 0f) {
            mFoldListener!!.onEndFold()
        }
        if (mPreviousFoldFactor == 0f && mFoldFactor > 0f) {
            mFoldListener!!.onStartFold()
        }
        mPreviousFoldFactor = mFoldFactor

        /* Reset all the transformation matrices back to identity before computing
         * the new transformation */
        for (x in 0 until mNumberOfFolds) {
            mMatrix[x]!!.reset()
        }
        val cTranslationFactor: Float = 1f - mFoldFactor
        val translatedDistance: Float = if (mIsHorizontal) {
            mOriginalWidth * cTranslationFactor
        } else {
            mOriginalHeight * cTranslationFactor
        }
        val translatedDistancePerFold = Math.round(translatedDistance / mNumberOfFolds).toFloat()

        /* For an odd number of folds, the rounding error may cause the
         * translatedDistancePerFold to be greater than the max fold width or height. */
        mFoldDrawWidth = if (mFoldMaxWidth < translatedDistancePerFold) {
            translatedDistancePerFold
        } else {
            mFoldMaxWidth
        }
        mFoldDrawHeight = if (mFoldMaxHeight < translatedDistancePerFold) {
            translatedDistancePerFold
        } else {
            mFoldMaxHeight
        }
        val translatedDistanceFoldSquared: Float = translatedDistancePerFold * translatedDistancePerFold

        /* Calculate the depth of the fold into the screen using pythagorean theorem. */
        val depth = if (mIsHorizontal) {
            Math.sqrt((mFoldDrawWidth * mFoldDrawWidth -
                translatedDistanceFoldSquared).toDouble()).toFloat()
        } else {
            Math.sqrt((mFoldDrawHeight * mFoldDrawHeight -
                translatedDistanceFoldSquared).toDouble()).toFloat()
        }

        /* The size of some object is always inversely proportional to the distance
        *  it is away from the viewpoint. The constant can be varied to affect the
        *  amount of perspective. */
        val scaleFactor = DEPTH_CONSTANT / (DEPTH_CONSTANT + depth)
        val scaledWidth: Float
        val scaledHeight: Float
        val bottomScaledPoint: Float
        val topScaledPoint: Float
        val rightScaledPoint: Float
        val leftScaledPoint: Float
        if (mIsHorizontal) {
            scaledWidth = mFoldDrawWidth * cTranslationFactor
            scaledHeight = mFoldDrawHeight * scaleFactor
        } else {
            scaledWidth = mFoldDrawWidth * scaleFactor
            scaledHeight = mFoldDrawHeight * cTranslationFactor
        }
        topScaledPoint = (mFoldDrawHeight - scaledHeight) / 2.0f
        bottomScaledPoint = topScaledPoint + scaledHeight
        leftScaledPoint = (mFoldDrawWidth - scaledWidth) / 2.0f
        rightScaledPoint = leftScaledPoint + scaledWidth
        val anchorPoint: Float = if (mIsHorizontal) {
            mAnchorFactor * mOriginalWidth
        } else {
            mAnchorFactor * mOriginalHeight
        }

        /* The fold along which the anchor point is located. */
        val midFold: Float = if (mIsHorizontal) {
            anchorPoint / mFoldDrawWidth
        } else {
            anchorPoint / mFoldDrawHeight
        }
        mSrc[0] = 0f
        mSrc[1] = 0f
        mSrc[2] = 0f
        mSrc[3] = mFoldDrawHeight
        mSrc[4] = mFoldDrawWidth
        mSrc[5] = 0f
        mSrc[6] = mFoldDrawWidth
        mSrc[7] = mFoldDrawHeight

        /* Computes the transformation matrix for each fold using the values calculated above. */
        for (x in 0 until mNumberOfFolds) {
            val isEven: Boolean = x % 2 == 0
            if (mIsHorizontal) {
                mDst[0] = if (anchorPoint > x * mFoldDrawWidth) {
                    anchorPoint + (x - midFold) * scaledWidth
                } else {
                    anchorPoint - (midFold - x) * scaledWidth
                }
                mDst[1] = if (isEven) 0f else topScaledPoint
                mDst[2] = mDst[0]
                mDst[3] = if (isEven) mFoldDrawHeight else bottomScaledPoint
                mDst[4] = if (anchorPoint > (x + 1) * mFoldDrawWidth) {
                    anchorPoint + (x + 1 - midFold) * scaledWidth
                } else {
                    anchorPoint - (midFold - x - 1) * scaledWidth
                }
                mDst[5] = if (isEven) topScaledPoint else 0f
                mDst[6] = mDst[4]
                mDst[7] = if (isEven) bottomScaledPoint else mFoldDrawHeight
            } else {
                mDst[0] = if (isEven) 0f else leftScaledPoint
                mDst[1] = if (anchorPoint > x * mFoldDrawHeight) {
                    anchorPoint + (x - midFold) * scaledHeight
                } else {
                    anchorPoint - (midFold - x) * scaledHeight
                }
                mDst[2] = if (isEven) leftScaledPoint else 0f
                mDst[3] = if (anchorPoint > (x + 1) * mFoldDrawHeight) {
                    anchorPoint + (x + 1 - midFold) * scaledHeight
                } else {
                    anchorPoint - (midFold - x - 1) * scaledHeight
                }
                mDst[4] = if (isEven) mFoldDrawWidth else rightScaledPoint
                mDst[5] = mDst[1]
                mDst[6] = if (isEven) rightScaledPoint else mFoldDrawWidth
                mDst[7] = mDst[3]
            }

            /* Pixel fractions are present for odd number of folds which need to be
             * rounded off here.*/
            for (y in 0..7) {
                mDst[y] = Math.round(mDst[y]).toFloat()
            }

            /* If it so happens that any of the folds have reached a point where
            *  the width or height of that fold is 0, then nothing needs to be
            *  drawn onto the canvas because the view is essentially completely
            *  folded.*/
            if (mIsHorizontal) {
                if (mDst[4] <= mDst[0] || mDst[6] <= mDst[2]) {
                    mShouldDraw = false
                    return
                }
            } else {
                if (mDst[3] <= mDst[1] || mDst[7] <= mDst[5]) {
                    mShouldDraw = false
                    return
                }
            }

            /* Sets the shadow and bitmap transformation matrices.*/
            mMatrix[x]!!.setPolyToPoly(
                mSrc,
                0,
                mDst,
                0,
                NUM_OF_POLY_POINTS / 2
            )
        }
        /* The shadows on the folds are split into two parts: Solid shadows and gradients.
         * Every other fold has a solid shadow which overlays the whole fold. Similarly,
         * the folds in between these alternating folds also have an overlaying shadow.
         * However, it is a gradient that takes up part of the fold as opposed to a solid
         * shadow overlaying the whole fold.*/

        /* Solid shadow paint object. */
        val alpha: Int = (mFoldFactor * 255 * SHADING_ALPHA).toInt()
        mSolidShadow!!.color = Color.argb(alpha, 0, 0, 0)
        if (mIsHorizontal) {
            mShadowGradientMatrix!!.setScale(mFoldDrawWidth, 1f)
            mShadowLinearGradient!!.setLocalMatrix(mShadowGradientMatrix)
        } else {
            mShadowGradientMatrix!!.setScale(1f, mFoldDrawHeight)
            mShadowLinearGradient!!.setLocalMatrix(mShadowGradientMatrix)
        }
        mGradientShadow!!.shader = mShadowLinearGradient
        mGradientShadow!!.alpha = alpha
    }

    /**
     * Called by draw to draw the child views. This may be overridden by derived classes to gain
     * control just before its children are drawn (but after its own view has been drawn). If our
     * method [prepareFold] has not been called ([Boolean] flag field [mIsFoldPrepared] is `false`)
     * or our [Float] field [mFoldFactor] is 0f (we are totally unfolded) we just call our super's
     * implementation of `dispatchDraw` and return. If our [Boolean] flag field [mShouldDraw] is
     * `false` we return without even passing the call on to our super.
     *
     * Otherwise we declare [Rect] variable `var src` then loop over [Int] `var x` for all of [Int]
     * field [mNumberOfFolds] folds:
     *
     *  * We set `src` to entry `x` in [Array] of [Rect] field [mFoldRectArray] and have [Canvas]
     *  parameter [canvas] save its current matrix and clip onto a private stack. We pre-concatenate
     *  the transformation matrix of fold `x` (the `x` entry in [Array] of [Matrix] field [mMatrix])
     *  onto the current matrix of [canvas]. (We then do some nonsense for JELLY_BEAN_MR2 which I
     *  will skip). We set the clipping rectangle of [canvas] to a rectangle whose left top is at
     *  (0,0) and whose right bottom is at the `right` field of `src` minus its `left` field, and
     *  `bottom` field minus the `top` field (the width and height of our source [Rect] in other
     *  words).
     *
     *  * If [Boolean] flag field [mIsHorizontal] is `true` we translate [canvas] by minus the value
     *  of the `left` field of `src` in the X direction, if our orientation is VERTICAL we translate
     *  [canvas] by minus the value of the `top` field of `src` in the Y direction.
     *
     *  * We call our super's implementation of `dispatchDraw`.
     *
     *  * If [Boolean] flag field [mIsHorizontal] is `true` we translate [canvas] by the value of
     *  the `left` field of `src` in the X direction, if our orientation is VERTICAL we translate
     *  [canvas] by the value of the `top` field of `src` in the Y direction (back to where it
     *  started?)
     *
     *  * We now branch on whether `x` is even or odd:
     *
     *  * even: We have [canvas] draw a rectangle whose left top corner is at (0,0) and whose right
     *  bottom corner is at ([mFoldDrawWidth], [mFoldDrawHeight]) using [Paint] field [mSolidShadow]
     *  as its [Paint].
     *
     *  * odd: We have [canvas] draw a rectangle whose left top corner is at (0,0) and whose right
     *  bottom corner is at ([mFoldDrawWidth], [mFoldDrawHeight]) using [Paint] field [mGradientShadow]
     *  as its [Paint].
     *
     *  * We have [canvas] remove all modifications to the matrix/clip state since the last save
     *  call, and loop around to process the next fold.
     *
     * @param canvas the [Canvas] on which to draw the view
     */
    override fun dispatchDraw(canvas: Canvas) {
        /* If prepareFold has not been called or if preparation has not completed yet,
         * then no custom drawing will take place so only need to invoke super's
         * onDraw and return. */
        if (!mIsFoldPrepared || mFoldFactor == 0f) {
            super.dispatchDraw(canvas)
            return
        }
        if (!mShouldDraw) {
            return
        }
        var src: Rect?
        /* Draws the bitmaps and shadows on the canvas with the appropriate transformations. */
        for (x in 0 until mNumberOfFolds) {
            src = mFoldRectArray[x]
            // The canvas is saved and restored for every individual fold
            @SuppressLint("UseKtx") // TODO: Canvas.withMatrix(){} Quick fix breaks the code
            canvas.save()

            // Concatenates the canvas with the transformation matrix for the
            // the segment of the view corresponding to the actual image being
            // displayed.
            canvas.concat(mMatrix[x])
            if (FoldingLayoutActivity.IS_JBMR2) {
                mDstRect!![0, 0, src!!.width()] = src.height()
                canvas.drawBitmap(mFullBitmap!!, src, mDstRect!!, null)
            } else {
                /* The same transformation matrix is used for both the shadow and the image
                 * segment. The canvas is clipped to account for the size of each fold and
                 * is translated so they are drawn in the right place. The shadow is then drawn on
                 * top of the different folds using the same transformation matrix.*/
                canvas.clipRect(0, 0, src!!.right - src.left, src.bottom - src.top)
                if (mIsHorizontal) {
                    canvas.translate(-src.left.toFloat(), 0f)
                } else {
                    canvas.translate(0f, -src.top.toFloat())
                }
                super.dispatchDraw(canvas)
                if (mIsHorizontal) {
                    canvas.translate(src.left.toFloat(), 0f)
                } else {
                    canvas.translate(0f, src.top.toFloat())
                }
            }
            /* Draws the shadows corresponding to this specific fold. */
            if (x % 2 == 0) {
                canvas.drawRect(0f, 0f, mFoldDrawWidth, mFoldDrawHeight, mSolidShadow!!)
            } else {
                canvas.drawRect(0f, 0f, mFoldDrawWidth, mFoldDrawHeight, mGradientShadow!!)
            }
            canvas.restore()
        }
    }

    companion object {
        /**
         * detail message for the NumberOfFoldingLayoutChildrenException that our method
         * [throwCustomException] throws if there are more than 1 child view added to us.
         */
        private const val FOLDING_VIEW_EXCEPTION_MESSAGE = "Folding Layout can only have 1 child at most"

        /**
         * Base alpha value for the shadows of the folds
         */
        private const val SHADING_ALPHA = 0.8f

        /**
         * Used for the x-coordinate for the end of the gradient line of the shadow gradient (for
         * HORIZONTAL folding) or the y-coordinate for the end of the gradient line of the shadow
         * gradient (for VERTICAL),
         */
        private const val SHADING_FACTOR = 0.5f

        /**
         * Constant used to create a 3D perspective scaling for the depth of a fold into the screen.
         */
        private const val DEPTH_CONSTANT = 1500

        /**
         * Number of points used when creating the transformation matrices using `setPolyToPoly`
         */
        private const val NUM_OF_POLY_POINTS = 8
    }
}