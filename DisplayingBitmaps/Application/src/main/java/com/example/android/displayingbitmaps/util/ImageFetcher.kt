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
@file:Suppress(
    "DEPRECATION",
    "ReplaceNotNullAssertionWithElvisReturn",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.displayingbitmaps.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.widget.Toast
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig
import com.example.android.displayingbitmaps.R
import com.example.android.displayingbitmaps.util.DiskLruCache.Snapshot
import  com.example.android.displayingbitmaps.util.ImageResizer.Companion.decodeSampledBitmapFromDescriptor
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A simple subclass of [ImageResizer] that fetches and resizes images fetched from a URL.
 */
class ImageFetcher : ImageResizer {
    /**
     * Handle to our LRU disk cache manager.
     */
    private var mHttpDiskCache: DiskLruCache? = null

    /**
     * [File] used to access our disk cache directory.
     */
    private var mHttpCacheDir: File? = null

    /**
     * Flag used to indicate that the disk cache has not been initialized (if `true` initialization
     * has not been done yet, if `false` we can start using the disk cache)
     */
    private var mHttpDiskCacheStarting = true

    /**
     * Lock object we use to synchronize disk cache use
     */
    private val mHttpDiskCacheLock = Object()

    /**
     * Initialize providing a target image width and height for the processing images. First we call
     * our super's constructor, then we call our method [initialize] to initialize ourselves.
     *
     * @param context [Context] to use to access resources
     * @param imageWidth width of the images
     * @param imageHeight height of the images
     */
    @Suppress("unused")
    constructor(
        context: Context,
        imageWidth: Int,
        imageHeight: Int
    ) : super(context, imageWidth, imageHeight) {
        initialize(context)
    }

    /**
     * Initialize providing a single target image size (used for both width and height). First we call
     * our super's constructor, then we call our method [initialize] to initialize ourselves.
     *
     * @param context [Context] to use to access resources
     * @param imageSize width and height of images.
     */
    constructor(context: Context, imageSize: Int) : super(context, imageSize) {
        initialize(context)
    }

    /**
     * Initializes our instance. First we call [checkConnection] with [context] to perform a simple
     * network connection check (toasting "No network connection found" and logging the error if
     * it fails). Then we initialize [File] field [mHttpCacheDir] with a [File] that accesses the
     * subdirectory HTTP_CACHE_DIR ("http") in our packages cache directory.
     *
     * @param context [Context] to use to access resources
     */
    private fun initialize(context: Context) {
        checkConnection(context)
        mHttpCacheDir = ImageCache.getDiskCacheDir(context = context, uniqueName = HTTP_CACHE_DIR)
    }

    /**
     * Initializes the disk cache. First we call our super's implementation of `initDiskCacheInternal`
     * then we call our method [initHttpDiskCache] to initialize our cache of http downloads.
     */
    override fun initDiskCacheInternal() {
        super.initDiskCacheInternal()
        initHttpDiskCache()
    }

    /**
     * Initialize our cache of http downloads. If our cache directory ([File] field [mHttpCacheDir])
     * does not exist yet, we call its [File.mkdirs] method to create it on disk. Then synchronized
     * on [Object] field [mHttpDiskCacheLock] we check that the usable space on the disk used for
     * [mHttpCacheDir] is greater than [HTTP_CACHE_SIZE], and if so wrapped in a try block intended
     * to catch [IOException] we initialize [DiskLruCache] field [mHttpDiskCache] by opening a disk
     * cache in the directory [mHttpCacheDir] of size [HTTP_CACHE_SIZE] with the app version of 1,
     * and the value count of 1 (1 file per cache entry). If we catch [IOException] we set
     * [mHttpDiskCache] to `null`.
     *
     * We then set [Boolean] field [mHttpDiskCacheStarting] to `false` and wake up all threads that
     * are waiting the monitor of [mHttpDiskCacheLock] before exiting the synchronized block.
     */
    private fun initHttpDiskCache() {
        if (!mHttpCacheDir!!.exists()) {
            mHttpCacheDir!!.mkdirs()
        }
        synchronized(mHttpDiskCacheLock) {
            if (ImageCache.getUsableSpace(mHttpCacheDir!!) > HTTP_CACHE_SIZE) {
                try {
                    mHttpDiskCache =
                        DiskLruCache.open(mHttpCacheDir!!, 1, 1, HTTP_CACHE_SIZE.toLong())
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "HTTP cache initialized")
                    }
                } catch (e: IOException) {
                    mHttpDiskCache = null
                }
            }
            mHttpDiskCacheStarting = false
            mHttpDiskCacheLock.notifyAll()
        }
    }

    /**
     * Closes the http cache and deletes all of its stored values, then initializes a new http cache.
     * First we call our super's implementation of `clearCacheInternal()`, then synchronized on
     * [Object] field [mHttpDiskCacheLock] we check that [DiskLruCache] field [mHttpDiskCache] is
     * not `null` and is not closed before, wrapped in a try block intended to catch [IOException]
     * we call the [File.delete] method of [mHttpDiskCache] to close the cache and delete all of its
     * stored values. If we catch [IOException] we just log the error. We then set [mHttpDiskCache]
     * to `null` and set [mHttpDiskCacheStarting] to `true` before calling [initHttpDiskCache] to
     * Initialize a new cache for http downloads.
     */
    override fun clearCacheInternal() {
        super.clearCacheInternal()
        synchronized(mHttpDiskCacheLock) {
            if (mHttpDiskCache != null && !mHttpDiskCache!!.isClosed) {
                try {
                    mHttpDiskCache!!.delete()
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "HTTP cache cleared")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "clearCacheInternal - $e")
                }
                mHttpDiskCache = null
                mHttpDiskCacheStarting = true
                initHttpDiskCache()
            }
        }
    }

    /**
     * Forces buffered operations of [DiskLruCache] field [mHttpDiskCache] to the filesystem. First
     * we call our super's implementation of `flushCacheInternal`, then synchronized on [Object]
     * field [mHttpDiskCacheLock] we check that [mHttpDiskCache] is not `null` before, wrapped in
     * a try block intended to catch [IOException] we call the [DiskLruCache.flush] method of
     * [mHttpDiskCache]. If we catch [IOException] we just log the error.
     */
    override fun flushCacheInternal() {
        super.flushCacheInternal()
        synchronized(mHttpDiskCacheLock) {
            if (mHttpDiskCache != null) {
                try {
                    mHttpDiskCache!!.flush()
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "HTTP cache flushed")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "flush - $e")
                }
            }
        }
    }

    /**
     * Closes the http disk cache. First we call our super's implementation of `closeCacheInternal`,
     * then synchronized on [Object] field [mHttpDiskCacheLock] we check that [DiskLruCache] field
     * [mHttpDiskCache] is not `null` before, wrapped in a try block intended to catch [IOException]
     * if [mHttpDiskCache] is not closed we call its [DiskLruCache.close] method and set it to `null`.
     * If we catch [IOException] we just log the error.
     */
    override fun closeCacheInternal() {
        super.closeCacheInternal()
        synchronized(mHttpDiskCacheLock) {
            if (mHttpDiskCache != null) {
                try {
                    if (!mHttpDiskCache!!.isClosed) {
                        mHttpDiskCache!!.close()
                        mHttpDiskCache = null
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "HTTP cache closed")
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "closeCacheInternal - $e")
                }
            }
        }
    }

    /**
     * Simple network connection check. We initialize [ConnectivityManager] variable `val cm` with a
     * handle to the system level service [Context.CONNECTIVITY_SERVICE]. Then we use it to initialize
     * our [NetworkInfo] variable `val networkInfo` with details about the currently active default
     * data network. If `networkInfo` is `null` or its [NetworkInfo.isConnectedOrConnecting] method
     * returns `false` we toast "No network connection found" and log the error.
     *
     * @param context [Context] to use to access resources
     */
    private fun checkConnection(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = cm.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting) {
            Toast.makeText(context, R.string.no_network_connection_toast, Toast.LENGTH_LONG).show()
            Log.e(TAG, "checkConnection - no connection found")
        }
    }

    /**
     * The main process method, which will be called by the [ImageWorker] in its [AsyncTask]
     * background thread. We initialize [String] variable `val key` with a hash key generated by
     * calling the [ImageCache.hashKeyForDisk] method with our [String] parameter [data]. We
     * initialize [FileDescriptor] variable `var fileDescriptor` to `null`, and [FileInputStream]
     * variable `var fileInputStream` to `null` then declare [Snapshot] variable `var snapshot`.
     *
     * Then synchronized on [Object] field [mHttpDiskCacheLock] we loop forever waiting for
     * [Boolean] field [mHttpDiskCacheStarting] to be `false`, calling the [Object.wait] method
     * of [mHttpDiskCacheLock] in a try block intended to catch and ignore [InterruptedException]
     * (this `wait` is designed to delay until our http disk cache is initialized).
     *
     * If [DiskLruCache] field [mHttpDiskCache] is not `null` we enter a try block where we set
     * `snapshot` to the value fetched from [mHttpDiskCache] for key `key`. If `snapshot` is `null`
     * we initialize [DiskLruCache.Editor] variable `val editor` with an [mHttpDiskCache] editor for
     * `key`, and if `editor` is not `null` we call the method [downloadUrlToStream] to download the
     * url given by `data` to an output stream created to write to the [DISK_CACHE_INDEX] file
     * of the `Entry` for key branching on the results of this call to call the `commit` method of
     * `editor` if the download is successful, or the `abort` method of `editor` if it fails. We
     * then try again to set `snapshot` to the value fetched from [mHttpDiskCache] for key `key`.
     *
     * If `snapshot` is not `null` now we set `fileInputStream` to the input stream for the file
     * with index [DISK_CACHE_INDEX] of `snapshot` and set `fileDescriptor` to the file descriptor
     * for `fileInputStream`.
     *
     * The catch blocks for the try block are:
     *
     *  * [IOException]: we log the error
     *
     *  * [IllegalStateException]: we log the error
     *
     * And in the finally block we check if `fileDescriptor` is `null` but `fileInputStream`
     * is not `null` before wrapped in a try block intended to catch and ignore `IOException` we
     * call the [FileInputStream.close] method of `fileInputStream`.
     *
     * On exiting the synchronized block we initialize [Bitmap] variable `var bitmap` to null, and
     * if `fileDescriptor` is not `null` we set `bitmap` to the decoded image read from the file
     * descriptor by the method [decodeSampledBitmapFromDescriptor] using [Int] field [mImageWidth]
     * as the width, [Int] field [mImageHeight] as the height and using the recycled bitmap returned
     * by the [imageCache] property if possible.
     *
     * Then if `fileInputStream` is not `null`, wrapped in a try block intended to catch and ignore
     * [IOException] we call the [FileInputStream.close] method of `fileInputStream`.
     *
     * Finally we return `bitmap` to the caller.
     *
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized [Bitmap]
     */
    private fun processBitmap(data: String): Bitmap? {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - $data")
        }
        val key: String = ImageCache.hashKeyForDisk(key = data)
        var fileDescriptor: FileDescriptor? = null
        var fileInputStream: FileInputStream? = null
        var snapshot: Snapshot?
        synchronized(mHttpDiskCacheLock) {

            // Wait for disk cache to initialize
            while (mHttpDiskCacheStarting) {
                @Suppress("CatchMayIgnoreException")
                try {
                    mHttpDiskCacheLock.wait()
                } catch (e: InterruptedException) {
                }
            }
            if (mHttpDiskCache != null) {
                try {
                    snapshot = mHttpDiskCache!![key]
                    if (snapshot == null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "processBitmap, not found in http cache, downloading...")
                        }
                        val editor: DiskLruCache.Editor? = mHttpDiskCache!!.edit(key)
                        if (editor != null) {
                            if (downloadUrlToStream(
                                    urlString = data,
                                    outputStream = editor.newOutputStream(DISK_CACHE_INDEX)
                                )
                            ) {
                                editor.commit()
                            } else {
                                editor.abort()
                            }
                        }
                        snapshot = mHttpDiskCache!![key]
                    }
                    if (snapshot != null) {
                        fileInputStream =
                            snapshot.getInputStream(DISK_CACHE_INDEX) as FileInputStream
                        fileDescriptor = fileInputStream.fd
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "processBitmap - $e")
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "processBitmap - $e")
                } finally {
                    if (fileDescriptor == null && fileInputStream != null) {
                        @Suppress("CatchMayIgnoreException")
                        try {
                            fileInputStream.close()
                        } catch (e: IOException) {
                        }
                    }
                }
            }
        }
        var bitmap: Bitmap? = null
        if (fileDescriptor != null) {
            bitmap = decodeSampledBitmapFromDescriptor(
                fileDescriptor = fileDescriptor,
                reqWidth = mImageWidth,
                reqHeight = mImageHeight,
                cache = imageCache
            )
        }
        if (fileInputStream != null) {
            @Suppress("CatchMayIgnoreException")
            try {
                fileInputStream.close()
            } catch (e: IOException) {
            }
        }
        return bitmap
    }

    /**
     * Processing logic used by [ImageWorker]. We just convert our parameter to its string
     * value and pass the call on to our [processBitmap] method.
     *
     * @param data [Object] version of http url?
     * @return [Bitmap] downloaded
     */
    @Suppress("RedundantNullableReturnType") // The method we override returns nullable
    override fun processBitmap(data: Any?): Bitmap? {
        return processBitmap(data.toString())!!
    }

    /**
     * Download a [Bitmap] from a URL and write the content to an output stream. First we call our
     * method [disableConnectionReuseIfNecessary] to work around a bug if our device is older than
     * FROYO. We initialize [HttpURLConnection] variable `var urlConnection`, [BufferedOutputStream]
     * variable `var out`, and [BufferedInputStream] variable `var inputStream` to `null`. Wrapped
     * in a try block intended to catch and log [IOException] we initialize [URL] variable `val url`
     * to an url constructed from our [String] parameter [urlString], and set `urlConnection` to the
     * [HttpURLConnection] created when opening a connection to `url`. We set `inputStream` to a
     * [BufferedInputStream] created from the input stream of `urlConnection`, and `out` to a
     * [BufferedOutputStream] created from our [OutputStream] parameter [outputStream]. We declare
     * [Int] variable `var b`, then while using the [BufferedInputStream.read] method of `inputStream`
     * to read bytes into `b`, we write each `b` read to `out`, returning `true` to the caller when
     * -1 is read (end of stream). In the finally block if `urlConnection` is not null we call its
     * [HttpURLConnection.disconnect] method to disconnect (indicates that other requests to the
     * server are unlikely in the near future). Then wrapped in a try block intended to catch and
     * ignore [IOException] we close `out` if it is not `null`, and close `inputStream` if it is not
     * `null`. We return `false` if we caught an [IOException] in our read/write try block.
     *
     * @param urlString The [URL] to fetch
     * @return `true` if successful, `false` otherwise
     */
    fun downloadUrlToStream(urlString: String?, outputStream: OutputStream?): Boolean {
        disableConnectionReuseIfNecessary()
        var urlConnection: HttpURLConnection? = null
        var out: BufferedOutputStream? = null
        var inputStream: BufferedInputStream? = null
        try {
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            inputStream = BufferedInputStream(urlConnection.inputStream, IO_BUFFER_SIZE)
            out = BufferedOutputStream(outputStream, IO_BUFFER_SIZE)
            var b: Int
            while (inputStream.read().also { b = it } != -1) {
                out.write(b)
            }
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Error in downloadBitmap - $e")
        } finally {
            urlConnection?.disconnect()
            @Suppress("CatchMayIgnoreException")
            try {
                out?.close()
                inputStream?.close()
            } catch (e: IOException) {
            }
        }
        return false
    }

    companion object {
        /** TAG used for logging  */
        private const val TAG = "ImageFetcher"

        /** Size of our disk cache  */
        private const val HTTP_CACHE_SIZE = 10 * 1024 * 1024 // 10MB

        /** Directory name for our disk cache  */
        private const val HTTP_CACHE_DIR = "http"

        /** Size of buffers for both `BufferedInputStream` and `BufferedOutputStream`  */
        private const val IO_BUFFER_SIZE = 8 * 1024

        /**
         * Index of the file in the `DiskLruCache` `Entry` that we use.
         */
        private const val DISK_CACHE_INDEX = 0

        /**
         * Workaround for bug pre-Froyo, see here for more info:
         * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
         * If the device is older than FROYO we set the system property "http.keepAlive" to "false".
         */
        @SuppressLint("ObsoleteSdkInt")
        fun disableConnectionReuseIfNecessary() {
            // HTTP connection reuse which was buggy pre-froyo
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                System.setProperty("http.keepAlive", "false")
            }
        }
    }
}