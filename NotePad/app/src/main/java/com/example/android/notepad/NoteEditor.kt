/*
 * Copyright (C) 2007 The Android Open Source Project
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
@file:Suppress("DEPRECATION", "ReplaceJavaStaticMethodWithKotlinAnalog", "ReplaceNotNullAssertionWithElvisReturn", "JoinDeclarationAndAssignment", "RedundantIf")

package com.example.android.notepad

import android.app.Activity
import android.app.LoaderManager.LoaderCallbacks
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText

/**
 * This Activity handles "editing" a note, where editing is responding to
 * [Intent.ACTION_VIEW] (request to view data), edit a note
 * [Intent.ACTION_EDIT], create a note [Intent.ACTION_INSERT], or
 * create a new note from the current contents of the clipboard [Intent.ACTION_PASTE].
 */
class NoteEditor : Activity(), LoaderCallbacks<Cursor?> {
    // Global mutable variables
    /**
     * Current state of this `NoteEditor`, either STATE_EDIT, or STATE_INSERT
     */
    private var mState = 0

    /**
     * Data URI for the note we are working with, either the Data URI of the `Intent` that
     * launched us, or the URI returned from the `insert` method of the `ContentResolver`
     * when we insert an empty record in the provider.
     */
    private var mUri: Uri? = null

    /**
     * `EditText` with id R.id.note in our layout file R.layout.note_editor used to enter note
     */
    private var mText: EditText? = null

    /**
     * The original content of the current note, either as restored in `onCreate` or set from
     * the cursor returned by our provider for COLUMN_NAME_NOTE in our `onLoadFinished` override
     */
    private var mOriginalContent: String? = null

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     */
    class LinedEditText(context: Context?, attrs: AttributeSet?) : EditText(context, attrs) {
        /**
         * Uninitialized `Rect` constructed in our `onCreate` override so that our
         * `onDraw` override does not need to allocate one.
         */
        private val mRect: Rect

        /**
         * `Paint` constructed and initialized in our `onCreate` override so that our
         * `onDraw` override does not need to allocate one.
         */
        private val mPaint: Paint

        /**
         * Perform inflation from XML, used by LayoutInflater. First we call our super's constructor,
         * then we initialize our field `Rect mRect` with a new instance, and initialize our
         * field `Paint mPaint` with a new instance, set its style to STROKE, and set its color
         * to Blue with an alpha of 0x80.
         *
         * param context The Context the view is running in, through which it can
         * access the current theme, resources, etc.
         * param attrs   The attributes of the XML tag that is inflating the view.
         */
        init {

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = Rect()
            mPaint = Paint()
            mPaint.style = Paint.Style.STROKE
            mPaint.color = -0x7fffff01
        }

