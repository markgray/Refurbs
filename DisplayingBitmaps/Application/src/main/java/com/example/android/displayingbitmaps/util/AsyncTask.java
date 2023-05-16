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

package com.example.android.displayingbitmaps.util;

import android.annotation.TargetApi;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * *************************************
 * Copied from JB release framework:
 * https://android.googlesource.com/platform/frameworks/base/+/jb-release/core/java/android/os/AsyncTask.java
 * <p>
 * so that threading behavior on all OS versions is the same and we can tweak behavior by using
 * executeOnExecutor() if needed.
 * <p>
 * There are 3 changes in this copy of AsyncTask:
 *    -pre-HC a single thread executor is used for serial operation
 *    (Executors.newSingleThreadExecutor) and is the default
 *    -the default THREAD_POOL_EXECUTOR was changed to use DiscardOldestPolicy
 *    -a new fixed thread pool called DUAL_THREAD_EXECUTOR was added
 * *************************************
 * <p>
 * AsyncTask enables proper and easy use of the UI thread. This class allows to
 * perform background operations and publish results on the UI thread without
 * having to manipulate threads and/or handlers.
 * <p>
 * AsyncTask is designed to be a helper class around {@link Thread} and {@link android.os.Handler}
 * and does not constitute a generic threading framework. AsyncTasks should ideally be
 * used for short operations (a few seconds at the most.) If you need to keep threads
 * running for long periods of time, it is highly recommended you use the various APIs
 * provided by the <code>java.util.concurrent</code> package such as {@link java.util.concurrent.Executor},
 * {@link java.util.concurrent.ThreadPoolExecutor} and {@link java.util.concurrent.FutureTask}.
 * <p>
 * An asynchronous task is defined by a computation that runs on a background thread and
 * whose result is published on the UI thread. An asynchronous task is defined by 3 generic
 * types, called <code>Params</code>, <code>Progress</code> and <code>Result</code>,
 * and 4 steps, called <code>onPreExecute</code>, <code>doInBackground</code>,
 * <code>onProgressUpdate</code> and <code>onPostExecute</code>.
 * <p>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * For more information about using tasks and threads, read the
 * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
 * Threads</a> developer guide.
 * </div>
 * <p>
 * <h2>Usage</h2>
 * AsyncTask must be subclassed to be used. The subclass will override at least
 * one method ({@link #doInBackground}), and most often will override a
 * second one ({@link #onPostExecute}.)
 * <p>
 * Here is an example of subclassing:
 * <pre class="prettyprint">
 * private class DownloadFilesTask extends AsyncTask&lt;URL, Integer, Long&gt; {
 *     protected Long doInBackground(URL... urls) {
 *         int count = urls.length;
 *         long totalSize = 0;
 *         for (int i = 0; i < count; i++) {
 *             totalSize += Downloader.downloadFile(urls[i]);
 *             publishProgress((int) ((i / (float) count) * 100));
 *             // Escape early if cancel() is called
 *             if (isCancelled()) break;
 *         }
 *         return totalSize;
 *     }
 *
 *     protected void onProgressUpdate(Integer... progress) {
 *         setProgressPercent(progress[0]);
 *     }
 *
 *     protected void onPostExecute(Long result) {
 *         showDialog("Downloaded " + result + " bytes");
 *     }
 * }
 * </pre>
 * <p>
 * Once created, a task is executed very simply:
 * <pre class="prettyprint">
 * new DownloadFilesTask().execute(url1, url2, url3);
 * </pre>
 * <p>
 * <h2>AsyncTask's generic types</h2>
 * The three types used by an asynchronous task are the following:
 * <ol>
 *     <li><code>Params</code>, the type of the parameters sent to the task upon
 *     execution.</li>
 *     <li><code>Progress</code>, the type of the progress units published during
 *     the background computation.</li>
 *     <li><code>Result</code>, the type of the result of the background
 *     computation.</li>
 * </ol>
 * Not all types are always used by an asynchronous task. To mark a type as unused,
 * simply use the type {@link Void}:
 * <pre>
 * private class MyTask extends AsyncTask&lt;Void, Void, Void&gt; { ... }
 * </pre>
 * <p>
 * <h2>The 4 steps</h2>
 * When an asynchronous task is executed, the task goes through 4 steps:
 * <ol>
 *     <li>{@link #onPreExecute()}, invoked on the UI thread immediately after the task
 *     is executed. This step is normally used to setup the task, for instance by
 *     showing a progress bar in the user interface.</li>
 *     <li>{@link #doInBackground}, invoked on the background thread
 *     immediately after {@link #onPreExecute()} finishes executing. This step is used
 *     to perform background computation that can take a long time. The parameters
 *     of the asynchronous task are passed to this step. The result of the computation must
 *     be returned by this step and will be passed back to the last step. This step
 *     can also use {@link #publishProgress} to publish one or more units
 *     of progress. These values are published on the UI thread, in the
 *     {@link #onProgressUpdate} step.</li>
 *     <li>{@link #onProgressUpdate}, invoked on the UI thread after a
 *     call to {@link #publishProgress}. The timing of the execution is
 *     undefined. This method is used to display any form of progress in the user
 *     interface while the background computation is still executing. For instance,
 *     it can be used to animate a progress bar or show logs in a text field.</li>
 *     <li>{@link #onPostExecute}, invoked on the UI thread after the background
 *     computation finishes. The result of the background computation is passed to
 *     this step as a parameter.</li>
 * </ol>
 * <p>
 * <h2>Cancelling a task</h2>
 * A task can be cancelled at any time by invoking {@link #cancel(boolean)}. Invoking
 * this method will cause subsequent calls to {@link #isCancelled()} to return true.
 * After invoking this method, {@link #onCancelled(Object)}, instead of
 * {@link #onPostExecute(Object)} will be invoked after {@link #doInBackground(Object[])}
 * returns. To ensure that a task is cancelled as quickly as possible, you should always
 * check the return value of {@link #isCancelled()} periodically from
 * {@link #doInBackground(Object[])}, if possible (inside a loop for instance.)
 * <p>
 * <h2>Threading rules</h2>
 * There are a few threading rules that must be followed for this class to
 * work properly:
 * <ul>
 *     <li>The AsyncTask class must be loaded on the UI thread. This is done
 *     automatically as of {@link android.os.Build.VERSION_CODES#JELLY_BEAN}.</li>
 *     <li>The task instance must be created on the UI thread.</li>
 *     <li>{@link #execute} must be invoked on the UI thread.</li>
 *     <li>Do not call {@link #onPreExecute()}, {@link #onPostExecute},
 *     {@link #doInBackground}, {@link #onProgressUpdate} manually.</li>
 *     <li>The task can be executed only once (an exception will be thrown if
 *     a second execution is attempted.)</li>
 * </ul>
 * <p>
 * <h2>Memory observability</h2>
 * AsyncTask guarantees that all callback calls are synchronized in such a way that the following
 * operations are safe without explicit synchronizations.
 * <ul>
 *     <li>Set member fields in the constructor or {@link #onPreExecute}, and refer to them
 *     in {@link #doInBackground}.
 *     <li>Set member fields in {@link #doInBackground}, and refer to them in
 *     {@link #onProgressUpdate} and {@link #onPostExecute}.
 * </ul>
 * <p>
 * <h2>Order of execution</h2>
 * When first introduced, AsyncTasks were executed serially on a single background
 * thread. Starting with {@link android.os.Build.VERSION_CODES#DONUT}, this was changed
 * to a pool of threads allowing multiple tasks to operate in parallel. Starting with
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, tasks are executed on a single
 * thread to avoid common application errors caused by parallel execution.
 * If you truly want parallel execution, you can invoke
 * {@link #executeOnExecutor(java.util.concurrent.Executor, Object[])} with
 * {@link #THREAD_POOL_EXECUTOR}.
 */
@SuppressWarnings({"WeakerAccess"})
public abstract class AsyncTask<Params, Progress, Result> {
    /**
     * TAG used for logging.
     */
    private static final String LOG_TAG = "AsyncTask";

    /**
     * The number of threads to keep in the pool of our {@code ThreadPoolExecutor THREAD_POOL_EXECUTOR},
     * even if they are idle
     */
    private static final int CORE_POOL_SIZE = 5;
    /**
     * The maximum number of threads to allow in the pool of our {@code ThreadPoolExecutor THREAD_POOL_EXECUTOR}.
     */
    private static final int MAXIMUM_POOL_SIZE = 128;
    /**
     * When the number of threads is greater than the core, this is the maximum time that excess idle
     * threads will wait for new tasks before terminating. The units used in the constructor for
     * {@code Executor THREAD_POOL_EXECUTOR} is {@code TimeUnit.SECONDS} so 1 second in our case.
     */
    private static final int KEEP_ALIVE = 1;

    /**
     * {@code ThreadFactory} to use when the executor creates a new thread
     */
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        /**
         * Number to use when forming the name of the thread created (its string value is appended to
         * the string "AsyncTask #"), it is incremented after the thread is created.
         */
        private final AtomicInteger mCount = new AtomicInteger(1);

        /**
         * Constructs a new {@code Thread}. We return a new instance of {@code Thread} which will run
         * {@code Runnable r} using the thread name formed by appending the string value of our field
         * {@code AtomicInteger mCount} to the string "AsyncTask #". {@code mCount} is incremented
         * after it is used.
         *
         * @param r a runnable to be executed by new thread instance
         * @return constructed thread, or {@code null} if the request to
         *         create a thread is rejected
         */
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    /**
     * The {@code BlockingQueue} to use for holding tasks before they are executed. This queue will
     * hold only the Runnable tasks submitted by the execute method. Used in constructing our field
     * {@code Executor THREAD_POOL_EXECUTOR}
     */
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<>(10);

    /**
     * An {@link java.util.concurrent.Executor} that can be used to execute tasks in parallel. It is
     * a {@code ThreadPoolExecutor} constructed to have CORE_POOL_SIZE (5) threads to kept in the
     * pool, even if they are idle, MAXIMUM_POOL_SIZE (128) maximum number of threads, a keep alive
     * in seconds of KEEP_ALIVE (1), {@code BlockingQueue<Runnable> sPoolWorkQueue} as its queue to
     * hold runnable tasks before they are executed, {@code sThreadFactory} as the thread factory
     * when the executor creates a new thread,  and a new {@code DiscardOldestPolicy} as its
     * handler to use when execution is blocked because the thread bounds and queue capacities are
     * reached (discards the oldest unhandled request and then retries execute, unless the executor
     * is shut down, in which case the task is discarded).
     */
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory,
            new ThreadPoolExecutor.DiscardOldestPolicy());

    /**
     * An {@link java.util.concurrent.Executor} that executes tasks one at a time in serial
     * order.  This serialization is global to a particular process. Uses {@code sThreadFactory}
     * as the thread factory when creating new threads.
     */
    public static final Executor SERIAL_EXECUTOR = Utils.hasHoneycomb() ? new SerialExecutor() :
            Executors.newSingleThreadExecutor(sThreadFactory);

    /**
     * {@code Executor} that reuses two threads operating off a shared unbounded queue. Two threads
     * will exist until the executor is explicitly shutdown. {@code sThreadFactory} will be used to
     * create them, and replace them if they terminate due to thrown exceptions.
     */
    public static final Executor DUAL_THREAD_EXECUTOR =
            Executors.newFixedThreadPool(2, sThreadFactory);

    /**
     * Message used to make the main thread post a result.
     */
    private static final int MESSAGE_POST_RESULT = 0x1;
    /**
     * Message used to make the main thread post progress.
     */
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    /**
     * {@code InternalHandler} used to send messages to the main thread.
     */
    private static final InternalHandler sHandler = new InternalHandler();

    /**
     * Default {@code Executor}, settable by our {@code setDefaultExecutor} method.
     */
    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
    /**
     * The {@code WorkerRunnable} (implements {@code Callable} with a {@code Params[] mParams} field
     * set by its constructor)
     */
    private final WorkerRunnable<Params, Result> mWorker;
    /**
     * Cancellable asynchronous computation used to wrap a Callable or Runnable object, we use it to
     * wrap {@code mWorker}.
     */
    private final FutureTask<Result> mFuture;

    /**
     * Status of this task, one of PENDING, RUNNING, or FINISHED. This is used by the method
     * {@code executeOnExecutor} to make sure a task is run only once (If the status is not
     * PENDING an exception is thrown).
     */
    private volatile Status mStatus = Status.PENDING;

    /**
     * Flag indicating that we have been canceled, set by our {@code cancel} method, queried by our
     * {@code isCancelled} method. If set our {@code finish} method calls {@code onCancelled} instead
     * of {@code onPostExecute} when the task in FINISHED.
     */
    private final AtomicBoolean mCancelled = new AtomicBoolean();
    /**
     * Flag indicating that the task has been invoked, set in the override of the {@code call} method
     * of the {@code Callable} interface of {@code WorkerRunnable<Params, Result> mWorker}
     */
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    /**
     * The executor below serializes the submission of tasks to the executor THREAD_POOL_EXECUTOR.
     * It appears to be used only to send commands to an instance of {@code CacheAsyncTask} in
     * {@code ImageWorker}.
     */
    @TargetApi(11)
    private static class SerialExecutor implements Executor {
        /**
         * Our queue of {@code Runnable} tasks.
         */
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
        /**
         * The currently executing {@code Runnable}.
         */
        Runnable mActive;

        /**
         * Executes the given command at some time in the future. First we insert an anonymous
         * {@code Runnable} class whose {@code run} override calls the {@code run} override of our
         * parameter {@code Runnable r} then calls our {@code scheduleNext} to run the next
         * {@code Runnable} on our {@code mTasks} Deque. Having added that {@code Runnable} we
         * check whether {@code Runnable mActive} is null (no task running) and if so we call
         * our {@code scheduleNext} method to start the ball rolling.
         *
         * @param r the runnable task
         */
        @Override
        public synchronized void execute(@NonNull final Runnable r) {
            mTasks.offer(new Runnable() {
                /**
                 * Called when the {@code execute} method of this {@code Runnable} is called. Wrapped
                 * in a try block we call the {@code run} method of {@code Runnable r}, with the
                 * finally block calling the method {@code scheduleNext} to execute the next task
                 * in our deque {@code mTasks} when {@code run} is done or dies for some reason.
                 */
                @Override
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        /**
         * Executes the next task in our deque {@code mTasks} if a task is not already running. We
         * set our field {@code Runnable mActive} to the head task of the queue {@code mTasks}, and
         * if that is not null, we call the {@code execute} method of THREAD_POOL_EXECUTOR to run it
         */
        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link AsyncTask#onPostExecute} has finished.
         */
        FINISHED,
    }

    /** Used to force static handler to be created. */
    @SuppressWarnings("unused")
    public static void init() {
        sHandler.getLooper();
    }

    /** Sets the default executor */
    @SuppressWarnings("unused")
    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread. First we
     * initialize our field {@code WorkerRunnable<Params, Result> mWorker} with an anonymous instance
     * whose {@code call} override first sets {@code mTaskInvoked} to true, then sets the thread
     * priority to THREAD_PRIORITY_BACKGROUND, and returns the value returned by the {@code postResult}
     * method when it operates on the value returned by the {@code doInBackground} method when given
     * our field {@code Params[] mParams}. Then we initialize {@code FutureTask<Result> mFuture} with
     * an anonymous instance whose {@code done} override calls our {@code postResultIfNotInvoked}
     * method with the result of the computation (waiting for it if necessary).
     */
    public AsyncTask() {
        mWorker = new WorkerRunnable<Params, Result>() {
            /**
             * Computes a result, or throws an exception if unable to do so. First we set {@code mTaskInvoked}
             * to true, then set the thread priority to THREAD_PRIORITY_BACKGROUND, and return the value
             * returned by the {@code postResult} method when it operates on the value returned by the
             * {@code doInBackground} method when given our field {@code Params[] mParams}.
             *
             * @return computed result
             */
            @Override
            public Result call() {
                mTaskInvoked.set(true);

                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                //noinspection unchecked
                return postResult(doInBackground(mParams));
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            /**
             * Protected method invoked when this task transitions to state {@code isDone}. Wrapped
             * in a try block we call our {@code postResultIfNotInvoked} method with the result of
             * the computation (waiting for it if necessary). If we catch InterruptedException we log
             * the error, if we catch ExecutionException we throw RuntimeException, and if we catch
             * CancellationException (our task was canceled) we call {@code postResultIfNotInvoked}
             * with null.
             */
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    /**
     * Convenience function to call {@code postResult} with our parameter {@code Result result} iff
     * our flag {@code wasTaskInvoked} is false.
     *
     * @param result result of background computation.
     */
    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    /**
     * Posts the {@code Result result} to the main thread. First we create {@code Message message}
     * with a {@code what} field of MESSAGE_POST_RESULT, and an {@code obj} field containing our
     * parameter {@code Result result} wrapped in a new instance of {@code AsyncTaskResult}. We then
     * send this Message to the {@code sHandler} whose {@code handleMessage} passes the result to
     * the {@code finish} method of this which calls the {@code onPostExecute} method with the
     * result (or the {@code onCancelled} method with result if the thread was canceled). We then
     * return {@code result} to the caller.
     *
     * @param result result of background computation
     * @return our parameter {@code result}
     */
    private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                new AsyncTaskResult<>(this, result));
        message.sendToTarget();
        return result;
    }

    /**
     * Returns the current status of this task. We just return the value in our field {@code mStatus}
     *
     * @return The current status.
     */
    @SuppressWarnings("unused")
    public final Status getStatus() {
        return mStatus;
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     *
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @SuppressWarnings("unchecked")
    protected abstract Result doInBackground(Params... params);

    /**
     * Runs on the UI thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    protected void onPreExecute() {
    }

    /**
     * Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.
     * <p>
     * This method won't be invoked if the task was cancelled.
     *
     * @param result The result of the operation computed by {@link #doInBackground}.
     *
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onPostExecute(Result result) {
    }

    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     *
     * @param values The values indicating progress.
     *
     * @see #publishProgress
     * @see #doInBackground
     */
    @SuppressWarnings({"UnusedDeclaration", "unchecked"})
    protected void onProgressUpdate(Progress... values) {
    }

    /**
     * Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.
     * <p>
     * The default implementation simply invokes {@link #onCancelled()} and
     * ignores the result. If you write your own implementation, do not call
     * <code>super.onCancelled(result)</code>.
     * <p>
     * We just call our zero parameter {@code onCancelled} method.
     *
     * @param result The result, if any, computed in
     *               {@link #doInBackground(Object[])}, can be null
     *
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @SuppressWarnings({"UnusedParameters"})
    protected void onCancelled(Result result) {
        onCancelled();
    }

    /**
     * Applications should preferably override {@link #onCancelled(Object)}.
     * This method is invoked by the default implementation of
     * {@link #onCancelled(Object)}.
     * <p>
     * Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.
     *
     * @see #onCancelled(Object)
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    protected void onCancelled() {
    }

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed
     * normally. If you are calling {@link #cancel(boolean)} on the task,
     * the value returned by this method should be checked periodically from
     * {@link #doInBackground(Object[])} to end the task as soon as possible.
     * <p>
     * We just return the value of our field {@code AtomicBoolean mCancelled}.
     *
     * @return <tt>true</tt> if task was cancelled before it completed
     *
     * @see #cancel(boolean)
     */
    public final boolean isCancelled() {
        return mCancelled.get();
    }

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run. If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     * <p>
     * Calling this method will result in {@link #onCancelled(Object)} being
     * invoked on the UI thread after {@link #doInBackground(Object[])}
     * returns. Calling this method guarantees that {@link #onPostExecute(Object)}
     * is never invoked. After invoking this method, you should check the
     * value returned by {@link #isCancelled()} periodically from
     * {@link #doInBackground(Object[])} to finish the task as early as
     * possible.
     * <p>
     * We set our field {@code AtomicBoolean mCancelled} to true, then return the value returned
     * by the {@code cancel} method of {@code FutureTask<Result> mFuture} when called with our
     * parameter {@code mayInterruptIfRunning}.
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     *        task should be interrupted; otherwise, in-progress tasks are allowed
     *        to complete.
     *
     * @return <tt>false</tt> if the task could not be cancelled,
     *         typically because it has already completed normally;
     *         <tt>true</tt> otherwise
     *
     * @see #isCancelled()
     * @see #onCancelled(Object)
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result. We return the value returned by the {@code get}
     * method of {@code FutureTask<Result> mFuture} (which waits if necessary
     * for the computation to complete, and then retrieves its result).
     *
     * @return The computed result.
     *
     * @throws java.util.concurrent.CancellationException If the computation was cancelled.
     * @throws java.util.concurrent.ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     */
    @SuppressWarnings("unused")
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result. We return the value
     * returned by the {@code get} method of {@code FutureTask<Result> mFuture}
     * (which waits if necessary for at most {@code timeout} milliseconds
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
     *         while waiting.
     * @throws java.util.concurrent.TimeoutException If the wait timed out.
     */
    @SuppressWarnings("unused")
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     * <p>
     * Note: this function schedules the task on a queue for a single background
     * thread or pool of threads depending on the platform version.  When first
     * introduced, AsyncTasks were executed serially on a single background thread.
     * Starting with {@link android.os.Build.VERSION_CODES#DONUT}, this was changed
     * to a pool of threads allowing multiple tasks to operate in parallel. Starting
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, tasks are back to being
     * executed on a single thread to avoid common application errors caused
     * by parallel execution.  If you truly want parallel execution, you can use
     * the {@link #executeOnExecutor} version of this method
     * with {@link #THREAD_POOL_EXECUTOR}; however, see commentary there for warnings
     * on its use.
     * <p>
     * This method must be invoked on the UI thread.
     * <p>
     * We just call the {@code executeOnExecutor} method using {@code sDefaultExecutor}
     * as the executor and our parameter {@code params} as the parameters to pass.
     *
     * @param params The parameters of the task.
     *
     * @return This instance of AsyncTask.
     *
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *         {@link AsyncTask.Status#RUNNING} or {@link AsyncTask.Status#FINISHED}.
     *
     * @see #executeOnExecutor(java.util.concurrent.Executor, Object[])
     * @see #execute(Runnable)
     */
    @SuppressWarnings("unchecked")
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     * <p>
     * This method is typically used with {@link #THREAD_POOL_EXECUTOR} to
     * allow multiple tasks to run in parallel on a pool of threads managed by
     * AsyncTask, however you can also use your own {@link java.util.concurrent.Executor} for custom
     * behavior.
     * <p>
     * <em>Warning:</em> Allowing multiple tasks to run in parallel from
     * a thread pool is generally <em>not</em> what one wants, because the order
     * of their operation is not defined.  For example, if these tasks are used
     * to modify any state in common (such as writing a file due to a button click),
     * there are no guarantees on the order of the modifications.
     * Without careful work it is possible in rare cases for the newer version
     * of the data to be over-written by an older one, leading to obscure data
     * loss and stability issues.  Such changes are best
     * executed in serial; to guarantee such work is serialized regardless of
     * platform version you can use this function with {@link #SERIAL_EXECUTOR}.
     * <p>
     * This method must be invoked on the UI thread.
     * <p>
     * First we make sure our status {@code mStatus} is PENDING, and if not we throw
     * IllegalStateException for RUNNING, or FINISHED status. If it is PENDING we set
     * it to RUNNING, call {@code onPreExecute}, store our parameter {@code params} in
     * the {@code mParams} field of {@code WorkerRunnable<Params, Result> mWorker} then
     * call the {@code execute} method of our parameter {@code exec} to execute
     * {@code FutureTask<Result> mFuture}.
     *
     * @param exec The executor to use.  {@link #THREAD_POOL_EXECUTOR} is available as a
     *              convenient process-wide thread pool for tasks that are loosely coupled.
     * @param params The parameters of the task.
     *
     * @return This instance of AsyncTask.
     *
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *         {@link AsyncTask.Status#RUNNING} or {@link AsyncTask.Status#FINISHED}.
     *
     * @see #execute(Object[])
     */
    @SuppressWarnings("unchecked")
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
                                                                       Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        exec.execute(mFuture);

        return this;
    }

    /**
     * Convenience version of {@link #execute(Object...)} for use with
     * a simple Runnable object. See {@link #execute(Object[])} for more
     * information on the order of execution.
     * <p>
     * We just call the {@code execute} method of our field {@code Executor sDefaultExecutor} to
     * execute our parameter {@code Runnable runnable}.
     *
     * @param runnable {@code Runnable} to execute
     * @see #execute(Object[])
     * @see #executeOnExecutor(java.util.concurrent.Executor, Object[])
     */
    @SuppressWarnings("unused")
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    /**
     * This method can be invoked from {@link #doInBackground} to
     * publish updates on the UI thread while the background computation is
     * still running. Each call to this method will trigger the execution of
     * {@link #onProgressUpdate} on the UI thread.
     * <p>
     * {@link #onProgressUpdate} will not be called if the task has been
     * canceled.
     * <p>
     * If this task has not been canceled, we create a message using our UI handler {@code sHandler}
     * whose {@code what} field is MESSAGE_POST_PROGRESS, and whose {@code obj} field is an
     * {@code AsyncTaskResult} with this as its task, and our parameter {@code values} as its data,
     * then send the message to the target {@code sHandler} (whose {@code handleMessage} method will
     * call the {@code onProgressUpdate} method of the {@code mTask} field of the {@code obj} field
     * (this) with our parameter {@code values}.
     *
     * @param values The progress values to update the UI with.
     *
     * @see #onProgressUpdate
     * @see #doInBackground
     */
    @SuppressWarnings({"unchecked", "unused"})
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                    new AsyncTaskResult<>(this, values)).sendToTarget();
        }
    }

    /**
     * Called by the {@code handleMessage} method of our UI handler in response to a MESSAGE_POST_RESULT
     * message. If our task had been cancelled we call our {@code onCancelled} method with our parameter
     * {@code result}, otherwise we call the {@code onPostExecute} with our parameter {@code result}.
     * In either case we set our status {@code mStatus} to FINISHED.
     *
     * @param result result returned by the background thread
     */
    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }

    /**
     * {@code Handler} that runs on the UI thread.
     */
    private static class InternalHandler extends Handler {
        /**
         * Called when we have a message to receive. We cast the {@code obj} field of our parameter
         * {@code msg} to initialize {@code AsyncTaskResult result}, then we switch on the {@code what}
         * field of {@code msg}:
         * <ul>
         *     <li>
         *         MESSAGE_POST_RESULT: we call the {@code finish} method of the {@code mTask} field
         *         of {@code result} with the value {@code result.mData[0]}, then break.
         *     </li>
         *     <li>
         *         MESSAGE_POST_PROGRESS: we call the {@code onProgressUpdate} method of the
         *         {@code mTask} field of {@code result} with the value {@code result.mData},
         *         then break.
         *     </li>
         * </ul>
         *
         * @param msg Message from the global message pool we are to handle
         */
        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult result = (AsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }

    /**
     * Class that adds a field {@code Params[] mParams} to the {@code Callable<Result>} class.
     *
     * @param <Params> type of our parameters field
     * @param <Result> type of our return value
     */
    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    /**
     * Simple holding class intended to hold the data returned from an {@code AsyncTask}
     *
     * @param <Data> type of data we are holding.
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class AsyncTaskResult<Data> {
        /**
         * {@code AsyncTask} we belong to, used in the {@code handleMessage} method of {@code InternalHandler}
         * to route the data to the correct task.
         */
        final AsyncTask mTask;
        /**
         * Data returned by the {@code AsyncTask}
         */
        final Data[] mData;

        /**
         * Our constructor, simply saves our parameters in our fields.
         *
         * @param task {@code AsyncTask} we belong to
         * @param data Data returned by the {@code AsyncTask}
         */
        @SuppressWarnings("unchecked")
        AsyncTaskResult(AsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}