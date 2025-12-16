/*
 * Copyright (C) 2012 The Android Open Source Project
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
@file:Suppress("DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.displayingbitmaps.ui

import android.app.ActionBar
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.android.displayingbitmaps.BuildConfig
import com.example.android.displayingbitmaps.R
import com.example.android.displayingbitmaps.provider.Images
import com.example.android.displayingbitmaps.util.ImageCache
import com.example.android.displayingbitmaps.util.ImageCache.ImageCacheParams
import com.example.android.displayingbitmaps.util.ImageFetcher
import com.example.android.displayingbitmaps.util.Utils

/**
 * [FragmentActivity] to display fullscreen image when a thumbnail in [ImageGridActivity] is clicked
 */
class ImageDetailActivity : FragmentActivity(), View.OnClickListener {
    /**
     * adapter that backs the [ViewPager], creating new [ImageDetailFragment] as it is scrolled
     */
    private var mAdapter: ImagePagerAdapter? = null

    /**
     * Singleton [ImageFetcher] called by the [ViewPager] child fragments to load images via this
     * one [ImageFetcher].
     */
    var imageFetcher: ImageFetcher? = null
        private set

    /**
     * [ViewPager] with ID `R.id.pager` in our layout file.
     */
    private var mPager: ViewPager? = null

