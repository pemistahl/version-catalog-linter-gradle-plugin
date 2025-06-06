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
        val formattedCatalog = joinCatalogSections(versions, libraries, bundles, plugins)

        File(versionCatalogFile.get().toURI()).writeText(formattedCatalog)
    }

    internal fun formatVersions(versions: List<Pair<IntRange, String>>): List<String> {
        return versions.asSequence()
            .map { pair ->
                val version = pair.second.trim()
                val parseResult = Toml.parse(version)
                val alias = parseResult.keySet().iterator().next()
                val formattedVersion =
                    if (parseResult.isString(alias)) {
                        "$alias = \"${parseResult.getString(alias)}\""
                    } else if (parseResult.isTable(alias)) {
                        parseVersionTable(parseResult, alias)
                    } else {
                        version
                    }
                formattedVersion
            }
            .sorted()
            .toList()
    }

    internal fun formatLibraries(libraries: List<Pair<IntRange, String>>): List<String> {
        return libraries.asSequence()
            .map { pair ->
                val library = pair.second.trim()
                val parseResult = Toml.parse(library)
                val alias = parseResult.keySet().iterator().next()
                val formattedLibrary =
                    if (parseResult.isString(alias)) {
                        parseLibraryString(parseResult, alias)
                    } else if (parseResult.isTable(alias)) {
                        parseLibraryTable(parseResult, alias)
                    } else {
                        library
                    }
                formattedLibrary
            }
            .sorted()
            .toList()
    }

    internal fun formatBundles(bundles: List<Pair<IntRange, String>>): List<String> {
        return bundles.asSequence()
            .map { pair ->
                val bundle = pair.second.trim()
                val parseResult = Toml.parse(bundle)
                val alias = parseResult.keySet().iterator().next()
                val libraries =
                    parseResult.getArray(alias)!!.toList()
                        .asSequence()
                        .map { library -> "\"$library\"" }
                        .sorted()
                        .toList()
                val separator = "\n    "
                "$alias = [$separator${libraries.joinToString(",$separator")}\n]"
            }
            .sorted()
            .toList()
    }

    internal fun formatPlugins(plugins: List<Pair<IntRange, String>>): List<String> {
        return plugins.asSequence()
            .map { pair ->
                val plugin = pair.second.trim()
                val parseResult = Toml.parse(plugin)
                val alias = parseResult.keySet().iterator().next()
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

                formattedPlugin
            }
            .sorted()
            .toList()
    }

    internal fun joinCatalogSections(
        versions: List<String>,
        libraries: List<String>,
        bundles: List<String>,
        plugins: List<String>,
    ): String {
        val sections = mutableListOf<String>()
        val separator = "\n"

        if (versions.isNotEmpty()) {
            sections.addAll(
                listOf(
                    VersionCatalogSection.VERSIONS.label,
                    versions.joinToString(separator),
                    "",
                ),
            )
        }

        if (libraries.isNotEmpty()) {
            sections.addAll(
                listOf(
                    VersionCatalogSection.LIBRARIES.label,
                    libraries.joinToString(separator),
                    "",
                ),
            )
        }

        if (bundles.isNotEmpty()) {
            sections.addAll(
                listOf(
                    VersionCatalogSection.BUNDLES.label,
                    bundles.joinToString(separator),
                    "",
                ),
            )
        }

        if (plugins.isNotEmpty()) {
            sections.addAll(
                listOf(
                    VersionCatalogSection.PLUGINS.label,
                    plugins.joinToString(separator),
                    "",
                ),
            )
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
                versionTable.getArray(arrayKey)!!.toList()
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
    ): String {
        return parseResult.getString(alias)!!.let { value ->
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
    }

    private fun parseLibraryTable(
        parseResult: TomlTable,
        alias: String,
    ): String {
        return parseResult.getTable(alias)!!.let { values ->
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
}
