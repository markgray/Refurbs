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

package com.example.android.displayingbitmaps.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.example.android.common.logger.Log;
import com.example.android.displayingbitmaps.BuildConfig;

import java.io.FileDescriptor;

/**
 * A simple subclass of {@link ImageWorker} that resizes images from resources given a target width
 * and height. Useful for when the input images might be too large to simply load directly into
 * memory.
 */
@SuppressWarnings("WeakerAccess")
public class ImageResizer extends ImageWorker {
    /**
     * TAG used for logging
     */
    private static final String TAG = "ImageResizer";
    /**
     * Width of image
     */
    protected int mImageWidth;
    /**
     * Height of image
     */
    protected int mImageHeight;

    /**
     * Initialize providing both width and height image size. First we call our super's constructor,
     * then we call our method {@code setImageSize} to set our fields {@code mImageWidth} and
     * {@code mImageHeight} to our parameters of the same name.
     *
     * @param context {@code Context} to use to access resources
     * @param imageWidth width of image
     * @param imageHeight height of image
     */
    public ImageResizer(Context context, int imageWidth, int imageHeight) {
        super(context);
        setImageSize(imageWidth, imageHeight);
    }

    /**
     * Initialize providing a single target image size (used for both width and height). First we call
     * our super's constructor, then we call our method {@code setImageSize} to set our fields {
     * @code mImageWidth} and {@code mImageHeight} both to our parameter {@code imageSize}.
     *
     * @param context {@code Context} to use to access resources
     * @param imageSize width and height of image
     */
    public ImageResizer(Context context, int imageSize) {
        super(context);
        setImageSize(imageSize);
    }

    /**
     * Set the target image width and height. We set our fields {@code mImageWidth} and {@code mImageHeight}
     * to our parameters {@code width} and {@code height} respectively.
     *
     * @param width width of image
     * @param height height of image
     */
    public void setImageSize(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     * Set the target image size (width and height will be the same). We call our method
     * {@code setImageSize(int,int)} with both parameters set to our parameter {@code size}.
     *
     * @param size width and height of image
     */
    public void setImageSize(int size) {
        setImageSize(size, size);
    }

    /**
     * The main processing method. This happens in a background task. In this case we are just
     * sampling down the bitmap and returning it from a resource. We just return the bitmap that
     * our method {@code decodeSampledBitmapFromResource} generates from the resource image given
     * by our parameter {@code int resId}, resized to the width {@code mImageWidth}, and the height
     * {@code mImageHeight}. The recycled bitmap returned by the method {@code getImageCache} will
     * be reused if possible.
     *
     * @param resId resource ID of image
     * @return {@code Bitmap} decoded from resource image
     */
    private Bitmap processBitmap(int resId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - " + resId);
        }
        return decodeSampledBitmapFromResource(mResources, resId, mImageWidth,
                mImageHeight, getImageCache());
    }

    /**
     * Converts its parameter to an integer resource ID and passes the call to our method
     * {@code processBitmap(int)}.
     *
     * @param data The data to identify which image to process, as provided by
     *            {@link ImageWorker#loadImage(Object, android.widget.ImageView)}
     * @return Bitmap version of the resource image whose resource ID is the integer value of our
     * parameter {@code Object data}.
     */
    @Override
    protected Bitmap processBitmap(Object data) {
        return processBitmap(Integer.parseInt(String.valueOf(data)));
    }

