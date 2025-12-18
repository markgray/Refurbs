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
package com.example.android.displayingbitmaps.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig

/**
 * A [BitmapDrawable] that keeps track of whether it is being displayed or cached. When the drawable
 * is no longer being displayed or cached, [recycle()][Bitmap.recycle] will be
 * called on this drawable's bitmap.
 */
class RecyclingBitmapDrawable
/**
 * Our constructor, we just call our super's constructor.
 *
 * @param res used to access resources for configuration purposes
 * @param bitmap [Bitmap] used by this drawable to render.
 */
(res: Resources?, bitmap: Bitmap?) : BitmapDrawable(res, bitmap) {
    /**
     * Cache reference count, incremented by our [setIsCached] method when called with `true`,
     * decremented by our [setIsCached] method when called with `false`, checked for 0 (or lower)
     * in [checkState] method as part of decision to recycle our bitmap.
     */
    private var mCacheRefCount: Int = 0

    /**
     * Display reference count, incremented by our [setIsDisplayed] method when called with `true`,
     * decremented by our [setIsDisplayed] method when called with `false`, checked for 0 (or lower)
     * in [checkState] method as part of decision to recycle our bitmap.
     */
    private var mDisplayRefCount: Int = 0

    /**
     * Flag indicating that we have been displayed, set to `true` in our [setIsDisplayed] method
     */
    private var mHasBeenDisplayed: Boolean = false

    /**
     * Notify the drawable that the displayed state has changed. Internally a count is kept so that
     * the drawable knows when it is no longer being displayed. Synchronized on this we branch on
     * the value of our [Boolean] parameter [isDisplayed]:
     *
     *  * `true`: we increment [Int] field [mDisplayRefCount] and set [Boolean] field
     *  [mHasBeenDisplayed] to `true`
     *
     *  * `false`: we decrement [Int] field [mDisplayRefCount]
     *
     * Finally we call our method [checkState] to see if the [Bitmap.recycle] method of our [Bitmap]
     * can be called.
     *
     * @param isDisplayed - Whether the drawable is being displayed or not
     */
    fun setIsDisplayed(isDisplayed: Boolean) {
        synchronized(this) {
            if (isDisplayed) {
                mDisplayRefCount++
                mHasBeenDisplayed = true
            } else {
                mDisplayRefCount--
            }
        }

        // Check to see if recycle() can be called
        checkState()
    }

    /**
     * Notify the drawable that the cache state has changed. Internally a count is kept so that the
     * drawable knows when it is no longer being cached. Synchronized on `this` we branch on the
     * value of our [Boolean] parameter [isCached]:
     *
     *  * `true`: we increment [Int] field [mCacheRefCount]
     *  * false: we decrement [Int] field [mCacheRefCount]
     *
     * Finally we call our method [checkState] to see if the [Bitmap.recycle] method of our [Bitmap]
     * can be called.
     *
     * @param isCached - Whether the drawable is being cached or not
     */
    fun setIsCached(isCached: Boolean) {
        synchronized(this) {
            if (isCached) {
                mCacheRefCount++
            } else {
                mCacheRefCount--
            }
        }

        // Check to see if recycle() can be called
        checkState()
    }

    /**
     * Checks to see if the [Bitmap.recycle] method can be called to recycle our bitmap. If the
     * drawable cache and display ref counts are less than or equal to 0, and this drawable has
     * been displayed, then we fetch the [Bitmap] used by this drawable to render and call its
     * [Bitmap.recycle] method to free the native object associated with the [Bitmap], and clear
     * the reference to the pixel data.
     */
    @Synchronized
    private fun checkState() {
        // If the drawable cache and display ref counts = 0, and this drawable
        // has been displayed, then recycle
        if (mCacheRefCount <= 0 && mDisplayRefCount <= 0 && mHasBeenDisplayed
            && hasValidBitmap()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "No longer being used or cached so recycling. "
                    + toString())
            }
            bitmap.recycle()
        }
    }

    /**
     * Returns `true` if we have a valid [Bitmap] to use for rendering. We initialize [Bitmap]
     * variable `val bitmap` with the [Bitmap] used by this drawable to render and if it is not
     * `null` and it has not been recycled we return `true`, otherwise we return `false`.
     *
     * @return `true` if we have a valid bitmap to use for rendering.
     */
    @Synchronized
    private fun hasValidBitmap(): Boolean {
        val bitmap: Bitmap? = bitmap
        return bitmap != null && !bitmap.isRecycled
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "CountingBitmapDrawable"
    }
}