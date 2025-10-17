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
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File

abstract class VersionCatalogChecker : DefaultTask() {
    @get:InputFile
    abstract val versionCatalogFile: Property<File>

    @get:Input
    abstract val bomsAndDependencies: MapProperty<String, List<String>>

    @TaskAction
    fun checkVersionCatalog() {
        val catalog = readVersionCatalog(versionCatalogFile.get())

        checkBomsAndDependencies(catalog.libraries)

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

    internal fun checkVersions(versions: List<VersionCatalogEntry>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(versions, VersionCatalogSection.VERSIONS))
        errorMessages.addAll(checkAlphabeticSorting(versions, VersionCatalogSection.VERSIONS))

        for (version in versions) {
            val parseResult = Toml.parse(version.content)
            val alias = parseResult.keySet().iterator().next()

            if (parseResult.isTable(alias)) {
                val versionTable = parseResult.getTableOrEmpty(alias)
                errorMessages.addAll(checkRichVersions(versionTable, alias, version.lineNumbers))
            }
        }

        errorMessages.sort()
        return errorMessages
    }

    internal fun checkLibraries(libraries: List<VersionCatalogEntry>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(libraries, VersionCatalogSection.LIBRARIES))
        errorMessages.addAll(checkAlphabeticSorting(libraries, VersionCatalogSection.LIBRARIES))

