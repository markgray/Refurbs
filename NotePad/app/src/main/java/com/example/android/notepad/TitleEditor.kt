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
@file:Suppress("UNUSED_PARAMETER", "unused", "ReplaceNotNullAssertionWithElvisReturn",
    "RedundantSuppression"
)

package com.example.android.notepad

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.Toast

/**
 * This Activity allows the user to edit a note's title. It displays a floating window
 * containing an EditText.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the [android.content.AsyncQueryHandler]
 * or [android.os.AsyncTask] object to perform operations asynchronously on a separate thread.
 */
class TitleEditor : Activity() {
    /**
     * An [EditText] object for preserving the edited title.
     */
    private var mText: EditText? = null

    /**
     * A [Uri] object for the note whose title is being edited.
     */
    private var mUri: Uri? = null

    /**
     * The title that has been saved to the database (see [saveTitle]).
     */
    private var mSavedTitle: String? = null

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * [Intent], it determines what kind of editing is desired, and then does it. First we call our
     * super's implementation of `onCreate`, then we set our content view to our layout file
     * `R.layout.title_editor`. We initialize [EditText] field [mText] by finding the view with id
     * `R.id.title`, then initialize our [Uri] field [mUri] to the data URI of the [Intent]
     * that launched us. We initialize [Cursor] variable `val cursor` to the [Cursor] returned from the
     * [ContentResolver.query] method of a [ContentResolver] instance for our application's package
     * when retrieving the note whose URI is [mUri], retrieving all the columns in [PROJECTION], with
     * `null` for the selection, selection arguments and sort order. If cursor is not `null` we call
     * its [Cursor.moveToFirst] method to move it to the first record, and set the text of [mText]
     * to the string in `cursor` contained in its [COLUMN_INDEX_TITLE] column index. Finally we
     * close `cursor`.
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the View for this Activity object's UI.
        setContentView(R.layout.title_editor)

        // Gets the View ID for the EditText box
        mText = findViewById(R.id.title)

        // Get the Intent that activated this Activity, and from it get the URI of the note whose
        // title we need to edit.
        mUri = intent.data

