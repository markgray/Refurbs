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
    "DEPRECATION",
    "VARIABLE_WITH_REDUNDANT_INITIALIZER",
    "ReplaceNotNullAssertionWithElvisReturn",
    "ReplaceJavaStaticMethodWithKotlinAnalog",
    "KotlinConstantConditions",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.imagepixelization

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.util.Arrays
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale

/**
 * This application shows three different graphics/animation concepts:
 *
 *  1. A pixelization effect is applied to an image with varying pixelization
 *  factors to achieve an image that is pixelized to varying degrees. In
 *  order to optimize the amount of image processing performed on the image
 *  being pixelized, the pixelization effect only takes place if a predefined
 *  amount of time has elapsed since the main image was last pixelized. The
 *  effect is also applied when the user stops moving the seekbar.
 *
 *  2. This application also shows how to use a ValueAnimator to achieve a
 *  smooth self-animating seekbar.
 *
 *  3. Lastly, this application shows a use case of AsyncTask where some
 *  computation heavy processing can be moved onto a background thread,
 *  so as to keep the UI completely responsive to user input.
 */
class ImagePixelization : ComponentActivity() {
    /**
     * Original [Bitmap] loaded from `R.drawable.image` jpg
     */
    var mImageBitmap: Bitmap? = null

    /**
     * [ImageView] in our layout with id `R.id.pixelView`, used to display our pixelated image.
     */
    var mImageView: ImageView? = null

    /**
     * [SeekBar] in our layout with id `R.id.seekbar`, used to control degree of pixelization
     */
    var mSeekBar: SeekBar? = null

    /**
     * Flag indicating the [CheckBox] in our options menu with id `R.id.checkbox`
     * ("Using AsyncTask") is checked.
     */
    var mIsChecked: Boolean = false

    /**
     * Flag indicating the [CheckBox] in our options menu with id `R.id.builtin_pixelation_checkbox`
     * ("Built-in Pixelization") is checked.
     */
    var mIsBuiltinPixelizationChecked: Boolean = false

    /**
     * Progress of the [SeekBar] with id `R.id.seekbar` last time our [invokePixelization]
     * method was called.
     */
    var mLastProgress: Int = 0

    /**
     * System time in milliseconds the last time our [invokePixelization] method was called.
     */
    var mLastTime: Long = 0

