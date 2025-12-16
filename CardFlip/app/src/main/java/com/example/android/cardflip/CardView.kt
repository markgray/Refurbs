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
    "UNUSED_PARAMETER",
    "unused",
    "ReplaceJavaStaticMethodWithKotlinAnalog",
    "PrivatePropertyName",
    "MemberVisibilityCanBePrivate",
    "ReplaceNotNullAssertionWithElvisReturn",
    "RedundantSuppression"
)

package com.example.android.cardflip

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * This [CardView] object is a view which can flip horizontally about its edges,
 * as well as rotate clockwise or counter-clockwise about any of its corners. In
 * the middle of a flip animation, this view darkens to imitate a shadow-like effect.
 *
 * The key behind the design of this view is the fact that the layout parameters and
 * the animation properties of this view are updated and reset respectively after
 * every single animation. Therefore, every consecutive animation that this
 * view experiences is completely independent of what its prior state was.
 */
class CardView : androidx.appcompat.widget.AppCompatImageView {
    /**
     * Constants used to specify the corner of the card that is of interest.
     */
    enum class Corner {
        /**
         * Top left corner
         */
        TOP_LEFT,

        /**
         * Top right corner
         */
        TOP_RIGHT,

        /**
         * Bottom left corener
         */
        BOTTOM_LEFT,

        /**
         * Bottom right corner
         */
        BOTTOM_RIGHT
    }

    /**
     * The distance along the Z axis (orthogonal to the X/Y plane on which views are drawn) from the
     * camera to this view.
     */
    private val CAMERA_DISTANCE = 8000

    /**
     * Minimum duration of a card flip animation in milliseconds.
     */
    private val MIN_FLIP_DURATION = 300

    /**
     * Constant we divide the velocity of a flip gesture by when calculating animation duration value.
     */
    private val VELOCITY_TO_DURATION_CONSTANT = 15

    /**
     * Maximum duration of a card flip animation in milliseconds.
     */
    private val MAX_FLIP_DURATION = 700

    /**
     * Number of degrees to rotate a card for each card below it (each card is rotated by a different
     * amount so all the cards are visible when rotated out).
     */
    private val ROTATION_PER_CARD = 2

    /**
     * Number of milliseconds to delay the rotation of a card for each card below it (used for full
     * rotation animation).
     */
    private val ROTATION_DELAY_PER_CARD = 50

    /**
     * Duration of a full rotation animation in milliseconds.
     */
    private val ROTATION_DURATION = 2000

    /**
     * The width of the transparent border around the bitmaps to anti-alias the image as it rotates.
     */
    private val ANTIALIAS_BORDER = 1

    /**
     * [BitmapDrawable] used for the front of the card, loaded with the jpg with resource id
     * `R.drawable.red`
     */
    private var mFrontBitmapDrawable: BitmapDrawable? = null

    /**
     * [BitmapDrawable] used for the back of the card, loaded with the jpg with resource id
     * `R.drawable.blue`
     */
    private var mBackBitmapDrawable: BitmapDrawable? = null

    /**
     * [BitmapDrawable] currently being displayed by this [CardView], either [BitmapDrawable] field
     * [mFrontBitmapDrawable] or [BitmapDrawable] field [mBackBitmapDrawable]
     */
    private var mCurrentBitmapDrawable: BitmapDrawable? = null

    /**
     * Flag indicating that the front of the [CardView] is showing, toggled by our method
     * [toggleFrontShowing] which is called only by our method [flipHorizontally], it is used
     * by our method [updateDrawableBitmap] to decide which [BitmapDrawable] to use for
     * [BitmapDrawable] field [mCurrentBitmapDrawable] which it then displays, choosing
     * [mFrontBitmapDrawable] if `true`, or [mBackBitmapDrawable] if `false`.
     */
    private var mIsFrontShowing = true

