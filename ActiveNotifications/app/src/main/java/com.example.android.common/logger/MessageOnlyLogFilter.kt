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
 * Simple [LogNode] filter, removes everything except the message.
 * Useful for situations like on-screen log output where you don't want a lot of metadata displayed,
 * just easy-to-read message updates as they're happening.
 */
class MessageOnlyLogFilter : LogNode {
    /**
     * The next [LogNode] in the chain.
     */
    var next: LogNode? = null

    /**
     * Takes the "next" LogNode as a parameter, to simplify chaining.
     *
     * @param next The next LogNode in the pipeline.
     */
    constructor(next: LogNode?) {
        this.next = next
    }

    constructor()

    /**
     * Formats the log data, removing extraneous information and passing it to the next
     * [LogNode] in the chain.
     *
     * @param priority The priority of the log message.
     * @param tag The tag associated with the log message.
     * @param msg The message to be logged.
     * @param tr An exception to be logged.
     */
    override fun println(priority: Int, tag: String?, msg: String?, tr: Throwable?) {
        if (next != null) {
            next!!.println(Log.NONE, null, msg, null)
        }
    }
}