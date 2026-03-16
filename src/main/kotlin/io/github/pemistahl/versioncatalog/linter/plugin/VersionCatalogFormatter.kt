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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File

abstract class VersionCatalogFormatter : DefaultTask() {
    @get:InputFile
    abstract val versionCatalogFile: Property<File>

    @TaskAction
    fun formatVersionCatalog() {
        val catalog = readVersionCatalog(versionCatalogFile.get())
        val versions = formatVersions(catalog.versions)
        val libraries = formatLibraries(catalog.libraries)
        val bundles = formatBundles(catalog.bundles)
        val plugins = formatPlugins(catalog.plugins)
        val formattedCatalog = joinCatalogSections(
            versions,
            libraries,
            bundles,
            plugins,
            catalog.versionsPrecedingComments,
            catalog.librariesPrecedingComments,
            catalog.bundlesPrecedingComments,
            catalog.pluginsPrecedingComments,
            catalog.trailingComments,
        )

        File(versionCatalogFile.get().toURI()).writeText(formattedCatalog)
    }

    internal fun formatVersions(versions: List<VersionCatalogEntry>): List<String> =
        versions
            .map { version ->
                val content = version.content.trim()
                val parseResult = Toml.parse(content)
                val alias = parseResult.keySet().iterator().next()
                val inlineComment = extractInlineComment(content)
                val formattedVersion =
                    if (parseResult.isString(alias)) {
                        "$alias = \"${parseResult.getString(alias)}\""
                    } else if (parseResult.isTable(alias)) {
                        parseVersionTable(parseResult, alias)
                    } else {
                        content
                    }

                val fullEntry = mutableListOf<String>()
                fullEntry.addAll(version.precedingComments)
                fullEntry.add(if (inlineComment != null) "$formattedVersion $inlineComment" else formattedVersion)

                alias to fullEntry.joinToString("\n")
            }.sortedBy { it.first }
            .map { it.second }

    internal fun formatLibraries(libraries: List<VersionCatalogEntry>): List<String> =
        libraries
            .map { library ->
                val content = library.content.trim()
                val parseResult = Toml.parse(content)
                val alias = parseResult.keySet().iterator().next()
                val inlineComment = extractInlineComment(content)
                val formattedLibrary =
                    if (parseResult.isString(alias)) {
                        parseLibraryString(parseResult, alias)
                    } else if (parseResult.isTable(alias)) {
                        parseLibraryTable(parseResult, alias)
                    } else {
                        content
                    }

                val fullEntry = mutableListOf<String>()
                fullEntry.addAll(library.precedingComments)
                fullEntry.add(if (inlineComment != null) "$formattedLibrary $inlineComment" else formattedLibrary)

                alias to fullEntry.joinToString("\n")
            }.sortedBy { it.first }
            .map { it.second }

    internal fun formatBundles(bundles: List<VersionCatalogEntry>): List<String> =
        bundles
            .map { bundle ->
                val content = bundle.content.trim()
                val parseResult = Toml.parse(content)
                val alias = parseResult.keySet().iterator().next()
                val inlineComment = extractInlineComment(content)
                val libraries =
                    parseResult
                        .getArray(alias)!!
                        .toList()
                        .asSequence()
                        .map { library -> "\"$library\"" }
                        .sorted()
                        .toList()
                val separator = "\n    "
                val formattedBundle = "$alias = [$separator${libraries.joinToString(",$separator")}\n]"

                val fullEntry = mutableListOf<String>()
                fullEntry.addAll(bundle.precedingComments)
                fullEntry.add(if (inlineComment != null) "$formattedBundle $inlineComment" else formattedBundle)

                alias to fullEntry.joinToString("\n")
            }.sortedBy { it.first }
            .map { it.second }

    internal fun formatPlugins(plugins: List<VersionCatalogEntry>): List<String> =
        plugins
            .map { plugin ->
                val content = plugin.content.trim()
                val parseResult = Toml.parse(content)
                val alias = parseResult.keySet().iterator().next()
                val inlineComment = extractInlineComment(content)
                val values = parseResult.getTable(alias)?.toMap()
                val id = values?.get("id")
                val version = values?.get("version")
                var formattedPlugin = "$alias = { id = \"$id\""

                if (version is String) {
                    formattedPlugin = "$formattedPlugin, version = \"$version\" }"
                } else if (version is TomlTable) {
                    formattedPlugin = "$formattedPlugin, version.ref = \"${version.get("ref")}\" }"
                } else if (version == null) {
                    formattedPlugin = "$formattedPlugin }"
                }

                val fullEntry = mutableListOf<String>()
                fullEntry.addAll(plugin.precedingComments)
                fullEntry.add(if (inlineComment != null) "$formattedPlugin $inlineComment" else formattedPlugin)

                alias to fullEntry.joinToString("\n")
            }.sortedBy { it.first }
            .map { it.second }

