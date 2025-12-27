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
@file:Suppress("UNUSED_PARAMETER", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.pdfrendererbasic

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import androidx.core.graphics.createBitmap

/**
 * This fragment has a big [ImageView] that shows PDF pages, and 2 [Button]s to move between pages.
 * We use a [PdfRenderer] to render PDF pages as [Bitmap]s.
 */
class PdfRendererBasicFragment
/**
 * Our required empty constructor.
 */
    : Fragment(), View.OnClickListener {
    /**
     * File descriptor of the PDF, points to FILENAME in the application cache.
     */
    private var mFileDescriptor: ParcelFileDescriptor? = null

    /**
     * [PdfRenderer] used to render the PDF.
     */
    private var mPdfRenderer: PdfRenderer? = null

    /**
     * [PdfRenderer.Page] that is currently shown on the screen.
     */
    private var mCurrentPage: PdfRenderer.Page? = null

    /**
     * [ImageView] that shows a PDF page as a [Bitmap], its resource id in our layout file is
     * `R.id.image`
     */
    private var mImageView: ImageView? = null

    /**
     * [Button] to move to the previous page, resource id `R.id.previous`
     */
    private var mButtonPrevious: Button? = null

    /**
     * [Button] to move to the next page, resource id `R.id.next`
     */
    private var mButtonNext: Button? = null

    /**
     * PDF page index, used only to restore a saved page number, or to start on page 0.
     */
    private var mPageIndex = 0

    /**
     * Called to have the fragment instantiate its user interface view. We return the [View] that
     * our [LayoutInflater] parameter [inflater] inflates from our layout file
     * `R.layout.fragment_pdf_renderer_basic`, using our [ViewGroup] parameter [container] for the
     * LayoutParams of the view without attaching to it.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            /* resource = */ R.layout.fragment_pdf_renderer_basic,
            /* root = */ container,
            /* attachToRoot = */ false
        )
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. First we call our super's implementation of `onViewCreated` then we
     * initialize our [ImageView] field [mImageView] by finding the view with id `R.id.image`, our
     * [Button] field [mButtonPrevious] by finding the view with id `R.id.previous`, and our
     * [Button] field [mButtonNext] by finding the view with id `R.id.next`. We then set the
     * [View.OnClickListener] of both [mButtonPrevious] and [mButtonNext] to this. We initialize our
     * [Int] field [mPageIndex] to 0, then if our [Bundle] parameter [savedInstanceState] is not
     * `null` we set [Int] field [mPageIndex] to the [Int] stored under the key
     * [STATE_CURRENT_PAGE_INDEX] ("current_page_index") in [savedInstanceState] defaulting to 0.
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Retain view references.
        mImageView = view.findViewById(R.id.image)
        mButtonPrevious = view.findViewById(R.id.previous)
        mButtonNext = view.findViewById(R.id.next)
        // Bind events.
        mButtonPrevious!!.setOnClickListener(this)
        mButtonNext!!.setOnClickListener(this)
        mPageIndex = 0
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0)
        }
    }

    /**
     * Called when the Fragment is visible to the user. First we call our super's implementation of
     * `onStart`. Then wrapped in a try block intended to catch and toast [IOException] to the
     * user we call our method [openRenderer] to set up our [PdfRenderer] to render our pdf file.
     * We then call our method [showPage] to show the page of the PDF at page index [Int] field
     * [mPageIndex] on the screen.
     */
    override fun onStart() {
        super.onStart()
        try {
            openRenderer(activity)
            showPage(mPageIndex)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Error! " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Called when the Fragment is no longer started. Wrapped in a try block intended to catch and
     * log [IOException] we call our method [closeRenderer] to close the current [PdfRenderer.Page]
     * in [mCurrentPage], our [PdfRenderer], and the file descriptor [mFileDescriptor]. We then call
     * our super's implementation of `onStop`.
     */
    override fun onStop() {
        try {
            closeRenderer()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        super.onStop()
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it can later be
     * reconstructed in a new instance if its process is restarted.  If a new instance of
     * the fragment later needs to be created, the data you place in the [Bundle] here will
     * be available in the Bundle given to [onCreate], [onCreateView], and [onActivityCreated].
     *
     * First we call our super's implementation of `onSaveInstanceState`, then if our
     * [PdfRenderer.Page] field [mCurrentPage] is not `null` we add the page index of [mCurrentPage]
     * to our [Bundle] parameter [outState] under the key [STATE_CURRENT_PAGE_INDEX]
     * ("current_page_index").
     *
     * @param outState the [Bundle] in which to place your saved state.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage!!.index)
        }
    }

    /**
     * Sets up a [PdfRenderer] and related resources. First we initialize [File] variable `val file`
     * using the absolute path to the application specific cache directory on the filesystem for the
     * parent path, and [FILENAME] ("sample.pdf") as the child pathname. If this file does not exist
     * (we have not been run before) we initialize [InputStream] variable `val asset` by using an
     * [AssetManager] instance for the application's package to open the asset file named [FILENAME],
     * initialize [FileOutputStream] variable `val output` with a new instance to write to the file
     * represented by [File] variable `file`. We allocate 1024 bytes for [ByteArray] variable
     * `val buffer`, and declare [Int] variable `var size`. We then loop, setting `size` to the
     * number of bytes that the [InputStream.read] method of `asset` reads into `buffer` and writing
     * `size` bytes from `buffer` to `output` until `size` is equal to -1 (end of file). Then we
     * close both `asset` and `output`.
     *
     * Now that we know that [File] variable `file` exists we initialize our [ParcelFileDescriptor]
     * field [mFileDescriptor] with a new instance to access [File] variable `file` in
     * [ParcelFileDescriptor.MODE_READ_ONLY] mode. If [mFileDescriptor] is not `null` we initialize
     * our [PdfRenderer] field [mPdfRenderer] with a new instance constructed to read from
     * [mFileDescriptor].
     *
     * @param context the [Context] to use to access the application specific cache directory, and
     * the [AssetManager] instance for the application's package (in order to read our example PDF
     * from our assets). It is supplied by the [getActivity] method in our `onStart` override.
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun openRenderer(context: Context?) {
        // In this sample, we read a PDF from the assets directory.
        val file = File(requireContext().cacheDir, FILENAME)
        if (!file.exists()) {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            val asset: InputStream = requireContext().assets.open(FILENAME)
            val output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var size: Int
            while (asset.read(buffer).also { size = it } != -1) {
                output.write(buffer, 0, size)
            }
            asset.close()
            output.close()
        }
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        // This is the PdfRenderer we use to render the PDF.
        if (mFileDescriptor != null) {
            mPdfRenderer = PdfRenderer(mFileDescriptor!!)
        }
    }

    /**
     * Closes the [PdfRenderer] and related resources. If our [PdfRenderer.Page] field [mCurrentPage]
     * is not `null` we call its [PdfRenderer.Page.close] method to close the `Page`.
     * We then call the [PdfRenderer.close] method of our [PdfRenderer] field [mPdfRenderer] to close
     * the renderer, and the [ParcelFileDescriptor.close] method of our [ParcelFileDescriptor] field
     * [mFileDescriptor] to close the [ParcelFileDescriptor] as well as the underlying OS resources
     * allocated to represent the stream.
     *
     * @throws [IOException] When the PDF file cannot be closed.
     */
    @Throws(IOException::class)
    private fun closeRenderer() {
        if (null != mCurrentPage) {
            mCurrentPage!!.close()
        }
        mPdfRenderer!!.close()
        mFileDescriptor!!.close()
    }

    /**
     * Shows the specified page of PDF to the screen. If the number of pages in the document is less
     * than or equal to the request page index in [Int] parameter [index] we return having done
     * nothing. If the [PdfRenderer.Page] field [mCurrentPage] is not `null` we call its
     * [PdfRenderer.Page.close] method to close the page. We then set [mCurrentPage] to the `Page`
     * that the [PdfRenderer.openPage] method of our [PdfRenderer] field [mPdfRenderer] opens for
     * rendering for the page index of [Int] parameter [index]. We initialize [Bitmap] variable
     * `val bitmap` with a ARGB_8888 bitmap created for the width and height of [mCurrentPage]. We
     * then use the [PdfRenderer.Page.render] method of [mCurrentPage] to render the page's content
     * for display on a screen into `bitmap`, and set that image as the content of our [ImageView]
     * field [mImageView]. Finally we call our method [updateUi] to enable or disable the control
     * buttons based on the current index, and set the activity's title to reflect the page number.
     *
     * @param index The page index.
     */
    private fun showPage(index: Int) {
        if (mPdfRenderer!!.pageCount <= index) {
            return
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage!!.close()
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer!!.openPage(index)
        // Important: the destination bitmap must be ARGB (not RGB).
        val bitmap = createBitmap(width = mCurrentPage!!.width, height = mCurrentPage!!.height)
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage!!.render(
            /* destination = */ bitmap,
            /* destClip = */ null,
            /* transform = */ null,
            /* renderMode = */ PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )
        // We are ready to show the Bitmap to user.
        mImageView!!.setImageBitmap(bitmap)
        updateUi()
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index, and sets the
     * activity's title to reflect the page number. First we initialize [Int] variable `val index`
     * with the page index of our [PdfRenderer.Page] field [mCurrentPage], and initialize [Int]
     * vqriable `val pageCount` with the number of pages in the document of [PdfRenderer] field
     * [mPdfRenderer]. We enable the [Button] field [mButtonPrevious] if `index` is not 0 and
     * disable it if it is. We enable the [Button] field [mButtonNext] if `index+1` is less than
     * `pageCount` and disable it if it is greater than or equal to `pageCount`. We then set the
     * title associated with this activity to the string formatted using the format string with
     * resource id `R.string.app_name_with_index` to display the value `index+1` (page number) and
     * `pageCount`.
     */
    private fun updateUi() {
        val index: Int = mCurrentPage!!.index
        val pageCount: Int = mPdfRenderer!!.pageCount
        mButtonPrevious!!.isEnabled = 0 != index
        mButtonNext!!.isEnabled = index + 1 < pageCount
        requireActivity().title = getString(
            /* resId = */ R.string.app_name_with_index,
            /* ...formatArgs = */ index + 1, pageCount
        )
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing, and unused.
     *
     * @return The number of pages.
     */
    val pageCount: Int
        get() = mPdfRenderer!!.pageCount

    /**
     * Called when a [View] has been clicked. We `when` switch on the id of our [View] parameter
     * [view]:
     *
     *  * `R.id.previous` "Previous": we call our [showPage] method to display the page that is 1
     *  page before the current page index of our [PdfRenderer.Page] field [mCurrentPage].
     *
     *  * `R.id.next` "Next": we call our [showPage] method to display the page that is 1 page after
     *  the current page index of our [PdfRenderer.Page] field [mCurrentPage]
     *
     * @param view The [View] that was clicked.
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.previous -> {
                // Move to the previous page
                showPage(mCurrentPage!!.index - 1)
            }

            R.id.next -> {
                // Move to the next page
                showPage(mCurrentPage!!.index + 1)
            }
        }
    }

    companion object {
        /**
         * Key string for saving the state of current page index in the bundle passed to our
         * [onSaveInstanceState] override and restored in our [onViewCreated] override.
         */
        private const val STATE_CURRENT_PAGE_INDEX = "current_page_index"

        /**
         * The filename of the PDF, both in our assets and in the cache directory.
         */
        private const val FILENAME = "sample.pdf"
    }
}
