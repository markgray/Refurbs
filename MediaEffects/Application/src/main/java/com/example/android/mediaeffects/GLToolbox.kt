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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.mediaeffects

import android.opengl.GLES20

/**
 * Toolbox for using GLES20.
 */
object GLToolbox {
    /**
     * Loads an OpenGL shader from its source code. We initialize [Int] variable `val shader` by
     * using the method [GLES20.glCreateShader] to create a shader of type of [Int] parameter
     * [shaderType] (either [GLES20.GL_VERTEX_SHADER] or [GLES20.GL_FRAGMENT_SHADER]), saving the
     * `GLuint` ([Int]) returned in order to reference it. If `shader` is not equal to 0, we call
     * [GLES20.glShaderSource] to replace the source code in the shader object `shader` with our
     * [String] parameter [source]. We then call the method [GLES20.glCompileShader] with `shader`
     * as its argument to compile the source code string that has been stored in the shader object.
     * We allocate 1 [Int] for [IntArray] variable `val compiled` then call the method
     * [GLES20.glGetShaderiv] to fetch the [GLES20.GL_COMPILE_STATUS] into `compiled` (sets it to
     * `GL_TRUE` (1) if the last compile operation on `shader` was successful, and `GL_FALSE` (0)
     * otherwise). If `compiled[0]` is 0 (`GL_FALSE`) we initialize [String] variable `val info`
     * with the information log for shader object `shader`, call [GLES20.glDeleteShader] to delete
     * `shader` then throw [RuntimeException] "Could not compile shader " with the details of the
     * problem. If everything went well however, we return `shader` to the caller.
     *
     * @param shaderType type of shader to be created either `GL_VERTEX_SHADER` or `GL_FRAGMENT_SHADER`
     * @param source OpenGL shader source code
     * @return value by which the loaded shader can be referenced
     */
    fun loadShader(shaderType: Int, source: String?): Int {
        val shader: Int = GLES20.glCreateShader(/* type = */ shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(/* shader = */ shader, /* string = */ source)
            GLES20.glCompileShader(/* shader = */ shader)
            val compiled = IntArray(size = 1)
            GLES20.glGetShaderiv(
                /* shader = */ shader,
                /* pname = */ GLES20.GL_COMPILE_STATUS,
                /* params = */ compiled,
                /* offset = */ 0
            )
            if (compiled[0] == 0) {
                val info: String = GLES20.glGetShaderInfoLog(/* shader = */ shader)
                GLES20.glDeleteShader(/* shader = */ shader)
                throw RuntimeException("Could not compile shader $shaderType:$info")
            }
        }
        return shader
    }

