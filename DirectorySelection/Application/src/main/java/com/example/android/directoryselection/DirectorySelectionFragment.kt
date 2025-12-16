/*
* Copyright 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNUSED_ANONYMOUS_PARAMETER", "MemberVisibilityCanBePrivate")

package com.example.android.directoryselection

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import java.io.FileNotFoundException
import java.lang.AutoCloseable

/**
 * Fragment that demonstrates how to use Directory Selection API.
 */
class DirectorySelectionFragment : Fragment() {
    /**
     * The [Uri] of the current directory.
     */
    var mCurrentDirectoryUri: Uri? = null

    /**
     * [TextView] with id `R.id.textview_current_directory`, used to display the name of the
     * currently selected directory (whose contents are displayed in the [RecyclerView] below it).
     */
    @JvmField
    var mCurrentDirectoryTextView: TextView? = null

    /**
     * [Button] with id `R.id.button_create_directory` used to create an [AlertDialog]
     * when clicked which allows the user to create a new directory.
     */
    @JvmField
    var mCreateDirectoryButton: Button? = null

    /**
     * [RecyclerView] that uses a [DirectoryEntryAdapter] to display the contents of the
     * current directory (files and directories below [Uri] field [mCurrentDirectoryUri]).
     */
    @JvmField
    var mRecyclerView: RecyclerView? = null

    /**
     * Adapter used to fill our [RecyclerView] field [mRecyclerView] with [DirectoryEntry] objects.
     */
    @JvmField
    var mAdapter: DirectoryEntryAdapter? = null

    /**
     * [RecyclerView.LayoutManager] used by our [RecyclerView] - set in [onViewCreated] but
     * never used.
     */
    @JvmField
    var mLayoutManager: RecyclerView.LayoutManager? = null