    /**
     * Flag indicating that the [CardView] has been flipped, it is toggled by our method
     * [toggleIsHorizontallyFlipped] at the end of the animation created by our method
     * [flipHorizontally] to flip the card from one stack to the other. It starts out
     * `false` to indicate that the card is on the right hand stack. If is used by our
     * override of [onDraw] to decide whether it needs to concatenate [Matrix] field
     * [mHorizontalFlipMatrix] to the [Canvas] before calling our super's implementation
     * of `onDraw`. The matrix scales the canvas horizontally about its midpoint with the
     * X scaling factor -1.
     */
    private var mIsHorizontallyFlipped = false

    /**
     * [Matrix] used to scale the canvas horizontally about its midpoint with the X scaling
     * factor -1, it is used by our override of [onDraw] if [mIsHorizontallyFlipped] is `true`
     * to "flip" the card to its back before calling our super's implementation of `onDraw`.
     */
    private var mHorizontalFlipMatrix: Matrix? = null

    /**
     * The [CardFlipListener] whose [CardFlipListener.onCardFlipStart] and
     * [CardFlipListener.onCardFlipEnd] overrides are called when a card flip
     * animation begins and ends respectively.
     */
    private var mCardFlipListener: CardFlipListener? = null

    /**
     * Simple constructor to use when creating a [CardView] from code. We first call our super's
     * constructor, then call our [initialize] method to initialize this instance.
     *
     * @param context The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     */
    constructor(context: Context?) : super(context!!) {
        initialize(context)
    }

    /**
     * Perform inflation from XML. First we call our super's constructor, then call our [initialize]
     * method to initialize this instance. UNUSED.
     *
     * @param context The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        initialize(context)
    }

    /**
     * Loads the bitmap drawables used for the front and back for this card. First we initialize our
     * [Matrix] field [mHorizontalFlipMatrix] with a new instance, then we set the distance along
     * the Z axis (orthogonal to the X/Y plane on which views are drawn) from the camera to this
     * view to our constant [CAMERA_DISTANCE] (8000). We initialize our [BitmapDrawable] field
     * [mFrontBitmapDrawable] with the [BitmapDrawable] created by our method [bitmapWithBorder]
     * from the jpg with resource id `R.drawable.red`, and [BitmapDrawable] field [mBackBitmapDrawable]
     * with the [BitmapDrawable] created by our method [bitmapWithBorder] from the jpg with resource
     * id `R.drawable.blue`. We then call our [updateDrawableBitmap] method to set [BitmapDrawable]
     * field [mCurrentBitmapDrawable] to [mFrontBitmapDrawable] (since [mIsFrontShowing] starts out
     * `true`) and then set the content of our [ImageView] to [mCurrentBitmapDrawable].
     *
     * @param context The [Context] the view is running in, UNUSED.
     */
    fun initialize(context: Context?) {
        mHorizontalFlipMatrix = Matrix()
        cameraDistance = CAMERA_DISTANCE.toFloat()
        mFrontBitmapDrawable =
            bitmapWithBorder(
                ResourcesCompat.getDrawable(
                    /* res = */ resources,
                    /* id = */ R.drawable.red,
                    /* theme = */ null
                ) as BitmapDrawable
            )
        mBackBitmapDrawable =
            bitmapWithBorder(
                ResourcesCompat.getDrawable(
                    /* res = */ resources,
                    /* id = */ R.drawable.blue,
                    /* theme = */ null
                ) as BitmapDrawable
            )
        updateDrawableBitmap()
    }

    /**
     * Adding a 1 pixel transparent border around the bitmap can be used to anti-alias the image as
     * it rotates. We initialize [Bitmap] variable `val bitmapWithBorder` with an instance whose
     * width and height are both 2 pixels larger than the width and height of our [BitmapDrawable]
     * parameter [bitmapDrawable], then initialize [Canvas] variable `val canvas` with an instance
     * which will draw into `bitmapWithBorder`. We then call the [Canvas.drawBitmap] method of
     * `canvas` to draw the [Bitmap] of `bitmapDrawable` into itself with the upper left corner at
     * (1,1). Finally we return the [BitmapDrawable] created from [Bitmap] `bitmapWithBorder` setting
     * initial target density based on the display metrics of the resources associated with this view.
     *
     * @param bitmapDrawable `BitmapDrawable` we want to surround with a 1 pixel transparent border
     * @return A `BitmapDrawable` containing our parameter `BitmapDrawable bitmapDrawable`
     * surrounded by a 1 pixel transparent border.
     */
    private fun bitmapWithBorder(bitmapDrawable: BitmapDrawable): BitmapDrawable {
        val bitmapWithBorder = createBitmap(
            width = bitmapDrawable.intrinsicWidth + ANTIALIAS_BORDER * 2,
            height = bitmapDrawable.intrinsicHeight + ANTIALIAS_BORDER * 2
        )
        val canvas = Canvas(bitmapWithBorder)
        canvas.drawBitmap(
            /* bitmap = */ bitmapDrawable.bitmap,
            /* left = */ ANTIALIAS_BORDER.toFloat(),
            /* top = */ ANTIALIAS_BORDER.toFloat(),
            /* paint = */ null
        )
        return bitmapWithBorder.toDrawable(resources = resources)
    }

