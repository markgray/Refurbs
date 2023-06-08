/*
 * Copyright (C) 2010 The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.obbapp

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.os.storage.OnObbStateChangeListener
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.widget.TextView
import java.io.File

/**
 * This class provides a basic demonstration of how to manage an OBB (Opaque Binary Blob) file.
 * It provides two buttons: one to mount an OBB and another to unmount an OBB. The main feature
 * is that it implements an OnObbStateChangeListener which updates some text fields with relevant
 * information.
 */
class ObbMountActivity : Activity() {
    /**
     * `TextView` we use to display the current status of our use of the Obb file
     */
    private var mStatus: TextView? = null

    /**
     * `TextView` we use to display the absolute path to the mounted OBB image data
     */
    private var mPath: TextView? = null

    /**
     * Our handle to the STORAGE_SERVICE system level service.
     */
    private var mSM: StorageManager? = null

    /**
     * Called with the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.obb_mount_activity. Then we set the
     * `OnClickListener` of the view with id R.id.mount ("Mount" `Button`) to our field
     * `mMountListener` and the `OnClickListener` of the view with id R.id.unmount
     * ("Unmount" `Button`) to our field `mUnmountListener`. We initialize our field
     * `TextView mStatus` by finding the view with id R.id.status and our field `TextView mPath`
     * by finding the view with id R.id.path. We then initialize our variable `ObbState state`
     * by calling the `getLastNonConfigurationInstance` to retrieve the `ObbState` object
     * that our `onRetainNonConfigurationInstance` override may have saved if we are being restarted
     * after a configuration change. If `state` is not null (we are being restarted) we set our
     * field `StorageManager mSM` to the value stored in the `storageManager` field of
     * `state`, set the text of `mStatus` to the value stored in the `status` field,
     * and set the text of `mPath` to the value stored in the `path` field of `state`.
     * If `state` is null on the other hand (we are just being started from scratch) we initialize
     * our field `StorageManager mSM` with a handle to the STORAGE_SERVICE system level service.
     * Finally we initialize our field `String mObbPath` to the pathname string of the `File`
     * "test1.obb" in the primary shared/external storage directory.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     * (we use the `getLastNonConfigurationInstance` method instead
     * which returns the `ObbState` that our `onRetainNonConfigurationInstance`
     * override stores in the `ObbState` that it returns).
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.obb_mount_activity)

        // Hook up button presses to the appropriate event handler.
        findViewById<View>(R.id.mount).setOnClickListener(mMountListener)
        findViewById<View>(R.id.unmount).setOnClickListener(mUnmountListener)

        // Text indications of current status
        mStatus = findViewById(R.id.status)
        mPath = findViewById(R.id.path)
        val state = lastNonConfigurationInstance as ObbState?
        if (state != null) {
            mSM = state.storageManager
            mStatus!!.text = state.status
            mPath!!.text = state.path
        } else {
            // Get an instance of the StorageManager
            mSM = applicationContext.getSystemService(STORAGE_SERVICE) as StorageManager
        }
        mObbPath = File(Environment.getExternalStorageDirectory(), "test1.obb").path
    }

    /**
     * The [OnObbStateChangeListener] we use. It is Used for receiving notifications from the
     * [StorageManager] about OBB file states.
     */
    var mEventListener: OnObbStateChangeListener = object : OnObbStateChangeListener() {
        /**
         * Called when an OBB has changed states. First we log the new state, then we set the text
         * of the field `TextView mStatus` to the string value of our parameter `int state`.
         * If `state` is equal to MOUNTED (The OBB container is now mounted and ready for use)
         * we set the text of the field `TextView mPath` to the absolute path to the mounted
         * OBB image data of the OBB whose pathname string is given by the field `mObbPath`,
         * otherwise we set the text to the empty string "".
         *
         * @param path  path to the OBB file the state change has happened on
         * @param state the current state of the OBB
         */
        override fun onObbStateChange(path: String, state: Int) {
            Log.d(TAG, "path=$path; state=$state")
            mStatus!!.text = state.toString()
            if (state == MOUNTED) {
                mPath!!.text = mSM!!.getMountedObbPath(mObbPath)
            } else {
                mPath!!.text = ""
            }
        }
    }

