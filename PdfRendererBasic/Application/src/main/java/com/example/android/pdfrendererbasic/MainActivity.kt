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
package com.example.android.pdfrendererbasic

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

/**
 * This sample demonstrates how to use PdfRenderer to display PDF documents on the screen.
 */
class MainActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.activity_main_real`.
     *
     * We initialize our [FrameLayout] variable `rootView` to the view with ID `R.id.container`
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
     * If our [Bundle] parameter [savedInstanceState] is `null` this is the first time we were
     * called, so we fetch the [FragmentManager] for interacting with fragments associated with this
     * activity and use it to begin a [FragmentTransaction] to which we chain an
     * [FragmentTransaction.add] command to add a new instance of [PdfRendererBasicFragment] to the
     * view in our layout with ID `R.id.container` using [FRAGMENT_PDF_RENDERER_BASIC]
     * ("pdf_renderer_basic") as its fragment tag, and to this command we chain a command to
     * `commit` the [FragmentTransaction].
     *
     * @param savedInstanceState if this is `null`, this is the first time we were called so we need
     * to construct and add our fragment [PdfRendererBasicFragment]. If it is not `null` we are
     * being called after a configuration change so the system will take care of restoring the old
     * fragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_real)
        val rootView = findViewById<FrameLayout>(R.id.container)
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
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, PdfRendererBasicFragment(), FRAGMENT_PDF_RENDERER_BASIC)
                .commit()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We retrieve a [MenuInflater]
     * for this context and use it to inflate our menu layout file `R.menu.main` into our [Menu]
     * parameter [menu], then return `true` to the caller so that the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed;
     * if you return `false` it will not be shown, we return `true`
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We switch on the item
     * id of our [MenuItem] parameter [item] and if it is `R.id.action_info` we construct a new
     * [AlertDialog.Builder], set its message to the string with resource ID `R.string.intro_message`
     * ("This sample demonstrates how to use PdfRenderer to display PDF documents on the screen."),
     * set its positive button to display the string with resource id [android.R.string.ok] ("OK")
     * with `null` as its [DialogInterface.OnClickListener], then chain a call to its `show` method
     * to display the [AlertDialog] it built. We then return `true` to the caller to consume the
     * event here. If the item id is not `R.id.action_info` we return the value returned by our
     * super's implementation of `onOptionsItemSelected`.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to proceed, `true` to
     * consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_info) {
            AlertDialog.Builder(this)
                .setMessage(R.string.intro_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * Tag name for our [PdfRendererBasicFragment] fragment, used when we add the fragment and
         * in the testing of [PdfRendererBasicFragment].
         */
        const val FRAGMENT_PDF_RENDERER_BASIC: String = "pdf_renderer_basic"
    }
}
