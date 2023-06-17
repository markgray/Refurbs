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
@file:Suppress("unused", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.common.logger

/**
 * Simple [LogNode] filter, removes everything except the message. Useful for situations like
 * on-screen log output where you don't want a lot of metadata displayed, just easy-to-read
 * message updates as they're happening.
 */
class MessageOnlyLogFilter : LogNode {
    /**
     * The next [LogNode] that data will be sent to..
     */
    var next: LogNode? = null

    /**
     * Takes the "next" [LogNode] as a parameter, to simplify chaining.
     *
     * @param next The next LogNode in the pipeline.
     */
    constructor(next: LogNode?) {
        this.next = next
    }

    constructor()

    /**
     * If our [LogNode] field [next] is not `null` we call its [LogNode.println] method with its
     * [priority] parameter set to [Log.NONE], its [tag] parameter set to `null`, and its [tr]
     * parameter set to `null`. Only its [msg] parameter is set to our [String] parameter [msg].
     *
     * @param priority Log level of the data being logged.  Verbose, Error, etc.
     * @param tag Tag for for the log data.  Can be used to organize log statements.
     * @param msg The actual message to be logged. The actual message to be logged.
     * @param tr If an exception was thrown, this can be sent along for the logging facilities
     * to extract and print useful information.
     */
    override fun println(priority: Int, tag: String?, msg: String?, tr: Throwable?) {
        if (next != null) {
            next!!.println(Log.NONE, null, msg, null)
        }
    }
}
