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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.cardflip

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.cardflip.CardView.Corner

/**
 * This application creates 2 stacks of playing cards. Using fling events, these cards can be
 * flipped from one stack to another where each flip comes with an associated animation. The cards
 * can be flipped horizontally from left to right or right to left depending on which stack the
 * animating card currently belongs to.
 *
 * This application also demonstrates an animation where a stack of cards can either be
 * be rotated out or back in about their bottom left corner in a counter-clockwise direction.
 *
 *  * Rotate out: Down fling on stack of cards
 *
 *  * Rotate in: Up fling on stack of cards
 *
 *  * Full rotation: Tap on stack of cards
 *
 * Note that in this demo touch events are disabled in the middle of any animation so only one card
 * can be flipped at a time. When the cards are in a rotated-out state, no new cards can be rotated
 * to or from that stack. These changes were made to simplify the code for this demo.
 */
class CardFlip : ComponentActivity(), CardFlipListener {
    /**
     * Half of the width of the [RelativeLayout] field [mLayout] (ID `R.id.main_relative_layout`)
     * used for the layout params for the [CardView] when a card is added to the stack by our method
     * [addNewCard], and in our [getStack] to determine which stack has been flung.
     */
    var mCardWidth: Int = 0

    /**
     * Height of the [RelativeLayout] field [mLayout] (ID `R.id.main_relative_layout`) used for the
     * layout params of the [CardView] when a card is added to the stack by our method [addNewCard].
     */
    var mCardHeight: Int = 0

    /**
     * Set from the resource with id `R.integer.vertical_card_margin`, used to set the vertical
     * padding of the [CardView] when a card is added to a stack by our method [addNewCard]
     */
    var mVerticalPadding: Int = 0

    /**
     * Set from the resource with id `R.integer.horizontal_card_margin`, used to set the horizontal
     * padding of the [CardView] when a card is added to a stack by our method [addNewCard]
     */
    var mHorizontalPadding: Int = 0

    /**
     * Set to `false` to disable the interpretation of touch events while an animation is occurring.
     */
    var mTouchEventsEnabled: Boolean = true

    /**
     * Flags indicating whether a particular stack is enabled, [RIGHT_STACK] and [LEFT_STACK] both
     * have an entry which is set to `false` when they are in a rotated out state. If either stack
     * is disabled then flings from one stack to the other (left or right fling) are ignored.
     */
    lateinit var mIsStackEnabled: BooleanArray

    /**
     * [RelativeLayout] in our layout with id `R.id.main_relative_layout` (the entire layout file).
     * Contains both stacks of cards.
     */
    var mLayout: RelativeLayout? = null

    /**
     * [List] containing our two stacks of cards.
     */
    var mStackCards: MutableList<ArrayList<CardView>>? = null

