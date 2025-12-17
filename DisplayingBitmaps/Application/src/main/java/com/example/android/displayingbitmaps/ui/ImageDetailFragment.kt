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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.example.android.displayingbitmaps.R
import com.example.android.displayingbitmaps.util.ImageFetcher
import com.example.android.displayingbitmaps.util.ImageWorker
import com.example.android.displayingbitmaps.util.ImageWorker.OnImageLoadedListener
import com.example.android.displayingbitmaps.util.Utils

/**
 * This fragment will populate the children of the ViewPager from [ImageDetailActivity].
 */
class ImageDetailFragment
/**
 * Empty constructor as per the Fragment documentation
 */
    : Fragment(), OnImageLoadedListener {
    /**
     * URL we are supposed to display
     */
    private var mImageUrl: String? = null

    /**
     * [RecyclingImageView] in our layout with ID `R.id.imageView`, used to display our URL, shares
     * [FrameLayout] with [ProgressBar] field [mProgressBar] and has its visibility toggled when
     * necessary.
     */
    private var mImageView: ImageView? = null

    /**
     * [ProgressBar] in our layout with ID `R.id.progressbar`, displays progress while URL is
     * being downloaded, shares [FrameLayout] with [ImageView] field [mImageView] and has its
     * visibility toggled when necessary.
     */
    private var mProgressBar: ProgressBar? = null

    /**
     * [ImageFetcher] which downloads and caches all urls for the app
     */
    private var mImageFetcher: ImageFetcher? = null

    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`. Then if we were instantiated with arguments we set our [String] field [mImageUrl]
     * to the string stored under the key [IMAGE_DATA_EXTRA] ("extra_image_data") defaulting to `null`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mImageUrl = if (arguments != null) {
            requireArguments().getString(IMAGE_DATA_EXTRA)
        } else {
            null
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view. First we use our
     * [LayoutInflater] parameter [inflater] to inflate our layout file `R.layout.image_detail_fragment`
     * into [View] variable `val v`. Then we locate the view with id `R.id.imageView` in `v` to
     * initialize our [ImageView] field [mImageView], and the view with id `R.id.progressbar` to
     * initialize our [ProgressBar] field [mProgressBar]. Finally we return `v` to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate and locate the main ImageView
        val v: View = inflater.inflate(R.layout.image_detail_fragment, container, false)
        mImageView = v.findViewById(R.id.imageView)
        mProgressBar = v.findViewById(R.id.progressbar)
        return v
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view hierarchy
     * instantiated. First we make sure our parent activity is an [ImageDetailActivity], and if it
     * is we initialize [ImageFetcher] field [mImageFetcher] with the [ImageFetcher] returned by our
     * activities [ImageDetailActivity.imageFetcher] property, then call the [ImageFetcher.loadImage]
     * method of [mImageFetcher] to load the url given by our [String] field [mImageUrl] into our
     * [ImageView] field [mImageView], using `this` as the [ImageWorker.OnImageLoadedListener] (our
     * [onImageLoaded] override will be called when the image has finished loading). Finally if our
     * parent activity is an [View.OnClickListener] and our device is `HONEYCOMB` or newer we set
     * the [View.OnClickListener] of [mImageView] to our activity.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (ImageDetailActivity::class.java.isInstance(activity)) {
            mImageFetcher = (activity as ImageDetailActivity?)!!.imageFetcher
            mImageFetcher!!.loadImage(data = mImageUrl, imageView = mImageView!!, listener = this)
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (View.OnClickListener::class.java.isInstance(activity) && Utils.hasHoneycomb()) {
            mImageView!!.setOnClickListener(activity as View.OnClickListener?)
        }
    }

    /**
     * Called when the fragment is no longer in use. First we call our super's implementation of
     * `onDestroy`, then if our [ImageView] field [mImageView] is not `null` we cancel any pending
     * image work for [mImageView] and set the image drawable of [mImageView] to null.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(imageView = mImageView)
            mImageView!!.setImageDrawable(null)
        }
    }

    /**
     * Called by [ImageWorker] once the image has been loaded. We just set the visibility of our
     * progress bar [mProgressBar] to GONE.
     *
     * @param success `true` if the image was loaded successfully, `false` if there was an error.
     */
    override fun onImageLoaded(success: Boolean) {
        // Set loading spinner to gone once image has loaded. Could also show
        // an error view here if needed.
        mProgressBar!!.visibility = View.GONE
    }

    companion object {
        /**
         * Key in our arguments for the URL we are supposed to display.
         */
        private const val IMAGE_DATA_EXTRA = "extra_image_data"

        /**
         * Factory method to generate a new instance of the fragment given an image number. First we
         * initialize [ImageDetailFragment] variable `val f`, and [Bundle] variable `val args` with
         * new instances. We add our [String] parameter [imageUrl] as an extra to `args` under the
         * key `IMAGE_DATA_EXTRA` ("extra_image_data") and set the arguments of [ImageDetailFragment]
         * variable `f` to `args`. Finally we return `f` to the caller.
         *
         * @param imageUrl The image url to load
         * @return A new instance of [ImageDetailFragment] with [imageUrl] an extra in its arguments
         */
        fun newInstance(imageUrl: String?): ImageDetailFragment {
            val f = ImageDetailFragment()
            val args = Bundle()
            args.putString(IMAGE_DATA_EXTRA, imageUrl)
            f.arguments = args
            return f
        }
    }
}
