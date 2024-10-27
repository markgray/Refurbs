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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.common.accounts

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log

/**
 * This is declared as a "android.accounts.AccountAuthenticator" server in AndroidManifest and is
 * described by the xml file xml/authenticator.xml, which defined the android:accountType to be
 * "com.example.android.basicsyncadapter.account"
 */
class GenericAccountService : Service() {
    /**
     * Our instance of [Authenticator] (an [AbstractAccountAuthenticator] implementation)
     */
    private var mAuthenticator: Authenticator? = null

    /**
     * Called by the system when the service is first created. After logging "Service created", we
     * initialize our [Authenticator] field [mAuthenticator] with a new instance.
     */
    override fun onCreate() {
        Log.i(TAG, "Service created")
        mAuthenticator = Authenticator(this)
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * We merely log this.
     */
    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
    }

    /**
     * Return the communication channel to the service.  May return `null` if clients can not bind to
     * the service. We return the [IBinder] returned by the [Authenticator.getIBinder] method of our
     * [mAuthenticator] field.
     *
     * @param intent The [Intent] that was used to bind to this service.
     * @return Return an IBinder through which clients can call on to the service.
     */
    override fun onBind(intent: Intent): IBinder? {
        return mAuthenticator!!.iBinder
    }

    /**
     * Our bare minimum implementation of the [AbstractAccountAuthenticator] interface
     */
    class Authenticator
    /**
     * Our constructor, we just call our super's constructor
     *
     * @param context [Context] our super uses to check for permissions: "this" in the
     * `onCreate` override of [GenericAccountService]
     */
    (context: Context?) : AbstractAccountAuthenticator(context) {
        /**
         * Returns a Bundle that contains the [Intent] of the activity that can be used to edit the
         * properties. In order to indicate success the activity should call the method
         * [AccountAuthenticatorResponse.onResult] with a non-`null` [Bundle].
         *
         * We just throw [UnsupportedOperationException]
         *
         * @param response used to set the result for the request. If the `Constants.INTENT_KEY`
         * is set in the bundle then this response field is to be used for sending future results
         * if and when the [Intent] is started.
         * @param accountType the "AccountType" whose properties are to be edited.
         * @return a [Bundle] containing the result or the [Intent] to start to continue the request.
         * If this is `null` then the request is considered to still be active and the result should
         * sent later using response.
         */
        override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle {
            throw UnsupportedOperationException()
        }

        /**
         * Adds an account of the specified accountType. We return `null`.
         *
         * @param response         to send the result back to the AccountManager, will never be null
         * @param accountType      the type of account to add, will never be null
         * @param authTokenType    the type of auth token to retrieve after adding the account, may be null
         * @param requiredFeatures a String array of authenticator-specific features that the added
         * account must support, may be null
         * @param options          a Bundle of authenticator-specific options. It always contains
         * `AccountManager.KEY_CALLER_PID` and `AccountManager.KEY_CALLER_UID`
         * fields which will let authenticator know the identity of the caller.
         * @return a Bundle result or null if the result is to be returned via the response. The result
         * will contain either:
         *
         *  *  [AccountManager.KEY_INTENT], or
         *  *  [AccountManager.KEY_ACCOUNT_NAME] and [AccountManager.KEY_ACCOUNT_TYPE] of
         *  the account that was added, or
         *  *  [AccountManager.KEY_ERROR_CODE] and [AccountManager.KEY_ERROR_MESSAGE] to
         *  indicate an error
         *
         * @throws NetworkErrorException if the authenticator could not honor the request due to a
         * network error
         */
        @Throws(NetworkErrorException::class)
        override fun addAccount(
            response: AccountAuthenticatorResponse,
            accountType: String,
            authTokenType: String,
            requiredFeatures: Array<String>,
            options: Bundle
        ): Bundle? {
            return null
        }

        /**
         * Checks that the user knows the credentials of an account. We return `null`.
         *
         * @param response to send the result back to the [AccountManager], will never be `null`
         * @param account  the account whose credentials are to be checked, will never be `null`
         * @param options  a [Bundle] of authenticator-specific options, may be `null`
         * @return a [Bundle] result or `null` if the result is to be returned via the response.
         * The result will contain either:
         *
         *  *  [AccountManager.KEY_INTENT], or
         *  *  [AccountManager.KEY_BOOLEAN_RESULT], `true` if the check succeeded, `false` otherwise
         *  *  [AccountManager.KEY_ERROR_CODE] and [AccountManager.KEY_ERROR_MESSAGE] to indicate
         *  an error
         *
         * @throws NetworkErrorException if the authenticator could not honor the request due to a
         * network error
         */
        @Throws(NetworkErrorException::class)
        override fun confirmCredentials(
            response: AccountAuthenticatorResponse,
            account: Account,
            options: Bundle
        ): Bundle? {
            return null
        }

        /**
         * Gets an auth token for an account. We just throw [UnsupportedOperationException], but...
         *
         * If not `null`, the resultant [Bundle] will contain different sets of keys
         * depending on whether a token was successfully issued and, if not, whether one
         * could be issued via some [android.app.Activity].
         *
         * If a token cannot be provided without some additional activity, the [Bundle] should
         * contain [AccountManager.KEY_INTENT] with an associated [Intent]. On the other hand, if
         * there is no such activity, then a [Bundle] containing [AccountManager.KEY_ERROR_CODE]
         *  and [AccountManager.KEY_ERROR_MESSAGE] should be returned.
         *
         * If a token can be successfully issued, the implementation should return the
         * [AccountManager.KEY_ACCOUNT_NAME] and [AccountManager.KEY_ACCOUNT_TYPE] of the
         * account associated with the token as well as the [AccountManager.KEY_AUTHTOKEN]. In
         * addition [AbstractAccountAuthenticator] implementations that declare themselves
         * `android:customTokens=true` may also provide a non-negative `KEY_CUSTOM_TOKEN_EXPIRY`
         * [Long] value containing the expiration timestamp of the expiration time (in millis since
         * the unix epoch), tokens will be cached in memory based on application's
         * packageName/signature for however long that was specified.
         *
         * Implementers should assume that tokens will be cached on the basis of `account` and
         * `authTokenType`. The system may ignore the contents of the supplied options [Bundle] when
         * determining to re-use a cached token. Furthermore, implementers should assume a supplied
         * expiration time will be treated as non-binding advice.
         *
         * Finally, note that for `android:customTokens=false` authenticators, tokens are cached
         * indefinitely until some client calls [AccountManager.invalidateAuthToken].
         *
         * @param response to send the result back to the [AccountManager], will never be `null`
         * @param account  the account whose credentials are to be retrieved, will never be `null`
         * @param authTokenType the type of auth token to retrieve, will never be `null`
         * @param options a [Bundle] of authenticator-specific options. It always contains
         * [AccountManager.KEY_CALLER_PID] and [AccountManager.KEY_CALLER_UID] fields which will
         * let the authenticator know the identity of the caller.
         * @return a [Bundle] result or `null` if the result is to be returned via the response.
         * @throws NetworkErrorException if the authenticator could not honor the request due to a
         * network error
         */
        @Throws(NetworkErrorException::class)
        override fun getAuthToken(
            response: AccountAuthenticatorResponse,
            account: Account,
            authTokenType: String,
            options: Bundle
        ): Bundle {
            throw UnsupportedOperationException()
        }

        /**
         * Ask the authenticator for a localized label for the given [authTokenType]. We just throw
         * [UnsupportedOperationException].
         *
         * @param authTokenType the [authTokenType] whose label is to be returned, will never be `null`
         * @return the localized label of the auth token type, may be `null` if the type isn't known
         */
        override fun getAuthTokenLabel(authTokenType: String): String {
            throw UnsupportedOperationException()
        }

        /**
         * Update the locally stored credentials for an account. We just throw
         * [UnsupportedOperationException]
         *
         * @param response to send the result back to the [AccountManager], will never be `null`
         * @param account the account whose credentials are to be updated, will never be `null`
         * @param authTokenType the type of auth token to retrieve after updating the credentials,
         * may be `null`
         * @param options a [Bundle] of authenticator-specific options, may be `null`
         * @return a [Bundle] result or `null` if the result is to be returned via the response.
         * The result will contain either:
         *
         *  *  [AccountManager.KEY_INTENT], or
         *  *  [AccountManager.KEY_ACCOUNT_NAME] and [AccountManager.KEY_ACCOUNT_TYPE] of
         *  the account whose credentials were updated, or
         *  *  [AccountManager.KEY_ERROR_CODE] and [AccountManager.KEY_ERROR_MESSAGE] to
         *  indicate an error
         *
         * @throws NetworkErrorException if the authenticator could not honor the request due to a
         * network error
         */
        @Throws(NetworkErrorException::class)
        override fun updateCredentials(
            response: AccountAuthenticatorResponse,
            account: Account,
            authTokenType: String,
            options: Bundle
        ): Bundle {
            throw UnsupportedOperationException()
        }

        /**
         * Checks if the account supports all the specified authenticator specific features. We just
         * throw [UnsupportedOperationException].
         *
         * @param response to send the result back to the [AccountManager], will never be `null`
         * @param account the account to check, will never be `null`
         * @param features an array of features to check, will never be `null`
         * @return a [Bundle] result or `null` if the result is to be returned via the response.
         * The result will contain either:
         *
         *  *  [AccountManager.KEY_INTENT], or
         *  *  [AccountManager.KEY_BOOLEAN_RESULT], `true` if the account has all the features,
         *  `false` otherwise
         *  *  [AccountManager.KEY_ERROR_CODE] and [AccountManager.KEY_ERROR_MESSAGE] to
         *  indicate an error
         *
         * @throws NetworkErrorException if the authenticator could not honor the request due to a
         * network error
         */
        @Throws(NetworkErrorException::class)
        override fun hasFeatures(
            response: AccountAuthenticatorResponse,
            account: Account,
            features: Array<String>
        ): Bundle {
            throw UnsupportedOperationException()
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "GenericAccountService"

        /**
         * Account name we are using (Normally user's identity, but we are not using user accounts
         * so any string will do).
         */
        const val ACCOUNT_NAME: String = "Account"

        /**
         * Obtain a handle to the [android.accounts.Account] used for sync in this application.
         *
         * It is important that the [accountType] specified here matches the value in your sync
         * adapter configuration XML file for android.accounts.AccountAuthenticator (often saved in
         * res/xml/syncadapter.xml). If this is not set correctly, you'll receive an error
         * indicating that "caller uid XXXXX is different than the authenticator's uid".
         *
         * @param accountType `AccountType` defined in the configuration XML file for
         * android.accounts.AccountAuthenticator (e.g. res/xml/syncadapter.xml).
         * @return Handle to application's [Account] (not guaranteed to resolve unless
         * CreateSyncAccount() has been called)
         */
        @Suppress("FunctionName")
        fun GetAccount(accountType: String?): Account {
            // Note: Normally the account name is set to the user's identity (username or email
            // address). However, since we aren't actually using any user accounts, it makes more sense
            // to use a generic string in this case.
            //
            // This string should *not* be localized. If the user switches locale, we would not be
            // able to locate the old account, and may erroneously register multiple accounts.
            val accountName = ACCOUNT_NAME
            return Account(accountName, accountType!!)
        }
    }
}