    /**
     * [GestureDetector] constructed to use our [SimpleOnGestureListener] field [mGestureListener]
     * to interpret touch events relevant to our app ([mGestureListener] overrides `onSingleTapUp`
     * and `onFling`)
     */
    var gDetector: GestureDetector? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.main`.
     *
     * We initialize our [RelativeLayout] property [mLayout] to the view with ID
     * `R.id.main_relative_layout` then call [ViewCompat.setOnApplyWindowInsetsListener] to take
     * over the policy for applying window insets to [mLayout], with the `listener` argument a
     * lambda that accepts the [View] passed the lambda in variable `v` and the [WindowInsetsCompat]
     * passed the lambda in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates the layout parameters
     * of `v` to be a [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`,
     * the right margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the
     * bottom margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [List] of [ArrayList] of [CardView] field [mStackCards] with a new
     * instance, and add two new instances of [ArrayList] of [CardView] to it. We allocate 2
     * [Boolean]'s for our [BooleanArray] field [mIsStackEnabled] and set both entries to `true`.
     * We initialize our [Int] field [mVerticalPadding] by fetching the integer resource with ID
     * `R.integer.vertical_card_margin`, and our [Int] field [mHorizontalPadding] by fetching the
     * integer resource with ID `R.integer.horizontal_card_margin` (these are both 30).
     * We initialize our [GestureDetector] field [gDetector] with a new instance which uses as
     * its [OnGestureListener] our [SimpleOnGestureListener] field [mGestureListener] (it overrides
     * the methods `onSingleTapUp` and `onFling` in order to do the card deck animations in
     * response to user gestures). We initialize our [ViewTreeObserver] variable `val observer`
     * with a handle to the [ViewTreeObserver] for the hierarchy of [RelativeLayout] property
     * [mLayout]. We then add to `observer` an anonymous [OnGlobalLayoutListener] whose
     * [OnGlobalLayoutListener.onGlobalLayout] override creates and adds the [CardView] objects
     * to [mLayout] (as well as adding them to the [RIGHT_STACK] of our [List] of [ArrayList] of
     * [CardView] field [mStackCards].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        mLayout = findViewById(R.id.main_relative_layout)
        ViewCompat.setOnApplyWindowInsetsListener(mLayout!!) { v: View, windowInsets: WindowInsetsCompat ->
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
        mStackCards = ArrayList()
        mStackCards!!.add(ArrayList())
        mStackCards!!.add(ArrayList())
        mIsStackEnabled = BooleanArray(2)
        mIsStackEnabled[0] = true
        mIsStackEnabled[1] = true
        mVerticalPadding = resources.getInteger(R.integer.vertical_card_margin)
        mHorizontalPadding = resources.getInteger(R.integer.horizontal_card_margin)
        gDetector = GestureDetector(this, mGestureListener)
        val observer: ViewTreeObserver = mLayout!!.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            /**
             * Callback method to be invoked when the global layout state or the visibility of views
             * within the view tree changes. First we remove `this` as an [OnGlobalLayoutListener]
             * of our [RelativeLayout] field [mLayout]. Then we initialize our [Int] field
             * [mCardHeight] with the height of [mLayout] and [Int] field [mCardWidth] with half
             * of its width. Finally we [repeat] for [STARTING_NUMBER_CARDS] `times` calling our
             * method [addNewCard] to add a new card to the [RIGHT_STACK] of our [List] of
             * [ArrayList] of [CardView] field [mStackCards].
             */
            override fun onGlobalLayout() {
                mLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mCardHeight = mLayout!!.height
                mCardWidth = mLayout!!.width / 2
                repeat(times = STARTING_NUMBER_CARDS) {
                    addNewCard(RIGHT_STACK)
                }
            }
        })
    }

    /**
     * Adds a new card to the specified stack. Also performs all the necessary layout setup to
     * place the card in the correct position. First we initialize our [CardView] variable
     * `val view` with a new instance, call its [CardView.updateTranslation] method to set the
     * appropriate translation of the card depending on how many cards are in the pile underneath
     * it, set its [CardFlipListener] to `this`, and set its padding to [mHorizontalPadding] for
     * both left and right and [mVerticalPadding] for both top and bottom.
     *
     * We initialize [RelativeLayout.LayoutParams] variable `val params` with a [mCardWidth] by
     * [mCardHeight] instance, set its `topMargin` field to 0, and set its `leftMargin` field to
     * [mCardWidth] if our parameter [stack] is [RIGHT_STACK], or to 0 if it is not. We then add
     * `view` to the [Int] parameter [stack] stack of our [List] of [ArrayList] of [CardView] field
     * [mStackCards], and add it to our [RelativeLayout] field [mLayout] with `params` as the layout
     * parameters to set on it.
     *
     * @param stack the stack to add the card to, either [RIGHT_STACK] or [LEFT_STACK]
     */
    fun addNewCard(stack: Int) {
        val view = CardView(this)
        view.updateTranslation(mStackCards!![stack].size)
        view.setCardFlipListener(this)
        view.setPadding(mHorizontalPadding, mVerticalPadding, mHorizontalPadding, mVerticalPadding)
        val params = RelativeLayout.LayoutParams(mCardWidth, mCardHeight)
        params.topMargin = 0
        params.leftMargin = if (stack == RIGHT_STACK) mCardWidth else 0
        mStackCards!![stack].add(view)
        mLayout!!.addView(view, params)
    }

    /**
     * Gesture Detector listens for fling events (our `onFling` override) in order to potentially
     * initiate a card flip event when a fling event occurs. Also listens for tap events (our
     * `onSingleTapUp` override) in order to potentially initiate a full rotation animation.
     */
    private val mGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        /**
         * Notified when a tap occurs with the up [MotionEvent] that triggered it. We initialize our
         * [Int] variable `val stack` with the stack number that our [getStack] method determines
         * experienced the [MotionEvent] parameter [motionEvent], then call our
         * [rotateCardsFullRotation] method to rotate that stack around the [Corner.BOTTOM_LEFT]
         * corner. We then return `true` to consume the event.
         *
         * @param motionEvent The up motion event that completed the first tap
         * @return `true` if the event is consumed, else `false`
         */
        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            val stack = getStack(motionEvent)
            rotateCardsFullRotation(stack, Corner.BOTTOM_LEFT)
            return true
        }

        /**
         * Notified of a fling event when it occurs with the initial on down [MotionEvent] that
         * started the fling and the move [MotionEvent] that triggered the current fling. The
         * calculated velocity is supplied along the x and y axis in pixels per second.
         *
         * We initialize our [Int] variable `val stack` with the stack number that our [getStack]
         * method determines experienced the event [e1], then initialize [ArrayList] of [CardView]
         * variable `val cardStack` with that `stack` from our [List] of [ArrayList] of [CardView]
         * field [mStackCards]. We initialize our [Int] variable `val size` with the size of
         * `cardStack` and if this is greater than 0 we call our [rotateCardView] method, which uses
         * `stack`, along with the velocity values [velocityX] and [velocityX] of the fling event to
         * determine in what direction the [CardView] at the top of the stack (`cardStack.get(size-1)`)
         * must be flipped. By the same logic, the new stack that the card belongs to after the
         * animation is also determined and updated. Whether we flipped a card or not we return `true`
         * to consume the event.
         *
         * @param e1        The first down motion event that started the fling.
         * @param e2        The move motion event that triggered the current onFling.
         * @param velocityX The velocity of this fling measured in pixels per second along the x axis.
         * @param velocityY The velocity of this fling measured in pixels per second along the y axis.
         * @return `true` if the event is consumed, else `false`
         */
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val stack: Int = getStack(e1!!)
            val cardStack: ArrayList<CardView> = mStackCards!![stack]
            val size: Int = cardStack.size
            if (size > 0) {
                rotateCardView(cardStack[size - 1], stack, velocityX, velocityY)
            }
            return true
        }
    }

    /**
     * Returns the appropriate stack corresponding to the [MotionEvent]. We initialize [Boolean]
     * variable `val isLeft` to `true` iff the X coordinate for the first pointer index of our
     * [MotionEvent] parameter [ev] is less than or equal to our field [mCardWidth] (the width of
     * a card) and to `false` if it is not. Then we return [LEFT_STACK] if `isLeft` is `true`, or
     * [RIGHT_STACK] if it is `false`.
     *
     * @param ev `MotionEvent` we are interested in
     * @return LEFT_STACK if the event occurred over the left stack, otherwise RIGHT_STACK
     */
    fun getStack(ev: MotionEvent): Int {
        val isLeft: Boolean = ev.x <= mCardWidth
        return if (isLeft) LEFT_STACK else RIGHT_STACK
    }

    /**
     * Uses the stack parameter, along with the velocity values of the fling event to determine in
     * what direction the card must be flipped. By the same logic, the new stack that the card
     * belongs to after the animation is also determined and updated. We initialize our [Boolean]
     * variable `val xGreaterThanY` to `true` if the absolute value of our parameter `velocityX`
     * is greater than the absolute value of our parameter `velocityY` and to `false` if it is not.
     * We initialize our [Boolean] variable `val bothStacksEnabled` to `true` iff both the
     * [RIGHT_STACK] and [LEFT_STACK] entries in the [BooleanArray] field [mIsStackEnabled]
     * are `true`. We initialize our [ArrayList] of [CardView] variable `val leftStack` with a
     * reference to the [LEFT_STACK] stack  of our [List] of [ArrayList] of [CardView] field
     * [mStackCards], and our [ArrayList] of [CardView] variable `val rightStack` with a reference
     * to the [RIGHT_STACK] stack  of our [List] of [ArrayList] of [CardView] field [mStackCards].
     * We then switch on the value of our parameter `stack`:
     *
     *  * [RIGHT_STACK]: If [velocityX] is less than 0 (right to left fling) and `xGreaterThanY`
     *  is also true (horizontal fling) we first check that `bothStacksEnabled` is `true` (breaking
     *  without doing anything if one or the other stack is disabled). Having decided we do need to
     *  flip the top card of [RIGHT_STACK] to [LEFT_STACK], we call the `bringChildToFront` method
     *  of [RelativeLayout] field [mLayout] to change the z order of [cardView] so it's on top of
     *  all other children, then call its [RelativeLayout.requestLayout] method to schedule a layout
     *  pass of its view tree. We then remove the top [CardView] from `rightStack` and add `cardView`
     *  to `leftStack`. Finally we call the [CardView.flipRightToLeft] method of [cardView] to
     *  animate the flip from the right to left stack using [velocityX] to determine how fast the
     *  animation should run, and then we break. On the other hand we check whether `xGreaterThanY`
     *  is `false` (a vertical fling) and if so we initialize [Boolean] variable `val rotateCardsOut`
     *  to `true` if [velocityY] is greater than 0 (a downward fling) and to `false` if it is not
     *  (an upward fling). We then call our method [rotateCards] to rotate the [RIGHT_STACK] stack
     *  around the [Corner.BOTTOM_LEFT] corner, rotating the stack out if `rotateCardsOut` is `true`,
     *  or back in if it is `false`.
     *
     *  * [LEFT_STACK]: If [velocityX] is greater than 0 (left to right fling) and `xGreaterThanY`
     *  is also `true` (horizontal fling) we first check that `bothStacksEnabled` is `true` (breaking
     *  without doing anything if one or the other stack is disabled). Having decided we do need to
     *  flip the top card of [LEFT_STACK] to [RIGHT_STACK], we call the
     *  [RelativeLayout.bringChildToFront] method of [RelativeLayout] field [mLayout] to change the
     *  z order of [cardView] so it's on top of all other children, then call its
     *  [RelativeLayout.requestLayout] method to schedule a layout pass of its view tree. We then
     *  remove the top [CardView] from `leftStack` and add [cardView] to `rightStack`. Finally we
     *  call the [CardView.flipLeftToRight] method of [cardView] to animate the flip from the left
     *  to right stack using [velocityX] to determine how fast the animation should run, and then we
     *  break. On the other hand we check whether `xGreaterThanY` is `false` (a vertical fling) and
     *  if so we initialize [Boolean] variable `val rotateCardsOut` to `true` if [velocityY] is
     *  greater than 0 (a downward fling) and to `false` if it is not (an upward fling). We then
     *  call our method [rotateCards] to rotate the [LEFT_STACK] stack around the [Corner.BOTTOM_LEFT]
     *  corner, rotating the stack out if `rotateCardsOut` is `true`, or back in if it is `false`.
     *  We then break.
     *
     *  * default: we just break.
     *
     * @param cardView  the [CardView] that needs to be flipped to the other stack.
     * @param stack     the stack that the [CardView] is on
     * @param velocityX The velocity of the fling measured in pixels per second along the x axis.
     * @param velocityY The velocity of the fling measured in pixels per second along the y axis.
     */
    fun rotateCardView(cardView: CardView, stack: Int, velocityX: Float, velocityY: Float) {
        val xGreaterThanY: Boolean = Math.abs(velocityX) > Math.abs(velocityY)
        val bothStacksEnabled: Boolean = mIsStackEnabled[RIGHT_STACK] && mIsStackEnabled[LEFT_STACK]
        val leftStack: ArrayList<CardView> = mStackCards!![LEFT_STACK]
        val rightStack: ArrayList<CardView> = mStackCards!![RIGHT_STACK]
        when (stack) {
            RIGHT_STACK -> if (velocityX < 0 && xGreaterThanY) {
                if (bothStacksEnabled) {
                    mLayout!!.bringChildToFront(cardView)
                    mLayout!!.requestLayout()
                    rightStack.removeAt(rightStack.size - 1)
                    leftStack.add(cardView)
                    cardView.flipRightToLeft(leftStack.size - 1, velocityX.toInt())
                }
            } else if (!xGreaterThanY) {
                val rotateCardsOut = velocityY > 0
                rotateCards(RIGHT_STACK, Corner.BOTTOM_LEFT, rotateCardsOut)
            }

            LEFT_STACK -> if (velocityX > 0 && xGreaterThanY) {
                if (bothStacksEnabled) {
                    mLayout!!.bringChildToFront(cardView)
                    mLayout!!.requestLayout()
                    leftStack.removeAt(leftStack.size - 1)
                    rightStack.add(cardView)
                    cardView.flipLeftToRight(rightStack.size - 1, velocityX.toInt())
                }
            } else if (!xGreaterThanY) {
                val rotateCardsOut = velocityY > 0
                rotateCards(LEFT_STACK, Corner.BOTTOM_LEFT, rotateCardsOut)
            }

            else -> {}
        }
    }

    /**
     * Part of the [CardFlipListener] interface, it is called at the end of the animation of
     * the flip of a card invoked by the [CardView.flipHorizontally] method of the [CardView].
     * We just re-enable the interpretation of touch events by setting [mTouchEventsEnabled]
     * to `true`.
     */
    override fun onCardFlipEnd() {
        mTouchEventsEnabled = true
    }

    /**
     * Part of the [CardFlipListener] interface, it is called at the beginning of the animation
     * of the flip of a card invoked by the [CardView.flipHorizontally] method of the [CardView].
     * We just disable the interpretation of touch events by setting [mTouchEventsEnabled]
     * to `false`.
     */
    override fun onCardFlipStart() {
        mTouchEventsEnabled = false
    }

    /**
     * Called when a touch screen event was not handled by any of the views under it. This is most
     * useful to process touch events that happen outside of your window bounds, where there is no
     * view to receive it. If our flag [mTouchEventsEnabled] is `true` (no card flip animations
     * are in progress) we return the value the [GestureDetector.onTouchEvent] override of our
     * [GestureDetector] field [gDetector] returns when we call it to interpret the [MotionEvent],
     * otherwise we return the value returned by our super's implementation of `onTouchEvent`.
     *
     * @param me The touch screen event being processed.
     * @return Return `true` if you have consumed the event, `false` if you haven't.
     */
    override fun onTouchEvent(me: MotionEvent): Boolean {
        return if (mTouchEventsEnabled) {
            gDetector!!.onTouchEvent(me)
        } else {
            super.onTouchEvent(me)
        }
    }

    /**
     * TODO: Continue here.
     * Retrieves an animator object for each card in the specified stack that either rotates it in
     * or out depending on its current state. All of these animations are then played together. First
     * we initialize our [List] of [Animator] variable `val animations` with a new instance, then we
     * initialize our [ArrayList] of [CardView] variable `val cards` with a reference to stack [stack]
     * in our [List] of [ArrayList] of [CardView] field [mStackCards]. Then we loop over [Int] variable
     * `i` for all of the [CardView] in `cards`, initializing [CardView] variable `val cardView` with
     * the [CardView] at position `i` in `cards` then adding to `animations` the [ObjectAnimator]
     * returned by the [CardView.getRotationAnimator] method of `cardView` when given `i` as
     * the height of the card from the top, for the corner [corner], passing it our parameter
     * [isRotatingOut] to inform it about whether we want to fan out cards (`true`) or collapse
     * the fanned out stack (`false`), and `false` for its `isClockwise` parameter so that it knows
     * that the fan out is counter clockwise. Then we call the [RelativeLayout.bringChildToFront]
     * method of our [RelativeLayout] field [mLayout] to change the z order of `cardView` so it's
     * on top of all other children, and loop around for the next [CardView] in `cards`.
     *
     * When done creating animations for cards in the stack we wish to rotate, and bringing each of
     * them to the front so that the cards being rotated in the current stack will overlay the cards
     * in the other stack we call the [RelativeLayout.requestLayout] method of [mLayout] in order to
     * apply the changes made to the Z ordering.
     *
     * Next we initialize [AnimatorSet] variable `val set` with a new instance and set it up to play
     * all of the animations in `animations` at the same time. Then we add an anonymous
     * [AnimatorListenerAdapter] to `set` whose [AnimatorListenerAdapter.onAnimationEnd] override
     * sets the [mIsStackEnabled] flag of stack [stack] to the inverse of our [Boolean] parameter
     * [isRotatingOut], disabling the flipping of cards from one stack to the other while this stack
     * is rotated out or enabling it if it just rotated back in (the other stack is free to rotate
     * in or out whatever the setting or this flag might be). Finally we start `set` running.
     *
     * @param stack stack number, either [RIGHT_STACK] or [LEFT_STACK]
     * @param corner corner, always [Corner.BOTTOM_LEFT] in our usage.
     * @param isRotatingOut if `true` fans the card stack out, if `false` rotates it back to a stack.
     */
    fun rotateCards(stack: Int, corner: Corner?, isRotatingOut: Boolean) {
        val animations: MutableList<Animator> = ArrayList()
        val cards: ArrayList<CardView> = mStackCards!![stack]
        for (i in cards.indices) {
            val cardView = cards[i]
            animations.add(cardView.getRotationAnimator(
                cardFromTop = i,
                corner = corner,
                isRotatingOut = isRotatingOut,
                isClockwise = false
            ))
            mLayout!!.bringChildToFront(cardView)
        }
        /* All the cards are being brought to the front in order to guarantee that
         * the cards being rotated in the current stack will overlay the cards in the
         * other stack. After the z-ordering of all the cards is updated, a layout must
         * be requested in order to apply the changes made.*/
        mLayout!!.requestLayout()
        val set = AnimatorSet()
        set.playTogether(animations)
        set.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. We just set `mIsStackEnabled[stack]` to the
             * inverse of `isRotatingOut` disabling the flipping of cards from one stack to the
             * other while this stack is rotated out or enabling it if it just rotated back in (the
             * other stack is free to rotate in or out whatever the setting or this flag might be).
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                mIsStackEnabled[stack] = !isRotatingOut
            }
        })
        set.start()
    }

    /**
     * Retrieves an animator object for each card in the specified stack to complete a full revolution
     * around one of its corners, and plays all of them together. First we initialize our [List] of
     * [Animator] variable `val animations` with a new instance, then we initialize [ArrayList] of
     * [CardView] variable `val cards` with a reference to stack [stack] in our [List] of [ArrayList]
     * of [CardView] field [mStackCards]. Then we loop over [Int] `i` for all of the [CardView] in
     * `cards`, initializing [CardView] variable `val cardView` with the [CardView] at position `i`
     * in `cards` then adding to `animations` the [ObjectAnimator] returned by the
     * [CardView.getFullRotationAnimator] method of `cardView` when given `i` as the height of the
     * card from the top, [corner] for the corner, and passing `false` for its `isClockwise` parameter
     * so that it knows that the rotation is to be counter clockwise. Then we call the
     * [RelativeLayout.bringChildToFront] method of our [RelativeLayout] field [mLayout] to change
     * the z order of `cardView` so it's on top of all other children, and loop around to the
     * next [CardView] in `cards`.
     *
     * When done creating animations for cards in the stack we wish to rotate, and bringing each of
     * them to the front so that the cards being rotated in the current stack will overlay the cards
     * in the other stack, we call the [RelativeLayout.requestLayout] method of [mLayout] in order
     * to apply the changes made to the Z ordering. We then set our flag [mTouchEventsEnabled] to
     * `false` to disable the interpretation of any touch events.
     *
     * Next we initialize [AnimatorSet] variable `val set` with a new instance and set it up to play
     * all of the animations in `animations` at the same time. Then we add an anonymous
     * [AnimatorListenerAdapter] to `set` whose [AnimatorListenerAdapter.onAnimationEnd] override
     * sets [mTouchEventsEnabled] to `true` to re-enable the interpretation of touch events. Finally
     * we start `set` running.
     *
     * @param stack stack number, either [RIGHT_STACK] or [LEFT_STACK]
     * @param corner corner, always [Corner.BOTTOM_LEFT] in our usage.
     */
    fun rotateCardsFullRotation(stack: Int, corner: Corner?) {
        val animations: MutableList<Animator> = ArrayList()
        val cards: ArrayList<CardView> = mStackCards!![stack]
        for (i in cards.indices) {
            val cardView: CardView = cards[i]
            animations.add(cardView.getFullRotationAnimator(
                cardFromTop = i,
                corner = corner,
                isClockwise = false
            ))
            /* Same reasoning for bringing cards to front as in rotateCards().*/
            mLayout!!.bringChildToFront(cardView)
        }
        mLayout!!.requestLayout()
        mTouchEventsEnabled = false
        val set = AnimatorSet()
        set.playTogether(animations)
        set.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. We just set `mTouchEventsEnabled` to true to
             * re-enable the interpretation of touch events.
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                mTouchEventsEnabled = true
            }
        })
        set.start()
    }

    companion object {
        /**
         * Pixel offset of a single card in a stack, used to offset the card depending on how many
         * cards are in the pile underneath it.
         */
        const val CARD_PILE_OFFSET: Int = 3

        /**
         * Number of cards that are created for the [RIGHT_STACK] when the activity starts.
         */
        const val STARTING_NUMBER_CARDS: Int = 15

        /**
         * Constant used to choose the stack on the right.
         */
        const val RIGHT_STACK: Int = 0

        /**
         * Constant used to choose the stack on the left.
         */
        const val LEFT_STACK: Int = 1
    }
}