    /**
     * Initiates a horizontal flip from right to left. First we call the [View.setPivotX] method
     * (aka kotlin `pivotX` property) to set the X location of the point around which our [View] is
     * rotated or scaled to 0. Then we call our method [flipHorizontally] to flip our [CardView]
     * counter clockwise at a velocity of our [Int] parameter [velocity] with our [Int] parameter
     * [numberInPile] as the number of cards underneath this card in the new pile so as to properly
     * adjust its position offset in the stack.
     *
     * @param numberInPile total number of cards in the left hand stack before the flip completes.
     * @param velocity     calculated from the velocity of the fling, and used to vary the duration of
     * the animation of the card flip.
     */
    fun flipRightToLeft(numberInPile: Int, velocity: Int) {
        pivotX = 0f
        flipHorizontally(numberInPile = numberInPile, clockwise = false, velocity = velocity)
    }

    /**
     * Initiates a horizontal flip from left to right. First we call the [View.setPivotX] method
     * (aka kotlin `pivotX` property) to set the X location of the point around which our [View] is
     * rotated or scaled to the width of our [View]. Then we call our method [flipHorizontally] to
     * flip our [CardView] clockwise at a velocity of our [Int] parameter [velocity] with our [Int]
     * parameter [numberInPile] as the number of cards underneath this card in the new pile so as to
     * properly adjust its position offset in the stack.
     *
     * @param numberInPile total number of cards in the left hand stack before the flip completes.
     * @param velocity     calculated from the velocity of the fling, and used to vary the duration
     * of the animation of the card flip.
     */
    fun flipLeftToRight(numberInPile: Int, velocity: Int) {
        pivotX = width.toFloat()
        flipHorizontally(numberInPile = numberInPile, clockwise = true, velocity = velocity)
    }

