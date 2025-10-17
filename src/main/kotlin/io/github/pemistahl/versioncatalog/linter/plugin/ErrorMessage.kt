/*
 * Copyright © 2024 Peter M. Stahl pemistahl@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pemistahl.versioncatalog.linter.plugin

internal class ErrorMessage(
    private val lineNumbers: IntRange,
    private val message: String,
) : Comparable<ErrorMessage> {
    constructor(lineNumber: Int, message: String) : this(lineNumber..lineNumber, message)

    override fun compareTo(other: ErrorMessage): Int {
        val thisRangeStart = lineNumbers.first
        val thisRangeEnd = lineNumbers.last
        val otherRangeStart = other.lineNumbers.first
        val otherRangeEnd = other.lineNumbers.last

        if (thisRangeStart == otherRangeStart && thisRangeEnd == otherRangeEnd) {
            return message.compareTo(other.message)
        }
        if (thisRangeStart == otherRangeStart) {
            return thisRangeEnd.compareTo(otherRangeEnd)
        }

        return thisRangeStart.compareTo(otherRangeStart)
    }

    override fun toString(): String {
        val lineNumber =
            if (lineNumbers.first == lineNumbers.last) {
                "Line ${lineNumbers.first}"
            } else {
                "Lines ${lineNumbers.first}-${lineNumbers.last}"
            }
        return "$lineNumber: $message"
    }
}
