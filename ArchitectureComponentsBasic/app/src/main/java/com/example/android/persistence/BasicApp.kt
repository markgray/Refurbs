/*
 * Copyright 2017, The Android Open Source Project
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
package com.example.android.persistence

import android.app.Application
import com.example.android.persistence.db.AppDatabase

/**
 * Android Application class. Used for accessing singletons.
 */
class BasicApp : Application() {
    /**
     * An instance of `AppExecutors` to use to access the Global executor pool for the whole
     * application.
     */
    private var mAppExecutors: AppExecutors? = null

    /**
     * Called when the application is starting, before any activity, service, or receiver objects
     * (excluding content providers) have been created. First we call our super's implementation of
     * `onCreate`, then we initialize our field `AppExecutors mAppExecutors` with a new
     * instance.
     */
    override fun onCreate() {
        super.onCreate()
        mAppExecutors = AppExecutors()
    }

    /**
     * Our `getRepository` method uses this to fetch the `AppDatabase` instance used for
     * the application, we just return the `AppDatabase` returned by the `getInstance`
     * method of `AppDatabase`.
     *
     * @return `AppDatabase` instance used for the application, creating it if need be
     */
    val database: AppDatabase
        get() = AppDatabase.getInstance(this, mAppExecutors!!)!!

    /**
     * Fetches the `DataRepository` we use to access the `AppDatabase` instance used for
     * the application. We just return the `DataRepository` returned by the `getInstance`
     * method of `DataRepository` for the `AppDatabase` returned by our method
     * `getDatabase`.
     *
     * @return `DataRepository` to use to access the `AppDatabase` instance used for the
     * application
     */
    val repository: DataRepository
        get() = DataRepository.getInstance(database)!!
}