    /**
     * TODO: Continue here.
     * Called when the activity is starting. First we check if the gradle generated constant
     * [BuildConfig.DEBUG] is true, and if it is we call the [Utils.enableStrictMode] method to set
     * the policy for what potentially suspect actions should be detected and logged using
     * [android.os.StrictMode]. Then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.image_detail_pager`.
     *
     * We initialize [DisplayMetrics] variable `val displayMetrics` with a new instance, and load it
     * with the display metrics that describe the size and density of our display. We then initialize
     * our [Int] variable `val height` with the [DisplayMetrics.heightPixels] field of `displayMetrics`, and
     * and [Int] variable `val width` with its [DisplayMetrics.widthPixels] field. We initialize our
     * [Int] variable `val longest` with half of the longest of `width` and `height`.
     *
     * We create [ImageCacheParams] variable `val cacheParams` to use [IMAGE_CACHE_DIR] as its
     * directory, and set its memory cache to 25% of app memory. We initialize our [ImageFetcher]
     * field [imageFetcher] with an instance using `longest` as both its width and height, add an
     * image cache to it using `cacheParams` as the configuration for the cache, then disable
     * fade-in of the image once it has been fetched.
     *
     * We initialize our [ImagePagerAdapter] field [mAdapter] to have as many pages as there are
     * urls in the array [Images.imageUrls], initialize our [ViewPager] field [mPager] by finding
     * the view with id `R.id.pager`, set its adapter to [mAdapter], set its page margin to the
     * value of the resource `R.dimen.horizontal_page_margin` (16 default, 64 for sw600dp), and
     * set its off screen page limit to 2.
     *
     * We set the [WindowManager.LayoutParams.FLAG_FULLSCREEN] flag of the current window for the
     * activity (hides all screen decorations (such as the status bar) while the window is displayed).
     * Then if the device is at least `HONEYCOMB` we initialize [ActionBar] variable `val actionBar`
     * with a reference to our activity's [ActionBar], disable the display of the activity
     * title/subtitle, and enable displaying "Home" as "Up" (selecting home will return one level
     * up rather than to the top level of the app).
     *
     * Next we set the [OnSystemUiVisibilityChangeListener] of [ViewPager] field [mPager] to an
     * anonymous class which hides [ActionBar] variable `actionBar` if the visibility has changed
     * to [View.SYSTEM_UI_FLAG_LOW_PROFILE], and shows it if not. We then set the system UI visibility
     * to [View.SYSTEM_UI_FLAG_LOW_PROFILE], and hide `actionBar`.
     *
     * Whether the device is HONEYCOMB of earlier we initialize [Int] variable `val extraCurrentItem`
     * with the [Int] stored as an extra in our Intent under the key [EXTRA_IMAGE] (defaulting to -1),
     * and if it is not -1 we call the [ViewPager.setCurrentItem] method of [mPager] (aka kotlin
     * `currentItem` property) to set its current item to `extraCurrentItem`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            Utils.enableStrictMode()
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_detail_pager)

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height: Int = displayMetrics.heightPixels
        val width: Int = displayMetrics.widthPixels

        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        val longest: Int = (if (height > width) height else width) / 2
        val cacheParams = ImageCacheParams(this, IMAGE_CACHE_DIR)
        cacheParams.setMemCacheSizePercent(0.25f) // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        imageFetcher = ImageFetcher(this, longest)
        imageFetcher!!.addImageCache(supportFragmentManager, cacheParams)
        imageFetcher!!.setImageFadeIn(false)

        // Set up ViewPager and backing adapter
        mAdapter = ImagePagerAdapter(supportFragmentManager, Images.imageUrls.size)
        mPager = findViewById(R.id.pager)
        mPager!!.adapter = mAdapter
        mPager!!.pageMargin = resources.getDimension(R.dimen.horizontal_page_margin).toInt()
        mPager!!.offscreenPageLimit = 2

        // Set up activity to go full screen
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        if (Utils.hasHoneycomb()) {
            val actionBar: ActionBar = actionBar!!
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)

            // Hide and show the ActionBar as the visibility changes
            mPager!!.setOnSystemUiVisibilityChangeListener { vis ->
                if (vis and View.SYSTEM_UI_FLAG_LOW_PROFILE != 0) {
                    actionBar.hide()
                } else {
                    actionBar.show()
                }
            }

            // Start low profile mode and hide ActionBar
            mPager!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
            actionBar.hide()
        }

        // Set the current item based on the extra passed in to this activity
        val extraCurrentItem: Int = intent.getIntExtra(EXTRA_IMAGE, -1)
        if (extraCurrentItem != -1) {
            mPager!!.currentItem = extraCurrentItem
        }
    }

    /**
     * Called after [onRestoreInstanceState], [onRestart], or [onPause], for our activity to start
     * interacting with the user. First we call our super's implementation of `onResume`, then we
     * call the  method of our [ImageFetcher] field [imageFetcher]
     * with `false` to clear its exit tasks early flag.
     */
    public override fun onResume() {
        super.onResume()
        imageFetcher!!.setExitTasksEarly(false)
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but
     * has not (yet) been killed. First we call our super's implementation of `onPause`, then we
     * call the [ImageFetcher.setExitTasksEarly] method of our [ImageFetcher] field [imageFetcher]
     * to set its exit tasks early flag, and its [ImageFetcher.flushCache] method to flush its cache
     * files to disk.
     */
    override fun onPause() {
        super.onPause()
        imageFetcher!!.setExitTasksEarly(true)
        imageFetcher!!.flushCache()
    }

    /**
     * Perform any final cleanup before an activity is destroyed. First we call our super's
     * implementation of `onDestroy` then we call the [ImageFetcher.closeCache] method of our
     * [ImageFetcher] field [imageFetcher] to close the disk cache associated with its [ImageCache]
     * object.
     */
    override fun onDestroy() {
        super.onDestroy()
        imageFetcher!!.closeCache()
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We switch on the id of
     * [MenuItem] parameter [item]:
     *
     *  * [android.R.id.home]: We call the [NavUtils.navigateUpFromSameTask] method to navigate up
     *  to the activity which launched us, and return `true` to the caller consuming the event.
     *
     *  * `R.id.clear_cache`: We call the [ImageFetcher.clearCache] method of [ImageFetcher] field
     *  [imageFetcher] to clear both the memory and disk cache associated with its [ImageCache]
     *  object, then we toast the message "Caches have been cleared", and return `true` to the caller
     *  thereby consuming the event.
     *
     * If the id is not one we are interested in we just return the value returned by our super's
     * implementation of `onOptionsItemSelected`.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to proceed, `true` to
     * consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }

            R.id.clear_cache -> {
                imageFetcher!!.clearCache()
                Toast.makeText(
                    this, R.string.clear_cache_complete_toast, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We fetch a [MenuInflater]
     * for our context and use it to inflate our menu layout file `R.menu.main_menu` into [Menu]
     * parameter [menu]. We then return `true` to the caller so that the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed;
     * if you return `false` it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * The main adapter that backs the [ViewPager]. A subclass of [FragmentStatePagerAdapter] as
     * there could be a large number of items in the [ViewPager] and we don't want to retain them
     * all in memory at once but create/destroy them on the fly.
     *
     * @param fm   [FragmentManager] for interacting with fragments associated with our activity
     * @param mSize size of the array of urls [Images.imageUrls]
     */
    private class ImagePagerAdapter(
        fm: FragmentManager?,
        val mSize: Int
    ) : FragmentStatePagerAdapter(fm!!) {
        /**
         * Returns the number of views available, which is the value our constructor stored in our
         * field [mSize].
         *
         * @return the number of views available.
         */
        override fun getCount(): Int {
            return mSize
        }

        /**
         * Return the [Fragment] associated with a specified position. We return a new instance of
         * [ImageDetailFragment] constructed to display the image found using the url whose index
         * is our [Int] parameter [position] in the array [Images.imageUrls].
         *
         * @param position position we want
         * @return [ImageDetailFragment] constructed to display the image found using the url
         * at index [position] in the [Images.imageUrls] array.
         */
        override fun getItem(position: Int): Fragment {
            return ImageDetailFragment.newInstance(Images.imageUrls[position])
        }
    }

    /**
     * Set on the [ImageView] in the [ViewPager] children fragments, to enable/disable low profile
     * mode when the [ImageView] is touched. We initialize [Int] variable `val vis` with the system
     * UI visibility mask, and if its [View.SYSTEM_UI_FLAG_LOW_PROFILE] bit is set we set the
     * [View.SYSTEM_UI_FLAG_VISIBLE] flag of [mPager], otherwise we set its
     * [View.SYSTEM_UI_FLAG_LOW_PROFILE] flag.
     *
     * @param v View that was clicked
     */
    override fun onClick(v: View) {
        val vis: Int = mPager!!.systemUiVisibility
        if (vis and View.SYSTEM_UI_FLAG_LOW_PROFILE != 0) {
            mPager!!.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else {
            mPager!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
    }

    companion object {
        /**
         * Subdirectory to store our cached bitmaps in
         */
        private const val IMAGE_CACHE_DIR = "images"

        /**
         * Key used to store the ID of the currently selected image in `Intent`
         */
        const val EXTRA_IMAGE: String = "extra_image"
    }
}
