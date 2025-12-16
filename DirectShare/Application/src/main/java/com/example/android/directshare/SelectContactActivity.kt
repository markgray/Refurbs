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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "unused")

package com.example.android.directshare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView

/**
 * The dialog for selecting a contact to share the text with. This dialog is shown when the user
 * taps on this sample's icon rather than any of the Direct Share contacts.
 */
class SelectContactActivity : Activity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.select_contact`. We
     * initialize [Intent] variable `val intent` with the intent that started this activity and if
     * the action of the intent is not [ACTION_SELECT_CONTACT] we call [finish] to close this
     * activity and then we return. Otherwise we initialize [ListView] variable `val list` by
     * finding the view with id `R.id.list`, set its adapter to our [ListAdapter] field [mAdapter],
     * and set its [OnItemClickListener] to our [OnItemClickListener] field [mOnItemClickListener].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_contact)
        val intent: Intent = intent
        if (ACTION_SELECT_CONTACT != intent.action) {
            finish()
            return
        }
        // Set up the list of contacts
        val list = findViewById<ListView>(R.id.list)
        list.adapter = mAdapter
        list.onItemClickListener = mOnItemClickListener
    }

    /**
     * Adapter we use to populate our [ListView].
     */
    private val mAdapter: ListAdapter = object : BaseAdapter() {
        /**
         * How many items are in the data set represented by this Adapter. We just return the `size`
         * of the [Array] of [Contact] singleton object [Contact.CONTACTS].
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            return Contact.CONTACTS.size
        }

        /**
         * Get the data item associated with the specified position in the data set. We just return
         * the [Contact] returned by call of the method [Contact.byId] for our [Int] parameter [i].
         *
         * @param i Position of the item whose data we want within the adapter's data set.
         * @return The data at the specified position.
         */
        override fun getItem(i: Int): Any {
            return Contact.byId(id = i)
        }

        /**
         * Get the row id associated with the specified position in the list. The item id is the same
         * as the position, so we just return our [Int] parameter [i] converted to a [Long].
         *
         * @param i The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        /**
         * Get a [View] that displays the data at the specified position in the data set. We set our
         * [View] variable `var viewLocal` to our [View] parameter [view]. It it is `null`, we
         * initialize it by using the [LayoutInflater] from the context of our [ViewGroup] parameter
         * [parent] to inflate our layout file `R.layout.contact` (just contains a single [TextView]
         * whose id is "contact_name"). We initialize [TextView] variable `val textView` by casting
         * `viewLocal` to a `TextView`, initialize [Contact] variable `val contact` by using our
         * [getItem] method to fetch the item at [Int] parameter [i], then call the method
         * [ContactViewBinder.bind] to bind `contact` to `textView` (loads `textView` with the text
         * and icon from `contact`). Finally we return `textView` to the caller.
         *
         * @param i The position of the item within the adapter's data set whose view we want.
         * @param view The old view to reuse, if possible.
         * @param parent The parent that this view will eventually be attached to
         * @return A [View] corresponding to the data at the specified position.
         */
        override fun getView(i: Int, view: View?, parent: ViewGroup): View {
            var viewLocal: View? = view
            if (viewLocal == null) {
                viewLocal = LayoutInflater.from(parent.context)
                    .inflate(R.layout.contact, parent, false)
            }
            val textView = viewLocal as TextView
            val contact = getItem(i) as Contact
            ContactViewBinder.bind(contact, textView)
            return textView
        }
    }

    /**
     * The [OnItemClickListener] used for the items in our [ListView].
     *
     * Its [OnItemClickListener.onItemClick] callback method will be invoked when an item in this
     * [AdapterView] has been clicked. It initializes its [Intent] variable `val data` with a new
     * instance, then adds its [Intent] parameter `i` as an extra to it using the key [Contact.ID]
     * ("contact_id"). It sets our activities result to `data` with the result code
     * [Activity.RESULT_OK], (-1) and calls [finish] to close this activity.
     */
    private val mOnItemClickListener = OnItemClickListener {
        // The AdapterView where the click happened.
        adapterView: AdapterView<*>,
            // The view within the AdapterView that was clicked
        view: View,
            //The position of the view in the adapter.
        i: Int,
            // The row id of the item that was clicked.
        l: Long ->

        val data = Intent()
        data.putExtra(Contact.ID, i)
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        /**
         * The action string for Intents.
         */
        const val ACTION_SELECT_CONTACT: String = "com.example.android.directshare.intent.action.SELECT_CONTACT"
    }
}
