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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNUSED_ANONYMOUS_PARAMETER")

package com.example.android.observability.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.android.observability.Injection
import com.example.android.observability.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Main screen of the app. Displays a user name and gives the option to update the user name.
 */
class UserActivity : AppCompatActivity() {
    /**
     * `TextView` with id R.id.user_name in our layout file layout/activity_user.xml which is
     * used to display the current user name.
     */
    private var mUserName: TextView? = null

    /**
     * `EditText` with id R.id.user_name_input in our layout file layout/activity_user.xml
     * which is used to enter a new user name
     */
    private var mUserNameInput: EditText? = null

    /**
     * `Button` with id R.id.update_user in our layout file layout/activity_user.xml which is
     * used to read the text in `EditText mUserNameInput` and use it to update the database.
     */
    private var mUpdateButton: Button? = null

    /**
     * `ViewModelFactory` instance we use to create `UserViewModel mViewModel`
     */
    private var mViewModelFactory: ViewModelFactory? = null

    /**
     * This `ViewModel` is used to update our UI based on the contents of the database, and to
     * update the database when the user changes the user name.
     */
    private var mViewModel: UserViewModel? = null

    /**
     * `CompositeDisposable` which is used to observe the `Flowable<String>` returned by
     * the `getUserName` of our `UserViewModel mViewModel`.
     */
    private val mDisposable = CompositeDisposable()

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_user. We
     * initialize our field `TextView mUserName` by finding the view with id R.id.user_name,
     * `EditText mUserNameInput` by finding the view with id R.id.user_name_input, and our field
     * `Button mUpdateButton` by finding the view with id R.id.update_user. We initialize our
     * field `ViewModelFactory mViewModelFactory` by calling the method `provideViewModelFactory`
     * in `Injection`, and use that to initialize `UserViewModel mViewModel` using the
     * `ViewModelProvider` created from it to get a `ViewModel` of the class `UserViewModel`.
     * Finally we set the `View.OnClickListener` of `mUpdateButton` to a lambda which calls
     * our method `updateUserName` to update the user name when clicked.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        mUserName = findViewById(R.id.user_name)
        mUserNameInput = findViewById(R.id.user_name_input)
        mUpdateButton = findViewById(R.id.update_user)
        mViewModelFactory = Injection.provideViewModelFactory(this)
        mViewModel = ViewModelProvider(this, mViewModelFactory!!)[UserViewModel::class.java]
        mUpdateButton!!.setOnClickListener { v: View? -> updateUserName() }
    }

    /**
     * Called after [.onCreate]  or after [.onRestart]. First we call our super's
     * implementation of `onStart`. Then we add a `Disposable` resource to our field
     * `CompositeDisposable mDisposable` using the `Flowable<String>` returned by the
     * `getUserName` method of `UserViewModel mViewModel` as the emitter which we subscribe
     * using the `Scheduler` instance intended for IO-bound work, and observe using the
     * scheduler running on the main thread to which we subscribe two lambdas whose onNext Consumer
     * consumes the string `userName` by setting the text of `TextView mUserName` to it
     * and whose onError Consumer consumes the `Throwable throwable` by logging it.
     */
    override fun onStart() {
        super.onStart()
        // Subscribe to the emissions of the user name from the view model.
        // Update the user name text view, at every onNext emission.
        // In case of error, log the exception.
        mDisposable.add(mViewModel!!.userName
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ userName: String? -> mUserName!!.text = userName }
            ) { throwable: Throwable? -> Log.e(TAG, "Unable to update username", throwable) })
    }

    /**
     * Called when you are no longer visible to the user. First we call our super's implementation
     * of `onStop`, then we call the `clear` method of `CompositeDisposable mDisposable`
     * to Atomically clear the container, and dispose of all the previously contained Disposables.
     * (clears all the subscriptions).
     */
    override fun onStop() {
        super.onStop()

        // clear all the subscriptions
        mDisposable.clear()
    }

    /**
     * `OnClickListener` for the `Button mUpdateButton`, we update the database entry for
     * the user name with the value the user has entered in `EditText mUserNameInput`. First we
     * retrieve the value the user has entered in `EditText mUserNameInput` to initialize
     * `String userName`, then we disable `mUpdateButton`. Then we add a `Disposable`
     * resource to our field `CompositeDisposable mDisposable` using the `Completable`
     * returned when we call the `updateUserName` of `UserViewModel mViewModel` as the
     * emitter which we subscribe to using the `Scheduler` instance intended for IO-bound work,
     * and observe using the scheduler running on the main thread to which we subscribe two lambdas
     * whose onNext Consumer consumes the completion by enabling `Button mUpdateButton`, and
     * whose onError Consumer consumes the `Throwable throwable` by logging it.
     */
    private fun updateUserName() {
        val userName = mUserNameInput!!.text.toString()
        // Disable the update button until the user name update has been done
        mUpdateButton!!.isEnabled = false
        // Subscribe to updating the user name.
        // Re-enable the button once the user name has been updated
        mDisposable.add(mViewModel!!.updateUserName(userName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mUpdateButton!!.isEnabled = true }
            ) { throwable: Throwable? -> Log.e(TAG, "Unable to update username", throwable) })
    }

    companion object {
        /**
         * TAG used for logging
         */
        private val TAG = UserActivity::class.java.simpleName
    }
}