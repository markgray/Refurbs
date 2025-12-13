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
@file:Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")

package com.example.android.activityanim

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.GridLayout
import androidx.core.graphics.scale

/**
 * This class has a bunch of static-like utility methods for creating [PictureData] objects
 * from randomly selected resources. It is used by the [ActivityAnimations] main `Activity` to
 * create the data that it displays in its [GridLayout]. It also has a method to create a thumbnail
 * [Bitmap] from an original [Bitmap] which is used by its [loadPhotos] method. The [getBitmap]
 * method uses a [HashMap] to cache the [Bitmap]'s it decodes from the resources to avoid having
 * to decode them more than once.
 */
class BitmapUtils {
    /**
     * The resource ids of the jpg images we load.
     */
    var mPhotos: IntArray = intArrayOf(
        R.drawable.p1,
        R.drawable.p2,
        R.drawable.p3,
        R.drawable.p4
    )

    /**
     * Description strings we assign randomly to the jpg's
     */
    var mDescriptions: Array<String> = arrayOf(
        "This picture was taken while sunbathing in a natural hot spring, which was " +
            "unfortunately filled with acid, which is a lasting memory from that trip, whenever " +
            "I look at my own skin.",
        "I took this shot with a pinhole camera mounted on a tripod constructed out of " +
            "soda straws. I felt that that combination best captured the beauty of the landscape " +
            "in juxtaposition with the detritus of mankind.",
        "I don't remember where or when I took this picture. All I know is that I was really " +
            "drunk at the time, and I woke up without my left sock.",
        "Right before I took this picture, there was a busload of school children right " +
            "in my way. I knew the perfect shot was coming, so I quickly yelled 'Free candy!!!' " +
            "and they scattered."
    )

    /**
     * Load pictures and descriptions. A real app wouldn't do it this way, but that's not the point
     * of this animation demo. Loading asynchronously is a better way to go for what can be time
     * consuming operations. First we initialize [ArrayList] of [PictureData] variable `pictures`
     * with a new instance. Then we loop for 30 repetitions:
     *
     *  * We initialize [Int] variable `resourceId` with a resource id randomly chosen from our
     *  [IntArray] property [mPhotos].
     *
     *  * We initialize [Bitmap] variable `bitmap` with the bitmap returned by our method [getBitmap]
     *  for `resourceId` (either from our [HashMap] of [Int] to [Bitmap] cache [sBitmapResourceMap]
     *  or decoded and cached if it was not found in the cache)
     *
     *  * We initialize [Bitmap] variable `thumbnail` with the thumbnail image returned by our method
     *  [getThumbnail] when passed `bitmap` and a maximum dimension of 200.
     *
     *  * We initialize [String] variable `description` with a string randomly chosen from our [Array]
     *  of [String] property [mDescriptions].
     *
     *  * We then add a new instance of [PictureData] constructed from `resourceId`, `description`,
     *  and `thumbnail` to [ArrayList] of [PictureData] variable `pictures` and loop back to make
     *  the next [PictureData] object.
     *
     * When done with our loop we return `pictures` to the caller.
     *
     * @param resources [Resources] instance to use to access resources.
     * @return A list of [PictureData] objects created using randomly chosen jpg's and
     * description strings.
     */
    fun loadPhotos(resources: Resources?): ArrayList<PictureData> {
        val pictures = ArrayList<PictureData>()
        (0..29).forEach { _: Int ->
            val resourceId: Int = mPhotos[(Math.random() * mPhotos.size).toInt()]
            val bitmap: Bitmap? = getBitmap(resources = resources, resourceId = resourceId)
            val thumbnail: Bitmap = getThumbnail(original = bitmap, maxDimension = 200)
            val description: String = mDescriptions[(Math.random() * mDescriptions.size).toInt()]
            pictures.add(
                PictureData(
                    resourceId = resourceId,
                    description = description,
                    thumbnail = thumbnail
                )
            )
        }
        return pictures
    }

    /**
     * Create and return a thumbnail image given the original source bitmap and a max dimension
     * (width or height). We initialize [Int] variable `width` with the width of our [Bitmap]
     * parameter [original] and initialize [Int] variable `height` with its height. We then declare
     * [Int] variable `scaledWidth`, and [Int] variable `scaledHeight`. We then branch on whether
     * `width` is greater than or equal to `height`:
     *
     *  * the width of `original` is the controlling dimension: we initialize [Float] variable
     *  `scaleFactor` to our [Int] parameter [maxDimension] divided by `width`, set `scaledWidth`
     *  to 200, and set `scaledHeight` to the truncated value of `scaleFactor` times `height`
     *
     *  * the height of `original` is the controlling dimension: we initialize [Float] variable
     *  `scaleFactor` to [Int] parameter [maxDimension] divided by `height`, set `scaledWidth` to
     *  the truncated value of `scaleFactor` times `width`, and set `scaledHeight` to 200.
     *
     * We then create and return the [Bitmap] created using the [Bitmap.createScaledBitmap] method
     * of our [Bitmap] parameter [original] with its `width` argument `scaledWidth` and its `height`
     * argument `scaledHeight` (its `filter` argument is `true` by default so the [Bitmap] will be
     * filtered).
     *
     * @param original original full sized [Bitmap]
     * @param maxDimension maximum dimension of the thumbnail for both width and height
     * @return a thumbnail image created from the original source bitmap whose maximum dimension is
     * given by our [Int] parameter [maxDimension]
     */
    @Suppress("SameParameterValue")
    private fun getThumbnail(original: Bitmap?, maxDimension: Int): Bitmap {
        val width: Int = original!!.width
        val height: Int = original.height
        val scaledWidth: Int
        val scaledHeight: Int
        if (width >= height) {
            val scaleFactor: Float = maxDimension.toFloat() / width
            scaledWidth = 200
            scaledHeight = (scaleFactor * height).toInt()
        } else {
            val scaleFactor: Float = maxDimension.toFloat() / height
            scaledWidth = (scaleFactor * width).toInt()
            scaledHeight = 200
        }
        return original.scale(width = scaledWidth, height = scaledHeight)
    }

    companion object {
        /**
         * Cache of already decoded [Bitmap]'s of resource jpg's, stored using the resource id
         * as the key, and used by our [getBitmap] method to avoid decoding images more than once.
         */
        @SuppressLint("UseSparseArrays")
        var sBitmapResourceMap: HashMap<Int, Bitmap?> = HashMap()

        /**
         * Utility method to get bitmap from cache or, if not there, load it from our resources. We
         * try to initialize [Bitmap] variable `bitmap` by fetching the bitmap stored under the key
         * [resourceId] in our [HashMap] of [Int] to [Bitmap] cache property [sBitmapResourceMap],
         * and if `bitmap` is `null ([Bitmap] not found) we set `bitmap` to the result returned by
         * the [BitmapFactory.decodeResource] method for [resourceId] and store it under the key
         * [resourceId] in [sBitmapResourceMap]. In any case we return `bitmap` to the caller.
         *
         * @param resources [Resources] instance to use to access resources
         * @param resourceId the resource id of the jpg we are to return a bitmap of
         * @return a [Bitmap] decoded from the jpg with resource id [resourceId]
         */
        @JvmStatic
        fun getBitmap(resources: Resources?, resourceId: Int): Bitmap? {
            var bitmap: Bitmap? = sBitmapResourceMap[resourceId]
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(resources, resourceId)
                sBitmapResourceMap[resourceId] = bitmap
            }
            return bitmap
        }
    }
}