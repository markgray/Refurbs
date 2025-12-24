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

package com.example.android.mediaeffects

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Our texture rendering class.
 */
class TextureRenderer {
    /**
     * The OpenGL program object created by compiling and linking the vertex and fragment shader
     * programs [VERTEX_SHADER] and [FRAGMENT_SHADER], it is used in our [renderTexture] method
     * where a call to the [GLES20.glUseProgram] method installs the program object as part of
     * current rendering state.
     */
    private var mProgram = 0

    /**
     * Location of the uniform variable in [mProgram] with the name "tex_sampler"
     */
    private var mTexSamplerHandle = 0

    /**
     * Location of the attribute variable in [mProgram] with the name "a_texcoord"
     */
    private var mTexCoordHandle = 0

    /**
     * Location of the attribute variable in [mProgram] with the name "a_position"
     */
    private var mPosCoordHandle = 0

    /**
     * [FloatBuffer] we place our vertices defined in the array [TEX_VERTICES] in in order
     * to later bind them to the vertex attribute [mTexCoordHandle] with a call to the
     * [GLES20.glVertexAttribPointer] method.
     */
    private var mTexVertices: FloatBuffer? = null

    /**
     * [FloatBuffer] we place our vertices defined in the array [POS_VERTICES] in order to
     * later bind them to the vertex attribute [mPosCoordHandle] with a call to the
     * [GLES20.glVertexAttribPointer] method.
     */
    private var mPosVertices: FloatBuffer? = null

    /**
     * Current width of our surface which is set by our [updateViewSize] method to the width passed
     * to the [GLSurfaceView.Renderer.onSurfaceChanged] override of the [MediaEffectsFragment]
     * fragment.
     */
    private var mViewWidth = 0

    /**
     * Current height of our surface which is set by our [updateViewSize] method to the height passed
     * to the [GLSurfaceView.Renderer.onSurfaceChanged] override of the [MediaEffectsFragment]
     * fragment.
     */
    private var mViewHeight = 0

    /**
     * Current width of our texture which is set by our [updateTextureSize] method to the width of
     * the bitmap we use as our texture as determined in the [MediaEffectsFragment.loadTextures]
     * method of the [MediaEffectsFragment] fragment.
     */
    private var mTexWidth = 0

    /**
     * Current height of our texture which is set by our [updateTextureSize] method to the height of
     * the bitmap we use as our texture as determined in the [MediaEffectsFragment.loadTextures]
     * method of the [MediaEffectsFragment] fragment.
     */
    private var mTexHeight = 0

