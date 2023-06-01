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
     * Loads an OpenGL shader from its source code. We initialize `int shader` by using the method
     * `glCreateShader` to create a shader of type `shaderType` (either GL_VERTEX_SHADER or
     * GL_FRAGMENT_SHADER), saving the `GLuint` returned in order to reference it. If `shader`
     * is not equal to 0, we call `glShaderSource` to replace the source code in the shader object
     * `shader` with our parameter `String source`. We then call the method `glCompileShader`
     * with `shader` as its argument to compile the source code string that has been stored in the
     * shader object. We allocate 1 int for `int[] compiled` then call the method `glGetShaderiv`
     * to fetch the GL_COMPILE_STATUS into `compiled` (sets it to GL_TRUE if the last compile operation
     * on `shader` was successful, and GL_FALSE otherwise). If `compiled[0]` is 0 (GL_FALSE)
     * we initialize `String info` with the information log for shader object `shader`,
     * call `glDeleteShader` to delete `shader` then throw RuntimeException "Could not compile shader "
     * with the details of the problem. If everything went well however, we return `shader` to the caller.
     *
     * @param shaderType type of shader to be created either GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
     * @param source OpenGL shader source code
     * @return value by which the loaded shader can be referenced
     */
    fun loadShader(shaderType: Int, source: String?): Int {
        val shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val info = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Could not compile shader $shaderType:$info")
            }
        }
        return shader
    }

    /**
     * Creates a OpenGL program object by compiling the vertex and fragment shader programs passed
     * it, and returns a GLuint by which it can be referenced. We initialize `int vertexShader`
     * with the value our method `loadShader` returns to reference the GL_VERTEX_SHADER it compiles
     * and loads from our parameter `vertexSource` and if this is 0 we return 0 to the caller. We
     * initialize `int pixelShader` with the value our method `loadShader` returns to reference
     * the GL_FRAGMENT_SHADER it compiles and loads from our parameter `fragmentSource` and if this
     * is 0 we return 0 to the caller. We initialize `int program` with the GLuint that the
     * `glCreateProgram` method returns to reference the empty program object it creates. If this
     * is not 0 we call the method `glAttachShader` to attach the shader object `vertexShader`
     * to `program`, then call our method `checkGlError` which will throw a RuntimeException
     * "glAttachShader" if `glGetError` returns any error other than GL_NO_ERROR. We call the method
     * `glAttachShader` to attach the shader object `pixelShader` to `program`, then
     * call our method `checkGlError` which will throw a RuntimeException "glAttachShader" if
     * `glGetError` returns any error other than GL_NO_ERROR. We next call the `glLinkProgram`
     * method to link the program object `program`. We allocate 1 int for `int[] linkStatus`
     * then call the method `glGetProgramiv` to fetch the GL_LINK_STATUS into `linkStatus`
     * (sets it to GL_TRUE if the last link operation on `program` was successful, and GL_FALSE
     * otherwise). If `linkStatus[0]` is not equal to GL_TRUE, we initialize `String info`
     * with the information log for the program object `program`, call `glDeleteProgram` to
     * delete `program` and throw RuntimeException "Could not link program: ". If everything goes
     * right however we return `program` to the caller.
     *
     * @param vertexSource OpenGL source code string for the vertex shader
     * @param fragmentSource OpenGL source code string for the fragment shader
     * @return value by which the created program object can be referenced
     */
    fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return 0
        }
        val program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES20.glAttachShader(program, pixelShader)
            checkGlError("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus,
                0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                val info = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Could not link program: $info")
            }
        }
        return program
    }

    /**
     * Checks for an OpenGL error and throws RuntimeException if anything but GL_NO_ERROR has occurred.
     * We initialize `int error` with the value of the error flag returned by the method `glGetError`.
     * If this is not GL_NO_ERROR we throw a RuntimeException constructed using the string formed by
     * concatenating our parameter `String op` followed by the string ": glError " followed by the
     * string value of `error`.
     *
     * @param op string to include in our RuntimeException if we need to throw it
     */
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
    }

    /**
     * Set texture parameters. We call the `glTexParameteri` method four times for the GL_TEXTURE_2D
     * target texture of the active texture unit, setting the single-valued texture parameters:
     *
     *  *
     * GL_TEXTURE_MAG_FILTER (The texture magnification function is used when the pixel being textured
     * maps to an area less than or equal to one texture element) to GL_LINEAR (Returns the weighted
     * average of the four texture elements that are closest to the center of the pixel being textured)
     *
     *  *
     * GL_TEXTURE_MIN_FILTER (The texture minifying function is used whenever the pixel being textured
     * maps to an area greater than one texture element) to GL_LINEAR (Returns the weighted average of
     * the four texture elements that are closest to the center of the pixel being textured)
     *
     *  *
     * GL_TEXTURE_WRAP_S (Sets the wrap parameter for texture coordinate s) to GL_CLAMP_TO_EDGE (causes
     * s coordinates to be clamped to the edge - textures stop at the last pixel when you fall off the edge).
     *
     *  *
     * GL_TEXTURE_WRAP_T (Sets the wrap parameter for texture coordinate t) to GL_CLAMP_TO_EDGE (causes
     * t coordinates to be clamped to the edge - textures stop at the last pixel when you fall off the edge).
     *
     *
     */
    fun initTexParams() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE)
    }
}