        /**
         * This is called to draw the LinedEditText object. First we initialize `int count` with
         * the number of lines of text in our `EditText`. We set `Rect r` to our field
         * `mRect` and `Paint paint` to our field `mPaint`. Then we loop over
         * `int i` for each of our `count` lines:
         *
         *  *
         * We initialize `int baseline` to the baseline for line `i` storing the
         * extent of the line in `r`
         *
         *  *
         * We have our parameter `Canvas canvas` a horizontal line from the `left`
         * field of `r` one pixel below `baseline` to the `right` field of
         * `r`
         *
         *
         * Once we have finished drawing our lines we call our super's implementation of `onDraw`.
         *
         * @param canvas The canvas on which the background is drawn.
         */
        override fun onDraw(canvas: Canvas) {

            // Gets the number of lines of text in the View.
            val count = lineCount

            // Gets the global Rect and Paint objects
            val r = mRect
            val paint = mPaint

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             */for (i in 0 until count) {

                // Gets the baseline coordinates for the current line of text
                val baseline = getLineBounds(i, r)

                /*
                 * Draws a line in the background from the left of the rectangle to the right,
                 * at a vertical position one dip below the baseline, using the "paint" object
                 * for details.
                 */canvas.drawLine(r.left.toFloat(), (baseline + 1).toFloat(), r.right.toFloat(), (baseline + 1).toFloat(), paint)
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas)
        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it. First we call our
     * super's implementation of `onCreate`. If our parameter `Bundle savedInstanceState`
     * is not null we set our field `String mOriginalContent` to the string stored under the
     * key ORIGINAL_CONTENT ("origContent") in it. We initialize `Intent intent` with the
     * `Intent` that launched this activity, and `String action` with the action of
     * `intent`. We then branch on the value of `action`:
     *
     *  *
     * ACTION_EDIT: We set our state `mState` to STATE_EDIT and set `Uri mUri`
     * to the data URI of the `Intent intent` that launched our activity.
     *
     *  *
     * ACTION_INSERT or ACTION_PASTE: We set our state `mState` to STATE_INSERT, set
     * our activities title to the string with resource id R.string.title_create ("New note"),
     * and set `Uri mUri` to the URL returned when we have a `ContentResolver`
     * instance for our application's package insert an entry into the table addressed by the
     * data URI of the `Intent intent` that launched our activity. If `mUri` is
     * null we log the error, finish the activity and return. Otherwise we call `setResult`
     * with the result code RESULT_OK and an `Intent` instance whose action is the string
     * value of `mUri`.
     *
     *  *
     * action was other than EDIT or INSERT: we log the error, finish the activity and return.
     *
     *
     * We use the `LoaderManager` for this activity to ensure a loader is initialized and active
     * for the loader id LOADER_ID, with null for the Optional arguments to supply to the loader at
     * construction, and 'this' as the Interface the LoaderManager will call to report about changes
     * in the state of the loader (our `onCreateLoader`, `onLoadFinished`, and
     * `onLoaderReset` overloads will be called). If `action` is ACTION_PASTE we call our
     * `performPaste` method to replace the note's data with the contents of the clipboard, and
     * set our state `mState` to STATE_EDIT. Finally we set our content view to our layout file
     * R.layout.note_editor, and initialize our field `EditText mText` by finding the view in
     * it with id R.id.note.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down then this Bundle contains the data it most recently supplied
     * in [.onSaveInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recovering the instance state from a previously destroyed Activity instance
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT)
        }

        /*
         * Creates an Intent to use when the Activity object's result is sent back to the
         * caller.
         */
        val intent = intent

        /*
         *  Sets up for the edit, based on the action specified for the incoming Intent.
         */

        // Gets the action that triggered the intent filter for this Activity
        val action = intent.action

        // For an edit action:
        if (Intent.ACTION_EDIT == action) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            mState = STATE_EDIT
            mUri = intent.data

            // For an insert or paste action:
        } else if (Intent.ACTION_INSERT == action || Intent.ACTION_PASTE == action) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an
            // empty record in the provider
            mState = STATE_INSERT
            title = getText(R.string.title_create)
            mUri = contentResolver.insert(intent.data!!, null)

            /*
             * If the attempt to insert the new note fails, shuts down this Activity. The
             * originating Activity receives back RESULT_CANCELED if it requested a result.
             * Logs that the insert failed.
             */if (mUri == null) {

                // Writes the log identifier, a message, and the URI that failed.
                Log.e(TAG, "Failed to insert new note into " + getIntent().data)

                // Closes the activity.
                finish()
                return
            }

            // Since the new entry was created, this sets the result to be returned
            // set the result to be returned.
            setResult(RESULT_OK, Intent().setAction(mUri.toString()))

