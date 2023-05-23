/*
 * Copyright 2013 The Android Open Source Project
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
@file:Suppress("UNUSED_PARAMETER", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.jobscheduler

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.PersistableBundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import com.example.android.jobscheduler.service.MyJobService
import java.lang.ref.WeakReference

/**
 * Schedules and configures jobs to be executed by a [JobScheduler].
 *
 *
 * [MyJobService] can send messages to this via a [Messenger]
 * that is sent in the Intent that starts the Service.
 */
class MainActivity : Activity() {
    /**
     * `EditText` the user uses to enter the number of seconds to delay before launching a job.
     */
    private var mDelayEditText: EditText? = null

    /**
     * `EditText` the user uses to enter the number of seconds for the maximum scheduling
     * latency. The job will be run by this deadline even if other requirements are not met.
     */
    private var mDeadlineEditText: EditText? = null

    /**
     * `EditText` the user uses to enter the number of seconds for the duration of the job.
     */
    private var mDurationTimeEditText: EditText? = null

    /**
     * `RadioButton` the user uses to select unmetered network connection (NETWORK_TYPE_UNMETERED)
     */
    private var mWiFiConnectivityRadioButton: RadioButton? = null

    /**
     * `RadioButton` the user uses to select any network connection (NETWORK_TYPE_ANY)
     */
    private var mAnyConnectivityRadioButton: RadioButton? = null

    /**
     * `CheckBox` the user uses to select requires charging
     */
    private var mRequiresChargingCheckBox: CheckBox? = null

    /**
     * `CheckBox` the user uses specify that to run, the job needs the device to be in idle mode.
     * Idle mode is a loose definition provided by the system, which means that the device is not in
     * use, and has not been in use for some time.
     */
    private var mRequiresIdleCheckbox: CheckBox? = null

    /**
     * `ComponentName` for `MyJobService`, used to construct the `JobInfo.Builder`
     * we use to which we configure and build, then use the JOB_SCHEDULER_SERVICE to schedule.
     */
    private var mServiceComponent: ComponentName? = null

    /**
     * Application-provided id for this job. Subsequent calls to cancel, or jobs created with the
     * same jobId, will update the pre-existing job with the same id. We increment it after every
     * creation of a new job.
     */
    private var mJobId = 0

    /**
     * Handler for incoming messages from the service, we pass a `Messenger` constructed from
     * this handler to `MyJobService` in its intent.
     */
    private var mHandler: IncomingMessageHandler? = null

    /**
     * Called when the activity is starting. We call our super's implementation of `onCreate`
     * then set our content view to our layout file R.layout.sample_main. We then proceed to initialize
     * our various UI widget references by finding them in our layout:
     *
     *  * [.mDelayEditText] R.id.delay_time
     *  * [.mDurationTimeEditText] R.id.duration_time
     *  * [.mDeadlineEditText] R.id.deadline_time
     *  * [.mWiFiConnectivityRadioButton] R.id.checkbox_unmetered
     *  * [.mAnyConnectivityRadioButton] R.id.checkbox_any
     *  * [.mRequiresChargingCheckBox] R.id.checkbox_charging
     *  * [.mRequiresIdleCheckbox] R.id.checkbox_idle
     *
     * We then initialize our field `ComponentName mServiceComponent` with a new instance for
     * `MyJobService`, and `IncomingMessageHandler mHandler` with a new instance.
     *
     * @param savedInstanceState we do not use `onSaveInstanceState` so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)

        // Set up UI.
        mDelayEditText = findViewById(R.id.delay_time)
        mDurationTimeEditText = findViewById(R.id.duration_time)
        mDeadlineEditText = findViewById(R.id.deadline_time)
        mWiFiConnectivityRadioButton = findViewById(R.id.checkbox_unmetered)
        mAnyConnectivityRadioButton = findViewById(R.id.checkbox_any)
        mRequiresChargingCheckBox = findViewById(R.id.checkbox_charging)
        mRequiresIdleCheckbox = findViewById(R.id.checkbox_idle)
        mServiceComponent = ComponentName(this, MyJobService::class.java)
        mHandler = IncomingMessageHandler(this)
    }

    /**
     * Called when you are no longer visible to the user. We call `stopService` to stop the
     * running of `MyJobService` then call our super's implementation of `onStop`.
     */
    override fun onStop() {
        // A service can be "started" and/or "bound". In this case, it's "started" by this Activity
        // and "bound" to the JobScheduler (also called "Scheduled" by the JobScheduler). This call
        // to stopService() won't prevent scheduled jobs to be processed. However, failing
        // to call stopService() would keep it alive indefinitely.
        stopService(Intent(this, MyJobService::class.java))
        super.onStop()
    }

