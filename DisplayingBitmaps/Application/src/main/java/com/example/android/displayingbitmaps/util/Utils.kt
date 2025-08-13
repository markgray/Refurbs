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
package com.example.android.displayingbitmaps.util

import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.annotation.RequiresApi
import com.example.android.displayingbitmaps.ui.ImageDetailActivity
import com.example.android.displayingbitmaps.ui.ImageGridActivity

/**
 * Class containing some static utility methods.
 */
object Utils {
    /**
     * Enables `StrictMode` logging for debugging purposes. If we do not have at least a
     * `GINGERBREAD` device we do nothing. Otherwise we create a new instance to initialize our
     * [StrictMode.ThreadPolicy.Builder] variable `val threadPolicyBuilder`, configure it to detect
     * all potentially suspect actions for this thread (slow calls, disk reads, disk writes, network
     * operations, mismatches between defined resource types and getter calls, and unbuffered
     * input/output operations) then set the penalty to Log detected violations to the system log.
     * We initialize [StrictMode.VmPolicy.Builder] variable `val vmPolicyBuilder` with a new instance,
     * configure it to detect all potentially suspect actions for the VM process (on any thread)
     * (leaks of Activity subclasses, network traffic from the calling app which is not wrapped in
     * SSL/TLS, calling application sends a content:// Uri to another app without setting
     * FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION, calling application
     * exposes a file:// Uri to another app, Closeable or other object with an explicit termination
     * method is finalized without having been closed, BroadcastReceiver or ServiceConnection is
     * leaked during Context teardown, SQLiteCursor or other SQLite object is finalized without
     * having been closed, and sockets in the calling app which have not been tagged using
     * TrafficStats) then set the penalty to Log detected violations to the system log.
     *
     * If we have at least a HONEYCOMB device we add the penalty to flash the screen to
     * `threadPolicyBuilder`, and set the class instance limit of `vmPolicyBuilder` for both
     * [ImageGridActivity], and [ImageDetailActivity] to 1. We not build `threadPolicyBuilder` and
     * set the thread policy for the current thread to it, and build `vmPolicyBuilder` and set the
     * policy for actions in the VM process (on any thread) to it.
     */
    @SuppressLint("ObsoleteSdkInt") // Obsolete @TargetApi left to remind if code is reused.
    @RequiresApi(VERSION_CODES.HONEYCOMB)
    fun enableStrictMode() {
        if (hasGingerbread()) {
            val threadPolicyBuilder = ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
            val vmPolicyBuilder = VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
            if (hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen()
                vmPolicyBuilder
                    .setClassInstanceLimit(ImageGridActivity::class.java, 1)
                    .setClassInstanceLimit(ImageDetailActivity::class.java, 1)
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build())
            StrictMode.setVmPolicy(vmPolicyBuilder.build())
        }
    }

    /**
     * Returns `true` if our build version is greater than FROYO
     *
     * @return `true` if our build version is greater than FROYO
     */
    @SuppressLint("ObsoleteSdkInt")
    fun hasFroyo(): Boolean {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= VERSION_CODES.FROYO
    }

    /**
     * Returns `true` if our build version is greater than GINGERBREAD
     *
     * @return `true` if our build version is greater than GINGERBREAD
     */
    @SuppressLint("ObsoleteSdkInt")
    fun hasGingerbread(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD
    }

    /**
     * Returns `true` if our build version is greater than HONEYCOMB
     *
     * @return `true` if our build version is greater than HONEYCOMB
     */
    @JvmStatic
    @SuppressLint("ObsoleteSdkInt")
    fun hasHoneycomb(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB
    }

    /**
     * Returns `true` if our build version is greater than HONEYCOMB_MR1
     *
     * @return `true` if our build version is greater than HONEYCOMB_MR1
     */
    @SuppressLint("ObsoleteSdkInt")
    fun hasHoneycombMR1(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1
    }

    /**
     * Returns `true` if our build version is greater than JELLY_BEAN
     *
     * @return `true` if our build version is greater than JELLY_BEAN
     */
    @SuppressLint("ObsoleteSdkInt")
    fun hasJellyBean(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN
    }

    /**
     * Returns `true` if our build version is greater than KITKAT
     *
     * @return `true` if our build version is greater than KITKAT
     */
    @SuppressLint("ObsoleteSdkInt")
    fun hasKitKat(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT
    }
}