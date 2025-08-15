/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.example.android.storageprovider

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log
import androidx.core.content.edit

/**
 * Toggles the user's login status via a login menu option, and enables/disables the cloud storage
 * content provider.
 */
class StorageProviderFragment : Fragment() {
    /**
     * Flag to indicate whether the user is "logged in" to our document provider or not
     */
    private var mLoggedIn: Boolean = false

    /**
     * Called when the fragment is starting. First we call through to our super's implementation of
     * `onCreate` then we set our [Boolean] flag [mLoggedIn] to the value that our method [readLoginValue]
     * reads from our shared preferences file. Finally we call the method [setHasOptionsMenu] to
     * report that this fragment would like to participate in populating the options menu by receiving
     * a call to [onCreateOptionsMenu] and related methods.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLoggedIn = readLoginValue()
        @Suppress("DEPRECATION") // TODO: Use MenuProvider
        setHasOptionsMenu(true)
    }

    /**
     * Prepare the Fragment host's standard options menu to be displayed. First we call our super's
     * implementation of `onPrepareOptionsMenu`. Then we locate the [MenuItem] variable `val item`
     * in our [Menu] parameter [menu] (already inflated by [MainActivity]) with the id
     * `R.id.sample_action`, and if our [Boolean] flag field [mLoggedIn] is `true` we set its title
     * to the string with id `R.string.log_out` ("Log out"), otherwise we set it to the string with
     * id `R.string.log_in` ("Log in").
     *
     * @param menu The options menu as last shown or first initialized by [onCreateOptionsMenu].
     */
    @Deprecated("Deprecated in Java") // TODO: Use MenuProvider
    override fun onPrepareOptionsMenu(menu: Menu) {
        @Suppress("DEPRECATION") // TODO: Use MenuProvider
        super.onPrepareOptionsMenu(menu)
        val item: MenuItem = menu.findItem(R.id.sample_action)
        item.setTitle(if (mLoggedIn) R.string.log_out else R.string.log_in)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the id of our
     * [MenuItem] parameter [item] is `R.id.sample_action`, we call our method [toggleLogin] to
     * toggle whether we are logged in or not, and if our [Boolean] flag field [mLoggedIn] is now
     * `true` we set the title of [item] to the string with id `R.string.log_out` ("Log out")
     * otherwise we set it to thestring with id `R.string.log_in` ("Log in"). We then retrieve a
     * [ContentResolver] instance for our application's package and call its [ContentResolver.notifyChange]
     * method to notify registered observers that a row was updated in the URI representing the
     * roots of our document provider [AUTHORITY] ("com.example.android.storageprovider.documents").
     * In any case we return `true` to our caller to consume the event here.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    @Deprecated("Deprecated in Java") // TODO: Use MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            toggleLogin()
            item.setTitle(if (mLoggedIn) R.string.log_out else R.string.log_in)

            // BEGIN_INCLUDE(notify_change)
            // Notify the system that the status of our roots has changed.  This will trigger
            // a call to MyCloudProvider.queryRoots() and force a refresh of the system
            // picker UI.  It's important to call this or stale results may persist.
            @Suppress("DEPRECATION")
            requireActivity()
                .contentResolver
                .notifyChange(
                    DocumentsContract.buildRootsUri(AUTHORITY),
                    null,
                    false
                )
            // END_INCLUDE(notify_change)
        }
        return true
    }

    /**
     * Dummy function to change the user's authorization status. First we toggle the value of our
     * [Boolean] flag field [mLoggedIn], then we call our method [writeLoginValue] to write the new
     * value to our shared preferences. Finally we log the new authorization status.
     */
    private fun toggleLogin() {
        // Replace this with your standard method of authentication to determine if your app
        // should make the user's documents available.
        mLoggedIn = !mLoggedIn
        writeLoginValue(mLoggedIn)
        Log.i(
            TAG,
            getString(
                if (mLoggedIn) {
                    R.string.logged_in_info
                } else {
                    R.string.logged_out_info
                }
            )
        )
    }

    /**
     * Dummy function to save whether the user is logged in. We initialize [SharedPreferences]
     * variable `val sharedPreferences` with the preferences file with the name whose resource id
     * is `R.string.app_name` ("StorageProvider"). We then use `sharedPreferences` to create an
     * [SharedPreferences.Editor] which we use to store the value of our [Boolean] parameter
     * [loggedIn] under the key with resource id `R.string.key_logged_in` ("logged_in")
     * and then apply the change.
     */
    private fun writeLoginValue(loggedIn: Boolean) {
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences(
            /* name = */ getString(R.string.app_name),
            /* mode = */ Context.MODE_PRIVATE
        )
        sharedPreferences.edit { putBoolean(getString(R.string.key_logged_in), loggedIn) }
    }

    /**
     * Dummy function to determine whether the user is logged in. We initialize [SharedPreferences]
     * variable `val sharedPreferences` with the preferences file with the name whose resource id is
     * `R.string.app_name` ("StorageProvider"). We then use `sharedPreferences` to fetch the boolean
     * stored under the key with resource id `R.string.key_logged_in` ("logged_in") defaulting to
     * `false`, which we return to the caller.
     *
     * @return value stored in shared preferences file under the key "logged_in".
     */
    private fun readLoginValue(): Boolean {
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getBoolean(getString(R.string.key_logged_in), false)
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "StorageProviderFragment"

        /**
         * android:authorities attribute of our `MyCloudProvider` document provider.
         */
        private const val AUTHORITY = "com.example.android.storageprovider.documents"
    }
}
