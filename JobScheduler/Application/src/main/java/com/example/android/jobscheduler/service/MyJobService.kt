/*
 * Copyright 2014 Google Inc.
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

package com.example.android.jobscheduler.service

import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Parcelable
import android.os.RemoteException
import android.util.Log
import com.example.android.jobscheduler.MainActivity

/**
 * TODO: Continue here.
 * Service to handle callbacks from the [JobScheduler]. Requests scheduled with the [JobScheduler]
 * ultimately land on this service's [onStartJob] method. It runs jobs for a specific amount of time
 * and finishes them. It keeps the activity updated with changes via a Messenger.
 */
class MyJobService : JobService() {
    /**
     * [Messenger] to  use to send messages back to [MainActivity]
     */
    private var mActivityMessenger: Messenger? = null

    /**
     * Called by the system when the service is first created. First we call our super's
     * implementation of `onCreate()` then we log that we have been created.
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * First we call our super's implementation of `onDestroy()` then we log that we have been
     * destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service destroyed")
    }

    /**
     * When the app's MainActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth.
     *
     * Called by the system every time a client explicitly starts the service by calling
     * [Context.startService], providing the arguments it supplied and a unique integer
     * token representing the start request.
     *
     * We initialize our [Messenger] field [mActivityMessenger] by retrieving from our [Intent]
     * parameter [intent] the [Parcelable] value that the activity that launched us stored under
     * the key [MainActivity.MESSENGER_INTENT_KEY] ("com.example.android.jobscheduler.MESSENGER_INTENT_KEY"),
     * then return the constant [JobService.START_NOT_STICKY] (if this service's process is killed
     * while it is started (after returning from [onStartCommand], and there are no new start intents
     * to deliver to it, then take the service out of the started state and don't recreate until a
     * future explicit call to [Context.startService].
     *
     * @param intent The [Intent] supplied to [Context.startService], as given. This may be `null`
     * if the service is being restarted after its process has gone away, and it had previously
     * returned anything except [JobService.START_STICKY_COMPATIBILITY].
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     * start.  Use with [stopSelfResult].
     *
     * @return The return value indicates what semantics the system should use for the service's
     * current started state. It may be one of the constants associated with the
     * [JobService.START_CONTINUATION_MASK] bits.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mActivityMessenger = intent.getParcelableExtra(
                MainActivity.MESSENGER_INTENT_KEY,
                Messenger::class.java
            )
        } else {
            @Suppress("DEPRECATION") // Needed for SDK < TIRAMISU
            mActivityMessenger = intent.getParcelableExtra(MainActivity.MESSENGER_INTENT_KEY)
        }
        return START_NOT_STICKY
    }

    /**
     * Override this method with the callback logic for your job. Any such logic needs to be
     * performed on a separate thread, as this function is executed on your application's main
     * thread. The work that this service "does" is simply wait for a certain duration and finish
     * the job (on another thread).
     *
     * First we call our method [sendMessage] to send a [Message] to [MainActivity] with
     * [MainActivity.MSG_COLOR_START] for the [Message.what] field of the message and the unique
     * job id of this job which is found using the [JobParameters.getJobId] method (kotlin `jobId`
     * property) of our [JobParameters] parameter [params] as the [Message.obj] field of the message.
     * We initialize [Long] variable `val duration` with the value stored in the extras of our
     * [JobParameters] parameter [params] under the key [MainActivity.WORK_DURATION_KEY]. We create
     * a new instance for [Handler] variable `val handler` and use it to schedule an anonymous
     * [Runnable] to run after `duration` milliseconds. The [Runnable.run] method of this anonymous
     * [Runnable] calls our method [sendMessage] with the parameters [MainActivity.MSG_COLOR_STOP]
     * for the [Message.what] field of the message and the unique job id of this job as the
     * [Message.obj] field of the message, and then calls the [jobFinished] method to inform the
     * JobManager we've finished executing.
     *
     * Finally we log the fact that we started the job, and return `true` to the caller as there's
     * more work to be done with this job (on the thread running on [Handler] variable `handler`).
     *
     * @param params Parameters specifying info about this job, including the extras bundle you
     * optionally provided at job-creation time.
     * @return `true`  your service needs to process the work (on a separate thread). `false` if
     * there's no more work to be done for this job.
     */
    override fun onStartJob(params: JobParameters): Boolean {
        // The work that this service "does" is simply wait for a certain duration and finish
        // the job (on another thread).
        sendMessage(messageID = MainActivity.MSG_COLOR_START, params = params.jobId)
        val duration: Long = params.extras.getLong(MainActivity.WORK_DURATION_KEY)

        // Uses a handler to delay the execution of jobFinished().
        val handler = Handler(Looper.myLooper()!!)
        handler.postDelayed({
            sendMessage(MainActivity.MSG_COLOR_STOP, params.jobId)
            jobFinished(params, false)
        }, duration)
        Log.i(TAG, "on start job: " + params.jobId)

        // Return true as there's more work to be done with this job.
        return true
    }

    /**
     * This method is called if the system has determined that you must stop execution of your job
     * even before you've had a chance to call [jobFinished].
     *
     * First we call our method [sendMessage] to send a [Message] to [MainActivity] with the
     * parameters [MainActivity.MSG_COLOR_STOP] for the [Message.what] field of the [Message] and
     * the unique job id of this job as the [Message.obj] field of the [Message]. We log the fact
     * that [onStopJob] has been called and return `false` to the caller to drop the job.
     *
     * @param params Parameters specifying info about this job.
     * @return `true` to indicate to the JobManager whether you'd like to reschedule this job based
     * on the retry criteria provided at job creation-time. `false` to drop the job. Regardless of
     * the value returned, your job must stop executing.
     */
    override fun onStopJob(params: JobParameters): Boolean {
        // Stop tracking these job parameters, as we've 'finished' executing.
        sendMessage(MainActivity.MSG_COLOR_STOP, params.jobId)
        Log.i(TAG, "on stop job: " + params.jobId)

        // Return false to drop the job.
        return false
    }

    /**
     * Used to send a [Message] to [MainActivity] using the [Messenger] passed as a parcelable extra
     * under the key [MainActivity.MESSENGER_INTENT_KEY] in the [Intent] that launched us. If our
     * [Messenger] field [mActivityMessenger] is `null` we log the error and return having done
     * nothing. We initialize [Message] variable `val m` with a [Message] from the global pool. We
     * set its [Message.what] field to our [Int] parameter [messageID] and its [Message.obj] field
     * to our [Any] parameter [params]. Then wrapped in a try block intended to catch and log
     * [RemoteException] we use [Messenger] field [mActivityMessenger] to send `m` to [MainActivity].
     *
     * @param messageID used as the [Message.what] field of the message we are sending
     * @param params used as the [Message.obj] field of the message we are sending
     */
    private fun sendMessage(messageID: Int, params: Any?) {
        // If this service is launched by the JobScheduler, there's no callback Messenger. It
        // only exists when the MainActivity calls startService() with the callback in the Intent.
        if (mActivityMessenger == null) {
            Log.d(TAG, "Service is bound, not started. There's no callback to send a message to.")
            return
        }
        val m: Message = Message.obtain()
        m.what = messageID
        m.obj = params
        try {
            mActivityMessenger!!.send(m)
        } catch (_: RemoteException) {
            Log.e(TAG, "Error passing service object back to activity.")
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private val TAG = MyJobService::class.java.simpleName
    }
}