        for (library in libraries) {
            val parseResult = Toml.parse(library.content)
            val alias = parseResult.keySet().iterator().next()

            val requiredOrder = "Required order: [module | group], name (, version(.ref))"
            val tableNotationMessage = "Use table notation instead of string notation for library with alias '$alias'. $requiredOrder"
            val notSortedMessage = "Attributes of library with alias '$alias' are not sorted correctly. $requiredOrder"
            val noBomMessage =
                "Library with alias '$alias' has no version defined and no BOM declaration exists for it."

            if (parseResult.isString(alias)) {
                errorMessages.add(ErrorMessage(library.lineNumbers, tableNotationMessage))
            } else if (parseResult.isTable(alias)) {
                val libraryTable = parseResult.getTableOrEmpty(alias)

                if (libraryTable.isTable("version")) {
                    val versionTable = libraryTable.getTableOrEmpty("version")
                    errorMessages.addAll(checkRichVersions(versionTable, alias, library.lineNumbers))
                }

                val attributes = libraryTable.keySet().iterator()
                val firstAttribute = attributes.next()
                val secondAttributeExists = attributes.hasNext()

                val isFirstAttributeModule = firstAttribute == "module"
                val isFirstAttributeGroup = firstAttribute == "group"

                if (secondAttributeExists) {
                    val secondAttribute = attributes.next()
                    val isSecondAttributeName = secondAttribute == "name"
                    val isSecondAttributeVersion = secondAttribute.startsWith("version")

                    val isModuleAndVersionSorted = isFirstAttributeModule && isSecondAttributeVersion
                    val isGroupAndNameSorted = isFirstAttributeGroup && isSecondAttributeName

                    if (!isModuleAndVersionSorted && !isGroupAndNameSorted) {
                        errorMessages.add(ErrorMessage(library.lineNumbers, notSortedMessage))
                    }

                    val hasGroup = libraryTable.isString("group")
                    val hasName = libraryTable.isString("name")

                    if (hasGroup && hasName && !attributes.hasNext()) {
                        if (!isLibraryDependencyOfBom(alias)) {
                            errorMessages.add(ErrorMessage(library.lineNumbers, noBomMessage))
                        }
                    }
                } else if (!isFirstAttributeModule && !isFirstAttributeGroup) {
                    errorMessages.add(ErrorMessage(library.lineNumbers, notSortedMessage))
                } else {
                    if (!isLibraryDependencyOfBom(alias)) {
                        errorMessages.add(ErrorMessage(library.lineNumbers, noBomMessage))
                    }
                }
            }
        }
        errorMessages.sort()
        return errorMessages
    }

    internal fun checkBundles(bundles: List<VersionCatalogEntry>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(bundles, VersionCatalogSection.BUNDLES))
        errorMessages.addAll(checkAlphabeticSorting(bundles, VersionCatalogSection.BUNDLES))

        for (bundle in bundles) {
            val parseResult = Toml.parse(bundle.content)
            val alias = parseResult.keySet().iterator().next()
            val libraries = parseResult.getArray(alias)!!.toList().map { it as String }
            val sortedLibraries = libraries.sorted()

            for ((i, library) in libraries.withIndex()) {
                val sortedLibrary = sortedLibraries[i]

                if (library != sortedLibrary) {
                    val lineNumber =
                        if (bundle.lineNumbers.first == bundle.lineNumbers.last) {
                            bundle.lineNumbers.first
                        } else {
                            bundle.lineNumbers.first + i + 1
                        }
                    val message =
                        "Libraries of bundle with alias '$alias' are not sorted alphabetically. " +
                            "Found library '$library' where '$sortedLibrary' was expected."
                    errorMessages.add(ErrorMessage(lineNumber, message))
                }
            }
        }
        errorMessages.sort()
        return errorMessages
    }

    internal fun checkPlugins(plugins: List<VersionCatalogEntry>): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()

        errorMessages.addAll(checkWhitespace(plugins, VersionCatalogSection.PLUGINS))
        errorMessages.addAll(checkAlphabeticSorting(plugins, VersionCatalogSection.PLUGINS))

        for (plugin in plugins) {
            val parseResult = Toml.parse(plugin.content)
            val alias = parseResult.keySet().iterator().next()
            val attributes = parseResult.getTable(alias)!!.keySet().iterator()
            val firstAttribute = attributes.next()

            if (firstAttribute != "id") {
                val message =
                    "Attributes of plugin with alias '$alias' are not sorted correctly. " +
                        "Required order: id, version(.ref)"
                errorMessages.add(ErrorMessage(plugin.lineNumbers, message))
            }
        }
        errorMessages.sort()
        return errorMessages
    }

    private fun checkBomsAndDependencies(libraries: List<VersionCatalogEntry>) {
        val errorMessages = mutableListOf<String>()
        val bomAliases = bomsAndDependencies.keySet().get()
        val dependencyAliases = bomsAndDependencies.get().values.flatten()
        val bomAndDependencyAliases = bomAliases.plus(dependencyAliases)
        val libraryAliases =
            libraries
                .asSequence()
                .map {
                    Toml
                        .parse(it.content.trim())
                        .keySet()
                        .iterator()
                        .next()
                }.filter { alias -> bomAndDependencyAliases.contains(alias) }
                .toSet()

        val missingAliases = bomAndDependencyAliases.subtract(libraryAliases)

        if (missingAliases.isNotEmpty()) {
            val formattedAliases = missingAliases.map { "'$it'" }.sorted().joinToString(separator = ", ")
            val alias = if (missingAliases.size == 1) "alias" else "aliases"
            errorMessages.add(
                "The following $alias in the version catalog " +
                    "linter settings cannot be matched with " +
                    "a library in the version catalog: $formattedAliases",
            )
        }

        val bomAliasesToNames =
            libraries
                .asSequence()
                .map { Toml.parse(it.content.trim()) }
                .filter { parseResult ->
                    val alias = parseResult.keySet().iterator().next()
                    bomAliases.contains(alias)
                }.map { parseResult ->
                    val alias = parseResult.keySet().iterator().next()
                    if (parseResult.isString(alias)) {
                        val bomName =
                            parseResult.getString(alias)!!.let { value ->
                                val valueParts = value.split(":")
                                if (valueParts.size in 2..3) {
                                    valueParts[1]
                                } else {
                                    valueParts[0]
                                }
                            }
                        alias to bomName
                    } else if (parseResult.isTable(alias)) {
                        val bomName =
                            parseResult.getTable(alias)!!.let { values ->
                                if (values.isString("module")) {
                                    values.getString("module")!!.let { module ->
                                        val moduleParts = module.split(":")
                                        if (moduleParts.size == 2) {
                                            moduleParts[1]
                                        } else {
                                            moduleParts[0]
                                        }
                                    }
                                } else if (values.contains("group") && values.contains("name")) {
                                    values["name"] as String
                                } else {
                                    null
                                }
                            }
                        alias to bomName
                    } else {
                        alias to null
                    }
                }.toMap()

        val invalidBomAliasesToNames =
            bomAliasesToNames
                .filter { (_, name) -> name != null && !name.endsWith("-bom") && !name.endsWith("-dependencies") }

        if (invalidBomAliasesToNames.isNotEmpty()) {
            val formattedAliases =
                invalidBomAliasesToNames
                    .map { (alias, _) -> "'$alias'" }
                    .sorted()
                    .joinToString(separator = ", ")
            errorMessages.add(
                "The libraries identified by the following aliases " +
                    "do not seem to be proper BOMs as their names do not " +
                    "end with the suffix '-bom' or '-dependencies': $formattedAliases",
            )
        }

        if (errorMessages.isNotEmpty()) {
            throw GradleException(errorMessages.joinToString("\n"))
        }
    }

    private fun checkWhitespace(
        entries: List<VersionCatalogEntry>,
        section: VersionCatalogSection,
    ): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()
        val splitRegex = Regex(" {2,}")

        for (entry in entries) {
            val parseResult = Toml.parse(entry.content)
            val alias = parseResult.keySet().iterator().next()

            if (entry.content.trimStart() != entry.content) {
                val message =
                    "Entry with alias '$alias' in section '${section.label}' must not have leading whitespace."
                errorMessages.add(ErrorMessage(entry.lineNumbers, message))
            }

            if (section == VersionCatalogSection.BUNDLES) {
                val arraySize = parseResult.getArray(alias)!!.size()
                val arrayElements = entry.content.split(Regex("(?:\r\n|\r|\n)(?: {4}|])"))
                val isArraySizeCorrect = arrayElements.size == arraySize + 2
                val arrayElementStartsWithWhitespace = arrayElements.any { it.startsWith(" ") }

                if (!isArraySizeCorrect || arrayElementStartsWithWhitespace) {
                    val message =
                        "Bundle with alias '$alias' " +
                            "must be indented with each library on a separate line " +
                            "preceded by four whitespace characters."
                    errorMessages.add(ErrorMessage(entry.lineNumbers, message))
                }
            } else if (entry.content.split(splitRegex).size > 1) {
                val message =
                    "Entry with alias '$alias' " +
                        "in section '${section.label}' " +
                        "must not have two or more adjacent whitespace characters."
                errorMessages.add(ErrorMessage(entry.lineNumbers, message))
            }

            if (entry.content.trimEnd() != entry.content) {
                val message =
                    "Entry with alias '$alias' " +
                        "in section '${section.label}' " +
                        "must not have trailing whitespace."
                errorMessages.add(ErrorMessage(entry.lineNumbers, message))
            }
        }
        return errorMessages
    }

    private fun checkAlphabeticSorting(
        entries: List<VersionCatalogEntry>,
        section: VersionCatalogSection,
    ): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()
        val sortedEntries =
            entries
                .asSequence()
                .map { it.content.trim() }
                .sorted()
                .toList()

        for ((i, entry) in entries.withIndex()) {
            val sortedEntry = sortedEntries[i]

            if (entry.content.trim() != sortedEntry) {
                val entryAlias =
                    Toml
                        .parse(entry.content)
                        .keySet()
                        .iterator()
                        .next()
                val sortedEntryAlias =
                    Toml
                        .parse(sortedEntry)
                        .keySet()
                        .iterator()
                        .next()
                val message =
                    "Entries are not sorted alphabetically in section '${section.label}'. " +
                        "Found alias '$entryAlias' where '$sortedEntryAlias' was expected."
                errorMessages.add(ErrorMessage(entry.lineNumbers, message))
            }
        }
        return errorMessages
    }

    private fun checkRichVersions(
        versionTable: TomlTable,
        alias: String,
        lineNumbers: IntRange,
    ): List<ErrorMessage> {
        val errorMessages = mutableListOf<ErrorMessage>()
        val attributes = versionTable.keySet().toList()
        val attributeIndices =
            listOf(
                attributes.indexOf("strictly"),
                attributes.indexOf("require"),
                attributes.indexOf("prefer"),
                attributes.indexOf("reject"),
            ).filter { it > -1 }

        if (attributeIndices != attributeIndices.sorted()) {
            val message =
                "Version attributes of entry with alias '$alias' are not sorted correctly. " +
                    "Required order: strictly, require, prefer, reject"
            errorMessages.add(ErrorMessage(lineNumbers, message))
        }
        return errorMessages
    }

    private fun isLibraryDependencyOfBom(libraryAlias: String): Boolean = bomsAndDependencies.get().values.any { it.contains(libraryAlias) }
}
