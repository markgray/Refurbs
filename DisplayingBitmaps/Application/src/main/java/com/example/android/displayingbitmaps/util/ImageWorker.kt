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

package com.example.android.displayingbitmaps.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.widget.AbsListView
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.android.common.logger.Log
import com.example.android.displayingbitmaps.BuildConfig
import com.example.android.displayingbitmaps.ui.ImageDetailActivity
import com.example.android.displayingbitmaps.ui.ImageGridFragment
import com.example.android.displayingbitmaps.util.ImageCache.ImageCacheParams
import java.lang.ref.WeakReference
import java.net.URL
import androidx.core.graphics.drawable.toDrawable

/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an
 * ImageView. It handles things like using a memory and disk cache, running the work in a background
 * thread and setting a placeholder image.
 */
abstract class ImageWorker protected constructor(context: Context) {
    /**
     * [ImageCache] used to handle disk and memory bitmap caching.
     */
    protected var imageCache: ImageCache? = null
        private set

    /**
     * The cache parameters to use to initialize the cache
     */
    private var mImageCacheParams: ImageCacheParams? = null

    /**
     * [Bitmap] used as placeholder while real image is fetched
     */
    private var mLoadingBitmap: Bitmap? = null

    /**
     * If set to `true`, the image will fade-in once it has been loaded by the background thread.
     */
    private var mFadeInBitmap: Boolean = true

    /**
     * Flag used to cancel background tasks when the app is paused
     */
    private var mExitTasksEarly = false

    /**
     * Flag used to pause the background task when doing so will improve performance, `true` causes
     * it to wait on [mPauseWorkLock] and loop until it changes to `false`
     */
    protected var mPauseWork: Boolean = false

    /**
     * Lock used to synchronize on the changing of the [mExitTasksEarly] flag
     */
    private val mPauseWorkLock = Object()

    /**
     * [Resources] object to use to access resources.
     */
    @JvmField
    @Suppress("JoinDeclarationAndAssignment") // Easier to debug this way
    protected var mResources: Resources

    /**
     * Our constructor, we just initialize our field `Resources mResources` with a Resources
     * instance for the application's package.
     */
    init {
        mResources = context.resources
    }