    /**
     * Called after [.onCreate]  or after [.onRestart] when
     * the activity had been stopped, but is now again being displayed to the
     * user.  It will be followed by [.onResume].
     *
     *
     * We create `Intent startServiceIntent` for starting the service `MyJobService`,
     * initialize `Messenger messengerIncoming` with a new instance specifying `mHandler`
     * as its handler for messages and add it as an extra under the key MESSENGER_INTENT_KEY to
     * `startServiceIntent`. We then call `startService` to request that `startServiceIntent`
     * be started.
     */
    override fun onStart() {
        super.onStart()
        // Start service and provide it a way to communicate with this class.
        val startServiceIntent = Intent(this, MyJobService::class.java)
        val messengerIncoming = Messenger(mHandler)
        startServiceIntent.putExtra(MESSENGER_INTENT_KEY, messengerIncoming)
        startService(startServiceIntent)
    }

    /**
     * Executed when user clicks on SCHEDULE JOB, specified by android:onClick="scheduleJob" element
     * in our layout file. We initialize `JobInfo.Builder builder` with a builder constructed
     * to build a `JobInfo` addressed for `mServiceComponent` (which is a component name
     * for `MyJobService`) using `mJobId` as the job id (which we post increment to be
     * ready for the next use). We set `String delay` to the string the user may have entered
     * for the delay in `EditText mDelayEditText`, and if it is not the empty string we set the
     * minimum latency of `builder` to 1000 times the long value of `delay`.
     *
     *
     * We set `String deadline` to the string the user may have entered for the deadline in
     * `EditText mDeadlineEditText`, and if it is not the empty string we set the maximum
     * scheduling latency of `builder` to 1000 times the long value of `deadline`.
     *
     *
     * We set `boolean requiresUnmetered` to the checked state of `mWiFiConnectivityRadioButton`
     * ("WiFi") and set `boolean requiresAnyConnectivity` to the checked state of
     * `mAnyConnectivityRadioButton` ("Any"). If `requiresUnmetered` is true we set the
     * required network type of `builder` to NETWORK_TYPE_UNMETERED, else if `requiresAnyConnectivity`
     * is true we set the required network type of `builder` to NETWORK_TYPE_ANY.
     *
     *
     * We set the requires device idle state of `builder` to the checked state of `mRequiresIdleCheckbox`
     * ("Idle"), and the requires device charging state of `builder` to the checked state of
     * `mRequiresChargingCheckBox` ("Charging").
     *
     *
     * We initialize `PersistableBundle extras` with a new instance. Set `String workDuration`
     * to the text contained in `EditText mDurationTimeEditText` ("Work duration:") and if it is
     * the empty string we set it to "1". We then store 1000 times the long value of `workDuration`
     * under the key WORK_DURATION_KEY ("com.example.android.jobscheduler.WORK_DURATION_KEY") in
     * `extras`. We then set the extras of `builder` to `extras`.
     *
     *
     * Finally we initialize `JobScheduler tm` with a handle to the system level service JOB_SCHEDULER_SERVICE
     * then build `builder` passing it to the `schedule` method of `tm` to schedule it
     * to be executed (it will replace any currently scheduled job with the same ID with the new information
     * in the JobInfo. If a job with the given ID is currently running, it will be stopped).
     *
     * @param v View that was clicked, unused.
     */
    fun scheduleJob(v: View?) {
        val builder = JobInfo.Builder(mJobId++, mServiceComponent!!)
        val delay = mDelayEditText!!.text.toString()
        if (!TextUtils.isEmpty(delay)) {
            builder.setMinimumLatency(java.lang.Long.valueOf(delay) * 1000)
        }
        val deadline = mDeadlineEditText!!.text.toString()
        if (!TextUtils.isEmpty(deadline)) {
            builder.setOverrideDeadline(java.lang.Long.valueOf(deadline) * 1000)
        }
        val requiresUnmetered = mWiFiConnectivityRadioButton!!.isChecked
        val requiresAnyConnectivity = mAnyConnectivityRadioButton!!.isChecked
        if (requiresUnmetered) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        } else if (requiresAnyConnectivity) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        }
        builder.setRequiresDeviceIdle(mRequiresIdleCheckbox!!.isChecked)
        builder.setRequiresCharging(mRequiresChargingCheckBox!!.isChecked)

        // Extras, work duration.
        val extras = PersistableBundle()
        var workDuration = mDurationTimeEditText!!.text.toString()
        if (TextUtils.isEmpty(workDuration)) {
            workDuration = "1"
        }
        extras.putLong(WORK_DURATION_KEY, java.lang.Long.valueOf(workDuration) * 1000)
        builder.setExtras(extras)

        // Schedule job
        Log.d(TAG, "Scheduling job")
        val tm = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        tm.schedule(builder.build())
    }

    /**
     * Executed when user clicks on CANCEL ALL, specified by android:onClick="cancelAllJobs" element
     * in our layout file. We initialize `JobScheduler tm` with a handle to the system level
     * service JOB_SCHEDULER_SERVICE, then call its `cancelAll()` method to cancel all jobs
     * that have been scheduled by our application.
     *
     * @param v View that was clicked, unused.
     */
    fun cancelAllJobs(v: View?) {
        val tm = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        tm.cancelAll()
        Toast.makeText(this@MainActivity, R.string.all_jobs_cancelled, Toast.LENGTH_SHORT).show()
    }

    /**
     * Executed when user clicks on FINISH LAST TASK, specified by android:onClick="finishJob" element
     * in our layout file. We initialize `JobScheduler jobScheduler` with a handle to the system
     * level service JOB_SCHEDULER_SERVICE. We use the `getAllPendingJobs()` method of `jobScheduler`
     * to retrieve all jobs that have been scheduled by application into `List<JobInfo> allPendingJobs`.
     * If the size of `allPendingJobs` is greater than 0, we retrieve the id of the 0'th job into
     * `int jobId`, then pass that to the `cancel` method of `jobScheduler` to cancel
     * the last job. Having done this we toast `jobId` in a formatted string: "Cancelled job %d".
     * If there are 0 entries in `allPendingJobs` we toast the string "No jobs to cancel".
     * (Note this actually cancels a random task, not necessarily the last!)
     *
     * @param v View that was clicked, unused
     */
    fun finishJob(v: View?) {
        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val allPendingJobs = jobScheduler.allPendingJobs
        if (allPendingJobs.size > 0) {
            // Finish the last one
            val jobId = allPendingJobs[0].id
            jobScheduler.cancel(jobId)
            Toast.makeText(
                this@MainActivity, String.format(getString(R.string.cancelled_job), jobId),
                Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this@MainActivity, getString(R.string.no_jobs_to_cancel),
                Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * A [Handler] allows you to send messages associated with a thread. A [Messenger]
     * uses this handler to communicate from [MyJobService]. It's also used to make
     * the start and stop views blink for a short period of time.
     * Our constructor. We call our super's zero argument constructor with [Looper.myLooper]
     * then save our parameter `MainActivity activity` in our field `mActivity`.
     *
     * @param activity this when called from `onCreate` method of `MainActivity`
     *
     */
    private class IncomingMessageHandler(activity: MainActivity) : Handler(Looper.myLooper()!!) {
        /**
         * `MainActivity` passed to our constructor. Prevent possible leaks with a weak reference.
         */
        private val mActivity: WeakReference<MainActivity>

        init {
            mActivity = WeakReference(activity)
        }

        /**
         * We implement this to receive messages. First we retrieve `MainActivity mainActivity`
         * from our `mActivity` weak reference and if it is null we just return. We initialize
         * `View showStartView` by finding the view with id R.id.onstart_textview, and
         * `View showStopView` by finding the view with id R.id.onstop_textview. We declare
         * `Message m` then switch on the value of the `what` field of our parameter
         * `msg`:
         *
         *  *
         * MSG_COLOR_START: Start received, we set the background color of `showStartView`
         * to R.color.start_received (#00FF00 green), and call our method `updateParamsTextView`
         * with the `obj` field of `msg` and the string "started" (this displays the
         * text "Job ID `valueOf(obj)` started" in the textview with id R.id.task_params)
         * We then create a message for `m` with the what field set to MSG_UNCOLOR_START
         * and send it to ourselves with a delay of 1000ms (this will clear the background color
         * after 1000ms and clear the textview).
         *
         *  *
         * MSG_COLOR_STOP: Stop received, we set the background color of `showStopView`
         * to R.color.stop_received (#FF0000 red), and call our method `updateParamsTextView`
         * with the `obj` field of `msg` and the string "stopped" (this displays the
         * text "Job ID `valueOf(obj)` stopped" in the textview with id R.id.task_params)
         * We then create a message for `m` with the what field set to MSG_UNCOLOR_STOP
         * and send it to ourselves with a delay of 2000ms (this will clear the background color
         * after 2000ms and clear the textview).
         *
         *  *
         * MSG_UNCOLOR_START: We set the background color of `showStartView` to
         * R.color.none_received (#999999 gray), and call our method `updateParamsTextView`
         * with null for the `jobId` and the empty string for the `action` (clears
         * all the changes done by a previous MSG_COLOR_START message).
         *
         *  *
         * MSG_UNCOLOR_STOP: We set the background color of `showStopView` to
         * R.color.none_received (#999999 gray), and call our method `updateParamsTextView`
         * with null for the `jobId` and the empty string for the `action` (clears
         * all the changes done by a previous MSG_COLOR_STOP message).
         *
         *
         *
         * @param msg `Message` containing a description (`what` field) and arbitrary data
         * object (`obj` field)
         */
        override fun handleMessage(msg: Message) {
            val mainActivity = mActivity.get()
                ?: // Activity is no longer available, exit.
                return
            val showStartView = mainActivity.findViewById<View>(R.id.onstart_textview)
            val showStopView = mainActivity.findViewById<View>(R.id.onstop_textview)
            val m: Message
            when (msg.what) {
                MSG_COLOR_START -> {
                    // Start received, turn on the indicator and show text.
                    showStartView.setBackgroundColor(getColor(R.color.start_received))
                    updateParamsTextView(msg.obj, "started")

                    // Send message to turn it off after a second.
                    m = Message.obtain(this, MSG_UNCOLOR_START)
                    sendMessageDelayed(m, 1000L)
                }

                MSG_COLOR_STOP -> {
                    // Stop received, turn on the indicator and show text.
                    showStopView.setBackgroundColor(getColor(R.color.stop_received))
                    updateParamsTextView(msg.obj, "stopped")

                    // Send message to turn it off after a second.
                    m = obtainMessage(MSG_UNCOLOR_STOP)
                    sendMessageDelayed(m, 2000L)
                }

                MSG_UNCOLOR_START -> {
                    showStartView.setBackgroundColor(getColor(R.color.none_received))
                    updateParamsTextView(null, "")
                }

                MSG_UNCOLOR_STOP -> {
                    showStopView.setBackgroundColor(getColor(R.color.none_received))
                    updateParamsTextView(null, "")
                }
            }
        }

        /**
         * Updates text displayed in the textview with id R.id.task_params. First we initialize our
         * variable `TextView paramsTextView` by finding the view with id R.id.task_params. If
         * our parameter `jobId` is null we set the text of `paramsTextView` to the empty
         * string and return. We initialize `String jobIdText` with the value of our parameter
         * `jobId` then set the text of `paramsTextView` to a string that formats the
         * strings `jobIdText` and `action` with the format "Job ID %s %s".
         *
         * @param jobId job id that has started or stopped, or null
         * @param action Action ("started" or "stopped" or "") that has occurred
         */
        private fun updateParamsTextView(jobId: Any?, action: String) {
            val paramsTextView = mActivity.get()!!.findViewById<TextView>(R.id.task_params)
            if (jobId == null) {
                paramsTextView.text = ""
                return
            }
            val jobIdText = jobId.toString()
            paramsTextView.text = String.format("Job ID %s %s", jobIdText, action)
        }

        /**
         * Convenience function for converting a color resource reference to an integer color.
         *
         * @param color color resource reference
         * @return A single color value in the form 0xAARRGGBB.
         */
        private fun getColor(@ColorRes color: Int): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mActivity.get()!!.resources.getColor(color, null)
            } else {
                @Suppress("DEPRECATION") // Needed for SDK < M
                mActivity.get()!!.resources.getColor(color)
            }
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        private val TAG = MainActivity::class.java.simpleName

        /**
         * User-defined message code for clearing Start received color 1000ms after MSG_COLOR_START
         */
        const val MSG_UNCOLOR_START: Int = 0

        /**
         * User-defined message code for clearing Stop received color 2000ms after MSG_COLOR_STOP
         */
        const val MSG_UNCOLOR_STOP: Int = 1

        /**
         * User-defined message code for Start received, turns on the "started" indicator and shows text.
         */
        const val MSG_COLOR_START: Int = 2

        /**
         * User-defined message code for Stop received, turns on the "stopped" indicator and shows text.
         */
        const val MSG_COLOR_STOP: Int = 3

        /**
         * Intent key for storing of `Messenger` for `MyJobService` to use to send us messages.
         * ("com.example.android.jobscheduler.MESSENGER_INTENT_KEY")
         */
        const val MESSENGER_INTENT_KEY: String = BuildConfig.APPLICATION_ID + ".MESSENGER_INTENT_KEY"

        /**
         * Intent key for storing of the duration that the job is supposed to last, `MyJobService`
         * waits for this long in milliseconds before quiting "com.example.android.jobscheduler.WORK_DURATION_KEY"
         */
        const val WORK_DURATION_KEY: String = BuildConfig.APPLICATION_ID + ".WORK_DURATION_KEY"
    }
}