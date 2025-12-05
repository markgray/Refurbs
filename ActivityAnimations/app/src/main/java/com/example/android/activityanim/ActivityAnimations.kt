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
    "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate"
)

package com.example.android.activityanim

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.activityanim.ActivityAnimations.Companion.PACKAGE
import com.example.android.activityanim.ActivityAnimations.Companion.sAnimatorScale

/**
 * This example shows how to create a custom activity animation when you want something more
 * than window animations can provide. The idea is to disable window animations for the
 * activities and to instead launch or return from the sub-activity immediately, but use
 * property animations inside the activities to customize the transition.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * https://www.youtube.com/watch?v=CPxkoe2MraA
 */
class ActivityAnimations : AppCompatActivity() {
    /**
     * [GridLayout] in our layout file layout/activity_animations.xml with the id `R.id.gridLayout`,
     * it is the only view in that layout file and is used to display our thumbnails.
     */
    var mGridLayout: GridLayout? = null

    /**
     * [HashMap] which contains a [PictureData] object for each of our [ImageView]
     * thumbnails displayed in our [GridLayout]
     */
    var mPicturesData: HashMap<ImageView, PictureData> = HashMap()

    /**
     * Instance  of [BitmapUtils] which we use for its [BitmapUtils.loadPhotos] method.
     */
    var mBitmapUtils: BitmapUtils = BitmapUtils()

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display then we call our super's implementation of `onCreate` and set our content view
     * to our layout file `R.layout.activity_animations`. We initialize our [ColorMatrix] variable
     * `grayMatrix` to a new instance, set its saturation to 0 (0 maps the color to gray-scale), and
     * create [ColorMatrixColorFilter] variable `grayscaleFilter` from it. We initialize [GridLayout]
     * property [mGridLayout] by finding the view with id `R.id.gridLayout`.
     *
     * Then we call [ViewCompat.setOnApplyWindowInsetsListener] to take over
     * the policy for applying window insets to [mGridLayout], with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using `WindowInsetsCompat.Type.ime()`. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We set the column count of [mGridLayout] to 3, and enable its use of default margins around
     * children based on the child's visual characteristics. We initialize [Resources] variable
     * `resources` with an instance for our application's package, and initialize [ArrayList] of
     * [PictureData] variable `pictures` with the random list generated by the [BitmapUtils.loadPhotos]
     * method of [BitmapUtils] property [mBitmapUtils] when passed `resources` as its argument. We
     * loop over [Int] variable `i` for all the [PictureData] objects in `pictures` and for each:
     *
     *  * We initialize [PictureData] variable `pictureData` with the [PictureData] in position
     *  `i` of `pictures`.
     *
     *  * We create [BitmapDrawable] variable `thumbnailDrawable` from the [Bitmap] in the
     *  [PictureData.thumbnail] field of `pictureData`, and set its color filter to `grayscaleFilter`.
     *
     *  * We initialize [ImageView] variable `imageView` with a new instance, set its
     *  [OnClickListener] to [OnClickListener] property [thumbnailClickListener], and set its
     *  content to `thumbnailDrawable`
     *
     *  * We store `pictureData` in [HashMap] of [ImageView] to [PictureData] property
     *  [mPicturesData] under the key `imageView`.
     *
     *  * Finally we add `imageView` to [GridLayout] property [mGridLayout] then loop around for the
     *  next [PictureData] in `pictures`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animations)

        // Grayscale filter used on all thumbnails
        val grayMatrix = ColorMatrix()
        grayMatrix.setSaturation(0f)
        val grayscaleFilter = ColorMatrixColorFilter(grayMatrix)
        mGridLayout = findViewById(R.id.gridLayout)
        ViewCompat.setOnApplyWindowInsetsListener(mGridLayout!!) { v: View, windowInsets: WindowInsetsCompat ->
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
        mGridLayout!!.columnCount = 3
        mGridLayout!!.useDefaultMargins = true

        // add all photo thumbnails to layout
        val resources: Resources = resources
        val pictures: ArrayList<PictureData> = mBitmapUtils.loadPhotos(resources)
        for (i in pictures.indices) {
            val pictureData: PictureData = pictures[i]
            val thumbnailDrawable: BitmapDrawable = pictureData.thumbnail.toDrawable(resources)
            thumbnailDrawable.colorFilter = grayscaleFilter
            val imageView = ImageView(this)
            imageView.setOnClickListener(thumbnailClickListener)
            imageView.setImageDrawable(thumbnailDrawable)
            mPicturesData[imageView] = pictureData
            mGridLayout!!.addView(imageView)
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a [MenuInflater]
     * for this context to inflate our menu layout file `R.menu.activity_better_window_animations`
     * into our [Menu] parameter [menu] and return `true` so that the menu will be displayed.
     *
     * @param menu The options menu in which we place our items.
     * @return You must return `true` for the menu to be displayed; if you return `false` it will
     * not be shown. We return `true`.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_better_window_animations, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the item id of our
     * [MenuItem] parameter [item] is `R.id.menu_slow`, we set our [Float] property [sAnimatorScale]
     * to 1 if the item is currently checked, or to 5 if it unchecked then toggle the checked state
     * of the item. In any case we return the value returned by our super's implementation of
     * `onOptionsItemSelected` to the caller.
     *
     * @param item The menu item that was selected.
     * @return Return `false` to allow normal menu processing to proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_slow) {
            sAnimatorScale = (if (item.isChecked) 1 else 5).toFloat()
            item.isChecked = !item.isChecked
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Called when a view has been clicked. When the user clicks a thumbnail, we bundle up
     * information about it and launch the details activity. We capture the [View] passed us in
     * variable `v`, then we allocate 2 ints to initialize [IntArray] variable `screenLocation`,
     * and call the [View.getLocationOnScreen] method of our [View] variable `v` to load it with the
     * coordinates of this view on the screen. We initialize [PictureData] variable `info` with the
     * object stored under the key `v` in our [HashMap] of [ImageView] to [PictureData] property
     * [mPicturesData]. We initialize [Intent] variable `subActivity` with an [Intent] intended to
     * launch the activity [PictureDetailsActivity], and initialize [Int] variable `orientation` with
     * the [Configuration.orientation] property of the current configuration that is in effect for a
     * [Resources] instance for our application's package. We then proceed to add extras to
     * [Intent] variable `subActivity`, using keys formed by concatenating to our [PACKAGE] name
     * the strings:
     *
     *  * [PACKAGE].orientation holds the value of `orientation`
     *
     *  * [PACKAGE].resourceId holds the value of the [PictureData.resourceId] property of `info`
     *
     *  * [PACKAGE].left holds the value of `screenLocation[0]`, the X coordinate of `v`
     *
     *  * [PACKAGE].top holds the value of `screenLocation[1]`, the Y coordinate of `v`
     *
     *  * [PACKAGE].width holds the width of `v`
     *
     *  * [PACKAGE].height holds the height of `v`
     *
     *  * [PACKAGE].description" hold the value of the [PictureData.description] property of `info`
     *
     * We then start `subActivity` running, and call the [overridePendingTransition] method (using
     * different overloads for different API levels) to cancel the normal window animation so only
     * our custom one is used.
     *
     * param v The view that was clicked.
     */
    private val thumbnailClickListener = OnClickListener { v: View ->
        // Interesting data to pass across are the thumbnail size/location, the
        // resourceId of the source bitmap, the picture description, and the
        // orientation (to avoid returning back to an obsolete configuration if
        // the device rotates again in the meantime)
        val screenLocation = IntArray(size = 2)
        v.getLocationOnScreen(screenLocation)
        val info: PictureData? = mPicturesData[v]
        val subActivity = Intent(
            this@ActivityAnimations,
            PictureDetailsActivity::class.java
        )
        val orientation: Int = resources.configuration.orientation
        subActivity.putExtra("$PACKAGE.orientation", orientation)
            .putExtra("$PACKAGE.resourceId", info!!.resourceId)
            .putExtra("$PACKAGE.left", screenLocation[0])
            .putExtra("$PACKAGE.top", screenLocation[1])
            .putExtra("$PACKAGE.width", v.width)
            .putExtra("$PACKAGE.height", v.height)
            .putExtra("$PACKAGE.description", info.description)
        startActivity(subActivity)

        // Override transitions: we don't want the normal window animation in addition
        // to our custom one
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION") // Needed for SDK older than 34
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        /**
         * Package prefix used when adding extras to the [Intent] that launches the activity
         * [PictureDetailsActivity]
         */
        private const val PACKAGE = "com.example.android.activityanim"

        /**
         * Amount to multiply the duration of the animation by, toggled between 1 and 5 in the
         * `onOptionsItemSelected` override if the item id is `R.id.menu_slow` ("Slow")
         */
        @JvmField
        var sAnimatorScale: Float = 1f
    }
}