    /**
     * Load an image specified by the data parameter into an [ImageView] (override
     * [ImageWorker.processBitmap] to define the processing logic). A memory and disk cache will be
     * used if an [ImageCache] has been added using [ImageWorker.addImageCache]. If the image is
     * found in the memory cache, it is set immediately, otherwise an [AsyncTask] will be created
     * to asynchronously load the bitmap.
     *
     * If our [Any] parameter [data] is `null` we return having done nothing. We initialize our
     * [BitmapDrawable] variable `var value` to `null`, and if our [ImageCache] field [imageCache]
     * is not `null` we try set `value` to a [Bitmap] from our memory cache that would be stored
     * under the key given by the string value of [data]. If `value` is not `null` we set `value`
     * as the content of our [ImageView] parameter [imageView] and if our [OnImageLoadedListener]
     * parameter [listener] is not `null` we call its [OnImageLoadedListener.onImageLoaded] overload
     * with `true` to notify it that the image was successfully loaded. If `value` is `null` on the
     * other hand we check if current work has been canceled or if there was no work in progress on
     * [imageView] before initializing our [BitmapWorkerTask] variable `val task` with a background
     * task to load from [data] into [imageView] with [listener] as its [OnImageLoadedListener]. We
     * initialize [AsyncDrawable] variable `val asyncDrawable` to serve as a placeholder using
     * [Bitmap] field [mLoadingBitmap] as the temporary image while the work is in progress,
     * specifying `task` as the worker task. We then set `asyncDrawable` as the content of
     * [ImageView] parameter [imageView]. We then execute `task` on the executor
     * [AsyncTask.DUAL_THREAD_EXECUTOR].
     *
     * @param data The [URL] of the image to download.
     * @param imageView The [ImageView] to bind the downloaded image to.
     * @param listener A listener that will be called back once the image has been loaded.
     */
    @JvmOverloads
    fun loadImage(
        data: Any?,
        imageView: ImageView,
        listener: OnImageLoadedListener? = null
    ) {
        if (data == null) {
            return
        }
        var value: BitmapDrawable? = null
        if (imageCache != null) {
            value = imageCache!!.getBitmapFromMemCache(data.toString())
        }
        if (value != null) {
            // Bitmap found in memory cache
            imageView.setImageDrawable(value)
            listener?.onImageLoaded(true)
        } else if (cancelPotentialWork(data, imageView)) {
            //BEGIN_INCLUDE(execute_background_task)
            val task = BitmapWorkerTask(data, imageView, listener)
            val asyncDrawable = AsyncDrawable(mResources, mLoadingBitmap, task)
            imageView.setImageDrawable(asyncDrawable)

            // NOTE: This uses a custom version of AsyncTask that has been pulled from the
            // framework and slightly modified. Refer to the docs at the top of the class
            // for more info on what was changed.
            task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR)
            //END_INCLUDE(execute_background_task)
        }
    }

    /**
     * Set placeholder [Bitmap] that shows when the the background thread is running. We just save
     * our [Bitmap] parameter [bitmap] in our [Bitmap] field [mLoadingBitmap].
     *
     * @param bitmap placeholder [Bitmap] to use
     */
    fun setLoadingImage(bitmap: Bitmap?) {
        mLoadingBitmap = bitmap
    }

    /**
     * Set placeholder [Bitmap] that shows when the the background thread is running. We set our
     * [Bitmap] field [mLoadingBitmap] to the [Bitmap] decoded from the image whose resource id
     * is our [Int] parameter [resId].
     *
     * @param resId resource id of image.
     */
    fun setLoadingImage(resId: Int) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId)
    }

    /**
     * Adds an [ImageCache] to this [ImageWorker] to handle disk and memory [Bitmap] caching. We
     * save our [ImageCache.ImageCacheParams] parameter [cacheParams] in our field [mImageCacheParams]
     * and use our [FragmentManager] parameter [fragmentManager] to set our [ImageCache] field
     * [imageCache] to an [ImageCache] constructed using [mImageCacheParams] as its parameters. We
     * create a new instance of [CacheAsyncTask] and execute it to initialize the disk cache.
     *
     * @param fragmentManager [FragmentManager] to use when dealing with the retained fragment.
     * @param cacheParams The cache parameters to use for the [ImageCache].
     */
    fun addImageCache(
        fragmentManager: FragmentManager?,
        cacheParams: ImageCacheParams?
    ) {
        mImageCacheParams = cacheParams
        imageCache = ImageCache.getInstance(fragmentManager!!, mImageCacheParams!!)
        CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE)
    }

    /**
     * Adds an [ImageCache] to this [ImageWorker] to handle disk and memory [Bitmap] caching. We set
     * our [ImageCacheParams] field [mImageCacheParams] to an instance that will use the directory
     * of our [String] parameter [diskCacheDirectoryName], and use the [FragmentManager] for
     * interacting with fragments associated with this activity when creating an instance of
     * [ImageCache] that uses [mImageCacheParams] as its configuration parameters which we then use
     * to initialize our [ImageCache] field [imageCache]. We create a new instance of [CacheAsyncTask]
     * and execute it to initialize the disk cache.
     *
     * @param activity [FragmentActivity] to use for [Context].
     * @param diskCacheDirectoryName A unique subdirectory name that will be appended to the
     * application cache directory.
     */
    fun addImageCache(
        activity: FragmentActivity,
        diskCacheDirectoryName: String?
    ) {
        mImageCacheParams = ImageCacheParams(activity, diskCacheDirectoryName!!)
        imageCache = ImageCache.getInstance(activity.supportFragmentManager, mImageCacheParams!!)
        CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE)
    }

    /**
     * If set to `true`, the image will fade-in once it has been loaded by the background thread. We
     * just save our [Boolean] parameter [fadeIn] in our [Boolean] field [mFadeInBitmap].
     *
     * @param fadeIn `true` to cause the image to fade-in once it has been loaded by the background
     * thread.
     */
    fun setImageFadeIn(fadeIn: Boolean) {
        mFadeInBitmap = fadeIn
    }

    /**
     * Sets our [Boolean] flag field [mExitTasksEarly] to our [Boolean] parameter [exitTasksEarly]
     * so that our background tasks will terminate before completing, and calls our method
     * [setPauseWork] with `false` to un-pause any paused work so that they can read the new value
     * of [mExitTasksEarly].
     *
     * @param exitTasksEarly `true` to cause our background tasks to terminate before completing.
     */
    fun setExitTasksEarly(exitTasksEarly: Boolean) {
        mExitTasksEarly = exitTasksEarly
        setPauseWork(false)
    }

    /**
     * Subclasses should override this to define any processing or work that must happen to produce
     * the final bitmap. This will be executed in a background thread and be long running. For
     * example, you could resize a large bitmap here, or pull down an image from the network.
     *
     * @param data The data to identify which image to process, as provided by
     * [ImageWorker.loadImage]
     * @return The processed [Bitmap]
     */
    protected abstract fun processBitmap(data: Any?): Bitmap?

    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    private inner class BitmapWorkerTask : AsyncTask<Void?, Void?, BitmapDrawable?> {
        /**
         * [Any] object we are processing in the background by calling the overloaded
         * [processBitmap] method of [ImageWorker] subclasses with this as the argument.
         * Its string value is also used to access the caches. It is set by the constructor.
         */
        var mData: Any?

        /**
         * [WeakReference] to the [ImageView] we are constructed to load into
         */
        private val imageViewReference: WeakReference<ImageView>

        /**
         * [OnImageLoadedListener] whose [OnImageLoadedListener.onImageLoaded] callback we are to
         * call in our [onPostExecute] method if it is not `null`. Set in our constructor.
         */
        private val mOnImageLoadedListener: OnImageLoadedListener?

        /**
         * Our constructor. We save our [Any] parameter [data] in our field [mData], and initialize
         * our [WeakReference] to an [ImageView] field [imageViewReference] with a [WeakReference]
         * to our [ImageView] parameter [imageView], then set our [OnImageLoadedListener] field
         * [mOnImageLoadedListener] to `null`.
         *
         * @param data [Any] object we are to process in the background
         * @param imageView [ImageView] we are bound to load.
         */
        @Suppress("unused")
        constructor(data: Any, imageView: ImageView) {
            mData = data
            imageViewReference = WeakReference(imageView)
            mOnImageLoadedListener = null
        }

        /**
         * Our constructor. We save our [Any] parameter [data] in our field [mData], and initialize
         * our [WeakReference] to an [ImageView] field [imageViewReference] with a [WeakReference]
         * to our [ImageView] parameter [imageView], then set our [OnImageLoadedListener] field
         * [mOnImageLoadedListener] to our [OnImageLoadedListener] parameter [listener].
         *
         * @param data [Any] object we are to process in the background
         * @param imageView [ImageView] we are bound to load.
         * @param listener [OnImageLoadedListener] whose [OnImageLoadedListener.onImageLoaded]
         * callback we are to call when done
         */
        constructor(data: Any, imageView: ImageView, listener: OnImageLoadedListener?) {
            mData = data
            imageViewReference = WeakReference(imageView)
            mOnImageLoadedListener = listener
        }

        /**
         * Background processing. We initialize [String] variable `val dataString` with the string
         * value of our [Any] field [mData], initialize our [Bitmap] variable `var bitmap` to `null`,
         * and our [BitmapDrawable] variable `var drawable` to `null`.
         *
         * Synchronized on [Object] field [mPauseWorkLock] we loop as long as the [Boolean] field
         * [mPauseWork] is `true` and our [isCancelled] property is `false` calling the [Object.wait]
         * method of [mPauseWorkLock] in a try block intended to catch and ignore [InterruptedException].
         * (Waiting if work is paused and the task is not cancelled).
         *
         * Then if [ImageCache] field [imageCache] is not `null`, and we are not canceled, and we
         * have an image view attached to this task, and the [Boolean] field [mExitTasksEarly] is
         * `false` we set `bitmap` to the bitmap retrieved from the disk cache using `dataString`
         * as the key.
         *
         * If `bitmap` is null (our bitmap was not cached), and we are not canceled, and we have
         * an image view attached to this task, and the [mExitTasksEarly] field is `false` we set
         * `bitmap` to the bitmap returned by the overridden method [processBitmap] when passed
         * [mData] as its `data` argument.
         *
         * If `bitmap` is not null we set `drawable` as follows:
         *
         *  * Device is newer than HONEYCOMB: we create a standard [BitmapDrawable] from `bitmap`
         *  to set `drawable`.
         *
         *  * Older than HONEYCOMB: we wrap `bitmap` in a [RecyclingBitmapDrawable] to set
         *  `drawable`.
         *
         * In either case, if [ImageCache] field [imageCache] is not `null` we add `drawable` to the
         * cache stored under the key `dataString`.
         *
         * Finally we return `drawable` to the caller (it will be the parameter passed to the
         * [onPostExecute] method).
         *
         * @param params The parameters of the task, not used.
         * @return [BitmapDrawable] we downloaded and processed (or found in disk cache).
         */
        override fun doInBackground(vararg params: Void?): BitmapDrawable {
            //BEGIN_INCLUDE(load_bitmap_in_background)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground - starting work")
            }
            val dataString = mData.toString()
            var bitmap: Bitmap? = null
            var drawable: BitmapDrawable? = null

            // Wait here if work is paused and the task is not cancelled
            synchronized(mPauseWorkLock) {
                while (mPauseWork && !isCancelled) {
                    @Suppress("CatchMayIgnoreException")
                    try {
                        mPauseWorkLock.wait()
                    } catch (e: InterruptedException) {
                    }
                }
            }

            // If the image cache is available and this task has not been cancelled by another
            // thread and the ImageView that was originally bound to this task is still bound back
            // to this task and our "exit early" flag is not set then try and fetch the bitmap from
            // the cache
            if (imageCache != null && !isCancelled && attachedImageView != null && !mExitTasksEarly) {
                bitmap = imageCache!!.getBitmapFromDiskCache(dataString)
            }

            // If the bitmap was not found in the cache and this task has not been cancelled by
            // another thread and the ImageView that was originally bound to this task is still
            // bound back to this task and our "exit early" flag is not set, then call the main
            // process method (as implemented by a subclass)
            if (bitmap == null && !isCancelled && attachedImageView != null && !mExitTasksEarly) {
                bitmap = processBitmap(data = mData)
            }

            // If the bitmap was processed and the image cache is available, then add the processed
            // bitmap to the cache for future use. Note we don't check if the task was cancelled
            // here, if it was, and the thread is still running, we may as well add the processed
            // bitmap to our cache as it might be used again in the future
            if (bitmap != null) {
                drawable = if (Utils.hasHoneycomb()) {
                    // Running on Honeycomb or newer, so wrap in a standard BitmapDrawable
                    bitmap.toDrawable(mResources)
                } else {
                    // Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
                    // which will recycle auto-magically
                    RecyclingBitmapDrawable(mResources, bitmap)
                }
                if (imageCache != null) {
                    imageCache!!.addBitmapToCache(data = dataString, value = drawable)
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground - finished work")
            }
            return drawable ?: BitmapDrawable()
            //END_INCLUDE(load_bitmap_in_background)
        }

        /**
         * Once the image is processed, associates it to the imageView. First we initialize our
         * [BitmapDrawable] variable `var valueLocal` to our [BitmapDrawable] parameter [result], and
         * our [Boolean] variable `var success` to `false`. If cancel was called on this task or the
         * [mExitTasksEarly] "exit early" flag is set we set `valueLocal` to `null` (we will not
         * bother to update our image view). We then initialize [ImageView] variable `val imageView`
         * to the image view attached to this task (the [attachedImageView] property), and if
         * `valueLocal` is not `null`, and ` imageView` is not `null` we set `success` to `true`
         * and set the drawable that `imageView` displays to `valueLocal`.
         *
         * Finally if [OnImageLoadedListener] field [mOnImageLoadedListener] is not null we call its
         * [OnImageLoadedListener.onImageLoaded] method with our `success` variable.
         *
         * @param result [BitmapDrawable] downloaded by [doInBackground] method (or retrieved from
         * disk cache).
         */
        override fun onPostExecute(result: BitmapDrawable?) {
            //BEGIN_INCLUDE(complete_background_work)
            var valueLocal: BitmapDrawable? = result
            var success = false
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled || mExitTasksEarly) {
                valueLocal = null
            }
            val imageView = attachedImageView
            if (valueLocal != null && imageView != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onPostExecute - setting bitmap")
                }
                success = true
                setImageDrawable(imageView = imageView, drawable = valueLocal)
            }
            mOnImageLoadedListener?.onImageLoaded(success)
            //END_INCLUDE(complete_background_work)
        }

        /**
         * Runs on the UI thread after [cancel] is invoked and [doInBackground] has finished. We
         * call our super's implementation of `onCancelled` then synchronized on [Object] field
         * [mPauseWorkLock] we call its [Object.notifyAll] method to wake all threads waiting for
         * [mPauseWorkLock].
         *
         * @param result [BitmapDrawable] returned from [doInBackground] method.
         */
        override fun onCancelled(result: BitmapDrawable?) {
            super.onCancelled(result) // docs say we should not call?
            synchronized(mPauseWorkLock) { mPauseWorkLock.notifyAll() }
        }

        /**
         * Returns the [ImageView] associated with this task as long as the [ImageView]'s task still
         * points to this task as well. Returns `null` otherwise. We initialize [ImageView] variable
         * `val imageView` by retrieving the reference object's referent from [imageViewReference]
         * (will be `null` if the garbage collector collected this [WeakReference] or it was
         * by the program). We initialize [BitmapWorkerTask] variable `val bitmapWorkerTask` to the
         * [BitmapWorkerTask] returned by our method [getBitmapWorkerTask] for `imageView`. If `this`
         * is the same as the variable `bitmapWorkerTask` we return `imageView`, otherwise we return
         * `null`.
         *
         * @return [ImageView] associated with this task or `null`
         */
        private val attachedImageView: ImageView?
            get() {
                val imageView = imageViewReference.get()
                val bitmapWorkerTask = getBitmapWorkerTask(imageView)
                return if (this === bitmapWorkerTask) {
                    imageView
                } else null
            }
    }

    /**
     * Interface definition for callback on image loaded successfully.
     */
    interface OnImageLoadedListener {
        /**
         * Called once the image has been loaded.
         *
         * @param success `true` if the image was loaded successfully, `false` if
         * there was an error.
         */
        fun onImageLoaded(success: Boolean)
    }

    /**
     * A custom [BitmapDrawable] that will be attached to the [ImageView] while the work is in
     * progress. Contains a reference to the actual worker task, so that it can be stopped if a
     * new binding is required, and makes sure that only the last started worker process can bind
     * its result, independently of the finish order.
     */
    private class AsyncDrawable(
        res: Resources?,
        bitmap: Bitmap?,
        bitmapWorkerTask: BitmapWorkerTask
    ) : BitmapDrawable(res, bitmap) {
        /**
         * [WeakReference] to the actual worker task that is downloading and processing the image
         */
        @Suppress("JoinDeclarationAndAssignment") // Easier to debug this way
        private val bitmapWorkerTaskReference: WeakReference<BitmapWorkerTask>

        /**
         * Our constructor. We call our super's two parameter constructor which creates a drawable
         * from our `Bitmap` parameter `bitmap` using `Resources` parameter `res` to access
         * resources. Then we initialize our field `bitmapWorkerTaskReference` with a weak
         * reference to our `BitmapWorkerTask` parameter `bitmapWorkerTask`.
         */
        init {
            bitmapWorkerTaskReference = WeakReference(bitmapWorkerTask)
        }

        /**
         * Getter for our field [bitmapWorkerTaskReference], it dereferences the weak reference
         * and returns the [BitmapWorkerTask] it points to.
         *
         * @return [BitmapWorkerTask] which is downloading our image, or `null` if the garbage
         * collector or program deleted it.
         */
        val bitmapWorkerTask: BitmapWorkerTask?
            get() = bitmapWorkerTaskReference.get()
    }

    /**
     * Called when the processing is complete and the final drawable should be set on the [ImageView].
     * If the [Boolean] field [mFadeInBitmap] is `true` we animate the transition from a transparent
     * drawable to our [Drawable] parameter [drawable] by creating [TransitionDrawable] variable
     * `val td` to be a transition drawable between the two layers composed of a new [ColorDrawable]
     * with the color given by [android.R.color.transparent], and our parameter [drawable]. We then
     * set the background drawable of [ImageView] parameter [imageView] to a [BitmapDrawable] created
     * from [Bitmap] field [mLoadingBitmap]. We then set the image drawable of [imageView] to `td`
     * and call the [TransitionDrawable.startTransition] method of `td` to fade it in over the next
     * [FADE_IN_TIME] (200) milliseconds.
     *
     * If the flag field [mFadeInBitmap] is `false` we just set the image drawable of [imageView]
     * to [drawable].
     *
     * @param imageView [ImageView] to display the drawable
     * @param drawable [Drawable] to display on the [ImageView]
     */
    private fun setImageDrawable(imageView: ImageView, drawable: Drawable) {
        if (mFadeInBitmap) {
            // Transition drawable with a transparent drawable and the final drawable
            val td = TransitionDrawable(arrayOf(
                imageView.resources.getColor(android.R.color.transparent).toDrawable(),
                drawable
            ))
            // Set background to loading bitmap
            imageView.setBackgroundDrawable(
                mLoadingBitmap!!.toDrawable(mResources))
            imageView.setImageDrawable(td)
            td.startTransition(FADE_IN_TIME)
        } else {
            imageView.setImageDrawable(drawable)
        }
    }

    /**
     * Pause any ongoing background work. This can be used as a temporary measure to improve
     * performance. For example background work could be paused when a `ListView` or `GridView` is
     * being scrolled using a [AbsListView.OnScrollListener] to keep scrolling smooth.
     *
     * If work is paused, be sure [setPauseWork] is called again with `false` before your fragment
     * or activity is destroyed (for example during [android.app.Activity.onPause] or there is a
     * risk the background thread will never finish.
     *
     * Synchronized on [Object] field [mPauseWorkLock] we save our [Boolean] parameter [pauseWork]
     * in our field [mPauseWork]. Then if [mPauseWork] is `false` we wake up all threads which are
     * waiting on [mPauseWorkLock].
     *
     * @param pauseWork `true` to pause the work, `false` to resume the work.
     */
    fun setPauseWork(pauseWork: Boolean) {
        synchronized(mPauseWorkLock) {
            mPauseWork = pauseWork
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll()
            }
        }
    }

    /**
     * [AsyncTask] used to process disk cache maintenance commands in the background.
     */
    protected inner class CacheAsyncTask : AsyncTask<Any?, Void?, Void?>() {
        /**
         * Performs disk operations on a background thread. We switch on the integer value of our
         * parameter `params[0]`:
         *
         *  * [MESSAGE_CLEAR]: we call the method [clearCacheInternal] which clears the cache stored
         *  in [ImageCache] field [imageCache].
         *
         *  * [MESSAGE_INIT_DISK_CACHE]: we call the method [initDiskCacheInternal] which
         *  initializes the cache stored in [ImageCache] field [imageCache].
         *
         *  * [MESSAGE_FLUSH]: we call the method [flushCacheInternal] which flushes the cache
         *  stored in [ImageCache] field [imageCache] to disk.
         *
         *  * [MESSAGE_CLOSE]: we call the method [closeCacheInternal] which closes the cache stored
         *  in [ImageCache] field [imageCache] and sets [imageCache] to null.
         *
         * @param params The parameters of the task, we use only `params[0]`
         * @return `null`
         */
        override fun doInBackground(vararg params: Any?): Void? {
            when (params[0] as Int) {
                MESSAGE_CLEAR -> clearCacheInternal()
                MESSAGE_INIT_DISK_CACHE -> initDiskCacheInternal()
                MESSAGE_FLUSH -> flushCacheInternal()
                MESSAGE_CLOSE -> closeCacheInternal()
            }
            return null
        }
    }

    /**
     * If our [ImageCache] field [imageCache] is not `null` we call its [ImageCache.initDiskCache]
     * method to initialize the disk cache.
     */
    protected open fun initDiskCacheInternal() {
        if (imageCache != null) {
            imageCache!!.initDiskCache()
        }
    }

    /**
     * If our [ImageCache] field [imageCache] is not `null` we call its [ImageCache.clearCache]
     * method to clear both the memory and the disk cache.
     */
    protected open fun clearCacheInternal() {
        if (imageCache != null) {
            imageCache!!.clearCache()
        }
    }

    /**
     * If our [ImageCache] field [imageCache] is not `null` we call its [ImageCache.flush] method to
     * flush the disk cache to disk.
     */
    protected open fun flushCacheInternal() {
        if (imageCache != null) {
            imageCache!!.flush()
        }
    }

    /**
     * If our [ImageCache] field [imageCache] is not `null` we call its [ImageCache.close] method to
     * close both the memory and the disk cache, then set [imageCache] to null.
     */
    protected open fun closeCacheInternal() {
        if (imageCache != null) {
            imageCache!!.close()
            imageCache = null
        }
    }

    /**
     * Creates a new instance of [CacheAsyncTask] and executes it with the command [MESSAGE_CLEAR]
     * to clear both the memory and the disk cache.
     */
    fun clearCache() {
        CacheAsyncTask().execute(MESSAGE_CLEAR)
    }

    /**
     * Creates a new instance of [CacheAsyncTask] and executes it with the command [MESSAGE_FLUSH]
     * to flush the disk cache to disk.
     */
    fun flushCache() {
        CacheAsyncTask().execute(MESSAGE_FLUSH)
    }

    /**
     * Creates a new instance of [CacheAsyncTask] and executes it with the command [MESSAGE_CLOSE]
     * close both the memory and the disk cache.
     */
    fun closeCache() {
        CacheAsyncTask().execute(MESSAGE_CLOSE)
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG: String = "ImageWorker"

        /**
         * Length of the transition in milliseconds when loading a drawable to an `ImageView`
         */
        private const val FADE_IN_TIME = 200

        // Parameters used when calling CacheAsyncTask().execute to request specific work in the background

        /**
         * Causes background thread to execute the [clearCacheInternal] method, which clears both
         * the memory and disk cache associated with our [ImageCache] object. Used by our [clearCache]
         * method which is called when the user selects the `R.id.clear_cache` menu item.
         */
        private const val MESSAGE_CLEAR = 0

        /**
         * Causes background thread to execute the [initDiskCacheInternal] method, which Initializes
         * the disk cache. Used by our [addImageCache] method which is called from the `onCreate`
         * methods of [ImageDetailActivity] and [ImageGridFragment].
         */
        private const val MESSAGE_INIT_DISK_CACHE = 1

        /**
         * Causes background thread to execute the [flushCacheInternal] method, which calls the
         * [ImageCache.flush] method of our [ImageCache] field [imageCache] which Flushes the disk
         * cache to disk. It is used in the `onPause` methods of both [ImageDetailActivity] and
         * [ImageGridFragment].
         */
        private const val MESSAGE_FLUSH = 2

        /**
         * Causes background thread to execute the [closeCacheInternal] method, which calls the
         * [ImageCache.close] method of [ImageCache] field [imageCache] which closes its [DiskLruCache].
         * It is used in the `onDestroy` methods of both [ImageDetailActivity] and [ImageGridFragment]
         */
        private const val MESSAGE_CLOSE = 3

        /**
         * Cancels any pending work attached to the [ImageView] parameter [imageView]. We initialize
         * our [BitmapWorkerTask] variable `val bitmapWorkerTask` by calling the [getBitmapWorkerTask]
         * method for our [ImageView] parameter [imageView]. If this is not `null` we call the
         * [BitmapWorkerTask.cancel]  method of `bitmapWorkerTask` with `true` to attempt to cancel
         * execution of this task.
         *
         * @param imageView [ImageView] whose task we wish to cancel
         */
        fun cancelWork(imageView: ImageView?) {
            val bitmapWorkerTask: BitmapWorkerTask? = getBitmapWorkerTask(imageView)
            if (bitmapWorkerTask != null) {
                bitmapWorkerTask.cancel(true)
                if (BuildConfig.DEBUG) {
                    val bitmapData = bitmapWorkerTask.mData
                    Log.d(TAG, "cancelWork - cancelled work for $bitmapData")
                }
            }
        }

        /**
         * Cancels any potential work in progress for its parameters and returns `true` if the
         * current work has been canceled or if there was no work in progress on the [ImageView]
         * parameter [imageView]. First we initialize [BitmapWorkerTask] variable `val bitmapWorkerTask`
         * by calling the [getBitmapWorkerTask] method for our [ImageView] parameter [imageView].
         * If this is not `null` we initialize [Any] variable `val bitmapData` to the
         * [BitmapWorkerTask.mData] field of `bitmapWorkerTask`, then if:
         *
         *  * `bitmapData` is `null`, or `bitmapData` is not equal to our [Any] parameter [data] we
         * call the [BitmapWorkerTask.cancel] method of `bitmapWorkerTask` to cancel the work and
         * return `true` (by falling through to the null branch of the previous if statement.
         *
         *  * otherwise we return `false`: The same work is already in progress.
         *
         * If `bitmapWorkerTask` is NOT `null` we return true.
         *
         * @param data [Any] which is being loaded into [ImageView] parameter [imageView]
         * @param imageView [ImageView] that is being loaded into
         * @return `false` if the work in progress deals with the same data. The work is not
         * stopped in that case.
         */
        fun cancelPotentialWork(data: Any, imageView: ImageView?): Boolean {
            //BEGIN_INCLUDE(cancel_potential_work)
            val bitmapWorkerTask = getBitmapWorkerTask(imageView)
            if (bitmapWorkerTask != null) {
                val bitmapData: Any? = bitmapWorkerTask.mData
                if (bitmapData == null || bitmapData != data) {
                    bitmapWorkerTask.cancel(true)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "cancelPotentialWork - cancelled work for $data")
                    }
                } else {
                    // The same work is already in progress.
                    return false
                }
            }
            return true
            //END_INCLUDE(cancel_potential_work)
        }

        /**
         * Retrieves the currently active work task (if any) associated with the [ImageView]
         * parameter [imageView]. If [imageView] is not `null` we initialize [Drawable] variable
         * `val drawable` with the drawable assigned to [imageView]. If `drawable` is an instance
         * of [AsyncDrawable] we return the [BitmapWorkerTask] returned by its property
         * [AsyncDrawable.bitmapWorkerTask] to the caller. If [imageView] is `null` or `drawable`
         * is not an [AsyncDrawable] we return `null` to the caller.
         *
         * @param imageView Any [ImageView]
         * @return Retrieve the currently active work task (if any) associated with [ImageView]
         * parameter [imageView]. `null` if there is no such task.
         */
        private fun getBitmapWorkerTask(imageView: ImageView?): BitmapWorkerTask? {
            if (imageView != null) {
                val drawable: Drawable? = imageView.drawable
                if (drawable is AsyncDrawable) {
                    return drawable.bitmapWorkerTask
                }
            }
            return null
        }
    }
}
