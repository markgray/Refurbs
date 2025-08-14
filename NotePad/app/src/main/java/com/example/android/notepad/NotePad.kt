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
package com.example.android.notepad

import android.net.Uri
import android.provider.BaseColumns
import androidx.core.net.toUri

/**
 * Defines a contract between the Note Pad content provider and its clients. A contract defines the
 * information that a client needs to access the provider as one or more data tables. A contract
 * is a public, non-extendable (final) class that contains constants defining column names and
 * URIs. A well-written client depends only on the constants in the contract.
 */
object NotePad {
    /**
     * Authority - symbolic name of the entire provider
     */
    const val AUTHORITY: String = "com.google.provider.NotePad"

    /**
     * Notes table contract
     */
    object Notes : BaseColumns {
        /**
         * The table name offered by this provider
         */
        const val TABLE_NAME: String = "notes"
        /*
         * URI definitions
         */
        /**
         * The scheme part for this provider's URI
         */
        private const val SCHEME = "content://"
        /*
         * Path parts for the URIs
         */
        /**
         * Path part for the Notes URI
         */
        private const val PATH_NOTES = "/notes"

        /**
         * Path part for the Note ID URI
         */
        private const val PATH_NOTE_ID = "/notes/"

        /**
         * 0-relative position of a note ID segment in the path part of a note ID URI
         */
        const val NOTE_ID_PATH_POSITION: Int = 1

        /**
         * Path part for the Live Folder URI
         */
        private const val PATH_LIVE_FOLDER = "/live_folders/notes"

        /**
         * The content:// style URL for this table
         * content://com.google.provider.NotePad/notes
         */
        val CONTENT_URI: Uri = (SCHEME + AUTHORITY + PATH_NOTES).toUri()

        /**
         * The content URI base for a single note. Callers must
         * append a numeric note id to this Uri to retrieve a note
         * content://com.google.provider.NotePad/notes/
         */
        val CONTENT_ID_URI_BASE: Uri = (SCHEME + AUTHORITY + PATH_NOTE_ID).toUri()

        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         * content://com.google.provider.NotePad/notes/#
         */
        @Suppress("unused")
        val CONTENT_ID_URI_PATTERN: Uri = "$SCHEME$AUTHORITY$PATH_NOTE_ID/#".toUri()

        /**
         * The content Uri pattern for a notes listing for live folders, used only for tests
         * content://com.google.provider.NotePad/live_folders/notes
         */
        @Suppress("unused")
        val LIVE_FOLDER_URI: Uri = (SCHEME + AUTHORITY + PATH_LIVE_FOLDER).toUri()
        /*
         * MIME type definitions
         */
        /**
         * The MIME type of [CONTENT_URI] providing a directory of notes.
         */
        const val CONTENT_TYPE: String = "vnd.android.cursor.dir/vnd.google.note"

        /**
         * The MIME type of a [CONTENT_URI] sub-directory of a single
         * note.
         */
        const val CONTENT_ITEM_TYPE: String = "vnd.android.cursor.item/vnd.google.note"

        /**
         * The default sort order for this table, descending on the "modified"
         */
        const val DEFAULT_SORT_ORDER: String = "modified DESC"
        /*
         * Column definitions
         */
        /**
         * Column name for the title of the note
         * <P>Type: TEXT</P>
         */
        const val COLUMN_NAME_TITLE: String = "title"

        /**
         * Column name of the note content
         * <P>Type: TEXT</P>
         */
        const val COLUMN_NAME_NOTE: String = "note"

        /**
         * Column name for the creation timestamp
         * <P>Type: INTEGER (long from System.currentTimeMillis())</P>
         */
        const val COLUMN_NAME_CREATE_DATE: String = "created"

        /**
         * Column name for the modification timestamp
         * <P>Type: INTEGER (long from System.currentTimeMillis())</P>
         */
        const val COLUMN_NAME_MODIFICATION_DATE: String = "modified"
    }
}