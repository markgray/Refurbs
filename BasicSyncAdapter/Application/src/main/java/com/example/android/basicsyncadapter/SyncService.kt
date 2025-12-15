/*
 * Copyright 2013 Google Inc.
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.basicsyncadapter

import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentResolver
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Service to handle sync requests. It is declared as such in the AndroidManifest.xml file with a
 * service element and is described by the file xml/syncadapter.xml
 *
 * This service is invoked in response to [Intent]'s with action `android.content.SyncAdapter`, and
 * returns a [IBinder] connection to [SyncAdapter]. For performance, only one sync adapter will be
 * initialized within this application's context.
 *
 * Note: The [SyncService] itself is not notified when a new sync occurs. It's role is to manage the
 * lifecycle of our [SyncAdapter] and provide a handle to said [SyncAdapter] to the OS on request.
 */
class SyncService : Service() {
    /**
     * Thread-safe constructor, creates static [SyncAdapter] instance. First we call our super's
     * constructor and log "Service created". Then we synchronize on [Any] field [sSyncAdapterLock]
     * and if our [SyncAdapter] field [sSyncAdapter] is `null` we initialize it with a new instance
     * configured to auto initialize (requests that have [ContentResolver.SYNC_EXTRAS_INITIALIZE]
     * set will be internally handled by [AbstractThreadedSyncAdapter] by calling
     * [ContentResolver.setIsSyncable] with 1 if it is currently set to <0).
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = SyncAdapter(context = applicationContext, autoInitialize = true)
            }
        }
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * Logging-only destructor.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service destroyed")
    }

    /**
     * Return [IBinder] handle for IPC communication with [SyncAdapter]. New sync requests will be
     * sent directly to the [SyncAdapter] using this channel. We just return the result of calling
     * the [SyncAdapter.getSyncAdapterBinder] method (aka kotlin `syncAdapterBinder` property) of
     * our [SyncAdapter] field [sSyncAdapter] (which returns a reference to the [IBinder] of the
     * [SyncAdapter] service.
     *
     * @param intent Calling [Intent]
     * @return [IBinder] handle for [SyncAdapter]
     */
    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter!!.syncAdapterBinder
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "SyncService"

        /**
         * `Object` we synchronize on in our constructor
         */
        private val sSyncAdapterLock = Any()

        /**
         * Our instance of [SyncAdapter] which we use to retrieve its [IBinder] to return to the
         * caller when our [onBind] method is called.
         */
        private var sSyncAdapter: SyncAdapter? = null
    }
}