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
package com.example.android.messagingservice

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date

/**
 * A simple logger that uses shared preferences to log messages, their reads
 * and replies. Don't use this in a real world application. This logger is only
 * used for displaying the messages in the text view.
 */
internal object MessageLogger {
    /**
     * Preference file name
     */
    private const val PREF_MESSAGE = "MESSAGE_LOGGER"

    /**
     * date/time formatter with the default formatting style for the default locale.
     */
    private val DATE_FORMAT = SimpleDateFormat.getDateTimeInstance()

    /**
     * Line break constant -- two line feeds
     */
    private const val LINE_BREAKS = "\n\n"

    /**
     * Key of preference String containing our log messages
     */
    const val LOG_KEY = "message_data"

    /**
     * Appends a new message to the end of our LOG_KEY preference value. First we fetch a handle
     * to the `SharedPreferences prefs` for our shared preferences file "MESSAGE_LOGGER",
     * then we prepend a formatted date string to the `String message` passed us. We then
     * use `prefs` to fetch the old LOG_KEY String, append LINE_BREAKS and our new message
     * to the end of it and put the longer LOG_KEY String back in the preferences file.
     *
     * @param context Activity returned by `getActivity()`
     * @param message Message to add to log
     */
    fun logMessage(context: Context, message: String) {
        var messageLocal = message
        val prefs = getPrefs(context)
        messageLocal = DATE_FORMAT.format(Date(System.currentTimeMillis())) + ": " + messageLocal
        prefs.edit()
            .putString(LOG_KEY, prefs.getString(LOG_KEY, "") + LINE_BREAKS + messageLocal)
            .apply()
    }

    /**
     * Returns a `SharedPreferences` Object to read and modify the shared preference file
     * PREF_MESSAGE.
     *
     * @param context Activity returned by `getActivity()`
     * @return The `SharedPreferences` instance for the preference file PREF_MESSAGE
     */
    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_MESSAGE, Context.MODE_PRIVATE)
    }

    /**
     * Returns the String value stored under LOG_KEY in our shared preferences file PREF_MESSAGE.
     *
     * @param context Activity returned by `getActivity()`
     * @return String value stored under LOG_KEY in our shared preferences file PREF_MESSAGE
     */
    fun getAllMessages(context: Context): String? {
        return getPrefs(context).getString(LOG_KEY, "")
    }

    /**
     * Clears the String value stored under LOG_KEY in our shared preferences file PREF_MESSAGE. It
     * does this by calling the method `SharedPreferences.Editor.remove` and `apply()`
     *
     * @param context Activity returned by `getActivity()`
     */
    fun clear(context: Context) {
        getPrefs(context).edit().remove(LOG_KEY).apply()
    }
}