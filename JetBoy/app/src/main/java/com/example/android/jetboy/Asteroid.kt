/*
 * Copyright (C) 2009 The Android Open Source Project
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
// FIXME: review and cleanup
@file:Suppress("unused")

package com.example.android.jetboy

import android.graphics.Bitmap
import com.example.android.jetboy.JetBoyView.JetBoyThread

/**
 * Class used to represent an individual "Asteroid" as it is animated across space.
 */
class Asteroid {
    /**
     * Index of the [Bitmap] in [Array] of [Bitmap] field [JetBoyThread.mAsteroids] used to draw us.
     */
    var mAniIndex: Int = 0

    /**
     * Our current Y coordinate
     */
    var mDrawY: Int = 0

    /**
     * Our current Y coordinate
     */
    var mDrawX: Int = 0

    /**
     * Unused apparently
     */
    var mExploding: Boolean = false

    /**
     * Set to true if the user is no longer allowed to destroy us
     */
    var mMissed: Boolean = false

    /**
     * System time in milliseconds when we were created.
     */
    var mStartTime: Long = 0
}