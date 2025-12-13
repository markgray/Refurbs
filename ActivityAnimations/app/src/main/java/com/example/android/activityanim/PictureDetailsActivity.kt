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

package com.example.android.activityanim

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.OnApplyWindowInsetsListener

/**
 * This sub-activity shows a zoomed-in view of a specific photo, along with the
 * picture's text description. Most of the logic is for the animations that will
 * be run when the activity is being launched and exited. When launching,
 * the large version of the picture will resize from the thumbnail version in the
 * main activity, colorizing it from the thumbnail's grayscale version at the
 * same time. Meanwhile, the black background of the activity will fade in and
 * the description will eventually slide into place. The exit animation runs all
 * of this in reverse.
 */
class PictureDetailsActivity : AppCompatActivity() {
    /**
     * [BitmapDrawable] we display in our [ImageView]. It is created from the drawable resource ID
     * that is passed as an extra in the [Intent] that launched us that is stored in the [Intent]
     * under the key ".resourceId"
     */
    private var mBitmapDrawable: BitmapDrawable? = null

    /**
     * [ColorMatrix] whose "saturation" property is animated. It is used to create the
     * [ColorMatrixColorFilter] which is then set on [BitmapDrawable] property [mBitmapDrawable].
     */
    private val colorizerMatrix = ColorMatrix()

    /**
     * [ColorDrawable] which is used as our background, its alpha is animated as we enter and exit.
     */
    var mBackground: ColorDrawable? = null

    /**
     * Translation needed to translate the X coordinate of the thumbnail to the full sized image
     */
    var mLeftDelta: Int = 0

    /**
     * Translation needed to translate the Y coordinate of the thumbnail to the full sized image
     */
    var mTopDelta: Int = 0

    /**
     * Scale factor to make the width of the large version the same size as the thumbnail
     */
    var mWidthScale: Float = 0f

    /**
     * Scale factor to make the height of the large version the same size as the thumbnail
     */
    var mHeightScale: Float = 0f

    /**
     * The [ImageView] with id `R.id.imageView` in which we display our picture.
     */
    private var mImageView: ImageView? = null

    /**
     * The [TextView] with id `R.id.description` which we use to display the description of the
     * picture (the description string is passed to use as an extra in the [Intent] that
     * launched us, stored under the key ".description"
     */
    private var mTextView: TextView? = null

    /**
     * [FrameLayout] with id `R.id.topLevelLayout`, it is the container for our [ShadowLayout]
     */
    private var mTopLevelLayout: FrameLayout? = null

    /**
     * [ShadowLayout] in our layout with id `R.id.shadowLayout`, it is a custom [RelativeLayout]
     * which paints a drop shadow behind all children.
     */
    private var mShadowLayout: ShadowLayout? = null

