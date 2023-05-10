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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog", "ReplaceNotNullAssertionWithElvisReturn", "FunctionName")

package com.example.android.cardreader

import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.android.common.logger.Log
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Arrays

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */
class LoyaltyCardReader(accountCallback: AccountCallback) : ReaderCallback {
    /**
     * Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
     * foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
     */
    private val mAccountCallback: WeakReference<AccountCallback>

    /**
     * Interface we use to report an account number read from the NFC adapter.
     */
    interface AccountCallback {
        /**
         * TODO: Add kdoc
         */
        fun onAccountReceived(account: String?)
    }

    /**
     * Our constructor. We save our parameter `AccountCallback accountCallback` in a new instance
     * of `WeakReference` for `mAccountCallback`.
     *
     * param accountCallback `AccountCallback` callback to call when we receive an account number.
     */
    init {
        mAccountCallback = WeakReference(accountCallback)
    }

    /**
     * Callback when a new tag is discovered by the system. Communication with the card should take
     * place here. First we log the message "New tag discovered". We initialize `IsoDep isoDep`
     * by fetching an instance for our parameter `Tag tag`. If this is not null, wrapped in a
     * try block intended to catch and log IOException we call the `connect` method of `isoDep`
     * to enable I/O operations to the tag from this `TagTechnology` object. We log the message
     * "Requesting remote AID: " then initialize `byte[] command` with an APDU for SELECT AID
     * command for our loyalty card service SAMPLE_LOYALTY_CARD_AID (the service we are interested in
     * communicating with). We log the message "Sending: " with the hex string of `command`
     * appended to it, then initialize `byte[] result` with the value returned by calling the
     * `transceive` method of `isoDep` with `command` as the raw ISO-DEP data to
     * send. We initialize `int resultLength` with the length of `result`, and initialize
     * the array `byte[] statusWord` with the last 2 bytes of `result` (the status word,
     * if AID is successfully selected, 0x9000 is returned here). We then initialize `byte[] payload`
     * with a copy of the rest of `result` (the optional payload, which is used here to hold the
     * account number). If `statusWord` is equal to SELECT_OK_SW (0x90, 0x00) we initialize
     * `String accountNumber` by decoding `payload` using the charset "UTF-8", log that
     * we "Received: " this, then fetch the `AccountCallback` we were constructed with from our
     * weak reference `mAccountCallback` in order to call its `onAccountReceived` method
     * to pass it the `accountNumber` string.
     *
     * @param tag Discovered tag
     */
    override fun onTagDiscovered(tag: Tag) {
        Log.i(TAG, "New tag discovered")
        // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
        // protocol.
        //
        // In order to communicate with a device using HCE, the discovered tag should be processed
        // using the IsoDep class.
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                // Connect to the remote NFC device
                isoDep.connect()
                // Build SELECT AID command for our loyalty card service.
                // This command tells the remote device which service we wish to communicate with.
                Log.i(TAG, "Requesting remote AID: $SAMPLE_LOYALTY_CARD_AID")
                val command = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID)
                // Send command to remote device
                Log.i(TAG, "Sending: " + ByteArrayToHexString(command))
                val result = isoDep.transceive(command)
                // If AID is successfully selected, 0x9000 is returned as the status word (last 2
                // bytes of the result) by convention. Everything before the status word is
                // optional payload, which is used here to hold the account number.
                val resultLength = result.size
                val statusWord = byteArrayOf(result[resultLength - 2], result[resultLength - 1])
                val payload = Arrays.copyOf(result, resultLength - 2)
                if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                    // The remote NFC device will immediately respond with its stored account number
                    val accountNumber = String(payload, Charsets.UTF_8)
                    Log.i(TAG, "Received: $accountNumber")
                    // Inform CardReaderFragment of received account number
                    mAccountCallback.get()!!.onAccountReceived(accountNumber)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error communicating with card: $e")
            }
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "LoyaltyCardReader"

        /**
         * AID for our loyalty card service.
         */
        private const val SAMPLE_LOYALTY_CARD_AID = "F222222222"

        /**
         * ISO-DEP command HEADER for selecting an AID.
         *
         *
         * Format: [Class | Instruction | Parameter 1 | Parameter 2]
         */
        private const val SELECT_APDU_HEADER = "00A40400"

        /**
         * "OK" status word sent in response to SELECT AID command (0x9000)
         */
        private val SELECT_OK_SW = byteArrayOf(0x90.toByte(), 0x00.toByte())

        /**
         * Build APDU for SELECT AID command. This command indicates which service a reader is interested
         * in communicating with. See ISO 7816-4.
         *
         *
         * We return the byte array constructed by our method `HexStringToByteArray` that it constructs
         * by converting to hex characters the string we form by concatenating the string SELECT_APDU_HEADER
         * followed by the hex encoding of the length of our parameter `String aid`, followed by
         * `aid` itself.
         *
         * @param aid Application ID (AID) to select
         * @return APDU for SELECT AID command
         */
        @JvmStatic
        fun BuildSelectApdu(aid: String): ByteArray {
            // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
            return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length / 2) + aid)
        }

        /**
         * Utility class to convert a byte array to a hexadecimal string. We initialize our constant
         * array `char[] hexArray` to contain the hexadecimal characters 0-F, then allocate for
         * `char[] hexChars` twice as many chars as the length of our parameter `byte[] bytes`,
         * and declare `int v`. We then loop with `j` as the index over the bytes in `bytes`
         * setting `v` to each byte masked by 0xFF to treat as unsigned, storing first the upper nibble
         * of `v` followed by the lower nibble. When done we return a string constructed from
         * `hexChars`.
         *
         * @param bytes Bytes to convert
         * @return String, containing hexadecimal representation.
         */
        @JvmStatic
        fun ByteArrayToHexString(bytes: ByteArray): String {
            val hexArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
            val hexChars = CharArray(bytes.size * 2)
            var v: Int
            for (j in bytes.indices) {
                v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        /**
         * Utility class to convert a hexadecimal string to a byte string. Behavior with input strings
         * containing non-hexadecimal characters is undefined. We initialize `int len` with the
         * length of our parameter `String s`. We allocate 1 byte per 2 hex characters for
         * `byte[] data`. We then loop for `i=0` to `i=len` stepping by 2 setting the
         * upper nibble of `data[i/2]` to the numeric value of the character at position `i`
         * in radix 16, and the lower nibble to the numeric value of the character at position `i+1`
         * in radix 16. When done with all the characters in `s` we return `data` to the caller.
         *
         * @param s String containing hexadecimal characters to convert
         * @return Byte array generated from input
         */
        @JvmStatic
        fun HexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i+1], 16)).toByte()
                i += 2
            }
            return data
        }
    }
}
