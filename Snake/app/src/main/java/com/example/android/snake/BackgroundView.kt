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
package com.example.android.snake

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Arrays
import androidx.core.content.withStyledAttributes

/**
 * Background View: Draw 4 full-screen RGBY triangles. This is drawn when the layout file
 * snake_layout.xml is inflated.
 *
 * In our `init` block we first call [setFocusable] with `true` (kotlin `isFocusable` property)
 * to enable our [View] to receive focus. Then we initialize the four colors in our [IntArray]
 * property [mColors] by using the [Context.withStyledAttributes] extension function to execute a
 * [TypedArray] lambda `block` that uses the the [TypedArray.getColor] method to retrieve the
 * values from our [AttributeSet] parameter [attrs] for the `R.styleable` attributes
 * BackgroundView_colorSegmentOne, BackgroundView_colorSegmentTwo, BackgroundView_colorSegmentThree,
 * and BackgroundView_colorSegmentFour.
 *
 * @param context The [Context] the view is running in, through which it can access the current
 * theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
class BackgroundView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * The four colors defined by the attributes BackgroundView_colorSegmentOne,
     * BackgroundView_colorSegmentTwo, BackgroundView_colorSegmentThree, and
     * BackgroundView_colorSegmentFour respectively.
     */
    private val mColors = IntArray(size = 4)

    /**
     * The [Paint] we use to draw our triangles.
     */
    private val mPaint = Paint()

    /**
     * Colors for each vertex
     */
    private lateinit var mFillColors: IntArray

    /**
     * Corner points for triangles (with offset = 2)
     */
    private val mIndices = shortArrayOf(0, 1, 2, 0, 3, 4, 0, 1, 4)

    /**
     * Vertex array for our three triangles.
     */
    private var mVertexPoints: FloatArray? = null

    init {
        isFocusable = true

        // retrieve colors for 4 segments from styleable properties
        context.withStyledAttributes(set = attrs, attrs = R.styleable.BackgroundView) {
            mColors[0] = getColor(R.styleable.BackgroundView_colorSegmentOne, Color.RED)
            mColors[1] = getColor(R.styleable.BackgroundView_colorSegmentTwo, Color.YELLOW)
            mColors[2] = getColor(R.styleable.BackgroundView_colorSegmentThree, Color.BLUE)
            mColors[3] = getColor(R.styleable.BackgroundView_colorSegmentFour, Color.GREEN)
        }
    }

    /**
     * We implement this to do our drawing. After asserting that [mVertexPoints] has been initialized
     * by our [onSizeChanged] override, we loop over our [Int] variable `var triangle` for all the
     * colors in the [mColors] array of [Int]:
     *
     *  * We set the color in [mFillColors] for all vertex points to current triangle color
     *
     *  * We then call the [Canvas.drawVertices] method of our parameter [canvas] to draw the
     *  current triangle and loop around for the next one.
     *
     * @param canvas the [Canvas] on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        assert(mVertexPoints != null)
        for (triangle in mColors.indices) {
            // Set color for all vertex points to current triangle color
            Arrays.fill(mFillColors, mColors[triangle])

            // Draw one triangle
            canvas.drawVertices(
                    Canvas.VertexMode.TRIANGLES,  // How to interpret the array of vertices: as TRIANGLES
                    (mVertexPoints ?: return).size,  // The number of values in the vertices and colors arrays
                    mVertexPoints ?: return,  // Array of vertices for the mesh
                    0,  // Number of values in the verts to skip before drawing.
                    null,  // No Textures
                    0,  // No Textures
                    mFillColors,  // color for each vertex
                    0,  // Number of values in colors to skip before drawing.
                    mIndices,  // array of indices to reference into the vertex and color arrays
                    triangle * 2,  // number of entries in the indices array to skip
                    3,  // Use 3 vertices via Index Array with offset 2
                    mPaint // Paint to use to draw
            )
        }
    }

    /**
     * This is called during layout when the size of this view has changed. First we call our super's
     * implementation of `onSizeChanged`. Then we initialize our array of [Float] field [mVertexPoints]
     * with five points defining the location of the center of our view, and the four corners of the
     * view based on the [Int] values of width [w], and height [h] passed us. Finally we initialize
     * our array of [Int] field [mFillColors] with a new array that is the same length as our field
     * [mVertexPoints].
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Construct our center and four corners
        mVertexPoints = floatArrayOf(
            w.toFloat() / 2f,
            h.toFloat() / 2f,
            0f,
            0f,
            w.toFloat(),
            0f,
            w.toFloat()
            , h.toFloat(),
            0f,
            h.toFloat()
        )
        mFillColors = IntArray((mVertexPoints ?: return).size)
    }
}