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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.tomlj.Toml
import java.io.File

abstract class VersionCatalogChecker : DefaultTask() {
    @get:InputFile
    abstract val versionCatalogFile: Property<File>

    @TaskAction
    fun checkVersionCatalog() {
        val catalog = readVersionCatalog(versionCatalogFile.get())
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkVersions(catalog.versions))
        errorMessages.addAll(checkLibraries(catalog.libraries))
        errorMessages.addAll(checkBundles(catalog.bundles))
        errorMessages.addAll(checkPlugins(catalog.plugins))

        if (errorMessages.isNotEmpty()) {
            errorMessages.sort()
            throw GradleException(errorMessages.joinToString("\n"))
        }
    }

    internal fun checkVersions(versions: List<Pair<IntRange, String>>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(versions, VersionCatalogSection.VERSIONS))
        errorMessages.addAll(checkAlphabeticSorting(versions, VersionCatalogSection.VERSIONS))

        for ((lineNumbers, version) in versions) {
            val parseResult = Toml.parse(version)
            val key = parseResult.keySet().iterator().next()

            if (parseResult.isTable(key)) {
                val attributes = parseResult.getTable(key)!!.keySet().toList()
                val attributeIndices =
                    listOf(
                        attributes.indexOf("strictly"),
                        attributes.indexOf("require"),
                        attributes.indexOf("prefer"),
                        attributes.indexOf("reject"),
                    ).filter { it > -1 }

                if (attributeIndices != attributeIndices.sorted()) {
                    val message =
                        "Attributes of version with key '$key' are not sorted correctly. " +
                            "Required order: strictly, require, prefer, reject"
                    errorMessages.add(ErrorMessage(lineNumbers, message))
                }
            }
        }

        errorMessages.sort()
        return errorMessages
    }

    internal fun checkLibraries(libraries: List<Pair<IntRange, String>>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(libraries, VersionCatalogSection.LIBRARIES))
        errorMessages.addAll(checkAlphabeticSorting(libraries, VersionCatalogSection.LIBRARIES))

        val bomDeclarations = getBomDeclarations(libraries)

        for ((lineNumbers, library) in libraries) {
            val parseResult = Toml.parse(library)
            val key = parseResult.keySet().iterator().next()
            val requiredOrder = "Required order: [module | group], name (, version(.ref))"

            if (parseResult.isString(key)) {
                val message =
                    "Use table notation instead of string notation for library with key '$key'. $requiredOrder"
                errorMessages.add(ErrorMessage(lineNumbers, message))
            } else if (parseResult.isTable(key)) {
                val attributes = parseResult.getTable(key)!!.keySet().iterator()
                val firstAttribute = attributes.next()
                val secondAttributeExists = attributes.hasNext()

                val isModuleDefined = firstAttribute == "module"
                val isGroupDefined = firstAttribute == "group"

                if (secondAttributeExists) {
                    val secondAttribute = attributes.next()

                    val isModuleDefined = isModuleDefined && secondAttribute.startsWith("version")
                    val isGroupAndNameDefined = isGroupDefined && secondAttribute == "name"
                    if (!isModuleDefined && !isGroupAndNameDefined) {
                        val message =
                            "Attributes of library with key '$key' are not sorted correctly. $requiredOrder"
                        errorMessages.add(ErrorMessage(lineNumbers, message))
                    }
                } else {
                    if (!isModuleDefined && !isGroupDefined) {
                        val message =
                            "Attributes of library with key '$key' are not sorted correctly. $requiredOrder"
                        errorMessages.add(ErrorMessage(lineNumbers, message))
                    } else {
                        val firstAttributeValue =
                            parseResult.getTable(key)?.getString(firstAttribute)?.split(":")
                                ?.first()
                        if (firstAttributeValue !in bomDeclarations) {
                            val message =
                                "Attributes of library with key '$key' has no version defined" +
                                    " or no bom declaration exists for '$firstAttributeValue'."
                            errorMessages.add(ErrorMessage(lineNumbers, message))
                        }
                    }
                }
            }
        }
        errorMessages.sort()
        return errorMessages
    }

    internal fun checkBundles(bundles: List<Pair<IntRange, String>>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(bundles, VersionCatalogSection.BUNDLES))
        errorMessages.addAll(checkAlphabeticSorting(bundles, VersionCatalogSection.BUNDLES))

        for ((lineNumbers, bundle) in bundles) {
            val parseResult = Toml.parse(bundle)
            val key = parseResult.keySet().iterator().next()
            val libraries = parseResult.getArray(key)!!.toList().map { it as String }
            val sortedLibraries = libraries.sorted()

            for ((i, library) in libraries.withIndex()) {
                val sortedLibrary = sortedLibraries[i]

                if (library != sortedLibrary) {
                    val numbers =
                        if (lineNumbers.first == lineNumbers.last) {
                            lineNumbers.first
                        } else {
                            lineNumbers.first + i + 1
                        }
                    val message =
                        "Libraries of bundle with key '$key' are not sorted alphabetically. " +
                            "Found library '$library' where '$sortedLibrary' was expected."
                    errorMessages.add(ErrorMessage(numbers..numbers, message))
                }
            }
        }
        errorMessages.sort()
        return errorMessages
    }

    internal fun checkPlugins(plugins: List<Pair<IntRange, String>>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(plugins, VersionCatalogSection.PLUGINS))
        errorMessages.addAll(checkAlphabeticSorting(plugins, VersionCatalogSection.PLUGINS))

        for ((lineNumbers, plugin) in plugins) {
            val parseResult = Toml.parse(plugin)
            val key = parseResult.keySet().iterator().next()
            val attributes = parseResult.getTable(key)!!.keySet().iterator()
            val firstAttribute = attributes.next()

            if (firstAttribute != "id") {
                val message =
                    "Attributes of plugin with key '$key' are not sorted correctly. " +
                        "Required order: id, version(.ref)"
                errorMessages.add(ErrorMessage(lineNumbers, message))
            }
        }
        errorMessages.sort()
        return errorMessages
    }

    private fun checkWhitespace(
        lines: List<Pair<IntRange, String>>,
        section: VersionCatalogSection,
    ): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()
        val splitRegex = Regex(" {2,}")

        for ((lineNumbers, line) in lines) {
            val parseResult = Toml.parse(line)
            val key = parseResult.keySet().iterator().next()

            if (line.trimStart() != line) {
                val message =
                    "Entry with key '$key' in section '${section.label}' must not have leading whitespace."
                errorMessages.add(ErrorMessage(lineNumbers, message))
            }

            if (section == VersionCatalogSection.BUNDLES) {
                val arraySize = parseResult.getArray(key)!!.size()
                val arrayElements = line.split(Regex("(?:\r\n|\r|\n)(?: {4}|])"))
                val isArraySizeCorrect = arrayElements.size == arraySize + 2
                val arrayElementStartsWithWhitespace = arrayElements.any { it.startsWith(" ") }

                if (!isArraySizeCorrect || arrayElementStartsWithWhitespace) {
                    val message =
                        "Bundle with key '$key' " +
                            "must be indented with each library on a separate line " +
                            "preceded by four whitespace characters."
                    errorMessages.add(ErrorMessage(lineNumbers, message))
                }
            } else if (line.split(splitRegex).size > 1) {
                val message =
                    "Entry with key '$key' " +
                        "in section '${section.label}' " +
                        "must not have two or more adjacent whitespace characters."
                errorMessages.add(ErrorMessage(lineNumbers, message))
            }

            if (line.trimEnd() != line) {
                val message =
                    "Entry with key '$key' " +
                        "in section '${section.label}' " +
                        "must not have trailing whitespace."
                errorMessages.add(ErrorMessage(lineNumbers, message))
            }
        }
        return errorMessages
    }

    private fun checkAlphabeticSorting(
        lines: List<Pair<IntRange, String>>,
        section: VersionCatalogSection,
    ): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()
        val sortedLines = lines.asSequence().map { it.second.trim() }.sorted().toList()

        for ((i, lineElem) in lines.withIndex()) {
            val (lineNumbers, line) = lineElem
            val sortedLine = sortedLines[i]

            if (line.trim() != sortedLine) {
                val lineKey = Toml.parse(line).keySet().iterator().next()
                val sortedLineKey = Toml.parse(sortedLine).keySet().iterator().next()
                val message =
                    "Entries are not sorted alphabetically in section '${section.label}'. " +
                        "Found key '$lineKey' where '$sortedLineKey' was expected."
                errorMessages.add(ErrorMessage(lineNumbers, message))
            }
        }
        return errorMessages
    }

    private fun getBomDeclarations(libraries: List<Pair<IntRange, String>>): List<String> {
        return libraries.filter {
            it.second.contains("bom")
        }.map {
            Toml.parse(it.second).let { parseResult ->
                val key = parseResult.keySet().first()
                parseResult.getTable(key)?.getString("module") ?: parseResult.getTable(key)
                    ?.getString("group")
            }!!.split(":").first()
        }
    }
}
