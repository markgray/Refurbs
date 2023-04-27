/*
 * Copyright (C) 2017 The Android Open Source Project
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
@file:Suppress("unused")

package com.example.android.persistence

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Global executor pools for the whole application.
 *
 *
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
class AppExecutors
/**
 * Constructor which uses its parameters to initialize our fields.
 *
 * @param mDiskIO     `Executor` to use for our `mDiskIO` field
 * @param mNetworkIO  `Executor` to use for our `mNetworkIO` field
 * @param mMainThread `Executor` to use for our `mMainThread` field
 */ private constructor(
    /**
     * Single-threaded Executor to use for disk io
     */
    private val mDiskIO: Executor,
    /**
     * Three thread thread pool to use for network io UNUSED
     */
    private val mNetworkIO: Executor,
    /**
     * `Executor` whose `Handler` uses the application's main looper, which lives in the
     * main thread of the application UNUSED
     */
    private val mMainThread: Executor) {
    /**
     * Our zero arguments constructor, which calls our four argument constructor with a new single
     * threaded Executor to use to initialize `Executor mDiskIO`, a three threaded Executor to
     * use to initialize `Executor mNetworkIO`, and a new instance of `MainThreadExecutor`
     * to initialize `Executor mMainThread`
     */
    constructor() : this(
        Executors.newSingleThreadExecutor(),
        Executors.newFixedThreadPool(3),
        MainThreadExecutor()
    )

    /**
     * Getter for our `Executor mDiskIO` field.
     *
     * @return contents of our `Executor mDiskIO` field
     */
    fun diskIO(): Executor {
        return mDiskIO
    }

    /**
     * Getter for our `Executor mNetworkIO` field.
     *
     * @return contents of our `Executor mNetworkIO` field
     */
    fun networkIO(): Executor {
        return mNetworkIO
    }

    /**
     * Getter for our `Executor mMainThread` field.
     *
     * @return contents of our `Executor mMainThread` field
     */
    fun mainThread(): Executor {
        return mMainThread
    }

    /**
     * Custom `Executor` class whose `Handler` uses the application's main looper, which
     * lives in the main thread of the application and is used to handle the UI.
     */
    private class MainThreadExecutor : Executor {
        /**
         * Our `Handler` which uses the application's main looper.
         */
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        /**
         * Executes the given command at some time in the future on the UI thread. We just call the
         * `post` method of our field `Handler mainThreadHandler` to add our parameter
         * `Runnable command` to the message queue of our `Handler`.
         *
         * @param command a `Runnable` to be added to the message queue of our `Handler`
         */
        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}