/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.android.emojicompat

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.emoji.text.EmojiCompat
import androidx.emoji.widget.EmojiAppCompatButton
import androidx.emoji.widget.EmojiAppCompatEditText
import androidx.emoji.widget.EmojiAppCompatTextView
import java.lang.ref.WeakReference

/**
 * This sample demonstrates usage of [EmojiCompat] support library. You can use this library
 * to prevent your app from showing missing emoji characters in the form of tofu (□). You
 * can use either bundled or downloadable emoji fonts. This sample shows both usages.
 */
class MainActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.activity_main`.
     *
     * We initialize our [ScrollView] variable `rootView` to the view with ID `R.id.scroll` then
     * call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying window
     * insets to `rootView`, with the `listener` argument a lambda that accepts the [View] passed
     * the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize [TextView] variable `val emojiTextView` by finding the view with id
     * `R.id.emoji_text_view` (this is a [EmojiAppCompatTextView]), then set its text to the
     * formatted string created using `R.string.emoji_text_view` as the format and [EMOJI] as the
     * format argument. We initialize [TextView] variable `val emojiEditText` by finding the view
     * with id `R.id.emoji_edit_text` (this is a [EmojiAppCompatEditText]), then set its text to
     * the formatted string created using `R.string.emoji_edit_text` as the format and [EMOJI] as
     * the format argument. We initialize [TextView] variable `val emojiButton` by finding the
     * view with id `R.id.emoji_button` (this is a [EmojiAppCompatButton]), then set its text
     * to the formatted string created using `R.string.emoji_button` as the format and [EMOJI]
     * as the format argument. We initialize our regular [TextView] variable `val regularTextView`
     * by finding the view with id `R.id.regular_text_view`, then because it is not supplied by
     * the [EmojiCompat] library we must get the singleton [EmojiCompat] instance and register
     * a new instance of our class [InitCallback] subclass with `regularTextView` in order to set
     * its text using the [EmojiCompat] library. We initialize [TextView] variable
     * `val customTextView` by finding the view with id `R.id.emoji_custom_text_view`
     * (a [CustomTextView]), then set its text to the formatted string created using
     * `R.string.emoji_edit_text` as the format and [EMOJI] as the format argument.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<ScrollView>(R.id.scroll)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // TextView variant provided by EmojiCompat library
        val emojiTextView = findViewById<TextView>(R.id.emoji_text_view)
        emojiTextView.text = getString(R.string.emoji_text_view, EMOJI)

        // EditText variant provided by EmojiCompat library
        val emojiEditText = findViewById<TextView>(R.id.emoji_edit_text)
        emojiEditText.text = getString(R.string.emoji_edit_text, EMOJI)

        // Button variant provided by EmojiCompat library
        val emojiButton = findViewById<TextView>(R.id.emoji_button)
        emojiButton.text = getString(R.string.emoji_button, EMOJI)

        // Regular TextView without EmojiCompat support; you have to manually process the text
        val regularTextView = findViewById<TextView>(R.id.regular_text_view)
        EmojiCompat.get().registerInitCallback(InitCallback(regularTextView))

        // Custom TextView
        val customTextView = findViewById<TextView>(R.id.emoji_custom_text_view)
        customTextView.text = getString(R.string.custom_text_view, EMOJI)
    }

    /**
     * This is the callback that our regular [TextView] (resource ID `R.id.regular_text_view`) needs
     * to use to set its text. Our constructor. We save a weak reference to our [TextView] parameter
     * `regularTextView` in our field [WeakReference] of [TextView] field [mRegularTextViewRef] in
     * our `init` block.
     *
     * @param regularTextView TextView we write to using the [EmojiCompat] library
     */
    private class InitCallback(regularTextView: TextView) : EmojiCompat.InitCallback() {
        /**
         * The regular [TextView] we were constructed to write to using the [EmojiCompat] library.
         */
        @Suppress("JoinDeclarationAndAssignment") // It is better this way
        private val mRegularTextViewRef: WeakReference<TextView>

        init {
            mRegularTextViewRef = WeakReference(/* referent = */ regularTextView)
        }

        /**
         * Called when [EmojiCompat] is initialized and the emoji data is loaded. We initialize our
         * [TextView] variable `val regularTextView` with the referent of the weak reference to the
         * [TextView] stored in field [mRegularTextViewRef]. If this is not `null` we initialize our
         * [EmojiCompat] variable `val compat` with the singleton [EmojiCompat] instance, and our
         * [Context] variable `val context` with the context of `regularTextView`. Finally we set
         * the text of `regularTextView` to the [CharSequence] returned by the [EmojiCompat.process]
         * method of `compat` after it processes the string generated by the [Context.getString]
         * method of `context` from the format string `R.string.regular_text_view` and the format
         * argument [EMOJI].
         */
        override fun onInitialized() {
            val regularTextView = mRegularTextViewRef.get()
            if (regularTextView != null) {
                val compat = EmojiCompat.get()
                val context: Context = regularTextView.context
                regularTextView.text = compat.process(context.getString(R.string.regular_text_view, EMOJI))
            }
        }
    }

    companion object {
        /**
         * [U+1F469] (WOMAN) + [U+200D] (ZERO WIDTH JOINER) + [U+1F4BB] (PERSONAL COMPUTER)
         */
        private const val WOMAN_TECHNOLOGIST = "\uD83D\uDC69\u200D\uD83D\uDCBB"

        /**
         * [U+1F469] (WOMAN) + [U+200D] (ZERO WIDTH JOINER) + [U+1F3A4] (MICROPHONE)
         */
        private const val WOMAN_SINGER = "\uD83D\uDC69\u200D\uD83C\uDFA4"

        /**
         * Emoji string used by format we use to set our text.
         */
        const val EMOJI: String = "$WOMAN_TECHNOLOGIST $WOMAN_SINGER"
    }
}
