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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.example.android.displayingbitmaps.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig
import java.io.FileDescriptor

/**
 * A simple subclass of [ImageWorker] that resizes images from resources given a target width and
 * height. Useful for when the input images might be too large to simply load directly into memory.
 */
open class ImageResizer : ImageWorker {
    /**
     * Width of image
     */
    protected var mImageWidth: Int = 0

    /**
     * Height of image
     */
    protected var mImageHeight: Int = 0

    /**
     * Initialize providing both width and height image size. First we call our super's constructor,
     * then we call our method [setImageSize] to set our fields [mImageWidth] and [mImageHeight] to
     * our [imageWidth] and [imageHeight] parameters respectively
     *
     * @param context [Context] to use to access resources
     * @param imageWidth width of image
     * @param imageHeight height of image
     */
    constructor(context: Context?, imageWidth: Int, imageHeight: Int) : super(context!!) {
        setImageSize(imageWidth, imageHeight)
    }

    /**
     * Initialize providing a single target image size (used for both width and height). First we call
     * our super's constructor, then we call our method [setImageSize] to set our fields [mImageWidth]
     * and [mImageHeight] both to our parameter [imageSize].
     *
     * @param context [Context] to use to access resources
     * @param imageSize width and height of image
     */
    constructor(context: Context?, imageSize: Int) : super(context!!) {
        setImageSize(imageSize)
    }

    /**
     * Set the target image width and height. We set our fields [mImageWidth] and [mImageHeight]
     * to our parameters [width] and [height] respectively.
     *
     * @param width width of image
     * @param height height of image
     */
    fun setImageSize(width: Int, height: Int) {
        mImageWidth = width
        mImageHeight = height
    }

    /**
     * Set the target image size (width and height will be the same). We call our two argument method
     * [setImageSize] with both parameters set to our parameter [size].
     *
     * @param size width and height of image
     */
    fun setImageSize(size: Int) {
        setImageSize(size, size)
    }