    /**
     * Animates a horizontal (about the y-axis) flip of this card. First we call our method
     * [toggleFrontShowing] to toggle the [mIsFrontShowing] flag. We initialize our
     * [PropertyValuesHolder] variable `val rotation` with an instance for the property
     * [View.ROTATION_Y] using a value of 180 if our [Boolean] parameter [clockwise] is `true`,
     * or minus 180 if it is `false`. We initialize [PropertyValuesHolder] variable `val xOffset`
     * with an instance for the property [View.TRANSLATION_X] and the value [CardFlip.CARD_PILE_OFFSET]
     * times our [Int] parameter [numberInPile] and [PropertyValuesHolder] variable `val yOffset`
     * with an instance for the property [View.TRANSLATION_Y] with the same value. We initialize
     * [ObjectAnimator] variable `val cardAnimator` with an instance that will animate the properties
     * held in `rotation`, `xOffset`, and `yOffset` of `this` [CardView]. We then add an
     * [AnimatorUpdateListener] to `cardAnimator` whose [AnimatorUpdateListener.onAnimationUpdate]
     * override checks if the elapsed/interpolated fraction of the animation it is listening to is
     * greater than or equal to 0.5 and if it is calls our [updateDrawableBitmap] method to update
     * the visible bitmap of this view so that the correct front or back image is being shown.
     *
     * We initialize [Keyframe] variable `val shadowKeyFrameStart` with an instance whose time is 0
     * and whose value is 0, [Keyframe] variable `val shadowKeyFrameMid` with an instance whose time
     * is 0.5 and whose value is 1, and [Keyframe] variable `val shadowKeyFrameEnd` with an instance
     * whose time is 1 and whose value is 0. We initialize [PropertyValuesHolder] variable
     * `val shadowPropertyValuesHolder` with an instance for the property "shadow" and the
     * [Keyframe]'s `shadowKeyFrameStart`, `shadowKeyFrameMid` and `shadowKeyFrameEnd`. We then
     * initialize [ObjectAnimator] variable `val colorizer` to an instance that will animate
     * `shadowPropertyValuesHolder` on `this` [CardView].
     *
     * We then call the [CardFlipListener.onCardFlipStart] override of our [CardFlipListener] field
     * [mCardFlipListener], initialize [AnimatorSet] variable `val set` with a new instance,
     * initialize [Int] variable `var duration` to a value calculated by subtracting the absolute
     * value of our [Int] parameter [velocity] divided by the constant [VELOCITY_TO_DURATION_CONSTANT]
     * (15) from the constant [MAX_FLIP_DURATION] (700). If this is less than the constant
     * [MIN_FLIP_DURATION] (300) we set `duration` to [MIN_FLIP_DURATION]. We then set the duration
     * of `set` to `duration`, set it up to play `cardAnimator` and `colorizer` together, set its
     * interpolator to an [AccelerateDecelerateInterpolator], and add an [AnimatorListenerAdapter]
     * whose [AnimatorListenerAdapter.onAnimationEnd] override calls our [toggleIsHorizontallyFlipped]
     * method to toggle the [mIsHorizontallyFlipped] flag and invalidate our view, calls our
     * [updateDrawableBitmap] method to update our image, calls our [updateLayoutParams] method to
     * update our layout parameters to their new values, and finally calls the
     * [CardFlipListener.onCardFlipEnd] override of our [CardFlipListener] field [mCardFlipListener]`.
     *
     * Having done all this, we start `set` running.
     *
     * @param numberInPile Specifies how many cards are underneath this card in the new pile so as
     * to properly adjust its position offset in the stack.
     * @param clockwise    Specifies whether the horizontal animation is 180 degrees clockwise or
     * 180 degrees counter clockwise.
     * @param velocity     calculated from the velocity of the fling, and used to vary the duration
     * of the animation of the card flip.
     */
    fun flipHorizontally(numberInPile: Int, clockwise: Boolean, velocity: Int) {
        toggleFrontShowing()
        val rotation: PropertyValuesHolder = PropertyValuesHolder.ofFloat(
            /* property = */ ROTATION_Y,
            /* ...values = */ (if (clockwise) 180 else -180).toFloat()
        )
        val xOffset = PropertyValuesHolder.ofFloat(
            /* property = */ TRANSLATION_X,
            /* ...values = */ (numberInPile * CardFlip.CARD_PILE_OFFSET).toFloat()
        )
        val yOffset = PropertyValuesHolder.ofFloat(
            /* property = */ TRANSLATION_Y,
            /* ...values = */ (numberInPile * CardFlip.CARD_PILE_OFFSET).toFloat()
        )
        val cardAnimator = ObjectAnimator.ofPropertyValuesHolder(
            /* target = */ this,
            /* ...values = */ rotation, xOffset, yOffset
        )
        cardAnimator.addUpdateListener { valueAnimator: ValueAnimator ->
            if (valueAnimator.animatedFraction >= 0.5) {
                updateDrawableBitmap()
            }
        }
        val shadowKeyFrameStart = Keyframe.ofFloat(/* fraction = */ 0f, /* value = */ 0f)
        val shadowKeyFrameMid = Keyframe.ofFloat(/* fraction = */ 0.5f, /* value = */ 1f)
        val shadowKeyFrameEnd = Keyframe.ofFloat(/* fraction = */ 1f, /* value = */ 0f)
        val shadowPropertyValuesHolder = PropertyValuesHolder.ofKeyframe(
            /* propertyName = */ "shadow",
            /* ...values = */ shadowKeyFrameStart, shadowKeyFrameMid, shadowKeyFrameEnd
        )
        val colorizer = ObjectAnimator.ofPropertyValuesHolder(
            /* target = */ this,
            /* ...values = */ shadowPropertyValuesHolder
        )
        mCardFlipListener!!.onCardFlipStart()
        val set = AnimatorSet()
        var duration = MAX_FLIP_DURATION - (Math.abs(velocity) / VELOCITY_TO_DURATION_CONSTANT)
        duration = if (duration < MIN_FLIP_DURATION) MIN_FLIP_DURATION else duration
        set.duration = duration.toLong()
        set.playTogether(/* ...items = */ cardAnimator, colorizer)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. This callback is not invoked for animations with
             * repeat count set to `INFINITE`. We call our [toggleIsHorizontallyFlipped] method to
             * toggle the [mIsHorizontallyFlipped] flag and invalidate our view, call our
             * [updateDrawableBitmap] method to update our image, call our [updateLayoutParams]
             * method to update our layout parameters to their new values, and finally
             * call the [CardFlipListener.onCardFlipEnd] override of our field [mCardFlipListener].
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                toggleIsHorizontallyFlipped()
                updateDrawableBitmap()
                updateLayoutParams()
                mCardFlipListener!!.onCardFlipEnd()
            }
        })
        set.start()
    }

    /**
     * Darkens this [ImageView]'s image by applying a shadow color filter over it. Called from a
     * [PropertyValuesHolder] animation. We initialize [Int] variable `val colorValue` by
     * subtracting 200 times our parameter [value] from 255. We then set our color filter to a rgb
     * color that uses `colorValue` for all components, using a [PorterDuff.Mode.MULTIPLY] mode.
     *
     * @param value amount to darken the image by, 0f to 1f.
     */
    fun setShadow(value: Float) {
        val colorValue: Int = (255 - 200 * value).toInt()
        setColorFilter(
            /* color = */ Color.rgb(colorValue, colorValue, colorValue),
            /* mode = */ PorterDuff.Mode.MULTIPLY
        )
    }

    /**
     * Convenience function to toggle the value of our [Boolean] field [mIsFrontShowing].
     */
    fun toggleFrontShowing() {
        mIsFrontShowing = !mIsFrontShowing
    }

    /**
     * Convenience function to toggle the value of our [Boolean] field [mIsHorizontallyFlipped],
     * and then call [invalidate] to invalidate the whole view so that [onDraw] will be called.
     */
    fun toggleIsHorizontallyFlipped() {
        mIsHorizontallyFlipped = !mIsHorizontallyFlipped
        invalidate()
    }

    /**
     * This is called during layout when the size of this view has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0. First we call our super's
     * implementation of `onSizeChanged`, then we set [Matrix] field [mHorizontalFlipMatrix] to
     * scale X by -1, Y by 1, around the pivot point (`w/2`,`h/2` (flips the [CardView] over when
     * concatenated to the [Canvas] matrix before drawing).
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mHorizontalFlipMatrix!!.setScale(
            /* sx = */ -1f,
            /* sy = */ 1f,
            /* px = */ (w / 2).toFloat(),
            /* py = */ (h / 2).toFloat()
        )
    }

    /**
     * We implement this to do our drawing. If our [Boolean] field [mIsHorizontallyFlipped] is `true`
     * we concatenate the current matrix of our [Canvas] parameter [canvas] with our [Matrix] field
     * [mHorizontalFlipMatrix] in order to scale the canvas horizontally about its midpoint in the
     * case that the card is in a horizontally flipped state ([mHorizontalFlipMatrix] scales X by
     * -1). In any case we then call our super's implementation of `onDraw` to draw us.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        if (mIsHorizontallyFlipped) {
            canvas.concat(mHorizontalFlipMatrix)
        }
        super.onDraw(canvas)
    }

    /**
     * Updates the layout parameters of this view so as to reset the `rotationX` and `rotationY`
     * parameters, and remain independent of its previous position, while also maintaining its
     * current position in the layout. We initialize [RelativeLayout.LayoutParams] variable
     * `val params` by retrieving the layout parameters for this view, then we set its `leftMargin`
     * field to its current "real" value which we calculate by adding to it the absolute value of
     * its rotation around the vertical axis through the pivot point modulo 360 divided by 180,
     * times 2 times the X coordinate of its pivot point minus the width of our view (this comes out
     * to 0 when the card has just been flipped to the left stack, and (on a pixel) to 540 (half its
     * width) when it has been flipped to the right stack). We then set our X rotation to 0 and our
     * Y rotation to 0 and set our view's layout params to `params`.
     */
    fun updateLayoutParams() {
        val params = layoutParams as RelativeLayout.LayoutParams
        params.leftMargin = (
            params.leftMargin + Math.abs(rotationY) % 360 / 180 * (2 * pivotX - width)
            ).toInt()
        rotationX = 0f
        rotationY = 0f
        layoutParams = params
    }

    /**
     * Toggles the visible bitmap of this view between its front and back drawables. If our field
     * [mIsFrontShowing] is `true` we set [mCurrentBitmapDrawable] to [mFrontBitmapDrawable],
     * if `false` we set it to [mBackBitmapDrawable]. Then we set the content of our [ImageView]
     * to [mCurrentBitmapDrawable].
     */
    fun updateDrawableBitmap() {
        mCurrentBitmapDrawable = if (mIsFrontShowing) mFrontBitmapDrawable else mBackBitmapDrawable
        setImageDrawable(mCurrentBitmapDrawable)
    }

    /**
     * Sets the appropriate translation of this card depending on how many cards are in the pile
     * underneath it. We call the [setTranslationX] method (kotlin `translationX` property) to set
     * the horizontal location of our view relative to its left position to [CardFlip.CARD_PILE_OFFSET]
     * times our [Int] parameter [numInPile] and call the [setTranslationY] method (kotlin
     * `translationX` property) to set the Y offset to the same value.
     *
     * @param numInPile number of cards underneath us.
     */
    fun updateTranslation(numInPile: Int) {
        translationX = (CardFlip.CARD_PILE_OFFSET * numInPile).toFloat()
        translationY = (CardFlip.CARD_PILE_OFFSET * numInPile).toFloat()
    }

    /**
     * Returns a rotation animation which rotates this card by some degree about one of its corners
     * either in the clockwise or counter-clockwise direction. Depending on how many cards lie below
     * this one in the stack, this card will be rotated by a different amount so all the cards are
     * visible when rotated out. First we call our [rotateCardAroundCorner] to set our pivot point
     * to rotate around our [Corner] parameter [corner]. Then we initialize [Int] variable
     * `var rotation` to [ROTATION_PER_CARD] times our [Int] parameter [cardFromTop]. If our
     * [Boolean] parameter [isClockwise] is `false` we negate `rotation`, if our [Boolean] parameter
     * [isRotatingOut] is `false` we set `rotation` to 0. Finally we return an [ObjectAnimator]
     * created to animate the `ROTATION` property of `this` to `rotation`.
     *
     * @param cardFromTop how far are we from the top, goes from 0 to one less than the size
     * of the stack.
     * @param corner which corner to rotate about, always [Corner.BOTTOM_LEFT] in our usage.
     * @param isRotatingOut if `true` fans the card stack out, if `false` rotates it back to a stack.
     * @param isClockwise if `true` we are rotating clock wise, if `false` counter clockwise.
     * @return An [ObjectAnimator] that animates the `ROTATION` property of 'this'
     */
    fun getRotationAnimator(
        cardFromTop: Int,
        corner: Corner?,
        isRotatingOut: Boolean,
        isClockwise: Boolean
    ): ObjectAnimator {
        rotateCardAroundCorner(corner)
        var rotation: Int = cardFromTop * ROTATION_PER_CARD
        if (!isClockwise) {
            rotation = -rotation
        }
        if (!isRotatingOut) {
            rotation = 0
        }
        return ObjectAnimator.ofFloat(
            /* target = */ this,
            /* property = */ ROTATION,
            /* ...values = */ rotation.toFloat()
        )
    }

    /**
     * Returns a full rotation animator which rotates this card by 360 degrees about one of its
     * corners either in the clockwise or counter-clockwise direction. Depending on how many cards
     * lie below this one in the stack, a different start delay is applied to the animation so the
     * cards don't all animate at once. First we initialize [Int] variable `val currentRotation`
     * with the current rotation of our view, then call our [rotateCardAroundCorner] method to set
     * our pivot point to be around our [Corner] parameter [corner]. We then initialize [Int] variable
     * `var rotation` to 360 minus `currentRotation`, and if our [Boolean] parameter [isClockwise]
     * is `false` we negate it. We initialize [ObjectAnimator] `val animator` with an instance which
     * animates the `ROTATION` property of `this` to `rotation`, set its start delay to
     * [ROTATION_DELAY_PER_CARD] times our [Int] parameter [cardFromTop] and set its duration to
     * [ROTATION_DURATION]. Then we add an [AnimatorListenerAdapter] whose
     * [AnimatorListenerAdapter.onAnimationEnd] override sets the rotation of our view to
     * `currentRotation` when the animation ends.
     *
     * Finally we return `animator` to the caller.
     *
     * @param cardFromTop how far are we from the top, goes from 0 to one less than the size
     * of the stack.
     * @param corner which corner to rotate about, always [Corner.BOTTOM_LEFT] in our usage.
     * @param isClockwise if `true` we are rotating clock wise, if `false` counter clockwise.
     * @return An [ObjectAnimator] that animates the ROTATION property of 'this'
     */
    fun getFullRotationAnimator(
        cardFromTop: Int,
        corner: Corner?,
        isClockwise: Boolean
    ): ObjectAnimator {
        val currentRotation = rotation.toInt()
        rotateCardAroundCorner(corner)
        var rotation = 360 - currentRotation
        rotation = if (isClockwise) rotation else -rotation
        val animator = ObjectAnimator.ofFloat(
            /* target = */ this,
            /* property = */ ROTATION,
            /* ...values = */ rotation.toFloat()
        )
        animator.startDelay = (ROTATION_DELAY_PER_CARD * cardFromTop).toLong()
        animator.duration = ROTATION_DURATION.toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. We just call the `setRotation` method to set
             * our rotation about the pivot point to `currentRotation`.
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                setRotation(currentRotation.toFloat())
            }
        })
        return animator
    }

    /**
     * Sets the appropriate pivot of this card so that it can be rotated about any one of its four
     * corners. We `when` switch on the value of our [Corner] parameter [corner]:
     *
     *  * [Corner.TOP_LEFT]: we set the X coordinate of our pivot point to 0, and the Y coordinate
     *  of our pivot point to 0.
     *
     *  * [Corner.TOP_RIGHT]: we set the X coordinate of our pivot point to the width of our view,
     *  and the Y coordinate of our pivot point to 0.
     *
     *  * [Corner.BOTTOM_LEFT]: we set the X coordinate of our pivot point to 0, and the Y
     *  coordinate of our pivot point to the height of our view.
     *
     *  * [Corner.BOTTOM_RIGHT]: we set the X coordinate of our pivot point to the width of our
     *  view, and the Y coordinate of our pivot point to the height of our view.
     *
     * * `else` we do nothing.
     *
     * @param corner which [Corner] to rotate about, always [Corner.BOTTOM_LEFT] in our usage.
     */
    fun rotateCardAroundCorner(corner: Corner?) {
        when (corner) {
            Corner.TOP_LEFT -> {
                pivotX = 0f
                pivotY = 0f
            }

            Corner.TOP_RIGHT -> {
                pivotX = width.toFloat()
                pivotY = 0f
            }

            Corner.BOTTOM_LEFT -> {
                pivotX = 0f
                pivotY = height.toFloat()
            }

            Corner.BOTTOM_RIGHT -> {
                pivotX = width.toFloat()
                pivotY = height.toFloat()
            }

            else -> {}
        }
    }

    /**
     * Setter for our [CardFlipListener] field [mCardFlipListener].
     *
     * @param cardFlipListener [CardFlipListener] whose [CardFlipListener.onCardFlipEnd] and
     * [CardFlipListener.onCardFlipStart] overrides we are supposed to call.
     */
    fun setCardFlipListener(cardFlipListener: CardFlipListener?) {
        mCardFlipListener = cardFlipListener
    }
}