    /**
     * Creates a OpenGL program object by compiling the vertex and fragment shader programs passed
     * it, and returns a `GLuint` by which it can be referenced. We initialize [Int] variable
     * `val vertexShader` with the value our method [loadShader] returns to reference the
     * `GL_VERTEX_SHADER` it compiles and loads from our [String] parameter [vertexSource] and if
     * this is 0 we return 0 to the caller. We initialize [Int] variable `val pixelShader` with the
     * value our method [loadShader] returns to reference the `GL_FRAGMENT_SHADER` it compiles and
     * loads from our [String] parameter [fragmentSource] and if this is 0 we return 0 to the caller.
     * We initialize [Int] variable `val program` with the `GLuint` that the [GLES20.glCreateProgram]
     * method returns to reference the empty program object it creates. If this is not 0 we call the
     * method [GLES20.glCreateProgram] to attach the shader object `vertexShader` to `program`, then
     * call our method [checkGlError] which will throw a [RuntimeException] "glAttachShader" if
     * [GLES20.glGetError] returns any error other than [GLES20.GL_NO_ERROR]. We call the method
     * [GLES20.glAttachShader] to attach the shader object `pixelShader` to `program`, then
     * call our method [checkGlError] which will throw a RuntimeException "glAttachShader" if
     * [GLES20.glGetError] returns any error other than [GLES20.GL_NO_ERROR]. We next call the
     * [GLES20.glLinkProgram] method to link the program object `program`. We allocate 1 int for
     * [IntArray] variable `val linkStatus` then call the method [GLES20.glGetProgramiv] to fetch
     * the [GLES20.GL_LINK_STATUS] into `linkStatus` (sets it to [GLES20.GL_TRUE] if the last link
     * operation on `program` was successful, and [GLES20.GL_FALSE] otherwise). If `linkStatus[0]`
     * is not equal to [GLES20.GL_TRUE], we initialize [String] variable `val info` with the
     * information log for the program object `program`, call [GLES20.glDeleteProgram] to delete
     * `program` and throw [RuntimeException] "Could not link program: ". If everything goes right
     * however we return `program` to the caller.
     *
     * @param vertexSource OpenGL source code string for the vertex shader
     * @param fragmentSource OpenGL source code string for the fragment shader
     * @return value by which the created program object can be referenced
     */
    fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
        val vertexShader: Int = loadShader(
            shaderType = GLES20.GL_VERTEX_SHADER,
            source = vertexSource
        )
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader: Int = loadShader(
            shaderType = GLES20.GL_FRAGMENT_SHADER,
            source = fragmentSource
        )
        if (pixelShader == 0) {
            return 0
        }
        val program: Int = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(/* program = */ program, /* shader = */ vertexShader)
            checkGlError(op = "glAttachShader")
            GLES20.glAttachShader(/* program = */ program, /* shader = */ pixelShader)
            checkGlError(op = "glAttachShader")
            GLES20.glLinkProgram(/* program = */ program)
            val linkStatus = IntArray(size = 1)
            GLES20.glGetProgramiv(
                /* program = */ program,
                /* pname = */ GLES20.GL_LINK_STATUS,
                /* params = */ linkStatus,
                /* offset = */ 0
            )
            if (linkStatus[0] != GLES20.GL_TRUE) {
                val info: String = GLES20.glGetProgramInfoLog(/* program = */ program)
                GLES20.glDeleteProgram(/* program = */ program)
                throw RuntimeException("Could not link program: $info")
            }
        }
        return program
    }

    /**
     * Checks for an OpenGL error and throws [RuntimeException] if anything but [GLES20.GL_NO_ERROR]
     * has occurred. We initialize [Int] variable `val error` with the value of the error flag
     * returned by the method [GLES20.glGetError]. If this is not [GLES20.GL_NO_ERROR] we throw a
     * [RuntimeException] constructed using the string formed by concatenating our [String] parameter
     * [op] followed by the string ": glError " followed by the string value of `error`.
     *
     * @param op string to include in our [RuntimeException] if we need to throw it
     */
    fun checkGlError(op: String) {
        val error: Int = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
    }

    /**
     * Set texture parameters. We call the [GLES20.glTexParameteri] method four times for the
     * [GLES20.GL_TEXTURE_2D] target texture of the active texture unit, setting the single-valued
     * texture parameters:
     *
     *  * [GLES20.GL_TEXTURE_MAG_FILTER] (The texture magnification function is used when the pixel
     *  being textured maps to an area less than or equal to one texture element) to [GLES20.GL_LINEAR]
     *  (Returns the weighted average of the four texture elements that are closest to the center of
     *  the pixel being textured)
     *
     *  * [GLES20.GL_TEXTURE_MIN_FILTER] (The texture minifying function is used whenever the pixel
     *  being textured maps to an area greater than one texture element) to [GLES20.GL_LINEAR]
     *  (Returns the weighted average of the four texture elements that are closest to the center
     *  of the pixel being textured)
     *
     *  * [GLES20.GL_TEXTURE_WRAP_S] (Sets the wrap parameter for texture coordinate s) to
     *  [GLES20.GL_CLAMP_TO_EDGE] (causes s coordinates to be clamped to the edge - textures
     *  stop at the last pixel when you fall off the edge).
     *
     *  * [GLES20.GL_TEXTURE_WRAP_T] (Sets the wrap parameter for texture coordinate t) to
     *  [GLES20.GL_CLAMP_TO_EDGE] (causes t coordinates to be clamped to the edge - textures
     *  stop at the last pixel when you fall off the edge).
     */
    fun initTexParams() {
        GLES20.glTexParameteri(
            /* target = */ GLES20.GL_TEXTURE_2D,
            /* pname = */ GLES20.GL_TEXTURE_MAG_FILTER,
            /* param = */ GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            /* target = */ GLES20.GL_TEXTURE_2D,
            /* pname = */ GLES20.GL_TEXTURE_MIN_FILTER,
            /* param = */ GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            /* target = */ GLES20.GL_TEXTURE_2D,
            /* pname = */ GLES20.GL_TEXTURE_WRAP_S,
            /* param = */ GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            /* target = */ GLES20.GL_TEXTURE_2D,
            /* pname = */ GLES20.GL_TEXTURE_WRAP_T,
            /* param = */ GLES20.GL_CLAMP_TO_EDGE
        )
    }
}