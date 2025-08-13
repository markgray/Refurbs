/*
 * Copyright (C) 2008 The Android Open Source Project
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
@file:Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate",
    "RedundantSuppression"
)

package com.example.android.displayingbitmaps.util

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.android.displayingbitmaps.util.Utils.hasHoneycomb
import java.util.ArrayDeque
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A copy of java's AsyncTask
 */
abstract class AsyncTask<Params, Progress, Result> {
    /**
     * The `WorkerRunnable` (implements `Callable` with a `Params[] mParams` field
     * set by its constructor)
     */
    private val mWorker: WorkerRunnable<Params, Result>

    /**
     * Cancellable asynchronous computation used to wrap a Callable or Runnable object, we use it to
     * wrap `mWorker`.
     */
    private val mFuture: FutureTask<Result>
    /**
     * Returns the current status of this task. We just return the value in our field `mStatus`
     *
     * @return The current status.
     */
    /**
     * Status of this task, one of PENDING, RUNNING, or FINISHED. This is used by the method
     * `executeOnExecutor` to make sure a task is run only once (If the status is not
     * PENDING an exception is thrown).
     */
    @Volatile
    var status: Status = Status.PENDING
        private set

    /**
     * Flag indicating that we have been canceled, set by our `cancel` method, queried by our
     * `isCancelled` method. If set our `finish` method calls `onCancelled` instead
     * of `onPostExecute` when the task in FINISHED.
     */
    private val mCancelled = AtomicBoolean()

    /**
     * Flag indicating that the task has been invoked, set in the override of the `call` method
     * of the `Callable` interface of `WorkerRunnable<Params, Result> mWorker`
     */
    private val mTaskInvoked = AtomicBoolean()

    /**
     * The executor below serializes the submission of tasks to the executor THREAD_POOL_EXECUTOR.
     * It appears to be used only to send commands to an instance of `CacheAsyncTask` in
     * `ImageWorker`.
     */
    @RequiresApi(11)
    private open class SerialExecutor : Executor {
        /**
         * Our queue of `Runnable` tasks.
         */
        val mTasks = ArrayDeque<Runnable>()

        /**
         * The currently executing `Runnable`.
         */
        var mActive: Runnable? = null

        /**
         * Executes the given command at some time in the future. First we insert an anonymous
         * `Runnable` class whose `run` override calls the `run` override of our
         * parameter `Runnable r` then calls our `scheduleNext` to run the next
         * `Runnable` on our `mTasks` Deque. Having added that `Runnable` we
         * check whether `Runnable mActive` is null (no task running) and if so we call
         * our `scheduleNext` method to start the ball rolling.
         *
         * @param r the runnable task
         */
        @Synchronized
        override fun execute(r: Runnable) {
            mTasks.offer(Runnable
            /**
             * Called when the `execute` method of this `Runnable` is called. Wrapped
             * in a try block we call the `run` method of `Runnable r`, with the
             * finally block calling the method `scheduleNext` to execute the next task
             * in our deque `mTasks` when `run` is done or dies for some reason.
             */
            /**
             * Called when the `execute` method of this `Runnable` is called. Wrapped
             * in a try block we call the `run` method of `Runnable r`, with the
             * finally block calling the method `scheduleNext` to execute the next task
             * in our deque `mTasks` when `run` is done or dies for some reason.
             */
            {
                try {
                    r.run()
                } finally {
                    scheduleNext()
                }
            })
            if (mActive == null) {
                scheduleNext()
            }
        }

        /**
         * Executes the next task in our deque `mTasks` if a task is not already running. We
         * set our field `Runnable mActive` to the head task of the queue `mTasks`, and
         * if that is not null, we call the `execute` method of THREAD_POOL_EXECUTOR to run it
         */
        @Synchronized
        protected fun scheduleNext() {
            if (mTasks.poll().also { mActive = it } != null) {
                THREAD_POOL_EXECUTOR.execute(mActive)
            }
        }
    }

    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    enum class Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,

        /**
         * Indicates that the task is running.
         */
        RUNNING,

