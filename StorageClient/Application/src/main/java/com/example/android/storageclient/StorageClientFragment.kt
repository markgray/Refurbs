@file:Suppress(
    "DEPRECATION",
    "ReplaceNotNullAssertionWithElvisReturn",
    "JoinDeclarationAndAssignment",
    "MemberVisibilityCanBePrivate"
)
/*
* Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.storageclient

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.Window
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.android.common.logger.Log
import java.io.FileDescriptor
import java.io.IOException

/**
 * This sample aims to help you understand the OPEN_DOCUMENT intent, which allows a client
 * application to access a list of Document Providers on the devices and choose a file from
 * any of them.
 */
class StorageClientFragment : Fragment() {
    /**
     * Called when the fragment is starting. First we call through to our super's implementation of
     * `onCreate`, then we call the [setHasOptionsMenu] method to report that this fragment has menu
     * items to contribute.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the
     * [MenuItem.getItemId] method (kotlin `itemId` property) of our [MenuItem] parameter [item]
     * returns `R.id.sample_action` we call our method [performFileSearch] to launch the "file
     * chooser" UI and select an image. In any case we return `true` to consume the event here.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    @Deprecated("Deprecated in Java") // TODO: Use MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            performFileSearch()
        }
        return true
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image. First we initialize
     * [Intent] variable `val intent` with an intent whose action is [Intent.ACTION_OPEN_DOCUMENT]
     * (Allow the user to select and return one or more existing documents). Then we add the
     * category [Intent.CATEGORY_OPENABLE] (indicates that the intent only wants URIs that can be
     * opened with [ContentResolver.openFileDescriptor]). We then set the MIME data type to "image/ *"
     * and start the intent for its result using the request code [READ_REQUEST_CODE].
     */
    fun performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        val intent = Intent(/* action = */ Intent.ACTION_OPEN_DOCUMENT)

