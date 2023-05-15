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
package com.example.android.directshare

import android.widget.TextView

/**
 * A simple utility to bind a [TextView] with a [Contact].
 */
object ContactViewBinder {
    /**
     * Binds the `textView` with the specified `contact`. We set the text of our parameter
     * `TextView textView` to the name returned by the `getName` method of our parameter
     * `contact`, and set the start Drawable of `textView` to the drawable whose resource
     * id is returned by the `getIcon` method of `contact`.
     *
     * @param contact  The contact.
     * @param textView The TextView.
     */
    fun bind(contact: Contact, textView: TextView) {
        textView.text = contact.name
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(contact.icon, 0, 0, 0)
    }
}