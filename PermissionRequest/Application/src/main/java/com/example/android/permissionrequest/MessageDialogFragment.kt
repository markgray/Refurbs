/*
 * Copyright (C) 2016 The Android Open Source Project
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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.permissionrequest

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment

/**
 * Shows a dialog with a brief message.
 */
class MessageDialogFragment : DialogFragment() {
    /**
     * Override to build your own custom [Dialog] container. We use a new [AlertDialog.Builder]
     * to build and create the [Dialog] we return, setting its message set to the string with
     * the resource id which is stored in our arguments under the key [ARG_MESSAGE_RES_ID]
     * ("message_res_id"), setting it to not be cancelable, and setting its positive button to
     * display the string with resource id [android.R.string.ok] ("OK") and using an anonymous
     * class as its [DialogInterface.OnClickListener] which calls its parent fragment's
     * [Listener.onOkClicked] override when clicked.
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or `null` if this is a freshly created Fragment.
     * @return Return a new [Dialog] instance to be displayed by the Fragment.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context)
            .setMessage(requireArguments().getInt(ARG_MESSAGE_RES_ID))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                (parentFragment as Listener?)!!.onOkClicked()
            }
            .create()
    }

    /**
     * Implement this to respond when your [MessageDialogFragment] has its positive button clicked.
     */
    internal interface Listener {
        /**
         * This method will be called when the positive button is clicked.
         */
        fun onOkClicked()
    }

    companion object {
        /**
         * Key in our argument bundle that contains resource id of string we are to show.
         */
        private const val ARG_MESSAGE_RES_ID = "message_res_id"

        /**
         * Factory method that creates and configures a new instance. First we initialize our
         * [MessageDialogFragment] variable `val fragment`, and [Bundle] variable `val args` with
         * new instances. Then we add our parameter [message] to `args` under the key
         * [ARG_MESSAGE_RES_ID] ("message_res_id") and set the arguments of `fragment` to `args`.
         * Finally we return `fragment` to the caller.
         *
         * @param message resource id of string we are to show.
         * @return new instance of [MessageDialogFragment] whose argument is set to our parameter.
         */
        @JvmStatic
        fun newInstance(@StringRes message: Int): MessageDialogFragment {
            val fragment = MessageDialogFragment()
            val args = Bundle()
            args.putInt(ARG_MESSAGE_RES_ID, message)
            fragment.arguments = args
            return fragment
        }
    }
}