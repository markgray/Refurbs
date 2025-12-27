/*
 * Copyright (C) 2014 The Android Open Source Project
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
@file:Suppress(
    "UNUSED_ANONYMOUS_PARAMETER",
    "ReplaceNotNullAssertionWithElvisReturn",
    "unused"
)

package com.example.android.permissionrequest

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment

/**
 * Prompts the user to confirm permission request.
 */
class ConfirmationDialogFragment : DialogFragment() {
    /**
     * Override to build your own custom Dialog container. We initialize [Array] of [String] variable
     * `val resources` by fetching from our arguments the string array stored under key [ARG_RESOURCES]
     * ("resources") then we return a new instance of [AlertDialog] we build by setting its message
     * to the formatted string of each `resources` entry joined together with a newline delimiter
     * using format `R.string.confirmation` ("This web page wants to use following resources:\n\n%s"),
     * whose negative button displays the text with id `R.string.deny` ("Deny") and whose
     * [DialogInterface.OnClickListener] is an anonymous class which calls the [Listener.onConfirmation]
     * method of our parent fragment with `false` as the allowed flag, and whose positive button
     * displays the text with id `R.string.allow` ("Allow") and whose [DialogInterface.OnClickListener]
     * is an anonymous class which calls the [Listener.onConfirmation] method of our parent fragment
     * with `true` as the allowed flag (the last method in the chain to the [AlertDialog.Builder] is
     * [AlertDialog.Builder.create] of course).
     *
     * @param savedInstanceState The last saved instance state of the [DialogFragment], or `null`
     * if this is a freshly created Fragment.
     * @return Return a new [Dialog] instance to be displayed by the Fragment.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val resources: Array<String?>? = requireArguments().getStringArray(ARG_RESOURCES)
        return AlertDialog.Builder(activity)
            .setMessage(getString(R.string.confirmation, TextUtils.join("\n", resources!!)))
            .setNegativeButton(R.string.deny) { dialog: DialogInterface?, which: Int ->
                (parentFragment as Listener?)!!.onConfirmation(false, resources)
            }
            .setPositiveButton(R.string.allow) { dialog: DialogInterface?, which: Int ->
                (parentFragment as Listener?)!!.onConfirmation(true, resources)
            }
            .create()
    }

    /**
     * Callback for the user's response.
     */
    internal interface Listener {
        /**
         * Called when the PermissionRequest is allowed or denied by the user.
         *
         * @param allowed   True if the user allowed the request.
         * @param resources The resources to be granted.
         */
        fun onConfirmation(allowed: Boolean, resources: Array<String?>?)
    }

    companion object {
        /**
         * Key in the arguments bundle for the [ConfirmationDialogFragment] in which we store the
         * array of permissions that we are requesting.
         */
        private const val ARG_RESOURCES = "resources"

        /**
         * Creates a new instance of [ConfirmationDialogFragment]. We create a new instance to
         * initialize our [ConfirmationDialogFragment] variable `val fragment`, and a new instance
         * for [Bundle] variable `val args`. We add our [Array] of [String] parameter [resources] as
         * a string array to `args` and set the arguments of `fragment` to it. Finally we return
         * `fragment` to the caller.
         *
         * @param resources The list of resources requested by PermissionRequest.
         * @return A new instance.
         */
        @JvmStatic
        fun newInstance(resources: Array<String?>?): ConfirmationDialogFragment {
            val fragment = ConfirmationDialogFragment()
            val args = Bundle()
            args.putStringArray(ARG_RESOURCES, resources)
            fragment.arguments = args
            return fragment
        }
    }
}