        // Filter to only show results that can be "opened", such as a file (as opposed to a list
        // of contacts or timezones)
        intent.addCategory(/* category = */ Intent.CATEGORY_OPENABLE)

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers, it would be
        // "*/*".
        intent.type = "image/*"
        startActivityForResult(/* intent = */ intent, /* requestCode = */ READ_REQUEST_CODE)
    }

    /**
     * Receive the result from a previous call to [startActivityForResult]. First we log the fact
     * that we were called, then if our [Int] parameter [requestCode] is [READ_REQUEST_CODE] and our
     * [Int] parameter [resultCode] is [Activity.RESULT_OK] we declare [Uri] variable `val uri` and
     * if our [Intent] parameter [resultData] is not `null` we set `uri` to the value returned by
     * the [Intent.getData] method (kotlin `data` property) of [resultData] (URI of the data this
     * [Intent] is targeting), log its string value, then call our method [showImage] to show the
     * image using a [DialogFragment].
     *
     * @param requestCode The integer request code originally supplied to [startActivityForResult],
     * allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its `setResult`.
     * @param resultData An [Intent], which can return result data to the caller (various data can
     * be attached to [Intent] "extras").
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        Log.i(TAG, "Received an \"Activity Result\"")
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            val uri: Uri?
            if (resultData != null) {
                uri = resultData.data
                Log.i(TAG, "Uri: $uri")
                showImage(uri)
            }
        }
    }

    /**
     * Given the URI of an image, shows it on the screen using a [DialogFragment]. If our [Uri]
     * parameter [uri] is not null we initialize [FragmentManager] variable `val fm` with the
     * [FragmentManager] for interacting with fragments associated with this activity, initialize
     * [ImageDialogFragment] variable `val imageDialog` with a new instance and initialize [Bundle]
     * variable `val fragmentArguments` with a new instance. We add [uri] as a parcelable to
     * `fragmentArguments` under the key "URI" then set the arguments of `imageDialog` to
     * `fragmentArguments`. Finally we call the [DialogFragment.show] method of `imageDialog` to
     * display the dialog, adding the fragment to the [FragmentManager] `fm` using "image_dialog"
     * as the fragment tag.
     *
     * @param uri the [Uri] of the image to display.
     */
    fun showImage(uri: Uri?) {
        if (uri != null) {
            // Since the URI is to an image, create and show a DialogFragment to display the
            // image to the user.
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val imageDialog = ImageDialogFragment()
            val fragmentArguments = Bundle()
            fragmentArguments.putParcelable(/* key = */ "URI", /* value = */ uri)
            imageDialog.arguments = fragmentArguments
            imageDialog.show(/* manager = */ fm, /* tag = */ "image_dialog")
        }
    }

    /**
     * [DialogFragment] which displays an image, given a URI.
     */
    class ImageDialogFragment : DialogFragment() {
        /**
         * [Dialog] we are modifying to be a [DialogFragment].
         */
        private var mDialog: Dialog? = null

        /**
         * The [Uri] we find in our arguments, points to an image to display.
         */
        private var mUri: Uri? = null

        /**
         * Called to do initial creation of a [DialogFragment]. First we call our super's
         * implementation of `onCreate`, then we initialize our [Uri] field [mUri] with the
         * parcelable object stored in our arguments under the key "URI".
         *
         * @param savedInstanceState If the fragment is being re-created from a previous saved
         * state, this is the state.
         */
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mUri = requireArguments().getParcelable(/* key = */ "URI")
        }

        /**
         * Create a [Bitmap] from the image located at our [Uri] parameter [uri] and return it.
         * First we initialize our [ParcelFileDescriptor] variable `var parcelFileDescriptor` to
         * `null`. Then wrapped in a try block intended to catch and log any exceptions, and whose
         * finally block tries to close `parcelFileDescriptor` if it is not `null`, we set
         * `parcelFileDescriptor` to the [ParcelFileDescriptor] that is a raw file descriptor to
         * access data in read only mode from [Uri] parameter [uri] (we do this by calling the
         * [ContentResolver.openFileDescriptor] method of a [ContentResolver] instance for our
         * application's package). We then initialize [FileDescriptor] variable `val fileDescriptor`
         * with the actual [FileDescriptor] associated with `parcelFileDescriptor`. We initialize
         * [Bitmap] variable `val image` to the bitmap that the [BitmapFactory.decodeFileDescriptor]
         * method decodes from the contents of the file referenced by `fileDescriptor`, close
         * `parcelFileDescriptor` and return `image` to the caller.
         *
         * @param uri the [Uri] for the image to return.
         */
        private fun getBitmapFromUri(uri: Uri): Bitmap? {
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            return try {
                parcelFileDescriptor = requireActivity().contentResolver.openFileDescriptor(uri, "r")
                val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
                val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image.", e)
                null
            } finally {
                try {
                    parcelFileDescriptor?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(TAG, "Error closing ParcelFile Descriptor")
                }
            }
        }

        /**
         * Override to build your own custom [Dialog] container. We initialize our [Dialog] field
         * [mDialog] with the [Dialog] returned by our super's constructor, fetch its window and
         * request the feature [Window.FEATURE_NO_TITLE], initialize [ImageView] variable
         * `val imageView` with a new instance and set it to be the content view of [mDialog].
         * We next construct a new [AsyncTask] variable `val imageLoadAsyncTask` whose
         * `doInBackground` override calls our `dumpImageMetaData` method to dump the filename
         * and size of the [Uri] in its zeroth parameter to the log, then returns the [Bitmap]
         * produced by our [getBitmapFromUri] method by reading the file pointed to by that [Uri].
         * Its `onPostExecute` override then sets the image of `imageView` to the bitmap returned
         * by `doInBackground`. We then execute `imageLoadAsyncTask` with our field [mUri] and
         * return [mDialog] to the caller.
         *
         * @param savedInstanceState The last saved instance state of the Fragment,
         * or `null` if this is a freshly created Fragment.
         * @return Return a new [Dialog] instance to be displayed by the Fragment.
         */
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            mDialog = super.onCreateDialog(savedInstanceState)
            // To optimize for the "light-box" style layout.  Since we're not actually displaying a
            // title, remove the bar along the top of the fragment where a dialog title would
            // normally go.
            mDialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            val imageView = ImageView(activity)
            mDialog!!.setContentView(imageView)

            // Loading the image is going to require some sort of I/O, which must occur off the UI
            // thread.  Changing the ImageView to display the image must occur ON the UI thread.
            // The easiest way to divide up this labor is with an AsyncTask.  The doInBackground
            // method will run in a separate thread, but onPostExecute will run in the main
            // UI thread.
            @SuppressLint("StaticFieldLeak")
            val imageLoadAsyncTask: AsyncTask<Uri, Void, Bitmap> =
                object : AsyncTask<Uri, Void, Bitmap>() {
                /**
                 * Override this method to perform a computation on a background thread. The specified
                 * parameters are the parameters passed to [execute] by the caller of this task.
                 * We call our [dumpImageMetaData] method to dump the filename and size of
                 * [Uri] argument `uris[0]` to the log, then return the [Bitmap] produced by our
                 * [getBitmapFromUri] method by reading the file pointed to by that [Uri].
                 *
                 * @param uris The parameters of the task.
                 * @return A result, defined by the subclass of this task.
                 */
                @Suppress("OVERRIDE_DEPRECATION")
                @Deprecated("Deprecated in Java")
                protected override fun doInBackground(vararg uris: Uri): Bitmap? {
                    dumpImageMetaData(uris[0])
                    return getBitmapFromUri(uris[0])
                }

                /**
                 * Runs on the UI thread after [doInBackground]. The parameter is the value
                 * returned by [doInBackground]. We set the image of [ImageView] variable
                 * `imageView` to the [bitmap] returned by [doInBackground] as our parameter.
                 *
                 * @param bitmap The result of the operation computed by [.doInBackground].
                 */
                @Suppress("OVERRIDE_DEPRECATION")
                @Deprecated("Deprecated in Java", ReplaceWith("imageView.setImageBitmap(bitmap)"))
                override fun onPostExecute(bitmap: Bitmap?) {
                    imageView.setImageBitmap(bitmap)
                }
            }
            imageLoadAsyncTask.execute(mUri)
            return mDialog!!
        }

        /**
         * Called when the Fragment is no longer started. We call our super's implementation of
         * `onStop`, then if the method [getDialog] (kotlin `dialog` property) returns a non-`null`
         * value, we call the [Dialog.dismiss] method of that [Dialog].
         */
        override fun onStop() {
            super.onStop()
            if (dialog != null) {
                dialog!!.dismiss()
            }
        }

        /**
         * Grabs metadata for a document specified by [Uri] parameter [uri], logs it to the screen.
         * We use a [ContentResolver] instance for our application's package to query our [Uri]
         * parameter [uri] for all columns saving the result in [Cursor] variable `val cursor`.
         * Then wrapped in a try block whose finally block closes `cursor` if `cursor` is not
         * `null`, and the [Cursor.moveToFirst] method of `cursor` returns `true` we initialize
         * [String] variable `var displayName` with the string in the column
         * [OpenableColumns.DISPLAY_NAME] ("_display_name") of `cursor` and log this. We initialize
         * [Int] variable `val sizeIndex` with the index number for the [OpenableColumns.SIZE]
         * column in `cursor`, and declare [String] variable `val size`. If column `sizeIndex` in
         * `cursor` is not `null` we set `size` to the string in column number `sizeIndex` in
         * `cursor`, otherwise we set it to "Unknown". Then we log the string "Size: " followed
         * by `size`.
         *
         * @param uri The [Uri] for the document whose metadata should be printed.
         */
        fun dumpImageMetaData(uri: Uri?) {

            // The query, since it only applies to a single document, will only return one row.
            // no need to filter, sort, or select fields, since we want all fields for one
            // document.
            val cursor: Cursor? = requireActivity().contentResolver.query(
                /* uri = */ uri!!,
                /* projection = */ null,
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
                /* cancellationSignal = */ null
            )
            @Suppress("ConvertTryFinallyToUseCall")
            try {
                // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursor != null && cursor.moveToFirst()) {
                    val column: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    // Note it's called "Display Name".  This is provider-specific, and
                    // might not necessarily be the file name.
                    var displayName: String? = null
                    if (column >= 0) {
                        displayName = cursor.getString(column)
                    }
                    Log.i(TAG, "Display Name: $displayName")
                    val sizeIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
                    // If the size is unknown, the value stored is null.  But since an int can't be
                    // null in java, the behavior is implementation-specific, which is just a fancy
                    // term for "unpredictable".  So as a rule, check if it's null before assigning
                    // to an int.  This will happen often:  The storage API allows for remote
                    // files, whose size might not be locally known.
                    val size: String
                    size = if (!cursor.isNull(sizeIndex)) {
                        // Technically the column stores an int, but cursor.getString will do the
                        // conversion automatically.
                        cursor.getString(sizeIndex)
                    } else {
                        "Unknown"
                    }
                    Log.i(TAG, "Size: $size")
                }
            } finally {
                cursor?.close()
            }
        }
    }

    companion object {
        /**
         * A request code's purpose is to match the result of a [startActivityForResult] with
         * the type of the original request. Choose any value.
         */
        private const val READ_REQUEST_CODE = 1337

        /**
         * TAG used for logging
         */
        const val TAG: String = "StorageClientFragment"
    }
}
