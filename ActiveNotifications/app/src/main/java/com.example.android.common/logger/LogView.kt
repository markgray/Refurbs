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
@file:Suppress(
    "SameParameterValue",
    "ReplaceNotNullAssertionWithElvisReturn",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.common.logger

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView

/**
 * Simple TextView which is used to output log data received through the LogNode interface.
 */
class LogView : AppCompatTextView, LogNode {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    )

    /**
     * Formats the log data and prints it out to the LogView.
     *
     * @param priority Log level of the data being logged.  Verbose, Error, etc.
     * @param tag Tag for for the log data.  Can be used to organize log statements.
     * @param msg The actual message to be logged. The actual message to be logged.
     * @param tr If an exception was thrown, this can be sent along for the logging facilities
     * to extract and print useful information.
     */
    override fun println(priority: Int, tag: String?, msg: String?, tr: Throwable?) {
        var priorityStr: String? = null
        when (priority) {
            Log.VERBOSE -> priorityStr = "VERBOSE"
            Log.DEBUG -> priorityStr = "DEBUG"
            Log.INFO -> priorityStr = "INFO"
            Log.WARN -> priorityStr = "WARN"
            Log.ERROR -> priorityStr = "ERROR"
            Log.ASSERT -> priorityStr = "ASSERT"
            else -> {}
        }

        // Handily, the Log class has a facility for converting a stack trace into a usable string.
        var exceptionStr: String? = null
        if (tr != null) {
            exceptionStr = Log.getStackTraceString(tr)
        }

        // Take the priority, tag, message, and exception, and concatenate as necessary
        // into one usable line of text.
        val outputBuilder = StringBuilder()
        val delimiter = "\t"
        appendIfNotNull(outputBuilder, priorityStr, delimiter)
        appendIfNotNull(outputBuilder, tag, delimiter)
        appendIfNotNull(outputBuilder, msg, delimiter)
        appendIfNotNull(outputBuilder, exceptionStr, delimiter)

        // In case this was originally called from an AsyncTask or some other off-UI thread,
        // make sure the update occurs within the UI thread.
        (context as Activity).runOnUiThread(Thread { // Display the text we just generated within the LogView.
            appendToLog(outputBuilder.toString())
        })
        if (next != null) {
            next!!.println(priority, tag, msg, tr)
        }
    }

    /**
     * Takes a string and adds to it, with a separator, if the bit to be added isn't null. Since
     * the logger takes so many arguments that might be null, this method helps cut out some of the
     * agonizing tedium of writing the same 3 lines over and over.
     *
     * @param source StringBuilder containing the text to append to.
     * @param addStr The String to append
     * @param delimiter The String to separate the source and appended strings. A tab or comma,
     * for instance.
     * @return The fully concatenated String as a StringBuilder
     */
    @Suppress("SameParameterValue")
    private fun appendIfNotNull(
        source: StringBuilder,
        addStr: String?,
        delimiter: String?
    ): StringBuilder {
        var delimiterLocal: String? = delimiter
        if (addStr != null) {
            if (addStr.isEmpty()) {
                delimiterLocal = ""
            }
            return source.append(addStr).append(delimiterLocal)
        }
        return source
    }

    /**
     * The next LogNode in the chain.
     */
    var next: LogNode? = null

    /**
     * Outputs the [String] parameter [s] as a new line of log data in the LogView.
     *
     * @param s String to be logged.
     */
    fun appendToLog(s: String) {
        append("\n$s")
    }
}