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

import java.io.File

internal fun readVersionCatalog(versionCatalogFile: File): VersionCatalog {
    val versions = mutableListOf<Pair<IntRange, String>>()
    val libraries = mutableListOf<Pair<IntRange, String>>()
    val bundles = mutableListOf<Pair<IntRange, String>>()
    val plugins = mutableListOf<Pair<IntRange, String>>()

    var versionsRead = false
    var librariesRead = false
    var bundlesRead = false
    var pluginsRead = false

    for ((lineNumber, line) in versionCatalogFile.readLines().withIndex()) {
        val actualLineNumber = lineNumber + 1
        val trimmedLine = line.trim()
        val trimmedLineIsNotEmpty = trimmedLine.isNotEmpty()

        if (trimmedLine == VersionCatalogSection.VERSIONS.label) {
            versionsRead = true
            librariesRead = false
            bundlesRead = false
            pluginsRead = false
        } else if (trimmedLine == VersionCatalogSection.LIBRARIES.label) {
            versionsRead = false
            librariesRead = true
            bundlesRead = false
            pluginsRead = false
        } else if (trimmedLine == VersionCatalogSection.BUNDLES.label) {
            versionsRead = false
            librariesRead = false
            bundlesRead = true
            pluginsRead = false
        } else if (trimmedLine == VersionCatalogSection.PLUGINS.label) {
            versionsRead = false
            librariesRead = false
            bundlesRead = false
            pluginsRead = true
        } else if (versionsRead && trimmedLineIsNotEmpty) {
            versions.add(Pair(actualLineNumber..actualLineNumber, line))
        } else if (librariesRead && trimmedLineIsNotEmpty) {
            libraries.add(Pair(actualLineNumber..actualLineNumber, line))
        } else if (bundlesRead && trimmedLineIsNotEmpty) {
            if (line.contains("[")) {
                bundles.add(Pair(actualLineNumber..actualLineNumber, line))
            } else {
                val (currentRange, currentLine) = bundles.removeAt(bundles.size - 1)
                val newRange = currentRange.first..actualLineNumber
                val newLine = currentLine + "\n" + line
                bundles.add(Pair(newRange, newLine))
            }
        } else if (pluginsRead && trimmedLineIsNotEmpty) {
            plugins.add(Pair(actualLineNumber..actualLineNumber, line))
        }
    }

    return VersionCatalog(versions, libraries, bundles, plugins)
}
