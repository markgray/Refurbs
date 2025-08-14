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

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.storage.OnObbStateChangeListener
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.io.File
import java.util.Locale

/**
 * This class provides a basic demonstration of how to manage an OBB (Opaque Binary Blob) file.
 * It provides two buttons: one to mount an OBB and another to unmount an OBB. The main feature
 * is that it implements an [OnObbStateChangeListener] which updates some text fields with relevant
 * information.
 */
class ObbMountActivity : ComponentActivity() {
    /**
     * [TextView] we use to display the current status of our use of the Obb file
     */
    private var mStatus: TextView? = null

    /**
     * [TextView] we use to display the absolute path to the mounted OBB image data
     */
    private var mPath: TextView? = null

    /**
     * Our handle to the [Context.STORAGE_SERVICE] system level service.
     */
    private var mSM: StorageManager? = null

    /**
     * Called with the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.obb_mount_activity`. Next we set
     * the [View.OnClickListener] of the [View] with id `R.id.mount` ("Mount" [Button]) to our field
     * [mMountListener] and the [View.OnClickListener] of the view with id `R.id.unmount` ("Unmount"
     * [Button]) to our field [mUnmountListener]. We initialize our [TextView] field [mStatus] by
     * finding the view with id `R.id.status` and our [TextView] field [mPath] by finding the view
     * with id `R.id.path`. We then initialize our [ObbState] variable `val state` by using the
     * [getLastNonConfigurationInstance] method (kotlin `lastNonConfigurationInstance` property) to
     * retrieve the [ObbState] object that our [onRetainNonConfigurationInstance] override may have
     * saved if we are being restarted after a configuration change. If `state` is not `null` (we
     * are being restarted) we set our [StorageManager] field [mSM] to the value stored in the
     * [ObbState.storageManager] field of `state`, set the text of [mStatus] to the value stored in
     * the [ObbState.status] field, and set the text of [mPath] to the value stored in the
     * [ObbState.path] field of `state`. If `state` is null on the other hand (we are just being
     * started from scratch) we initialize our [StorageManager] field [mSM] with a handle to the
     * [Context.STORAGE_SERVICE] system level service. Finally we initialize our [String] field
     * [mObbPath] to the pathname string of the [File] "test1.obb" in the primary shared/external
     * storage directory.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     * (we use the [getLastNonConfigurationInstance] method instead which returns the
     * [ObbState] that our [onRetainNonConfigurationInstance] override stores in the
     * [ObbState] that it returns).
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.obb_mount_activity)
        val rootView = findViewById<RelativeLayout>(R.id.root_view)
        // TODO: Position buttons here instead of in xml
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

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
         * of the [TextView] field [mStatus] to the string value of our [Int] parameter [state].
         * If [state] is equal to [OnObbStateChangeListener.MOUNTED] (The OBB container is now
         * mounted and ready for use) we set the text of the [TextView] field [mPath] to the absolute
         * path to the mounted OBB image data of the OBB whose pathname string is given by the
         * [String] field [mObbPath], otherwise we set the text to the empty string "".
         *
         * @param path  path to the OBB file the state change has happened on
         * @param state the current state of the OBB
         */
        override fun onObbStateChange(path: String, state: Int) {
            Log.d(TAG, "path=$path; state=$state")
            mStatus!!.text = String.format(Locale.ENGLISH,"%d", state)
            if (state == MOUNTED) {
                mPath!!.text = mSM!!.getMountedObbPath(mObbPath)
            } else {
                mPath!!.text = ""
            }
        }
    }

    /**
     * A call-back for when the user presses the "Mount" button. Wrapped in a try block intended to
     * catch and log IllegalArgumentException we call the [StorageManager.mountObb] method of our
     * [StorageManager] field [mSM] to try to mount the OBB whose file system path is given by our
     * [String] field [mObbPath] and whose [OnObbStateChangeListener] is the [OnObbStateChangeListener]
     * field [mEventListener]. If the method returns `true` (the mount call was successfully queued)
     * we set the text of [mStatus] to the string with resource id `R.string.attempting_mount`
     * ("Attempting to mount..."), if it returns `false` (the mount call was not queued) we set the
     * text of [mStatus] to the string with resource id `R.string.failed_to_start_mount` ("Failed
     * to start mount process...").
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
        } catch (_: IllegalArgumentException) {
            mStatus!!.setText(R.string.obb_already_mounted)
            Log.d(TAG, "OBB already mounted")
        }
    }

    /**
     * A call-back for when the user presses the "Unmount" button. Wrapped in a try block intended
     * to catch and log [IllegalArgumentException] we call the [StorageManager.unmountObb] method of
     * our [StorageManager] field [mSM] to try to unmount the OBB whose filed system path is given
     * by our [String] field [mObbPath] and whose [OnObbStateChangeListener] is the
     * [OnObbStateChangeListener] field [mEventListener]. If the method returns `true` (the unmount
     * call was successfully queued) we set the text of [TextView] field [mStatus] to the string
     * with resource id `R.string.attempting_unmount` ("Attempting to unmount..."), if it returns
     * `false` (the unmount call was not queued) we set the text of [mStatus] to the string with
     * resource id `R.string.failed_to_start_unmount` ("Failed to start unmount process...").
     */
    var mUnmountListener: View.OnClickListener = View.OnClickListener {
        try {
            if (mSM!!.unmountObb(mObbPath, false, mEventListener)) {
                mStatus!!.setText(R.string.attempting_unmount)
            } else {
                mStatus!!.setText(R.string.failed_to_start_unmount)
            }
        } catch (_: IllegalArgumentException) {
            mStatus!!.setText(R.string.obb_not_mounted)
            Log.d(TAG, "OBB not mounted")
        }
    }

    /**
     * Called by the system, as part of destroying an activity due to a configuration change, when
     * it is known that a new instance will immediately be created for the new configuration. You
     * can return any object you like here, including the activity instance itself, which can later
     * be retrieved by calling [getLastNonConfigurationInstance] in the new activity instance.
     * We return a new instance of [ObbState] constructed to save the [StorageManager] field [mSM],
     * the text displayed in [TextView] field [mStatus], and the text displayed in [TextView] field
     * [mPath].
     *
     * @return an [ObbState] holding the value of the [StorageManager] field [mSM], the text
     * displayed in [TextView] field [mStatus], and the text displayed in [TextView] field [mPath]
     * (these will then be restored in the [onCreate] override from the [Object] returned by the
     * [getLastNonConfigurationInstance] method).
     */
    @Deprecated("Use a {@link androidx.lifecycle.ViewModel} to store non config state.")
    override fun onRetainCustomNonConfigurationInstance(): Any {
        // Since our OBB mount is tied to the StorageManager, retain it
        return ObbState(mSM, mStatus!!.text, mPath!!.text)
    }

    /**
     * Class used to save our state in our [onRetainNonConfigurationInstance] override and
     * restore it after a configuration change by calling the [getLastNonConfigurationInstance]
     * method in our `onCreate` override.
     */
    private class ObbState
    /**
     * Our constructor, it just saves its parameters in the fields intended for them.
     *
     * @param storageManager [StorageManager] to save in our [storageManager] field.
     * @param status [CharSequence] to save in our [status] field
     * @param path [CharSequence] to save in our [path] field
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
