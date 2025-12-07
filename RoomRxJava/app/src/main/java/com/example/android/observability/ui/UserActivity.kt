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
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.observability.Injection
import com.example.android.observability.R
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

/**
 * Main screen of the app. Displays a user name and gives the option to update the user name.
 */
class UserActivity : AppCompatActivity() {
    /**
     * [TextView] with id `R.id.user_name` in our layout file layout/activity_user.xml which is
     * used to display the current user name.
     */
    private var mUserName: TextView? = null

    /**
     * [EditText] with id `R.id.user_name_input` in our layout file layout/activity_user.xml
     * which is used to enter a new user name
     */
    private var mUserNameInput: EditText? = null

    /**
     * [Button] with id `R.id.update_user` in our layout file layout/activity_user.xml which is
     * used to read the text in [EditText] field [mUserNameInput] and use it to update the database.
     */
    private var mUpdateButton: Button? = null

    /**
     * [ViewModelFactory] instance we use to create [UserViewModel] field [mViewModel].
     */
    private var mViewModelFactory: ViewModelFactory? = null

    /**
     * This [UserViewModel] is used to update our UI based on the contents of the database, and to
     * update the database when the user changes the user name.
     */
    private var mViewModel: UserViewModel? = null

    /**
     * [CompositeDisposable] which is used to observe the [Flowable] of [String] returned by
     * the [UserViewModel.userName] property of our [UserViewModel] field [mViewModel].
     */
    private val mDisposable = CompositeDisposable()

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.activity_user`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID
     * `R.id.root_view` then call [ViewCompat.setOnApplyWindowInsetsListener] to
     * take over the policy for applying window insets to `rootView`, with the
     * `listener` argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [TextView] field [mUserName] by finding the view with id `R.id.user_name`,
     * [EditText] field [mUserNameInput] by finding the view with id `R.id.user_name_input`, and our
     * [Button] field [mUpdateButton] by finding the view with id `R.id.update_user`. We initialize
     * our [ViewModelFactory] field [mViewModelFactory] to the [ViewModelFactory] returned by the
     * [Injection.provideViewModelFactory] method, and use that to initialize [UserViewModel] field
     * [mViewModel] using the [ViewModelProvider] created from it to get a [ViewModel] of the class
     * [UserViewModel]. Finally we set the [View.OnClickListener] of [mUpdateButton] to a lambda
     * which calls our method [updateUserName] to update the user name when clicked.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        mUserName = findViewById(R.id.user_name)
        mUserNameInput = findViewById(R.id.user_name_input)
        mUpdateButton = findViewById(R.id.update_user)
        mViewModelFactory = Injection.provideViewModelFactory(this)
        mViewModel = ViewModelProvider(this, mViewModelFactory!!)[UserViewModel::class.java]
        mUpdateButton!!.setOnClickListener { _: View? -> updateUserName() }
    }

    /**
     * Called after [onCreate]  or after [onRestart]. First we call our super's implementation of
     * `onStart`. Then we add a [Disposable] resource to our [CompositeDisposable] field [mDisposable]
     * using the [Flowable] of [String] returned by the [UserViewModel.userName] propetry of
     * [UserViewModel] field [mViewModel] as the emitter which we subscribe using the [Scheduler]
     * instance intended for IO-bound work, and observe using the scheduler running on the main
     * thread to which we subscribe two lambdas whose `onNext` Consumer consumes the string
     * `userName` by setting the text of [TextView] field [mUserName] to it and whose `onError`
     * Consumer consumes the [Throwable] `throwable` passed it by logging it.
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
            ) { throwable: Throwable? -> Log.e(TAG, "Unable to update username", throwable) }
        )
    }

    /**
     * Called when you are no longer visible to the user. First we call our super's implementation
     * of `onStop`, then we call the [CompositeDisposable.clear] method of [CompositeDisposable]
     * field [mDisposable] to Atomically clear the container, and dispose of all the previously
     * contained Disposables. (clears all the subscriptions).
     */
    override fun onStop() {
        super.onStop()

        // clear all the subscriptions
        mDisposable.clear()
    }

    /**
     * [View.OnClickListener] for the [Button] field [mUpdateButton], we update the database entry
     * for the user name with the value the user has entered in [EditText] field [mUserNameInput].
     * First we retrieve the value the user has entered in [EditText] field [mUserNameInput] to
     * initialize [String] variable `val userName`, and we disable [mUpdateButton]. Then we add a
     * [Disposable] resource to our [CompositeDisposable] field [mDisposable] using the
     * [Completable] returned when we call the [UserViewModel.updateUserName] method of
     * [UserViewModel] field [mViewModel] as the emitter which we subscribe to using the [Scheduler]
     * instance intended for IO-bound work, and observe using the scheduler running on the main
     * thread to which we subscribe two lambdas whose `onNext` [Consumer] consumes the completion by
     * enabling [Button] field [mUpdateButton], and whose `onError` [Consumer] consumes the
     * [Throwable] `throwable` passed it by logging it.
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