    /**
     * The main processing method. This happens in a background task. In this case we are just
     * sampling down the bitmap and returning it from a resource. We just return the bitmap that
     * our method [decodeSampledBitmapFromResource] generates from the resource image given
     * by our [Int] parameter [resId], resized to the width [mImageWidth], and the height
     * [mImageHeight]. The recycled bitmap returned by the [ImageCache] field [imageCache] will
     * be reused if possible.
     *
     * @param resId resource ID of image
     * @return [Bitmap] decoded from resource image, and resized to [mImageWidth] by [mImageHeight].
     */
    private fun processBitmap(resId: Int): Bitmap {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - $resId")
        }
        return decodeSampledBitmapFromResource(
            res = mResources,
            resId = resId,
            reqWidth = mImageWidth,
            reqHeight = mImageHeight,
            cache = imageCache
        )
    }

    /**
     * Converts its parameter to an integer resource ID and passes the call to our method
     * [processBitmap].
     *
     * @param data The data to identify which image to process, as provided by [ImageWorker.loadImage]
     * @return [Bitmap] version of the resource image whose resource ID is the integer value of our
     * [Any] parameter [data].
     */
    override fun processBitmap(data: Any?): Bitmap? {
        return processBitmap(data.toString().toInt())
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "ImageResizer"

        /**
         * Decode and sample down a bitmap from resources to the requested width and height. We
         * initialize [BitmapFactory.Options] variable `val options` with a new instance, set its
         * [BitmapFactory.Options.inJustDecodeBounds] field to `true` then call the method
         * [BitmapFactory.decodeResource] to check the dimensions of the image. Then we set the
         * [BitmapFactory.Options.inJustDecodeBounds] field of `options` to the value calculated by
         * our method [calculateInSampleSize] when passed `options` as its [BitmapFactory.Options]
         * argument `options`, [reqWidth] as its `reqWidth` argument and [reqHeight] as its
         * `reqHeight` argument. If our device is HONEYCOMB or newer we call our method
         * [addInBitmapOptions] with `options` as its [BitmapFactory.Options] argument `options`
         * and [cache] as its [ImageCache] argument `cache` to modify `options` to try to use
         * [BitmapFactory.Options.inBitmap] to recycle a [Bitmap] from [ImageCache] parameter
         * [cache]. We then set the [BitmapFactory.Options.inJustDecodeBounds] field of `options`
         * to `false` and return the [Bitmap] generated by the [BitmapFactory.decodeResource] method.
         *
         * @param res The [Resources] object containing the image data
         * @param resId The resource id of the image data
         * @param reqWidth The requested width of the resulting [Bitmap]
         * @param reqHeight The requested height of the resulting [Bitmap]
         * @param cache The [ImageCache] used to find candidate bitmaps for use with
         * [BitmapFactory.Options.inBitmap]
         * @return A [Bitmap] sampled down from the original with the same aspect ratio and
         * dimensions that are equal to or greater than the requested width and height
         */
        fun decodeSampledBitmapFromResource(
            res: Resources?,
            resId: Int,
            reqWidth: Int,
            reqHeight: Int,
            cache: ImageCache?
        ): Bitmap {

            // BEGIN_INCLUDE (read_bitmap_dimensions)
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(
                options = options,
                reqWidth = reqWidth,
                reqHeight = reqHeight
            )
            // END_INCLUDE (read_bitmap_dimensions)

            // If we're running on Honeycomb or newer, try to use inBitmap
            if (Utils.hasHoneycomb()) {
                addInBitmapOptions(options = options, cache = cache)
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeResource(res, resId, options)
        }

        /**
         * Decode and sample down a bitmap from a file to the requested width and height. We
         * initialize [BitmapFactory.Options] variable `val options` with a new instance, set its
         * [BitmapFactory.Options.inJustDecodeBounds] field to `true` then call the method
         * [BitmapFactory.decodeFile] with our [String] parameter [filename] as its [String]
         * argument `pathName`, and `options` as its [BitmapFactory.Options] argument `options` to
         * check the dimensions of the image. Then we set the [BitmapFactory.Options.inSampleSize]
         * field of `options` to the value calculated by our method [calculateInSampleSize] when
         * passed `options` as its [BitmapFactory.Options] argument `options`, [reqWidth] as its
         * `reqWidth` argument and [reqHeight] as its `reqHeight` argument. If our device is
         * HONEYCOMB or newer we call our method [addInBitmapOptions] with `options` as its
         * [BitmapFactory.Options] argument `options` and [cache] as its [ImageCache] argument
         * `cache` to modify `options` to try to use [BitmapFactory.Options.inBitmap] to recycle a
         * [Bitmap] from [ImageCache] parameter [cache]. We then set the
         * [BitmapFactory.Options.inJustDecodeBounds] field of `options` to `false` and return the
         * [Bitmap] generated by the [BitmapFactory.decodeFile] method when passed our [String]
         * parameter [filename] as its [String] argument `pathName`, and `options` as its
         * [BitmapFactory.Options] argument `options`.
         *
         * @param filename The full path of the file to decode
         * @param reqWidth The requested width of the resulting [Bitmap]
         * @param reqHeight The requested height of the resulting [Bitmap]
         * @param cache The [ImageCache] used to find candidate bitmaps for use with
         * [BitmapFactory.Options.inBitmap]
         * @return A [Bitmap] sampled down from the original with the same aspect ratio and
         * dimensions that are equal to or greater than the requested width and height.
         */
        fun decodeSampledBitmapFromFile(
            filename: String?,
            reqWidth: Int,
            reqHeight: Int,
            cache: ImageCache?
        ): Bitmap {

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filename, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // If we're running on Honeycomb or newer, try to use inBitmap
            if (Utils.hasHoneycomb()) {
                addInBitmapOptions(options, cache)
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeFile(filename, options)
        }

        /**
         * Decode and sample down a bitmap from a file input stream to the requested width and
         * height. We initialize [BitmapFactory.Options] variable `val options` with a new instance,
         * set its [BitmapFactory.Options.inJustDecodeBounds] field to `true` then call the method
         * [BitmapFactory.decodeFileDescriptor] with our [FileDescriptor] parameter [fileDescriptor]
         * as its [FileDescriptor] argument `fd`, `null` as its [Rect] parameter `outPadding` and
         * `options` as its [BitmapFactory.Options] argument `opts` to check the dimensions of the
         * image. Then we set the [BitmapFactory.Options.inSampleSize] field of `options` to
         * the value calculated by our method [calculateInSampleSize] when passed `options` as its
         * [BitmapFactory.Options] argument `options`, [reqWidth] as its `reqWidth` argument and
         * [reqHeight] as its `reqHeight` argument. If our device is HONEYCOMB or newer we call our
         * method [addInBitmapOptions] with `options` as its [BitmapFactory.Options] argument
         * `options` and [cache] as its [ImageCache] argument `cache` to modify `options` to try to
         * use [BitmapFactory.Options.inBitmap] to recycle a [Bitmap] from [ImageCache] parameter
         * [cache]. We then set the [BitmapFactory.Options.inJustDecodeBounds] field of `options` to
         * `false` and return the [Bitmap] generated by the [BitmapFactory.decodeFileDescriptor]
         * method when passed our [FileDescriptor] parameter [fileDescriptor] as its [FileDescriptor]
         * argument `fd`, `null` as its [Rect] parameter `outPadding` and `options` as its
         * [BitmapFactory.Options] argument `opts`.
         *
         * @param fileDescriptor The [FileDescriptor] to read from
         * @param reqWidth The requested width of the resulting [Bitmap]
         * @param reqHeight The requested height of the resulting [Bitmap]
         * @param cache The [ImageCache] used to find candidate bitmaps for use with
         * [BitmapFactory.Options.inBitmap]
         * @return A [Bitmap] sampled down from the original with the same aspect ratio and
         * dimensions that are equal to or greater than the requested width and height
         */
        fun decodeSampledBitmapFromDescriptor(
            fileDescriptor: FileDescriptor?,
            reqWidth: Int,
            reqHeight: Int,
            cache: ImageCache?
        ): Bitmap {

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // If we're running on Honeycomb or newer, try to use inBitmap
            if (Utils.hasHoneycomb()) {
                addInBitmapOptions(options, cache)
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
        }

        /**
         * Modifies its [BitmapFactory.Options] parameter [options] to try to reuse a bitmap fetched
         * from its [ImageCache] parameter [cache]. First we set the [BitmapFactory.Options.inMutable]
         * field of [options] to `true` to force the decoder to return mutable bitmaps. If [cache]
         * is not `null` we initialize [Bitmap] variable `val inBitmap` with the [Bitmap] returned
         * by the [ImageCache.getBitmapFromReusableSet] method of [cache] when passed [options] as
         * its [BitmapFactory.Options] argument `options`. If `inBitmap` is not `null` we set the
         * [BitmapFactory.Options.inBitmap] field of [options] to `inBitmap`.
         *
         * @param options [BitmapFactory.Options] we are to modify
         * @param cache [ImageCache] we are to try to fetch reusable bitmaps from.
         */
        @SuppressLint("ObsoleteSdkInt")
        @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
        private fun addInBitmapOptions(options: BitmapFactory.Options, cache: ImageCache?) {
            //BEGIN_INCLUDE(add_bitmap_options)
            // inBitmap only works with mutable bitmaps so force the decoder to
            // return mutable bitmaps.
            options.inMutable = true
            if (cache != null) {
                // Try and find a bitmap to use for inBitmap
                val inBitmap = cache.getBitmapFromReusableSet(options = options)
                if (inBitmap != null) {
                    options.inBitmap = inBitmap
                }
            }
            //END_INCLUDE(add_bitmap_options)
        }

        /**
         * Calculate an [BitmapFactory.Options.inSampleSize] for use in a [BitmapFactory.Options]
         * object when decoding bitmaps using the decode* methods from [BitmapFactory]. This
         * implementation calculates the closest [BitmapFactory.Options.inSampleSize] that is a
         * power of 2 and will result in the final decoded [Bitmap] having a width and height equal
         * to or larger than the requested width and height.
         *
         * We initialize [Int] variable `val height` to the [BitmapFactory.Options.outHeight] field
         * of [BitmapFactory.Options] parameter [options], [Int] variable `val width` to the
         * [BitmapFactory.Options.outWidth] field of [options], and initialize [Int] variable
         * `var inSampleSize` to 1. Then if `height` is greater than our [Int] parameter [reqHeight]
         * or `width` is greater than [Int] parameter [reqWidth] we need to scale the image. We
         * initialize [Int] variable `val halfHeight` to be half of `height` and [Int] variable
         * `val halfWidth` to be half of `width`. Then we loop while the value
         * `halfHeight/inSampleSize` is greater than [reqHeight] and `halfWidth/inSampleSize`
         * is greater than [reqWidth] doubling `inSampleSize` (this calculates the largest
         * `inSampleSize` value that is a power of 2 and keeps both height and width larger than
         * the requested height and width).
         *
         * Then we add some additional logic for strange aspect ratios. We initialize [Long] variable
         * `var totalPixels` to be `width*height/inSampleSize`, and initialize [Long] variable
         * `val totalReqPixelsCap` to be [reqWidth] times [reqHeight] times 2. Then while
         * `totalPixels` is greater than `totalReqPixelsCap` we multiply `inSampleSize` by 2 and
         * divide `totalPixels` by two (anything more than 2x the requested pixels we'll sample down
         * further).
         *
         * Finally we return `inSampleSize` to the caller (it is always 1 in our case -- there being
         * no need for the scaling calculation).
         *
         * @param options An [BitmapFactory.Options] object with out* params already populated (run
         * through a decode* method with [BitmapFactory.Options.inJustDecodeBounds] set to `true`
         * @param reqWidth The requested width of the resulting [Bitmap]
         * @param reqHeight The requested height of the resulting [Bitmap]
         * @return The value to be used for [BitmapFactory.Options.inSampleSize]
         */
        fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            // BEGIN_INCLUDE (calculate_sample_size)
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth) {
                    inSampleSize *= 2
                }

                // This offers some additional logic in case the image has a strange
                // aspect ratio. For example, a panorama may have a much larger
                // width than height. In these cases the total pixels might still
                // end up being too large to fit comfortably in memory, so we should
                // be more aggressive with sample down the image (=larger inSampleSize).
                var totalPixels: Long = (width * height / inSampleSize).toLong()

                // Anything more than 2x the requested pixels we'll sample down further
                val totalReqPixelsCap: Long = (reqWidth * reqHeight * 2).toLong()
                while (totalPixels > totalReqPixelsCap) {
                    inSampleSize *= 2
                    totalPixels /= 2
                }
            }
            return inSampleSize
            // END_INCLUDE (calculate_sample_size)
        }
    }
}