    /**
     * This is the orientation of the screen when [ActivityAnimations] launched us, it is passed
     * to us as an extra in the [Intent] that launched us stored under the key ".orientation", if
     * the orientation of the screen is different when we are exiting we do not try to animate the
     * image back to the thumbnail position, we just animate the scale around the center, and fade
     * it out since it won't match up with whatever's actually in the center
     */
    private var mOriginalOrientation = 0

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to edge
     * display then we call our super's implementation of `onCreate` and set our content view to our
     * layout file `R.layout.picture_info`. We initialize our [ImageView] property [mImageView] by
     * finding the view with id `R.id.imageView`, and our [FrameLayout] property [mTopLevelLayout]
     * by finding the view with id `R.id.topLevelLayout`.
     *
     * Next we use the method [ViewCompat.setOnApplyWindowInsetsListener] to set an
     * [OnApplyWindowInsetsListener] on [mTopLevelLayout] to take over the policy for applying
     * window insets to [mTopLevelLayout], with the `listener` argument a lambda that accepts the
     * [View] passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in
     * variable `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [ShadowLayout] property [mShadowLayout] by finding the view with id
     * `R.id.shadowLayout`, and our [TextView] property [mTextView] by finding the view with id
     * `R.id.description`.
     *
     * We initialize [Bundle] variable `bundle` by retrieving the map of all extras previously added
     * with `putExtra()` from the [Intent] that started this activity. We initialize [Bitmap] variable
     * `bitmap` by using the [BitmapUtils.getBitmap] method to either return a cached [Bitmap] or
     * the [Bitmap] it decodes from the jpg whose resource id is stored under the key ".resourceId"
     * in `bundle`. We initialize [String] variable `description` by retrieving the string stored
     * under the key ".description" in `bundle`, [Int] variable `thumbnailTop` by retrieving the [Int]
     * stored under the key ".top", [Int] variable `thumbnailLeft` by retrieving the [Int] stored
     * under the key ".left", [Int] variable `thumbnailWidth` by retrieving the [Int] stored under
     * the key ".width", [Int] variable `thumbnailHeight` by retrieving the [Int] stored under the
     * key ".height", and our [Int] property [mOriginalOrientation] by retrieving the [Int] stored
     * under the key ".orientation".
     *
     * We initialize our [BitmapDrawable] property [mBitmapDrawable] with an instance constructed
     * from `bitmap`, set it to be the content of [ImageView] property [mImageView] and set
     * `description` as the text of [TextView] property [mTextView]. We initialize our [ColorDrawable]
     * property [mBackground] with an instance created from [Color.BLACK] and set it to be the
     * background of [FrameLayout] property [mTopLevelLayout].
     *
     * Now if our [Bundle] parameter [savedInstanceState] is not `null` we are being recreated
     * automatically by the window manager (e.g., device rotation) so we skip any animation and
     * return. If it is `null` we are being started by [ActivityAnimations] and we want to animate
     * this transition, so we initialize [ViewTreeObserver] variable `observer` with a handle to the
     * [ViewTreeObserver] for the hierarchy of [ImageView] property [mImageView] and add an anonymous
     * [OnPreDrawListener] whose [OnPreDrawListener.onPreDraw] override first removes itself as an
     * [OnPreDrawListener], then allocates 2 ints for [IntArray] variable `screenLocation` and loads
     * them with the X and Y coordinates of [mImageView] on the screen. It sets [Int] property
     * [mLeftDelta] to [Int] variable `thumbnailLeft` minus `screenLocation[0]` (which is the distance
     * between the X location of the thumbnail and the X location of the full size version), and sets
     * [Int] property [mTopDelta] to [Int] variable `thumbnailTop` minus `screenLocation[1]` (which
     * is the distance between the Y location of the thumbnail and the Y location of the full size
     * version). It then sets [Float] property [mWidthScale] to `thumbnailWidth` divided by the width
     * of [ImageView] property [mImageView] (which is how much the width of the full size version
     * needs to be scaled down to be the same size as the thumbnail) and sets [Float] property
     * [mHeightScale] to `thumbnailHeight` divided by the height of [mImageView] (which is how much
     * the height of the full size version needs to be scaled down to be the same size as the
     * thumbnail). It then calls our method [runEnterAnimation] to construct and run the animations
     * that transition to our layout, and returns `true` to allow drawing to proceed.
     *
     * @param savedInstanceState we just use this to only run the animation if we're coming from the
     * parent activity (it is `null`), not if we're recreated automatically by the window manager
     * (e.g., device rotation, when it is not `null`)
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.picture_info)
        mImageView = findViewById(R.id.imageView)
        mTopLevelLayout = findViewById(R.id.topLevelLayout)
        ViewCompat.setOnApplyWindowInsetsListener(mTopLevelLayout!!) { v, windowInsets ->
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
        mShadowLayout = findViewById(R.id.shadowLayout)
        mTextView = findViewById(R.id.description)

        // Retrieve the data we need for the picture/description to display and
        // the thumbnail to animate it from
        val bundle: Bundle? = intent.extras
        val bitmap: Bitmap? = BitmapUtils.getBitmap(
            resources = resources,
            resourceId = bundle!!.getInt("$PACKAGE_NAME.resourceId")
        )
        val description: String? = bundle.getString("$PACKAGE_NAME.description")
        val thumbnailTop: Int = bundle.getInt("$PACKAGE_NAME.top")
        val thumbnailLeft: Int = bundle.getInt("$PACKAGE_NAME.left")
        val thumbnailWidth: Int = bundle.getInt("$PACKAGE_NAME.width")
        val thumbnailHeight: Int = bundle.getInt("$PACKAGE_NAME.height")
        mOriginalOrientation = bundle.getInt("$PACKAGE_NAME.orientation")

        mBitmapDrawable = bitmap!!.toDrawable(resources)
        mImageView!!.setImageDrawable(mBitmapDrawable)
        mTextView!!.text = description
        mBackground = Color.BLACK.toDrawable()
        mTopLevelLayout!!.background = mBackground

        // Only run the animation if we're coming from the parent activity, not if
        // we're recreated automatically by the window manager (e.g., device rotation)
        if (savedInstanceState == null) {
            val observer: ViewTreeObserver = mImageView!!.viewTreeObserver
            observer.addOnPreDrawListener(object : OnPreDrawListener {
                /**
                 * Callback method to be invoked when the view tree is about to be drawn. At this
                 * point, all views in the tree have been measured and given a frame. Clients can
                 * use this to adjust their scroll bounds or even to request a new layout before
                 * drawing occurs. First we remove ourselves as a [OnPreDrawListener], then we
                 * allocate 2 [Int]'s for [IntArray] variable `screenLocation` and load them with
                 * the X and Y coordinates of [ImageView] property [mImageView] on the screen. We set
                 * [Int] property [mLeftDelta] to `thumbnailLeft` minus `screenLocation[0]` (which
                 * is the distance between the X location of the thumbnail and the X location of the
                 * full size version), and set [Int] property [mTopDelta] to `thumbnailTop` minus
                 * `screenLocation[1]` (which is the distance between the Y location of the thumbnail
                 * and the Y location of the full size version). We then set [Float] property
                 * [mWidthScale] to `thumbnailWidth` divided by the width of [mImageView] (which is
                 * how much the width of the full size version needs to be scaled down to be the same
                 * size as the thumbnail) and set [Float] property [mHeightScale] to `thumbnailHeight`
                 * divided by the height of [mImageView] (which is how much the height of the full
                 * size version needs to be scaled down to be the same size as the thumbnail). We
                 * then call our method [runEnterAnimation] to construct and run the animations that
                 * transition to our layout, and return true to allow drawing to proceed.
                 *
                 * @return Return true to proceed with the current drawing pass, or false to cancel.
                 */
                override fun onPreDraw(): Boolean {
                    mImageView!!.viewTreeObserver.removeOnPreDrawListener(this)

                    // Figure out where the thumbnail and full size versions are, relative
                    // to the screen and each other
                    val screenLocation = IntArray(2)
                    mImageView!!.getLocationOnScreen(screenLocation)
                    mLeftDelta = thumbnailLeft - screenLocation[0]
                    mTopDelta = thumbnailTop - screenLocation[1]

                    // Scale factors to make the large version the same size as the thumbnail
                    mWidthScale = thumbnailWidth.toFloat() / mImageView!!.width
                    mHeightScale = thumbnailHeight.toFloat() / mImageView!!.height
                    runEnterAnimation()
                    return true
                }
            })
        }
        addOurOnBackPressedCallback()
    }

    /**
     * The enter animation scales the picture in from its previous thumbnail size/location,
     * colorizing it in parallel. In parallel, the background of the activity is fading in. When
     * the picture is in place, the text description drops down. We initialize [Long] variable
     * `duration` to [ANIM_DURATION] (500) multiplied by the [ActivityAnimations.sAnimatorScale]
     * property of [ActivityAnimations] (`sAnimatorScale` is either 1 or 5 depending on whether the
     * "Slow" [MenuItem] in the options menu of [ActivityAnimations] is unchecked or checked
     * respectively). We set the X location of the point around which [ImageView] property
     * [mImageView] is scaled (or rotated) to 0 and the Y location of the point around which
     * [mImageView] is scaled (or rotated) to 0 as well. We then scale the X dimension of [mImageView]
     * down to [Float] property [mWidthScale] and the Y dimension of [mImageView] down to [Float]
     * property [mHeightScale] (this shrinks the full size version down to be the same size as the
     * thumbnail). We translate the horizontal location of [mImageView] by [Int] property [mLeftDelta]
     * and the vertical location of [mImageView] by [Int] property [mTopDelta] (this places the now
     * shrunken full sized version over the location of the thumbnail). We set the alpha of [TextView]
     * property [mTextView] to 0 (we'll fade the text in after the animation of [mImageView] ends).
     *
     * We use the [View.animate] method of [mImageView] to fetch a [ViewPropertyAnimator] for it,
     * set its duration to [Long] variable `duration`, have it animate the X and the Y size of
     * [mImageView] both to 1, have it animate the X and Y translation both to 0, set its
     * [TimeInterpolator] to [TimeInterpolator] property [sDecelerator] and specify an anonymous
     * [Runnable] to be run when the animation ends whose [Runnable.run] override animates the
     * translation and alpha property of [TextView] property [mTextView] to its final values.
     *
     * We now initialize [ObjectAnimator] variable `bgAnim` with an instance which will animate the
     * "alpha" property of [ColorDrawable] property [mBackground] from 0 to 255, set its duration to
     * `duration` and start it running. We initialize [ObjectAnimator] variable `colorizer` with an
     * instance which will animate the "saturation" property of this [PictureDetailsActivity] from
     * 0 to 1, set its duration to `duration` and start it running. We initialize [ObjectAnimator]
     * variable `shadowAnim` with an instance which will animate the "shadowDepth" property of
     * [ShadowLayout] property [mShadowLayout] from 0 to 1, set its duration to `duration` and start
     * it running.
     */
    @SuppressLint("ObjectAnimatorBinding")
    fun runEnterAnimation() {
        val duration: Long = (ANIM_DURATION * ActivityAnimations.sAnimatorScale).toLong()

        // Set starting values for properties we're going to animate. These
        // values scale and position the full size version down to the thumbnail
        // size/location, from which we'll animate it back up
        mImageView!!.pivotX = 0f
        mImageView!!.pivotY = 0f
        mImageView!!.scaleX = mWidthScale
        mImageView!!.scaleY = mHeightScale
        mImageView!!.translationX = mLeftDelta.toFloat()
        mImageView!!.translationY = mTopDelta.toFloat()

        // We'll fade the text in later
        mTextView!!.alpha = 0f

        // Animate scale and translation to go from thumbnail to full size
        mImageView!!.animate().setDuration(duration)
            .scaleX(1f).scaleY(1f)
            .translationX(0f).translationY(0f)
            .setInterpolator(sDecelerator)
            .withEndAction { // Animate the description in after the image animation
                // is done. Slide and fade the text in from underneath
                // the picture.
                mTextView!!.translationY = -mTextView!!.height.toFloat()
                mTextView!!.animate().setDuration(duration / 2)
                    .translationY(0f).alpha(1f).interpolator = sDecelerator
            }

        // Fade in the black background
        val bgAnim: ObjectAnimator = ObjectAnimator.ofInt(
            /* target = */ mBackground,
            /* propertyName = */ "alpha",
            /* ...values = */  0, 255
        )
        bgAnim.duration = duration
        bgAnim.start()

        // Animate a color filter to take the image from grayscale to full color.
        // This happens in parallel with the image scaling and moving into place.
        val colorizer: ObjectAnimator = ObjectAnimator.ofFloat(
            /* target = */ this@PictureDetailsActivity,
            /* propertyName = */ "saturation",
            /* ...values = */ 0f, 1f
        )
        colorizer.duration = duration
        colorizer.start()

        // Animate a drop-shadow of the image
        val shadowAnim: ObjectAnimator = ObjectAnimator.ofFloat(
            /* target = */ mShadowLayout,
            /* propertyName = */ "shadowDepth",
            /* ...values = */ 0f, 1f
        )
        shadowAnim.duration = duration
        shadowAnim.start()
    }

    /**
     * The exit animation is basically a reverse of the enter animation, except that if the
     * orientation has changed we simply scale the picture back into the center of the screen.
     * We initialize [Long] variable `duration` to [ANIM_DURATION] (500) multiplied by the
     * [ActivityAnimations.sAnimatorScale] property of [ActivityAnimations] (`sAnimatorScale` is
     * either 1 or 5 depending on whether the "Slow" [MenuItem] in the options menu of
     * [ActivityAnimations] is unchecked or checked respectively). Next we declare [Boolean]
     * variable `fadeOut`. If the [Configuration.orientation] property of the current [Configuration]
     * that is in effect for the [Resources] of our application's package is not equal to our [Int]
     * property [mOriginalOrientation] (we were rotated since we were launched by [ActivityAnimations])
     * we set the X, Y location of the pivot point of [mImageView] to the center of the view, set
     * [Int] properties [mLeftDelta] and [mTopDelta] both to 0 and set `fadeOut` to true. If the
     * orientation is the same as when we were launched we just set `fadeOut` to false.
     *
     * Now we we animate the Y translation of our [mTextView] to minus its height, and its alpha
     * property to 0 with a duration of half of `duration`, a [TimeInterpolator] of [TimeInterpolator]
     * property [sAccelerator] and an anonymous [Runnable] as its end action which animates the
     * scale of [mImageView] back to the size of the thumbnail, its X translation to [mLeftDelta]
     * its Y translation to [mTopDelta] and add our [Runnable] parameter [endAction] as its end
     * action. If `fadeOut` is true it also animates the alpha property of [mImageView] to 0. The
     * [Runnable.run] override initializes [ObjectAnimator] variable `bgAnim` with an instance which
     * will animate the "alpha" property of [ColorDrawable] property [mBackground] to 0, sets its
     * duration to `duration` and starts it running. It then initializes [ObjectAnimator] variable
     * `shadowAnim` with an instance which will animate the "shadowDepth" property of [ShadowLayout]
     * property [mShadowLayout] from 1 to 0, sets its duration to `duration` and starts it running.
     * Finally it initializes [ObjectAnimator] variable `colorizer` with an instance which will
     * animate the "saturation" property of this [PictureDetailsActivity] from 1 to 0 (from full
     * color back to black and white), sets its duration to `duration` and starts it running.
     *
     * @param endAction This action gets run after the animation completes (this is when we actually
     * switch activities)
     */
    @SuppressLint("ObjectAnimatorBinding")
    fun runExitAnimation(endAction: Runnable?) {
        val duration: Long = (ANIM_DURATION * ActivityAnimations.sAnimatorScale).toLong()

        // No need to set initial values for the reverse animation; the image is at the
        // starting size/location that we want to start from. Just animate to the
        // thumbnail size/location that we retrieved earlier 

        // Caveat: configuration change invalidates thumbnail positions; just animate
        // the scale around the center. Also, fade it out since it won't match up with
        // whatever's actually in the center
        val fadeOut: Boolean
        if (resources.configuration.orientation != mOriginalOrientation) {
            mImageView!!.pivotX = (mImageView!!.width / 2).toFloat()
            mImageView!!.pivotY = (mImageView!!.height / 2).toFloat()
            mLeftDelta = 0
            mTopDelta = 0
            fadeOut = true
        } else {
            fadeOut = false
        }

        // First, slide/fade text out of the way
        mTextView!!.animate()
            .translationY(-mTextView!!.height.toFloat())
            .alpha(0f).setDuration(duration / 2)
            .setInterpolator(sAccelerator)
            .withEndAction { // Animate image back to thumbnail size/location
                mImageView!!
                    .animate()
                    .setDuration(duration)
                    .scaleX(mWidthScale)
                    .scaleY(mHeightScale)
                    .translationX(mLeftDelta.toFloat())
                    .translationY(mTopDelta.toFloat())
                    .withEndAction(endAction)
                if (fadeOut) {
                    mImageView!!.animate().alpha(0f)
                }
                // Fade out background
                val bgAnim: ObjectAnimator = ObjectAnimator.ofInt(
                    /* target = */ mBackground,
                    /* propertyName = */ "alpha",
                    /* ...values = */ 0
                )
                bgAnim.duration = duration
                bgAnim.start()

                // Animate the shadow of the image
                val shadowAnim: ObjectAnimator = ObjectAnimator.ofFloat(
                    /* target = */ mShadowLayout,
                    /* propertyName = */ "shadowDepth",
                    /* ...values = */ 1f, 0f
                )
                shadowAnim.duration = duration
                shadowAnim.start()

                // Animate a color filter to take the image back to grayscale,
                // in parallel with the image scaling and moving into place.
                val colorizer: ObjectAnimator = ObjectAnimator.ofFloat(
                    /* target = */ this@PictureDetailsActivity,
                    /* propertyName = */ "saturation",
                    /* ...values = */ 1f, 0f
                )
                colorizer.duration = duration
                colorizer.start()
            }
    }

    /**
     * This method adds an [OnBackPressedCallback] to the [OnBackPressedDispatcher] that replaces the
     * old `onBackPressed` override. The [OnBackPressedCallback] will be called when the activity has
     * detected the user's press of the back key. We call our method [runExitAnimation] with an
     * anonymous [Runnable] to run when the animation ends, whose [Runnable.run] override just calls
     * the [finish] method to exit the activity.
     */
    private fun addOurOnBackPressedCallback() {
        val callback = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                runExitAnimation { // *Now* go ahead and exit the activity
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(owner = this, onBackPressedCallback = callback)
    }

    /**
     * This is called by the colorizing animator. It sets a saturation factor that is then passed
     * onto a filter on the picture's drawable. We call the [ColorMatrix.setSaturation]  method of
     * [ColorMatrix] property [colorizerMatrix] to set the matrix to affect the saturation of colors
     * by our [Float] parameter [value], initialize [ColorMatrixColorFilter] variable `colorizerFilter`
     * with an instance created from [colorizerMatrix], and set the color filter for [BitmapDrawable]
     * property [mBitmapDrawable] to `colorizerFilter`.
     *
     * @param value value to set the saturation to
     */
    @Suppress("unused") // actually used by the animation of the "saturation" property
    fun setSaturation(value: Float) {
        colorizerMatrix.setSaturation(value)
        val colorizerFilter = ColorMatrixColorFilter(colorizerMatrix)
        mBitmapDrawable!!.colorFilter = colorizerFilter
    }

    /**
     * Call this when your activity is done and should be closed. First we call our super's
     * implementation of `finish` then we call the [overridePendingTransition] method to skip the
     * standard window animations (using a different overload for SDK's older than SDK 34).
     */
    override fun finish() {
        super.finish()

        // override transitions to skip the standard window animations
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                /* overrideType = */ OVERRIDE_TRANSITION_CLOSE,
                /* enterAnim = */ 0,
                /* exitAnim = */ 0
            )
        } else {
            @Suppress("DEPRECATION") // Needed for SDK older than 34
            overridePendingTransition(/* enterAnim = */ 0, /* exitAnim = */ 0)
        }
    }

    companion object {
        /**
         * [DecelerateInterpolator] which is used for the animation from thumbnail to full size, as
         * well as the translation and alpha animation of the picture's text description.
         */
        private val sDecelerator: TimeInterpolator = DecelerateInterpolator()

        /**
         * [AccelerateInterpolator] used for the slide/fade text out of the way animation performed
         * as part of our exit animation.
         */
        private val sAccelerator: TimeInterpolator = AccelerateInterpolator()

        /**
         * Package prefix used when adding extras to the [Intent] that launches this activity
         */
        private const val PACKAGE_NAME = "com.example.android.activityanim"

        /**
         * Base animation duration which is multiplied by the [ActivityAnimations.sAnimatorScale]
         * property of [ActivityAnimations] to calculate the duration used for all of the animations
         * for both enter and exit animations.
         */
        private const val ANIM_DURATION = 500
    }
}