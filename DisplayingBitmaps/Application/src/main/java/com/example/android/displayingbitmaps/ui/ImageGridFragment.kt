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

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_FLING
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig
import com.example.android.displayingbitmaps.R
import com.example.android.displayingbitmaps.provider.Images
import com.example.android.displayingbitmaps.util.ImageCache
import com.example.android.displayingbitmaps.util.ImageCache.ImageCacheParams
import com.example.android.displayingbitmaps.util.ImageFetcher
import com.example.android.displayingbitmaps.util.ImageWorker
import com.example.android.displayingbitmaps.util.Utils
import kotlin.math.floor

/**
 * The main fragment that powers the [ImageGridActivity] screen. Fairly straight forward [GridView]
 * implementation with the key addition being the [ImageWorker] class w/[ImageCache] to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly if, for example, the user rotates the device.
 */
class ImageGridFragment
/**
 * Empty constructor as per the Fragment documentation
 */
    : Fragment(), OnItemClickListener {
    /**
     * Image thumbnail size in pixels (`R.dimen.image_thumbnail_size` converted to pixels)
     */
    private var mImageThumbSize: Int = 0

    /**
     * Spacing between thumbnails in our GridView in pixels (`R.dimen.image_thumbnail_spacing`
     * converted to pixels)
     */
    private var mImageThumbSpacing: Int = 0

    /**
     * [ImageAdapter] adapter which fills our [GridView]
     */
    private var mAdapter: ImageAdapter? = null

    /**
     * [ImageFetcher] used to fetch and resize images fetched from a URL.
     */
    private var mImageFetcher: ImageFetcher? = null

    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`, then we report that this fragment would like to participate in populating
     * the options menu by receiving a call to [onCreateOptionsMenu] and related methods. We
     * initialize [Int] field [mImageThumbSize] by retrieving the raw pixel dimension conversion of
     * the resource value `R.dimen.image_thumbnail_size`, and [Int] field [mImageThumbSpacing] by
     * retrieving the pixel conversion of `R.dimen.image_thumbnail_spacing`. We initialize
     * [ImageAdapter] field [mAdapter] with a new instance. We initialize [ImageCache.ImageCacheParams]
     * variable `val cacheParams` with a new instance which uses [IMAGE_CACHE_DIR] ("thumbs") as its
     * subdirectory, and set its memory cache to 25% of app memory. We initialize [ImageFetcher]
     * field [mImageFetcher] to a new instance configured to use [mImageThumbSize] as the width and
     * height of the bitmaps it decodes from the fetched images, set its loading image to
     * `R.drawable.empty_photo` (displays while image is being downloaded in the background), and
     * add to [mImageFetcher] an image cache using `cacheParams` as the cache parameters to use for
     * the image cache.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mImageThumbSize = resources.getDimensionPixelSize(R.dimen.image_thumbnail_size)
        mImageThumbSpacing = resources.getDimensionPixelSize(R.dimen.image_thumbnail_spacing)
        mAdapter = ImageAdapter(activity)
        val cacheParams = ImageCacheParams(
            context = (activity as Context),
            diskCacheDirectoryName = IMAGE_CACHE_DIR
        )
        cacheParams.setMemCacheSizePercent(fraction = 0.25f) // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = ImageFetcher(
            context = (activity as Context),
            imageSize = mImageThumbSize
        )
        mImageFetcher!!.setLoadingImage(resId = R.drawable.empty_photo)
        mImageFetcher!!.addImageCache(
            fragmentManager = requireActivity().supportFragmentManager,
            cacheParams = cacheParams
        )
    }

    /**
     * Called to have the fragment instantiate its user interface view. We initialize [View] variable
     * `val v` by using our [LayoutInflater] parameter [inflater] to inflate our layout file
     * `R.layout.image_grid_fragment`, and [GridView] variable `val mGridView` by finding the view
     * in `v` with id `R.id.gridView`. We set the adapter of `mGridView` to [ImageAdapter] field
     * [mAdapter] and its [OnItemClickListener] to `this`. We set the [AbsListView.OnScrollListener]
     * of `mGridView` to an anonymous class whose [AbsListView.OnScrollListener.onScrollStateChanged]
     * override calls the [ImageFetcher.setPauseWork] method of [ImageFetcher] field [mImageFetcher]
     * with `true` if the device is older than `honeycomb` in order to help with performance when the
     * scroll state was [AbsListView.OnScrollListener.SCROLL_STATE_FLING], otherwise it calls
     * [ImageFetcher.setPauseWork] with `false` to un-pause work if it was paused. Its
     * [AbsListView.OnScrollListener.onScroll] override does nothing.
     *
     * We fetch the [ViewTreeObserver] of `mGridView` in order to add an anonymous class
     * [OnGlobalLayoutListener] whose [OnGlobalLayoutListener.onGlobalLayout] override gets the
     * final width of the [GridView] and then calculates the number of columns and the width of each
     * column. It then sets the number of columns of [mAdapter] to that number of columns and the
     * height of an item to the width of the columns (to make a nice square thumbnail).
     *
     * Finally we return `v` to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the
     * fragment.
     * @param container If non-`null`, this is the parent view that the fragment's UI will be
     * attached to. The fragment should not add the view itself, but this can be used to generate
     * the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(
            /* resource = */ R.layout.image_grid_fragment,
            /* root = */ container,
            /* attachToRoot = */ false
        )
        val mGridView = v.findViewById<GridView>(R.id.gridView)
        mGridView.adapter = mAdapter
        mGridView.onItemClickListener = this
        mGridView.setOnScrollListener(object : AbsListView.OnScrollListener {
            /**
             * Callback method to be invoked while grid view is being scrolled. If our parameter
             * `scrollState` is SCROLL_STATE_FLING, and our device is older than honeycomb we
             * call the `setPauseWork(true)` method of `mImageFetcher` to help with
             * performance, otherwise we call `setPauseWork(false)` to resume the background
             * loading of images.
             *
             * @param absListView The view whose scroll state is being reported
             * @param scrollState The current scroll state. One of [SCROLL_STATE_TOUCH_SCROLL],
             * [SCROLL_STATE_IDLE], or [SCROLL_STATE_FLING]
             */
            override fun onScrollStateChanged(absListView: AbsListView, scrollState: Int) {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == SCROLL_STATE_FLING) {
                    // Before Honeycomb pause image loading on scroll to help with performance
                    if (!Utils.hasHoneycomb()) {
                        mImageFetcher!!.setPauseWork(true)
                    }
                } else {
                    mImageFetcher!!.setPauseWork(false)
                }
            }

            /**
             * Callback method to be invoked when the list or grid has been scrolled. We do nothing.
             *
             * @param absListView The view whose scroll state is being reported
             * @param firstVisibleItem the index of the first visible cell (ignore if
             * visibleItemCount == 0)
             * @param visibleItemCount the number of visible cells
             * @param totalItemCount the number of items in the list adaptor
             */
            override fun onScroll(
                absListView: AbsListView, firstVisibleItem: Int,
                visibleItemCount: Int, totalItemCount: Int
            ) {
            }
        })

        // This listener is used to get the final width of the GridView and then calculate the
        // number of columns and the width of each column. The width of each column is variable
        // as the GridView has stretchMode=columnWidth. The column width is used to set the height
        // of each view so we get nice square thumbnails.
        mGridView.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                /**
                 * Callback method to be invoked when the global layout state or the visibility of
                 * views within the view tree changes. If the [ImageAdapter.numColumns] property of
                 * [mAdapter] returns 0 (we have not configured it before) we calculate [Int]
                 * variable `val numColumns` by taking the floor of the width of `mGridView` divided
                 * by the sum of the thumbnail size [mImageThumbSize] and spacing [mImageThumbSpacing].
                 * If `numColumns` is greater than zero we calculate [Int] variable `val columnWidth`
                 * by dividing the width of `mGridView` by `numColumns` then subtracting the spacing
                 * [mImageThumbSpacing]. We then set the [ImageAdapter.numColumns] property of
                 * [mAdapter] to `numColumns`, and use its [ImageAdapter.setItemHeight] method to
                 * set the height of each item to `columnWidth`. Then we remove `this` as a
                 * [OnGlobalLayoutListener].
                 */
                override fun onGlobalLayout() {
                    if (mAdapter!!.numColumns == 0) {
                        val numColumns = floor(
                            (mGridView.width / (mImageThumbSize + mImageThumbSpacing))
                                .toDouble()
                        ).toInt()
                        if (numColumns > 0) {
                            val columnWidth = mGridView.width / numColumns - mImageThumbSpacing
                            mAdapter!!.numColumns = numColumns
                            mAdapter!!.setItemHeight(columnWidth)
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "onCreateView - numColumns set to $numColumns")
                            }
                            if (Utils.hasJellyBean()) {
                                mGridView.viewTreeObserver
                                    .removeOnGlobalLayoutListener(this)
                            } else {
                                mGridView.viewTreeObserver
                                    .removeGlobalOnLayoutListener(this)
                            }
                        }
                    }
                }
            })
        return v
    }

    /**
     * Called when the fragment is visible to the user and actively running. First we call our super's
     * implementation of `onResume`. Then we call the [ImageFetcher.setExitTasksEarly] method of
     * [ImageFetcher] field [mImageFetcher] with `false` to clear its "exit early" flag. Finally we
     * call the [ImageAdapter.notifyDataSetChanged] method of [ImageAdapter] field [mAdapter] to have
     * it notify attached observers that the underlying data set has been changed and any [View]
     * reflecting the data set should refresh itself.
     */
    override fun onResume() {
        super.onResume()
        mImageFetcher!!.setExitTasksEarly(false)
        mAdapter!!.notifyDataSetChanged()
    }

    /**
     * Called when the [Fragment] is no longer resumed. First we call our super's implementation of
     * `onPause`. We call the [ImageFetcher.setPauseWork] method of [ImageFetcher] field [mImageFetcher]
     * with `false` to un-pause any background work that may have been paused. We call the
     * [ImageFetcher.setExitTasksEarly] method of [ImageFetcher] field [mImageFetcher] with `true`
     * to set its "exit early" flag, and its [ImageFetcher.flushCache] method to flush the disk cache
     * to disk.
     */
    override fun onPause() {
        super.onPause()
        mImageFetcher!!.setPauseWork(false)
        mImageFetcher!!.setExitTasksEarly(true)
        mImageFetcher!!.flushCache()
    }

    /**
     * Called when the fragment is no longer in use. This is called after [onStop] and
     * before [onDetach]. First we call our super's implementation of `onDestroy`,
     * then we call the [ImageFetcher.closeCache] method of [ImageFetcher] field [mImageFetcher]
     * to close the disk cache associated with its [ImageCache] object.
     */
    override fun onDestroy() {
        super.onDestroy()
        mImageFetcher!!.closeCache()
    }

    /**
     * Callback method to be invoked when an item in our [GridView] (resource ID `R.id.gridView`)
     * has been clicked. First we initialize [Intent] variable `val i` with an [Intent] to launch
     * [ImageDetailActivity], and add an extra to it with our [Long] parameter [id] stored under the
     * key [ImageDetailActivity.EXTRA_IMAGE] ("extra_image"). If our device is at least `JELLY_BEAN`,
     * we initialize [ActivityOptions] variable `val options` to be an animation where the new
     * activity is scaled from a small originating area of the screen to its final full representation,
     * then start the activity intent `i` with `options` converted to a [Bundle] as its options. If
     * the device is older than `JELLY_BEAN` we just start the activity intent `i`.
     *
     * @param parent The AdapterView where the click happened.
     * @param v The view within the AdapterView that was clicked (this will be a view provided by
     * the adapter)
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was clicked.
     */
    override fun onItemClick(parent: AdapterView<*>?, v: View, position: Int, id: Long) {
        val i = Intent(activity, ImageDetailActivity::class.java)
        i.putExtra(ImageDetailActivity.EXTRA_IMAGE, id.toInt())
        if (Utils.hasJellyBean()) {
            // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
            // show plus the thumbnail image in GridView is cropped. so using
            // makeScaleUpAnimation() instead.
            val options = ActivityOptions.makeScaleUpAnimation(
                /* source = */ v,
                /* startX = */ 0,
                /* startY = */ 0,
                /* width = */ v.width,
                /* height = */ v.height
            )
            requireActivity().startActivity(/* intent = */ i, /* options = */ options.toBundle())
        } else {
            startActivity(/* intent = */ i)
        }
    }

    /**
     * Initialize the contents of the [Fragment] host's standard options menu. We just use our
     * [MenuInflater] parameter [inflater] to inflate our menu layout file `R.menu.main_menu` into
     * our [Menu] parameter [menu].
     *
     * @param menu The options menu in which you place your items.
     * @param inflater a [MenuInflater] you can use to inflate xml menu layout files.
     */
    @Deprecated("Replace with addMenuProvider(MenuProvider)") // TODO: Switch to MenuProvider
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We switch on the value
     * returned by the [MenuItem.getItemId] method (kotlin `itemId` property) of our [MenuItem]
     * parameter [item] and if it is `R.id.clear_cache` we call the [ImageFetcher.clearCache] method
     * of [ImageFetcher] field [mImageFetcher] which clears both the memory and disk cache associated
     * with its [ImageCache] object. We toast the message "Caches have been cleared" and return `true`
     * to the caller to signify that we consumed the event. If it is not an item that we recognise,
     * we return the value returned by our super's implementation of `onOptionsItemSelected`.
     *
     * @param item The [MenuItem] that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    @Deprecated("Deprecated in Java") // TODO: Switch to MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_cache -> {
                mImageFetcher!!.clearCache()
                Toast.makeText(
                    activity, R.string.clear_cache_complete_toast,
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * The main adapter that backs the [GridView]. This is fairly standard except the number of
     * columns in the [GridView] is used to create a fake top row of empty views as we use a
     * transparent `ActionBar` and don't want the real top row of images to start off covered by it.
     */
    private inner class ImageAdapter(
        /**
         * [Context] we were constructed with, used to create views.
         */
        private val mContext: Context?
    ) : BaseAdapter() {
        /**
         * Item height, set to the column width of our [GridView] using the [setItemHeight] method
         * by our [OnGlobalLayoutListener.onGlobalLayout] override once the dimensions of the
         * [GridView] are known.
         */
        private var mItemHeight: Int = 0

        /**
         * Number of columns in our [GridView], set using the [setItemHeight] method by our
         * [OnGlobalLayoutListener.onGlobalLayout] override once the dimensions of the [GridView]
         * are known.
         */
        var numColumns: Int = 0

        /**
         * Height of the action bar, used to create a "fake" row to prevent the action bar obscuring
         * the top of the real first row of images in our [GridView]
         */
        private var mActionBarHeight: Int = 0

        /**
         * [AbsListView.LayoutParams] for a normal image in our [GridView], set to `MATCH_PARENT`
         * for both width and height in our constructor, then set to `MATCH_PARENT` and the specified
         * [mItemHeight] by our [setItemHeight] method.
         */
        private var mImageViewLayoutParams: AbsListView.LayoutParams

        /**
         * We initialize our `LayoutParams` field `mImageViewLayoutParams` with an instance whose
         * width and height are both MATCH_PARENT. We initialize `TypedValue` variable `val tv` with
         * a new instance and if we are able to resolve a value for android.R.attr.actionBarSize
         * into it we set our field `mActionBarHeight` to the value in `tv` converted to integer
         * pixels.
         */
        init {
            mImageViewLayoutParams = AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Calculate ActionBar height
            val tv = TypedValue()
            if (context!!.theme.resolveAttribute(
                    /* resid = */ android.R.attr.actionBarSize,
                    /* outValue = */ tv,
                    /* resolveRefs = */ true
                )
            ) {
                mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                    /* data = */ tv.data,
                    /* metrics = */ context!!.resources.displayMetrics
                )
            }
        }

        /**
         * How many items are in the data set represented by this Adapter. If columns have yet to be
         * determined, we return 0 items. Otherwise we return the number of urls in the array
         * [Images.imageThumbUrls] plus the number of columns (to account for the top empty row).
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            // If columns have yet to be determined, return no items
            return if (numColumns == 0) {
                0
            } else Images.imageThumbUrls.size + numColumns
            // Size + number of columns for top empty row
        }

        /**
         * Get the data item associated with the specified position in the data set. If our [Int]
         * parameter [position] is less than [numColumns] (part of the empty row) we return `null`,
         * otherwise we return the url in the array [Images.imageThumbUrls] at index [position]
         * minus [numColumns].
         *
         * @param position Position of the item whose data we want within the adapter's
         * data set.
         * @return The data at the specified position.
         */
        override fun getItem(position: Int): Any {
            return (if (position < numColumns) null else Images.imageThumbUrls[position - numColumns])!!
        }

        /**
         * Get the row id associated with the specified position in the list. If `position` is
         * less than [numColumns] (part of the empty row) we return 0, otherwise we return
         * [position] minus [numColumns].
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            return if (position < numColumns) 0 else (position - numColumns).toLong()
        }

        /**
         * Returns the number of types of Views that will be created by [getView]. We have two
         * types of views, the normal [ImageView] and the top row of empty views so we return 2.
         *
         * @return The number of types of Views that will be created by this adapter
         */
        override fun getViewTypeCount(): Int {
            // Two types of views, the normal ImageView and the top row of empty views
            return 2
        }

        /**
         * Get the type of View that will be created by [getView] for the specified item. If [Int]
         * parameter [position] is less than [numColumns] (the top row of empty views) we return
         * 1, otherwise we return 0.
         *
         * @param position The position of the item within the adapter's data set whose view type we
         * want.
         * @return An integer representing the type of View.
         */
        override fun getItemViewType(position: Int): Int {
            return if (position < numColumns) 1 else 0
        }

        /**
         * Indicates whether the item ids are stable across changes to the underlying data. Ours are,
         * so we return `true`.
         *
         * @return `true` if the same id always refers to the same object.
         */
        override fun hasStableIds(): Boolean {
            return true
        }

        /**
         * Get a View that displays the data at the specified position in the data set. After copying
         * our [View] parameter [convertView] into [View] variable `var convertViewLocal`, we check
         * if this is the top row ([Int] parameter [position] is less than [numColumns]), and if it
         * is we set `convertViewLocal` to a new instance if it is `null`, set the layout parameters
         * of `convertViewLocal` to a new instance with the width MATCH_PARENT, and the height given
         * by [Int] field [mActionBarHeight] then we return `convertView` to the caller.
         *
         * If it is for one of the main [ImageView] thumbnails we declare [ImageView] `val imageView`,
         * and if `convertViewLocal` is `null` we initialize `imageView` with a new instance of
         * [RecyclingImageView], set its scale type to [ImageView.ScaleType.CENTER_CROP] (Scale the
         * image uniformly (maintain the image's aspect ratio) so that both dimensions (width and
         * height) of the image will be equal to or larger than the corresponding dimension of the
         * view (minus padding), the image is then centered in the view.), and set its layout
         * parameters to our [AbsListView.LayoutParams] field [mImageViewLayoutParams]. If
         * [convertView] is not `null` we set `imageView` to it. If the height of the layout
         * parameters of `imageView` is not equal to our [Int] field [mItemHeight] we set the layout
         * parameters of `imageView` to our [AbsListView.LayoutParams] field [mImageViewLayoutParams].
         * If the [ImageView.getDrawable] method of `imageView` returns `null` we call the
         * [ImageView.setImageDrawable] method of `imageView` with a new instance or [BitmapDrawable].
         * We then call the [ImageFetcher.loadImage] method of our [ImageFetcher] field [mImageFetcher]
         * to load the image whose url is at index [position] minus [numColumns] in the
         * [Images.imageThumbUrls] array asynchronously into [ImageView] variable `imageView` (this
         * also takes care of setting a placeholder image while the background thread runs). Finally
         * we return `imageView` to the caller.
         *
         * @param position The position of the item within the adapter's data set of the item whose
         * view we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this
         * view is non-`null` and of an appropriate type before using. If it is not possible to
         * convert this view to display the correct data, this method can create a new view.
         * Heterogeneous lists can specify their number of view types, so that this [View] is always
         * of the right type (see [getViewTypeCount] and [getItemViewType]).
         * @param container The parent that this view will eventually be attached to
         * @return A [View] corresponding to the data at the specified position.
         */
        override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
            // First check if this is the top row
            var convertViewLocal: View? = convertView
            if (position < numColumns) {
                if (convertViewLocal == null) {
                    convertViewLocal = View(mContext)
                }
                // Set empty view with height of ActionBar
                convertViewLocal.layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, mActionBarHeight
                )
                return convertViewLocal
            }

            // Now handle the main ImageView thumbnails
            val imageView: ImageView
            if (convertViewLocal == null) { // if it's not recycled, instantiate and initialize
                imageView = RecyclingImageView(mContext)
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP)
                imageView.setLayoutParams(mImageViewLayoutParams)
            } else { // Otherwise re-use the converted view
                imageView = convertViewLocal as ImageView
            }

            // Check the height matches our calculated column width
            if (imageView.layoutParams.height != mItemHeight) {
                imageView.layoutParams = mImageViewLayoutParams
            }

            // TODO: markgray kludge to fix nullability kotlin conversion error (argh!)
            if (imageView.drawable == null) {
                imageView.setImageDrawable(BitmapDrawable())
            }

            // Finally load the image asynchronously into the ImageView, this also takes care of
            // setting a placeholder image while the background thread runs
            mImageFetcher!!.loadImage(
                data = Images.imageThumbUrls[position - numColumns],
                imageView = imageView
            )
            return imageView
        }

        /**
         * Sets the item height. Useful for when we know the column width so the height can be set
         * to match. If our [Int] parameter [height] is already equal to our field [mItemHeight]
         * we return having done nothing. Otherwise we set [mItemHeight] to [height], and initialize
         * our [AbsListView.LayoutParams] field [mImageViewLayoutParams] with a new instance whose
         * width is `MATCH_PARENT`, and whose height is [mItemHeight]. We call the
         * [ImageFetcher.setImageSize] method of [ImageFetcher] field [mImageFetcher] to set its
         * height to [height], then call the [notifyDataSetChanged] method to notify attached
         * observers that the underlying data has been changed and any View reflecting the data set
         * should refresh itself.
         *
         * @param height height to set the item height to.
         */
        fun setItemHeight(height: Int) {
            if (height == mItemHeight) {
                return
            }
            mItemHeight = height
            mImageViewLayoutParams = AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                mItemHeight
            )
            mImageFetcher!!.setImageSize(height)
            notifyDataSetChanged()
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "ImageGridFragment"

        /**
         * subdirectory name that will be appended to the application cache directory, used to store
         * cached images.
         */
        private const val IMAGE_CACHE_DIR = "thumbs"
    }
}
