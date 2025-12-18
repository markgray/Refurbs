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
@file:Suppress("JoinDeclarationAndAssignment")

package com.example.android.emojicompat

import android.app.Application
import android.content.res.AssetManager
import android.util.Log
import androidx.core.provider.FontRequest
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiSpan
import androidx.emoji.text.FontRequestEmojiCompatConfig

/**
 * This application uses [EmojiCompat], it is specified by the android:name attribute of the
 * application element of app/src/main/AndroidManifest.xml
 */
class EmojiCompatApplication : Application() {
    /**
     * Called when the application is starting, before any activity, service, or receiver objects
     * (excluding content providers) have been created. First we call our super's implementation of
     * `onCreate`. We declare [EmojiCompat.Config] variable `val config` then if our use bundled
     * emoji flag ([USE_BUNDLED_EMOJI]) is:
     *
     *  * `true` - we set `config` to a new instance of [BundledEmojiCompatConfig] (this is a
     *  [EmojiCompat.Config] implementation that loads the metadata using [AssetManager] and
     *  bundled resources.
     *
     *  * `false` - We initialize [FontRequest] variable `val fontRequest` with an instance using
     *  "com.google.android.gms.fonts" as the authority of the Font Provider to be used for the
     *  request, "com.google.android.gms" as the package for the Font Provider to be used for the
     *  request,  "Noto Color Emoji Compat" as the query to be sent over to the provider, and the
     *  resource array `R.array.com_google_android_gms_fonts_certs` as the list of sets of hashes
     *  for the certificates the provider should be signed with. Then we set `config` to an
     *  asynchronous request for `fontRequest`, which we instruct to replace all emojis found with
     *  [EmojiSpan]'s, and to which we register an anonymous [EmojiCompat.InitCallback] class whose
     * [EmojiCompat.InitCallback.onInitialized] override logs "EmojiCompat initialized", and whose
     * [EmojiCompat.InitCallback.onFailed] override logs the error.
     *
     * Finally we call the [EmojiCompat.init] method with `config` as the argument.
     */
    override fun onCreate() {
        super.onCreate()
        val config: EmojiCompat.Config
        config = if (USE_BUNDLED_EMOJI) {
            // Use the bundled font for EmojiCompat
            BundledEmojiCompatConfig(applicationContext)
        } else {
            // Use a downloadable font for EmojiCompat
            val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)
            FontRequestEmojiCompatConfig(applicationContext, fontRequest)
                .setReplaceAll(true)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    /**
                     * Called when EmojiCompat is initialized and the emoji data is loaded.
                     * We log this.
                     */
                    override fun onInitialized() {
                        Log.i(TAG, "EmojiCompat initialized")
                    }

                    /**
                     * Called when an unrecoverable error occurs during EmojiCompat initialization.
                     * We log this.
                     *
                     * @param throwable the error or exception that occurred
                     */
                    override fun onFailed(throwable: Throwable?) {
                        Log.e(TAG, "EmojiCompat initialization failed", throwable)
                    }
                })
        }
        EmojiCompat.init(config)
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "EmojiCompatApplication"

        /**
         * Change this to `false` when you want to use the downloadable Emoji font.
         */
        private const val USE_BUNDLED_EMOJI = true
    }
}
