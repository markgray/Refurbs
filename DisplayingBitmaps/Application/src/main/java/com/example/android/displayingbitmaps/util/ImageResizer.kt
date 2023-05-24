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
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig
import java.io.FileDescriptor

/**
 * A simple subclass of [ImageWorker] that resizes images from resources given a target width
 * and height. Useful for when the input images might be too large to simply load directly into
 * memory.
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
     * then we call our method `setImageSize` to set our fields `mImageWidth` and
     * `mImageHeight` to our parameters of the same name.
     *
     * @param context `Context` to use to access resources
     * @param imageWidth width of image
     * @param imageHeight height of image
     */
    constructor(context: Context?, imageWidth: Int, imageHeight: Int) : super(context!!) {
        setImageSize(imageWidth, imageHeight)
    }

    /**
     * Initialize providing a single target image size (used for both width and height). First we call
     * our super's constructor, then we call our method `setImageSize` to set our fields {
     * @code mImageWidth} and `mImageHeight` both to our parameter `imageSize`.
     *
     * @param context `Context` to use to access resources
     * @param imageSize width and height of image
     */
    constructor(context: Context?, imageSize: Int) : super(context!!) {
        setImageSize(imageSize)
    }

    /**
     * Set the target image width and height. We set our fields `mImageWidth` and `mImageHeight`
     * to our parameters `width` and `height` respectively.
     *
     * @param width width of image
     * @param height height of image
     */
    fun setImageSize(width: Int, height: Int) {
        mImageWidth = width
        mImageHeight = height
    }

    /**
     * Set the target image size (width and height will be the same). We call our method
     * `setImageSize(int,int)` with both parameters set to our parameter `size`.
     *
     * @param size width and height of image
     */
    fun setImageSize(size: Int) {
        setImageSize(size, size)
    }

    /**
     * The main processing method. This happens in a background task. In this case we are just
     * sampling down the bitmap and returning it from a resource. We just return the bitmap that
     * our method `decodeSampledBitmapFromResource` generates from the resource image given
     * by our parameter `int resId`, resized to the width `mImageWidth`, and the height
     * `mImageHeight`. The recycled bitmap returned by the method `getImageCache` will
     * be reused if possible.
     *
     * @param resId resource ID of image
     * @return `Bitmap` decoded from resource image
     */
    private fun processBitmap(resId: Int): Bitmap {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - $resId")
        }
        return decodeSampledBitmapFromResource(mResources, resId, mImageWidth,
            mImageHeight, imageCache)
    }

    /**
     * Converts its parameter to an integer resource ID and passes the call to our method
     * `processBitmap(int)`.
     *
     * @param data The data to identify which image to process, as provided by
     * [ImageWorker.loadImage]
     * @return Bitmap version of the resource image whose resource ID is the integer value of our
     * parameter `Object data`.
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
         * Decode and sample down a bitmap from resources to the requested width and height. We initialize
         * `BitmapFactory.Options options` with a new instance, set its `inJustDecodeBounds`
         * field to true then call the method `BitmapFactory.decodeResource(res, resId, options)`
         * to check the dimensions of the image. Then we set the `inSampleSize` field of `options`
         * to the value calculated by our method `calculateInSampleSize(options, reqWidth, reqHeight)`.
         * If our device is HONEYCOMB or newer we call our method `addInBitmapOptions(options, cache)`
         * to modify `options` to try to use `inBitmap` to recycle a bitmap from `ImageCache cache`.
         * We then set the `inJustDecodeBounds` field of `options` to false and return the bitmap
         * generated by the `decodeResource(res, resId, options)` method of `BitmapFactory`.
         *
         * @param res The resources object containing the image data
         * @param resId The resource id of the image data
         * @param reqWidth The requested width of the resulting bitmap
         * @param reqHeight The requested height of the resulting bitmap
         * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
         * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
         * that are equal to or greater than the requested width and height
         */
        fun decodeSampledBitmapFromResource(res: Resources?, resId: Int,
                                            reqWidth: Int, reqHeight: Int, cache: ImageCache?): Bitmap {

            // BEGIN_INCLUDE (read_bitmap_dimensions)
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            // END_INCLUDE (read_bitmap_dimensions)

            // If we're running on Honeycomb or newer, try to use inBitmap
            if (Utils.hasHoneycomb()) {
                addInBitmapOptions(options, cache)
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeResource(res, resId, options)
        }

        /**
         * Decode and sample down a bitmap from a file to the requested width and height. We initialize
         * `BitmapFactory.Options options` with a new instance, set its `inJustDecodeBounds`
         * field to true then call the method `BitmapFactory.decodeFile(filename, options)` to check
         * the dimensions of the image. Then we set the `inSampleSize` field of `options` to
         * the value calculated by our method `calculateInSampleSize(options, reqWidth, reqHeight)`.
         * If our device is HONEYCOMB or newer we call our method `addInBitmapOptions(options, cache)`
         * to modify `options` to try to use `inBitmap` to recycle a bitmap from `ImageCache cache`.
         * We then set the `inJustDecodeBounds` field of `options` to false and return the bitmap
         * generated by the `decodeFile(filename, options)` method of `BitmapFactory`.
         *
         * @param filename The full path of the file to decode
         * @param reqWidth The requested width of the resulting bitmap
         * @param reqHeight The requested height of the resulting bitmap
         * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
         * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
         * that are equal to or greater than the requested width and height
         */
        fun decodeSampledBitmapFromFile(filename: String?,
                                        reqWidth: Int, reqHeight: Int, cache: ImageCache?): Bitmap {

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
         * Decode and sample down a bitmap from a file input stream to the requested width and height.
         * We initialize `BitmapFactory.Options options` with a new instance, set its `inJustDecodeBounds`
         * field to true then call the method `BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)`
         * to check the dimensions of the image. Then we set the `inSampleSize` field of `options` to
         * the value calculated by our method `calculateInSampleSize(options, reqWidth, reqHeight)`.
         * If our device is HONEYCOMB or newer we call our method `addInBitmapOptions(options, cache)`
         * to modify `options` to try to use `inBitmap` to recycle a bitmap from `ImageCache cache`.
         * We then set the `inJustDecodeBounds` field of `options` to false and return the bitmap
         * generated by the `decodeFileDescriptor(fileDescriptor, null, options)` method of `BitmapFactory`.
         *
         * @param fileDescriptor The file descriptor to read from
         * @param reqWidth The requested width of the resulting bitmap
         * @param reqHeight The requested height of the resulting bitmap
         * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
         * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
         * that are equal to or greater than the requested width and height
         */
        fun decodeSampledBitmapFromDescriptor(
            fileDescriptor: FileDescriptor?, reqWidth: Int, reqHeight: Int, cache: ImageCache?): Bitmap {

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
         * Modifies its parameter `BitmapFactory.Options options` to try to reuse a bitmap fetched
         * from its parameter `ImageCache cache`. First we set the `inMutable` field of
         * `options` to true to force the decoder to return mutable bitmaps. If `cache` is
         * not null we initialize `Bitmap inBitmap` with the bitmap returned by the
         * `getBitmapFromReusableSet(options)` method of `cache`. If `inBitmap` is
         * not null we set the `inBitmap` field of `options` to `inBitmap`.
         *
         * @param options `Options` we are to modify
         * @param cache `ImageCache` we are to try to fetch reusable bitmaps from.
         */
        @SuppressLint("ObsoleteSdkInt")
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private fun addInBitmapOptions(options: BitmapFactory.Options, cache: ImageCache?) {
            //BEGIN_INCLUDE(add_bitmap_options)
            // inBitmap only works with mutable bitmaps so force the decoder to
            // return mutable bitmaps.
            options.inMutable = true
            if (cache != null) {
                // Try and find a bitmap to use for inBitmap
                val inBitmap = cache.getBitmapFromReusableSet(options)
                if (inBitmap != null) {
                    options.inBitmap = inBitmap
                }
            }
            //END_INCLUDE(add_bitmap_options)
        }

        /**
         * Calculate an inSampleSize for use in a [android.graphics.BitmapFactory.Options] object when decoding
         * bitmaps using the decode* methods from [android.graphics.BitmapFactory]. This implementation calculates
         * the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap
         * having a width and height equal to or larger than the requested width and height.
         *
         *
         * We initialize `int height` to the `outHeight` field of `options`, `int width`
         * to the `outWidth` field of `options`, and initialize `int inSampleSize` to 1.
         * Then if `height` is greater than `reqHeight` or `width` is greater than
         * `reqWidth` we need to scale the image. We initialize `halfHeight` to be half of
         * `height` and `halfWidth` to be half of `width`. Then we loop while the value
         * `halfHeight/inSampleSize` is greater than `reqHeight` and `halfWidth/inSampleSize`
         * is greater than `reqWidth` doubling `inSampleSize` (this calculates the largest
         * `inSampleSize` value that is a power of 2 and keeps both height and width larger than
         * the requested height and width).
         *
         *
         * Then we add some additional logic for strange aspect ratios. We initialize `totalPixels`
         * to be `width*height/inSampleSize`, and initialize `totalReqPixelsCap` to be
         * `reqWidth*reqHeight*2`. Then while `totalPixels` is greater than `totalReqPixelsCap`
         * we multiply `inSampleSize` by 2 and divide `totalPixels` by two (anything more than
         * 2x the requested pixels we'll sample down further)
         *
         *
         * Finally we return `inSampleSize` to the caller (it is always 1 in our case -- there being
         * no need for the scaling calculation).
         *
         * @param options An options object with out* params already populated (run through a decode*
         * method with inJustDecodeBounds==true
         * @param reqWidth The requested width of the resulting bitmap
         * @param reqHeight The requested height of the resulting bitmap
         * @return The value to be used for inSampleSize
         */
        fun calculateInSampleSize(options: BitmapFactory.Options,
                                  reqWidth: Int, reqHeight: Int): Int {
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
                var totalPixels = (width * height / inSampleSize).toLong()

                // Anything more than 2x the requested pixels we'll sample down further
                val totalReqPixelsCap = (reqWidth * reqHeight * 2).toLong()
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