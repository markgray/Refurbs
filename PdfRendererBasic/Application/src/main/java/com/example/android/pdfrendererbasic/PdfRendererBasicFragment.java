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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This fragment has a big {@code ImageView} that shows PDF pages, and 2
 * {@link android.widget.Button}s to move between pages. We use a
 * {@link android.graphics.pdf.PdfRenderer} to render PDF pages as
 * {@link android.graphics.Bitmap}s.
 */
public class PdfRendererBasicFragment extends Fragment implements View.OnClickListener {

    /**
     * Key string for saving the state of current page index in the bundle passed to our
     * {@code onSaveInstanceState} override and restored in our {@code onViewCreated} override.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    /**
     * The filename of the PDF, both in our assets and in the cache directory.
     */
    private static final String FILENAME = "sample.pdf";

    /**
     * File descriptor of the PDF, points to FILENAME in the application cache.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link android.graphics.pdf.PdfRenderer} used to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * {@code Page} that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap},
     * its resource id in our layout file is R.id.image
     */
    private ImageView mImageView;

    /**
     * {@link android.widget.Button} to move to the previous page, resource id R.id.previous
     */
    private Button mButtonPrevious;

    /**
     * {@link android.widget.Button} to move to the next page, resource id R.id.next
     */
    private Button mButtonNext;

    /**
     * PDF page index, used only to restore a saved page number, or to start on page 0.
     */
    private int mPageIndex;

    /**
     * Our required empty constructor.
     */
    public PdfRendererBasicFragment() {
    }

