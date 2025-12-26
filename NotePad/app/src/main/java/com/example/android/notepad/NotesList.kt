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
@file:Suppress(
    "DEPRECATION",
    "ReplaceJavaStaticMethodWithKotlinAnalog",
    "JoinDeclarationAndAssignment",
    "ReplaceNotNullAssertionWithElvisReturn",
    "UnusedImport"
)

package com.example.android.notepad

import android.app.ListActivity
import android.app.Activity.DEFAULT_KEYS_SHORTCUT
import android.app.Activity.RESULT_OK
import android.app.LoaderManager
import android.app.LoaderManager.LoaderCallbacks
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.CursorAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView

/**
 * Displays a list of notes. Will display notes from the [Uri] provided in the incoming Intent if
 * there is one, otherwise it defaults to displaying the contents of the [NotePadProvider].
 */
class NotesList : ListActivity(), LoaderCallbacks<Cursor> {
    /**
     * The adapter we use for our [ListView], displays the column [NotePad.Notes.COLUMN_NAME_TITLE]
     * from our [Cursor] in the [TextView] with id [android.R.id.text1] of our item layout file
     * `R.layout.notes_list_item`
     */
    private var mAdapter: SimpleCursorAdapter? = null

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`.
     * We call the [setDefaultKeyMode] method to select the default key handling for this activity
     * to be [DEFAULT_KEYS_SHORTCUT] (so that the user does not need to hold down the menu key to
     * execute menu shortcuts). We initialize [Intent] variable `val intent` with the intent that
     * started this Activity, and if there is no data that this intent is targeting we set the data
     * this intent is operating on to our default [NotePad.Notes.CONTENT_URI]
     * ("content://com.google.provider.NotePad/notes"). We retrieve our [ListView] widget and set
     * its [View.OnCreateContextMenuListener] to `this` (this causes our [onCreateContextMenu]
     * override to be called when a note in our [ListView] is long-clicked). We initialize [Array]
     * of [String] variable `val dataColumns` to the names of the cursor columns to display in the
     * item views (in our case just the title column [NotePad.Notes.COLUMN_NAME_TITLE] "title") and
     * [IntArray] variable `val viewIDs` to the view id that will display the cursor column
     * ([android.R.id.text1] "text1"). Then we initialize our [SimpleCursorAdapter] field [mAdapter]
     * with an instance which will use the layout file `R.layout.notes_list_item`, a `null` cursor
     * (the cursor will be set by [CursorLoader] when loaded), `dataColumns` for the list of column
     * names to use, `viewIDs` for the list of view id's to display each column in, and the flag
     * [CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER] to have the adapter register a content observer
     * on the cursor and call [onContentChanged] when a notification comes in. We then set the adapter
     * of our [ListView] to [mAdapter], and fetch the [LoaderManager] for this activity (creating it
     * if needed) and call its [LoaderManager.initLoader] method to initialize it for loader id
     * [LOADER_ID] (0) specifying 'this' as the [LoaderCallbacks] (our [onCreateLoader],
     * [onLoadFinished], and [onLoaderReset] overrides will be called).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        val intent = intent

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.data == null) {
            intent.data = NotePad.Notes.CONTENT_URI
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        listView.setOnCreateContextMenuListener(this)

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        val dataColumns: Array<String> = arrayOf(NotePad.Notes.COLUMN_NAME_TITLE)

        // The view IDs that will display the cursor columns, initialized to the TextView in
        // notes_list_item.xml
        val viewIDs: IntArray = intArrayOf(android.R.id.text1)

        // Creates the backing adapter for the ListView.
        mAdapter = SimpleCursorAdapter(
            this,  // The Context for the ListView
            R.layout.notes_list_item,  // Points to the XML for a list item
            null,  // The cursor is set by CursorLoader when loaded
            dataColumns,  // list of column names representing the data to bind to the UI
            viewIDs,  // views that should display columns in dataColumns
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER // adapter will register a content observer on the cursor
        )

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        listAdapter = mAdapter
        // Initialize the LoaderManager and start the query
        loaderManager.initLoader(LOADER_ID, null, this)
    }

    /**
     * Called when the user clicks the device's Menu button the first time for this Activity.
     * Android passes in a [Menu] object that is populated with items. We set up a menu that
     * provides the `Insert` option plus a list of alternative actions for this Activity. Other
     * applications that want to handle notes can "register" themselves in Android by providing an
     * intent filter that includes the category ALTERNATIVE and the mimeTYpe
     * [NotePad.Notes.CONTENT_TYPE]. If they do this, the code in [onCreateOptionsMenu] will add the
     * Activity that contains the intent filter to its list of options. In effect, the menu will
     * offer the user other applications that can handle notes.
     *
     * We initialize [MenuInflater] variable `val inflater` with a [MenuInflater] for this context
     * and use it to inflate our menu layout file `R.menu.list_options_menu` into our [Menu]
     * parameter [menu]. We initialize [Intent] variable `val intent` with a new instance with a
     * `null` action and a data uri consisting of the data contained in the [Intent.getData] (kotlin
     * `data` property) data uri of the intent that started this activity. We then add the category
     * [Intent.CATEGORY_ALTERNATIVE] to `intent` (the activity should be considered as an alternative
     * action to the data the user is currently viewing). Then we add to [Menu] parameter [menu] a
     * group of menu items corresponding to actions that can be performed with a group identifier of
     * [Intent.CATEGORY_ALTERNATIVE], an unique item ID of 0, with NONE for the order of the items,
     * using the component name for the class [NotesList], with `null` for the specific items to
     * place first, with [Intent] variable `intent` as the [Intent] describing the kinds of items
     * to populate in the list, 0 for the additional options, and `null` for the optional array in
     * which to place the menu items. Finally we return the value returned by our super's
     * implementation of `onCreateOptionsMenu` to the caller.
     *
     * @param menu A [Menu] object, to which menu items should be added.
     * @return `true` always. The menu should be displayed.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate menu from XML resource
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.list_options_menu, menu)

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        val intent = Intent(/* action = */ null, /* uri = */ intent.data)
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
        menu.addIntentOptions(
            /* groupId = */ Menu.CATEGORY_ALTERNATIVE,  // group identifier
            /* itemId = */ 0,  // Unique item ID
            /* order = */ 0,  // order for the items NONE
            /* caller = */ ComponentName(this, NotesList::class.java),  // current activity component name
            /* specifics = */ null,  // Specific items to place first
            /* intent = */ intent,  // Intent describing the kinds of items to populate in the list
            /* flags = */ 0,  // Additional options
            /* outSpecificItems = */ null // Optional array in which to place the menu items
        )
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Prepare the Screen's standard options menu to be displayed. First we call our super's
     * implementation of `onPrepareOptionsMenu`. We initialize [ClipboardManager] variable
     * `val clipboard` with the system level service [CLIPBOARD_SERVICE], and initialize [MenuItem]
     * variable `val mPasteItem` by finding the item in our [Menu] parameter [menu] with the id
     * `R.id.menu_paste` ("Paste"). If `clipboard` currently contains an item in its primary clip
     * we enable `mPasteItem`, if not then we disable it instead. We initialize [Boolean] variable
     * `val haveItems` to `true` if our [ListAdapter] has more than 0 items in its data set, then
     * branch on the value of `haveItems`:
     *
     *  * `true`: (there are items in the list, which implies that one of them is selected so we
     * need to generate the actions that can be performed on the current selection) We initialize
     * [Uri] variable `val uri` by appending the cursor row ID of the currently selected list item
     * to the data [Uri] of the [Intent] that launched us. We allocate one entry to initialize
     * [Array] of [Intent] `val specifics` (this will be used to send an [Intent] based on the
     * selected menu item). We then initialize `specifics[0]` with an [Intent] whose action is
     * [Intent.ACTION_EDIT], and whose data URI is `uri` (the selected note). We allocate one entry
     * to initialize [Array] of [MenuItem] variable `val items`, and initialize [Intent] variable
     * `val intent` with an [Intent] with no specific action (`null`), using the [Uri] of the
     * selected note (`uri`) as the data URI. We add the category [Intent.CATEGORY_ALTERNATIVE] to
     * `intent` (prepares the Intent as a place to group alternative options in the menu). We then
     * add the alternatives to the menu using the [Menu.addIntentOptions] method of [Menu] parameter
     * [menu] with [Menu.CATEGORY_ALTERNATIVE] as the alternatives group, [Menu.NONE] as the unique
     * item ID, [Menu.NONE] as the ordering, `null` for the caller [ComponentName], `specifics` to
     * place that option first, `intent` to describe the kinds of items to populate in the `specifics`
     * options, [Menu.NONE] for the flags, and `items` as the array to place the menu items generated
     * from the specifics to [Intent] variable `intent` mappings. If `items[0]` is not `null` after
     * this we set its item shortcut to "1" for a numeric keyboard, or "e" for a keyboard with
     * alphabetic keys.
     *
     *  * `false`: (list is empty) We remove all items in the `menu` in the group
     *  [Menu.CATEGORY_ALTERNATIVE].
     *
     * Finally we return `true` to display the menu.
     *
     * @param menu The options menu as last shown or first initialized by [onCreateOptionsMenu].
     * @return You must return true for the menu to be displayed
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        // The paste menu item is enabled if there is data on the clipboard.
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val mPasteItem: MenuItem = menu.findItem(R.id.menu_paste)

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.isEnabled = true
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.isEnabled = false
        }

        // Gets the number of notes currently being displayed.
        val haveItems = listAdapter.count > 0

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            val uri: Uri = ContentUris.withAppendedId(intent.data!!, selectedItemId)

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            val specifics: Array<Intent?> = arrayOfNulls(1)

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = Intent(Intent.ACTION_EDIT, uri)

            // Creates an array of menu items with one element. This will contain the EDIT option.
            val items: Array<MenuItem?> = arrayOfNulls(1)

            // Creates an Intent with no specific action, using the URI of the selected note.
            val intent = Intent(null, uri)

            /*
             * Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE)

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                /* groupId = */ Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                /* itemId = */ Menu.NONE,  // A unique item ID is not required.
                /* order = */ Menu.NONE,  // The alternatives don't need to be in order.
                /* caller = */ null,  // The caller's name is not excluded from the group.
                /* specifics = */ specifics,  // These specific options must appear first.
                /* intent = */ intent,  // These Intent objects map to the options in specifics.
                /* flags = */ Menu.NONE,  // No flags are required.
                /* outSpecificItems = */ items // The menu items generated from the specifics-to-Intents mapping
            )
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {

                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0]!!.setShortcut('1', 'e')
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE)
        }

        // Displays the menu
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We branch on the
     * value returned by the [MenuItem.getItemId] method of our [MenuItem] parameter [item]
     * (kotlin `itemId` property), its item Id:
     *
     *  * `R.id.menu_add` "New note": We launch an [Intent] that has an action of [Intent.ACTION_INSERT],
     *  and whose data URI is the data URI of the [Intent] that launched us, and return `true` to consume
     *  the event here (this starts the [NoteEditor] Activity in [NotePad]).
     *
     *  * `R.id.menu_paste` "Paste": We launch an [Intent] that has an action of [Intent.ACTION_PASTE],
     *  and whose data URI is the data URI of the [Intent] that launched us, and return `true` to consume
     *  the event here (this starts the [NoteEditor] Activity in [NotePad]).
     *
     *  * `else`: we return the value returned by our super's implementation of `onOptionsItemSelected`
     *  to the caller.
     *
     * @param item The menu item that was selected by the user
     * @return `true` if the "New note" or "Paste" menu items were selected; otherwise, the result
     * of calling the parent method.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add -> {
                /**
                 * Launches a new Activity using an Intent. The intent filter for the Activity
                 * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
                 * In effect, this starts the NoteEditor Activity in NotePad.
                 */
                startActivity(Intent(Intent.ACTION_INSERT, intent.data))
                true
            }

            R.id.menu_paste -> {
                /**
                 * Launches a new Activity using an Intent. The intent filter for the Activity
                 * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
                 * In effect, this starts the NoteEditor Activity in NotePad.
                 */
                startActivity(Intent(Intent.ACTION_PASTE, intent.data))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This method is called when the user context-clicks a note in the list. [NotesList] registers
     * itself as the handler for context menus in its [ListView] (this is done in [onCreate]).
     * The only available options are COPY and DELETE. Context-click is equivalent to long-press.
     *
     * First we declare [AdapterContextMenuInfo] variable `val info`, then wrapped in try block
     * intended to catch and log [ClassCastException] we cast our [ContextMenuInfo] parameter
     * [menuInfo] to set `info`. We then initialize [Cursor] variable `val cursor` with the item
     * in our list adapter whose position is given by the [AdapterContextMenuInfo.position] field
     * of `info` (this is the position in the adapter for which the context menu is being displayed).
     * If `cursor` is `null` we return (for some reason the requested item isn't available, so we do
     * nothing). We initialize [MenuInflater] variable `val inflater` with a [MenuInflater] for this
     * [Context] and use it to inflate our context menu layout file `R.menu.list_context_menu` into
     * our [ContextMenu] parameter [menu]. We set the context menu header's title to the string
     * stored in the [COLUMN_INDEX_TITLE] column of `cursor` (title of the selected note). We
     * initialize [Intent] variable `val intent` with an instance whose action is `null`, and whose
     * data URI is constructed by appending the string value of the [AdapterContextMenuInfo.id]
     * field of `info` (row id of the item for which the context menu is being displayed) to the
     * data URI of the [Intent] that launched this activity. We then add the category
     * [Intent.CATEGORY_ALTERNATIVE] to `intent`. Finally we add to [ContextMenu] parameter [menu]
     * a group of menu items with the group identifier [Menu.CATEGORY_ALTERNATIVE], 0 (NONE) for the
     * Unique item ID, 0 (NONE) for the order for the items, the component name created from the
     * class of [NotesList] as the current activity component name, `null` for the specific
     * items to place first, `intent` as the `Intent` describing the kinds of items to populate
     * in the list, 0 for the flags, and `null` for the optional array in which to place the menu
     * items generated.
     *
     * @param menu A [ContextMenu] object to which items should be added.
     * @param view The [View] for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     */
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {

        // The data from the menu item.
        val info: AdapterContextMenuInfo

        // Tries to get the position of the item in the ListView that was long-pressed.
        info = try {
            // Casts the incoming data object into the type for AdapterView objects.
            menuInfo as AdapterContextMenuInfo
        } catch (e: ClassCastException) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e)
            return
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        val cursor = (listAdapter.getItem(info.position) as Cursor?) ?: return

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.

        // Inflate menu from XML resource
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.list_context_menu, menu)

        // Sets the menu header to be the c.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE))

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        val intent = Intent(
            /* action = */ null,
            /* uri = */ Uri.withAppendedPath(intent.data, Integer.toString(info.id.toInt()))
        )
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
        menu.addIntentOptions(
            /* groupId = */ Menu.CATEGORY_ALTERNATIVE,  // group identifier
            /* itemId = */ 0,  // Unique item ID NONE
            /* order = */ 0,  // order for the items NONE
            /* caller = */ ComponentName(this, NotesList::class.java),  // current activity component name
            /* specifics = */ null,  // specific items to place first
            /* intent = */ intent,  // Intent describing the kinds of items to populate in the list
            /* flags = */ 0,  // Additional options controlling how the items are added
            /* outSpecificItems = */ null  // Optional array in which to place the menu items generated
        )
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see [onCreateContextMenu]). The only menu items that are actually handled are "DELETE" and
     * "COPY". Anything else is an alternative option, for which default handling should be done.
     *
     * First we declare [AdapterContextMenuInfo] variable `val info`, then wrapped in try block
     * intended to catch log [ClassCastException] and return `false`, we cast the [ContextMenuInfo]
     * value returned by the [MenuItem.getMenuInfo] method (kotlin `menuInfo` property) of our
     * [MenuItem] parameter [item] to [AdapterContextMenuInfo] to set `info`. We initialize [Uri]
     * variable `val noteUri` by appending the [AdapterContextMenuInfo.id] field of `info` (row id
     * of the item for which the context menu is being displayed) to the data URI that launched this
     * activity. We then `when` switch on the item id of our [MenuItem] parameter [item]:
     *
     *  * `R.id.context_open` "Open": We start an activity with an [Intent] that has the action
     *  [Intent.ACTION_EDIT] and whose data URI is `noteUri` (launches an activity to view/edit the
     *  currently selected item), then return `true` to consume the event here.
     *
     *  * `R.id.context_copy` "Copy": We initialize [ClipboardManager] variable `val clipboard` with
     *  a handle to the system level service [CLIPBOARD_SERVICE], and set the current primary clip
     *  on the clipboard to a [ClipData] created by having a [ContentResolver] instance for our
     *  application's package copy the note addressed by `noteUri` to the clip board using the
     *  User-visible label "Note" for the clip data, then return `true` to consume the event here.
     *
     *  * `R.id.context_delete` "Delete": We use a [ContentResolver] instance for our application's
     *  package to delete the note with the URI `noteUri`, then return `true` to consume the event
     *  here.
     *
     *  * `else`: we return the value returned by our super's implementation of `onContextItemSelected`
     *  to the caller.
     *
     * @param item The selected menu item
     * @return `true` if the menu item was DELETE, and no default processing is need, otherwise
     * `false`, which triggers the default handling of the item.
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // The data from the menu item.
        val info: AdapterContextMenuInfo?

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        info = try {
            // Casts the data object in the item into the type for AdapterView objects.
            item.menuInfo as AdapterContextMenuInfo?
        } catch (e: ClassCastException) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e)

            // Triggers default processing of the menu item.
            return false
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        val noteUri = ContentUris.withAppendedId(intent.data!!, info!!.id)
        return when (item.itemId) {
            R.id.context_open -> {
                // Launch activity to view/edit the currently selected item
                startActivity(Intent(Intent.ACTION_EDIT, noteUri))
                true
            }

            R.id.context_copy -> {
                // Gets a handle to the clipboard service.
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                // Copies the notes URI to the clipboard. In effect, this copies the note itself
                clipboard.setPrimaryClip(
                    ClipData.newUri(
                        /* resolver = */ contentResolver,  // resolver to retrieve URI info
                        /* label = */ "Note",  // label for the clip
                        /* uri = */ noteUri // the URI
                    )  // new clipboard item holding a URI
                )

                // Returns to the caller and skips further processing.
                true
            }

            R.id.context_delete -> {

                // Deletes the note from the provider by passing in a URI in note ID format.
                // Please see the introductory note about performing provider operations on the
                // UI thread.
                contentResolver.delete(
                    noteUri,  // The URI of the provider
                    null,  // No where clause is needed, since only a single note ID is being passed in.
                    null // No where clause is used, so no where arguments are needed.
                )

                // Returns to the caller and skips further processing.
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    /**
     * This method is called when the user clicks a note in the displayed list. This method handles
     * incoming actions of either PICK (get data from the provider) or GET_CONTENT (get or create
     * data). If the incoming action is EDIT, this method sends a new Intent to start NoteEditor.
     *
     * First we initialize [Uri] variable `val uri` by appending the row id of the clicked item,
     * our [Long] parameter [id], to the data URI of the [Intent] that launched us, and initialize
     * [String] variable `val action` with the action of that [Intent]. If `action` is equal to
     * [Intent.ACTION_PICK] or to [Intent.ACTION_PICK] we set the result that our activity will
     * return to its caller to a new instance of [Intent] with the data URI of `uri` and result
     * code [RESULT_OK]. Otherwise we launch an [Intent] with the action code [Intent.ACTION_EDIT]
     * and the data URI `uri`.
     *
     * @param l The [ListView] that contains the clicked item
     * @param v The [View] of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Deprecated("Deprecated in Java")
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {

        // Constructs a new URI from the incoming URI and the row ID
        val uri: Uri = ContentUris.withAppendedId(intent.data!!, id)

        // Gets the action from the incoming Intent
        val action: String? = intent.action

        // Handles requests for note data
        if (Intent.ACTION_PICK == action || Intent.ACTION_GET_CONTENT == action) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, Intent().setData(uri))
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(Intent(Intent.ACTION_EDIT, uri))
        }
    }

    // LoaderManager callbacks

    /**
     * Instantiate and return a new [Loader] for the given ID. We return a new instance of
     * [CursorLoader] using the data URI of the [Intent] that launched us as the URI for the
     * content to retrieve, [PROJECTION] for the list of which columns to return, `null` for
     * the SQL WHERE clause selection, `null` for the selection arguments, and
     * [NotePad.Notes.DEFAULT_SORT_ORDER] for the sort order.
     *
     * @param i The ID whose loader is to be created.
     * @param bundle Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Deprecated("Deprecated in Java")
    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(
            /* context = */ this,
            /* uri = */ intent.data,  // Use the default content URI for the provider.
            /* projection = */ PROJECTION,  // Return the note ID and title for each note.
            /* selection = */ null,  // No where clause, return all records.
            /* selectionArgs = */ null,  // No where clause, therefore no where column values.
            /* sortOrder = */ NotePad.Notes.DEFAULT_SORT_ORDER // Use the default sort order.
        )
    }

    /**
     * Called when a previously created loader has finished its load. We call the
     * [SimpleCursorAdapter.changeCursor] method of our [SimpleCursorAdapter] field
     * [mAdapter] to change its underlying cursor to our [Cursor] parameter [cursor].
     *
     * @param cursorLoader The [Loader] that has finished.
     * @param cursor       The data generated by the Loader.
     */
    @Deprecated("Deprecated in Java")
    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        mAdapter!!.changeCursor(cursor)
    }

    /**
     * Called when a previously created loader is being reset, thus making its data unavailable.
     * We call the [SimpleCursorAdapter.changeCursor] method of our [SimpleCursorAdapter] field
     * [mAdapter] to change its cursor to `null` (this removes the cursor reference from the
     * adapter).
     *
     * @param cursorLoader The Loader that is being reset.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
        // Since the Loader is reset, this removes the cursor reference from the adapter.
        mAdapter!!.changeCursor(null)
    }

    companion object {
        /**
         * TAG used for logging and debugging
         */
        private const val TAG = "NotesList"

        /**
         * Unique identifier for the loader we use
         */
        private const val LOADER_ID = 0

        /**
         * The columns needed by the cursor adapter
         */
        private val PROJECTION: Array<String> = arrayOf(
            BaseColumns._ID,  // 0
            NotePad.Notes.COLUMN_NAME_TITLE
        )

        /**
         * The index of the title column, needs to match the location in PROJECTION
         */
        private const val COLUMN_INDEX_TITLE = 1
    }
}
