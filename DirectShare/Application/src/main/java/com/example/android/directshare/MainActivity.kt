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
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Provides the landing screen of this sample. There is nothing particularly interesting here. All
 * the codes related to the Direct Share feature are in [SampleChooserTargetService].
 */
class MainActivity : ComponentActivity() {
    /**
     * [EditText] with resource id `R.id.body`, user enters text here that they want to share.
     */
    private var mEditBody: EditText? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We set the action bar of our activity to the [Toolbar] whose view has id `R.id.toolbar`,
     * initialize [EditText] field [mEditBody] by finding the view with id `R.id.body`, and set
     * the [View.OnClickListener] of the view with id `R.id.share` to our field [mOnClickListener].
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
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
     * [View.OnClickListener] for the [Button] with id `R.id.share` ("SHARE"), its
     * [View.OnClickListener.onClick] override simply calls our method [share].
     */
    private val mOnClickListener = View.OnClickListener { v ->
        /**
         * Called when the Button with id R.id.share is clicked. We use a `when` statement to make
         * sure the view has the id R.id.share before calling our method `share`.
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
     * `R.string.send_intent_title` ("Send a message via:").
     */
    private fun share() {
        val sharingIntent = Intent(/* action = */ Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(
            /* name = */ Intent.EXTRA_TEXT,
            /* value = */ mEditBody!!.text.toString()
        )
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.send_intent_title)))
    }
}
