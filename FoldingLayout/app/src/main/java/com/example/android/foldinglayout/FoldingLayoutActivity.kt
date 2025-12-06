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
@file:Suppress("DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate")

package com.example.android.foldinglayout

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.foldinglayout.FoldingLayout.Orientation
import com.example.android.foldinglayout.FoldingLayout.Orientation.HORIZONTAL
import com.example.android.foldinglayout.FoldingLayout.Orientation.VERTICAL
import java.io.IOException

/**
 * This application creates  a paper like folding effect of some view. The number of folds,
 * orientation (vertical or horizontal) of the fold, and the anchor point about which the view
 * will fold can be set to achieve different folding effects.
 *
 * Using bitmap and canvas scaling techniques, the [FoldingLayout] can be scaled so as to depict
 * a paper-like folding effect. The addition of shadows on the separate folds adds a sense of
 * realism to the visual effect.
 *
 * This application shows folding of a [TextureView] containing a live camera feed, as well as the
 * folding of an [ImageView] with a static image. The [TextureView] experiences jagged edges as a
 * result of scaling operations on rectangles. The [ImageView] however contains a 1 pixel transparent
 * border around its contents which can be used to avoid this unwanted artifact.
 */
class FoldingLayoutActivity : ComponentActivity() {
    /**
     * Permissions we need to ask for.
     */
    var permissions: Array<String> = arrayOf(Manifest.permission.CAMERA)

    /**
     * The instance of [FoldingLayout] in our layout with id `R.id.fold_view`
     */
    private var mFoldLayout: FoldingLayout? = null

    /**
     * The [SeekBar] in our layout with id `R.id.anchor_seek_bar`, used to move the anchor point
     * of the [FoldingLayout].
     */
    private var mAnchorSeekBar: SeekBar? = null

    /**
     * Orientation of our [FoldingLayout], toggled by the menu item with id `R.id.toggle_orientation`
     * (labeled either "Horizontal" or "Vertical" depending on the current orientation).
     */
    private var mOrientation: Orientation = HORIZONTAL

    /**
     * Used to set the fold factor of our [FoldingLayout] using "scrolling" of its view.
     */
    private var mTranslation: Int = 0

    /**
     * Number of folds that our [FoldingLayout] creates, set by the [Spinner] in our
     * options menu whose id is `R.id.num_of_folds`
     */
    private var mNumberOfFolds: Int = 2

    /**
     * Top location or our [FoldingLayout] on the screen, set in our [onWindowFocusChanged]
     * override by calling the [View.getLocationOnScreen] method of [mFoldLayout].
     */
    private var mParentPositionY: Int = -1

    /**
     * Distance in pixels a touch can wander before we think the user is scrolling, set in [onCreate]
     * by calling the [ViewConfiguration.getScaledTouchSlop] method of our [ViewConfiguration] (which
     * contains methods to access standard constants used in the UI for timeouts, sizes, and distances
     * for our device).
     */
    private var mTouchSlop: Int = -1

    /**
     * Anchor factor, set by the [SeekBar] field [mAnchorSeekBar] to a value between 0f and 1f, used
     * to locate the anchor point of the [FoldingLayout]
     */
    private var mAnchorFactor: Float = 0f

    /**
     * A flag to indicate that our [OnItemSelectedListener.onItemSelected] override for the spinner
     * with id `R.id.num_of_folds` has been called once already (during its creation) and all further
     * calls are in response to user input.
     */
    private var mDidLoadSpinner: Boolean = true

    /**
     * Flag to indicate that we are not in the middle of reacting to the user scrolling our view.
     */
    private var mDidNotStartScroll: Boolean = true

    /**
     * Flag to indicate that we are folding a live camera feed instead of a still image, toggled by
     * the [CheckBox] with id `R.id.camera_feed` ("Camera Feed") in our options menu
     */
    private var mIsCameraFeed: Boolean = false

    /**
     * Flag to indicate that our [FoldingLayout] should use sepia mode (use a [Paint] with a
     * saturation value of 0, mapping colors to gray-scale) for the folds while it is folding,
     * toggled by the checkbox with id `R.id.sepia` ("Sepia Off") in our options menu.
     */
    private var mIsSepiaOn: Boolean = true

    /**
     * The [GestureDetector] we create to interpret scrolling gestures using an instance of our
     * class [ScrollGestureDetector].
     */
    private var mScrollGestureDetector: GestureDetector? = null

