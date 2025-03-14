/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.directshare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast

/**
 * Provides the UI for sharing a text with a [Contact].
 */
class SendMessageActivity : Activity() {
    /**
     * The text to share.
     */
    private var mBody: String? = null

    /**
     * The ID of the contact to share the text with.
     */
    private var mContactId: Int = 0

    // View references.

    /**
     * [TextView] that contains the contact name that was selected.
     */
    private var mTextContactName: TextView? = null

    /**
     * [TextView] that contains the text that was shared.
     */
    private var mTextMessageBody: TextView? = null

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.send_message`, and
     * set the title associated with this activity to the string with resource id
     * `R.string.sending_message` ("Sending a message"). We initialize our [TextView] field
     * [mTextContactName] by finding the view with id `R.id.contact_name`, and [TextView] field
     * [mTextMessageBody] by finding the view with id `R.id.message_body`. We call our method
     * [resolveIntent] with the [Intent] that started this activity saving its [Boolean] return value
     * in [Boolean] variable `val resolved` (it returns `true` if the action of the intent is
     * [Intent.ACTION_SEND] and the type is "text/plain", `false` otherwise). If `resolve` is `false`
     * we call the [finish] method to close this activity and return. Otherwise we locate the button
     * with id `R.id.send` and set its [View.OnClickListener] to our field [mOnClickListener].
     * Then we call our [prepareUi] method to set up the UI. If the [Intent] that launched us did
     * not contain a contact id ([mContactId] is [Contact.INVALID_ID]) we call our [selectContact]
     * method to launch the [SelectContactActivity] activity in order for the user to select one.
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_message)
        setTitle(R.string.sending_message)

        // View references.
        mTextContactName = findViewById(R.id.contact_name)
        mTextMessageBody = findViewById(R.id.message_body)
        // Resolve the share Intent.
        val resolved = resolveIntent(intent)
        if (!resolved) {
            finish()
            return
        }
        // Bind event handlers.
        findViewById<View>(R.id.send).setOnClickListener(mOnClickListener)
        // Set up the UI.
        prepareUi()
        // The contact ID will not be passed on when the user clicks on the app icon rather than any
        // of the Direct Share icons. In this case, we show another dialog for selecting a contact.
        if (mContactId == Contact.INVALID_ID) {
            selectContact()
        }
    }

    /**
     * Called when an activity you launched exits, giving you the [requestCode] you started it with,
     * the [resultCode] it returned, and any additional data from it. We switch on the value of our
     * [Int] parameter [requestCode]:
     *
     *  * [REQUEST_SELECT_CONTACT]: If the result code [resultCode] is [Activity.RESULT_OK] we
     *  initialize our [Int] field [mContactId] with the value stored under the key [Contact.ID] in
     *  our [Intent] parameter [data], defaulting to [Contact.INVALID_ID]. If [mContactId] is equal
     *  to [Contact.INVALID_ID] we call the [finish] method to close this activity and return.
     *  Otherwise we call our [prepareUi] method to set up the UI.
     *
     *  * default: We call our super's implementation of `onActivityResult`.
     *
     * @param requestCode The [Int] request code originally supplied to [startActivityForResult],
     * allowing you to identify who this result came from.
     * @param resultCode The [Int] result code returned by the child activity through its [setResult]
     * method.
     * @param data An [Intent], which can return result data to the caller (various data can be
     * attached to [Intent] "extras").
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_SELECT_CONTACT -> {
                if (resultCode == RESULT_OK) {
                    mContactId = data.getIntExtra(Contact.ID, Contact.INVALID_ID)
                }
                // Give up sharing the send_message if the user didn't choose a contact.
                if (mContactId == Contact.INVALID_ID) {
                    finish()
                    return
                }
                prepareUi()
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Resolves the passed [Intent]. This method can only resolve intents for sharing a plain text.
     * [mBody] and [mContactId] are modified accordingly. If the action of our [Intent] parameter
     * [intent] is [Intent.ACTION_SEND] and its type is "text/plain" we initialize our [String]
     * field [mBody] with the string stored as an extra in [intent] under the key [Intent.EXTRA_TEXT],
     * and initialize our [Int] field [mContactId] with the [mContactId] stored under the key
     * [Contact.ID], defaulting to [Contact.INVALID_ID], then we return `true` to the caller.
     * Otherwise we return `false` to the caller.
     *
     * @param intent The [Intent].
     * @return `true` if the [Intent] parameter [intent] is resolved properly.
     */
    private fun resolveIntent(intent: Intent): Boolean {
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            mBody = intent.getStringExtra(Intent.EXTRA_TEXT)
            mContactId = intent.getIntExtra(Contact.ID, Contact.INVALID_ID)
            return true
        }
        return false
    }

    /**
     * Sets up the UI. If our [Int] field [mContactId] is not equal to [Contact.INVALID_ID], we
     * initialize our [Contact] variable `val contact` by fetching the contact with id [mContactId],
     * then bind the values in `contact` to the [TextView] field [mTextContactName]. In any case we
     * set the text of [TextView] field [mTextMessageBody] to [String] field [mBody].
     */
    private fun prepareUi() {
        if (mContactId != Contact.INVALID_ID) {
            val contact: Contact = Contact.byId(mContactId)
            ContactViewBinder.bind(contact, mTextContactName!!)
        }
        mTextMessageBody!!.text = mBody
    }

    /**
     * Delegates selection of a [Contact] to [SelectContactActivity]. We initialize our [Intent]
     * variable `val intent` with an [Intent] constructed to launch [SelectContactActivity], set its
     * action to [SelectContactActivity.ACTION_SELECT_CONTACT], and start that [Intent] for a result
     * using [REQUEST_SELECT_CONTACT] as the request code.
     */
    private fun selectContact() {
        val intent = Intent(this, SelectContactActivity::class.java)
        intent.action = SelectContactActivity.ACTION_SELECT_CONTACT
        startActivityForResult(intent, REQUEST_SELECT_CONTACT)
    }

    /**
     * [View.OnClickListener] for the "SEND" button in our ui, its [View.OnClickListener.onClick]
     * override just calls our [send] method.
     */
    private val mOnClickListener = View.OnClickListener { view: View ->
        /**
         * Called when the view is clicked, we switch on the id of our [View] parameter [view] so
         * as to handle only the "SEND" button in our ui, and call our method [send] if it is.
         *
         * @param view view that was clicked
         */
        when (view.id) {
            R.id.send -> send()
        }
    }

    /**
     * Pretends to send the text to the contact. This only shows a dummy message.
     */
    private fun send() {
        Toast.makeText(this,
            getString(R.string.message_sent, mBody, Contact.byId(mContactId).name),
            Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        /**
         * The request code for [SelectContactActivity]. This is used when the user doesn't select
         * any of Direct Share icons.
         */
        private const val REQUEST_SELECT_CONTACT = 1
    }
}