    /**
     * Called to do initial creation of our fragment. We just call through to our super's
     * implementation of `onCreate`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    @Suppress("RedundantOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We use our [LayoutInflater]
     * parameter [inflater] to inflate our layout file `R.layout.fragment_directory_selection`
     * using our [ViewGroup] parameter [container] for the LayoutParams without attaching to it
     * and return the [View] created to our caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself, but this
     * can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_directory_selection, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. First we call through to our super's implementation of `onViewCreated`,
     * then we locate the view with id `R.id.button_open_directory` ("Open directory") and set its
     * [OnClickListener] to an anonymous class whose [OnClickListener.onClick] override launches an
     * intent for its result with the action [Intent.ACTION_OPEN_DOCUMENT_TREE]. Next we initialize
     * our [TextView] field [mCurrentDirectoryTextView] by finding the view with id
     * `R.id.textview_current_directory`, and our [Button] field [mCreateDirectoryButton] by finding
     * the view with id `R.id.button_create_directory` ("Create Directory"). We then set the
     * [OnClickListener] of [mCreateDirectoryButton] to an anonymous class whose [OnClickListener.onClick]
     * override launches an [AlertDialog] which allows the user to enter the name of a directory that
     * he would like to create and creates it in the current directory [mCurrentDirectoryUri].
     *
     * After this we initialize our [RecyclerView] field [mRecyclerView] by finding the view with
     * id `R.id.recyclerview_directory_entries`, and initialize [RecyclerView.LayoutManager] field
     * [mLayoutManager] by using the [RecyclerView.getLayoutManager] method (aka `layoutManager`
     * property in kotlin) of [mRecyclerView] to retrieve it (we never reference this again). We
     * scroll [mRecyclerView] to position 0, initialize our [DirectoryEntryAdapter] field [mAdapter]
     * with a new instance constructed with a new (empty) instance of [ArrayList] of [DirectoryEntry]
     * and set the adapter of [mRecyclerView] to it.
     *
     * @param rootView The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(rootView, savedInstanceState)
        rootView.findViewById<View>(R.id.button_open_directory)
            .setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                @Suppress("DEPRECATION") // TODO: Replace with registerForActivityResult
                startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
            }
        mCurrentDirectoryTextView = rootView.findViewById(R.id.textview_current_directory)
        mCreateDirectoryButton = rootView.findViewById(R.id.button_create_directory)
        mCreateDirectoryButton!!.setOnClickListener {
            val editView = EditText(activity)
            AlertDialog.Builder(activity)
                .setTitle(R.string.create_directory)
                .setView(editView)
                .setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    try {
                        createDirectory(mCurrentDirectoryUri,
                            editView.text.toString())
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                    updateDirectoryEntries(mCurrentDirectoryUri)
                }
                .setNegativeButton(android.R.string.cancel
                ) { _, _ -> }
                .show()
        }
        mRecyclerView = rootView.findViewById(R.id.recyclerview_directory_entries)
        mLayoutManager = mRecyclerView!!.layoutManager
        mRecyclerView!!.scrollToPosition(0)
        mAdapter = DirectoryEntryAdapter(ArrayList())
        mRecyclerView!!.adapter = mAdapter
    }

    /**
     * Receive the result from a previous call to [startActivityForResult]. First we call our
     * super's implementation of `onActivityResult`. Then if our [Int] parameter [requestCode] is
     * [REQUEST_CODE_OPEN_DIRECTORY], and our [Int] parameter [resultCode] is [Activity.RESULT_OK]
     * we log the string value of the data [Uri] of our [Intent] parameter [data], call our method
     * [updateDirectoryEntries] with that [Uri], and then notify any registered observers of
     * [mAdapter] that the data set has changed which will reload its [RecyclerView] with the new
     * data from the [Uri].
     *
     * @param requestCode The integer request code originally supplied to [startActivityForResult],
     * allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its
     * [Activity.setResult] method.
     * @param data An [Intent], which can return result data to the caller (various data can be
     * attached to [Intent] "extras").
     */
    @SuppressLint("NotifyDataSetChanged")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") // TODO: Replace with registerForActivityResult
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, String.format("Open Directory result Uri : %s", data!!.data))
            updateDirectoryEntries(data.data)
            mAdapter!!.notifyDataSetChanged()
        }
    }

    /**
     * Updates the current directory of the [Uri] parameter [uri] and its children directories, and
     * updates the [mRecyclerView] depending on the contents of the children. First we initialize
     * [ContentResolver] variable `val contentResolver` with a [ContentResolver] instance for our
     * application's package. We initialize [String] variable `val documentId` with the
     * [DocumentsContract.Document.COLUMN_DOCUMENT_ID] ("document_id") column by calling the method
     * [DocumentsContract.getTreeDocumentId] for our [Uri] parameter [uri] (this is something like
     * "primary:Testing/sub1" assuming the user selected the directory "Testing/sub1" from the base
     * directory of the device). We initialize [Uri] variable `val docUri` with a URI representing
     * the target [DocumentsContract.Document.COLUMN_DOCUMENT_ID] in a document provider by calling
     * [DocumentsContract.buildDocumentUriUsingTree] with [uri] and `documentId` producing in our
     * example for "Testing/sub1":
     *
     * content://com.android.externalstorage.documents/tree/primary%3ATesting%2Fsub1/document/primary%3ATesting%2Fsub1
     *
     * We initialize [Uri]  variable `val childrenUri` with a URI representing the children of the
     * target directory by calling [DocumentsContract.buildChildDocumentsUriUsingTree] with [uri]
     * and `documentId` producing in our example for "Testing/sub1":
     *
     * content://com.android.externalstorage.documents/tree/primary%3ATesting%2Fsub1/document/primary%3ATesting%2Fsub1/children
     *
     * We then initialize [Cursor] variable `val docCursor` by using our [ContentResolver] variable
     * `contentResolver` to query the [Uri] variable `docUri` for all entries returning the columns
     * [DocumentsContract.Document.COLUMN_DISPLAY_NAME] and [DocumentsContract.Document.COLUMN_MIME_TYPE].
     * Then wrapped in a try block whose finally block calls our method [closeQuietly] to close
     * `docUri` ignoring any exceptions we loop moving `docCursor` to the next column and after
     * logging the string contents of column 0 (the directory name) and column 1 (the mime type)
     * we set [Uri] field [mCurrentDirectoryUri] to [uri], set the text of [mCurrentDirectoryTextView]
     * to the string in column 0 of `docCursor`, and enable the button [mCreateDirectoryButton].
     *
     * We initialize [Cursor] variable `val childCursor` by using our [ContentResolver] variable
     * `contentResolver` to query the [Uri] variable `childrenUri` for all entries returning the
     * columns [DocumentsContract.Document.COLUMN_DISPLAY_NAME] and [DocumentsContract.Document.COLUMN_MIME_TYPE].
     * Then wrapped in a try block whose finally block calls our method [closeQuietly] to close
     * `childCursor` ignoring any exceptions we first initialize [MutableList] of [DirectoryEntry]
     * variable `val directoryEntries` with a new [ArrayList]. Then we loop moving `childCursor` to
     * the next column and after logging the string contents of column 0 (the directory name) and
     * column 1 (the mime type) we initialize [DirectoryEntry] variable `val entry` with a new
     * instance, set its field [DirectoryEntry.fileName] to the string contents of column 0, and its
     * field [DirectoryEntry.mimeType] to the string contents of column 1 of `childCursor`. We then
     * add `entry` to `directoryEntries` before looping around for the next row.
     *
     * After done with all the entries in `childCursor` we call the
     * [DirectoryEntryAdapter.setDirectoryEntries] method of [DirectoryEntryAdapter] field [mAdapter]
     * to set its data set to `directoryEntries`and call its [DirectoryEntryAdapter.notifyDataSetChanged]
     * method to cause it to reload its [RecyclerView] with the new data.
     *
     * @param uri The [Uri] of the current directory.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateDirectoryEntries(uri: Uri?) {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        val documentId: String = DocumentsContract.getTreeDocumentId(uri)
        val docUri: Uri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
        val childrenUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
        val docCursor: Cursor? = contentResolver.query(
            docUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )
        try {
            while (docCursor!!.moveToNext()) {
                Log.d(TAG, "found doc =" + docCursor.getString(0) + ", mime=" + docCursor
                    .getString(1))
                mCurrentDirectoryUri = uri
                mCurrentDirectoryTextView!!.text = docCursor.getString(0)
                mCreateDirectoryButton!!.isEnabled = true
            }
        } finally {
            closeQuietly(docCursor)
        }
        val childCursor = contentResolver.query(childrenUri, arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
        try {
            val directoryEntries: MutableList<DirectoryEntry> = ArrayList()
            while (childCursor!!.moveToNext()) {
                Log.d(TAG, "found child=" + childCursor.getString(0) + ", mime=" + childCursor
                    .getString(1))
                val entry = DirectoryEntry()
                entry.fileName = childCursor.getString(0)
                entry.mimeType = childCursor.getString(1)
                directoryEntries.add(entry)
            }
            mAdapter!!.setDirectoryEntries(directoryEntries)
            mAdapter!!.notifyDataSetChanged()
        } finally {
            closeQuietly(childCursor)
        }
    }

    /**
     * Creates a directory under the directory represented by our [Uri] parameter [uri]. First we
     * initialize [ContentResolver] variable `val contentResolver` with a [ContentResolver] instance
     * for our application's package. We initialize [String] variable `val documentId` with the
     * [DocumentsContract.Document.COLUMN_DOCUMENT_ID] ("document_id") column by calling
     * [DocumentsContract.getTreeDocumentId] for the [Uri] parameter [uri] (this is something like
     * "primary:Testing/sub1" assuming the user selected the directory "Testing/sub1" from the base
     * directory of the device). Then we initialize [Uri] variable `val docUri` with a URI for the
     * target [DocumentsContract.Document.COLUMN_DOCUMENT_ID] in a document provider by calling
     * [DocumentsContract.buildDocumentUriUsingTree] with [uri] and `documentId`. Next we call the
     * [DocumentsContract.createDocument] method to create a new document with the MIME type
     * [DocumentsContract.Document.MIME_TYPE_DIR] and display name from our [String] parameter
     * [directoryName] inside the directory `docUri` and save the newly created document URI in
     * [Uri] variable `val directoryUri`. If this is not `null` (success!) we log the fact that we
     * created it and toast that fact as well. If it is `null` (failure) we log and toast that fact.
     *
     * @param uri The [Uri] of the directory under which a new directory is created.
     * @param directoryName The directory name of a new directory.
     */
    @Throws(FileNotFoundException::class)
    fun createDirectory(uri: Uri?, directoryName: String?) {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        val documentId: String = DocumentsContract.getTreeDocumentId(/* documentUri = */ uri)
        val docUri: Uri = DocumentsContract.buildDocumentUriUsingTree(
            /* treeUri = */ uri,
            /* documentId = */ documentId
        )
        val directoryUri: Uri? = DocumentsContract.createDocument(
            /* content = */ contentResolver,
            /* parentDocumentUri = */ docUri,
            /* mimeType = */ DocumentsContract.Document.MIME_TYPE_DIR,
            /* displayName = */ directoryName!!
        )
        if (directoryUri != null) {
            Log.i(TAG, String.format(
                "Created directory : %s, Document Uri : %s, Created directory Uri : %s",
                directoryName, docUri, directoryUri)
            )
            Toast.makeText(activity, String.format(
                "Created a directory [%s]",
                directoryName
            ), Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG, String.format(
                "Failed to create a directory : %s, Uri %s",
                directoryName, docUri)
            )
            Toast.makeText(activity, String.format(
                "Failed to create a directory [%s] : ",
                directoryName
            ), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Closes its [AutoCloseable] parameter [closeable]. If [closeable] is not `null`, wrapped in
     * a try block which catches and rethrows [RuntimeException] but ignores all other exceptions,
     * we call the [AutoCloseable.close] method of [closeable].
     *
     * @param closeable [AutoCloseable] to close, a [Cursor] in our case
     */
    fun closeQuietly(closeable: AutoCloseable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (rethrown: RuntimeException) {
                throw rethrown
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        private val TAG = DirectorySelectionFragment::class.java.simpleName

        /**
         * Unique request code we use when launching our ACTION_OPEN_DOCUMENT_TREE intent.
         */
        const val REQUEST_CODE_OPEN_DIRECTORY: Int = 1

        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * @return A new instance of fragment [DirectorySelectionFragment].
         */
        fun newInstance(): DirectorySelectionFragment {
            return DirectorySelectionFragment()
        }
    }
}
