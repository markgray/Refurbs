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
import android.text.InputFilter
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import android.text.method.TransformationMethod
import androidx.emoji.widget.EmojiTextViewHelper
import androidx.emoji.text.EmojiCompat

/**
 * A sample implementation of custom [AppCompatTextView]. You can use [EmojiTextViewHelper] to make
 * your custom [AppCompatTextView] compatible with [EmojiCompat].
 *
 * Our constructor performs inflation from XML and apply a class-specific base style from a theme
 * attribute or style resource. First we call our super's constructor, then in our `init` block we
 * call our method [getEmojiTextViewHelper] (aka kotlin property [emojiTextViewHelper] to get the
 * [EmojiTextViewHelper] for this TextView (constructing a new instance and saving it in our
 * [EmojiTextViewHelper] backing field [mEmojiTextViewHelper] if this is the first time it is
 * called) and call its [EmojiTextViewHelper.updateTransformationMethod] method to update the
 * widget's [TransformationMethod] so that the transformed text can be processed.
 *
 * @param context The Context the view is running in, through which it can access the current theme,
 * resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 * resource that supplies default values for the view. Can be 0 to not look for defaults.
 */
class CustomTextView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context!!, attrs, defStyleAttr) {
    /**
     * Our instance of a `EmojiTextViewHelper` to use to as a Utility class to enhance custom
     * TextView widgets with `EmojiCompat`.
     */
    private var mEmojiTextViewHelper: EmojiTextViewHelper? = null

    init {
        emojiTextViewHelper.updateTransformationMethod()
    }

    /**
     * Sets the list of input filters that will be used if the buffer is Editable. Has no effect
     * otherwise. We call our super's implementation of `setFilters` with the EmojiCompat
     * [InputFilter]'s appended to the widget [InputFilter]'s in our [Array] of [InputFilter]
     * parameter [filters].
     *
     * @param filters filters that will be called in succession whenever the text of an Editable is
     * changed (we are not editable so these are ignored).
     */
    override fun setFilters(filters: Array<InputFilter>) {
        @Suppress("UsePropertyAccessSyntax", "RedundantSuppression")
        super.setFilters(emojiTextViewHelper.getFilters(filters))
    }

    /**
     * Sets the properties of this field to transform input to ALL CAPS display. This may use a
     * "small caps" formatting if available. This setting will be ignored if this field is editable
     * or selectable. This call replaces the current transformation method. Disabling this will not
     * necessarily restore the previous behavior from before this was enabled. We first call our
     * super's implementation of `setAllCaps`, then we call the [EmojiTextViewHelper.setAllCaps]
     * method of the [EmojiTextViewHelper] for this TextView returned by our [EmojiTextViewHelper]
     * property [emojiTextViewHelper].
     *
     * @param allCaps if `true` the transformation method is set to `AllCapsTransformationMethod`
     */
    override fun setAllCaps(allCaps: Boolean) {
        super.setAllCaps(allCaps)
        emojiTextViewHelper.setAllCaps(allCaps)
    }

    /**
     * Returns the [EmojiTextViewHelper] for this TextView. If [mEmojiTextViewHelper] is currently
     * `null` we first initialize it with a new instance of [EmojiTextViewHelper] for "this"
     * TextView. Finally we return [mEmojiTextViewHelper] to the caller.
     *
     * @return the [EmojiTextViewHelper] for this TextView.
     */
    private val emojiTextViewHelper: EmojiTextViewHelper
        get() {
            if (mEmojiTextViewHelper == null) {
                mEmojiTextViewHelper = EmojiTextViewHelper(this)
            }
            return mEmojiTextViewHelper!!
        }
}