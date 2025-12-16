/*
 * Copyright (C) 2015 The Android Open Source Project
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
@file:Suppress("DEPRECATION")
/**
 * TODO: Replace with advice in the below URL
 * Deprecated For publishing direct share targets, please follow the instructions in
 * https://developer.android.com/training/sharing/receive.html#providing-direct-share-targets instead.
 */

package com.example.android.directshare

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import com.example.android.directshare.Contact.Companion.byId

/**
 * Provides the Direct Share items to the system. Specified in AndroidManifest with an intent filter
 * whose action is "android.service.chooser.ChooserTargetService".
 */
@SuppressLint("Deprecated")
class SampleChooserTargetService : ChooserTargetService() {
    /**
     * Called by the system to retrieve a set of deep-link [targets][ChooserTarget] that can handle
     * an intent. The returned list should be sorted such that the most relevant targets appear
     * first. The score for each ChooserTarget will be combined with the system's score for the
     * original target Activity to sort and filter targets presented to the user.
     *
     * *Important:* Calls to this method from other applications will occur on a binder thread, not
     * on your app's main thread. Make sure that access to relevant data within your app is
     * thread-safe.
     *
     * First we create [ComponentName] variable `val componentName` from the name of this application's
     * package and the canonical name of the class [SendMessageActivity]. Then we initialize our
     * [ArrayList] of [ChooserTarget] variable `val targets` with a new instance. Then indexed by
     * `i` we loop through all the [Contact] objects in the [Array] of [Contact] singleton object
     * [Contact.CONTACTS] setting [Contact] variable `val contact` to the `Contact` with id `i`,
     * initializing [Bundle] variable `val extras` with a new instance then adding `i` as an extra
     * to it under the key [Contact.ID] ("contact_id"). We then add a new [ChooserTarget] to the
     * list `targets` constructed using the [Contact.name] of `contact` as the title of the target
     * that will be shown to a user, an Icon pointing to the drawable resource of the icon of
     * `contact` as the icon to represent the target, a ranking score of 0.5, `componentName` as the
     * component to be launched if this target is chosen, and `extras` as extra values to be merged
     * into the [Intent] when this target is chosen.
     *
     * When done building `targets`, we return it to the caller.
     *
     * @param targetActivityName the [ComponentName] of the matched activity that referred the
     * system to this [ChooserTargetService]
     * @param matchedFilter the specific [IntentFilter] on the component that was matched
     * @return a list of deep-link targets to fulfill the intent match, sorted by relevance
     */
    @Deprecated("Deprecated in Java")
    override fun onGetChooserTargets(
        targetActivityName: ComponentName,
        matchedFilter: IntentFilter
    ): List<ChooserTarget> {
        val componentName = ComponentName(
            /* pkg = */ packageName,
            /* cls = */ SendMessageActivity::class.java.canonicalName!!
        )
        // The list of Direct Share items. The system will show the items the way they are sorted
        // in this list.
        val targets = ArrayList<ChooserTarget>()
        for (i in Contact.CONTACTS.indices) {
            val contact: Contact = byId(id = i)
            val extras = Bundle()
            extras.putInt(Contact.ID, i)
            targets.add(ChooserTarget( // The name of this target.
                contact.name,
                // The icon to represent this target.
                Icon.createWithResource(this, contact.icon),
                // The ranking score for this target (0.0-1.0); the system will omit items with
                // low scores when there are too many Direct Share items.
                0.5f,
                // The name of the component to be launched if this target is chosen.
                componentName,
                // The extra values here will be merged into the Intent when this target is chosen.
                extras))
        }
        return targets
    }
}