    /**
     * Initializes the OpenGL engine to do what needs to be done. We initialize our [Int] field
     * [mProgram] with the reference to the compiled and linked program object that the method
     * [GLToolbox.createProgram] creates from our shaders [VERTEX_SHADER], and [FRAGMENT_SHADER].
     * We initialize [Int] field [mTexSamplerHandle] with the location in [mProgram] of the uniform
     * variable with the name "tex_sampler", [Int] field [mTexCoordHandle] with the location in
     * [mProgram] of the attribute variable with the name "a_texcoord", and [Int] field
     * [mPosCoordHandle] with the location in [mProgram] of the attribute variable with the name
     * "a_position". We initialize [FloatBuffer] field [mTexVertices] by allocating a direct byte
     * buffer long enough to hold the [TEX_VERTICES] array, set its order to [ByteOrder.nativeOrder],
     * then create a view of this byte buffer as a float buffer. We then bulk put all the vertex
     * values in [TEX_VERTICES] into [mTexVertices], chaining a call to `position(0)` to position the
     * [FloatBuffer] back to the beginning. We initialize [FloatBuffer] field [mPosVertices] by
     * allocating a direct byte buffer long enough to hold the POS_VERTICES array, set its order to
     * `nativeOrder`, then create a view of this byte buffer as a float buffer. We then bulk put all
     * the vertex values in POS_VERTICES into [mPosVertices], chaining a call to [FloatBuffer.position]
     * to position the [FloatBuffer] back to its beginning (0).
     */
    fun initialize() {
        // Create program
        mProgram = GLToolbox.createProgram(
            vertexSource = VERTEX_SHADER,
            fragmentSource = FRAGMENT_SHADER
        )

        // Bind attributes and uniforms
        mTexSamplerHandle = GLES20.glGetUniformLocation(
            /* program = */ mProgram,
            /* name = */ "tex_sampler"
        )
        mTexCoordHandle = GLES20.glGetAttribLocation(
            /* program = */ mProgram,
            /* name = */ "a_texcoord"
        )
        mPosCoordHandle = GLES20.glGetAttribLocation(
            /* program = */ mProgram,
            /* name = */ "a_position"
        )

        // Setup coordinate buffers
        mTexVertices = ByteBuffer.allocateDirect(
            /* capacity = */ TEX_VERTICES.size * FLOAT_SIZE_BYTES
        ).order(/* bo = */ ByteOrder.nativeOrder()).asFloatBuffer()
        mTexVertices!!.put(/* src = */ TEX_VERTICES).position(/* newPosition = */ 0)

        mPosVertices = ByteBuffer.allocateDirect(
            /* capacity = */ POS_VERTICES.size * FLOAT_SIZE_BYTES
        ).order(/* bo = */ ByteOrder.nativeOrder()).asFloatBuffer()
        mPosVertices!!.put(/* src = */ POS_VERTICES).position(/* newPosition = */ 0)
    }

    /**
     * Not actually used apparently. We call the [GLES20.glDeleteProgram] method to delete the
     * program object pointed to by [Int] field [mProgram].
     */
    fun tearDown() {
        GLES20.glDeleteProgram(/* program = */ mProgram)
    }

    /**
     * Called from the [MediaEffectsFragment.loadTextures] method of [MediaEffectsFragment] to set
     * our texture width ([Int] field [mTexWidth]) and texture height ([Int] field [mTexHeight]) to
     * the bitmap's width and height. After doing this we call our method [computeOutputVertices]
     * to calculate the correct values of the vertices in [FloatBuffer] field [mPosVertices].
     *
     * @param texWidth new image width
     * @param texHeight new image height
     */
    fun updateTextureSize(texWidth: Int, texHeight: Int) {
        mTexWidth = texWidth
        mTexHeight = texHeight
        computeOutputVertices()
    }

    /**
     * Called from the [GLSurfaceView.Renderer.onSurfaceChanged] override of [MediaEffectsFragment]
     * to set our view width ([Int] field [mViewWidth]) and view height ([Int] field [mViewHeight])
     * to the views width and height. After doing this we call our method [computeOutputVertices]
     * to calculate the correct values of the vertices in [FloatBuffer] field [mPosVertices].
     *
     * @param viewWidth new view width
     * @param viewHeight new view height
     */
    fun updateViewSize(viewWidth: Int, viewHeight: Int) {
        mViewWidth = viewWidth
        mViewHeight = viewHeight
        computeOutputVertices()
    }

