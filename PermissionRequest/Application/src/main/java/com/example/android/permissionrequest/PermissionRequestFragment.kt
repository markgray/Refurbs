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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.permissionrequest

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.ConsoleMessage.MessageLevel
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.android.common.logger.Log
import com.example.android.permissionrequest.ConfirmationDialogFragment.Companion.newInstance
import com.example.android.permissionrequest.MessageDialogFragment.Companion.newInstance

/**
 * This fragment shows a [WebView] and loads a web app from the [SimpleWebServer].
 */
class PermissionRequestFragment : Fragment(),
    ConfirmationDialogFragment.Listener,
    MessageDialogFragment.Listener
{

    /**
     * We use this web server to serve HTML files in the assets folder. This is because we cannot
     * use the JavaScript method "getUserMedia" from "file:///android_assets/..." URLs.
     */
    private var mWebServer: SimpleWebServer? = null

    /**
     * A reference to the [WebView].
     */
    private var mWebView: WebView? = null

    /**
     * This field stores the [PermissionRequest] from the web application until it is allowed
     * or denied by user.
     */
    private var mPermissionRequest: PermissionRequest? = null

    /**
     * For testing only, not set when the app is running.
     */
    private var mConsoleMonitor: ConsoleMonitor? = null

    /**
     * Called to have the fragment instantiate its user interface view. We use our [LayoutInflater]
     * parameter [inflater] to inflate our layout file `R.layout.fragment_permission_request` using
     * our [ViewGroup] parameter [container] for the LayoutParams without attaching the view to it
     * and return the [View] created to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the [View].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            /* resource = */ R.layout.fragment_permission_request,
            /* root = */ container,
            /* attachToRoot = */ false
        )
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. We initialize our [WebView] field [mWebView] by finding the view in
     * our [View] parameter [view] with the id `R.id.web_view`. Then we set the chrome handler of
     * [mWebView] to our [WebChromeClient] field [mWebChromeClient] (for use in handling JavaScript
     * dialogs etc.). Finally we retrieve the [WebSettings] object of [mWebView] and call our method
     * [configureWebSettings] with it to enable JavaScript.
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mWebView = view.findViewById(R.id.web_view)
        // Here, we use #mWebChromeClient with implementation for handling PermissionRequests.
        mWebView!!.webChromeClient = mWebChromeClient
        configureWebSettings(mWebView!!.settings)
    }

    /**
     * Called when the fragment is visible to the user and actively running. First we call our
     * super's implementation of `onResume`, then we initialize our [Int] variable `val port` to
     * 8080. We initialize our [SimpleWebServer] field [mWebServer] with a new instance which uses
     * port `port`, and the underlying [AssetManager] of a [Resources] instance for the application's
     * package. We then call the [SimpleWebServer.start] method of [mWebServer] to create a new
     * thread for itself and start running listening to port 8080. If we have not been granted
     * the permission CAMERA we call our method [requestCameraPermission] to request the user for
     * that permission. Otherwise we call the `loadUrl` method of `mWebView` to load the url
     * "[...](http://localhost:8080/sample.html)".
     */
    override fun onResume() {
        super.onResume()
        val port = 8080
        mWebServer = SimpleWebServer(port, resources.assets)
        mWebServer!!.start()
        // This is for runtime permission on Marshmallow and above; It is not directly related to
        // PermissionRequest API.
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            mWebView!!.loadUrl("http://localhost:$port/sample.html")
        }
    }

    /**
     * Called when the [Fragment] is no longer resumed. We call the [SimpleWebServer.stop] method of
     * [mWebServer] to have it close its listening socket and exit its `run` override. Then we call
     * our super's implementation of `onPause`.
     */
    override fun onPause() {
        mWebServer!!.stop()
        super.onPause()
    }

    /**
     * Callback for the result from requesting permissions. This method is invoked for every call on
     * [requestPermissions]. If our [Int] parameter [requestCode] is [REQUEST_CAMERA_PERMISSION]
     * (the one we use) we check to make sure that the length of our [Array] of [String] parameter
     * [permissions] is 1, the length of [IntArray] parameter [grantResults] is 1, and the value
     * of `grantResults[0]` is [PackageManager.PERMISSION_GRANTED] and if not we log the message
     * "Camera permission not granted." If it is granted we check that our [WebView] field
     * [mWebView] is not `null` and our [SimpleWebServer] field [mWebServer] is not `null` before
     * calling the [WebView.loadUrl] method of [mWebView] to load the url formed by appending the
     * string "[...](http://localhost):" followed by the port of [mWebServer] followed by the string
     * "/sample.html". If it is not a response to our [REQUEST_CAMERA_PERMISSION] call we pass the
     * call to our super's implementation of `onRequestPermissionsResult`.
     *
     * @param requestCode The request code passed in [requestPermissions].
     * @param permissions The requested permissions. Never `null`.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never `null`.
     */
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // This is for runtime permission on Marshmallow and above; It is not directly related to
        // PermissionRequest API.
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (permissions.size != 1 || grantResults.size != 1
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted.")
            } else if (mWebView != null && mWebServer != null) {
                mWebView!!.loadUrl("http://localhost:" + mWebServer!!.port + "/sample.html")
            }
        } else {
            @Suppress("DEPRECATION")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * Called to request CAMERA permission. If the method [shouldShowRequestPermissionRationale]
     * returns `true`, we have asked the user for permission before and he turned us down so we
     * create a new instance of [MessageDialogFragment] to display the message with resource id
     * `R.string.permission_message` ("This sample app uses camera."), the positive button
     * `onClicked` override will call our override of [onOkClicked] which will then call the method
     * [requestPermissions]. If [shouldShowRequestPermissionRationale] returns `false` we call the
     * method [requestPermissions] now.
     */
    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            newInstance(R.string.permission_message)
                .show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    /**
     * This [WebChromeClient] has implementation for handling [PermissionRequest].
     */
    private val mWebChromeClient: WebChromeClient = object : WebChromeClient() {
        /**
         * Notify the host application that web content is requesting permission to access the
         * specified resources and the permission currently isn't granted or denied. The host
         * application must invoke [PermissionRequest.grant] or [PermissionRequest.deny].
         * If this method isn't overridden, the permission is denied.
         *
         * First we log the fact that we were called, then we save our [PermissionRequest] parameter
         * [request] in our [PermissionRequest] field [mPermissionRequest]. We initialize [Array] of
         * [String] variable `val requestedResources` by fetching the resources the web page is
         * trying to access from [request]. Then we loop through all the [String] variable `var r`
         * in `requestedResources`, and if `r` is equal to [PermissionRequest.RESOURCE_VIDEO_CAPTURE]
         * ("android.webkit.resource.VIDEO_CAPTURE") we create and show a new instance of
         * [ConfirmationDialogFragment] to display the text consisting of the string "This web page
         * wants to use following resources:" followed by the string [PermissionRequest.RESOURCE_VIDEO_CAPTURE]
         * (its ALLOW and DENY buttons will call our [onConfirmation] override with `true` and `false`
         * respectively). Having found the permission we were interested in we break out of the loop.
         *
         * @param request the PermissionRequest from current web content.
         */
        override fun onPermissionRequest(request: PermissionRequest) {
            Log.i(TAG, "onPermissionRequest")
            mPermissionRequest = request
            val requestedResources: Array<String> = request.resources
            for (r in requestedResources) {
                if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    // In this sample, we only accept video capture request.
                    newInstance(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        .show(childFragmentManager, FRAGMENT_DIALOG)
                    break
                }
            }
        }

        /**
         * This method is called when the permission request is canceled by the web content. First
         * we log the fact that we were called, then we set our [PermissionRequest] field
         * [mPermissionRequest] to `null`, and initialize [DialogFragment] variable `val fragment`
         * by using a private [FragmentManager] for placing and managing Fragments inside of this
         * [Fragment] to find the fragment with the tag [FRAGMENT_DIALOG], and if this is not `null`
         * we call its `dismiss` method to dismiss the fragment and its dialog.
         *
         * @param request the [PermissionRequest] that needs be canceled.
         */
        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            Log.i(TAG, "onPermissionRequestCanceled")
            // We dismiss the prompt UI here as the request is no longer valid.
            mPermissionRequest = null
            val fragment = childFragmentManager
                .findFragmentByTag(FRAGMENT_DIALOG) as DialogFragment?
            fragment?.dismiss()
        }

        /**
         * Report a JavaScript console message to the host application. The [WebChromeClient] should
         * override this to process the log message as they see fit. We switch on the [MessageLevel]
         * of the [ConsoleMessage] parameter [message], in all cases just logging the `message`
         * string from it. If our [ConsoleMonitor] field [mConsoleMonitor] is not `null` we call its
         * [ConsoleMonitor.onConsoleMessage] implementation (this is only used by tests, not in the
         * runtime). Finally we return `true` to indicate that we handled the message.
         *
         * @param message Object containing details of the console message.
         * @return `true` if the message is handled by the client.
         */
        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (message.messageLevel()) {
                MessageLevel.TIP -> Log.v(TAG, message.message())
                MessageLevel.LOG -> Log.i(TAG, message.message())
                MessageLevel.WARNING -> Log.w(TAG, message.message())
                MessageLevel.ERROR -> Log.e(TAG, message.message())
                MessageLevel.DEBUG -> Log.d(TAG, message.message())
            }
            if (null != mConsoleMonitor) {
                mConsoleMonitor!!.onConsoleMessage(message)
            }
            return true
        }
    }

    /**
     * Called when our [MessageDialogFragment] has its positive button clicked. We just call the
     * method [requestPermissions] to request CAMERA permission using [REQUEST_CAMERA_PERMISSION]
     * as the request code.
     */
    override fun onOkClicked() {
        @Suppress("DEPRECATION")
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    /**
     * Called when our [ConfirmationDialogFragment] has one of its buttons clicked, the positive
     * button calls with the [Boolean] parameter [allowed] `true`, and the negative calls with it
     * `false`. We branch on the value of our parameter [allowed]:
     *
     *  * `true`: we call the [PermissionRequest.grant] method of [PermissionRequest] field
     *  [mPermissionRequest] with the [Array] of [String] parameter [resources], and log the
     *  message "Permission granted."
     *
     *  * `false`: we call the [PermissionRequest.deny] method of [PermissionRequest] field
     *  [mPermissionRequest], and log the message "Permission request denied."
     *
     * We then set [mPermissionRequest] to `null`.
     *
     * @param allowed   `true` if the user allowed the request.
     * @param resources The resources to be granted.
     */
    override fun onConfirmation(allowed: Boolean, resources: Array<String?>?) {
        if (allowed) {
            mPermissionRequest!!.grant(resources)
            Log.d(TAG, "Permission granted.")
        } else {
            mPermissionRequest!!.deny()
            Log.d(TAG, "Permission request denied.")
        }
        mPermissionRequest = null
    }

    /**
     * Setter for our [ConsoleMonitor] field [mConsoleMonitor] used only for testing.
     *
     * @param monitor [ConsoleMonitor] to post message to
     */
    @Suppress("unused")
    fun setConsoleMonitor(monitor: ConsoleMonitor?) {
        mConsoleMonitor = monitor
    }

    /**
     * For testing.
     */
    interface ConsoleMonitor {
        /**
         * Report a JavaScript console message to the host application.
         *
         * @param message Object containing details of the console message.
         * @return `true` if the message is handled by the client.
         */
        fun onConsoleMessage(message: ConsoleMessage)
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private val TAG = PermissionRequestFragment::class.java.simpleName

        /**
         * TAG used for the [MessageDialogFragment] we show.
         */
        private const val FRAGMENT_DIALOG = "dialog"

        /**
         * Request code we use when calling [requestPermissions], it is returned to us in our
         * [onRequestPermissionsResult] override to identify which request has been processed.
         */
        private const val REQUEST_CAMERA_PERMISSION = 1

        /**
         * Configures the [WebSettings] of a WebView to enable JavaScript execution.
         *
         * @param settings the [WebSettings] object we are to configure.
         */
        @SuppressLint("SetJavaScriptEnabled")
        private fun configureWebSettings(settings: WebSettings) {
            settings.javaScriptEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }
}
