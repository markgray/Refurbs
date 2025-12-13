/*
 * Copyright 2017, The Android Open Source Project
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
package com.example.android.persistence.db.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Contains static methods to convert from a [Long] timestamp in milliseconds since January 1, 1970,
 * 00:00:00 GMT to a [Date] (the [toDate] method), and vice-versa (the [toTimestamp] method).
 */
object DateConverter {
    /**
     * Converts its [Long] parameter [timestamp] to a [Date] object.
     *
     * @param timestamp timestamp in milliseconds since January 1, 1970, 00:00:00 GMT
     * @return [Date] instance for the time [timestamp].
     */
    @JvmStatic
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return if (timestamp == null) null else Date(timestamp)
    }

    /**
     * Converts a [Date] instance to a [Long] milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * @param date [Date] instance we wish to convert to milliseconds since January 1, 1970,
     * 00:00:00 GMT
     * @return milliseconds since January 1, 1970, 00:00:00 GMT version of our [Date]
     * parameter [date]
     */
    @JvmStatic
    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}