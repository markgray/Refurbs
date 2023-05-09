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
@file:Suppress("UNUSED_PARAMETER", "unused", "DEPRECATION", "ReplaceJavaStaticMethodWithKotlinAnalog", "PrivatePropertyName", "MemberVisibilityCanBePrivate")

package com.example.android.cardflip

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout

/**
 * This CardView object is a view which can flip horizontally about its edges,
 * as well as rotate clockwise or counter-clockwise about any of its corners. In
 * the middle of a flip animation, this view darkens to imitate a shadow-like effect.
 *
 * The key behind the design of this view is the fact that the layout parameters and
 * the animation properties of this view are updated and reset respectively after
 * every single animation. Therefore, every consecutive animation that this
 * view experiences is completely independent of what its prior state was.
 */
class CardView : ImageView {
    /**
     * Constants used to specify the corner of the card that is of interest.
     */
    enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
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
     * `BitmapDrawable` used for the front of the card, loaded with the jpg with resource id
     * R.drawable.red
     */
    private var mFrontBitmapDrawable: BitmapDrawable? = null

    /**
     * `BitmapDrawable` used for the back of the card, loaded with the jpg with resource id
     * R.drawable.blue
     */
    private var mBackBitmapDrawable: BitmapDrawable? = null

    /**
     * `BitmapDrawable` currently being displayed by this `CardView`, either
     * `BitmapDrawable mFrontBitmapDrawable` or `BitmapDrawable mBackBitmapDrawable`
     */
    private var mCurrentBitmapDrawable: BitmapDrawable? = null

    /**
     * Flag indicating that the front of the `CardView` is showing, toggled by our method
     * `toggleFrontShowing` which is called only by our method `flipHorizontally`, it
     * is used by our method `updateDrawableBitmap` to decide which `BitmapDrawable`
     * to use for `BitmapDrawable mCurrentBitmapDrawable` which it then displays, choosing
     * `mFrontBitmapDrawable` if true, or `mBackBitmapDrawable` if false.
     */
    private var mIsFrontShowing = true

    /**
     * Flag indicating that the `CardView` has been flipped, it is toggled by our method
     * `toggleIsHorizontallyFlipped` at the end of the animation created by our method
     * `flipHorizontally` to flip the card from one stack to the other. It starts out
     * false to indicate that the card is on the right hand stack. If is used by our override of
     * `onDraw` to decide whether it needs to concatenate `Matrix mHorizontalFlipMatrix`
     * to the `Canvas` before calling our super's implementation of `onDraw`. The matrix
     * scales the canvas horizontally about its midpoint with the X scaling factor -1.
     */
    private var mIsHorizontallyFlipped = false

    /**
     * Matrix used to scale the canvas horizontally about its midpoint with the X scaling factor -1,
     * it is used by our override of `onDraw` if `mIsHorizontallyFlipped` is true to
     * "flip" the card to its back before calling our super's implementation of `onDraw`.
     */
    private var mHorizontalFlipMatrix: Matrix? = null

    /**
     * The `CardFlipListener` whose `onCardFlipStart` and `onCardFlipEnd` overrides
     * are called when a card flip animation begins and ends respectively.
     */
    private var mCardFlipListener: CardFlipListener? = null

    /**
     * Simple constructor to use when creating a `CardView` from code. We first call our super's
     * constructor, then call our `init` method to initialize this instance.
     *
     * @param context The Context the view is running in, through which it can access the current
     * theme, resources, etc.
     */
    constructor(context: Context?) : super(context) {
        init(context)
    }

    /**
     * Perform inflation from XML. First we call our super's constructor, then call our `init`
     * method to initialize this instance. UNUSED.
     *
     * @param context The Context the view is running in, through which it can access the current
     * theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    /**
     * Loads the bitmap drawables used for the front and back for this card. First we initialize our
     * field `Matrix mHorizontalFlipMatrix` with a new instance, then we set the distance along
     * the Z axis (orthogonal to the X/Y plane on which views are drawn) from the camera to this view
     * to our constant CAMERA_DISTANCE (8000). We initialize our field `BitmapDrawable mFrontBitmapDrawable`
     * with the `BitmapDrawable` created by our method `bitmapWithBorder` from the jpg with
     * resource id R.drawable.red, and `BitmapDrawable mBackBitmapDrawable` with the `BitmapDrawable`
     * created by our method `bitmapWithBorder` from the jpg with resource id R.drawable.blue.
     * We then call our `updateDrawableBitmap` method to set `mCurrentBitmapDrawable` to
     * `mFrontBitmapDrawable` (since `mIsFrontShowing` starts our true) and then set the
     * content of our `ImageView` to `mCurrentBitmapDrawable`.
     *
     * @param context The Context the view is running in, UNUSED.
     */
    fun init(context: Context?) {
        mHorizontalFlipMatrix = Matrix()
        cameraDistance = CAMERA_DISTANCE.toFloat()
        mFrontBitmapDrawable = bitmapWithBorder(resources
            .getDrawable(R.drawable.red) as BitmapDrawable)
        mBackBitmapDrawable = bitmapWithBorder(resources
            .getDrawable(R.drawable.blue) as BitmapDrawable)
        updateDrawableBitmap()
    }