    /**
     * Called from the [MediaEffectsFragment.renderResult] method of [MediaEffectsFragment] which is
     * called by its [MediaEffectsFragment.onDrawFrame] override to render the current frame, using
     * the desired texture name. First we call the [GLES20.glBindFramebuffer] method to bind the
     * [GLES20.GL_FRAMEBUFFER] target to the frame buffer 0 (the default framebuffer object provided
     * by the windowing system). Then we call the [GLES20.glUseProgram] method to install the program
     * object pointed to by [Int] field [mProgram] as part of the current rendering state. We then
     * call the [GLToolbox.checkGlError] method to check for any errors, throwing a [RuntimeException]
     * if any occurred.
     *
     * We next call the [GLES20.glViewport] method to set the viewport with (0,0) as the lower left
     * corner in pixels, and with [Int] field [mViewWidth] and [Int] field [mViewHeight] as the width
     * and height of the viewport. We then call the [GLToolbox.checkGlError] method to check for any
     * errors, throwing a [RuntimeException] if any occurred.
     *
     * We call [GLES20.glDisable] to disable [GLES20.GL_BLEND] which disables blending of the
     * computed fragment color values with the values in the color buffers. We call the
     * [GLES20.glVertexAttribPointer] method to specify that the generic vertex attribute whose
     * location is in [Int] field [mTexCoordHandle] (attribute variable "a_texcoord") is to use the
     * array in [FloatBuffer] field [mTexVertices], and that there are 2 components per generic
     * vertex attribute of type [GLES20.GL_FLOAT], they should not be normalized, and that the
     * stride is 0. We then call the [GLES20.glEnableVertexAttribArray] method to enable the
     * generic vertex attribute array pointed to by [Int] field [mTexCoordHandle] (when enabled,
     * the values in the generic vertex attribute array will be accessed and used for rendering
     * when calls are made to vertex array commands such as [GLES20.glDrawArrays] or
     * [GLES20.glDrawElements]).
     *
     * We call the [GLES20.glVertexAttribPointer] method to specify that the generic vertex attribute
     * whose location is in [Int] field [mPosCoordHandle] (attribute variable "a_position") is to use
     * the array in [FloatBuffer] field [mPosVertices], and that there are 2 components per generic
     * vertex attribute of type [GLES20.GL_FLOAT], they should not be normalized, and that the stride
     * is 0. We then call the [GLES20.glEnableVertexAttribArray] method to enable the generic vertex
     * attribute array [mPosCoordHandle] (when enabled, the values in the generic  vertex attribute
     * array will be accessed and used for rendering when calls are made to vertex array commands
     * such as [GLES20.glDrawArrays] or [GLES20.glDrawElements]). We then call the [GLToolbox.checkGlError]
     * method to check for any errors, throwing a [RuntimeException] if any occurred.
     *
     * We call the [GLES20.glActiveTexture] method to select [GLES20.GL_TEXTURE0] to be the active
     * texture unit, then call the [GLToolbox.checkGlError] method to check for any errors, throwing
     * a [RuntimeException] if any occurred. We then call the [GLES20.glBindTexture] method to bind
     * the [GLES20.GL_TEXTURE_2D] texture target to the texture with the name of our [Int] parameter
     * [texId] (texture targets become aliases for the textures currently bound to them). We then
     * call the [GLToolbox.checkGlError] method to check for any errors, throwing a [RuntimeException]
     * if any occurred. Then we call the [GLES20.glUniform1i] method to specify the value of the
     * uniform variable whose address is in [Int] field [mTexSamplerHandle] (the uniform variable
     * with the name "tex_sampler") to be 0 (the texture unit to use).
     *
     * We call the [GLES20.glClearColor] method to specify the red, green, blue, and alpha values
     * used when the color buffers are cleared to be a black with a 1.0 alpha value. We call the
     * [GLES20.glClear] method to clear the [GLES20.GL_COLOR_BUFFER_BIT] buffer (clears the color
     * buffer). Finally we call the [GLES20.glDrawArrays] method to render [GLES20.GL_TRIANGLE_STRIP]
     * primitives from its array data, using 0 as the starting index, and using 4 as the number of
     * vertices to be rendered.
     *
     * @param texId Generated texture name to use
     */
    fun renderTexture(texId: Int) {
        // Bind default FBO
        GLES20.glBindFramebuffer(/* target = */ GLES20.GL_FRAMEBUFFER, /* framebuffer = */ 0)

        // Use our shader program
        GLES20.glUseProgram(/* program = */ mProgram)
        GLToolbox.checkGlError(op = "glUseProgram")

        // Set viewport
        GLES20.glViewport(
            /* x = */ 0,
            /* y = */ 0,
            /* width = */ mViewWidth,
            /* height = */ mViewHeight
        )
        GLToolbox.checkGlError(op = "glViewport")

        // Disable blending
        GLES20.glDisable(/* cap = */ GLES20.GL_BLEND)

        // Set the vertex attributes
        GLES20.glVertexAttribPointer(
            /* indx = */ mTexCoordHandle,
            /* size = */ 2,
            /* type = */ GLES20.GL_FLOAT,
            /* normalized = */ false,
            /* stride = */ 0,
            /* ptr = */ mTexVertices
        )
        GLES20.glEnableVertexAttribArray(/* index = */ mTexCoordHandle)

        GLES20.glVertexAttribPointer(
            /* indx = */ mPosCoordHandle,
            /* size = */ 2,
            /* type = */ GLES20.GL_FLOAT,
            /* normalized = */ false,
            /* stride = */ 0,
            /* ptr = */ mPosVertices
        )
        GLES20.glEnableVertexAttribArray(/* index = */ mPosCoordHandle)
        GLToolbox.checkGlError(op = "vertex attribute setup")

        // Set the input texture
        GLES20.glActiveTexture(/* texture = */ GLES20.GL_TEXTURE0)
        GLToolbox.checkGlError(op = "glActiveTexture")
        GLES20.glBindTexture(/* target = */ GLES20.GL_TEXTURE_2D, /* texture = */ texId)
        GLToolbox.checkGlError(op = "glBindTexture")
        GLES20.glUniform1i(/* location = */ mTexSamplerHandle, /* x = */ 0)

        // Draw
        GLES20.glClearColor(
            /* red = */ 0.0f,
            /* green = */ 0.0f,
            /* blue = */ 0.0f,
            /* alpha = */ 1.0f
        )
        GLES20.glClear(/* mask = */ GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawArrays(/* mode = */ GLES20.GL_TRIANGLE_STRIP, /* first = */ 0, /* count = */ 4)
    }

    /**
     * Computes the vertices for the [FloatBuffer] field [mPosVertices] which supplies the vertices
     * for the attribute variable "a_position" which is used as the location of the current vertex.
     * If [mPosVertices] is `null` we do nothing. Otherwise we calculate the image aspect ratio in
     * [Float] variable `val imgAspectRatio` to be [Int] field [mTexWidth] divided by [Int] field
     * [mTexHeight], the view aspect ratio in [Float] variable `val viewAspectRatio` to be [Int]
     * field [mViewWidth] divided by [Int] field [mViewHeight], and the relative aspect ratio in
     * [Float] variable `val relativeAspectRatio` to be `viewAspectRatio` divided by `imgAspectRatio`.
     * We declare [Float] variable `val x0`, `val y0`, `val x1`, and `val y1` then we branch on the
     * value of `relativeAspectRatio`:
     *
     *  * greater than 1.0f: we set `x0` to `-1.0f/relativeAspectRatio`, `y0` to -1.0f, `x1` to
     *  `1.0f/relativeAspectRatio`, and `y1` to 1.0f
     *
     *  * less then 1.0f: we set `x0` to -1.0, `y0` to `-relativeAspectRatio`, `x1` to 1.0f, and
     *  `y1` to `relativeAspectRatio`.
     *
     * We initialize [FloatArray] variable `val coords` to contain `x0, y0, x1, y0, x0, y1, x1, y1`
     * then bulk copy it to [FloatBuffer] field [mPosVertices], chaining to it a call to
     * [FloatBuffer.position] to reposition [mPosVertices] to its beginning.
     */
    private fun computeOutputVertices() {
        if (mPosVertices != null) {
            val imgAspectRatio: Float = mTexWidth / mTexHeight.toFloat()
            val viewAspectRatio: Float = mViewWidth / mViewHeight.toFloat()
            val relativeAspectRatio: Float = viewAspectRatio / imgAspectRatio
            val x0: Float
            val y0: Float
            val x1: Float
            val y1: Float
            if (relativeAspectRatio > 1.0f) {
                x0 = -1.0f / relativeAspectRatio
                y0 = -1.0f
                x1 = 1.0f / relativeAspectRatio
                y1 = 1.0f
            } else {
                x0 = -1.0f
                y0 = -relativeAspectRatio
                x1 = 1.0f
                y1 = relativeAspectRatio
            }
            val coords: FloatArray = floatArrayOf(x0, y0, x1, y0, x0, y1, x1, y1)
            mPosVertices!!.put(/* src = */ coords).position(/* newPosition = */ 0)
        }
    }

    companion object {
        /**
         * Our vertex shader code. Meaning of each line of code:
         *
         *  * attribute vec4 a_position; Vertex attributes are used to communicate from "outside"
         *  to the vertex shader. Unlike uniform variables, values are provided per vertex (and
         *  not globally for all vertices). Its location is retrieved to set `mPosCoordHandle`
         *  and used as the index of the generic vertex attribute to be modified in a call to
         *  `glVertexAttribPointer` and `glEnableVertexAttribArray`.
         *
         *  * attribute vec2 a_texcoord; The location of this attribute is retrieved to set
         *  `mTexCoordHandle`, and used as the index of the generic vertex attribute to be modified
         *  in a call to `glVertexAttribPointer` and `glEnableVertexAttribArray`.
         *
         *  * varying vec2 v_texcoord; Varying variables provide an interface between Vertex and
         *  Fragment Shader. Vertex Shaders compute values per vertex and fragment shaders compute
         *  values per fragment. If you define a varying variable in a vertex shader, its value
         *  will be interpolated (perspective-correct) over the primitive being rendered and you
         *  can access the interpolated value in the fragment shader.
         *
         *  * void main() {  Starts the program
         *
         *  * gl_Position = a_position; gl_Position is a built-in vertex shader output variable,
         *  whose type is defined by the OpenGL specification to be a vec4, this statement sets
         *  it to the value of our current attribute `a_position`,
         *
         *  * v_texcoord = a_texcoord; Here we communicate the value of our attribute `a_texcoord`
         *  to our fragment shader using our shared `varying vec2 v_texcoord`.
         *
         *  * } The end of our program.
         */
        private const val VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_Position = a_position;\n" +
            "  v_texcoord = a_texcoord;\n" +
            "}\n"

        /**
         * Our frament shader code. Meaning of each line of code:
         *
         *  * precision mediump float; This determines how much precision the GPU uses when
         *  calculating floats. highp is high precision, and of course more intensive than mediump
         *  (medium precision) and lowp (low precision).
         *
         *  * uniform sampler2D tex_sampler; Uniforms are so named because they do not change from
         *  one execution of a shader program to the next within a particular rendering call. A
         *  sampler2D is a 2D texture. The location of `tex_sampler` is saved by a call to
         *  `glGetUniformLocation` in `mTexSamplerHandle`. It is used in a call to
         *  `glUniform1i` which sets the texture unit to 0.
         *
         *  * varying vec2 v_texcoord; Varying variables provide an interface between Vertex and
         *  Fragment Shader. It is set to the value of its `attribute vec2 a_texcoord`.
         *
         *  * void main() {  Start of our program.
         *
         *  * gl_FragColor = texture2D(tex_sampler, v_texcoord); `gl_FragColor` is a built-in output
         *  variable for setting the `vec4` fragment color. `texture2D` looks up the color for the
         *  coordinates given by `v_texcoord` using the sampler `tex_sampler` (which is just the
         *  texture we bound to GL_TEXTURE_2D for the default texture unit GL_TEXTURE0.
         *
         *  * } The end of our program.
         */
        private const val FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
            "}\n"

        /**
         * Vertices we copy to [FloatBuffer] field [mTexVertices]
         */
        private val TEX_VERTICES = floatArrayOf(
            0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )

        /**
         * Vertices we copy to [FloatBuffer] field [mPosVertices]
         */
        private val POS_VERTICES = floatArrayOf(
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f
        )

        /**
         * Number of bytes in a float value
         */
        private const val FLOAT_SIZE_BYTES = 4
    }
}