    /**
     * The [ItemSelectedListener] (subclass of [OnItemSelectedListener]) we use to react to items
     * selected in the [Spinner] with id `R.id.num_of_folds` in our options menu (used to select
     * the number of folds to use when folding our [FoldingLayout]).
     */
    private var mItemSelectedListener: ItemSelectedListener? = null

    /**
     * The camera instance we use as the content of our [TextureView] field [mTextureView] when the
     * camera source is selected by checking the [CheckBox] with id `R.id.camera_feed` ("Camera Feed")
     * in our options menu.
     */
    private var mCamera: Camera? = null

    /**
     * The `TextureView` that holds our camera preview.
     */
    private var mTextureView: TextureView? = null

    /**
     * The [ImageView] that holds our still image, the jpg with resource id `R.drawable.image`.
     */
    private var mImageView: ImageView? = null

    /**
     * [Paint] whose saturation is set to 0 (gray scale for all colors), used while the view is
     * folding if the [CheckBox] with id `R.id.sepia` ("Sepia Off") is not checked.
     */
    private var mSepiaPaint: Paint? = null

    /**
     * Default [Paint] used while the view is folding if the [CheckBox] with id `R.id.sepia`
     * ("Sepia Off") is checked.
     */
    private var mDefaultPaint: Paint? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.activity_fold`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the
     * [View] passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [ImageView] property [mImageView] by finding the view with id
     * `R.id.image_view`, set its padding to [ANTIALIAS_PADDING] on all sides, set its scale type
     * (how the image should be resized or moved to match the size of the [ImageView]) to
     * [ImageView.ScaleType.FIT_XY] (Scale the image using [Matrix.ScaleToFit.FILL] (scale in X
     * and Y independently, so that src matches dst exactly), and then set its content to the jpg
     * with resource id `R.drawable.image`. We then initialize our [TextureView] field
     * [mTextureView] with a new instance, and set its [SurfaceTextureListener] to our
     * [SurfaceTextureListener] field [mSurfaceTextureListener] (its
     * [SurfaceTextureListener.onSurfaceTextureAvailable] override opens the [Camera] and sets
     * the [SurfaceTexture] to be used for live preview of the camera). We then initialize our
     * [SeekBar] field [mAnchorSeekBar] by finding the view with id `R.id.anchor_seek_bar`. We
     * initialize our [FoldingLayout] field [mFoldLayout] by finding the view with id
     * `R.id.fold_view`, set its background color to [Color.BLACK], and set its [OnFoldListener]
     * to our [OnFoldListener] field [mOnFoldListener]. We initialize our [Int] field [mTouchSlop]
     * to the standard constant for the UI on this device for the distance in pixels a touch can
     * wander before we think the user is scrolling. We set the [OnSeekBarChangeListener] of our
     * [SeekBar] field [mAnchorSeekBar] to our [OnSeekBarChangeListener] field
     * [mSeekBarChangeListener] (its [OnSeekBarChangeListener.onStopTrackingTouch] override sets
     * the anchor factor (relative location of the anchor point for the folding) of our
     * [FoldingLayout] field [mFoldLayout] to its current progress divided by 100). We initialize
     * our [Paint] fields [mDefaultPaint] and [mSepiaPaint] with new instances. We then initialize
     * our [ColorMatrix] variables `var m1` and `val m2` with new instances. We set the saturation
     * of `m1` to 0 (gray scale) and set `m2` to scale RED by 1.0, GREEN by .95, BLUE by .82 and
     * ALPHA by 1.0, then set `m1` to the concatenation of `m2` and `m1` (which will have the same
     * effect as applying `m1` and then applying `m2`). We then set the color filter of [Paint]
     * field [mSepiaPaint] to a [ColorMatrixColorFilter] created from `m1`
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fold)
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

        mImageView = findViewById(R.id.image_view)
        mImageView!!.setPadding(
            ANTIALIAS_PADDING, ANTIALIAS_PADDING,
            ANTIALIAS_PADDING, ANTIALIAS_PADDING
        )
        mImageView!!.scaleType = ImageView.ScaleType.FIT_XY
        mImageView!!.setImageDrawable(resources.getDrawable(R.drawable.image))
        mTextureView = TextureView(this)
        mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
        mAnchorSeekBar = findViewById(R.id.anchor_seek_bar)
        mFoldLayout = findViewById(R.id.fold_view)
        mFoldLayout!!.setBackgroundColor(Color.BLACK)
        mFoldLayout!!.setFoldListener(mOnFoldListener)
        mTouchSlop = ViewConfiguration.get(this).scaledTouchSlop
        mAnchorSeekBar!!.setOnSeekBarChangeListener(mSeekBarChangeListener)
        mScrollGestureDetector = GestureDetector(this, ScrollGestureDetector())
        mItemSelectedListener = ItemSelectedListener()
        mDefaultPaint = Paint()
        mSepiaPaint = Paint()
        val m1 = ColorMatrix()
        val m2 = ColorMatrix()
        m1.setSaturation(0f)
        m2.setScale(1f, .95f, .82f, 1.0f)
        m1.setConcat(m2, m1)
        mSepiaPaint!!.colorFilter = ColorMatrixColorFilter(m1)
    }

    /**
     * Called when the main window associated with the activity has been attached to the window
     * manager. First we call our super's implementation of `onAttachedToWindow`, then we
     * ask the user for the required permissions by calling [requestPermissions].
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestPermissions(permissions, 1)
    }

    /**
     * This listener, along with the [setSepiaLayer] method below, show a possible use case
     * of the [OnFoldListener] provided with the [FoldingLayout]. This is a fun extra addition
     * to the demo showing what kind of visual effects can be applied to the child of the
     * [FoldingLayout] by setting the layer type to hardware. With a hardware layer type
     * applied to the child, a paint object can also be applied to the same layer. Using
     * the concatenation of two different color matrices (above), a color filter was created
     * which simulates a sepia effect on the layer.
     */
    private val mOnFoldListener: OnFoldListener = object : OnFoldListener {
        /**
         * Called when the [FoldingLayout] we are a [OnFoldListener] for is starting its folding.
         * If our [Boolean] flag field [mIsSepiaOn] is `true` we call our [setSepiaLayer] method
         * to place the child of our [FoldingLayout] into "sepia" mode, ie. using hardware layer
         * with the sepia colored [Paint] field [mSepiaPaint] as its paint.
         */
        override fun onStartFold() {
            if (mIsSepiaOn) {
                setSepiaLayer(mFoldLayout!!.getChildAt(0), true)
            }
        }

        /**
         * Called when the [FoldingLayout] we are a [OnFoldListener] for is ending its folding. We
         * call our [setSepiaLayer] method to place the child of our [FoldingLayout] into "non-sepia"
         * mode (whether it was that way already or not) by setting the paint of its hardware layer
         * to [Paint] field [mDefaultPaint].
         */
        override fun onEndFold() {
            setSepiaLayer(mFoldLayout!!.getChildAt(0), false)
        }
    }

    /**
     * Called to set the [Paint] of the the current layer of its [View] parameter [view] to the
     * [Paint] selected by its [Boolean] parmameter [isSepiaLayerOn]. If [IS_JBMR2] is `true` we
     * return without doing anything ([Build.VERSION_CODES.JELLY_BEAN_MR2] has a bug which makes
     * our code not work correctly). Other wise we branch on the value of [isSepiaLayerOn]:
     *
     *  * `true`: We call the [View.setLayerType] method of [view] to set the type of layer backing
     *  this view to [View.LAYER_TYPE_HARDWARE] (Indicates that the view has a hardware layer. A
     *  hardware layer is backed by a hardware specific texture (generally Frame Buffer Objects or
     *  FBO on OpenGL hardware) and causes the view to be rendered using Android's hardware rendering
     *  pipeline, but only if hardware acceleration is turned on for the view hierarchy. When
     *  hardware acceleration is turned off, hardware layers behave exactly as software layers.)
     *  We then call the [View.setLayerPaint] method of [view] to update the Paint object used with
     *  the current layer to be [Paint] field [mSepiaPaint].
     *
     *  * `false`: we call the [View.setLayerPaint] method of [view] to update the Paint object used
     *  with the current layer to be [Paint] field [mDefaultPaint].
     */
    private fun setSepiaLayer(view: View, isSepiaLayerOn: Boolean) {
        if (!IS_JBMR2) {
            if (isSepiaLayerOn) {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                view.setLayerPaint(mSepiaPaint)
            } else {
                view.setLayerPaint(mDefaultPaint)
            }
        }
    }

    /**
     * Creates a [SurfaceTextureListener] in order to prepare a [TextureView]
     * which displays a live, and continuously updated, feed from the Camera.
     */
    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        /**
         * Invoked when a [TextureView]'s [SurfaceTexture] is ready for use. We initialize our
         * [Camera] field [mCamera] with the [Camera] object created by the method [Camera.open].
         * If [mCamera] is `null` and the [Camera.getNumberOfCameras] method tells us that there
         * are more than 1 camera we set [mCamera] to the [Camera] object created by the method
         * [Camera.open] for the camera id [Camera.CameraInfo.CAMERA_FACING_FRONT]. If [mCamera]
         * is still `null` we return having done nothing. Otherwise wrapped in a try block intended
         * to catch [IOException] (in order to print a stack trace) we set the preview texture of
         * [mCamera] to our [SurfaceTexture] parameter [surfaceTexture], set its display orientation
         * to 90 degrees (portrait), and finally we call the [Camera.startPreview] method of [mCamera]
         * to start capturing and drawing preview frames to the screen.
         *
         * @param surfaceTexture The surface returned by [TextureView.getSurfaceTexture]
         * @param i              The width of the surface
         * @param i2             The height of the surface
         */
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i2: Int) {
            mCamera = Camera.open()
            if (mCamera == null && Camera.getNumberOfCameras() > 1) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
            }
            if (mCamera == null) {
                return
            }
            try {
                mCamera!!.setPreviewTexture(surfaceTexture)
                mCamera!!.setDisplayOrientation(90)
                mCamera!!.startPreview()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /**
         * Invoked when the [SurfaceTexture]'s buffers size changed. We ignore this since [Camera]
         * does all the work for us.
         *
         * @param surfaceTexture The surface returned by [TextureView.getSurfaceTexture]
         * @param i              The new width of the surface
         * @param i2             The new height of the surface
         */
        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i2: Int) {
            // Ignored, Camera does all the work for us
        }

        /**
         * Invoked when the specified [SurfaceTexture] is about to be destroyed. If our [Camera]
         * field [mCamera] is not `null` we call its [Camera.stopPreview] method to have it stop
         * capturing and drawing preview frames to the surface, then call its [Camera.release]
         * method to disconnect and release its resources. Finally we return `true` to indicate
         * that no further rendering should happen inside the surface texture.
         *
         * @param surfaceTexture The surface about to be destroyed
         */
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            if (mCamera != null) {
                mCamera!!.stopPreview()
                mCamera!!.release()
            }
            return true
        }

        /**
         * Invoked when the specified [SurfaceTexture] is updated through
         * [SurfaceTexture.updateTexImage]. We ignore.
         *
         * @param surfaceTexture The surface just updated
         */
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            // Invoked every time there's a new Camera preview frame
        }
    }

    /**
     * A listener for scrolling changes in the seekbar. The anchor point of the folding view is
     * updated every time the seekbar stops tracking touch events. Every time the anchor point
     * is updated, the folding view is restored to a default unfolded state.
     */
    private val mSeekBarChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        /**
         * Notification that the progress level has changed. We ignore.
         *
         * @param seekBar The [SeekBar] whose progress has changed
         * @param i       The current progress level, between 0 and 100.
         * @param b       `true` if the progress change was initiated by the user.
         */
        override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {}

        /**
         * Notification that the user has started a touch gesture. We ignore.
         *
         * @param seekBar The SeekBar in which the touch gesture began
         */
        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        /**
         * Notification that the user has finished a touch gesture. First we set our [Int] field
         * [mTranslation] to 0 (the user has not been scrolling the child of our [FoldingLayout]).
         * Then we set the [Float] field [mAnchorFactor] to the current level of progress of
         * [mAnchorSeekBar] divided by 100f. Finally we set the [FoldingLayout.anchorFactor]
         * property of [FoldingLayout] field [mFoldLayout] to [mAnchorFactor] to have it adjust
         * the anchor point of its folding to the location corresponding to it.
         *
         * @param seekBar The [SeekBar] in which the touch gesture began
         */
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            mTranslation = 0
            mAnchorFactor = mAnchorSeekBar!!.progress.toFloat() / 100.0f
            mFoldLayout!!.anchorFactor = mAnchorFactor
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. If our [Boolean] flag field
     * [IS_JBMR2] indicates that our device is running the buggy `JELLY_BEAN_MR2` release we fetch
     * a [MenuInflater] for our context and use it to inflate the options menu `R.menu.fold_with_bug`
     * into [Menu] parameter [menu] (it lacks the option items for the camera). Otherwise we fetch a
     * [MenuInflater] for our context and use it to inflate the options menu `R.menu.fold` into [menu]
     * (full featured option menu). We initialize our [Spinner] variable `val s` by finding the item
     * in [menu] with id `R.id.num_of_folds` and retrieving its action view. We then set the
     * [OnItemSelectedListener] of `s` to our [ItemSelectedListener] field [mItemSelectedListener]
     * and return `true` to the caller so that our menu will be displayed.
     *
     * @param menu The options [Menu] in which we place our items.
     * @return You must return `true` for the menu to be displayed.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (IS_JBMR2) {
            menuInflater.inflate(R.menu.fold_with_bug, menu)
        } else {
            menuInflater.inflate(R.menu.fold, menu)
        }
        val s = menu.findItem(R.id.num_of_folds).actionView as Spinner?
        s!!.onItemSelectedListener = mItemSelectedListener
        return true
    }

    /**
     * Called when the current [Window] of the activity gains or loses focus. First we call our
     * super's implementation of `onWindowFocusChanged`. Then we allocate 2 [Int]s for our [IntArray]
     * variable `val loc` and call the [View.getLocationOnScreen] method of [FoldingLayout] field
     * [mFoldLayout] to have it load `loc` with its window location. We then set our [Int] field
     * [mParentPositionY] to the Y coordinate stored in `loc[1]`.
     *
     * @param hasFocus Whether the window of this activity has focus.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val loc = IntArray(2)
        mFoldLayout!!.getLocationOnScreen(loc)
        mParentPositionY = loc[1]
    }

    /**
     * Called when a touch screen event was not handled by any of the views under it. We return the
     * value returned by the [GestureDetector.onTouchEvent] method of our [GestureDetector] field
     * [mScrollGestureDetector].
     *
     * @param me The touch screen [MotionEvent] being processed.
     * @return Return `true` if you have consumed the event, `false` if you haven't.
     */
    override fun onTouchEvent(me: MotionEvent): Boolean {
        return mScrollGestureDetector!!.onTouchEvent(me)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We `when` switch on
     * the `itemId` of our [MenuItem] parameter [item]:
     *
     *  * `R.id.animate_fold`: ("Animate") we call our method [animateFold] to animate the folding
     *  view inwards (to a completely folded state) from its current state and then back out to its
     *  original state.
     *
     *  * `R.id.toggle_orientation`: ("Vertical" or "Horizontal" depending on the state of the
     *  current orientation) we toggle our [Orientation] field [mOrientation] between [VERTICAL]
     *  or [HORIZONTAL], then set the title of our [MenuItem] parameter [item] to "Vertical" if
     *  the new state is [HORIZONTAL] or to "Horizontal" if it is [VERTICAL]. We then set our [Int]
     *  field [mTranslation] to 0 and set the [FoldingLayout.orientation] property of our
     *  [FoldingLayout] field [mFoldLayout] to set the orientation it folds in to [mOrientation].
     *
     *  * `R.id.camera_feed`: ("Camera Feed" or "Static Image" depending on the current image
     *  source) we toggle our [Boolean] field [mIsCameraFeed], and if the new value is `true` we
     *  set the title of our [MenuItem] parameter [item] to "Static Image", if it is `false` we
     *  set the title to "Camera Feed". We then set the checked state of our item's [CheckBox] to
     *  the value of [mIsCameraFeed]. We now branch on the value of [mIsCameraFeed]:
     *
     *  * `true`: we call the [FoldingLayout.removeView] method of [FoldingLayout] field [mFoldLayout]
     *  to remove [ImageView] field [mImageView], then call its [FoldingLayout.addView] method to add
     *  the [TextureView] field [mTextureView] using the height and width of [mFoldLayout] as its
     *  layout parameters.
     *
     *  * `false`: we call the [FoldingLayout.removeView] method of [FoldingLayout] field
     *  [mFoldLayout] to remove [TextureView] field [mTextureView], then call its
     *  [FoldingLayout.addView] method to add the [ImageView] field [mImageView] using the height
     *  and width of [mFoldLayout] as its layout parameters.
     *
     *  * `R.id.sepia`: ("Sepia Off") we toggle the value of our [Boolean] field [mIsSepiaOn], then
     *  set the checked state of our item's [CheckBox] to the inverse of [mIsSepiaOn]. Then if
     *  [mIsSepiaOn] is now `true` and the current fold factor of [mFoldLayout] is not 0 (it is
     *  partially folded) we call our method [setSepiaLayer] to set the sepia layer mode of the
     *  one and only child of [mFoldLayout], otherwise we call the method to clear its sepia layer
     *  mode.
     *
     *  * default: we do nothing.
     *
     * Finally we return the value returned by our super's implementation of `onOptionsItemSelected`
     * to the caller.
     *
     * @param item The [MenuItem] that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to proceed, `true` to
     * consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.animate_fold -> animateFold()
            R.id.toggle_orientation -> {
                mOrientation = if (mOrientation == HORIZONTAL) VERTICAL else HORIZONTAL
                item.setTitle(if (mOrientation == HORIZONTAL) R.string.vertical else R.string.horizontal)
                mTranslation = 0
                mFoldLayout!!.orientation = mOrientation
            }

            R.id.camera_feed -> {
                mIsCameraFeed = !mIsCameraFeed
                item.setTitle(if (mIsCameraFeed) R.string.static_image else R.string.camera_feed)
                item.isChecked = mIsCameraFeed
                if (mIsCameraFeed) {
                    mFoldLayout!!.removeView(mImageView)
                    mFoldLayout!!.addView(mTextureView, ViewGroup.LayoutParams(
                        mFoldLayout!!.width, mFoldLayout!!.height))
                } else {
                    mFoldLayout!!.removeView(mTextureView)
                    mFoldLayout!!.addView(mImageView, ViewGroup.LayoutParams(
                        mFoldLayout!!.width, mFoldLayout!!.height))
                }
                mTranslation = 0
            }

            R.id.sepia -> {
                mIsSepiaOn = !mIsSepiaOn
                item.isChecked = !mIsSepiaOn
                if (mIsSepiaOn && mFoldLayout!!.foldFactor != 0f) {
                    setSepiaLayer(mFoldLayout!!.getChildAt(0), true)
                } else {
                    setSepiaLayer(mFoldLayout!!.getChildAt(0), false)
                }
            }

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Animates the folding view inwards (to a completely folded state) from its current state and
     * then back out to its original state. We initialize our [Float] variable `val foldFactor` with
     * the current fold factor of our [FoldingLayout] field [mFoldLayout] (the value of its
     * [FoldingLayout.foldFactor] property, a value between 0 (not folded) and 1 (fully folded)).
     * We create [ObjectAnimator] variable `val animator` to animate the [FoldingLayout.foldFactor]
     * property of [mFoldLayout] between `foldFactor` and 1 (the [FoldingLayout.foldFactor] property
     * of [mFoldLayout] will be called for each step of the animation). We set the repeat mode of
     * `animator` to [ValueAnimator.REVERSE], its repeat count to 1, its duration to our [Int] field
     * [FOLD_ANIMATION_DURATION] (1000 milliseconds), and its interpolator to a new instance of
     * [AccelerateInterpolator]. Finally we start `animator` running.
     */
    @SuppressLint("ObjectAnimatorBinding")
    fun animateFold() {
        val foldFactor = mFoldLayout!!.foldFactor
        val animator = ObjectAnimator.ofFloat(mFoldLayout, "foldFactor", foldFactor, 1f)
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = 1
        animator.duration = FOLD_ANIMATION_DURATION.toLong()
        animator.interpolator = AccelerateInterpolator()
        animator.start()
    }

    /**
     * Listens for selection events of the spinner located on the action bar. Every
     * time a new value is selected, the number of folds in the folding view is updated
     * and it is also restored to a default unfolded state.
     */
    private inner class ItemSelectedListener : OnItemSelectedListener {
        /**
         * Callback method to be invoked when an item in this view has been selected. We initialize
         * our [Int] field [mNumberOfFolds] by parsing as an integer the string value of item at
         * position `pos` in the `parent` [AdapterView] (the string value of an `Item` is the title
         * of it, and the titles of the items in our [Spinner] are taken from the "string-array" in
         * our resources whose ID is `R.array.num_of_folds_array` which is a list of the strings
         * "2", "3", "4", "5", "6", "7", "8", and "1" so applying the [String.toInt] returns their
         * [Int] value). If our [Boolean] field [mDidLoadSpinner] is `true` we set it to `false` (we
         * are just being called when our [Spinner] is created, not as the result of a user action),
         * otherwise we set our [Int] field [mTranslation] to 0 (undoes any current folding due to
         * the user's scrolling (folding) of the view), and set the number of folds that our
         * [FoldingLayout] field [mFoldLayout] will fold in to [mNumberOfFolds].
         *
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param pos The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
            mNumberOfFolds = parent.getItemAtPosition(pos).toString().toInt()
            if (mDidLoadSpinner) {
                mDidLoadSpinner = false
            } else {
                mTranslation = 0
                mFoldLayout!!.numberOfFolds = mNumberOfFolds
            }
        }

        /**
         * Callback method to be invoked when the selection disappears from this view. We ignore.
         *
         * @param arg0 The [AdapterView] that now contains no selected item.
         */
        override fun onNothingSelected(arg0: AdapterView<*>?) {}
    }

    /**
     * This class uses user touch events to fold and unfold the folding view.
     */
    private inner class ScrollGestureDetector : SimpleOnGestureListener() {
        /**
         * Notified when a tap occurs with the down [MotionEvent] that triggered it. This will be
         * triggered immediately for every down event. All other events should be preceded by this.
         * We just set our [Boolean] field [mDidNotStartScroll] to `true` (we are not yet in the
         * middle of a scroll gesture), and return `true` to the caller.
         *
         * @param e The down motion event.
         * @return true (no hint why? false works just as well)
         */
        override fun onDown(e: MotionEvent): Boolean {
            mDidNotStartScroll = true
            return true
        }

        /**
         * Notified when a scroll occurs with the initial on down [MotionEvent] and the current
         * move [MotionEvent]. The distance in x and y is also supplied for convenience.
         *
         * All the logic here is used to determine by what factor the paper view should be folded in
         * response to the user's touch events. The logic here uses vertical scrolling to fold a
         * vertically oriented view and horizontal scrolling to fold a horizontally oriented fold.
         * Depending on where the anchor point of the fold is, movements towards or away from the
         * anchor point will either fold or unfold the paper respectively.
         *
         * The translation logic here also accounts for the touch slop when a new user touch
         * begins, but before a scroll event is first invoked.
         *
         * We declare our [Int] variable `val touchSlop` and [Float] variable `val factor`, then
         * branch on the value of our [Orientation] field [mOrientation]:
         *
         *  * [VERTICAL]: we set `factor` to the absolute value of the result of dividing our [Int]
         *  field [mTranslation] by the height of our [FoldingLayout] field [mFoldLayout]. If the Y
         *  coordinate of [MotionEvent] parameter [e2] (coordinate of the move motion that triggered
         *  the current call) minus our [Int] field [mParentPositionY] is less than or equal to the
         *  height of [mFoldLayout] and its Y coordinate minus [mParentPositionY] is greater than 0
         *  (the scroll started occurring over [mFoldLayout]) we want to update the value of
         *  [mTranslation] (while less than 0 it represents the total distance that the user has
         *  scrolled) and to do this we branch based on whether the [mFoldLayout] is folded less
         *  than the value the current Y position would order it to be or greater:
         *
         *  * Greater: (the user is folding it more) we subtract [Float] parameter [distanceY] (the
         *  distance the user has scrolled since we were last called) from the accumulated scroll in
         *  [mTranslation], and if [distanceY] is less than 0 we set `touchSlop` to minus
         *  `mTouchSlop`, if greater than or equal to 0 we set it to `mTouchSlop`.
         *
         *  * Less than or equal to: (the user is unfolding it) we add `distanceY` (the distance
         *  the user has scrolled since we were last called) to the accumulated scroll in
         *  `mTranslation`, and if `distanceY` is less than 0 we set `touchSlop` to [Int] field
         *  [mTouchSlop], if greater than or equal to 0 we set it to minus [mTouchSlop].
         *
         *  * If our [Boolean] field [mDidNotStartScroll] is true we set [mTranslation] to
         *  [mTranslation] plus `touchSlop`. If [mTranslation] is less than minus the height of
         *  [mFoldLayout] we set it to minus the height of [mFoldLayout] (it is now totally folded).
         *
         *  * [HORIZONTAL]: we set `factor` to the absolute value of the result of dividing our
         *  [Int] field [mTranslation] by the width of our [FoldingLayout] field [mFoldLayout].
         *  We branch based on whether the raw X coordinate of [MotionEvent] parameter [e2]
         *  (coordinate of the move motion that triggered the current call) is greater than the
         *  with of [mFoldLayout] times our [Float] field [mAnchorFactor]:
         *
         *  * Greater: (the user is folding it more) we subtract [Float] parameter [distanceX] (the
         *  distance the user has scrolled since we were last called) from the accumulated scroll in
         *  [mTranslation], and if [distanceX] is less than 0 we set `touchSlop` to minus [Int]
         *  field [mTouchSlop], if greater than or equal to 0 we set it to [mTouchSlop].
         *
         *  * Less than or equal to: (the user is unfolding it) we add [Float] parameter [distanceX]
         *  (the distance the user has scrolled since we were last called) to the accumulated scroll
         *  in [mTranslation], and if [distanceX] is less than 0 we set `touchSlop` to [mTouchSlop],
         *  if greater than or equal to 0 we set it to minus [mTouchSlop].
         *
         *  * If [mDidNotStartScroll] is `true` we set [mTranslation] to [mTranslation] plus
         *  `touchSlop`. If [mTranslation] is less than minus the width of [mFoldLayout] we set it
         *  to minus the width of [mFoldLayout] (it is now totally folded).
         *
         * We now set our [Boolean] field [mDidNotStartScroll] to `false` (we have started scrolling).
         * If our [Int] field [mTranslation] is greater than 0 we set it to 0 (we do not try to unfold
         * more than fully unfolded). Finally we call the [FoldingLayout.foldFactor] property of
         * [FoldingLayout] field [mFoldLayout] to have it fold to the value of [Float] variable
         * `factor`, and return `true` to the caller (`false` works too).
         *
         * @param e1 The first down [MotionEvent] that started the scrolling.
         * @param e2 The move [MotionEvent] that triggered the current onScroll.
         * @param distanceX The distance along the X axis that has been scrolled since the last
         * call to [onScroll]. This is NOT the distance between [e1] and [e2].
         * @param distanceY The distance along the Y axis that has been scrolled since the last
         * call to [onScroll]. This is NOT the distance between [e1] and [e2].
         * @return `true` if the event is consumed, else `false`
         */
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val touchSlop: Int
            val factor: Float
            if (mOrientation == VERTICAL) {
                factor = Math.abs(mTranslation.toFloat() / mFoldLayout!!.height.toFloat())
                if (e2.y - mParentPositionY <= mFoldLayout!!.height
                    && e2.y - mParentPositionY >= 0) {
                    if (e2.y - mParentPositionY > mFoldLayout!!.height * mAnchorFactor) {
                        mTranslation -= distanceY.toInt()
                        touchSlop = if (distanceY < 0) -mTouchSlop else mTouchSlop
                    } else {
                        mTranslation += distanceY.toInt()
                        touchSlop = if (distanceY < 0) mTouchSlop else -mTouchSlop
                    }
                    mTranslation = if (mDidNotStartScroll) mTranslation + touchSlop else mTranslation
                    if (mTranslation < -mFoldLayout!!.height) {
                        mTranslation = -mFoldLayout!!.height
                    }
                }
            } else {
                factor = Math.abs(mTranslation.toFloat() / mFoldLayout!!.width.toFloat())
                if (e2.rawX > mFoldLayout!!.width * mAnchorFactor) {
                    mTranslation -= distanceX.toInt()
                    touchSlop = if (distanceX < 0) -mTouchSlop else mTouchSlop
                } else {
                    mTranslation += distanceX.toInt()
                    touchSlop = if (distanceX < 0) mTouchSlop else -mTouchSlop
                }
                mTranslation = if (mDidNotStartScroll) mTranslation + touchSlop else mTranslation
                if (mTranslation < -mFoldLayout!!.width) {
                    mTranslation = -mFoldLayout!!.width
                }
            }
            mDidNotStartScroll = false
            if (mTranslation > 0) {
                mTranslation = 0
            }
            mFoldLayout!!.foldFactor = factor
            return true
        }
    }

    companion object {
        /**
         * A bug was introduced in Android 4.3 that ignores changes to the Canvas state
         * between multiple calls to super.dispatchDraw() when running with hardware acceleration.
         * To account for this bug, a slightly different approach was taken to fold a
         * static image whereby a bitmap of the original contents is captured and drawn
         * in segments onto the canvas. However, this method does not permit the folding
         * of a TextureView hosting a live camera feed which continuously updates.
         * Furthermore, the sepia effect was removed from the bitmap variation of the
         * demo to simplify the logic when running with this workaround."
         */
        @JvmField
        @SuppressLint("ObsoleteSdkInt")
        val IS_JBMR2: Boolean = Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2

        /**
         * Padding used for our `ImageView mImageView`
         */
        private const val ANTIALIAS_PADDING: Int = 1

        /**
         * Duration of the animation of the "foldFactor" property of `FoldingLayout mFoldLayout`
         */
        private const val FOLD_ANIMATION_DURATION: Int = 1000
    }
}