            // If the action was other than EDIT or INSERT:
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting")
            finish()
            return
        }

        // Initialize the LoaderManager and start the query
        loaderManager.initLoader(LOADER_ID, null, this)

        // For a paste, initializes the data from clipboard.
        if (Intent.ACTION_PASTE == action) {
            // Does the paste
            performPaste()
            // Switches the state to EDIT so the title can be modified.
            mState = STATE_EDIT
        }

        // Sets the layout for this Activity. See res/layout/note_editor.xml
        setContentView(R.layout.note_editor)

        // Gets a handle to the EditText in the the layout.
        mText = findViewById(R.id.note)
    }

    /**
     * This method is called when an Activity loses focus during its normal operation.
     * The Activity has a chance to save its state so that the system can restore
     * it.
     *
     *
     * Notice that this method isn't a normal part of the Activity lifecycle. It won't be called
     * if the user simply navigates away from the Activity.
     *
     *
     * We store our field `String mOriginalContent` under the key ORIGINAL_CONTENT in our
     * parameter `Bundle outState`, then call our super's implementation of `onSaveInstanceState`.
     *
     * @param outState Bundle in which to place our saved state.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        // Save away the original text, so we still have it if the activity
        // needs to be re-created.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent)
        // Call the superclass to save the any view hierarchy state
        super.onSaveInstanceState(outState)
    }

    /**
     * This method is called when the Activity loses focus.
     *
     *
     * While there is no need to override this method in this app, it is shown here to highlight
     * that we are not saving any state in onPause, but have moved app state saving to onStop
     * callback.
     *
     *
     * In earlier versions of this app and popular literature it had been shown that onPause is a good
     * place to persist any unsaved work, however, this is not really a good practice because of how
     * application and process lifecycle behave.
     *
     *
     * As a general guideline apps should have a way of saving their business logic that does not
     * solely rely on Activity (or other component) lifecycle state transitions.
     * As a backstop you should save any app state, not saved during lifetime of the Activity, in
     * onStop().
     *
     *
     * For a more detailed explanation of this recommendation please read
     * [
 * Processes and Application Life Cycle ](https://developer.android.com/guide/topics/processes/process-lifecycle.html).
     * [
 * Pausing and Resuming an Activity ](https://developer.android.com/training/basics/activity-lifecycle/pausing.html).
     *
     *
     * We just call our super's implementation of `onPause`
     */
    @Suppress("RedundantOverride")
    override fun onPause() {
        super.onPause()
    }

    /**
     * This method is called when the Activity becomes invisible.
     *
     *
     * For Activity objects that edit information, onStop() may be the one place where changes maybe
     * saved.
     *
     *
     * If the user hasn't done anything, then this deletes or clears out the note, otherwise it
     * writes the user's work to the provider.
     *
     *
     * First we call our super's implementation of `onStop`, then we initialize `String text`
     * with the text in our field `EditText mText`, and `int length` with the length of
     * `text`. If we now branch on a series of 'if' statements:
     *
     *  *
     * If this activity is in the process of finishing and `length` is equal to 0, we
     * set our result code to `RESULT_CANCELED` and call our `deleteNote` method
     * to instruct our provider to delete the note that the user was working on.
     *
     *  *
     * If our state `mState` is equal to STATE_EDIT we call our `updateNote` to
     * have our provider save `text` as the note contents and null as the title of the
     * note entry that was being edited.
     *
     *  *
     * If our state `mState` is equal to STATE_INSERT we call our `updateNote` to
     * have our provider save `text` as both the note contents the title of the note
     * entry that was being edited, then set our state `mState` to STATE_EDIT.
     *
     *
     */
    override fun onStop() {
        super.onStop()

        // Get the current note text.
        val text = mText!!.text.toString()
        val length = text.length

        /**
         * If the Activity is in the midst of finishing and there is no text in the current
         * note, returns a result of CANCELED to the caller, and deletes the note. This is done
         * even if the note was being edited, the assumption being that the user wanted to
         * "clear out" (delete) the note.
         */
        if (isFinishing && length == 0) {
            setResult(RESULT_CANCELED)
            deleteNote()

            /*
                 * Writes the edits to the provider. The note has been edited if an existing note
                 * was retrieved into the editor *or* if a new note was inserted.
                 * In the latter case, onCreate() inserted a new empty note into the provider,
                 * and it is this new note that is being edited.
                 */
        } else if (mState == STATE_EDIT) {
            // Creates a map to contain the new values for the columns
            updateNote(text, null)
        } else if (mState == STATE_INSERT) {
            updateNote(text, text)
            mState = STATE_EDIT
        }
    }

    /**
     * This method is called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     *
     * Builds the menus for editing and inserting, and adds in alternative actions that
     * registered themselves to handle the MIME types for this application.
     *
     *
     * We initialize `MenuInflater inflater` with an instance for this context, and then use it
     * to inflate our menu layout file R.menu.editor_options_menu into our parameter `Menu menu`.
     * If our state `mState` is equal to STATE_EDIT we initialize `Intent intent` with a
     * new instance with `mUri` as its data URI, add the category CATEGORY_ALTERNATIVE to it,
     * then add to `menu` a group of menu items corresponding to actions that can be performed
     * for the Intent `intent` using CATEGORY_ALTERNATIVE (Category code for the order integer
     * for items/groups that are alternative actions) as the group identifier that the items should
     * be part of, with 0 as the item id, 0 as the sort order, the `ComponentName` of 'this'
     * `NoteEditor` class, null for the specific items to be placed first, `Intent intent`
     * as the Intent describing the kinds of items to populate in the list as defined by the method
     * `queryIntentActivityOptions` (which Retrieves a set of activities that should be
     * presented to the user as similar options), 0 for the flags of Additional options controlling
     * how the items are added, and null for the Optional array in which to place the menu items that
     * were generated. In any case we return the value returned by our super's implementation of
     * `onCreateOptionsMenu` to the caller.
     *
     * @param menu A Menu object to which items should be added.
     * @return True to display the menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate menu from XML resource
        val inflater = menuInflater
        inflater.inflate(R.menu.editor_options_menu, menu)

        // Only add extra menu items for a saved note
        if (mState == STATE_EDIT) {
            // Append to the
            // menu items for any other activities that can do stuff with it
            // as well.  This does a query on the system for any activities that
            // implement the ALTERNATIVE_ACTION for our data, adding a menu item
            // for each one that is found.
            val intent = Intent(null, mUri)
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                ComponentName(this, NoteEditor::class.java), null, intent, 0, null)
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Prepare the Screen's standard options menu to be displayed. We initialize `Cursor cursor`
     * by calling the `query` method of a `ContentResolver` instance for our application's
     * package with `mUri` as the URI for the note that is to be retrieved, PROJECTION for the
     * columns to retrieve, and null for the selection criteria, selection arguments, and sort order.
     * We call the `moveToFirst` method of `cursor` to move it to the first row. We then
     * initialize `int colNoteIndex` with the index in `cursor` that contains the column
     * that contains the column named COLUMN_NAME_NOTE, initialize `String savedNote` with the
     * column whose index is `colNoteIndex`, and initialize `String currentNote` with the
     * text string contained in `EditText mText` (the version that the user has been editing).
     * If `savedNote` is equal to `currentNote` we find the item in our parameter `Menu menu`
     * with id R.id.menu_revert ("Revert changes") and set it to be invisible, and if they are not
     * equal we set it to be visible. Then we close `cursor` and return the value returned by
     * our super's implementation of `onPrepareOptionsMenu` to the caller.
     *
     * @param menu The options menu as last shown or first initialized by onCreateOptionsMenu().
     * @return You must return true for the menu to be displayed;
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Check if note has changed and enable/disable the revert option
        val cursor = contentResolver.query(
            mUri!!,  // The URI for the note that is to be retrieved.
            PROJECTION,  // The columns to retrieve
            null,  // No selection criteria are used, so no where columns are needed.
            null,  // No where columns are used, so no where values are needed.
            null // No sort order is needed.
        )
        cursor!!.moveToFirst()
        val colNoteIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE)
        val savedNote = cursor.getString(colNoteIndex)
        val currentNote = mText!!.text.toString()
        if (savedNote == currentNote) {
            menu.findItem(R.id.menu_revert).isVisible = false
        } else {
            menu.findItem(R.id.menu_revert).isVisible = true
        }
        cursor.close()
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * This method is called when a menu item is selected. Android passes in the selected item.
     * The switch statement in this method calls the appropriate method to perform the action the
     * user chose. We switch on the value of the item id of our parameter `MenuItem item`:
     *
     *  *
     * R.id.menu_save: ("Save") We initialize `String text` with the text contained in
     * our `EditText mText`, then call our `updateNote` method to store `text`
     * in the COLUMN_NAME_NOTE column of the note with the URI `Uri mUri`, and passing
     * null as the title so that the `updateNote` method will construct and store a new
     * title derived from `text`. We then break.
     *
     *  *
     * R.id.menu_delete: ("Delete") We call our method `deleteNote` to delete the note
     * with the URI `Uri mUri` from our database, call `finish` to end this activity,
     * and then break.
     *
     *  *
     * R.id.menu_revert: ("Revert changes") We call our method `cancelNote` to cancel
     * the work done on a note (it deletes the note if it was newly created, or reverts to
     * the original text of the note if it was being edited) and then break.
     *
     *
     * Finally we return the value returned by our super's implementation of `onOptionsItemSelected`
     * to the caller.
     *
     * @param item The selected MenuItem
     * @return True to indicate that the item was processed, and no further work is necessary. False
     * to proceed to further processing as indicated in the MenuItem object.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle all of the possible menu actions.
        when (item.itemId) {
            R.id.menu_save -> {
                val text = mText!!.text.toString()
                updateNote(text, null)
                finish()
            }

            R.id.menu_delete -> {
                deleteNote()
                finish()
            }

            R.id.menu_revert -> cancelNote()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * A helper method that replaces the note's data with the contents of the clipboard. First we
     * initialize `ClipboardManager clipboard` with a handle to the system level service
     * CLIPBOARD_SERVICE, initialize `ContentResolver cr` with a content resolver instance, and
     * initialize `ClipData clip` with the current primary clip on the clipboard. If `clip`
     * is null we do nothing more, but if it is not null we initialize `String text` and
     * `String title` to null, initialize `ClipData.Item item` to the item at index 0 in
     * `clip` and initialize `Uri uri` with the raw URI contained in `item`. If
     * `uri` is not null, and the type of `uri` as determined by the `getType` method
     * of `cr` is equal to NotePad.Notes.CONTENT_ITEM_TYPE (has the MIME type of our notes) we
     * initialize `Cursor orig` by having `cr` query the content provider for `uri`,
     * using PROJECTION for the columns to retrieve, and null for the selection, selection arguments,
     * and sort order. If `orig` is not null we move it to the first row, initialize `int colNoteIndex`
     * with the column index of the COLUMN_NAME_NOTE named column in `orig`, and  initialize
     * `int colTitleIndex` with the column index of the COLUMN_NAME_TITLE named column in `orig`.
     * We then set `text` to the string stored under index `colNoteIndex` in `orig`,
     * and set `title` to the string stored under index `colTitleIndex` in `orig`.
     * We then close `orig`. If `text` is still null we set it to the string created by
     * coercing `item` to text. Finally we call our `updateNote` method to replace the
     * current note contents stored in the address `Uri mUri` in our database with `text`
     * and `title`.
     */
    private fun performPaste() {

        // Gets a handle to the Clipboard Manager
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        // Gets a content resolver instance
        val cr = contentResolver

        // Gets the clipboard data from the clipboard
        val clip = clipboard.primaryClip
        if (clip != null) {
            var text: String? = null
            var title: String? = null

            // Gets the first item from the clipboard data
            val item = clip.getItemAt(0)

            // Tries to get the item's contents as a URI pointing to a note
            val uri = item.uri

            // Tests to see that the item actually is an URI, and that the URI
            // is a content URI pointing to a provider whose MIME type is the same
            // as the MIME type supported by the Note pad provider.
            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE == cr.getType(uri)) {

                // The clipboard holds a reference to data with a note MIME type. This copies it.
                val orig = cr.query(
                    uri,  // URI for the content provider
                    PROJECTION,  // Get the columns referred to in the projection
                    null,  // No selection variables
                    null,  // No selection variables, so no criteria are needed
                    null // Use the default sort order
                )

                // If the Cursor is not null, and it contains at least one record
                // (moveToFirst() returns true), then this gets the note data from it.
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        val colNoteIndex = orig.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE)
                        val colTitleIndex = orig.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE)
                        text = orig.getString(colNoteIndex)
                        title = orig.getString(colTitleIndex)
                    }

                    // Closes the cursor.
                    orig.close()
                }
            }

            // If the contents of the clipboard wasn't a reference to a note, then
            // this converts whatever it is to text.
            if (text == null) {
                text = item.coerceToText(this).toString()
            }

            // Updates the current note with the retrieved title and text.
            updateNote(text, title)
        }
    }

    /**
     * Replaces the current note contents with the text and title provided as arguments. First we
     * initialize `ContentValues values` with a new instance, and store the current system time
     * in milliseconds under the key NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE ("modified") in it.
     * We branch on whether our state `mState` is STATE_INSERT:
     *
     *  *
     * It is STATE_INSERT: If our parameter `String title` is null we initialize `int length`
     * with the length of our parameter `String text` and set `title` to a substring
     * of `text` that is is 31 characters long or the number of characters in the `text`
     * plus one, whichever is smaller. Then if `length` is greater than 30 we initialize
     * `int lastSpace` to the position of the last space character in title and if this
     * is greater than 0 we set `title` to the substring between 0 and `lastSpace`.
     * Then we store `title` in `values` under the key NotePad.Notes.COLUMN_NAME_TITLE
     * ("title").
     *
     *  *
     * If it is not STATE_INSERT, but `title` is not null: We store `title` in
     * `values` under the key NotePad.Notes.COLUMN_NAME_TITLE ("title").
     *
     *
     * We then store `text` in `values` under the key NotePad.Notes.COLUMN_NAME_NOTE
     * ("note"). We then request a `ContentResolver` instance for our application's package to
     * update the record with the address `Uri mUri` in our database with the contents of
     * `values` using null for both the selection and selection arguments.
     *
     * @param text The new note contents to use.
     * @param title The new note title to use
     */
    private fun updateNote(text: String, title: String?) {

        // Sets up a map to contain values to be updated in the provider.
        var titleLocal = title
        val values = ContentValues()
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis())

        // If the action is to insert a new note, this creates an initial title for it.
        if (mState == STATE_INSERT) {

            // If no title was provided as an argument, create one from the note text.
            if (titleLocal == null) {

                // Get the note's length
                val length = text.length

                // Sets the title by getting a substring of the text that is 31 characters long
                // or the number of characters in the note plus one, whichever is smaller.
                titleLocal = text.substring(0, Math.min(30, length))

                // If the resulting length is more than 30 characters, chops off any
                // trailing spaces
                if (length > 30) {
                    val lastSpace = titleLocal.lastIndexOf(' ')
                    if (lastSpace > 0) {
                        titleLocal = titleLocal.substring(0, lastSpace)
                    }
                }
            }
            // In the values map, sets the value of the title
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, titleLocal)
        } else if (titleLocal != null) {
            // In the values map, sets the value of the title
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, titleLocal)
        }

        // This puts the desired notes text into the map.
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text)

        /*
         * Updates the provider with the new values in the map. The ListView is updated
         * automatically. The provider sets this up by setting the notification URI for
         * query Cursor objects to the incoming URI. The content resolver is thus
         * automatically notified when the Cursor for the URI changes, and the UI is
         * updated.
         * Note: This is being done on the UI thread. It will block the thread until the
         * update completes. In a sample app, going against a simple provider based on a
         * local database, the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         */
        contentResolver.update(
            mUri!!,  // The URI for the record to update.
            values,  // The map of column names and new values to apply to them.
            null,  // No selection criteria are used, so no where columns are necessary.
            null // No where columns are used, so no where arguments are necessary.
        )
    }

    /**
     * This helper method cancels the work done on a note.  It deletes the note if it was newly created,
     * or reverts to the original text of the note if it was being edited. We branch on the value of
     * our state `mState`:
     *
     *  *
     * STATE_EDIT: (Put the original note text back into the database) We initialize `ContentValues values`
     * with a new instance, and store `String mOriginalContent` in it under the key NotePad.Notes.COLUMN_NAME_NOTE
     * ("note"). We then request a `ContentResolver` instance for our application's package to
     * update the record in our database with the address `Uri mUri` with `values` using
     * null for both the selection and selection arguments.
     *
     *  *
     * STATE_INSERT: (We inserted an empty note, make sure to delete it) we call our method
     * `deleteNote` to delete the empty note from our data base.
     *
     *
     * We then set our result code to RESULT_CANCELED, and call `finish` to finish this activity.
     */
    private fun cancelNote() {
        if (mState == STATE_EDIT) {
            // Put the original note text back into the database
            val values = ContentValues()
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent)
            contentResolver.update(mUri!!, values, null, null)
        } else if (mState == STATE_INSERT) {
            // We inserted an empty note, make sure to delete it
            deleteNote()
        }
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * Take care of deleting a note. Simply deletes the entry. We request a `ContentResolver`
     * instance for our application's package to to delete from our database the record with the
     * address `Uri mUri`, and set the text of `EditText mText` to the empty string.
     */
    private fun deleteNote() {
        contentResolver.delete(mUri!!, null, null)
        mText!!.setText("")
    }
    // LoaderManager callbacks
    /**
     * Instantiate and return a new Loader for the given ID. We return a new instance of `CursorLoader`
     * that uses `Uri mUri` as the URI for the note that is to be retrieved, PROJECTION as the
     * columns to retrieve, and null for the selection, selection arguments and sort order.
     *
     * @param i      The ID whose loader is to be created.
     * @param bundle Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Deprecated("Deprecated in Java")
    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor?> {
        return CursorLoader(
            this,
            mUri,  // The URI for the note that is to be retrieved.
            PROJECTION,  // The columns to retrieve
            null,  // No selection criteria are used, so no where columns are needed.
            null,  // No where columns are used, so no where values are needed.
            null // No sort order is needed.
        )
    }

    /**
     * Called when a previously created loader has finished its load. If our parameter `Cursor cursor`
     * is not null, and we are able to move it to the first row, and our state `mState` is equal
     * to STATE_EDIT: We initialize `int colTitleIndex` to the column index for the column named
     * COLUMN_NAME_TITLE ("title") in `cursor`, and `int colNoteIndex` to the column index
     * for the column named COLUMN_NAME_NOTE ("note") in `cursor`. We then initialize `String title`
     * with the string at index `colTitleIndex` in `cursor`, initialize `Resources res`
     * to a `Resources` instance for our application's package and use it to format `String text`
     * from `title` using the format with resource id R.string.title_edit ("Edit: %1$s"). We then
     * set the title associated with this activity to `text`. We initialize `String note`
     * with the string at index `colNoteIndex` in `cursor` and use it to set the text to
     * be displayed in `EditText mText` while retaining the cursor position. Finally if
     * `mOriginalContent` is null we set it to `note` (to allow the user to revert changes).
     *
     * @param cursorLoader The Loader that has finished.
     * @param cursor       The data generated by the Loader.
     */
    @Deprecated("Deprecated in Java")
    override fun onLoadFinished(cursorLoader: Loader<Cursor?>, cursor: Cursor?) {

        // Modifies the window title for the Activity according to the current Activity state.
        if (cursor != null && cursor.moveToFirst() && mState == STATE_EDIT) {
            // Set the title of the Activity to include the note title
            val colTitleIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE)
            val colNoteIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE)

            // Gets the title and sets it
            val title = cursor.getString(colTitleIndex)
            val res = resources
            val text = String.format(res.getString(R.string.title_edit), title)
            setTitle(text)

            // Gets the note text from the Cursor and puts it in the TextView, but doesn't change
            // the text cursor's position.
            val note = cursor.getString(colNoteIndex)
            mText!!.setTextKeepState(note)
            // Stores the original note text, to allow the user to revert changes.
            if (mOriginalContent == null) {
                mOriginalContent = note
            }
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * We ignore.
     *
     * @param cursorLoader The Loader that is being reset.
     */
    @Deprecated("Deprecated in Java")
    override fun onLoaderReset(cursorLoader: Loader<Cursor?>) {}

    companion object {
        /**
         * For logging and debugging purposes
         */
        private const val TAG = "NoteEditor"

        /**
         * Creates a projection that returns the note ID, note title, and the note contents.
         */
        private val PROJECTION = arrayOf(
            BaseColumns._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE
        )

        /**
         * Key used for the saved state of the activity in `onSaveInstanceState` (we save the string
         * `String mOriginalContent` in the `Bundle` passed it and retrieve it in `onCreate`
         */
        private const val ORIGINAL_CONTENT = "origContent"
        // This Activity can be started by more than one action. Each action is represented
        // as a "state" constant which is stored in our field mState
        /**
         * Edit state stored in our field `int mState`, entered as the result of receiving an
         * ACTION_EDIT, ACTION_INSERT, or ACTION_PASTE action in the `Intent` that launched us.
         */
        private const val STATE_EDIT = 0

        /**
         * Insert state stored in our field `int mState`, entered as the result of receiving an
         * ACTION_INSERT, or ACTION_PASTE action in the `Intent` that launched us.
         */
        private const val STATE_INSERT = 1

        /**
         * Unique identifier for the loader we start (or reuse if one is already running) in our
         * `onCreate` override (our `LoaderManager` callbacks handle connecting to and
         * querying our provider).
         */
        private const val LOADER_ID = 1
    }
}