    /**
     * [Bitmap] we use to draw our custom pixelated image into.
     */
    var mPixelatedBitmap: Bitmap? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.activity_image_pixelization`.
     *
     * We initialize our [RelativeLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [ImageView] field [mImageView] by finding the view with id `R.id.pixelView`,
     * and our [SeekBar] field [mSeekBar] by finding the view with id `R.id.seekbar`. We initialize
     * our [Bitmap] field [mImageBitmap] by decoding the jpg with resource id `R.drawable.image`,
     * and set it to be the content of [mImageView]. Finally we set the [OnSeekBarChangeListener] of
     * [mSeekBar] to our [OnSeekBarChangeListener] field [mOnSeekBarChangeListener].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_pixelization)
        val rootView = findViewById<RelativeLayout>(R.id.root_view)
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
        mImageView = findViewById(R.id.pixelView)
        mSeekBar = findViewById(R.id.seekbar)
        mImageBitmap = BitmapFactory.decodeResource(resources, R.drawable.image)
        mImageView!!.setImageBitmap(mImageBitmap)
        mSeekBar!!.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    }

    /**
     * The [OnSeekBarChangeListener] for our [SeekBar] field [mSeekBar] (the [SeekBar] with id
     * `R.id.seekbar`), communicates changes to the position of the [SeekBar] to the parts of
     * our code responsible for pixilating our image.
     */
    private val mOnSeekBarChangeListener: OnSeekBarChangeListener =
        object : OnSeekBarChangeListener {
            /**
             * Notification that the user has finished a touch gesture. If the new position of our
             * [SeekBar] is [SEEKBAR_STOP_CHANGE_DELTA] (5) different than the last position we
             * pixilated for we call our method [invokePixelization] to update the level of
             * pixilation of our image.
             *
             * @param seekBar The [SeekBar] in which the touch gesture began
             */
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (Math.abs(mSeekBar!!.progress - mLastProgress) > SEEKBAR_STOP_CHANGE_DELTA) {
                    invokePixelization()
                }
            }

            /**
             * Notification that the user has started a touch gesture. We ignore.
             *
             * @param seekBar The [SeekBar] in which the touch gesture began
             */
            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            /**
             * Notification that the progress level has changed. We just call our method
             * [checkIfShouldPixelize] to first check if enough time has elapsed since the
             * last pixelization call was invoked, and if it has it calls our method
             * `invokePixelization` to update the level of pixilation of our image.
             *
             * @param seekBar The [SeekBar] whose progress has changed
             * @param progress The current progress level.
             * @param fromUser `true` if the progress change was initiated by the user.
             */
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                checkIfShouldPixelize()
            }
        }

    /**
     * Checks if enough time has elapsed since the last pixelization call was invoked. This prevents
     * too many pixelization processes from being invoked at the same time while previous ones have
     * not yet completed. We check if the current system time is [TIME_BETWEEN_TASKS] (400) later
     * than our [Long] field [mLastTime] and if it is we call our method `invokePixelization` to
     * update the level of pixilation of our image.
     */
    fun checkIfShouldPixelize() {
        if (System.currentTimeMillis() - mLastTime > TIME_BETWEEN_TASKS) {
            invokePixelization()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We fetch a [MenuInflater]
     * for this context and use it to inflate our menu layout file `R.menu.image_pixelization` into
     * our [Menu] parameter [menu].
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_pixelization, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We switch on the item
     * id of our [MenuItem] parameter [item]:
     *
     *  * `R.id.animate`: ("Animate") We initialize [ObjectAnimator] variable `val animator` with an
     *  instance configured to animate the [Int] "progress" property of [SeekBar] field [mSeekBar]
     *  from 0 to the upper limit its range. We set its [TimeInterpolator] to a new instance of
     *  [LinearInterpolator] (interpolator where the rate of change is constant), set its
     *  duration to SEEKBAR_ANIMATION_DURATION (10_000), and start it running.
     *
     *  * `R.id.checkbox`: ("Using AsyncTask") If our [Boolean] field [mIsChecked] is `true` we set
     *  the checked state of [MenuItem] parameter [item] to `false` and set [Boolean] field
     *  [mIsChecked] to `false`, if it is currently `false` we set the checked state of [item] to
     *  `true` and set [mIsChecked] to true.
     *
     *  * `R.id.builtin_pixelation_checkbox`: ("Built-in Pixelization") We toggle the value of our
     *  [Boolean] field [mIsBuiltinPixelizationChecked], and set the checked state of [item] to it.
     *
     *  * default: we ignore.
     *
     * We then return `true` to the caller to consume the event.
     *
     * @param item The [MenuItem] that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to proceed, `true` to
     * consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.animate -> {
                val animator = ObjectAnimator.ofInt(
                    mSeekBar,
                    "progress",
                    0,
                    mSeekBar!!.max
                )
                animator.interpolator = LinearInterpolator()
                animator.duration = SEEKBAR_ANIMATION_DURATION.toLong()
                animator.start()
            }

            R.id.checkbox -> if (mIsChecked) {
                item.isChecked = false
                mIsChecked = false
            } else {
                item.isChecked = true
                mIsChecked = true
            }

            R.id.builtin_pixelation_checkbox -> {
                mIsBuiltinPixelizationChecked = !mIsBuiltinPixelizationChecked
                item.isChecked = mIsBuiltinPixelizationChecked
            }

            else -> {}
        }
        return true
    }

    /**
     * A simple pixelization algorithm. This uses a box blur algorithm where all the pixels within
     * some region are averaged, and that average pixel value is then applied to all the pixels
     * within that region. A higher pixelization factor imposes a smaller number of regions of
     * greater size. Similarly, a smaller pixelization factor imposes a larger number of regions of
     * smaller size.
     *
     * We initialize [Int] variable `val width` with the width of our [Bitmap] parameter [bitmap]
     * and [Int] variable `val height` with its height. If our [Bitmap] field [mPixelatedBitmap] is
     * `null` or has a different width or height than [bitmap] we set [mPixelatedBitmap] to a
     * `width` by `height` instance created to use ARGB_8888 config (each RGB and alpha
     * component occupies 1 byte).
     *
     * We initialize [Int] variable `var xPixels` to our parameter `pixelizationFactor` times `width`
     * truncated to [Int], and if this is 0 we set it to 1. We initialize [Int] variable `yPixels`
     * to our [Float] parameter [pixelizationFactor] times `height` truncated to [Int], and if this
     * is 0 we set it to 1. We declare [Int] variable `var pixel`, [Int] variable `var red`, [Int]
     * variable `var green`, [Int] variable `var blue`, and [Int] variable `var numPixels` setting
     * them to 0. We allocate a `width` times `height` [IntArray] for `val bitmapPixels`, and load
     * a copy of the data in [Bitmap] parameter [bitmap] into it. We allocate a `yPixels` times
     * `xPixels` [IntArray] for `val pixels`, and declare [Int] variable `var maxX` and [Int]
     * variable `var maxY`.
     *
     * Now we loop over [Int] variable `y` while `y` is less then `height` incrementing by `yPixels`
     * and over [Int] variable `x` for `x` less then `width` incrementing by `xPixels`:
     *
     *  * We set `numPixels`, `red`, `green`, and `blue` all to 0. We set `maxX` to the minimum of
     *  `x + xPixels` and `width` (ie to the end point of the square we are considering) and `maxY`
     *  to the minimum of `y + yPixels` and `height` (the bottom of our square).
     *
     *  * We now loop over [Int] `var i` from `x` until `maxX` and [Int] `var j` from `y` to `maxY`
     *  setting `pixel` to the pixel at `j` times `width` plus `i` in `bitmapPixels`, then adding
     *  the red component of `pixel` to `red`, the green component to `green`, the blue component
     *  to `blue` and incrementing `numPixels`.
     *
     *  * When done adding up the color components in our square we set `pixel` to an RGB color
     *  formed by dividing each component by `numPixels`. We then fill all elements of [IntArray]
     *  `pixels` with `pixel`.
     *
     *  * We then initialize [Int] variable `val w` with the minimum of `xPixels` and `width` minus
     *  `x` and [Int] variable `val h` with the minimum of `yPixels` and `height` minus `y`. We then
     *  set the pixels in [Bitmap] field [mPixelatedBitmap] from `x` to `x + w` and from `y` to
     *  `y + h` to `pixels` using a stride of `w` and loop around to consider the next square.
     *
     * When done we return a [BitmapDrawable] created from [Bitmap] field [mPixelatedBitmap].
     *
     * @param pixelizationFactor pixelization factor to achieve.
     * @param bitmap original [Bitmap] to pixilatize.
     * @return a pixelated [BitmapDrawable] version of our [Bitmap] parameter [bitmap].
     */
    fun customImagePixelization(pixelizationFactor: Float, bitmap: Bitmap?): BitmapDrawable {
        val width: Int = bitmap!!.width
        val height: Int = bitmap.height
        if (mPixelatedBitmap == null ||
            !(width == mPixelatedBitmap!!.width && height == mPixelatedBitmap!!.height)
        ) {
            mPixelatedBitmap = createBitmap(width = width, height = height)
        }
        var xPixels: Int = (pixelizationFactor * width.toFloat()).toInt()
        xPixels = if (xPixels > 0) xPixels else 1
        var yPixels: Int = (pixelizationFactor * height.toFloat()).toInt()
        yPixels = if (yPixels > 0) yPixels else 1
        var pixel: Int
        var red: Int
        var green: Int
        var blue: Int
        var numPixels: Int
        val bitmapPixels = IntArray(size = width * height)
        bitmap.getPixels(
            /* pixels = */ bitmapPixels,
            /* offset = */ 0,
            /* stride = */ width,
            /* x = */ 0,
            /* y = */ 0,
            /* width = */ width,
            /* height = */ height
        )
        val pixels = IntArray(size = yPixels * xPixels)
        var maxX: Int
        var maxY: Int
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                blue = 0
                green = blue
                red = green
                numPixels = red
                maxX = Math.min(x + xPixels, width)
                maxY = Math.min(y + yPixels, height)
                for (i in x until maxX) {
                    for (j in y until maxY) {
                        pixel = bitmapPixels[j * width + i]
                        red += Color.red(pixel)
                        green += Color.green(pixel)
                        blue += Color.blue(pixel)
                        numPixels++
                    }
                }
                pixel = Color.rgb(red / numPixels, green / numPixels, blue / numPixels)
                Arrays.fill(pixels, pixel)
                val w: Int = Math.min(xPixels, width - x)
                val h: Int = Math.min(yPixels, height - y)
                mPixelatedBitmap!!.setPixels(
                    /* pixels = */ pixels,
                    /* offset = */ 0,
                    /* stride = */ w,
                    /* x = */ x,
                    /* y = */ y,
                    /* width = */ w,
                    /* height = */ h
                )
                x += xPixels
            }
            y += yPixels
        }
        return mPixelatedBitmap!!.toDrawable(resources = resources)
    }

    /**
     * This method of image pixelization utilizes the bitmap scaling operations built into the
     * framework. By downscaling the bitmap and upscaling it back to its original size (while
     * setting the filter flag to false), the same effect can be achieved with much better
     * performance.
     *
     * We initialize [Int] variable `val width` to the width of our [Bitmap] parameter [bitmap] and
     * [Int] variable `val height` to its height. We initialize [Int] variable
     * `var downScaleFactorWidth` to our [Float] parameter [pixelizationFactor] times `width`
     * truncated to [Int], and if this is 0 we set it to 1. We initialize [Int] variable
     * `var downScaleFactorHeight` to our [Float] parameter [pixelizationFactor] times `height`
     * truncated to [Int], and if this is 0 we set it to 1. We initialize [Int] variable
     * `val downScaledWidth` to `width` divided by `downScaleFactorWidth` and [Int] variable
     * `val downScaledHeight` to `height` divided by `downScaleFactorHeight`.
     *
     * We create [Bitmap] variable `val pixelatedBitmap` from our [Bitmap] parameter [bitmap] using
     * the [Bitmap.createScaledBitmap] method to scale [bitmap] to `downScaledWidth` by
     * `downScaledHeight` without filtering.
     *
     * Now we branch on whether we are running on a JELLY_BEAN_MR1 or newer device:
     *
     *  * JELLY_BEAN_MR1 or newer: We initialize [BitmapDrawable] variable `val bitmapDrawable` with
     *  an instance created from `pixelatedBitmap`, disable its drawable filter, and return it to
     *  the caller.
     *
     *  * Older than JELLY_BEAN_MR1: We initialize [Bitmap] variable `val upscaled` with an instance
     *  created from `pixelatedBitmap` scaled to be `width` by `height` without a filter and return
     *  a [BitmapDrawable] created from it to our caller.
     *
     * @param pixelizationFactor pixelization factor to achieve.
     * @param bitmap original `Bitmap` to pixilatize.
     * @return a pixelated `BitmapDrawable` version of our parameter `Bitmap bitmap`.
     */
    @SuppressLint("ObsoleteSdkInt")
    fun builtInPixelization(pixelizationFactor: Float, bitmap: Bitmap?): BitmapDrawable {
        val width: Int = bitmap!!.width
        val height: Int = bitmap.height
        var downScaleFactorWidth: Int = (pixelizationFactor * width).toInt()
        downScaleFactorWidth = if (downScaleFactorWidth > 0) downScaleFactorWidth else 1
        var downScaleFactorHeight: Int = (pixelizationFactor * height).toInt()
        downScaleFactorHeight = if (downScaleFactorHeight > 0) downScaleFactorHeight else 1
        val downScaledWidth: Int = width / downScaleFactorWidth
        val downScaledHeight: Int = height / downScaleFactorHeight
        val pixelatedBitmap: Bitmap = bitmap.scale(
            width = downScaledWidth,
            height = downScaledHeight,
            filter = false
        )

        /* Bitmap's createScaledBitmap method has a filter parameter that can be set to either
         * true or false in order to specify either bilinear filtering or point sampling
         * respectively when the bitmap is scaled up or down.
         *
         * Similarly, a BitmapDrawable also has a flag to specify the same thing. When the
         * BitmapDrawable is applied to an ImageView that has some scaleType, the filtering
         * flag is taken into consideration. However, for optimization purposes, this flag was
         * ignored in BitmapDrawables before Jelly Bean MR1.
         *
         * Here, it is important to note that prior to JBMR1, two bitmap scaling operations
         * are required to achieve the pixelization effect. Otherwise, a BitmapDrawable
         * can be created corresponding to the downscaled bitmap such that when it is
         * up-scaled to fit the ImageView, the upscaling operation is a lot faster since
         * it uses internal optimizations to fit the ImageView.
         */
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val bitmapDrawable = pixelatedBitmap.toDrawable(resources = resources)
            bitmapDrawable.isFilterBitmap = false
            bitmapDrawable
        } else {
            val upscaled = pixelatedBitmap.scale(width = width, height = height, filter = false)
            upscaled.toDrawable(resources = resources)
        }
    }

    /**
     * Invokes pixelization either on the main thread or on a background thread depending on whether
     * or not the checkbox with id `R.id.checkbox` ("Using AsyncTask") was checked. We set our [Long]
     * field [mLastTime] to the current system time, set our [Int] field [mLastProgress] to the current
     * progress of [SeekBar] field [], then branch on the value of [Boolean] field [mIsChecked]:
     *
     *  * `true`: (background thread) We initialize [PixelizeImageAsyncTask] variable
     *  `val asyncPixelateTask` with a new instance and start it running with the value of the
     *  current progress of [SeekBar] field [mSeekBar] divided by [PROGRESS_TO_PIXELIZATION_FACTOR],
     *  and [Bitmap] field [mImageBitmap] as its two arguments.
     *
     *  * `false`: (main thread) We set the content of [ImageView] field [mImageView] to the
     *  [BitmapDrawable] that our method [pixelizeImage] creates from [Bitmap] field [mImageBitmap]
     *  for a pixelization factorof the current progress of [SeekBar] field [mSeekBar] divided by
     *  [PROGRESS_TO_PIXELIZATION_FACTOR].
     */
    fun invokePixelization() {
        mLastTime = System.currentTimeMillis()
        mLastProgress = mSeekBar!!.progress
        if (mIsChecked) {
            val asyncPixelateTask = PixelizeImageAsyncTask()
            asyncPixelateTask.execute(
                mSeekBar!!.progress / PROGRESS_TO_PIXELIZATION_FACTOR,
                mImageBitmap
            )
        } else {
            mImageView!!.setImageDrawable(
                pixelizeImage(
                    pixelizationFactor = mSeekBar!!.progress / PROGRESS_TO_PIXELIZATION_FACTOR,
                    bitmap = mImageBitmap
                )
            )
        }
    }

    /**
     * Selects either the custom pixelization algorithm that sets and gets bitmap pixels manually or
     * the one that uses built-in bitmap operations. We branch on the value of our [Boolean] field
     * [mIsBuiltinPixelizationChecked]:
     *
     *  * `true`: (built-in bitmap operations) We return the [BitmapDrawable] created by our method
     *  [builtInPixelization] from [Bitmap] parameter [bitmap] for a pixelization factor of our
     *  [Float] parameter [pixelizationFactor]
     *
     *  * `false`: (custom manual pixelization) We return the [BitmapDrawable] created by our method
     *  [customImagePixelization] from [Bitmap] parameter [bitmap] for a pixelization factor of our
     *  [Float] parameter [pixelizationFactor]
     *
     * @param pixelizationFactor pixelization factor to achieve.
     * @param bitmap original [Bitmap] to pixilatize.
     * @return a pixelated [BitmapDrawable] version of our [Bitmap] parameter [bitmap].
     */
    fun pixelizeImage(pixelizationFactor: Float, bitmap: Bitmap?): BitmapDrawable {
        return if (mIsBuiltinPixelizationChecked) {
            builtInPixelization(pixelizationFactor, bitmap)
        } else {
            customImagePixelization(pixelizationFactor, bitmap)
        }
    }

    /**
     * Implementation of the AsyncTask class showing how to run the
     * pixelization algorithm in the background, and retrieving the
     * pixelated image from the resulting operation.
     */
    @SuppressLint("StaticFieldLeak")
    private inner class PixelizeImageAsyncTask : AsyncTask<Any?, Void?, BitmapDrawable>() {
        /**
         * We override this method to perform a computation on a background thread. We initialize
         * [Float] variable `val pixelizationFactor` by casting `params[0]` to `Float` and [Bitmap]
         * variable `val originalBitmap` by casting `params[1]` to [Bitmap]. We then return the
         * [BitmapDrawable] returned by our method [pixelizeImage] for the arguments `pixelizationFactor`
         * and `originalBitmap` to the caller (which will then be passed to our [onPostExecute]
         * override on the UI thread).
         *
         * @param params The parameters of the task, a [Float] `pixelizationFactor` and a [Bitmap]
         * `originalBitmap`
         * @return a [BitmapDrawable] created from `originalBitmap` for a pixelization
         * factor of `pixelizationFactor`
         */
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Any?): BitmapDrawable {
            val pixelizationFactor = params[0] as Float
            val originalBitmap = params[1] as Bitmap
            return pixelizeImage(pixelizationFactor, originalBitmap)
        }

        /**
         * Runs on the UI thread after [doInBackground]. The specified result is the value
         * returned by [doInBackground]. We set the content of our [ImageView] field [mImageView]
         * to our [BitmapDrawable] parameter [result].
         *
         * @param result The result of the operation computed by [doInBackground].
         */
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: BitmapDrawable) {
            mImageView!!.setImageDrawable(result)
        }

        /**
         * Runs on the UI thread before [doInBackground]. We ignore.
         */
        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
        }

        /**
         * Runs on the UI thread after [publishProgress] is invoked. The specified values are
         * the values passed to [publishProgress]. We ignore.
         *
         * @param values The values indicating progress.
         */
        @Deprecated("Deprecated in Java")
        override fun onProgressUpdate(vararg values: Void?) {
        }
    }

    companion object {
        /**
         * Duration of the animation of the "progress" property of our [SeekBar] field [mSeekBar]
         * (10 seconds)
         */
        private const val SEEKBAR_ANIMATION_DURATION = 10_000

        /**
         * Delay between successive calls to [invokePixelization] in milliseconds.
         */
        private const val TIME_BETWEEN_TASKS = 400

        /**
         * Change in [SeekBar] position necessary before we decide to call [invokePixelization].
         */
        private const val SEEKBAR_STOP_CHANGE_DELTA = 5

        /**
         * Conversion factor between [SeekBar] position and the amount of pixelization we produce
         * (ie the pixelization is the [SeekBar] position divided by [PROGRESS_TO_PIXELIZATION_FACTOR])
         */
        private const val PROGRESS_TO_PIXELIZATION_FACTOR = 4000.0f
    }
}
