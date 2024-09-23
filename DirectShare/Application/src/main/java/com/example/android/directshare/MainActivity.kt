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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.directshare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toolbar
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Provides the landing screen of this sample. There is nothing particularly interesting here. All
 * the codes related to the Direct Share feature are in [SampleChooserTargetService].
 */
class MainActivity : ComponentActivity() {
    /**
     * [EditText] with resource id [R.id.body], user enters text here that they want to share.
     */
    private var mEditBody: EditText? = null

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file [R.layout.main]. We set the
     * action bar of our activity to the [Toolbar] whose view has id [R.id.toolbar], initialize
     * [EditText] field [mEditBody] by finding the view with id [R.id.body], and set the
     * [View.OnClickListener] of the view with id [R.id.share] to our field [mOnClickListener].
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top+actionBar!!.height/2
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        setActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        mEditBody = findViewById(R.id.body)
        findViewById<View>(R.id.share).setOnClickListener(mOnClickListener)
    }

    /**
     * [View.OnClickListener] for the [Button] with id [R.id.share] ("SHARE"), its
     * [View.OnClickListener.onClick] override simply calls our method [share].
     */
    private val mOnClickListener = View.OnClickListener { v ->
        /**
         * Called when the Button with id R.id.share is clicked. We use a switch statement to make
         * sure the view has id R.id.share before calling our method `share`.
         *
         * @param v `View` that was clicked.
         */
        when (v.id) {
            R.id.share -> share()
        }
    }

    /**
     * Emits a sample share [Intent]. We initialize [Intent] variable `val sharingIntent` with an
     * [Intent] whose action is [Intent.ACTION_SEND], set its type to "text/plain", and add the text
     * in our [EditText] field [mEditBody] as an extra under the key [Intent.EXTRA_TEXT]
     * ("android.intent.extra.TEXT"). We then start the chooser activity created by the
     * [Intent.createChooser] method from `sharingIntent`, with the title set to the string with id
     * [R.string.send_intent_title] ("Send a message via:").
     */
    private fun share() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, mEditBody!!.text.toString())
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.send_intent_title)))
    }
}