    private fun extractInlineComment(content: String): String? {
        var isInsideQuotes = false
        var commentIndex = -1
        for (i in content.indices) {
            val char = content[i]
            if (char == '\"') {
                isInsideQuotes = !isInsideQuotes
            } else if (char == '#' && !isInsideQuotes) {
                commentIndex = i
                break
            }
        }
        return if (commentIndex != -1) content.substring(commentIndex).trimEnd() else null
    }

    internal fun joinCatalogSections(
        versions: List<String>,
        libraries: List<String>,
        bundles: List<String>,
        plugins: List<String>,
        versionsPrecedingComments: List<String> = emptyList(),
        librariesPrecedingComments: List<String> = emptyList(),
        bundlesPrecedingComments: List<String> = emptyList(),
        pluginsPrecedingComments: List<String> = emptyList(),
        trailingComments: List<String> = emptyList(),
    ): String {
        val sections = mutableListOf<String>()
        val separator = "\n"

        if (versions.isNotEmpty()) {
            sections.addAll(versionsPrecedingComments)
            sections.addAll(
                listOf(
                    VersionCatalogSection.VERSIONS.label,
                    versions.joinToString(separator),
                    "",
                ),
            )
        }

        if (libraries.isNotEmpty()) {
            sections.addAll(librariesPrecedingComments)
            sections.addAll(
                listOf(
                    VersionCatalogSection.LIBRARIES.label,
                    libraries.joinToString(separator),
                    "",
                ),
            )
        }

        if (bundles.isNotEmpty()) {
            sections.addAll(bundlesPrecedingComments)
            sections.addAll(
                listOf(
                    VersionCatalogSection.BUNDLES.label,
                    bundles.joinToString(separator),
                    "",
                ),
            )
        }

        if (plugins.isNotEmpty()) {
            sections.addAll(pluginsPrecedingComments)
            sections.addAll(
                listOf(
                    VersionCatalogSection.PLUGINS.label,
                    plugins.joinToString(separator),
                    "",
                ),
            )
        }

        if (trailingComments.isNotEmpty()) {
            sections.addAll(trailingComments)
            sections.add("")
        }

        return sections.joinToString(separator)
    }

    private fun parseVersionTable(
        parseResult: TomlTable,
        alias: String,
    ): String {
        val stringKeys = listOf("strictly", "require", "prefer")
        val arrayKey = "reject"
        val versionTable = parseResult.getTable(alias) ?: parseResult
        var value = "$alias = {"
        val tableValues = mutableListOf<String>()

        for (stringKey in stringKeys) {
            if (versionTable.isString(stringKey)) {
                versionTable.getString(stringKey)!!.let { v ->
                    tableValues.add("$stringKey = \"$v\"")
                }
            }
        }

        if (versionTable.isArray(arrayKey)) {
            val versionsArray =
                versionTable
                    .getArray(arrayKey)!!
                    .toList()
                    .asSequence()
                    .map { v -> "\"$v\"" }
                    .sorted()
                    .toList()
            tableValues.add("$arrayKey = [ ${versionsArray.joinToString(", ")} ]")
        }

        if (tableValues.isNotEmpty()) {
            value = "$value ${tableValues.joinToString(", ")}"
        }

        value = "$value }"
        return value
    }

    private fun parseLibraryString(
        parseResult: TomlTable,
        alias: String,
    ): String =
        parseResult.getString(alias)!!.let { value ->
            val valueParts = value.split(":")
            when (valueParts.size) {
                3 -> {
                    val (group, name, version) = valueParts
                    "$alias = { group = \"$group\", name = \"$name\", version = \"$version\" }"
                }
                2 -> {
                    val (group, name) = valueParts
                    "$alias = { group = \"$group\", name = \"$name\" }"
                }
                else -> {
                    "$alias = $value"
                }
            }
        }

    private fun parseLibraryTable(
        parseResult: TomlTable,
        alias: String,
    ): String =
        parseResult.getTable(alias)!!.let { values ->
            var lib = "$alias = {"
            if (values.isString("module")) {
                values.getString("module")!!.let { module ->
                    val moduleParts = module.split(":")
                    if (moduleParts.size == 2) {
                        val (group, name) = moduleParts
                        lib = "$lib group = \"$group\", name = \"$name\""
                    } else {
                        lib = "$lib module = \"$module\""
                    }
                }
            } else if (values.contains("group") && values.contains("name")) {
                val group = values["group"]
                val name = values["name"]
                lib = "$lib group = \"$group\", name = \"$name\""
            }
            if (values.contains("version")) {
                val version = values["version"]
                if (version is String) {
                    lib = "$lib, version = \"$version\""
                } else if (version is TomlTable) {
                    lib =
                        if (version.get("ref") != null) {
                            "$lib, version.ref = \"${version.get("ref")}\""
                        } else {
                            "$lib, ${parseVersionTable(version, "version")}"
                        }
                }
            }

            lib = "$lib }"
            lib
        }
}
