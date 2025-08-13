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
@file:Suppress("DEPRECATION", "CatchMayIgnoreException", "PLATFORM_CLASS_MAPPED_TO_KOTLIN", "JoinDeclarationAndAssignment", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.displayingbitmaps.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.annotation.RequiresApi
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.SoftReference
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Collections

/**
 * This class handles disk and memory caching of bitmaps in conjunction with the [ImageWorker] class
 * and its subclasses. Use [ImageCache.getInstance] to get an instance of this class, although
 * usually a cache should be added directly to an [ImageWorker] by calling [ImageWorker.addImageCache].
 * Our constructor just calls our [initialize] method with our parameter in our `init` block.
 *
 * @param cacheParams The cache parameters to use to initialize the cache
 */
class ImageCache private constructor(cacheParams: ImageCacheParams) {
    /**
     * [DiskLruCache] we use to cache to disk
     */
    private var mDiskLruCache: DiskLruCache? = null

    /**
     * Least recently used Memory cache
     */
    private var mMemoryCache: LruCache<String, BitmapDrawable>? = null

    /**
     * [ImageCacheParams] that were used to create this instance.
     */
    private var mCacheParams: ImageCacheParams? = null

    /**
     * Object we use to synchronize our access to the disk cache.
     */
    private val mDiskCacheLock = Object()

    /**
     * Flag we use to prevent access to the disk cache until it has been initialized.
     */
    private var mDiskCacheStarting = true

    /**
     * Set of reusable bitmaps that can be populated into the `inBitmap` field of
     * [BitmapFactory.Options]
     */
    private var mReusableBitmaps: MutableSet<SoftReference<Bitmap>>? = null

    /**
     * Part of the constructor of our `ImageCache`, it just calls our `initialize` method with the
     * `ImageCacheParams` the constructor was called wiht
     */
    init {
        initialize(cacheParams)
    }

    /**
     * Initialize the cache, providing all parameters. First we save our [ImageCacheParams] parameter
     * [cacheParams] in our [ImageCacheParams] field [mCacheParams].
     * If the [ImageCacheParams.memoryCacheEnabled] field of [mCacheParams] is true we:
     *
     *  1. If [BuildConfig.DEBUG] is true we log the fact that a memory cache was created.
     *
     *  2. If we are running on HONEYCOMB or above, we initialize our [mReusableBitmaps] field with
     *  a "synchronized set wrapped" [HashSet] of soft referenced [Bitmap] objects.
     *
     *  3. We initialize our [LruCache] of [String] to [BitmapDrawable] field [mMemoryCache] with a
     *  new instance whose size is the [ImageCacheParams.memCacheSize] property of [mCacheParams]
     *  overriding its [LruCache.entryRemoved]  method to notify a [RecyclingBitmapDrawable] that it
     *  is no longer cached or to add a standard [BitmapDrawable] to the [MutableSet] of
     *  [SoftReference]'s to [Bitmap]'s field [mReusableBitmaps] (our Set of reusable bitmaps).
     *  It also overrides the [LruCache.sizeOf] method to return the size of the bitmap divided by
     *  1024.
     *
     * Finally if the [ImageCacheParams.initDiskCacheOnCreate] field of [cacheParams] is `true` we
     * call the [initDiskCache] method (it is always `false` in our case, and the [initDiskCache]
     * method is called on a background thread instead).
     *
     * @param cacheParams The cache parameters to initialize the cache
     */
    private fun initialize(cacheParams: ImageCacheParams) {
        mCacheParams = cacheParams

        //BEGIN_INCLUDE(init_memory_cache)
        // Set up memory cache
        if (mCacheParams!!.memoryCacheEnabled) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Memory cache created (size = " + mCacheParams!!.memCacheSize + ")")
            }

            // If we're running on Honeycomb or newer, create a set of reusable bitmaps that can be
            // populated into the inBitmap field of BitmapFactory.Options. Note that the set is
            // of SoftReferences which will actually not be very effective due to the garbage
            // collector being aggressive clearing Soft/WeakReferences. A better approach
            // would be to use a strongly references bitmaps, however this would require some
            // balancing of memory usage between this set and the bitmap LruCache. It would also
            // require knowledge of the expected size of the bitmaps. From Honeycomb to JellyBean
            // the size would need to be precise, from KitKat onward the size would just need to
            // be the upper bound (due to changes in how inBitmap can re-use bitmaps).
            if (Utils.hasHoneycomb()) {
                mReusableBitmaps = Collections.synchronizedSet(HashSet())
            }
            mMemoryCache = object : LruCache<String, BitmapDrawable>(mCacheParams!!.memCacheSize) {
                /**
                 * Notify the removed entry that is no longer being cached. Called for entries that
                 * have been evicted or removed. This method is invoked when a value is evicted to
                 * make space, removed by a call to [remove], or replaced by a call to [put]. The
                 * default implementation does nothing. If the [BitmapDrawable] parameter [oldValue]
                 * is an instance of [RecyclingBitmapDrawable] we call its
                 * [RecyclingBitmapDrawable.setIsCached] method with `false` to notify it that it is
                 * no longer cached, otherwise if we are running on HONEYCOMB or above we add the
                 * bitmap of [oldValue] to the [mReusableBitmaps] set so it can be reused.
                 *
                 * @param evicted `true` if the entry is being removed to make space, `false`
                 * if the removal was caused by a [put] or [remove].
                 * @param key key of the entry removed
                 * @param oldValue the old value for `key`, if it existed.
                 * @param newValue the new value for `key`, if it exists. If non-`null`,
                 * this removal was caused by a [put]. Otherwise it was caused by
                 * an eviction or a [remove].
                 */
                override fun entryRemoved(
                    evicted: Boolean,
                    key: String,
                    oldValue: BitmapDrawable,
                    newValue: BitmapDrawable?
                ) {
                    if (RecyclingBitmapDrawable::class.java.isInstance(oldValue)) {
                        // The removed entry is a recycling drawable, so notify it
                        // that it has been removed from the memory cache
                        (oldValue as RecyclingBitmapDrawable).setIsCached(false)
                    } else {
                        // The removed entry is a standard BitmapDrawable
                        if (Utils.hasHoneycomb()) {
                            // We're running on Honeycomb or later, so add the bitmap
                            // to a SoftReference set for possible use with inBitmap later
                            mReusableBitmaps!!.add(SoftReference(oldValue.bitmap))
                        }
                    }
                }

                /**
                 * Measure item size in kilobytes rather than units which is more practical for a
                 * bitmap cache. We calculate [Int] variable `val bitmapSize` to be the size of
                 * the [BitmapDrawable] parameter [value] as calculated by our method [getBitmapSize]
                 * divided by 1024. If `bitmapSize` is 0 we return 1 to the caller, otherwise we
                 * return `bitmapSize`.
                 *
                 * @param key key of the `Entry` in our cache
                 * @param value the value being cached
                 * @return the size of the entry for `key` and `value` in
                 * user-defined units.
                 */
                override fun sizeOf(key: String, value: BitmapDrawable): Int {
                    val bitmapSize = getBitmapSize(value) / 1024
                    return if (bitmapSize == 0) 1 else bitmapSize
                }
            }
        }
        //END_INCLUDE(init_memory_cache)

        // By default the disk cache is not initialized here as it should be initialized
        // on a separate thread due to disk access.
        if (cacheParams.initDiskCacheOnCreate) {
            // Set up disk cache
            initDiskCache()
        }
    }

    /**
     * Initializes the disk cache. Note that this includes disk access so this should not be
     * executed on the main/UI thread. By default an [ImageCache] does not initialize the disk
     * cache when it is created, instead you should call [initDiskCache] to initialize it on a
     * background thread. Synchronized on our [Object] field [mDiskCacheLock] we first check
     * whether [mDiskLruCache] is `null` or its [DiskLruCache.isClosed] method returns `true`
     * (the cache does not exist, or it is closed), and if so we set [File] variable
     * `val diskCacheDir` to the [ImageCacheParams.diskCacheDir] field of [mCacheParams]. If the
     * [ImageCacheParams.diskCacheEnabled] field of [mCacheParams] is `true` and `diskCacheDir` is
     * not `null` we call the [File.mkdirs] method of `diskCacheDir` is it does not already exist.
     * We check to see if the usable space calculated by our method [getUsableSpace] for
     * `diskCacheDir` is greater than the [ImageCacheParams.diskCacheSize] field of [mCacheParams]
     * and if so we wrap in a try block intended to catch [IOException], code which initializes
     * [mDiskLruCache] with an instance of [DiskLruCache] created to write to `diskCacheDir` using
     * an app version of 1, a value count of 1 (one file per entry) and a maximum size given by the
     * [ImageCacheParams.diskCacheSize] field of [mCacheParams] (if we catch an [IOException] doing
     * this we set the [ImageCacheParams.diskCacheDir] field of [mCacheParams] to `null` and log the
     * error).
     *
     * Whether we needed to create the disk cache or it already existed, we set our field
     * [mDiskCacheStarting] to `false` and wake up all threads that are waiting on the monitor
     * of [mDiskCacheLock].
     */
    fun initDiskCache() {
        // Set up disk cache
        synchronized(mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache!!.isClosed) {
                val diskCacheDir: File? = mCacheParams!!.diskCacheDir
                if (mCacheParams!!.diskCacheEnabled && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs()
                    }
                    if (getUsableSpace(diskCacheDir) > mCacheParams!!.diskCacheSize) {
                        try {
                            mDiskLruCache = DiskLruCache.open(
                                diskCacheDir, 1, 1,
                                mCacheParams!!.diskCacheSize.toLong()
                            )
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Disk cache initialized")
                            }
                        } catch (e: IOException) {
                            mCacheParams!!.diskCacheDir = null
                            Log.e(TAG, "initDiskCache - $e")
                        }
                    }
                }
            }
            mDiskCacheStarting = false
            mDiskCacheLock.notifyAll()
        }
    }

    /**
     * Adds a bitmap to both memory and disk cache. First if either of our parameters is `null` we
     * return having done nothing. If our [LruCache] of [String] to [BitmapDrawable] field
     * [mMemoryCache] is not `null` we add the our [BitmapDrawable] parameter [value] to the memory
     * cache, calling the [RecyclingBitmapDrawable.setIsCached] method of [value] with `true` if it
     * is an instance of [RecyclingBitmapDrawable] first before storing [value] using our [String]
     * parameter [data] a the key in [mMemoryCache].
     *
     * Then synchronized on [Object] field [mDiskCacheLock] if [mDiskLruCache] is not `null` we
     * generate [String] variable `val key` by calling [hashKeyForDisk] with our [String] parameter
     * [data] and initialize [OutputStream] variable `var out` to null. Then wrapped in a `try`
     * block we fetch a snapshot of the `Entry` with key `key` in [mDiskLruCache] to initialize our
     * [DiskLruCache.Snapshot] variable `val snapshot`. If this is `null` we open an
     * [DiskLruCache.Editor] variable `val editor` to edit the entry for `key` and if `editor` is
     * not `null` we set `out` to be an output stream for the file with index [DISK_CACHE_INDEX] and
     * then compress the bitmap of `value` using the [ImageCacheParams.compressFormat] field of
     * [mCacheParams] as the format, its [ImageCacheParams.compressQuality] field as the quality,
     * and writing the compressed file to `out`. We then call the [DiskLruCache.Editor.commit]
     * method of `editor` to finalize the edit and close `out`. If `snapshot` was not `null` we
     * close the file with index [DISK_CACHE_INDEX].
     *
     * In the catch blocks of the above try block we log the error for both [IOException] and
     * [Exception], and in its `finally` block we close `out` if it is not null and ignore any
     * [IOException] that occurs when doing this.
     *
     * @param data Unique identifier for the bitmap to store
     * @param value The bitmap drawable to store
     */
    fun addBitmapToCache(data: String?, value: BitmapDrawable?) {
        //BEGIN_INCLUDE(add_bitmap_to_cache)
        if (data == null || value == null) {
            return
        }

        // Add to memory cache
        if (mMemoryCache != null) {
            if (RecyclingBitmapDrawable::class.java.isInstance(value)) {
                // The removed entry is a recycling drawable, so notify it
                // that it has been added into the memory cache
                (value as RecyclingBitmapDrawable).setIsCached(true)
            }
            mMemoryCache!!.put(data, value)
        }
        synchronized(mDiskCacheLock) {
            // Add to disk cache
            if (mDiskLruCache != null) {
                val key = hashKeyForDisk(data)
                var out: OutputStream? = null
                try {
                    val snapshot: DiskLruCache.Snapshot? = mDiskLruCache!![key]
                    if (snapshot == null) {
                        val editor: DiskLruCache.Editor? = mDiskLruCache!!.edit(key)
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX)
                            value.bitmap.compress(
                                mCacheParams!!.compressFormat,
                                mCacheParams!!.compressQuality,
                                out
                            )
                            editor.commit()
                            out.close()
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX)!!.close()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "addBitmapToCache - $e")
                } catch (e: Exception) {
                    Log.e(TAG, "addBitmapToCache - $e")
                } finally {
                    try {
                        out?.close()
                    } catch (_: IOException) {
                    }
                }
            }
        }
        //END_INCLUDE(add_bitmap_to_cache)
    }

    /**
     * Get from memory cache. We initialize [BitmapDrawable] variable `var memValue` to `null`, and
     * if our field [mMemoryCache] is not `null` we set `memValue` to the [BitmapDrawable] stored
     * in it using our [String] parameter [data] as the key. Finally we return `memValue` to the
     * caller.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap drawable if found in cache, null otherwise
     */
    fun getBitmapFromMemCache(data: String): BitmapDrawable? {
        //BEGIN_INCLUDE(get_bitmap_from_mem_cache)
        var memValue: BitmapDrawable? = null
        if (mMemoryCache != null) {
            memValue = mMemoryCache!![data]
        }
        if (BuildConfig.DEBUG && memValue != null) {
            Log.d(TAG, "Memory cache hit")
        }
        return memValue
        //END_INCLUDE(get_bitmap_from_mem_cache)
    }

    /**
     * Get from disk cache. We generate [String] variable `val key` from our [String] parameter
     * [data] using our method [hashKeyForDisk], and initialize [Bitmap] variable `var bitmap` to
     * `null`. Synchronizing on our [Object] field [mDiskCacheLock] we first loop waiting until
     * our [Boolean] field [mDiskCacheStarting] goes `false` (this will happen after the disk cache
     * is initialized).
     *
     * If our [DiskLruCache] field [mDiskLruCache] is not `null` we initialize [InputStream] variable
     * `var inputStream` to `null` and wrapped in a try block intended to catch [IOException] we
     * fetch a [DiskLruCache.Snapshot] snapshot to variable `val snapshot` from [mDiskLruCache] for
     * the key `key`, and if `snapshot` is not `null` we set `inputStream` to an input stream for
     * the file with index [DISK_CACHE_INDEX]. Then if `inputStream` is not `null` we set
     * [FileDescriptor] variable `val fd` to its file descriptor and use the method
     * [ImageResizer.decodeSampledBitmapFromDescriptor] to decode the contents of the file into
     * [Bitmap] variable `val bitmap`. If we catch [IOException] we just log the error, and in
     * the finally block we close `inputStream` if it is not `null`, ignoring [IOException].
     *
     * Finally we return `bitmap` to the caller.
     *
     * @param data Unique identifier for which item to get
     * @return The [Bitmap] if found in cache, null otherwise
     */
    fun getBitmapFromDiskCache(data: String): Bitmap? {
        //BEGIN_INCLUDE(get_bitmap_from_disk_cache)
        val key = hashKeyForDisk(data)
        var bitmap: Bitmap? = null
        synchronized(mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait()
                } catch (_: InterruptedException) {
                }
            }
            if (mDiskLruCache != null) {
                var inputStream: InputStream? = null
                try {
                    val snapshot: DiskLruCache.Snapshot? = mDiskLruCache!![key]
                    if (snapshot != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Disk cache hit")
                        }
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX)
                        if (inputStream != null) {
                            val fd: FileDescriptor = (inputStream as FileInputStream).fd

                            // Decode bitmap, but we don't want to sample so give
                            // MAX_VALUE as the target dimensions
                            bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(
                                fd, Int.MAX_VALUE, Int.MAX_VALUE, this
                            )
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "getBitmapFromDiskCache - $e")
                } finally {
                    try {
                        inputStream?.close()
                    } catch (_: IOException) {
                    }
                }
            }
            return bitmap
        }
        //END_INCLUDE(get_bitmap_from_disk_cache)
    }

    /**
     * Searches our [MutableSet] of [SoftReference] to [Bitmap] field [mReusableBitmaps] for a
     * [Bitmap] that can be reused by [BitmapFactory]. First we initialize [Bitmap] variable
     * `var bitmap` to `null`. Then if [mReusableBitmaps] is not `null`, and is not empty we
     * synchronize on [mReusableBitmaps], create an [MutableIterator] of [SoftReference] to
     * [Bitmap] variable `val iterator` for [mReusableBitmaps], and declare [Bitmap] variable
     * `var item`. Looping over all of the bitmaps available to `iterator` we set `item` to each
     * bitmap and if:
     *
     *  * `item` is not `null` and is mutable, we call our method [canUseForInBitmap] to check if
     *  `item` can be reused for a bitmap as specified by our [BitmapFactory.Options] parameter
     *  [options], and if it can we set `bitmap` to `item`, remove it from `iterator` then break.
     *
     *  * else: we remove the item from the [mReusableBitmaps] set since it has been cleared.
     *
     * Finally we return `bitmap` to the caller.
     *
     * @param options - [BitmapFactory.Options] with out* options populated
     * @return [Bitmap] that case be used for `inBitmap`
     */
    fun getBitmapFromReusableSet(options: BitmapFactory.Options): Bitmap? {
        //BEGIN_INCLUDE(get_bitmap_from_reusable_set)
        var bitmap: Bitmap? = null
        if (mReusableBitmaps != null && mReusableBitmaps!!.isNotEmpty()) {
            synchronized(mReusableBitmaps!!) {
                val iterator: MutableIterator<SoftReference<Bitmap>> = mReusableBitmaps!!.iterator()
                var item: Bitmap?
                while (iterator.hasNext()) {
                    item = iterator.next().get()
                    if (null != item && item.isMutable) {
                        // Check to see it the item can be used for inBitmap
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item

                            // Remove from reusable set so it can't be used again
                            iterator.remove()
                            break
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        iterator.remove()
                    }
                }
            }
        }
        return bitmap
        //END_INCLUDE(get_bitmap_from_reusable_set)
    }

    /**
     * Clears both the memory and disk cache associated with this [ImageCache] object. Note that
     * this includes disk access so this should not be executed on the main/UI thread. First if
     * [LruCache] of [String] to [BitmapDrawable] field [mMemoryCache] is not `null` we call its
     * [LruCache.evictAll] method to clear the cache. Then synchronized on [Object] field
     * [mDiskCacheLock] we set [Boolean] field [mDiskCacheStarting] to `true`, and if [DiskLruCache]
     * field [mDiskLruCache] is not `null` and is not closed we wrap code to call its
     * [DiskLruCache.delete] method in a try block intended to catch [IOException], with the catch
     * block just logging the error. We then set [mDiskLruCache] to null and call our
     * [initDiskCache] method to create a new empty cache.
     */
    fun clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache!!.evictAll()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Memory cache cleared")
            }
        }
        synchronized(mDiskCacheLock) {
            mDiskCacheStarting = true
            if (mDiskLruCache != null && !mDiskLruCache!!.isClosed) {
                try {
                    mDiskLruCache!!.delete()
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Disk cache cleared")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "clearCache - $e")
                }
                mDiskLruCache = null
                initDiskCache()
            }
        }
    }

    /**
     * Flushes the disk cache associated with this [ImageCache] object. Note that this includes
     * disk access so this should not be executed on the main/UI thread. Synchronized on our
     * [Object] field [mDiskCacheLock] we check that [DiskLruCache] field [mDiskLruCache] is not
     * `null` before calling its [DiskLruCache.flush] method wrapped in a try block intended to
     * catch IOException (the catch block just logs the error).
     */
    fun flush() {
        synchronized(mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    mDiskLruCache!!.flush()
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Disk cache flushed")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "flush - $e")
                }
            }
        }
    }

    /**
     * Closes the disk cache associated with this [ImageCache] object. Note that this includes
     * disk access so this should not be executed on the main/UI thread. Synchronized on our
     * [Object] field [mDiskCacheLock] we check that [DiskLruCache] field [mDiskLruCache] is not
     * `null` before doing anything. If it is not `null`, wrapped in a try block intended to catch
     * [IOException] we make sure that [mDiskLruCache] is not already closed before calling its
     * [DiskLruCache.close] method and setting it to `null` (the catch block just logs the error).
     */
    fun close() {
        synchronized(mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    if (!mDiskLruCache!!.isClosed) {
                        mDiskLruCache!!.close()
                        mDiskLruCache = null
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Disk cache closed")
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "close - $e")
                }
            }
        }
    }

    /**
     * A holder class that contains cache parameters. It Creates a set of image cache parameters
     * that can be provided to [ImageCache.getInstance] or `[ImageWorker.addImageCache]. In our
     * `init` block we initialize our [File] field [diskCacheDir] with the [File] returned by our
     * method [getDiskCacheDir].
     *
     * @param context A [Context] to use.
     * @param diskCacheDirectoryName A unique subdirectory name that will be appended to the
     * application cache directory. Usually "cache" or "images" is sufficient.
     */
    class ImageCacheParams(context: Context, diskCacheDirectoryName: String) {
        /** memory cache size in kilobytes  */
        var memCacheSize: Int = DEFAULT_MEM_CACHE_SIZE

        /** Disk cache size in bytes  */
        var diskCacheSize: Int = DEFAULT_DISK_CACHE_SIZE

        /** Directory for disk cache  */
        var diskCacheDir: File?

        /** Compression format when caching to disk  */
        var compressFormat: CompressFormat = DEFAULT_COMPRESS_FORMAT

        /** Compression quality when compressing to disk  */
        var compressQuality: Int = DEFAULT_COMPRESS_QUALITY

        /** enable flag for memory cache  */
        var memoryCacheEnabled: Boolean = DEFAULT_MEM_CACHE_ENABLED

        /** enable flag for disk cache  */
        var diskCacheEnabled: Boolean = DEFAULT_DISK_CACHE_ENABLED

        /** flag for initializing disk cache on UI thread  */
        var initDiskCacheOnCreate: Boolean = DEFAULT_INIT_DISK_CACHE_ON_CREATE

        /**
         * We initialize our field `File diskCacheDir` with the `File` returned by our
         * method `getDiskCacheDir`.
         *
         * param context A context to use.
         * param diskCacheDirectoryName A unique subdirectory name that will be appended to the
         * application cache directory. Usually "cache" or "images"
         * is sufficient.
         */
        init {
            diskCacheDir = getDiskCacheDir(context = context, uniqueName = diskCacheDirectoryName)
        }

        /**
         * Sets the memory cache size stored in [Int] field [memCacheSize] based on a fraction of
         * the max available VM memory. Eg. setting fraction to 0.2 would set the memory cache to
         * one fifth of the available memory.
         *
         * Throws [IllegalArgumentException] if fraction is < 0.01 or > .8. [memCacheSize] is stored
         * in kilobytes instead of bytes as this will eventually be passed to construct a [LruCache]
         * which takes an [Int] in its constructor.
         *
         * This value should be chosen carefully based on a number of factors
         * Refer to the corresponding Android Training class for more discussion:
         * http://developer.android.com/training/displaying-bitmaps/
         *
         * First we check that our [Float] parameter [fraction] is between 0.01 and 0.8, throwing
         * [IllegalArgumentException] if it is not. We then set [memCacheSize] to [fraction] times
         * the maximum amount of memory that the Java virtual machine will attempt to use divided
         * by 1024 to convert to kilobytes.
         *
         * @param fraction Fraction of available app memory to use to size memory cache
         */
        fun setMemCacheSizePercent(fraction: Float) {
            require(!(fraction < 0.01f || fraction > 0.8f)) {
                ("setMemCacheSizePercent - percent must be "
                    + "between 0.01 and 0.8 (inclusive)")
            }
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            memCacheSize = Math.round(fraction * Runtime.getRuntime().maxMemory() / 1024)
        }
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over configuration
     * changes. It will be used to retain the ImageCache object.
     */
    class RetainFragment
    /**
     * Empty constructor as per the Fragment documentation
     */
        : Fragment() {
        /**
         * The Object we store.
         */
        var mObject: Object? = null

        /**
         * Called to do initial creation of a fragment. First we call our super's implementation of
         * `onCreate`, then we call [setRetainInstance] with `true` to make sure this [Fragment] is
         * retained over a configuration change.
         *
         * @param savedInstanceState If the fragment is being re-created from
         * a previous saved state, this is the state, we do not use.
         */
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Make sure this Fragment is retained over a configuration change
            retainInstance = true
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    companion object {
        /**
         * TAG used for logging and for fragment tag
         */
        private const val TAG = "ImageCache"

        /**
         * Default memory cache size in kilobytes
         */
        private const val DEFAULT_MEM_CACHE_SIZE = 1024 * 5 // 5MB

        /**
         * Default disk cache size in bytes
         */
        private const val DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10 // 10MB

        // Compression settings when writing images to disk cache using Bitmap.Compress
        /** Format of the compressed image JPEG  */
        private val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG

        /** Quality of the compressed image  */
        private const val DEFAULT_COMPRESS_QUALITY = 70

        /** Index of file in cache `Entry`  */
        private const val DISK_CACHE_INDEX = 0

        // Constants to easily toggle various caches
        /** Toggle for enabling memory cache  */
        private const val DEFAULT_MEM_CACHE_ENABLED = true

        /** Toggle for enabling disk cache  */
        private const val DEFAULT_DISK_CACHE_ENABLED = true

        /** If true the disk cache is initialized on main thread, should be false to do in background  */
        private const val DEFAULT_INIT_DISK_CACHE_ON_CREATE = false

        /**
         * Return an [ImageCache] instance. A [RetainFragment] is used to retain the [ImageCache]
         * object across configuration changes such as a change in device orientation. First we
         * search for, or create an instance of the non-UI [RetainFragment] in order to initialize
         * our [RetainFragment] variable `val mRetainFragment`. We initialize our [ImageCache]
         * variable `var imageCache` by retrieving the [RetainFragment.mObject] property of
         * [RetainFragment] field `mRetainFragment` (`mRetainFragment` exists only to hold an
         * object across configuration changes). If `imageCache` is `null` (first time we were
         * called) we set it to a new instance using our [ImageCacheParams] parameter [cacheParams]
         * in the constructor and set the [RetainFragment.mObject] property of `mRetainFragment` to
         * it to save `imageCache` across configuration changes. Finally we return `imageCache` to
         * the caller.
         *
         * @param fragmentManager The fragment manager to use when dealing with the retained fragment.
         * @param cacheParams The cache parameters to use if the [ImageCache] needs instantiation.
         * @return An existing retained [ImageCache] object or a new one if one did not exist
         */
        fun getInstance(
            fragmentManager: FragmentManager,
            cacheParams: ImageCacheParams
        ): ImageCache {

            // Search for, or create an instance of the non-UI RetainFragment
            val mRetainFragment = findOrCreateRetainFragment(fragmentManager)

            // See if we already have an ImageCache stored in RetainFragment
            var imageCache = mRetainFragment.mObject as ImageCache?

            // No existing ImageCache, create one and store it in RetainFragment
            if (imageCache == null) {
                imageCache = ImageCache(cacheParams)
                mRetainFragment.mObject = (imageCache as Object)
            }
            return imageCache
        }

        /**
         * Checks to see if its [Bitmap] parameter [candidate] can be reused by [BitmapFactory]
         * given the options specified in the [BitmapFactory.Options] parameter [targetOptions].
         *
         * If the device is older than KITKAT the bitmap can only be reused if the width of
         * [candidate] is equal to the [BitmapFactory.Options.outWidth] field of [targetOptions],
         * the height is equal to the [BitmapFactory.Options.outHeight] field, and the
         * [BitmapFactory.Options.inSampleSize] field of [targetOptions] is equal to 1 so we just
         * return whether this is `true` or not for devices older the KITKAT.
         *
         * From KITKAT onward we can re-use if the byte size of the new bitmap specified by the
         * parameter [targetOptions] is smaller than the allocation byte count of [candidate],
         * so we calculate [Int] variable `val width` by dividing the [BitmapFactory.Options.outWidth]
         * field of [targetOptions] by its [BitmapFactory.Options.inSampleSize] field and calculate
         * [Int] variable `val height` by dividing the [BitmapFactory.Options.outHeight] field of
         * [targetOptions] by its [BitmapFactory.Options.inSampleSize] field. Then [Int] variable
         * `val byteCount` is obtained by multiplying `width` by `height` by the bytes per pixel
         * count that our method [getBytesPerPixel] calculates from the [Bitmap.Config] that the
         * bitmap [candidate] is using. Finally we return `true` if `byteCount` is less than or equal
         * to the size of the allocated memory used to store the pixels of [candidate].
         *
         * @param candidate - [Bitmap] to check
         * @param targetOptions - Options that have the out* value populated
         * @return `true` if [candidate] can be used for `inBitmap` re-use with [targetOptions].
         */
        @SuppressLint("ObsoleteSdkInt")
        @RequiresApi(VERSION_CODES.KITKAT)
        private fun canUseForInBitmap(
            candidate: Bitmap, targetOptions: BitmapFactory.Options): Boolean {
            if (!Utils.hasKitKat()) {
                // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
                return candidate.width == targetOptions.outWidth &&
                    candidate.height == targetOptions.outHeight &&
                    targetOptions.inSampleSize == 1
            }

            // From Android 4.4 (KitKat) onward we can re-use if the byte size of the new bitmap
            // is smaller than the reusable bitmap candidate allocation byte count.
            val width = targetOptions.outWidth / targetOptions.inSampleSize
            val height = targetOptions.outHeight / targetOptions.inSampleSize
            val byteCount = width * height * getBytesPerPixel(candidate.config!!)
            return byteCount <= candidate.allocationByteCount
        }

        /**
         * Return the byte usage per pixel of a [Bitmap] based on its configuration. We do what
         * amounts to a switch on the value of our [Bitmap.Config] parameter [config]:
         *
         *  * ARGB_8888: we return 4
         *  * RGB_565: we return 2
         *  * ARGB_4444: we return 2
         *  * ALPHA_8: we return 1
         *  * any other value we return 1
         *
         * @param config The bitmap configuration.
         * @return The byte usage per pixel.
         */
        private fun getBytesPerPixel(config: Bitmap.Config): Int {
            return when (config) {
                Bitmap.Config.ARGB_8888 -> {
                    4
                }

                Bitmap.Config.RGB_565 -> {
                    2
                }

                Bitmap.Config.ARGB_4444 -> {
                    2
                }

                Bitmap.Config.ALPHA_8 -> {
                    1
                }

                else -> 1
            }
        }

        /**
         * Get a usable cache directory (external if available, internal otherwise). We set the
         * [String] variable `val cachePath` to the path to the path of the external storage if the
         * external storage is mounted or it is not removable, otherwise we set it to the absolute
         * path to the application specific cache directory. Finally we return a [File] that uses
         * the pathname formed by concatenating `cachePath` to the file separator character followed
         * by our [String] parameter [uniqueName].
         *
         * @param context The context to use
         * @param uniqueName A unique directory name to append to the cache dir
         * @return The cache dir
         */
        fun getDiskCacheDir(context: Context, uniqueName: String): File {
            // Check if media is mounted or storage is built-in, if so, try and use external cache dir
            // otherwise use internal cache dir
            val cachePath = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() ||
                !isExternalStorageRemovable) getExternalCacheDir(context)!!.path else context.cacheDir.path
            return File(cachePath + File.separator + uniqueName)
        }

        /**
         * A hashing method that changes a string (like a URL) into a hash suitable for using as a
         * disk filename. First we declare [String] variable `val cacheKey`, then wrapped in a try
         * block intended to catch [NoSuchAlgorithmException] we initialize [MessageDigest] variable
         * `val mDigest` with a message digest algorithm that implements the "MD5" digest algorithm.
         * We call its [MessageDigest.update] method with the bytes of `key` to update the digest,
         * then set `cacheKey` to the hex string version of the completed digest. The catch block
         * sets `cacheKey` the string value of the hash code of the `key` object. Finally we return
         * `cacheKey` to the caller.
         *
         * @param key key that we want to create a hash key for
         * @return String hashed from `key` that is suitable for using as a disk filename (such as
         * "b402bfa80269c5d1a26fbc0f8c34e3e4")
         */
        fun hashKeyForDisk(key: String): String {
            val cacheKey: String
            cacheKey = try {
                val mDigest = MessageDigest.getInstance("MD5")
                mDigest.update(key.toByteArray())
                bytesToHexString(mDigest.digest())
            } catch (_: NoSuchAlgorithmException) {
                key.hashCode().toString()
            }
            return cacheKey
        }

        /**
         * Converts an array of bytes into a string representing the hexadecimal values of each byte
         * in order. First we initialize [StringBuilder] variable `val sb` with a new instance. Then
         * we loop over `i` for all of the bytes in [ByteArray] parameter [bytes]. We set [String]
         * variable `val hex` to the hex value of [bytes] masked with 0xFF (to remove sign extension),
         * and if the length of `hex` is 1 we append a leading zero to `sb` before appending `hex`.
         * When done with all of the bytes in [bytes] we return the string value of `sb` to the
         * caller.
         *
         * @param bytes array of bytes to convert to hex characters
         * @return Hex string version of the [ByteArray] parameter [bytes]
         */
        private fun bytesToHexString(bytes: ByteArray): String {
            // http://stackoverflow.com/questions/332079
            val sb = StringBuilder()
            for (i in bytes.indices) {
                val hex = Integer.toHexString(0xFF and bytes[i].toInt())
                if (hex.length == 1) {
                    sb.append('0')
                }
                sb.append(hex)
            }
            return sb.toString()
        }

        /**
         * Get the size in bytes of a [Bitmap] in a [BitmapDrawable]. Note that from Android 4.4
         * (KitKat) onward this returns the allocated memory size of the [Bitmap] which can be larger
         * than the actual bitmap data byte count (in the case it was re-used).
         *
         * First we initialize [Bitmap] variable `val bitmap` with the bitmap used by [BitmapDrawable]
         * parameter [value] to render. If the device is at least KITKAT we return the size of the
         * allocated memory used to store the pixels in `bitmap`.
         *
         * If the device is at least HONEYCOMB_MR1 we return the minimum number of bytes that can be
         * used to store the pixels in `bitmap`.
         *
         * Otherwise we return the number of bytes between rows in `bitmap` (the value returned by
         * [Bitmap.getRowBytes], aka kotlin `rowBytes` property) times the height of `bitmap`.
         *
         * @param value [BitmapDrawable] whose [Bitmap] size we want.
         * @return [Bitmap] size in bytes.
         */
        @SuppressLint("ObsoleteSdkInt")
        @RequiresApi(VERSION_CODES.KITKAT)
        fun getBitmapSize(value: BitmapDrawable): Int {
            val bitmap: Bitmap = value.bitmap

            // From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
            // larger than bitmap byte count.
            if (Utils.hasKitKat()) {
                return bitmap.allocationByteCount
            }
            return if (Utils.hasHoneycombMR1()) {
                bitmap.byteCount
            } else bitmap.rowBytes * bitmap.height

            // Pre HC-MR1
        }

        /**
         * Check if external storage is built-in or removable. If the device is at least GINGERBREAD
         * we return the value returned by [Environment.isExternalStorageRemovable], otherwise we
         * return true.
         *
         * @return `true` if external storage is removable (like an SD card), `false`
         * otherwise.
         */
        @get:RequiresApi(VERSION_CODES.GINGERBREAD)
        val isExternalStorageRemovable: Boolean
            get() = if (Utils.hasGingerbread()) {
                Environment.isExternalStorageRemovable()
            } else true

        /**
         * Get the external app cache directory. If the device is at least FROYO, we return the value
         * returned by the [Context.getExternalCacheDir] method of our parameter [Context] parameter
         * [context]. Otherwise we initialize [String] variable `val cacheDir` by concatenating the
         * string "/Android/data/" followed by our package name followed by the string "/cache/". We
         * then return a new instance of [File] for the path formed by concatenating the path string
         * name for the external storage directory [File] to our string `cacheDir`.
         *
         * @param context The [Context] to use.
         * @return The external cache dir
         */
        @RequiresApi(VERSION_CODES.FROYO)
        fun getExternalCacheDir(context: Context): File? {
            if (Utils.hasFroyo()) {
                return context.externalCacheDir
            }

            // Before Froyo we need to construct the external cache dir ourselves
            val cacheDir = "/Android/data/" + context.packageName + "/cache/"
            return File(Environment.getExternalStorageDirectory().path + cacheDir)
        }

        /**
         * Check how much usable space is available at a given path. If the device is at least
         * GINGERBREAD we return the value returned by the [File.getUsableSpace] method of our
         * [File] parameter [path]. Otherwise we initialize [StatFs] variable `val stats` with
         * a new instance for the path of [path] (Constructs a new StatFs for looking at the stats
         * of the filesystem at path. Upon construction, the stat of the file system will be performed,
         * and the values retrieved available from the methods on this class). Finally we return the
         * size, in bytes, of a block on the file system times the number of blocks that are free on
         * the file system and available to applications.
         *
         * @param path The path to check
         * @return The space available in bytes
         */
        @SuppressLint("UsableSpace")
        @RequiresApi(VERSION_CODES.GINGERBREAD)
        fun getUsableSpace(path: File): Long {
            if (Utils.hasGingerbread()) {
                return path.usableSpace
            }
            val stats = StatFs(path.path)
            return stats.blockSize.toLong() * stats.availableBlocks.toLong()
        }

        /**
         * Locate an existing instance of this [Fragment] or if not found, create and add it using
         * the [FragmentManager] parameter [fm]. First we use [fm] to search for a fragment with the
         * tag [TAG] ("ImageCache") to initialize [RetainFragment] variable `var mRetainFragment`.
         * Then if `mRetainFragment` is `null` we set it to a new instance then use [fm] to create
         * a [FragmentTransaction] which we chain to `add` `mRetainFragment` using [TAG] as its tag,
         * and chain that to `commit` it (since `mRetainFragment` has no UI we can allow it to be
         * committed after an activity's state is saved). Finally we return `mRetainFragment` to the
         * caller.
         *
         * @param fm The [FragmentManager] we should use.
         * @return The existing instance of the [Fragment] or the new instance if just
         * created.
         */
        private fun findOrCreateRetainFragment(fm: FragmentManager): RetainFragment {
            //BEGIN_INCLUDE(find_create_retain_fragment)
            // Check to see if we have retained the worker fragment.
            var mRetainFragment = fm.findFragmentByTag(TAG) as RetainFragment?

            // If not retained (or first time running), we need to create and add it.
            if (mRetainFragment == null) {
                mRetainFragment = RetainFragment()
                fm.beginTransaction().add(mRetainFragment, TAG).commitAllowingStateLoss()
            }
            return mRetainFragment
            //END_INCLUDE(find_create_retain_fragment)
        }
    }
}