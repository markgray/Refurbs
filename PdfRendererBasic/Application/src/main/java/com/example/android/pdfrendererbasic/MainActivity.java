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

package com.example.android.pdfrendererbasic;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This sample demonstrates how to use PdfRenderer to display PDF documents on the screen.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Tag name for our {@code PdfRendererBasicFragment} fragment, used when we add the fragment and
     * in the testing of {@code PdfRendererBasicFragment}.
     */
    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";

    /**
     * Called when the activity is starting. First we call our super's implementation of {@code onCreate},
     * then we set our content view to our layout file R.layout.activity_main_real. If our parameter
     * {@code Bundle savedInstanceState} is null this is the first time we were called, so we fetch
     * the FragmentManager for interacting with fragments associated with this activity and use it to
     * begin a {@code FragmentTransaction} to which we chain an {@code add} command to add a new instance
     * of {@code PdfRendererBasicFragment} to the view in our layout with ID R.id.container using
     * FRAGMENT_PDF_RENDERER_BASIC ("pdf_renderer_basic") as its fragment tag, and to this command we
     * chain a command to commit the {@code FragmentTransaction}.
     *
     * @param savedInstanceState if this is null, this is the first time we were called so we need to
     *                           construct and add our fragment {@code PdfRendererBasicFragment}. If
     *                           it is not null we are being called after a configuration change so
     *                           the system will take care of restoring the old fragment.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_real);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PdfRendererBasicFragment(), FRAGMENT_PDF_RENDERER_BASIC)
                    .commit();
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We retrieve a {@code MenuInflater}
     * for this context and use it to inflate our menu layout file R.menu.main into our parameter
     * {@code Menu menu}, then return true to the caller so that the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown, we return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We switch on the item
     * id of our parameter {@code MenuItem item} and if it is R.id.action_info we construct a new
     * {@code AlertDialog.Builder}, set its message to the string with resource ID R.string.intro_message
     * ("This sample demonstrates how to use PdfRenderer to display PDF documents on the screen."),
     * set its positive button to display the string with resource id android.R.string.ok ("OK") with
     * null as its {@code OnClickListener}, then chain a call to its {@code show} method to display the
     * {@code AlertDialog} it built. We then return true to the caller to consume the event here. If the
     * item id is not R.id.action_info we return the value returned by our super's implementation of
     * {@code onOptionsItemSelected}.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.intro_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