        /*
         * Using the URI passed in with the triggering Intent, gets the note.
         *
         * Note: This is being done on the UI thread. It will block the thread until the query
         * completes. In a sample app, going against a simple provider based on a local database,
         * the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         */
        val cursor: Cursor? = contentResolver.query(
            /* uri = */ mUri!!,  // The URI for the note that is to be retrieved.
            /* projection = */ PROJECTION,  // The columns to retrieve
            /* selection = */ null,  // No selection criteria are used, so no where columns are needed.
            /* selectionArgs = */ null,  // No where columns are used, so no where values are needed.
            /* sortOrder = */ null // No sort order is needed.
        )
        if (cursor != null) {

            // The Cursor was just retrieved, so its index is set to one record *before* the first
            // record retrieved. This moves it to the first record.
            cursor.moveToFirst()

            // Displays the current title text in the EditText object.
            mText!!.setText(cursor.getString(COLUMN_INDEX_TITLE))
            cursor.close()
        }
    }

    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     * We just call our super's implementation of `onResume`.
     */
    @Suppress("RedundantOverride", "RedundantSuppression")
    override fun onResume() {
        super.onResume()
    }

    /**
     * This method is called when the Activity loses focus. While there is no need to override this
     * method in this app, it is shown here to highlight that we are not saving any state in
     * [onPause], but have moved app state saving to the [onStop] callback. In earlier versions of
     * this app and popular literature it had been shown that [onPause] is a good place to persist
     * any unsaved work, however, this is not really a good practice because of how application and
     * process lifecycle behave. As a general guideline apps should have a way of saving their
     * business logic that does not solely rely on Activity (or other component) life cycle state
     * transitions. As a backstop you should save any app state, not saved during lifetime of the
     * Activity, in [onStop]. For a more detailed explanation of this recommendation please read:
     * [Processes and Application Life Cycle ](https://developer.android.com/guide/topics/processes/process-lifecycle.html).
     * [Pausing and Resuming an Activity ](https://developer.android.com/training/basics/activity-lifecycle/pausing.html).
     *
     * We just call our super's implementation of `onPause`.
     */
    @Suppress("RedundantOverride", "RedundantSuppression")
    override fun onPause() {
        super.onPause()
    }

    /**
     * This method is called when the Activity becomes invisible. For Activity objects that edit
     * information, [onStop] may be the one place where changes are saved. First we call our
     * super's implementation of `onStop()`, then we call our method [saveTitle] to update the
     * note with the text currently in the text box.
     */
    override fun onStop() {
        super.onStop()
        saveTitle()
    }

    /**
     * Specified as its [OnClickListener] by a android:onClick="onClickOk" attribute for the
     * button in our layout with id android:id="@+id/ok" ("OK"). We call our method [saveTitle]
     * to update the note with the text currently in the text box, then call [finish] to end
     * this activity.
     *
     * @param v the [View] that was clicked.
     */
    fun onClickOk(v: View?) {
        saveTitle()
        finish()
    }

    /**
     * Saves the title if required. If our [EditText] field [mText] has some text in it, we
     * initialize [String] variable `val newTitle` with it. If the string in `newTitle` does not
     * equal the string in our [String] field [mSavedTitle] we need to update the title in the
     * database:
     *
     *  * We initialize [ContentValues] variable `val values` with a new instance and insert
     *  `newTitle` in it under the key [NotePad.Notes.COLUMN_NAME_TITLE] ("title").
     *
     *  * We fetch a [ContentResolver] for our package and call its [ContentResolver.update] method
     *  to update the note whose [Uri] is our [Uri] field [mUri], with `values` as the values map
     *  containing the columns to update and the values to use, with `null` for both the selection
     *  and selection arguments.
     *
     *  * We then set our field `mSavedTitle` to `newTitle`.
     *
     * If there is no text in [mText] we just toast the string with resource id `R.string.title_blank`
     * ("Blank title not saved").
     */
    private fun saveTitle() {
        if (!TextUtils.isEmpty(mText!!.text)) {
            val newTitle = mText!!.text.toString()
            if (newTitle != mSavedTitle) {
                // Creates a values map for updating the provider.
                val values = ContentValues()

                // In the values map, sets the title to the current contents of the edit box.
                values.put(NotePad.Notes.COLUMN_NAME_TITLE, newTitle)

                /*
                 * Updates the provider with the note's new title.
                 *
                 * Note: This is being done on the UI thread. It will block the thread until the
                 * update completes. In a sample app, going against a simple provider based on a
                 * local database, the block will be momentary, but in a real app you should use
                 * android.content.AsyncQueryHandler or android.os.AsyncTask.
                 */
                contentResolver.update(
                    /* uri = */ mUri!!,  // The URI for the note to update.
                    /* values = */ values,  // The values map containing the columns to update and the values to use.
                    /* where = */ null,  // No selection criteria is used, so no "where" columns are needed.
                    /* selectionArgs = */ null // No "where" columns are used, so no "where" values are needed.
                )
                mSavedTitle = newTitle
            }
        } else {
            Toast.makeText(this, R.string.title_blank, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        /**
         * This is a special intent action that means "edit the title of a note".
         * it is used in AndroidManifest for an action name of our intent-filter
         */
        const val EDIT_TITLE_ACTION: String = "com.android.notepad.action.EDIT_TITLE"

        /**
         * Creates a projection that returns the note ID and the note contents.
         */
        private val PROJECTION: Array<String> = arrayOf(
            BaseColumns._ID,  // 0
            NotePad.Notes.COLUMN_NAME_TITLE
        )

        /**
         * The position of the title column in a Cursor returned by the provider.
         */
        private const val COLUMN_INDEX_TITLE: Int = 1
    }
}