    /**
     * Adding a 1 pixel transparent border around the bitmap can be used to anti-alias the image as
     * it rotates. We initialize `Bitmap bitmapWithBorder` with an instance whose width and
     * height are both 2 pixels larger than the width and height of our parameter `bitmapDrawable`,
     * then initialize `Canvas canvas` with an instance which will draw into `bitmapWithBorder`.
     * We then call the `drawBitmap` of `canvas` to draw the `Bitmap` of `bitmapDrawable`
     * into itself with the upper left corner at (1,1). Finally we return the `BitmapDrawable`
     * created from `Bitmap bitmapWithBorder` setting initial target density based on the display
     * metrics of the resources associated with this view.
     *
     * @param bitmapDrawable `BitmapDrawable` we want to surround with a 1 pixel transparent border
     * @return A `BitmapDrawable` containing our parameter `BitmapDrawable bitmapDrawable`
     * surrounded by a 1 pixel transparent border.
     */
    private fun bitmapWithBorder(bitmapDrawable: BitmapDrawable): BitmapDrawable {
        val bitmapWithBorder = Bitmap.createBitmap(bitmapDrawable.intrinsicWidth +
            ANTIALIAS_BORDER * 2, bitmapDrawable.intrinsicHeight + ANTIALIAS_BORDER * 2,
            Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapWithBorder)
        canvas.drawBitmap(bitmapDrawable.bitmap, ANTIALIAS_BORDER.toFloat(), ANTIALIAS_BORDER.toFloat(), null)
        return BitmapDrawable(resources, bitmapWithBorder)
    }

    /**
     * Initiates a horizontal flip from right to left. First we call the `setPivotX` method to
     * set the X location of the point around which our `View` is rotated or scaled to 0. Then
     * we call our method `flipHorizontally` to flip our `CardView` counter clockwise at
     * a velocity of `velocity` with `numberInPile` as the number of cards underneath this
     * card in the new pile so as to properly adjust its position offset in the stack.
     *
     * @param numberInPile total number of cards in the left hand stack before the flip completes.
     * @param velocity     calculated from the velocity of the fling, and used to vary the duration of
     * the animation of the card flip.
     */
    fun flipRightToLeft(numberInPile: Int, velocity: Int) {
        pivotX = 0f
        flipHorizontally(numberInPile, false, velocity)
    }

    /**
     * Initiates a horizontal flip from left to right. First we call the `setPivotX` method to
     * set the X location of the point around which our `View` is rotated or scaled to the width
     * of our `View`. Then we call our method `flipHorizontally` to flip our `CardView`
     * clockwise at a velocity of `velocity` with `numberInPile` as the number of cards
     * underneath this card in the new pile so as to properly adjust its position offset in the stack.
     *
     * @param numberInPile total number of cards in the left hand stack before the flip completes.
     * @param velocity     calculated from the velocity of the fling, and used to vary the duration of
     * the animation of the card flip.
     */
    fun flipLeftToRight(numberInPile: Int, velocity: Int) {
        pivotX = width.toFloat()
        flipHorizontally(numberInPile, true, velocity)
    }

