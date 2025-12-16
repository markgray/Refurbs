/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.cardreader

import android.annotation.SuppressLint
import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.ReaderCallback
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.android.cardreader.LoyaltyCardReader.AccountCallback
import com.example.android.common.logger.Log

/**
 * Generic UI for sample discovery.
 */
class CardReaderFragment : Fragment(), AccountCallback {
    /**
     * Our [LoyaltyCardReader] custom [ReaderCallback] callback class instance, invoked when an NFC
     * card is scanned while the device is running in reader mode. Passed as the callback when we
     * call the [NfcAdapter.enableReaderMode] method of the default NFC adapter.
     */
    var mLoyaltyCardReader: LoyaltyCardReader? = null

    /**
     * [TextView] in our layout with ID `R.id.card_account_field` which we use to display the account
     * number our [AccountCallback.onAccountReceived] override receives from `LoyaltyCardReader`.
     */
    private var mAccountField: TextView? = null

    /**
     * Called when sample is created. We just call our super's implementation of `onCreate`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    @Suppress("RedundantOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We use our [LayoutInflater]
     * parameter [inflater] to inflate our layout file `R.layout.main_fragment` into [View] variable
     * `val v` using our [ViewGroup] parameter [container] for the layout parameters. If `v` is not
     * `null` we initialize our [TextView] field [mAccountField] by finding the view in it with ID
     * `R.id.card_account_field` and set its text to the string "Waiting...". We initialize our
     * [LoyaltyCardReader] field [mLoyaltyCardReader] with a new instance, then call our method
     * [enableReaderMode] to enable reader mode, disable Android Beam and register our card reader
     * callback [mLoyaltyCardReader].
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.main_fragment, container, false)
        if (v != null) {
            mAccountField = v.findViewById(R.id.card_account_field)
            mAccountField!!.text = "Waiting..."
            mLoyaltyCardReader = LoyaltyCardReader(accountCallback = this)

            // Disable Android Beam and register our card reader callback
            enableReaderMode()
        }
        return v
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally tied to [Activity.onPause]
     * of the containing Activity's lifecycle. We call our super's implementation of `onPause` then
     * call our method [disableReaderMode] to restore the NFC adapter to normal mode of operation.
     */
    override fun onPause() {
        super.onPause()
        disableReaderMode()
    }

    /**
     * Called when the fragment is visible to the user and actively running. We call our super's
     * implementation of `onResume`, then call our method [enableReaderMode] to enable reader mode,
     * disable Android Beam and register our card reader callback [mLoyaltyCardReader].
     */
    override fun onResume() {
        super.onResume()
        enableReaderMode()
    }

    /**
     * Enables reader mode, disables Android Beam and registers our [LoyaltyCardReader] card reader
     * callback field [mLoyaltyCardReader]. We first log the message "Enabling reader mode", then
     * initialize [Activity] variable `val activity` with the [FragmentActivity] this fragment is
     * currently associated with. We use `activity` as the context in order to initialize [NfcAdapter]
     * variable `val nfc` with the default NFC adapter. If `nfc` is not `null` we call its
     * [NfcAdapter.enableReaderMode] method to limit the NFC controller to reader mode while this
     * [Activity] is in the foreground, using `activity` as the [Activity] that requests the adapter
     * to be in reader mode, [mLoyaltyCardReader] as the callback to be called when a tag is
     * discovered, [READER_FLAGS] for the flags indicating poll technologies and other optional
     * parameters ([NfcAdapter.FLAG_READER_NFC_A] and [NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK] in
     * our case), with `null` as the extras bundle. Note: the flags indicate that this activity is
     * interested in NFC-A devices (including other Android devices), and that the system should not
     * check for the presence of NDEF-formatted data (e.g. Android Beam).
     */
    private fun enableReaderMode() {
        Log.i(TAG, "Enabling reader mode")
        val activity: Activity? = activity
        val nfc: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
        nfc?.enableReaderMode(
            /* activity = */ activity,
            /* callback = */ mLoyaltyCardReader,
            /* flags = */ READER_FLAGS,
            /* extras = */ null
        )
    }

    /**
     * Restores the NFC adapter to normal mode of operation: supporting peer-to-peer (Android Beam),
     * card emulation, and polling for all supported tag technologies. We first log the message
     * "Disabling reader mode", then initialize [Activity] variable `val activity` with the
     * [FragmentActivity] this fragment is currently associated with. We use `activity` as the
     * context in order to initialize [NfcAdapter] variable `val nfc` with the default NFC adapter.
     * If `nfc` is not `null` we call its [NfcAdapter.disableReaderMode] method using `activity` as
     * the Activity that currently has reader mode enabled.
     */
    private fun disableReaderMode() {
        Log.i(TAG, "Disabling reader mode")
        val activity: Activity? = activity
        val nfc: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
        nfc?.disableReaderMode(activity)
    }

    /**
     * Callback called by [LoyaltyCardReader] when it receives an account number read from the
     * nfc adapter. We use the [FragmentActivity] this fragment is currently associated with
     * to call its [Activity.runOnUiThread] method to run an anonymous [Runnable] whose
     * [Runnable.run] override sets the text of our [TextView] field [mAccountField] to our
     * [String] parameter [account].
     *
     * @param account account number read from the nfc adapter by the `onTagDiscovered` override
     * of [LoyaltyCardReader]
     */
    override fun onAccountReceived(account: String?) {
        // This callback is run on a background thread, but updates to UI elements must be performed
        // on the UI thread.
        requireActivity().runOnUiThread { mAccountField!!.text = account }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "CardReaderFragment"

        /**
         * Recommended NfcAdapter flags for reading from other Android devices. Indicates that this
         * activity is interested in NFC-A devices (including other Android devices), and that the
         * system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
         */
        var READER_FLAGS: Int = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }
}