        /**
         * Indicates that [AsyncTask.onPostExecute] has finished.
         */
        FINISHED
    }

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread. First we
     * initialize our field `WorkerRunnable<Params, Result> mWorker` with an anonymous instance
     * whose `call` override first sets `mTaskInvoked` to true, then sets the thread
     * priority to THREAD_PRIORITY_BACKGROUND, and returns the value returned by the `postResult`
     * method when it operates on the value returned by the `doInBackground` method when given
     * our field `Params[] mParams`. Then we initialize `FutureTask<Result> mFuture` with
     * an anonymous instance whose `done` override calls our `postResultIfNotInvoked`
     * method with the result of the computation (waiting for it if necessary).
     */
    init {
        mWorker = object : WorkerRunnable<Params, Result>() {
            /**
             * Computes a result, or throws an exception if unable to do so. First we set `mTaskInvoked`
             * to true, then set the thread priority to THREAD_PRIORITY_BACKGROUND, and return the value
             * returned by the `postResult` method when it operates on the value returned by the
             * `doInBackground` method when given our field `Params[] mParams`.
             *
             * @return computed result
             */
            override fun call(): Result {
                mTaskInvoked.set(true)
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                return postResult(doInBackground(*mParams))
            }
        }
        mFuture = object : FutureTask<Result>(mWorker) {
            /**
             * Protected method invoked when this task transitions to state `isDone`. Wrapped
             * in a try block we call our `postResultIfNotInvoked` method with the result of
             * the computation (waiting for it if necessary). If we catch InterruptedException we log
             * the error, if we catch ExecutionException we throw RuntimeException, and if we catch
             * CancellationException (our task was canceled) we call `postResultIfNotInvoked`
             * with null.
             */
            override fun done() {
                try {
                    postResultIfNotInvoked(get())
                } catch (e: InterruptedException) {
                    Log.w(LOG_TAG, e)
                } catch (e: ExecutionException) {
                    throw RuntimeException("An error occurred while executing doInBackground()",
                        e.cause)
                } catch (e: CancellationException) {
                    postResultIfNotInvoked(null)
                }
            }
        }
    }

    /**
     * Convenience function to call `postResult` with our parameter `Result result` iff
     * our flag `wasTaskInvoked` is false.
     *
     * @param result result of background computation.
     */
    private fun postResultIfNotInvoked(result: Result?) {
        val wasTaskInvoked = mTaskInvoked.get()
        if (!wasTaskInvoked) {
            postResult(result!!)
        }
    }

    /**
     * Posts the `Result result` to the main thread. First we create `Message message`
     * with a `what` field of MESSAGE_POST_RESULT, and an `obj` field containing our
     * parameter `Result result` wrapped in a new instance of `AsyncTaskResult`. We then
     * send this Message to the `sHandler` whose `handleMessage` passes the result to
     * the `finish` method of this which calls the `onPostExecute` method with the
     * result (or the `onCancelled` method with result if the thread was canceled). We then
     * return `result` to the caller.
     *
     * @param result result of background computation
     * @return our parameter `result`
     */
    private fun postResult(result: Result): Result {
        val message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
            AsyncTaskResult(this, result))
        message.sendToTarget()
        return result
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to [.execute]
     * by the caller of this task.
     *
     *
     * This method can call [.publishProgress] to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     *
     * @see .onPreExecute
     * @see .onPostExecute
     *
     * @see .publishProgress
     */
    protected abstract fun doInBackground(vararg params: Params): Result

    /**
     * Runs on the UI thread before [.doInBackground].
     *
     * @see .onPostExecute
     *
     * @see .doInBackground
     */
    protected fun onPreExecute() {}

    /**
     * Runs on the UI thread after [.doInBackground]. The
     * specified result is the value returned by [.doInBackground].
     *
     *
     * This method won't be invoked if the task was cancelled.
     *
     * @param result The result of the operation computed by [.doInBackground].
     *
     * @see .onPreExecute
     *
     * @see .doInBackground
     *
     * @see .onCancelled
     */
    protected open fun onPostExecute(result: Result) {}

    /**
     * Runs on the UI thread after [.publishProgress] is invoked.
     * The specified values are the values passed to [.publishProgress].
     *
     * @param values The values indicating progress.
     *
     * @see .publishProgress
     *
     * @see .doInBackground
     */
    protected fun onProgressUpdate(vararg values: Array<out Any?>) {}

    /**
     * Runs on the UI thread after [.cancel] is invoked and
     * [.doInBackground] has finished.
     *
     *
     * The default implementation simply invokes [.onCancelled] and
     * ignores the result. If you write your own implementation, do not call
     * `super.onCancelled(result)`.
     *
     *
     * We just call our zero parameter `onCancelled` method.
     *
     * @param result The result, if any, computed in
     * [.doInBackground], can be null
     *
     * @see .cancel
     * @see .isCancelled
     */
    protected open fun onCancelled(result: Result) {
        onCancelled()
    }

    /**
     * Applications should preferably override [.onCancelled].
     * This method is invoked by the default implementation of
     * [.onCancelled].
     *
     *
     * Runs on the UI thread after [.cancel] is invoked and
     * [.doInBackground] has finished.
     *
     * @see .onCancelled
     * @see .cancel
     * @see .isCancelled
     */
    protected fun onCancelled() {}
    /**
     * If `true` we have been canceled
     */
    val isCancelled: Boolean
        /**
         * Returns <tt>true</tt> if this task was cancelled before it completed
         * normally. If you are calling [.cancel] on the task,
         * the value returned by this method should be checked periodically from
         * [.doInBackground] to end the task as soon as possible.
         *
         *
         * We just return the value of our field `AtomicBoolean mCancelled`.
         *
         * @return <tt>true</tt> if task was cancelled before it completed
         *
         * @see .cancel
         */
        get() = mCancelled.get()

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run. If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     *
     * Calling this method will result in [.onCancelled] being
     * invoked on the UI thread after [.doInBackground]
     * returns. Calling this method guarantees that [.onPostExecute]
     * is never invoked. After invoking this method, you should check the
     * value returned by [.isCancelled] periodically from
     * [.doInBackground] to finish the task as early as
     * possible.
     *
     *
     * We set our field `AtomicBoolean mCancelled` to true, then return the value returned
     * by the `cancel` method of `FutureTask<Result> mFuture` when called with our
     * parameter `mayInterruptIfRunning`.
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete.
     *
     * @return <tt>false</tt> if the task could not be cancelled,
     * typically because it has already completed normally;
     * <tt>true</tt> otherwise
     *
     * @see .isCancelled
     * @see .onCancelled
     */
    fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        mCancelled.set(true)
        return mFuture.cancel(mayInterruptIfRunning)
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result. We return the value returned by the `get`
     * method of `FutureTask<Result> mFuture` (which waits if necessary
     * for the computation to complete, and then retrieves its result).
     *
     * @return The computed result.
     *
     * @throws java.util.concurrent.CancellationException If the computation was cancelled.
     * @throws java.util.concurrent.ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     * while waiting.
     */
    @Throws(InterruptedException::class, ExecutionException::class)
    fun get(): Result {
        return mFuture.get()
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result. We return the value
     * returned by the `get` method of `FutureTask<Result> mFuture`
     * (which waits if necessary for at most `timeout` milliseconds
     * for the computation to complete, and then retrieves its result).
     *
     * @param timeout Time to wait before cancelling the operation.
     * @param unit The time unit for the timeout.
     *
     * @return The computed result.
     *
     * @throws java.util.concurrent.CancellationException If the computation was cancelled.
     * @throws java.util.concurrent.ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     * while waiting.
     * @throws java.util.concurrent.TimeoutException If the wait timed out.
     */
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    operator fun get(timeout: Long, unit: TimeUnit?): Result {
        return mFuture[timeout, unit]
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     *
     * Note: this function schedules the task on a queue for a single background
     * thread or pool of threads depending on the platform version.  When first
     * introduced, AsyncTasks were executed serially on a single background thread.
     * Starting with [android.os.Build.VERSION_CODES.DONUT], this was changed
     * to a pool of threads allowing multiple tasks to operate in parallel. Starting
     * [android.os.Build.VERSION_CODES.HONEYCOMB], tasks are back to being
     * executed on a single thread to avoid common application errors caused
     * by parallel execution.  If you truly want parallel execution, you can use
     * the [.executeOnExecutor] version of this method
     * with [.THREAD_POOL_EXECUTOR]; however, see commentary there for warnings
     * on its use.
     *
     *
     * This method must be invoked on the UI thread.
     *
     *
     * We just call the `executeOnExecutor` method using `sDefaultExecutor`
     * as the executor and our parameter `params` as the parameters to pass.
     *
     * @param params The parameters of the task.
     *
     * @return This instance of AsyncTask.
     *
     * @throws IllegalStateException If [.getStatus] returns either
     * [AsyncTask.Status.RUNNING] or [AsyncTask.Status.FINISHED].
     *
     * @see .executeOnExecutor
     * @see .execute
     */
    fun execute(vararg params: Params): AsyncTask<Params, Progress, Result> {
        return executeOnExecutor(sDefaultExecutor, *params)
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     *
     * This method is typically used with [.THREAD_POOL_EXECUTOR] to
     * allow multiple tasks to run in parallel on a pool of threads managed by
     * AsyncTask, however you can also use your own [java.util.concurrent.Executor] for custom
     * behavior.
     *
     *
     * *Warning:* Allowing multiple tasks to run in parallel from
     * a thread pool is generally *not* what one wants, because the order
     * of their operation is not defined.  For example, if these tasks are used
     * to modify any state in common (such as writing a file due to a button click),
     * there are no guarantees on the order of the modifications.
     * Without careful work it is possible in rare cases for the newer version
     * of the data to be over-written by an older one, leading to obscure data
     * loss and stability issues.  Such changes are best
     * executed in serial; to guarantee such work is serialized regardless of
     * platform version you can use this function with [.SERIAL_EXECUTOR].
     *
     *
     * This method must be invoked on the UI thread.
     *
     *
     * First we make sure our status `mStatus` is PENDING, and if not we throw
     * IllegalStateException for RUNNING, or FINISHED status. If it is PENDING we set
     * it to RUNNING, call `onPreExecute`, store our parameter `params` in
     * the `mParams` field of `WorkerRunnable<Params, Result> mWorker` then
     * call the `execute` method of our parameter `exec` to execute
     * `FutureTask<Result> mFuture`.
     *
     * @param exec The executor to use.  [.THREAD_POOL_EXECUTOR] is available as a
     * convenient process-wide thread pool for tasks that are loosely coupled.
     * @param params The parameters of the task.
     *
     * @return This instance of AsyncTask.
     *
     * @throws IllegalStateException If [.getStatus] returns either
     * [AsyncTask.Status.RUNNING] or [AsyncTask.Status.FINISHED].
     *
     * @see .execute
     */
    fun executeOnExecutor(exec: Executor,
                          vararg params: Params): AsyncTask<Params, Progress, Result> {
        if (status != Status.PENDING) {
            when (status) {
                Status.RUNNING -> throw IllegalStateException("Cannot execute task:"
                    + " the task is already running.")

                Status.FINISHED -> throw IllegalStateException("Cannot execute task:"
                    + " the task has already been executed "
                    + "(a task can be executed only once)")

                else -> {}
            }
        }
        status = Status.RUNNING
        onPreExecute()
        mWorker.mParams = params as Array<Params>
        exec.execute(mFuture)
        return this
    }

    /**
     * This method can be invoked from [.doInBackground] to
     * publish updates on the UI thread while the background computation is
     * still running. Each call to this method will trigger the execution of
     * [.onProgressUpdate] on the UI thread.
     *
     *
     * [.onProgressUpdate] will not be called if the task has been
     * canceled.
     *
     *
     * If this task has not been canceled, we create a message using our UI handler `sHandler`
     * whose `what` field is MESSAGE_POST_PROGRESS, and whose `obj` field is an
     * `AsyncTaskResult` with this as its task, and our parameter `values` as its data,
     * then send the message to the target `sHandler` (whose `handleMessage` method will
     * call the `onProgressUpdate` method of the `mTask` field of the `obj` field
     * (this) with our parameter `values`.
     *
     * @param values The progress values to update the UI with.
     *
     * @see .onProgressUpdate
     *
     * @see .doInBackground
     */
    @Suppress("unused")
    protected fun publishProgress(vararg values: Progress) {
        if (!isCancelled) {
            sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                AsyncTaskResult(this, *values)).sendToTarget()
        }
    }

    /**
     * Called by the `handleMessage` method of our UI handler in response to a MESSAGE_POST_RESULT
     * message. If our task had been cancelled we call our `onCancelled` method with our parameter
     * `result`, otherwise we call the `onPostExecute` with our parameter `result`.
     * In either case we set our status `mStatus` to FINISHED.
     *
     * @param result result returned by the background thread
     */
    private fun finish(result: Any?) {
        if (isCancelled) {
            onCancelled(result as Result)
        } else {
            onPostExecute(result as Result)
        }
        status = Status.FINISHED
    }

    /**
     * `Handler` that runs on the UI thread.
     * @noinspection deprecation
     */
    private class InternalHandler : Handler(Looper.myLooper()!!) {
        /**
         * Called when we have a message to receive. We cast the `obj` field of our parameter
         * `msg` to initialize `AsyncTaskResult result`, then we switch on the `what`
         * field of `msg`:
         *
         *  *
         * MESSAGE_POST_RESULT: we call the `finish` method of the `mTask` field
         * of `result` with the value `result.mData[0]`, then break.
         *
         *  *
         * MESSAGE_POST_PROGRESS: we call the `onProgressUpdate` method of the
         * `mTask` field of `result` with the value `result.mData`,
         * then break.
         *
         *
         *
         * @param msg Message from the global message pool we are to handle
         */
        override fun handleMessage(msg: Message) {
            val result = msg.obj as AsyncTaskResult<*>
            when (msg.what) {
                MESSAGE_POST_RESULT ->  // There is only one result
                    result.mTask.finish(result.mData[0])

                MESSAGE_POST_PROGRESS -> result.mTask.onProgressUpdate(result.mData)
            }
        }
    }

    /**
     * Class that adds a field `Params[] mParams` to the `Callable<Result>` class.
     *
     * @param <Params> type of our parameters field
     * @param <Result> type of our return value
    </Result></Params> */
    private abstract class WorkerRunnable<Params, Result> : Callable<Result> {
        lateinit var mParams: Array<Params>
    }

    /**
     * Simple holding class intended to hold the data returned from an `AsyncTask`
     *
     * @param <Data> type of data we are holding.
    </Data> */
    private class AsyncTaskResult<Data>(
        /**
         * `AsyncTask` we belong to, used in the `handleMessage` method of `InternalHandler`
         * to route the data to the correct task.
         */
        val mTask: AsyncTask<*, *, *>, vararg data: Data) {
        /**
         * Data returned by the `AsyncTask`
         */
        @Suppress("JoinDeclarationAndAssignment") // Easier to debug this way
        val mData: Array<Data>

        /**
         * Our constructor, simply saves our parameters in our fields.
         *
         * param task `AsyncTask` we belong to
         * param data Data returned by the `AsyncTask`
         * @noinspection rawtypes
         */
        init {
            mData = data as Array<Data>
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val LOG_TAG = "AsyncTask"

        /**
         * The number of threads to keep in the pool of our `ThreadPoolExecutor THREAD_POOL_EXECUTOR`,
         * even if they are idle
         */
        private const val CORE_POOL_SIZE = 5

        /**
         * The maximum number of threads to allow in the pool of our `ThreadPoolExecutor THREAD_POOL_EXECUTOR`.
         */
        private const val MAXIMUM_POOL_SIZE = 128

        /**
         * When the number of threads is greater than the core, this is the maximum time that excess idle
         * threads will wait for new tasks before terminating. The units used in the constructor for
         * `Executor THREAD_POOL_EXECUTOR` is `TimeUnit.SECONDS` so 1 second in our case.
         */
        private const val KEEP_ALIVE = 1

        /**
         * `ThreadFactory` to use when the executor creates a new thread
         */
        private val sThreadFactory: ThreadFactory = object : ThreadFactory {
            /**
             * Number to use when forming the name of the thread created (its string value is appended to
             * the string "AsyncTask #"), it is incremented after the thread is created.
             */
            private val mCount = AtomicInteger(1)

            /**
             * Constructs a new `Thread`. We return a new instance of `Thread` which will run
             * `Runnable r` using the thread name formed by appending the string value of our field
             * `AtomicInteger mCount` to the string "AsyncTask #". `mCount` is incremented
             * after it is used.
             *
             * @param r a runnable to be executed by new thread instance
             * @return constructed thread, or `null` if the request to
             * create a thread is rejected
             */
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "AsyncTask #" + mCount.getAndIncrement())
            }
        }

        /**
         * The `BlockingQueue` to use for holding tasks before they are executed. This queue will
         * hold only the Runnable tasks submitted by the execute method. Used in constructing our field
         * `Executor THREAD_POOL_EXECUTOR`
         */
        private val sPoolWorkQueue: BlockingQueue<Runnable> = LinkedBlockingQueue(10)

        /**
         * An [java.util.concurrent.Executor] that can be used to execute tasks in parallel. It is
         * a `ThreadPoolExecutor` constructed to have CORE_POOL_SIZE (5) threads to kept in the
         * pool, even if they are idle, MAXIMUM_POOL_SIZE (128) maximum number of threads, a keep alive
         * in seconds of KEEP_ALIVE (1), `BlockingQueue<Runnable> sPoolWorkQueue` as its queue to
         * hold runnable tasks before they are executed, `sThreadFactory` as the thread factory
         * when the executor creates a new thread,  and a new `DiscardOldestPolicy` as its
         * handler to use when execution is blocked because the thread bounds and queue capacities are
         * reached (discards the oldest unhandled request and then retries execute, unless the executor
         * is shut down, in which case the task is discarded).
         */
        val THREAD_POOL_EXECUTOR: Executor = ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE.toLong(),
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory,
            ThreadPoolExecutor.DiscardOldestPolicy())

        /**
         * An [java.util.concurrent.Executor] that executes tasks one at a time in serial
         * order.  This serialization is global to a particular process. Uses `sThreadFactory`
         * as the thread factory when creating new threads.
         */
        val SERIAL_EXECUTOR: Executor = if (hasHoneycomb()) SerialExecutor() else Executors.newSingleThreadExecutor(sThreadFactory)

        /**
         * `Executor` that reuses two threads operating off a shared unbounded queue. Two threads
         * will exist until the executor is explicitly shutdown. `sThreadFactory` will be used to
         * create them, and replace them if they terminate due to thrown exceptions.
         */
        val DUAL_THREAD_EXECUTOR: Executor = Executors.newFixedThreadPool(2, sThreadFactory)

        /**
         * Message used to make the main thread post a result.
         */
        private const val MESSAGE_POST_RESULT = 0x1

        /**
         * Message used to make the main thread post progress.
         */
        private const val MESSAGE_POST_PROGRESS = 0x2

        /**
         * `InternalHandler` used to send messages to the main thread.
         */
        private val sHandler = InternalHandler()

        /**
         * Default `Executor`, settable by our `setDefaultExecutor` method.
         */
        @Volatile
        private var sDefaultExecutor = SERIAL_EXECUTOR

        /** Used to force static handler to be created.  */
        @Suppress("unused")
        fun init() {
            sHandler.looper
        }

        /** Sets the default executor  */
        @Suppress("unused")
        fun setDefaultExecutor(exec: Executor) {
            sDefaultExecutor = exec
        }

        /**
         * Convenience version of [.execute] for use with
         * a simple Runnable object. See [.execute] for more
         * information on the order of execution.
         *
         *
         * We just call the `execute` method of our field `Executor sDefaultExecutor` to
         * execute our parameter `Runnable runnable`.
         *
         * @param runnable `Runnable` to execute
         * @see .execute
         * @see .executeOnExecutor
         */
        @Suppress("unused")
        fun execute(runnable: Runnable?) {
            sDefaultExecutor.execute(runnable)
        }
    }
}