    /**
     * A call-back for when the user presses the "Mount" button. Wrapped in a try block intended to
     * catch and log IllegalArgumentException we call the {@code mountObb} method of our field
     * {@code StorageManager mSM} to try to mount the OBB whose filed system path is given by our
     * field {@code String mObbPath} and whose {@code OnObbStateChangeListener} is the field
     * {@code OnObbStateChangeListener mEventListener}. If the method returns true (the mount
     * call was successfully queued) we set the text of {@code mStatus} to the string with resource
     * id R.string.attempting_mount ("Attempting to mount..."), if it returns false (the mount call
     * was not queued) we set the text of {@code mStatus} to the string with resource id
     * R.string.failed_to_start_mount ("Failed to start mount process...").
     */
    var mMountListener: View.OnClickListener = View.OnClickListener {
        try {
            // We don't need to synchronize here to avoid clobbering the
            // content of mStatus because the callback comes to our main
            // looper.
            if (mSM!!.mountObb(mObbPath, null, mEventListener)) {
                mStatus!!.setText(R.string.attempting_mount)
            } else {
                mStatus!!.setText(R.string.failed_to_start_mount)
            }
        } catch (e: IllegalArgumentException) {
            mStatus!!.setText(R.string.obb_already_mounted)
            Log.d(TAG, "OBB already mounted")
        }
    }

    /**
     * A call-back for when the user presses the "Unmount" button. Wrapped in a try block intended to
     * catch and log IllegalArgumentException we call the {@code unmountObb} method of our field
     * {@code StorageManager mSM} to try to unmount the OBB whose filed system path is given by our
     * field {@code String mObbPath} and whose {@code OnObbStateChangeListener} is the field
     * {@code OnObbStateChangeListener mEventListener}. If the method returns true (the unmount
     * call was successfully queued) we set the text of {@code mStatus} to the string with resource
     * id R.string.attempting_unmount ("Attempting to unmount..."), if it returns false (the unmount call
     * was not queued) we set the text of {@code mStatus} to the string with resource id
     * R.string.failed_to_start_unmount ("Failed to start unmount process...").
     */
    var mUnmountListener: View.OnClickListener = View.OnClickListener {
        try {
            if (mSM!!.unmountObb(mObbPath, false, mEventListener)) {
                mStatus!!.setText(R.string.attempting_unmount)
            } else {
                mStatus!!.setText(R.string.failed_to_start_unmount)
            }
        } catch (e: IllegalArgumentException) {
            mStatus!!.setText(R.string.obb_not_mounted)
            Log.d(TAG, "OBB not mounted")
        }
    }

    /**
     * Called by the system, as part of destroying an activity due to a configuration change, when
     * it is known that a new instance will immediately be created for the new configuration. You
     * can return any object you like here, including the activity instance itself, which can later
     * be retrieved by calling [.getLastNonConfigurationInstance] in the new activity instance.
     * We return a new instance of `ObbState` constructed to save the field `StorageManager mSM`,
     * the text displayed in `TextView mStatus`, and the text displayed in `TextView mPath`.
     *
     * @return an `ObbState` holding the value of the field `StorageManager mSM`, the text
     * displayed in `TextView mStatus`, and the text displayed in `TextView mPath` (these
     * will then be restored in the `onCreate` override from the `Object` returned by the
     * `getLastNonConfigurationInstance` method).
     */
    override fun onRetainNonConfigurationInstance(): Any {
        // Since our OBB mount is tied to the StorageManager, retain it
        return ObbState(mSM, mStatus!!.text, mPath!!.text)
    }

    /**
     * Class used to save our state in our `onRetainNonConfigurationInstance` override and
     * restore it after a configuration change by calling the `getLastNonConfigurationInstance`
     * method in our `onCreate` override.
     */
    private class ObbState
    /**
     * Our constructor, it just saves its parameters in the fields intended for them.
     *
     * @param storageManager `StorageManager` to save in our `StorageManager storageManager` field
     * @param status `CharSequence` to save in our `CharSequence status` field
     * @param path `CharSequence` to save in our `CharSequence path` field
     */(
        /**
         * Place to store the `StorageManager storageManager` passed to our constructor.
         */
        var storageManager: StorageManager?,
        /**
         * Place to store the `CharSequence status` passed to our constructor.
         */
        var status: CharSequence,
        /**
         * Place to store the `StorageManager path` passed to our constructor.
         */
        var path: CharSequence)

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "ObbMount"

        /**
         * Path to the OBB file (the pathname string)
         */
        private var mObbPath: String? = null
    }
}