    /**
     * Called to have the fragment instantiate its user interface view. We return the {@code View}
     * that our parameter {@code LayoutInflater inflater} inflates from our layout file
     * R.layout.fragment_pdf_renderer_basic, using our parameter {@code ViewGroup container} for the
     * LayoutParams of the view without attaching to it.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_renderer_basic, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view. First we call our
     * super's implementation of {@code onViewCreated} then we initialize our field {@code ImageView mImageView}
     * by finding the view with id R.id.image, our field {@code Button mButtonPrevious} by finding the
     * view with id R.id.previous, and our field {@code Button mButtonNext} by finding the view with
     * id R.id.next. We then set the {@code OnClickListener} of both {@code mButtonPrevious} and
     * {@code mButtonNext} to this. We initialize our field {@code int mPageIndex} to 0, then if our
     * parameter {@code Bundle savedInstanceState} is not null we set {@code mPageIndex} to the int
     * stored under the key STATE_CURRENT_PAGE_INDEX ("current_page_index") in {@code savedInstanceState}
     * defaulting to 0.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Retain view references.
        mImageView = view.findViewById(R.id.image);
        mButtonPrevious = view.findViewById(R.id.previous);
        mButtonNext = view.findViewById(R.id.next);
        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);

        mPageIndex = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }
    }

    /**
     * Called when the Fragment is visible to the user. First we call our super's implementation of
     * {@code onStart}. Then wrapped in a try block intended to catch and toast IOException to the
     * user we call our method {@code openRenderer} to set up our {@code PdfRenderer} to render our
     * pdf file. We then call our method {@code showPage} to show the page of PDF at page index
     * {@code mPageIndex} on the screen.
     */
    @Override
    public void onStart() {
        super.onStart();
        try {
            //noinspection ConstantConditions
            openRenderer(getActivity());
            showPage(mPageIndex);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when the Fragment is no longer started. Wrapped in a try block intended to catch and
     * log IOException we call our method {@code closeRenderer} to close the current {@code Page}
     * {@code mCurrentPage}, our {@code PdfRenderer}, and the file descriptor {@code mFileDescriptor}.
     * We then call our super's implementation of {@code onStop}.
     */
    @Override
    public void onStop() {
        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance of its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onActivityCreated(Bundle)}.
     * <p>
     * First we call our super's implementation of {@code onSaveInstanceState}, then if our field
     * {@code Page mCurrentPage} is not null we add the page index of {@code mCurrentPage} to our
     * parameter {@code Bundle outState} under the key STATE_CURRENT_PAGE_INDEX ("current_page_index").
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources. First we initialize
     * {@code File file} using the absolute path to the application specific cache directory on the
     * filesystem for the parent path, and FILENAME ("sample.pdf") as the child pathname. If this file
     * does not exist (we have not been run before) we initialize {@code InputStream asset} by using
     * an AssetManager instance for the application's package to open the asset file named FILENAME,
     * initialize {@code FileOutputStream output} with a new instance to write to the file represented
     * by {@code File file}. We allocate 1024 bytes for {@code byte[] buffer}, and declare {@code int size}.
     * We then loop, setting {@code size} to the number of bytes that the {@code read} method of {@code asset}
     * reads into {@code buffer} and writing {@code size} bytes from {@code buffer} to {@code output}
     * until {@code size} is equal to -1 (end of file). Then we close both {@code asset} and {@code output}.
     * <p>
     * Now that we know that {@code File file} exists we initialize our field {@code ParcelFileDescriptor mFileDescriptor}
     * with a new instance to access {@code File file} in MODE_READ_ONLY mode. If {@code mFileDescriptor}
     * is not null we initialize our field {@code PdfRenderer mPdfRenderer} with a new instance constructed
     * to read from {@code mFileDescriptor}.
     *
     * @param context {@code Context} to use to access the application specific cache directory, and
     *                the AssetManager instance for the application's package (in order to read our
     *                example PDF from our assets). It is supplied by the {@code getActivity} method
     *                in our {@code onStart} override.
     * @throws IOException if an I/O error occurs.
     */
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            InputStream asset = context.getAssets().open(FILENAME);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        // This is the PdfRenderer we use to render the PDF.
        if (mFileDescriptor != null) {
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        }
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources. If our field
     * {@code Page mCurrentPage} is not null we call its {@code close} method to close the {@code Page}.
     * We then call the {@code close} method of our field {@code PdfRenderer mPdfRenderer} to close
     * the renderer, and the {@code close} method of our field {@code ParcelFileDescriptor mFileDescriptor}
     * to close the ParcelFileDescriptor as well as the underlying OS resources allocated to represent
     * the stream.
     *
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    /**
     * Shows the specified page of PDF to the screen. If the number of pages in the document is less
     * than or equal to the request page index {@code index} we return having done nothing. If the
     * field {@code Page mCurrentPage} is not null we call its {@code close} method to close the page.
     * We then set {@code mCurrentPage} to the {@code Page} that the {@code openPage} method of our
     * field {@code PdfRenderer mPdfRenderer} opens for rendering for the page index {@code index}.
     * We initialize {@code Bitmap bitmap} with a ARGB_8888 bitmap created for the width and height
     * of {@code mCurrentPage}. We then use the {@code render} method of {@code mCurrentPage} to
     * render the page's content for display on a screen into {@code bitmap}, and set that image as
     * the content of our field {@code ImageView mImageView}. Finally we call our method {@code updateUi}
     * to enable or disable the control buttons based on the current index, and set the activity's
     * title to reflect the page number.
     *
     * @param index The page index.
     */
    private void showPage(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(bitmap);
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index, and sets the
     * activity's title to reflect the page number. First we initialize {@code int index} with the
     * page index of our field {@code Page mCurrentPage}, and initialize {@code int pageCount} with
     * the number of pages in the document of {@code PdfRenderer mPdfRenderer}. We enable the button
     * {@code mButtonPrevious} if {@code index} is not 0 and disable it if it is. We enable the button
     * {@code mButtonNext} if {@code index+1} is less than {@code pageCount} and disable it if it is
     * greater than or equal to {@code pageCount}. We then set the title associated with this activity
     * to the string formatted using the format string with resource id R.string.app_name_with_index
     * to display the value {@code index+1} (page number) and {@code pageCount}.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        mButtonPrevious.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
        //noinspection ConstantConditions
        getActivity().setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing, and unused.
     *
     * @return The number of pages.
     */
    @SuppressWarnings("unused")
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    /**
     * Called when a view has been clicked. We switch on the id of our parameter {@code View view}:
     * <ul>
     *     <li>
     *         R.id.previous: we call our {@code showPage} method to display the page that is 1 page
     *         before the current page index of our field {@code Page mCurrentPage} then break.
     *     </li>
     *     <li>
     *         R.id.next: we call our {@code showPage} method to display the page that is 1 page
     *         after the current page index of our field {@code Page mCurrentPage} then break.
     *     </li>
     * </ul>
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous: {
                // Move to the previous page
                showPage(mCurrentPage.getIndex() - 1);
                break;
            }
            case R.id.next: {
                // Move to the next page
                showPage(mCurrentPage.getIndex() + 1);
                break;
            }
        }
    }

}
