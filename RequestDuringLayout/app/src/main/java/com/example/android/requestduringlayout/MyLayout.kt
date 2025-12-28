@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.requestduringlayout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

/**
 * Custom layout to enable the convoluted way of requesting-during-layout that we're
 * trying to show here. Yes, it's a hack. But it's a case that many apps hit (in much more
 * complicated and less demo-able ways), so it's interesting to at least understand the
 * artifacts that come from this sequence of events.
 */
class MyLayout : LinearLayout {
    /**
     * Number of the last [Button] added to our view.
     */
    var numButtons: Int = 0

    /**
     * If this flag is `true` we call our [addButton] method in our [onLayout] override.
     */
    var mAddRequestPending: Boolean = false

    /**
     * If this flag is `true` we call our [removeButton] method in our [onLayout] override.
     */
    var mRemoveRequestPending: Boolean = false

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We just call our super's constructor.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a
     * reference to a style resource that supplies default values for
     * the view. Can be 0 to not look for defaults.
     */
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    /**
     * Perform inflation from XML. We just call our super's constructor.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Our one argument constructor. We just call our super's constructor.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context)

    /**
     * Called from layout when this view should assign a size and position to each of its children.
     * First we call our super's implementation of `onLayout`, then if our [mRemoveRequestPending]
     * field is `true` we call our [removeButton] method to remove the first button we have added,
     * then set our [mRemoveRequestPending] field to false. If our [mAddRequestPending] field is
     * `true` we call our [addButton] method to add a new button to our view and then set the
     * [mAddRequestPending] field to false.
     *
     * @param changed This is a new size or position for this view
     * @param l       Left position, relative to parent
     * @param t       Top position, relative to parent
     * @param r       Right position, relative to parent
     * @param b       Bottom position, relative to parent
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // Here is the root of the problem: we are adding/removing views during layout. This
        // means that this view and its container will be put into an uncertain state that
        // can be difficult to discover and recover from.
        // Better approach: just add/remove at a time when layout is not running, certainly not
        // in the middle of onLayout(), or other layout-associated logic.
        if (mRemoveRequestPending) {
            removeButton()
            mRemoveRequestPending = false
        }
        if (mAddRequestPending) {
            addButton()
            mAddRequestPending = false
        }
    }

    /**
     * Removes the [View] at position 1 in our layout if we have 1 or more children.
     */
    private fun removeButton() {
        if (childCount > 1) {
            removeViewAt(1)
        }
    }

    /**
     * Adds a new button to our layout. We initialize [Button] variable `val button` with a new
     * instance, set its layout parameters to `WRAP_CONTENT` by `WRAP_CONTENT`, set its text to the
     * string formed by concatenating the string value of [numButtons] (post incrementing it) to the
     * string "Button ", then call the [addView] method to add it to our [ViewGroup]
     */
    @SuppressLint("SetTextI18n")
    private fun addButton() {
        val button = Button(context)
        button.layoutParams = LayoutParams(
            /* width = */ LayoutParams.WRAP_CONTENT,
            /* height = */ LayoutParams.WRAP_CONTENT
        )
        button.text = "Button " + numButtons++
        addView(button)
    }
}