    /**
     * Animates a horizontal (about the y-axis) flip of this card. First we call our method
     * `toggleFrontShowing` to toggle the `mIsFrontShowing` flag. We initialize our
     * variable `PropertyValuesHolder rotation` with an instance for the property ROTATION_Y
     * using a value of 180 if our parameter `clockwise` is true, or 180 if it is false. We
     * initialize `PropertyValuesHolder xOffset` with an instance for the property TRANSLATION_X
     * and the value CardFlip.CARD_PILE_OFFSET times our parameter `numberInPile` and
     * `PropertyValuesHolder yOffset` with an instance for the property TRANSLATION_Y with the
     * same value. We initialize `ObjectAnimator cardAnimator` with an instance that will
     * animate the properties held in `rotation`, `xOffset`, and `yOffset` of 'this'.
     * We then add an `AnimatorUpdateListener` to `cardAnimator` whose `onAnimationUpdate`
     * override checks if the elapsed/interpolated fraction of the animation it is listening too is
     * greater than or equal to 0.5 and if it is calls our `updateDrawableBitmap` method to update
     * the visible bitmap of this view so that the correct front or back image is being shown.
     *
     *
     * We initialize `Keyframe shadowKeyFrameStart` with an instance whose time is 0 and whose
     * value is 0, `Keyframe shadowKeyFrameMid` with an instance whose time is 0.5 and whose
     * value is 1, and `Keyframe shadowKeyFrameEnd` with an instance whose time is 1 and whose
     * value is 0. We initialize `PropertyValuesHolder shadowPropertyValuesHolder` with an
     * for the property "shadow" and the `Keyframe`'s `shadowKeyFrameStart`, `shadowKeyFrameMid`
     * and `shadowKeyFrameEnd`. We then initialize `ObjectAnimator colorizer` to an instance
     * that will animate `shadowPropertyValuesHolder` on 'this'.
     *
     *
     * We then call the `onCardFlipStart` override of our field `CardFlipListener mCardFlipListener`,
     * initialize `AnimatorSet set` with a new instance, initialize `int duration` to a value
     * calculated by subtracting the absolute value of our parameter `velocity` divided by the constant
     * VELOCITY_TO_DURATION_CONSTANT (15) from the constant MAX_FLIP_DURATION (700). If this is less than
     * the constant MIN_FLIP_DURATION (300) we set `duration` to MIN_FLIP_DURATION. We then set the
     * duration of `set` to `duration`, set it up to play `cardAnimator` and `colorizer`
     * together, add an `AnimatorListenerAdapter` whose `onAnimationEnd` override calls our
     * `toggleIsHorizontallyFlipped` method to toggle the `mIsHorizontallyFlipped` flag and
     * invalidate our view, calls our `updateDrawableBitmap` method to update our image, calls our
     * `updateLayoutParams` method to update our layout parameters to their new values, and finally
     * calls the `onCardFlipEnd` override of our field `CardFlipListener mCardFlipListener`.
     *
     *
     * Having done all this, we start `set` running.
     *
     * @param numberInPile Specifies how many cards are underneath this card in the new
     * pile so as to properly adjust its position offset in the stack.
     * @param clockwise    Specifies whether the horizontal animation is 180 degrees
     * clockwise or 180 degrees counter clockwise.
     * @param velocity     calculated from the velocity of the fling, and used to vary the duration of
     * the animation of the card flip.
     */
    fun flipHorizontally(numberInPile: Int, clockwise: Boolean, velocity: Int) {
        toggleFrontShowing()
        val rotation = PropertyValuesHolder.ofFloat(ROTATION_Y,
            (
                if (clockwise) 180 else -180).toFloat())
        val xOffset = PropertyValuesHolder.ofFloat(TRANSLATION_X,
            (
                numberInPile * CardFlip.CARD_PILE_OFFSET).toFloat())
        val yOffset = PropertyValuesHolder.ofFloat(TRANSLATION_Y,
            (
                numberInPile * CardFlip.CARD_PILE_OFFSET).toFloat())
        val cardAnimator = ObjectAnimator.ofPropertyValuesHolder(this, rotation,
            xOffset, yOffset)
        cardAnimator.addUpdateListener { valueAnimator ->
            if (valueAnimator.animatedFraction >= 0.5) {
                updateDrawableBitmap()
            }
        }
        val shadowKeyFrameStart = Keyframe.ofFloat(0f, 0f)
        val shadowKeyFrameMid = Keyframe.ofFloat(0.5f, 1f)
        val shadowKeyFrameEnd = Keyframe.ofFloat(1f, 0f)
        val shadowPropertyValuesHolder = PropertyValuesHolder.ofKeyframe("shadow", shadowKeyFrameStart, shadowKeyFrameMid, shadowKeyFrameEnd)
        val colorizer = ObjectAnimator.ofPropertyValuesHolder(this,
            shadowPropertyValuesHolder)
        mCardFlipListener!!.onCardFlipStart()
        val set = AnimatorSet()
        var duration = MAX_FLIP_DURATION - Math.abs(velocity) / VELOCITY_TO_DURATION_CONSTANT
        duration = if (duration < MIN_FLIP_DURATION) MIN_FLIP_DURATION else duration
        set.duration = duration.toLong()
        set.playTogether(cardAnimator, colorizer)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
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
     * Darkens this ImageView's image by applying a shadow color filter over it. Called from a
     * `PropertyValuesHolder` animation. We initialize `int colorValue` by subtracting
     * 200 times our parameter `value` from 255. We then set our color filter to a rgb color
     * that uses `colorValue` for all components, using a MULTIPLY PorterDuff mode.
     *
     * @param value amount to darken the image by, 0 to 1.
     */
    fun setShadow(value: Float) {
        val colorValue = (255 - 200 * value).toInt()
        setColorFilter(Color.rgb(colorValue, colorValue, colorValue),
            PorterDuff.Mode.MULTIPLY)
    }

    /**
     * Convenience function to toggle the value of our `boolean mIsFrontShowing` field
     */
    fun toggleFrontShowing() {
        mIsFrontShowing = !mIsFrontShowing
    }

    /**
     * Convenience function to toggle the value of our `boolean mIsHorizontallyFlipped` field
     */
    fun toggleIsHorizontallyFlipped() {
        mIsHorizontallyFlipped = !mIsHorizontallyFlipped
        invalidate()
    }

    /**
     * This is called during layout when the size of this view has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0. First we call our super's
     * implementation of `onSizeChanged`, then we set `mHorizontalFlipMatrix` to scale
     * X by -1, Y by 1, around the pivot point (`w/2`,`h/2` (flips the `CardView`
     * over when concatenated to the `Canvas` matrix before drawing).
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mHorizontalFlipMatrix!!.setScale(-1f, 1f, (w / 2).toFloat(), (h / 2).toFloat())
    }

    /**
     * We implement this to do our drawing. If our flag `mIsHorizontallyFlipped` is true we
     * concatenate the current matrix of our parameter `Canvas canvas` with our matrix
     * `Matrix mHorizontalFlipMatrix` in order to scale the canvas horizontally about its
     * midpoint in the case that the card is in a horizontally flipped state (`mHorizontalFlipMatrix`
     * scales X by -1). In any case we then call our super's implementation of `onDraw` to draw us.
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
     * Updates the layout parameters of this view so as to reset the rotationX and rotationY parameters,
     * and remain independent of its previous position, while also maintaining its current position in
     * the layout. We initialize `RelativeLayout.LayoutParams params` by retrieving the layout
     * parameters for this view, then we set its `leftMargin` field to its current "real" value
     * which we calculate by adding to it the absolute value of its rotation around the vertical axis
     * through the pivot point modulo 360 divided by 180, times 2 times the X coordinate of its pivot
     * point minus the width of our view (this comes out to 0 when the card has just been flipped to
     * the left stack, and (on a pixel) to 540 (half its width) when it has been flipped ot the right
     * stack). Set then set our X rotation to 0 and our Y rotation to 0 and set our view's layout params
     * to `params`.
     */
    fun updateLayoutParams() {
        val params = layoutParams as RelativeLayout.LayoutParams
        params.leftMargin = (params.leftMargin + Math.abs(rotationY) % 360 / 180 *
            (2 * pivotX - width)).toInt()
        rotationX = 0f
        rotationY = 0f
        layoutParams = params
    }

    /**
     * Toggles the visible bitmap of this view between its front and back drawables. If our field
     * `mIsFrontShowing` is true we set `mCurrentBitmapDrawable` to `mFrontBitmapDrawable`,
     * if false we set it to `mBackBitmapDrawable`. Then we set the content of our `ImageView`
     * to `mCurrentBitmapDrawable`.
     */
    fun updateDrawableBitmap() {
        mCurrentBitmapDrawable = if (mIsFrontShowing) mFrontBitmapDrawable else mBackBitmapDrawable
        setImageDrawable(mCurrentBitmapDrawable)
    }

    /**
     * Sets the appropriate translation of this card depending on how many cards are in the pile
     * underneath it. We call the `setTranslationX` method to set the horizontal location of
     * our view relative to its [left][.getLeft] position to CardFlip.CARD_PILE_OFFSET times
     * our parameter `numInPile` and call the `setTranslationY` to set the Y offset to
     * the same value.
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
     * visible when rotated out. First we call our `rotateCardAroundCorner` to set our pivot
     * point to rotate around our parameter `Corner corner`. Then we initialize `int rotation`
     * to ROTATION_PER_CARD times our parameter `cardFromTop`. If our parameter `isClockwise`
     * is false we negate `rotation`, if our parameter `isRotatingOut` is false we set
     * `rotation` to 0. Finally we return an `ObjectAnimator` created to animate the ROTATION
     * property of 'this' to `rotation`.
     *
     * @param cardFromTop how far are we from the top, goes from 0 to one less than the size of the stack.
     * @param corner which corner to rotate about, always BOTTOM_LEFT in our usage.
     * @param isRotatingOut if true fans the card stack out, if false rotates it back to a stack.
     * @param isClockwise if true we are rotating clock wise, if false counter clockwise.
     * @return An `ObjectAnimator` that animates the ROTATION property of 'this'
     */
    fun getRotationAnimator(cardFromTop: Int, corner: Corner?,
                            isRotatingOut: Boolean, isClockwise: Boolean): ObjectAnimator {
        rotateCardAroundCorner(corner)
        var rotation = cardFromTop * ROTATION_PER_CARD
        if (!isClockwise) {
            rotation = -rotation
        }
        if (!isRotatingOut) {
            rotation = 0
        }
        return ObjectAnimator.ofFloat(this, ROTATION, rotation.toFloat())
    }

    /**
     * Returns a full rotation animator which rotates this card by 360 degrees about one of its corners
     * either in the clockwise or counter-clockwise direction. Depending on how many cards lie below
     * this one in the stack, a different start delay is applied to the animation so the cards don't
     * all animate at once. First we initialize `int currentRotation` with the current rotation
     * of our view, then call our `rotateCardAroundCorner` method to set our pivot point to be
     * around our parameter `Corner corner`. We then initialize `int rotation` to 360 minus
     * `currentRotation`, and if our parameter `isClockwise` is false we negate it. We
     * initialize `ObjectAnimator animator` with an instance which animates the ROTATION property
     * of 'this' to `rotation`, set its start delay to ROTATION_DELAY_PER_CARD times our parameter
     * `cardFromTop` and set its duration to ROTATION_DURATION. Then we add an `AnimatorListenerAdapter`
     * whose `onAnimationEnd` sets the rotation of our view to `currentRotation`.
     *
     * Finally we return `animator` to the caller.
     *
     * @param cardFromTop how far are we from the top, goes from 0 to one less than the size of the stack.
     * @param corner which corner to rotate about, always BOTTOM_LEFT in our usage.
     * @param isClockwise if true we are rotating clock wise, if false counter clockwise.
     * @return An `ObjectAnimator` that animates the ROTATION property of 'this'
     */
    fun getFullRotationAnimator(cardFromTop: Int, corner: Corner?,
                                isClockwise: Boolean): ObjectAnimator {
        val currentRotation = rotation.toInt()
        rotateCardAroundCorner(corner)
        var rotation = 360 - currentRotation
        rotation = if (isClockwise) rotation else -rotation
        val animator = ObjectAnimator.ofFloat(this, ROTATION, rotation.toFloat())
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
     * corners. We switch on the value of our parameter `Corner corner`:
     *
     *  *
     * TOP_LEFT: we set the X coordinate of our pivot point to 0, and the Y coordinate of our
     * pivot point to 0 then break.
     *
     *  *
     * TOP_RIGHT: we set the X coordinate of our pivot point to the width of our view, and
     * the Y coordinate of our pivot point to 0 then break.
     *
     *  *
     * BOTTOM_LEFT: we set the X coordinate of our pivot point to 0, and the Y coordinate of our
     * pivot point to the height of our view then break.
     *
     *  *
     * BOTTOM_RIGHT: we set the X coordinate of our pivot point to the width of our view, and
     * the Y coordinate of our pivot point to the height of our view then break.
     *
     *
     *
     * @param corner which corner to rotate about, always BOTTOM_LEFT in our usage.
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
     * Setter for our field `CardFlipListener mCardFlipListener`.
     *
     * @param cardFlipListener `CardFlipListener` whose `onCardFlipEnd` and
     * `onCardFlipStart` overrides we are supposed to call.
     */
    fun setCardFlipListener(cardFlipListener: CardFlipListener?) {
        mCardFlipListener = cardFlipListener
    }
}