    /**
     * Decode and sample down a bitmap from resources to the requested width and height. We initialize
     * {@code BitmapFactory.Options options} with a new instance, set its {@code inJustDecodeBounds}
     * field to true then call the method {@code BitmapFactory.decodeResource(res, resId, options)}
     * to check the dimensions of the image. Then we set the {@code inSampleSize} field of {@code options}
     * to the value calculated by our method {@code calculateInSampleSize(options, reqWidth, reqHeight)}.
     * If our device is HONEYCOMB or newer we call our method {@code addInBitmapOptions(options, cache)}
     * to modify {@code options} to try to use {@code inBitmap} to recycle a bitmap from {@code ImageCache cache}.
     * We then set the {@code inJustDecodeBounds} field of {@code options} to false and return the bitmap
     * generated by the {@code decodeResource(res, resId, options)} method of {@code BitmapFactory}.
     *
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight, ImageCache cache) {

        // BEGIN_INCLUDE (read_bitmap_dimensions)
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // END_INCLUDE (read_bitmap_dimensions)

        // If we're running on Honeycomb or newer, try to use inBitmap
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, cache);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and height. We initialize
     * {@code BitmapFactory.Options options} with a new instance, set its {@code inJustDecodeBounds}
     * field to true then call the method {@code BitmapFactory.decodeFile(filename, options)} to check
     * the dimensions of the image. Then we set the {@code inSampleSize} field of {@code options} to
     * the value calculated by our method {@code calculateInSampleSize(options, reqWidth, reqHeight)}.
     * If our device is HONEYCOMB or newer we call our method {@code addInBitmapOptions(options, cache)}
     * to modify {@code options} to try to use {@code inBitmap} to recycle a bitmap from {@code ImageCache cache}.
     * We then set the {@code inJustDecodeBounds} field of {@code options} to false and return the bitmap
     * generated by the {@code decodeFile(filename, options)} method of {@code BitmapFactory}.
     *
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    @SuppressWarnings("unused")
    public static Bitmap decodeSampledBitmapFromFile(String filename,
                                                     int reqWidth, int reqHeight, ImageCache cache) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // If we're running on Honeycomb or newer, try to use inBitmap
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, cache);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     * We initialize {@code BitmapFactory.Options options} with a new instance, set its {@code inJustDecodeBounds}
     * field to true then call the method {@code BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)}
     * to check the dimensions of the image. Then we set the {@code inSampleSize} field of {@code options} to
     * the value calculated by our method {@code calculateInSampleSize(options, reqWidth, reqHeight)}.
     * If our device is HONEYCOMB or newer we call our method {@code addInBitmapOptions(options, cache)}
     * to modify {@code options} to try to use {@code inBitmap} to recycle a bitmap from {@code ImageCache cache}.
     * We then set the {@code inJustDecodeBounds} field of {@code options} to false and return the bitmap
     * generated by the {@code decodeFileDescriptor(fileDescriptor, null, options)} method of {@code BitmapFactory}.
     *
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromDescriptor(
            FileDescriptor fileDescriptor, int reqWidth, int reqHeight, ImageCache cache) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // If we're running on Honeycomb or newer, try to use inBitmap
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, cache);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    /**
     * Modifies its parameter {@code BitmapFactory.Options options} to try to reuse a bitmap fetched
     * from its parameter {@code ImageCache cache}. First we set the {@code inMutable} field of
     * {@code options} to true to force the decoder to return mutable bitmaps. If {@code cache} is
     * not null we initialize {@code Bitmap inBitmap} with the bitmap returned by the
     * {@code getBitmapFromReusableSet(options)} method of {@code cache}. If {@code inBitmap} is
     * not null we set the {@code inBitmap} field of {@code options} to {@code inBitmap}.
     *
     * @param options {@code Options} we are to modify
     * @param cache {@code ImageCache} we are to try to fetch reusable bitmaps from.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void addInBitmapOptions(BitmapFactory.Options options, ImageCache cache) {
        //BEGIN_INCLUDE(add_bitmap_options)
        // inBitmap only works with mutable bitmaps so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;

        if (cache != null) {
            // Try and find a bitmap to use for inBitmap
            Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

            if (inBitmap != null) {
                options.inBitmap = inBitmap;
            }
        }
        //END_INCLUDE(add_bitmap_options)
    }

    /**
     * Calculate an inSampleSize for use in a {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link android.graphics.BitmapFactory}. This implementation calculates
     * the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap
     * having a width and height equal to or larger than the requested width and height.
     * <p>
     * We initialize {@code int height} to the {@code outHeight} field of {@code options}, {@code int width}
     * to the {@code outWidth} field of {@code options}, and initialize {@code int inSampleSize} to 1.
     * Then if {@code height} is greater than {@code reqHeight} or {@code width} is greater than
     * {@code reqWidth} we need to scale the image. We initialize {@code halfHeight} to be half of
     * {@code height} and {@code halfWidth} to be half of {@code width}. Then we loop while the value
     * {@code halfHeight/inSampleSize} is greater than {@code reqHeight} and {@code halfWidth/inSampleSize}
     * is greater than {@code reqWidth} doubling {@code inSampleSize} (this calculates the largest
     * {@code inSampleSize} value that is a power of 2 and keeps both height and width larger than
     * the requested height and width).
     * <p>
     * Then we add some additional logic for strange aspect ratios. We initialize {@code totalPixels}
     * to be {@code width*height/inSampleSize}, and initialize {@code totalReqPixelsCap} to be
     * {@code reqWidth*reqHeight*2}. Then while {@code totalPixels} is greater than {@code totalReqPixelsCap}
     * we multiply {@code inSampleSize} by 2 and divide {@code totalPixels} by two (anything more than
     * 2x the requested pixels we'll sample down further)
     * <p>
     * Finally we return {@code inSampleSize} to the caller (it is always 1 in our case -- there being
     * no need for the scaling calculation).
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // BEGIN_INCLUDE (calculate_sample_size)
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            long totalPixels = width * height / inSampleSize;

            // Anything more than 2x the requested pixels we'll sample down further
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
        // END_INCLUDE (calculate_sample_size)
    }
}
