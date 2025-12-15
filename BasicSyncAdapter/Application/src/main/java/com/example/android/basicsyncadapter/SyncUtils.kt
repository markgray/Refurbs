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
@file:Suppress("DEPRECATION", "FunctionName")

package com.example.android.basicsyncadapter

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import com.example.android.basicsyncadapter.provider.FeedContract
import com.example.android.common.accounts.GenericAccountService
import androidx.core.content.edit

/**
 * Static helper methods for working with the sync framework.
 */
object SyncUtils {
    /**
     * how frequently the sync should be performed, in seconds: 1 hour (in seconds)
     */
    private const val SYNC_FREQUENCY = (60 * 60).toLong()

    /**
     * Content provider authority ("com.example.android.basicsyncadapter")
     */
    private const val CONTENT_AUTHORITY = FeedContract.CONTENT_AUTHORITY

    /**
     * Shared preference key for flag indicating that we have run before, and do not need to preform
     * an initial sync.
     */
    private const val PREF_SETUP_COMPLETE = "setup_complete"

    /**
     * AccountType defined in the configuration XML file for android.accounts.AccountAuthenticator
     * (e.g. res/xml/syncadapter.xml).
     * Value below must match the account type specified in res/xml/syncadapter.xml
     */
    const val ACCOUNT_TYPE: String = "com.example.android.basicsyncadapter.account"

    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     * First we initialize [Boolean] variable `var newAccount` to false, and initialize [Boolean]
     * variable `val setupComplete` with the shared preference [Boolean] stored under the key
     * [PREF_SETUP_COMPLETE] ("setup_complete"), defaulting to `false` if it does not exist yet.
     * We initialize [Account] variable `val account` with the value returned by the
     * [GenericAccountService.GetAccount] method of our class [GenericAccountService] for our
     * account type [ACCOUNT_TYPE] (`GetAccount` creates it if it is the first run, or the user
     * has deleted the account). We initialize [AccountManager] variable `val accountManager` with
     * a handle to the system level service [Context.ACCOUNT_SERVICE]. If the `addAccountExplicitly`
     * method of `accountManager` succeeds in adding `account` (it returns true) the account did not
     * previously exist so we:
     *
     *  * Inform the system that this account supports sync
     *  * Inform the system that this account is eligible for auto sync when the network is up
     *  * Recommend a schedule for automatic synchronization.
     *  * Set our flag `newAccount` to true
     *
     * Finally if this is a `newAccount` or `setupComplete` is false we call our method
     * [TriggerRefresh] to trigger an immediate sync and store `true` in our shared preferences
     * under the key [PREF_SETUP_COMPLETE].
     *
     * @param context [Context] we are running in.
     */
    @SuppressLint("ApplySharedPref")
    fun CreateSyncAccount(context: Context) {
        var newAccount = false
        val setupComplete: Boolean = PreferenceManager
            .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false)

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        val account: Account = GenericAccountService.GetAccount(ACCOUNT_TYPE)
        val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1)
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true)
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(
                account, CONTENT_AUTHORITY, Bundle(), SYNC_FREQUENCY)
            newAccount = true
        }

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            TriggerRefresh()
            PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
                putBoolean(PREF_SETUP_COMPLETE, true)
            }
        }
    }

    /**
     * Helper method to trigger an immediate sync ("refresh"). This should only be used when we need
     * to preempt the normal sync schedule. Typically, this means the user has pressed the "refresh"
     * button.
     *
     * Note that [ContentResolver.SYNC_EXTRAS_MANUAL] will cause an immediate sync, without any
     * optimization to preserve battery life. If you know new data is available (perhaps via a GCM
     * notification), but the user is not actively waiting for that data, you should omit this flag;
     * this will give the OS additional freedom in scheduling your sync request.
     *
     * First we initialize [Bundle] variable `val b` with a new instance, then we store `true` in it
     * under the key [ContentResolver.SYNC_EXTRAS_MANUAL] ("force") (Setting this extra is the
     * equivalent of setting both [ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS] and
     * [ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF]), and store `true` in it under the key
     * [ContentResolver.SYNC_EXTRAS_EXPEDITED] ("expedited") (If this extra is set to `true`, the
     * sync request will be scheduled at the front of the sync request queue and without any delay).
     * Finally we use the [ContentResolver.requestSync] method to start an asynchronous sync
     * operation for the sync account [ACCOUNT_TYPE], authority [FeedContract.CONTENT_AUTHORITY],
     * and with `b` as the as the extras to pass to the [SyncAdapter].
     */
    fun TriggerRefresh() {
        val b = Bundle()
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        ContentResolver.requestSync(
            /* account = */ GenericAccountService.GetAccount(ACCOUNT_TYPE),  // Sync account
            /* authority = */ FeedContract.CONTENT_AUTHORITY,  // Content authority
            /* extras = */ b  // Extras
        )
    }
}